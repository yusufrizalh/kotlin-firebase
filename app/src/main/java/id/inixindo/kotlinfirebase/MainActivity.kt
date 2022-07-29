package id.inixindo.kotlinfirebase

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        val TAG = "RegisterActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button_register.setOnClickListener {
            Log.d(TAG, "Try to register")
            performRegister()
        }

        selectphoto_imageview_register.setOnClickListener {
            Log.d(TAG, "Try to select image view")
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            // proses dan cek bahwa ada image yg diambil dari galeri
            Log.d(TAG, "Image was selected")
            selectedPhotoUri = data.data

            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri)
            selectphoto_imageview_register.setImageBitmap(bitmap)
            selectphoto_imageview_register.alpha = 0f
        }
    }

    private fun performRegister() {
        val email = edittext_email_register.text.toString()
        val username = edittext_username_register.text.toString()
        val password = edittext_password_register.text.toString()

        if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(applicationContext, "Please enter all fields!", Toast.LENGTH_LONG).show()
        }
        Log.d(TAG, "Try to create user with email: $email")

        // Akses Firebase Authentication untuk membuat user baru dengan email dan password
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                // jika gagal
                if (!it.isSuccessful) return@addOnCompleteListener
                // jika sukses
                Log.d(
                    TAG,
                    "Successfully create user with uid: ${it.result.user!!.uid}"
                )
                uploadImageToFirebaseStorage()
            }
            .addOnFailureListener {
                Log.d(TAG, "Failed to crete user: ${it.message}")
                Toast.makeText(
                    applicationContext,
                    "Failed to crete user: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    var selectedPhotoUri: Uri? = null

    private fun uploadImageToFirebaseStorage() {
        if (selectedPhotoUri == null) return

        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")

        ref.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully upload image: ${it.metadata?.path}")
                ref.downloadUrl.addOnSuccessListener {
                    Log.d(TAG, "File location: $it")
                    saveUserToFirebaseDatabase(it.toString())
                }
            }.addOnFailureListener {
                Log.d(TAG, "Failed to upload image to storage: ${it.message}")
                Toast.makeText(
                    applicationContext,
                    "Failed to upload image to storage: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun saveUserToFirebaseDatabase(profileImageUrl: String) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        val user = User(uid, edittext_username_register.text.toString(), profileImageUrl)

        ref.setValue(user).addOnSuccessListener {
            Log.d(TAG, "Successfully save user to database")
        }.addOnFailureListener {
            Log.d(TAG, "Failed to save user to database")
        }
    }
}

class User(val uid: String, val username: String, val profileImageUrl: String)