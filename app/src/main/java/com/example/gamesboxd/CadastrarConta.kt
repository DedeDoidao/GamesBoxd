package com.example.gamesboxd

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class CadastrarConta : AppCompatActivity() {
    private lateinit var InputFoto: ImageView
    private lateinit var SelecionarImg: ActivityResultLauncher<Intent>
    private lateinit var InputNome: EditText
    private lateinit var InputEmail: EditText
    private lateinit var InputSenha: EditText
    private lateinit var InputConfSenha: EditText
    private lateinit var InputUser: EditText
    private lateinit var Cadastrar: Button

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var storageRef: StorageReference

    private var imgUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cadastrar_conta)

        InputFoto = findViewById(R.id.imgView_Foto)
        SelecionarImg = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                imgUri = result.data?.data
                InputFoto.setImageURI(imgUri)
            }
        }
        storageRef = FirebaseStorage.getInstance().reference
        InputNome = findViewById(R.id.inputNome)
        InputEmail = findViewById(R.id.inputEmail)
        InputUser = findViewById(R.id.inputUser)
        InputSenha = findViewById(R.id.inputSenha)
        InputConfSenha = findViewById(R.id.inputConfirmarSenha)
        Cadastrar = findViewById(R.id.btn_Cadastrar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        InputFoto.setOnClickListener {
            ObterImg_Galeria()
        }

        Cadastrar.setOnClickListener{
            val nome = InputNome.text.toString()
            val email = InputEmail.text.toString()
            val user = InputUser.text.toString()
            val senha = InputSenha.text.toString()
            val confSenha = InputConfSenha.text.toString()

            if(ConfereCampos(nome, email, user, senha, confSenha)){

                if(ConfereSenha(senha, confSenha)){
                    firestore.collection("Users").whereEqualTo("id", user)
                        .get().addOnCompleteListener { task ->

                            if(task.isSuccessful){

                                if(task.result?.documents?.isEmpty() == true){

                                    auth.createUserWithEmailAndPassword(email, senha).addOnCompleteListener { cadastro ->
                                        if(cadastro.isSuccessful){
                                            val userId = auth.currentUser?.uid
                                            if(userId != null){
                                                val user = hashMapOf(
                                                    "nome" to nome,
                                                    "email" to email,
                                                    "id" to user,
                                                    "picture" to null
                                                )
                                                firestore.collection("Users")
                                                    .document(userId).set(user).addOnCompleteListener { task ->
                                                        if(task.isSuccessful){

                                                            if(imgUri != null){
                                                                UploadImg(userId, imgUri!!)
                                                                showSnack("Cadastro realizado com sucesso", ContextCompat.getColor(this, R.color.ColorSecundary))
                                                            }
                                                        } else {
                                                            val errorMessage = task.exception?.message ?: "Erro desconhecido"
                                                            Log.e("FirestoreError", errorMessage)
                                                            showSnack("Erro ao registrar usuário no banco de dados!", Color.RED)
                                                        }
                                                    }
                                            }
                                        }
                                    }.addOnFailureListener { exeception ->

                                        val mensagemErro = when (exeception){
                                            is FirebaseAuthWeakPasswordException -> "Digite uma senha de no minimo 6 caracteres!"
                                            is FirebaseAuthInvalidCredentialsException -> "Digite um email válido!"
                                            is FirebaseAuthUserCollisionException -> "Email já cadastrado!"
                                            is FirebaseNetworkException -> "Sem conexão com a internet!"
                                            else -> "Erro ao cadastrar!"
                                        }
                                        showSnack(mensagemErro, Color.RED)

                                }
                            } else {
                                    showSnack("Id já cadastrado!", Color.RED)
                                }
                        }
                    }
                } else {
                    showSnack("As senhas estão diferentes", Color.GRAY)
                }
            } else {
                showSnack("Preencha todos os campos!", Color.GRAY)
            }
        }
    }

    private fun ObterImg_Galeria(){
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        SelecionarImg.launch(intent)
    }

    private fun UploadImg(userId: String, imgUri: Uri){
        val imgRef = storageRef.child("profileImages/$userId.jpg")

        imgRef.putFile(imgUri).addOnSuccessListener {

            imgRef.downloadUrl.addOnSuccessListener { uri ->
                firestore.collection("Users").document(userId).update("picture", uri.toString())
                    .addOnSuccessListener {
                        showSnack("Imagem enviada com sucesso", ContextCompat.getColor(this, R.color.ColorSecundary))
                    }.addOnFailureListener {
                        showSnack("Erro ao atualizar a imagem!", Color.RED)
                    }
            } .addOnFailureListener {
                showSnack("Erro ao subir a imagem!", Color.RED)
            }
        }
    }

    private fun showSnack(message: String, color: Int) {
        val view = findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        snackbar.setBackgroundTint(color)
        snackbar.show()
    }
    private fun ConfereCampos(nome: String, email: String, user: String, senha: String, confSenha: String): Boolean{
        if(nome.isEmpty() || email.isEmpty() || senha.isEmpty() || confSenha.isEmpty()){
            return false
        }
        return true
    }
    private fun ConfereSenha(senha: String, confirmacao: String): Boolean{
        if(senha == confirmacao){
            return true
        }
        return false
    }
}