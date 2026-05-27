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
import com.example.housefinder.db.entities.StudentReservationDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyReservationsAdapter(
    private val onChat: (StudentReservationDetails) -> Unit,
    private val onViewReceipt: (StudentReservationDetails) -> Unit
) : ListAdapter<StudentReservationDetails, MyReservationsAdapter.ReservationViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_reservation, parent, false)
        return ReservationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReservationViewHolder, position: Int) {
        holder.bind(getItem(position), onChat, onViewReceipt)
    }

    class ReservationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val listingTitle = itemView.findViewById<TextView>(R.id.txt_reservation_listing_title)
        private val reference = itemView.findViewById<TextView>(R.id.txt_reservation_reference)
        private val status = itemView.findViewById<TextView>(R.id.txt_reservation_status)
        private val details = itemView.findViewById<TextView>(R.id.txt_reservation_details)
        private val reservedAt = itemView.findViewById<TextView>(R.id.txt_reserved_at)
        private val chatButton = itemView.findViewById<View>(R.id.btn_chat_provider)
        private val receiptButton = itemView.findViewById<View>(R.id.btn_view_receipt)

        fun bind(
            item: StudentReservationDetails,
            onChat: (StudentReservationDetails) -> Unit,
            onViewReceipt: (StudentReservationDetails) -> Unit
        ) {
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
            details.text = itemView.context.getString(
                R.string.reservation_details_value,
                item.location,
                item.monthlyRent.toInt(),
                item.providerName
            )
            reservedAt.text = itemView.context.getString(
                R.string.reserved_at_value,
                dateFormatter.format(Date(item.reservedAt))
            )
            chatButton.setOnClickListener { onChat(item) }
            receiptButton.setOnClickListener { onViewReceipt(item) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<StudentReservationDetails>() {
            override fun areItemsTheSame(
                oldItem: StudentReservationDetails,
                newItem: StudentReservationDetails
            ): Boolean = oldItem.reservationId == newItem.reservationId

            override fun areContentsTheSame(
                oldItem: StudentReservationDetails,
                newItem: StudentReservationDetails
            ): Boolean = oldItem == newItem
        }
    }
}
