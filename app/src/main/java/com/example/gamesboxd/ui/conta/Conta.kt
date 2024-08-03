package com.example.gamesboxd.ui.conta

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.example.gamesboxd.AlterarEmail
import com.example.gamesboxd.AlterarSenha
import com.example.gamesboxd.R
import com.example.gamesboxd.databinding.FragmentContaBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class Conta : Fragment() {
    private lateinit var binding: FragmentContaBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var storageRef: StorageReference

    private lateinit var nome: String
    private lateinit var email: String
    private lateinit var user: String
    private lateinit var picture: String
    private lateinit var btnEditar: Button
    private lateinit var imgView: ImageView
    private lateinit var alterarSenha: TextView


    private var imgUri: Uri? = null
    private var initialImgUri: Uri? = null
    private lateinit var SelecionarImg: ActivityResultLauncher<Intent>

    private lateinit var inputEmail: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View {
        binding = FragmentContaBinding.inflate(inflater,container, false)
        val userId = auth.currentUser?.uid

        storageRef = FirebaseStorage.getInstance().reference

        SelecionarImg = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if(result.resultCode == RESULT_OK && result.data != null){
                imgUri = result.data?.data
                Glide.with(this).load(imgUri).into(binding.imageViewFoto)
            }
        }

        imgView = binding.imageViewFoto
        imgView.setOnClickListener{
            ObterImgGaleria()
        }

        alterarSenha = binding.textViewAlterarSenha
        alterarSenha.setOnClickListener{
            val intent = Intent(requireActivity(), AlterarSenha::class.java)
            startActivity(intent)
        }

        inputEmail = binding.inputEmail
        inputEmail.setOnClickListener{
            val intent = Intent(requireActivity(), AlterarEmail::class.java)
            startActivity(intent)
        }

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
                val user_Atual = binding.inputUser.text.toString()
                val foto_Atual = (binding.imageViewFoto.drawable as? BitmapDrawable)?.let { drawable ->
                    drawableToUri(drawable, requireContext())
                }
                val updates = mutableListOf<Pair<String, Any>>()
                var atualizou = false

                val imgRef = storageRef.child("profileImages/$userId.jpg")

                if (user_Atual != user) {
                    AtualizarId(user_Atual) { idDisponivel ->
                        if (!idDisponivel) {
                                showSnack("Id indisponível!", Color.RED)
                            } else {
                                updates.add("id" to user_Atual)
                                atualizou = true
                            }
                        }
                    }

                    if (nome_Atual != nome) {
                        updates.add("nome" to nome_Atual)
                        atualizou = true
                    }

                    if (foto_Atual != null && foto_Atual != initialImgUri) {
                        imgRef.putFile(foto_Atual).addOnSuccessListener {
                            imgRef.downloadUrl.addOnSuccessListener { uri ->
                                updates.add("picture" to uri.toString())
                                atualizou = true
                                AplicarMundancas(userId, updates, atualizou)
                            }
                        }
                    } else {
                        AplicarMundancas(userId, updates, atualizou)
                    }

            }
        }

        return binding.root
    }
    private fun showSnack(message: String, color: Int) {
        val snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT)
        snackbar.setBackgroundTint(color)
        snackbar.show()
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

    fun ObterImgGaleria(){
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        SelecionarImg.launch(intent)
    }

    fun AtualizarId(idNova: String, callback: (Boolean) -> Unit){
        firestore.collection("Users").whereEqualTo("id", idNova)
            .get().addOnCompleteListener { task ->
                if(task.isSuccessful){
                    var idDisponivel = task.result.isEmpty()
                    callback(idDisponivel)
                } else {
                    callback(false)
                }
            }
    }

    fun AplicarMundancas(userId: String, updates: List<Pair<String, Any>>, atualizou: Boolean){
        if(atualizou){
            firestore.collection("Users").document(userId).update(updates.toMap()).addOnCompleteListener { task ->
                if(task.isSuccessful){
                    showSnack("Conta atualizada!", ContextCompat.getColor(requireActivity(), R.color.ColorSecundary))
                } else {
                    showSnack("Erro ao atualizar conta: ${task.exception?.message}", Color.RED)
                }
            }
        } else {
            showSnack("Altere os dados desejados primeiro!", Color.GRAY)
        }
    }

}
