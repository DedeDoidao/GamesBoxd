package com.example.gamesboxd.ui.conta

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.gamesboxd.R
import com.example.gamesboxd.databinding.FragmentContaBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Conta : Fragment() {
    private lateinit var binding: FragmentContaBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var teste: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View {
        binding = FragmentContaBinding.inflate(inflater,container, false)
        val userId = auth.currentUser?.uid

        if (userId != null) {
            firestore.collection("Users").document(userId).addSnapshotListener { documento, error ->

                if(documento != null){
                    val nome = documento.getString("nome")
                    val email = documento.getString("email")

                    binding.inputNome.setText(nome)
                    binding.inputEmail.setText(email)
                }

            }
        }



        return binding.root
    }
}