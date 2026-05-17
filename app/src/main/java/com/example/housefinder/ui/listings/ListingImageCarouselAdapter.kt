package com.example.housefinder.ui.listings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.housefinder.R
import com.example.housefinder.db.entities.ListingImage
import com.example.housefinder.ui.common.ListingImageLoader

class ListingImageCarouselAdapter : ListAdapter<ListingImage, ListingImageCarouselAdapter.ImageViewHolder>(
    Diff
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.img_detail_carousel_item)

        fun bind(item: ListingImage) {
            ListingImageLoader.bind(image, item.imagePath)
        }
    }

    private object Diff : DiffUtil.ItemCallback<ListingImage>() {
        override fun areItemsTheSame(oldItem: ListingImage, newItem: ListingImage): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ListingImage, newItem: ListingImage): Boolean =
            oldItem == newItem
    }
}
