package com.example.gamesboxd

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var AbrirEsqueciSenha: TextView
    private lateinit var AbrirCadastrarConta: TextView
    private lateinit var Entrar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        AbrirEsqueciSenha = findViewById(R.id.textView_Password)
        AbrirCadastrarConta = findViewById(R.id.textView_SignUp)
        Entrar = findViewById(R.id.btn_SigIn)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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
}