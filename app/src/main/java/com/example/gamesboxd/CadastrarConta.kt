package com.example.gamesboxd

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore

class CadastrarConta : AppCompatActivity() {
    private lateinit var InputNome: EditText
    private lateinit var InputEmail: EditText
    private lateinit var InputSenha: EditText
    private lateinit var InputConfSenha: EditText
    private lateinit var Cadastrar: Button

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cadastrar_conta)

        InputNome = findViewById(R.id.inputNome)
        InputEmail = findViewById(R.id.inputEmail)
        InputSenha = findViewById(R.id.inputSenha)
        InputConfSenha = findViewById(R.id.inputConfirmarSenha)
        Cadastrar = findViewById(R.id.btn_Cadastrar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Cadastrar.setOnClickListener{
            val nome = InputNome.text.toString()
            val email = InputEmail.text.toString()
            val senha = InputSenha.text.toString()
            val confSenha = InputConfSenha.text.toString()

            if(ConfereCampos(nome, email, senha, confSenha)){

                if(ConfereSenha(senha, confSenha)){
                    auth.createUserWithEmailAndPassword(email, senha).addOnCompleteListener { cadastro ->
                        if(cadastro.isSuccessful){
                            val userId = auth.currentUser?.uid
                            if(userId != null){
                                val user = hashMapOf(
                                    "email" to email,
                                    "nome" to nome
                                )
                                firestore.collection("Users")
                                    .document(userId).set(user).addOnCompleteListener { task ->
                                    if(task.isSuccessful){
                                        showSnack("Cadastro realizado com sucesso", Color.GREEN)
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
                    showSnack("As senhas estão diferentes", Color.GRAY)
                }
            } else {
                showSnack("Preencha todos os campos!", Color.GRAY)
            }
        }
    }

    private fun showSnack(message: String, color: Int) {
        val view = findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        snackbar.setBackgroundTint(color)
        snackbar.show()
    }
    private fun ConfereCampos(nome: String, email: String, senha: String, confSenha: String): Boolean{
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