package com.example.housefinder.ui.listings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.housefinder.R
import com.example.housefinder.ui.common.ListingImageLoader
import com.example.housefinder.ui.common.ListingInputOptions
import com.example.housefinder.ui.common.HouseDateFormatter
import com.facebook.shimmer.ShimmerFrameLayout

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
        private val priceOverlay: TextView = itemView.findViewById(R.id.txt_listing_price_overlay)
        private val distance: TextView = itemView.findViewById(R.id.txt_listing_distance)
        private val typeBadge: TextView = itemView.findViewById(R.id.txt_listing_type_badge)
        private val availabilityBadge: TextView = itemView.findViewById(R.id.txt_listing_availability_badge)
        private val statusBadge: TextView = itemView.findViewById(R.id.txt_listing_status_badge)
        private val viewDetailsButton: Button = itemView.findViewById(R.id.btn_view_listing_details)
        private val coverImage: ImageView = itemView.findViewById(R.id.img_listing_cover)
        private val shimmerLayout: ShimmerFrameLayout = itemView.findViewById(R.id.shimmer_listing_cover)

        fun bind(item: ListingWithImage) {
            val listing = item.listing
            title.text = listing.title
            location.text = listing.location
            priceOverlay.text = itemView.context.getString(
                R.string.listing_price_overlay,
                listing.price.toInt()
            )
            distance.text = itemView.context.getString(
                R.string.listing_distance_to_campus,
                listing.distanceToCampusKm
            )
            typeBadge.text = ListingInputOptions.toDisplayType(listing.type)
            availabilityBadge.text = itemView.context.getString(
                R.string.listing_available_badge,
                HouseDateFormatter.toDisplayDate(listing.availabilityDate)
            )
            val isReserved = listing.status == "RESERVED"
            statusBadge.text = if (isReserved) {
                itemView.context.getString(R.string.listing_status_reserved)
            } else {
                itemView.context.getString(R.string.listing_status_available)
            }
            statusBadge.setBackgroundResource(
                if (isReserved) R.drawable.bg_role_pill_inactive else R.drawable.bg_role_pill_active
            )
            statusBadge.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (isReserved) R.color.register_text_dark else R.color.white
                )
            )

            ListingImageLoader.bind(coverImage, item.coverImagePath, shimmerLayout = shimmerLayout)

            itemView.setOnClickListener { onClick(item) }
            viewDetailsButton.setOnClickListener { onClick(item) }
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
