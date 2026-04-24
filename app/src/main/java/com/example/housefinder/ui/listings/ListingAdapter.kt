package com.example.housefinder.ui.listings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.housefinder.R
import com.example.housefinder.db.entities.Listing

class ListingAdapter(
    private val onClick: (Listing) -> Unit
) : RecyclerView.Adapter<ListingAdapter.ListingViewHolder>() {

    private val items = mutableListOf<Listing>()

    fun submitList(newItems: List<Listing>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_listing, parent, false)
        return ListingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListingViewHolder, position: Int) {
        holder.bind(items[position], onClick)
    }

    override fun getItemCount(): Int = items.size

    class ListingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.txt_title)
        private val location: TextView = itemView.findViewById(R.id.txt_location)
        private val price: TextView = itemView.findViewById(R.id.txt_price)
        private val availability: TextView = itemView.findViewById(R.id.txt_availability)

        fun bind(item: Listing, onClick: (Listing) -> Unit) {
            title.text = item.title
            location.text = item.location
            price.text = "BWP ${item.price.toInt()} / month"
            availability.text = "Available: ${item.availabilityDate}"
            itemView.setOnClickListener { onClick(item) }
        }
    }
}

