package com.example.housefinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.housefinder.data.repository.ChatRepository
import com.example.housefinder.data.repository.ListingRepository
import com.example.housefinder.data.repository.UserRepository
import com.example.housefinder.db.entities.ChatMessage
import com.example.housefinder.db.entities.conversationIdFor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChatThreadViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val listingRepository: ListingRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _partnerName = MutableStateFlow("")
    val partnerName: StateFlow<String> = _partnerName

    private val _listingTitle = MutableStateFlow("")
    val listingTitle: StateFlow<String> = _listingTitle

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    fun loadThread(conversationId: String, partnerId: Int, listingId: Int, currentUserId: Int) {
        viewModelScope.launch {
            val listing = listingRepository.getByIdOnce(listingId)
            if (listing == null) {
                _error.emit("Listing no longer exists")
                return@launch
            }

            val expectedConversationId = conversationIdFor(currentUserId, partnerId, listingId)
            if (conversationId != expectedConversationId) {
                _error.emit("Invalid conversation")
                return@launch
            }

            val isProviderParticipant = currentUserId == listing.providerId || partnerId == listing.providerId
            if (!isProviderParticipant || currentUserId == partnerId) {
                _error.emit("Unauthorized conversation")
                return@launch
            }

            _partnerName.value = userRepository.getById(partnerId)?.name ?: "Unknown User"
            _listingTitle.value = listing.title

            chatRepository.getConversationForUser(conversationId, currentUserId).collectLatest {
                _messages.value = it
                chatRepository.markConversationRead(conversationId, currentUserId)
            }
        }
    }

    fun sendMessage(currentUserId: Int, partnerId: Int, listingId: Int, conversationId: String, text: String) {
        viewModelScope.launch {
            val listing = listingRepository.getByIdOnce(listingId)
            if (listing == null) {
                _error.emit("Listing no longer exists")
                return@launch
            }

            val expectedConversationId = conversationIdFor(currentUserId, partnerId, listingId)
            val isProviderParticipant = currentUserId == listing.providerId || partnerId == listing.providerId
            if (conversationId != expectedConversationId || !isProviderParticipant || currentUserId == partnerId) {
                _error.emit("Unauthorized conversation")
                return@launch
            }

            chatRepository.insert(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    senderId = currentUserId,
                    receiverId = partnerId,
                    listingId = listingId,
                    message = text,
                    isRead = false
                )
            )
        }
    }
}
