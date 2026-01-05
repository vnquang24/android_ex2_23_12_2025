/**
 * Image Viewer Activity
 * 
 * @author Vũ Nhật Quang
 */
package com.example.filemanager

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class ImageViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        val filePath = intent.getStringExtra("filePath")
        val imageView: ImageView = findViewById(R.id.imageView)

        filePath?.let {
            val file = File(it)
            supportActionBar?.title = file.name
            
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
