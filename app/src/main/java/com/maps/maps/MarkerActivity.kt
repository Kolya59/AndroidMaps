package com.maps.maps

import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_marker.*

class MarkerActivity : AppCompatActivity() {
    private lateinit var image: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_marker)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        this.textView.text = intent.getStringExtra("com.maps.maps.NAME")
        this.imageView.setImageBitmap(image)
    }

    private fun onEdit() {

    }

    private fun onSave() {

    }

    private fun onCancel() {

    }
}
