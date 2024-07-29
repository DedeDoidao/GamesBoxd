package com.example.gamesboxd

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.internal.safeparcel.SafeParcelable
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
import java.util.UUID

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
                        showSnack("Login realizado!", ContextCompat.getColor(this, R.color.ColorSecundary))
                        Handler(Looper.getMainLooper()).postDelayed({
                            ShowMenu()
                        }, 2000)
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
    }
    private fun showSnack(message: String, color: Int) {
        val view = findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        snackbar.setBackgroundTint(color)
        snackbar.show()
    }

    private fun ShowMenu(){
        val intent = Intent(this, Menu::class.java)
        startActivity(intent)
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
                            showSnack("Login Realizado com Google!", ContextCompat.getColor(this, R.color.ColorSecundary))
                            Handler(Looper.getMainLooper()).postDelayed({
                                ShowMenu()
                            }, 2500)
                        } else {
                            val newuser = hashMapOf(
                                "nome" to displayName,
                                "email" to email,
                                "id" to UUID.randomUUID().toString(),
                                "picture" to userImg
                            )
                            userDocRef.set(newuser).addOnCompleteListener { cadastrargoogle ->
                                if(cadastrargoogle.isSuccessful){
                                    showSnack("Cadastro realizado com Google!", ContextCompat.getColor(this, R.color.ColorSecundary))
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        ShowMenu()
                                    }, 2000)

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

}