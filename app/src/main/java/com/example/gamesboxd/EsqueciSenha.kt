package com.example.gamesboxd

import android.graphics.Color
import android.os.Bundle
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
import com.google.firebase.firestore.FirebaseFirestore

class EsqueciSenha : AppCompatActivity() {

    private lateinit var InputEmail: EditText
    private lateinit var RecuperarSenha: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore : FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_esqueci_senha)
        InputEmail = findViewById(R.id.inputEmail)
        RecuperarSenha = findViewById(R.id.btn_RecuperarSenha)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        RecuperarSenha.setOnClickListener {
            val email = InputEmail.text.toString()

            if (email.isEmpty()) {
                showSnack("Insira seu email!", Color.GRAY)
            } else {
                // Verifica se o email existe no Firestore
                firestore.collection("Users").whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            auth.sendPasswordResetEmail(email)
                                .addOnCompleteListener { enviarEmail ->
                                    if (enviarEmail.isSuccessful) {
                                        showSnack("Confira seu email!", Color.GREEN)
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    val mensagemErro = when (exception) {
                                        is FirebaseNetworkException -> "Sem conexão com a Internet!"
                                        is FirebaseAuthInvalidCredentialsException -> "Digite um email válido!"
                                        else -> "Erro ao enviar o email"
                                    }
                                    showSnack(mensagemErro, Color.RED)
                                }
                        } else {
                            showSnack("Email não cadastrado!", Color.RED)
                        }
                    }
                    .addOnFailureListener {
                        showSnack("Erro ao verificar o email no banco de dados!", Color.RED)
                    }
            }
        }
    }

    private fun showSnack(message: String, color: Int) {
        val view = findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        snackbar.setBackgroundTint(color)
        snackbar.show()
    }
}