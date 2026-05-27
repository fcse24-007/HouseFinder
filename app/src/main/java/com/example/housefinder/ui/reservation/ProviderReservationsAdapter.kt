package com.example.housefinder.ui.reservation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.housefinder.R
import com.example.housefinder.db.entities.ProviderReservationDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProviderReservationsAdapter(
    private val onChat: (ProviderReservationDetails) -> Unit,
    private val onViewReceipt: (ProviderReservationDetails) -> Unit
) : ListAdapter<ProviderReservationDetails, ProviderReservationsAdapter.ReservationViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_provider_reservation, parent, false)
        return ReservationViewHolder(view, onChat, onViewReceipt)
    }

    override fun onBindViewHolder(holder: ReservationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReservationViewHolder(
        itemView: View,
        private val onChat: (ProviderReservationDetails) -> Unit,
        private val onViewReceipt: (ProviderReservationDetails) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val listingTitle = itemView.findViewById<TextView>(R.id.txt_provider_listing_title)
        private val reference = itemView.findViewById<TextView>(R.id.txt_provider_reference)
        private val status = itemView.findViewById<TextView>(R.id.txt_provider_status)
        private val student = itemView.findViewById<TextView>(R.id.txt_provider_student)
        private val reservedAt = itemView.findViewById<TextView>(R.id.txt_provider_reserved_at)
        private val chatButton = itemView.findViewById<View>(R.id.btn_chat_student)
        private val receiptButton = itemView.findViewById<View>(R.id.btn_view_receipt_provider)
        private var currentItem: ProviderReservationDetails? = null

        init {
            chatButton.setOnClickListener { currentItem?.let(onChat) }
            receiptButton.setOnClickListener { currentItem?.let(onViewReceipt) }
        }

        fun bind(item: ProviderReservationDetails) {
            currentItem = item
            val dateFormatter = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
            listingTitle.text = item.listingTitle
            reference.text = itemView.context.getString(R.string.reservation_reference_value, item.referenceNumber)
            val statusLabel = when (item.status.uppercase()) {
                "ACTIVE" -> itemView.context.getString(R.string.reservation_status_active)
                "PENDING" -> itemView.context.getString(R.string.reservation_status_pending)
                else -> item.status
            }
            val isActive = item.status.equals("ACTIVE", ignoreCase = true)
            status.text = statusLabel
            status.setBackgroundResource(
                if (isActive) R.drawable.bg_role_pill_active else R.drawable.bg_role_pill_inactive
            )
            status.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (isActive) R.color.white else R.color.register_text_dark
                )
            )
            student.text = itemView.context.getString(
                R.string.provider_reservation_student_value,
                item.studentName,
                item.studentIdentifier
            )
            reservedAt.text = itemView.context.getString(
                R.string.reserved_at_value,
                dateFormatter.format(Date(item.reservedAt))
            )
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ProviderReservationDetails>() {
            override fun areItemsTheSame(
                oldItem: ProviderReservationDetails,
                newItem: ProviderReservationDetails
            ): Boolean = oldItem.reservationId == newItem.reservationId

            override fun areContentsTheSame(
                oldItem: ProviderReservationDetails,
                newItem: ProviderReservationDetails
            ): Boolean = oldItem == newItem
        }
    }
}
