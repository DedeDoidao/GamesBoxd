package com.example.gamesboxd

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar

class AlterarSenha : AppCompatActivity() {

    private lateinit var Senha: EditText
    private lateinit var ConfirmacaoSenha: EditText
    private lateinit var Alterarsenha: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_alterar_senha)
        Senha = findViewById(R.id.inputSenha)
        ConfirmacaoSenha = findViewById(R.id.inputSenhaConf)
        Alterarsenha = findViewById(R.id.btn_AlterarSenha)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showSnack(message: String, color: Int) {
        val view = findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        snackbar.setBackgroundTint(color)
        snackbar.show()
    }

    private fun ConfirmaSenha(senha: String, confSenha: String): Boolean{
        if (senha == confSenha){
            VerificaSenhaLogin(senha)
        }
        return false
    }

    private fun VerificaSenhaLogin(senha: String){

    }

}