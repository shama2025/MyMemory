package com.mashaffer.mymemory

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.storage.storage
import com.mashaffer.mymemory.models.BoardSize
import com.mashaffer.mymemory.utils.BitmapScaler
import com.mashaffer.mymemory.utils.isPermissionGranted
import com.mashaffer.mymemory.utils.requestPermission
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.mashaffer.mymemory.utils.EXTRA_GAME_NAME
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {

    companion object{
        private const val PICK_PHOTO_CODE = 65
        private const val READ_EXTERNAL_PHOTOS_CODE = 248
        private const val READ_PHOTOS_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        private const val MIN_GAME_LENGTH_NAME = 3
        private const val Max_GAME_LENGTH_NAME = 14
    }

    private var numImagesRequired = -1
    private var TAG = "CreateActivity"
    private lateinit var  rvImagePicker: RecyclerView
    private lateinit var imagePickerAdapter: ImagePickerAdapter
    private lateinit var etGameName: EditText
    private lateinit var btnSave: Button
    private val storage = Firebase.storage
    private val db = Firebase.firestore
    private lateinit var pbUploading: ProgressBar

    // A URI, Uniform resource Identifier, identifies where a resource lives
    // Gives a directory path of something, ie a photo
    private val chosenImage = mutableListOf<Uri>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvImagePicker = findViewById(R.id.rvImagePicker)
        etGameName = findViewById(R.id.etGameName)
        btnSave = findViewById(R.id.btnSave)
        pbUploading = findViewById(R.id.pbUploading)

        val bundle = intent.getBundleExtra("myBundle")
        val getExtraBoardSize = bundle?.getString("extra_board_size") as BoardSize
        numImagesRequired = getExtraBoardSize.getNumPairs()

        supportActionBar?.title = "Choose pics( 0 / 12)"

        btnSave.setOnClickListener({
            saveDataToFirebase()
        })
        etGameName.filters = arrayOf(InputFilter.LengthFilter(Max_GAME_LENGTH_NAME))
        etGameName.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(p0: Editable?) {
                btnSave.isEnabled = shouldEnabledSaveButton()
            }
        })

        imagePickerAdapter = ImagePickerAdapter(this, chosenImage, getExtraBoardSize , object:View.OnClickListener {
            override fun onPlaceHolderClicked(){
                // Implicit intents are used to not care which application handles the intent
                // Explicit intents launch other activities in your app
                if(isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION)){
                    launchIntentForPhotos()
                }else{
                    requestPermission(this@CreateActivity, READ_PHOTOS_PERMISSION, READ_EXTERNAL_PHOTOS_CODE)
                }


            }
        })
        rvImagePicker = imagePickerAdapter
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this, getExtraBoardSize.getWidth())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permission:Array<out String>, grantResults:IntArray){
        if(requestCode == READ_EXTERNAL_PHOTOS_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PacketManager.Permission_Granted){
                launchIntentForPhotos()
            }else{
                Toast.makeText(this, "In order to create a custom game, you need to provide access to your photos", Toast.LENGTH_LONG)
            }
        }
        super.onRequestPermissionsResult(requestCode, permission, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode != PICK_PHOTO_CODE || resultCode != Activity.RESULT_OK || data == null){
            Log.i(TAG, "Did not get data back from the launched activity, user likely canceled flow!")
            return
        }
        val selected: Uri? = data.data
        val clipDaata = data.clipData // Will have multiple images
        if(clipDaata!= null){
            Log.i(TAG, "clipData numImages ${clipDaata.itemCount}: $clipDaata}")
            for( i in 0 until clipDaata.itemCount){
                val clipItem = clipDaata.getItemAt(i)
                if(chosenImage < numImagesRequired){
                    chosenImage.add(clipItem.uri)
                }
            }
        }else if(selected != null){
            Log.i(TAG, "data: $selected")
            chosenImage.add((selected))
        }
        imagePickerAdapter.notifyDataSetChanged()
        supportActionBar?.title = "Choose pics {${chosenImage.size}/$numImagesRequired"
        btnSave.isEnabled = shouldEnabledSaveButton()
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun shouldEnabledSaveButton(): Boolean {
        // Checks if button is enabled
        if(chosenImage.size != numImagesRequired || etGameName.text.isBlank() || etGameName.length() < MIN_GAME_LENGTH_NAME){
            return false
        }
        return true
    }

    private fun launchIntentForPhotos(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Choose Pics"), PICK_PHOTO_CODE)
    }

    private fun saveDataToFirebase() {
        Log.i(TAG, "saveDataToFirebase")
        btnSave.isEnabled = false
        val customGameName: String = etGameName.text.toString()
        // Check firestore to make sure game name doesn't exist
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            if (document != null && document.data != null){
                    AlertDialog.Builder(this)
                        .setTitle("Name Taken")
                        .setMessage("A Game Already exists with the name: ${customGameName}")
                        .setPositiveButton("OK", null)
                        .show()
                btnSave.isEnabled = true
                }else{
                    handleImageUploading(customGameName)
            }
        }.addOnFailureListener({
            Toast.makeText(this, "Error Uploading game!", Toast.LENGTH_SHORT).show()
            btnSave.isEnabled = true
        })
    }

    private fun handleImageUploading(customGameName: String) {
        pbUploading.visibility = View.VISIBLE
        var didEncountError : Boolean = false
        val uploadImageUrls: MutableList<String> = mutableListOf<String>()
        for((index, photoUri) in chosenImage.withIndex()){
            val imageBytearray = getImageBytearray(photoUri)
            val filePath = "images/${customGameName}/${System.currentTimeMillis()}-${index}.jpg"
            val photoUriRef: StorageReference = storage.reference.child(filePath)
            photoUriRef.putBytes(imageBytearray).continueWithTask({
                    photoLoadTask -> Log.i(TAG, "Uploaded bytes: ${photoLoadTask.result?.bytesTransferred}")
                photoUriRef.downloadUrl
            }).addOnCompleteListener({ downloadUrlTask ->
                if(!downloadUrlTask.isSuccessful){

                    Toast.makeText(this, "Failed to Upload Image", Toast.LENGTH_SHORT).show()
                    didEncountError = true
                    return@addOnCompleteListener
                }
                if(didEncountError){
                    pbUploading.visibility = View.GONE
                    return@addOnCompleteListener
                }
                val downloadUrl: String = downloadUrlTask.result.toString()
                uploadImageUrls.add(downloadUrl)
                pbUploading.progress = uploadImageUrls.size * 100 / chosenImage.size
                if(uploadImageUrls.size == chosenImage.size){
                    handleAllImagesUploaded(customGameName, uploadImageUrls)
                }
            })

        }
    }

    /**
     * Handles firestore database
     */
    private fun handleAllImagesUploaded(gameName: String, imageUrls: MutableList<String>) {
        db.collection("games").document(gameName)
            .set(mapOf("images" to imageUrls))
            .addOnCompleteListener({ gameCreationTask ->
                if(!gameCreationTask.isSuccessful){
                    Toast.makeText(this, "Failed game creation", Toast.LENGTH_SHORT).show()
                }

                AlertDialog.Builder(this).setTitle("Upload Complete! Let's play your game").setPositiveButton("OK"){ _, _ ->
                    pbUploading.visibility = View.GONE
                    val resultData = Intent()
                    resultData.putExtra(EXTRA_GAME_NAME, gameName)
                    setResult(Activity.RESULT_OK, resultData)
                    finish()
                }
            })
    }

    private fun getImageBytearray(photoUri: Uri): ByteArray {
        val originalBitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            val source: ImageDecoder.Source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        }else{
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }
        val scaledBitmap = BitmapScaler.scaleToFitHeight(originalBitmap, 250)
        val byteArrayOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }


}