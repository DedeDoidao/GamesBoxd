package com.example.gamesboxd.ui.conta

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.example.gamesboxd.R
import com.example.gamesboxd.databinding.FragmentContaBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.FileOutputStream

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

    private var imgUri: Uri? = null
    private var initialImgUri: Uri? = null
    private lateinit var SelecionarImg: ActivityResultLauncher<Intent>

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
                val UsuarioAtual = auth.currentUser

                val nome_Atual = binding.inputNome.text.toString()
                val email_Atual = binding.inputEmail.text.toString()
                val user_Atual = binding.inputUser.text.toString()
                val foto_Atual = (binding.imageViewFoto.drawable as? BitmapDrawable)?.let { drawable ->
                    drawableToUri(drawable, requireContext())
                }

                if(foto_Atual != null && ConfereDados(nome_Atual, email_Atual,
                        user_Atual, foto_Atual)){

                    val imgRef = storageRef.child("profileImages/$userId.jpg")

                    firestore.collection("Users").whereEqualTo("email", email_Atual).get().addOnCompleteListener { VerificarDisponibilidade ->

                        if(VerificarDisponibilidade.isSuccessful){

                            if(VerificarDisponibilidade.result?.documents?.isEmpty() == true){

                                UsuarioAtual.let{
                                    val credential = EmailAuthProvider.getCredential(it?.email!!, "123456")
                                    it.reauthenticate(credential).addOnCompleteListener { reauthenticate ->
                                        if(reauthenticate.isSuccessful){
                                            it.verifyBeforeUpdateEmail(email_Atual).addOnCompleteListener{ task ->
                                                if(task.isSuccessful){
                                                    showSnack("Email de verificação enviado para $email_Atual.", Color.GREEN)
                                                    firestore.collection("Users").document(userId).update("email", email_Atual)
                                                } else {
                                                    showSnack("Erro ao enviar email de verificação: ${task.exception?.message}", Color.RED)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                showSnack("Email já cadastrado!", Color.RED)
                            }
                        }

                    }

                    firestore.collection("Users").document(userId).update("id", user_Atual)
                    firestore.collection("Users").document(userId).update("nome", nome_Atual)

                    imgRef.putFile(foto_Atual).addOnSuccessListener{
                       imgRef.downloadUrl.addOnSuccessListener { uri ->
                           firestore.collection("Users").document(userId).update("picture", uri.toString())
                       }

                    }
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

    private fun ConfereDados(name: String, e_mail: String, id: String, img: Uri): Boolean{
     if(nome != name || email != e_mail || user != id || initialImgUri != img)
         return true else return false
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

}


