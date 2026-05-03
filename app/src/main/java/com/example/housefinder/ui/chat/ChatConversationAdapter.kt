package com.example.housefinder.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.housefinder.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatConversationAdapter(
    private val onClick: (ChatConversationItem) -> Unit
) : ListAdapter<ChatConversationItem, ChatConversationAdapter.ChatConversationViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_conversation, parent, false)
        return ChatConversationViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ChatConversationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChatConversationViewHolder(
        itemView: View,
        private val onClick: (ChatConversationItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val partnerName = itemView.findViewById<TextView>(R.id.txt_partner_name)
        private val listingTitle = itemView.findViewById<TextView>(R.id.txt_listing_title)
        private val lastMessage = itemView.findViewById<TextView>(R.id.txt_last_message)
        private val timestamp = itemView.findViewById<TextView>(R.id.txt_chat_time)
        private var currentItem: ChatConversationItem? = null

        init {
            itemView.setOnClickListener { currentItem?.let(onClick) }
        }

        fun bind(item: ChatConversationItem) {
            currentItem = item
            val timeFormatter = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
            partnerName.text = item.partnerName
            listingTitle.text = itemView.context.getString(R.string.chat_listing_prefix, item.listingTitle)
            lastMessage.text = item.lastMessage
            timestamp.text = timeFormatter.format(Date(item.timestamp))
            val messageColor = if (item.hasUnread) R.color.register_text_dark else R.color.register_text_light
            val messageStyle = if (item.hasUnread) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
            lastMessage.setTextColor(ContextCompat.getColor(itemView.context, messageColor))
            lastMessage.setTypeface(null, messageStyle)
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ChatConversationItem>() {
            override fun areItemsTheSame(oldItem: ChatConversationItem, newItem: ChatConversationItem): Boolean {
                return oldItem.conversationId == newItem.conversationId
            }

            override fun areContentsTheSame(oldItem: ChatConversationItem, newItem: ChatConversationItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
