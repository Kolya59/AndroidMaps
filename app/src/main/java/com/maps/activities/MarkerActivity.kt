package com.maps.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


open class MarkerActivity : AppCompatActivity() {
    private lateinit var mImageView: ImageView

    private var mImage: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_marker)

        mImageView = findViewById(R.id.imageView)

        // Загрузка изображения
        mImage = intent.getParcelableExtra(getString(R.string.state_image_key))
        if (mImage != null) {
            openImage()
        }

        // Установка обработчиков событий
        findViewById<Button>(R.id.buttonCapture)?.setOnClickListener { onClickPhoto() }
        findViewById<Button>(R.id.buttonSave)?.setOnClickListener { onClickSave() }
        findViewById<Button>(R.id.buttonCancel)?.setOnClickListener { onClickCancel() }
    }

    // Сохранение состояния
    override fun onSaveInstanceState(outState: Bundle) {
        try {
            outState.putParcelable(getString(R.string.state_image_key), mImage)
            super.onSaveInstanceState(outState)
        } catch (e: Exception) {
            Log.e(getString(R.string.log_tag), "Save state error: ${e.message}")
        }
    }

    // Загрузка сосотяния
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        try {
            mImage = savedInstanceState.getParcelable(getString(R.string.state_image_key))
            if ( mImage != null) {
                openImage()
            }
        } catch (e: Exception) {
            Log.e(getString(R.string.log_tag), "Restore state error: ${e.message}")
        }
    }

    // Обработчик нажатия на кнопку фото
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
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImage)
                    startActivityForResult(takePictureIntent, resources.getInteger(R.integer.image_req_code))
                }
            }
        }
    }

    // Обработчик нажатия на кнопку сохранить
    private fun onClickSave() {
        val output = Intent()
        if (mImage != null) {
            output.putExtra(getString(R.string.state_image_key), mImage)
        }
        setResult(Activity.RESULT_OK, output)
        finish()
    }

    private fun onClickCancel() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    // Обработчик результа активности с фото
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == resources.getInteger(R.integer.image_req_code) &&
            resultCode == Activity.RESULT_OK) {
            mImageView.setImageURI(mImage)
        }
    }

    // Показ фото на экране
    private fun openImage() { mImageView.setImageURI(mImage) }

    // Создание файла для фото
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${UUID.randomUUID()}_",
            ".jpg",
            storageDir
        ).apply {
            mImage = absoluteFile.toURI() as Uri?
        }
    }
}