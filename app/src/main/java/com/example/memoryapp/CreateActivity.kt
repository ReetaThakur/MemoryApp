package com.example.memoryapp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memoryapp.models.BoardSize
import com.example.memoryapp.utils.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {
    private lateinit var btnSave: Button
    private lateinit var etGameName: EditText
    private lateinit var rvImagePicker: RecyclerView
    private lateinit var boardSize: BoardSize
    private var numImageRequired = -1
    private lateinit var imagePickerAdapter: ImagePickerAdapter
    private val chosenImageUri = mutableListOf<Uri>()
    private val storage = Firebase.storage
    private val db = Firebase.firestore
    private lateinit var pbUploading:ProgressBar

    companion object {
        private val TAG = "CreateActivity"
        private const val PICK_PHOTO_CODE = 655
        private const val READ_EXTERNAL_PHOTOS_CODE = 21
        private const val READ_PHOTOS_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
        private const val MIN_GAME_LENGTH = 3
        private const val MAX_GAME_LENGTH = 14
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)
        btnSave = findViewById(R.id.btnSave)
        etGameName = findViewById(R.id.etGameName)
        pbUploading=findViewById(R.id.pbUploading)
        rvImagePicker = findViewById(R.id.imagePickerrecyclerView)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numImageRequired = boardSize.getNumPairs()
        supportActionBar?.title = "Choose pics(0 /$numImageRequired)"

        btnSave.setOnClickListener {
            saveDataToFirebase()
        }

        etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_LENGTH))
        etGameName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                btnSave.isEnabled = shouldEnableSaveButton()
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {}

        })


        imagePickerAdapter = ImagePickerAdapter(this,
            chosenImageUri,
            boardSize,
            object : ImagePickerAdapter.ImageClickListner {
                override fun onPlaceHolderClicked() {
                    if (isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION)) {
                        launchIntentForPhoots()
                    } else {
                        requestPermission(this@CreateActivity, READ_PHOTOS_PERMISSION,
                            READ_EXTERNAL_PHOTOS_CODE)
                    }

                }

            })
        rvImagePicker.adapter = imagePickerAdapter
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }

    private fun saveDataToFirebase() {
        Log.i(TAG,"saveDataToFirebase")
        btnSave.isEnabled=false
        val customGameName = etGameName.text.toString()

        //check that we're not over writing someone else''s data
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            if (document != null && document.data != null) {
                AlertDialog.Builder(this).setTitle("Name Taken")
                    .setMessage("A game already exists with this name '$customGameName'. Please choose another")
                    .setPositiveButton("OK",null)
                    .show()
                btnSave.isEnabled=true
            } else {
                handleImageUploading(customGameName)
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG,"Encounter error while saving memory game",exception)
            Toast.makeText(this,"Encounter error while saving memory game",Toast.LENGTH_SHORT).show()
            btnSave.isEnabled=true
        }
    }

    private fun handleImageUploading(gameName: String) {
        pbUploading.visibility= View.VISIBLE
        Log.i(TAG, "saveDataToFirebase")
        var didEncounterError = false
        val uploadImageUrls = mutableListOf<String>()
        for ((index, photoUri) in chosenImageUri.withIndex()) {
            val imageByteArray = getImageByteArray(photoUri)
            val filePath = "image/$gameName/${System.currentTimeMillis()}-${index}.jpg"
            val photoReference = storage.reference.child(filePath)
            photoReference.putBytes(imageByteArray).continueWithTask { photoUploadTask ->
                Log.i(CreateActivity.TAG,
                    "Upload bytes: ${photoUploadTask.result?.bytesTransferred}")
                photoReference.downloadUrl
            }.addOnCompleteListener { downloadUriTask ->
                if (!downloadUriTask.isSuccessful) {
                    Log.e(CreateActivity.TAG,
                        "Exception with firebase storage",
                        downloadUriTask.exception)
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                    didEncounterError = true
                    return@addOnCompleteListener
                }
                if (didEncounterError) {
                    pbUploading.visibility=View.GONE
                    return@addOnCompleteListener
                }
                val downloadUrls = downloadUriTask.result.toString()
                uploadImageUrls.add(downloadUrls)
                pbUploading.progress=uploadImageUrls.size * 100 /chosenImageUri.size
                Log.i(CreateActivity.TAG,
                    "Finished uploading $photoUri,num upload ${uploadImageUrls.size}")
                if (uploadImageUrls.size == chosenImageUri.size) {
                    handleAllImagesUpload(gameName, uploadImageUrls)
                }
            }
        }
    }

     private   fun handleAllImagesUpload(gameName: String, imageUri: MutableList<String>) {
            db.collection("games").document(gameName).set(mapOf("images" to imageUri))
                .addOnCompleteListener { gameCreationTask ->
                    pbUploading.visibility = View.GONE
                    if (!gameCreationTask.isSuccessful) {
                        Log.e(TAG, "Exception with game creation", gameCreationTask.exception)
                        Toast.makeText(this, "Failed game creation", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener

                    }
                }

            Log.i(TAG, "Successfully created game $gameName")
            AlertDialog.Builder(this).setTitle("Upload complete! Let's play your game $gameName")
                .setPositiveButton("OK") { _, _ ->
                    val resultData = Intent()
                    resultData.putExtra(EXTRA_GAME_NAME, gameName)
                    setResult(Activity.RESULT_OK, resultData)
                    finish()
                }.show()

        }


      private  fun getImageByteArray(photoUri: Uri): ByteArray {
            val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, photoUri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
            }
            Log.i(TAG, "Original width ${originalBitmap.width} and heigth ${originalBitmap.height}")
            val scaleBitmap = BitmapScaler.scaleToFitHeigth(originalBitmap, 250)
            Log.i(TAG, "Scale width ${scaleBitmap.width} and heigth ${scaleBitmap.height}")
            val byteOutputStream = ByteArrayOutputStream()
            scaleBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteOutputStream)
            return byteOutputStream.toByteArray()

        }

        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ) {
            if (requestCode == READ_EXTERNAL_PHOTOS_CODE) {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launchIntentForPhoots()
                } else {
                    Toast.makeText(this,
                        "In order to create a custom game, you need to provide access to your photos",
                        Toast.LENGTH_SHORT).show()
                }
            }
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }


        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            if (item.itemId == android.R.id.home) {
                finish()
                return true
            }
            return super.onOptionsItemSelected(item)
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode != PICK_PHOTO_CODE || resultCode != Activity.RESULT_OK || data == null) {
                Log.w(TAG, "Did not get data from the launched activity, user likely canceled flow")
                return
            }
            val selectedUri = data.data
            val clipData = data.clipData
            if (clipData != null) {
                Log.i(TAG, "clipData numImage${clipData.itemCount}: $clipData")
                for (i in 0 until clipData.itemCount) {
                    val clipItem = clipData.getItemAt(i)
                    if (chosenImageUri.size > numImageRequired) {
                        chosenImageUri.add(clipItem.uri)
                    }
                }
            } else if (selectedUri != null) {
                Log.i(TAG, "data:$selectedUri")
                chosenImageUri.add(selectedUri)
            }
            imagePickerAdapter.notifyDataSetChanged()
            supportActionBar?.title = "Choose pics(${chosenImageUri.size}/$numImageRequired)"

            //for enabling the save button
            btnSave.isEnabled = shouldEnableSaveButton()
        }

      private  fun shouldEnableSaveButton(): Boolean {
            if (chosenImageUri.size != numImageRequired) {
                return false
            }
            if (etGameName.text.isBlank() || etGameName.text.length < MIN_GAME_LENGTH) {
                return false
            }
            return true
        }

        //for picking the images from phone
      private  fun launchIntentForPhoots() {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(Intent.createChooser(intent, "Choose pics"), PICK_PHOTO_CODE)
        }
    }

