package com.example.housefinder.data.repository

import com.example.housefinder.db.dao.ChatMessageDao
import com.example.housefinder.db.entities.ChatMessage
import kotlinx.coroutines.flow.Flow

import javax.inject.Inject

class ChatRepository @Inject constructor(private val chatMessageDao: ChatMessageDao) {

    fun getConversationForUser(conversationId: String, userId: Int): Flow<List<ChatMessage>> =
        chatMessageDao.getConversationForUser(conversationId, userId)

    fun getConversationList(userId: Int): Flow<List<ChatMessage>> =
        chatMessageDao.getConversationList(userId)

    suspend fun insert(message: ChatMessage) = chatMessageDao.insert(message)

    suspend fun markConversationRead(conversationId: String, userId: Int) =
        chatMessageDao.markConversationRead(conversationId, userId)

    fun getUnreadCount(userId: Int): Flow<Int> = chatMessageDao.getUnreadCount(userId)
}
