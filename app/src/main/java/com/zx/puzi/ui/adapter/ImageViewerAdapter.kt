package com.zx.puzi.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.zx.puzi.R

class ImageViewerAdapter(
    private val imageUrls: List<String>,
    private val onImageClick: () -> Unit
) : RecyclerView.Adapter<ImageViewerAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photoView: PhotoView = view.findViewById(R.id.photoView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_viewer, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        Glide.with(holder.photoView)
            .load(imageUrls[position])
            .into(holder.photoView)

        holder.photoView.setOnClickListener {
            onImageClick()
        }
    }

    override fun getItemCount(): Int = imageUrls.size
}
