package com.miahina.ongekimai

import android.graphics.Bitmap
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ImagePagerAdapter(
    private val images: List<Bitmap>
) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    class ImageViewHolder(
        val imageView: ImageView
    ) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageViewHolder {

        val imageView = ImageView(parent.context)

        imageView.layoutParams =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

        imageView.adjustViewBounds = true
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER

        return ImageViewHolder(imageView)
    }

    override fun onBindViewHolder(
        holder: ImageViewHolder,
        position: Int
    ) {
        holder.imageView.setImageBitmap(images[position])
    }

    override fun getItemCount(): Int {
        return images.size
    }
}