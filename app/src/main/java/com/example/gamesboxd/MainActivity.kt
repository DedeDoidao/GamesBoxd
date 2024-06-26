package com.example.gamesboxd

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private lateinit var InputName: EditText
    private lateinit var InputSenha: EditText
    private lateinit var AbrirEsqueciSenha: TextView
    private lateinit var AbrirCadastrarConta: TextView
    private lateinit var Entrar: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInButton: com.google.android.gms.common.SignInButton
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        InputName = findViewById(R.id.inputEmail)
        InputSenha = findViewById(R.id.inputPassword)
        AbrirEsqueciSenha = findViewById(R.id.textView_Password)
        AbrirCadastrarConta = findViewById(R.id.textView_SignUp)
        Entrar = findViewById(R.id.btn_SigIn)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        googleSignInButton = findViewById(R.id.btn_Google)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("240112613430-8j99980hvt1rhpjhjciav6ukmi2vpamd.apps.googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        googleSignInButton.setOnClickListener {
            signInGoogle()
        }
        Entrar.setOnClickListener {
            val username = InputName.text.toString()
            val password = InputSenha.text.toString()

            if(username.isEmpty() || password.isEmpty()){
                showSnack("Complete todos os campos", Color.GRAY)
            } else {
                auth.signInWithEmailAndPassword(username, password).addOnCompleteListener(this){ task ->

                    if(task.isSuccessful){
                        showSnack("Login realizado!", Color.GREEN)
                    } else {
                        showSnack("Credencias não encontradas!", Color.RED)
                    }
                }
            }
        }

        AbrirEsqueciSenha.setOnClickListener {
            val intent = Intent(this, EsqueciSenha::class.java)
            startActivity(intent)
        }

        AbrirCadastrarConta.setOnClickListener {
            val intent = Intent(this, CadastrarConta::class.java)
            startActivity(intent)
        }

    }

    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
    }
    private fun showSnack(message: String, color: Int) {
        val view = findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        snackbar.setBackgroundTint(color)
        snackbar.show()
    }

    private fun signInGoogle(){
        val intent = googleSignInClient.signInIntent
        abreActivity.launch(intent)
    }

   var abreActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result: ActivityResult ->

       if(result.resultCode == RESULT_OK){
           val intent = result.data
           val task = GoogleSignIn.getSignedInAccountFromIntent(intent)

           try{
                val conta = task.getResult(ApiException::class.java)
               LoginGoogle(conta.idToken!!)
           } catch (exception: ApiException) {
               showSnack("Erro ao fazer login com Google: ${exception.localizedMessage}", Color.RED)
           }
       }
   }

    private fun LoginGoogle(token: String){
        val credencial = GoogleAuthProvider.getCredential(token, null)
        auth.signInWithCredential(credencial).addOnCompleteListener(this){
            task: Task<AuthResult> ->

            if(task.isSuccessful){
                val user = auth.currentUser

                if(user != null){
                    val userId = user.uid
                    val email = user.email
                    val displayName = user.displayName
                    val userImg = user.photoUrl.toString()

                    val userDocRef = firestore.collection("Users").document(userId)
                    userDocRef.get().addOnSuccessListener { document ->
                        if(document.exists()){
                            showSnack("Login Realizado com Google!", Color.GREEN)
                        } else {
                            val newuser = hashMapOf(
                                "email" to email,
                                "nome" to displayName,
                                "picture" to userImg
                            )
                            userDocRef.set(newuser).addOnCompleteListener { cadastrargoogle ->
                                if(cadastrargoogle.isSuccessful){
                                    showSnack("Cadastro realizado com Google!", Color.GREEN)
                                } else {
                                    showSnack("Erro ao registrar usuário no banco de dados!", Color.RED)
                                }
                            }
                        }

                    }.addOnFailureListener {
                        showSnack("Erro ao verificar usuário no banco de dados!", Color.RED)
                    }
                }
            } else {
                showSnack("Credencias não encontradas!", Color.RED)
            }
        }
    }


    private fun SalvarImgConta (imagemUri: Uri, userId: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit){
        val imagemRef = FirebaseStorage.getInstance().reference.child("profileImages/$userId.jpg")
        val upload = imagemRef.putFile(imagemUri)

        upload.continueWithTask { task ->
            if(!task.isSuccessful){
                task.exception?.let { throw it }
            }
            imagemRef.downloadUrl
        }.addOnCompleteListener { task ->
            if(task.isSuccessful){
                val DownloadUri = task.result
                onSuccess(DownloadUri.toString())
            } else {
                onFailure(task.exception ?: Exception("Erro desconhecido"))
            }
        }
    }

    private fun CarregarImagem(imagemUri: Uri){
        val userId = auth.currentUser?.uid ?: return
        SalvarImgConta(imagemUri, userId, { imagemUrl ->
            firestore.collection("Users").document(userId).update("picture", imagemUrl)
                .addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        showSnack("Imagem atualizada com sucesso!", Color.GREEN)
                    } else {
                        showSnack("Erro ao atualizar a imagem!", Color.RED)
                    }
                }
        }, { exception ->
            showSnack("Erro ao fazer upload da imagem: ${exception.localizedMessage}", Color.RED)
        })
    }
}