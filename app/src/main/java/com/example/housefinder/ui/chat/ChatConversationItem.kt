package com.example.housefinder.ui.chat

data class ChatConversationItem(
    val conversationId: String,
    val listingId: Int,
    val partnerId: Int,
    val partnerName: String,
    val listingTitle: String,
    val lastMessage: String,
    val timestamp: Long,
    val hasUnread: Boolean
)
