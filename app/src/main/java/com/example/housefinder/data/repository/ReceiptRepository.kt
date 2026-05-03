package com.example.housefinder.data.repository

import com.example.housefinder.db.dao.ReceiptDao
import com.example.housefinder.db.entities.Receipt

import javax.inject.Inject

class ReceiptRepository @Inject constructor(private val receiptDao: ReceiptDao) {

    suspend fun insert(receipt: Receipt): Long = receiptDao.insert(receipt)

    suspend fun getForReservation(reservationId: Int): Receipt? = receiptDao.getForReservation(reservationId)
}
