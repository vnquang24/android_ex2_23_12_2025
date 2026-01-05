/**
 * Text Viewer Activity
 * 
 * @author Vũ Nhật Quang
 */
package com.example.filemanager

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class TextViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_viewer)

        val filePath = intent.getStringExtra("filePath")
        val textView: TextView = findViewById(R.id.textContentView)

        filePath?.let {
            val file = File(it)
            supportActionBar?.title = file.name
            
            try {
                val content = file.readText()
                textView.text = content
            } catch (e: Exception) {
                textView.text = "Không thể đọc file: ${e.message}"
            }
        }
    }
}
