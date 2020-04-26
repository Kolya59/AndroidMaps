package com.maps.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.LatLng
import com.maps.models.Dot
import com.maps.store.Store
import java.io.*
import java.util.*


open class MarkerActivity : AppCompatActivity() {
    private lateinit var mImageView: ImageView
    private lateinit var mStore: Store
    private lateinit var mDot: Dot

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_marker)

        mStore = Store(this)
        mImageView = findViewById(R.id.imageView)

        val position = intent.getParcelableExtra<LatLng>("key")
        mDot = mStore.findMarker(position)!!
        if (mDot.getURI() != null) { openImage() }

        findViewById<Button>(R.id.buttonCapture)?.setOnClickListener { onClickPhoto() }
        findViewById<Button>(R.id.buttonSave)?.setOnClickListener { onClickSave() }
    }

    private fun onClickPhoto() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    Log.e(getString(R.string.log_tag), "Create tmp file error: $ex")
                    null
                }
                photoFile?.also {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mDot.getURI())
                    startActivity(takePictureIntent)
                }
            }
        }
    }

    private fun onClickSave() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) { mImageView.setImageURI(mDot.getURI()) }
    }

    private fun openImage() {
        mImageView.setImageURI(mDot.getURI())
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${UUID.randomUUID()}_",
            ".jpg",
            storageDir
        ).apply {
            mStore.updateMarkerUri(mDot, Uri.parse(absolutePath))
        }
    }
}