/**
 * File Adapter for RecyclerView
 * 
 * @author Vũ Nhật Quang
 */
package com.example.filemanager

import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FileAdapter(
    private val items: MutableList<FileItem>,
    private val onItemClick: (FileItem) -> Unit,
    private val onRename: (FileItem) -> Unit,
    private val onDelete: (FileItem) -> Unit,
    private val onCopy: (FileItem) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    private var selectedPosition: Int = -1

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnCreateContextMenuListener {
        val iconImageView: ImageView = itemView.findViewById(R.id.iconImageView)
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        val detailsTextView: TextView = itemView.findViewById(R.id.detailsTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(items[position])
                }
            }

            itemView.setOnCreateContextMenuListener(this)
            
            itemView.setOnLongClickListener {
                selectedPosition = adapterPosition
                false
            }
        }

        override fun onCreateContextMenu(
            menu: ContextMenu,
            v: View,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val fileItem = items[position]
                
                menu.setHeaderTitle(fileItem.name)
                
                menu.add(0, 1, 0, "Đổi tên").setOnMenuItemClickListener {
                    onRename(fileItem)
                    true
                }
                
                menu.add(0, 2, 0, "Xóa").setOnMenuItemClickListener {
                    onDelete(fileItem)
                    true
                }
                
                if (!fileItem.isDirectory) {
                    menu.add(0, 3, 0, "Sao chép").setOnMenuItemClickListener {
                        onCopy(fileItem)
                        true
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val fileItem = items[position]
        
        holder.nameTextView.text = fileItem.name
        
        if (fileItem.isDirectory) {
            holder.iconImageView.setImageResource(R.drawable.ic_folder)
            holder.detailsTextView.text = "Thư mục"
        } else {
            when (fileItem.getFileExtension().lowercase()) {
                "txt" -> holder.iconImageView.setImageResource(R.drawable.ic_text_file)
                "jpg", "jpeg", "png", "bmp" -> holder.iconImageView.setImageResource(R.drawable.ic_image_file)
                else -> holder.iconImageView.setImageResource(R.drawable.ic_file)
            }
            
            val sizeKB = fileItem.size / 1024
            holder.detailsTextView.text = if (sizeKB > 0) "$sizeKB KB" else "${fileItem.size} bytes"
        }
    }

    override fun getItemCount(): Int = items.size
}
