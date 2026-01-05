/**
 * File Item Data Class
 * 
 * @author Vũ Nhật Quang
 */
package com.example.filemanager

import java.io.File
import java.io.Serializable

data class FileItem(
    val file: File,
    val name: String = file.name,
    val isDirectory: Boolean = file.isDirectory,
    val path: String = file.absolutePath,
    val size: Long = if (file.isFile) file.length() else 0,
    val lastModified: Long = file.lastModified()
) : Serializable {
    
    fun getFileExtension(): String {
        return if (!isDirectory && name.contains(".")) {
            name.substringAfterLast(".")
        } else ""
    }
    
    fun isTextFile(): Boolean {
        return getFileExtension().lowercase() == "txt"
    }
    
    fun isImageFile(): Boolean {
        val ext = getFileExtension().lowercase()
        return ext in listOf("jpg", "jpeg", "png", "bmp")
    }
}
