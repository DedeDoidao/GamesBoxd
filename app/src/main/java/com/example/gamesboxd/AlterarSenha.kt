package com.example.gamesboxd

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthWeakPasswordException


class AlterarSenha : AppCompatActivity() {

    private lateinit var SenhaAtual: EditText
    private lateinit var SenhaNova: EditText
    private lateinit var ConfirmacaoSenhaNova: EditText
    private lateinit var Alterarsenha: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_alterar_senha)

        SenhaAtual = findViewById(R.id.input_Senha_Email)
        SenhaNova = findViewById(R.id.inputSenhaNova)
        ConfirmacaoSenhaNova = findViewById(R.id.inputSenhaConf)
        Alterarsenha = findViewById(R.id.btn_AlterarSenha)

        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Alterarsenha.setOnClickListener {

            if(ConfirmaSenha(SenhaNova.text.toString(), ConfirmacaoSenhaNova.text.toString())){
                val currentUser = auth.currentUser
                val credencial = EmailAuthProvider.getCredential(currentUser?.email!!, SenhaAtual.text.toString())
                currentUser.reauthenticate(credencial).addOnCompleteListener { reautenticar ->

                    if(SenhaNova.text.toString() != SenhaAtual.text.toString()){
                        if(reautenticar.isSuccessful){
                            currentUser.updatePassword(SenhaNova.text.toString()).addOnCompleteListener { alterarSenha ->
                                if(alterarSenha.isSuccessful){
                                    showSnack("Senha atualizada com sucesso!", ContextCompat.getColor(this, R.color.ColorSecundary))
                                }
                            }.addOnFailureListener { exception ->
                                val erro = when (exception) {
                                    is FirebaseAuthWeakPasswordException -> "Digite uma senha de no minimo 6 caracteres!"
                                    is FirebaseNetworkException -> "Sem conexão com a internet!"
                                    else -> "Erro ao cadastrar!"
                                }
                                showSnack(erro, Color.RED)
                            }
                        } else {
                            showSnack("Erro na reautenticação: ${reautenticar.exception?.message}", Color.RED)
                        }
                    } else {
                        showSnack("Senha já registrada!", Color.RED)
                    }

                }
            } else {
                showSnack("As senhas não coincidem!", Color.RED)
            }

        }
    }

    private fun showSnack(message: String, color: Int) {
        val view = findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        snackbar.setBackgroundTint(color)
        snackbar.show()
    }

    private fun ConfirmaSenha(senha: String, confSenha: String): Boolean{
        if (senha == confSenha) return true else return false
    }

}