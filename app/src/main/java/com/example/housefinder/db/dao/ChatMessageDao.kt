package com.example.housefinder.db.dao

import androidx.room.*
import com.example.housefinder.db.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<ChatMessage>)

    @Query("""
        SELECT * FROM chat_messages
        WHERE conversationId = :conversationId
        ORDER BY timestamp ASC
    """)
    fun getConversation(conversationId: String): Flow<List<ChatMessage>>

    @Query("""
        SELECT * FROM chat_messages
        WHERE (senderId = :userId OR receiverId = :userId)
        GROUP BY conversationId
        ORDER BY timestamp DESC
    """)
    fun getConversationList(userId: Int): Flow<List<ChatMessage>>

    @Query("UPDATE chat_messages SET isRead = 1 WHERE conversationId = :conversationId AND receiverId = :userId")
    suspend fun markConversationRead(conversationId: String, userId: Int)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE receiverId = :userId AND isRead = 0")
    fun getUnreadCount(userId: Int): Flow<Int>

    // Clears a conversation history when needed
    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun clearConversation(conversationId: String)

}