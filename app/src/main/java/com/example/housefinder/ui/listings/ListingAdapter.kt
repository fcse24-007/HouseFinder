package com.example.housefinder.ui.listings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.housefinder.R
import com.example.housefinder.db.entities.AppDatabase
import com.example.housefinder.db.entities.Listing
import com.example.housefinder.ui.common.ListingImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.example.housefinder.db.entities.ListingWithImage

class ListingAdapter(
    private val onClick: (ListingWithImage) -> Unit
) : ListAdapter<ListingWithImage, ListingAdapter.ListingViewHolder>(ListingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_listing, parent, false)
        return ListingViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ListingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ListingViewHolder(
        itemView: View,
        private val onClick: (ListingWithImage) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.txt_listing_title)
        private val location: TextView = itemView.findViewById(R.id.txt_listing_location)
        private val price: TextView = itemView.findViewById(R.id.txt_listing_price)
        private val type: TextView = itemView.findViewById(R.id.txt_listing_type)
        private val coverImage: ImageView = itemView.findViewById(R.id.img_listing_cover)

        fun bind(item: ListingWithImage) {
            val listing = item.listing
            title.text = listing.title
            location.text = listing.location
            price.text = "BWP ${listing.price.toInt()} / month"
            type.text = listing.type

            ListingImageLoader.bind(coverImage, item.coverImagePath)

            itemView.setOnClickListener { onClick(item) }
        }
    }

    private class ListingDiffCallback : DiffUtil.ItemCallback<ListingWithImage>() {
        override fun areItemsTheSame(oldItem: ListingWithImage, newItem: ListingWithImage): Boolean {
            return oldItem.listing.id == newItem.listing.id
        }

        override fun areContentsTheSame(oldItem: ListingWithImage, newItem: ListingWithImage): Boolean {
            return oldItem == newItem
        }
    }
}

