/**
 * File Manager Activity
 * 
 * @author Vũ Nhật Quang
 */
package com.example.filemanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FileManagerActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FileAdapter
    private lateinit var toolbar: Toolbar
    private lateinit var btnBack: ImageButton
    private var currentDirectory: File? = null
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_manager)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            navigateToParentDirectory()
        }

        recyclerView = findViewById(R.id.recyclerViewFiles)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Kiểm tra quyền truy cập storage
        if (checkStoragePermission()) {
            initializeFileManager()
        } else {
            requestStoragePermission()
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val read = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            )
            val write = ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, PERMISSION_REQUEST_CODE)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(intent, PERMISSION_REQUEST_CODE)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeFileManager()
            } else {
                Toast.makeText(this, "Cần cấp quyền truy cập storage", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (checkStoragePermission()) {
                initializeFileManager()
            } else {
                Toast.makeText(this, "Cần cấp quyền truy cập storage", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initializeFileManager() {
        currentDirectory = Environment.getExternalStorageDirectory()
        loadFiles()
    }

    private fun loadFiles() {
        currentDirectory?.let { dir ->
            supportActionBar?.title = dir.name
            
            // Hiển thị/ẩn nút quay lại
            val parentDir = dir.parentFile
            val isRootDirectory = parentDir == null || 
                (dir.absolutePath == Environment.getExternalStorageDirectory().absolutePath)
            btnBack.visibility = if (isRootDirectory) View.GONE else View.VISIBLE
            
            val files = dir.listFiles()?.map { FileItem(it) } ?: emptyList()
            val sortedFiles = files.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
            
            adapter = FileAdapter(
                items = sortedFiles.toMutableList(),
                onItemClick = { fileItem -> handleFileClick(fileItem) },
                onRename = { fileItem -> showRenameDialog(fileItem) },
                onDelete = { fileItem -> showDeleteDialog(fileItem) },
                onCopy = { fileItem -> showCopyDialog(fileItem) }
            )
            
            recyclerView.adapter = adapter
        }
    }

    private fun navigateToParentDirectory() {
        currentDirectory?.parentFile?.let { parent ->
            if (parent.canRead()) {
                currentDirectory = parent
                loadFiles()
            } else {
                Toast.makeText(this, "Không thể truy cập thư mục cha", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Đã ở thư mục gốc", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleFileClick(fileItem: FileItem) {
        if (fileItem.isDirectory) {
            currentDirectory = fileItem.file
            loadFiles()
        } else {
            if (fileItem.isTextFile()) {
                openTextFile(fileItem)
            } else if (fileItem.isImageFile()) {
                openImageFile(fileItem)
            } else {
                Toast.makeText(this, "Không hỗ trợ định dạng file này", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openTextFile(fileItem: FileItem) {
        val intent = Intent(this, TextViewerActivity::class.java)
        intent.putExtra("filePath", fileItem.path)
        startActivity(intent)
    }

    private fun openImageFile(fileItem: FileItem) {
        val intent = Intent(this, ImageViewerActivity::class.java)
        intent.putExtra("filePath", fileItem.path)
        startActivity(intent)
    }

    private fun showRenameDialog(fileItem: FileItem) {
        val input = android.widget.EditText(this)
        input.setText(fileItem.name)
        
        AlertDialog.Builder(this)
            .setTitle("Đổi tên")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val newName = input.text.toString()
                if (newName.isNotEmpty()) {
                    renameFile(fileItem, newName)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun renameFile(fileItem: FileItem, newName: String) {
        val newFile = File(fileItem.file.parent, newName)
        if (fileItem.file.renameTo(newFile)) {
            Toast.makeText(this, "Đổi tên thành công", Toast.LENGTH_SHORT).show()
            loadFiles()
        } else {
            Toast.makeText(this, "Đổi tên thất bại", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteDialog(fileItem: FileItem) {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc muốn xóa ${fileItem.name}?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteFile(fileItem)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteFile(fileItem: FileItem) {
        if (fileItem.file.deleteRecursively()) {
            Toast.makeText(this, "Xóa thành công", Toast.LENGTH_SHORT).show()
            loadFiles()
        } else {
            Toast.makeText(this, "Xóa thất bại", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCopyDialog(fileItem: FileItem) {
        val input = android.widget.EditText(this)
        input.hint = "Nhập đường dẫn thư mục đích"
        
        AlertDialog.Builder(this)
            .setTitle("Sao chép file")
            .setView(input)
            .setPositiveButton("Sao chép") { _, _ ->
                val destPath = input.text.toString()
                if (destPath.isNotEmpty()) {
                    copyFile(fileItem, destPath)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun copyFile(fileItem: FileItem, destPath: String) {
        try {
            val destDir = File(destPath)
            if (!destDir.exists()) {
                destDir.mkdirs()
            }
            
            val destFile = File(destDir, fileItem.name)
            fileItem.file.copyTo(destFile, overwrite = true)
            
            Toast.makeText(this, "Sao chép thành công", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Sao chép thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_file_manager, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_new_folder -> {
                showCreateFolderDialog()
                true
            }
            R.id.action_new_file -> {
                showCreateFileDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showCreateFolderDialog() {
        val input = android.widget.EditText(this)
        input.hint = "Tên thư mục"
        
        AlertDialog.Builder(this)
            .setTitle("Tạo thư mục mới")
            .setView(input)
            .setPositiveButton("Tạo") { _, _ ->
                val folderName = input.text.toString()
                if (folderName.isNotEmpty()) {
                    createFolder(folderName)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun createFolder(folderName: String) {
        currentDirectory?.let { dir ->
            val newFolder = File(dir, folderName)
            if (newFolder.mkdir()) {
                Toast.makeText(this, "Tạo thư mục thành công", Toast.LENGTH_SHORT).show()
                loadFiles()
            } else {
                Toast.makeText(this, "Tạo thư mục thất bại", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCreateFileDialog() {
        val input = android.widget.EditText(this)
        input.hint = "Tên file (ví dụ: file.txt)"
        
        AlertDialog.Builder(this)
            .setTitle("Tạo file văn bản mới")
            .setView(input)
            .setPositiveButton("Tạo") { _, _ ->
                val fileName = input.text.toString()
                if (fileName.isNotEmpty()) {
                    createTextFile(fileName)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun createTextFile(fileName: String) {
        currentDirectory?.let { dir ->
            val newFile = File(dir, fileName)
            try {
                if (newFile.createNewFile()) {
                    Toast.makeText(this, "Tạo file thành công", Toast.LENGTH_SHORT).show()
                    loadFiles()
                } else {
                    Toast.makeText(this, "File đã tồn tại", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Tạo file thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        currentDirectory?.let { dir ->
            val isRootDirectory = dir.absolutePath == Environment.getExternalStorageDirectory().absolutePath
            if (isRootDirectory) {
                super.onBackPressed()
            } else {
                navigateToParentDirectory()
            }
        } ?: super.onBackPressed()
    }
}
