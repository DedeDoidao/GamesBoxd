package com.example.gamesboxd.ui.conta

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.example.gamesboxd.R
import com.example.gamesboxd.databinding.FragmentContaBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream

class Conta : Fragment() {
    private lateinit var binding: FragmentContaBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var nome: String
    private lateinit var email: String
    private lateinit var user: String
    private lateinit var picture: String
    private lateinit var btnEditar: Button

    private var initialImgUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View {
        binding = FragmentContaBinding.inflate(inflater,container, false)
        val userId = auth.currentUser?.uid

        if (userId != null) {
            firestore.collection("Users").document(userId).addSnapshotListener { documento, error ->

                if(documento != null){
                    nome = documento.getString("nome").toString()
                    email = documento.getString("email").toString()
                    user = documento.getString("id").toString()
                    picture = documento.getString("picture").toString()

                    binding.inputNome.setText(nome)
                    binding.inputEmail.setText(email)
                    binding.inputUser.setText(user)
                    if(picture != null){
                        Glide.with(this).load(picture).into(binding.imageViewFoto)
                        initialImgUri = picture.toUri()
                    }
                }

            }
            btnEditar = binding.btnEditar
            btnEditar.setOnClickListener {

                val nome_Atual = binding.inputNome.text.toString()
                val email_Atual = binding.inputEmail.text.toString()
                val user_Atual = binding.inputUser.text.toString()
                val foto_Atual = (binding.imageViewFoto.drawable as? BitmapDrawable)?.let { drawable ->
                    drawableToUri(drawable, requireContext())
                }

                if(foto_Atual != null && ConfereDados(nome_Atual, email_Atual,
                        user_Atual, foto_Atual)){

                    btnEditar.setBackgroundColor(Color.BLUE)
                }

            }
        }

        return binding.root
    }
    private fun ConfereDados(name: String, e_mail: String, id: String, img: Uri): Boolean{

     if(nome != name || email != e_mail || user != id || initialImgUri != img){
         return true
     } else{
         return false
     }
    }

    fun drawableToUri(drawable: BitmapDrawable, context: Context): Uri? {
        val bitmap = drawable.bitmap
        val file = File(context.cacheDir, "temp_image.jpg")
        try {
            val outStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
            outStream.flush()
            outStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Uri.fromFile(file)
    }

}


