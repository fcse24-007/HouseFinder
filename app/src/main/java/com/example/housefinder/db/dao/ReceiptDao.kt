package com.example.housefinder.db.dao

import androidx.room.*
import com.example.housefinder.db.entities.*
import kotlinx.coroutines.flow.Flow


@Dao
interface ReceiptDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(receipt: Receipt): Long

    @Query("SELECT * FROM receipts WHERE reservationId = :reservationId LIMIT 1")
    suspend fun getForReservation(reservationId: Int): Receipt?
}