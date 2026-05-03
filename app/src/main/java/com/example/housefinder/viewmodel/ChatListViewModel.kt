package com.example.housefinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.housefinder.data.repository.ChatRepository
import com.example.housefinder.data.repository.ListingRepository
import com.example.housefinder.data.repository.UserRepository
import com.example.housefinder.ui.chat.ChatConversationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val listingRepository: ListingRepository
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<ChatConversationItem>>(emptyList())
    val conversations: StateFlow<List<ChatConversationItem>> = _conversations

    fun loadConversations(currentUserId: Int) {
        viewModelScope.launch {
            chatRepository.getConversationList(currentUserId).collectLatest { latestMessages ->
                val rows = withContext(Dispatchers.IO) {
                    latestMessages.map { message ->
                        val partnerId = if (message.senderId == currentUserId) message.receiverId else message.senderId
                        val partnerName = userRepository.getById(partnerId)?.name ?: "Unknown User"
                        val listingTitle = listingRepository.getByIdOnce(message.listingId)?.title ?: "Unknown Listing"
                        
                        ChatConversationItem(
                            conversationId = message.conversationId,
                            listingId = message.listingId,
                            partnerId = partnerId,
                            partnerName = partnerName,
                            listingTitle = listingTitle,
                            lastMessage = message.message,
                            timestamp = message.timestamp,
                            hasUnread = message.receiverId == currentUserId && !message.isRead
                        )
                    }
                }
                _conversations.value = rows.sortedByDescending { it.timestamp }
            }
        }
    }
}
