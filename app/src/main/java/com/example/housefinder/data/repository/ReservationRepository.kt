package com.example.housefinder.data.repository

import com.example.housefinder.db.dao.ReservationDao
import com.example.housefinder.db.entities.Reservation
import com.example.housefinder.db.entities.StudentReservationDetails
import kotlinx.coroutines.flow.Flow

import javax.inject.Inject

class ReservationRepository @Inject constructor(private val reservationDao: ReservationDao) {

    suspend fun insert(reservation: Reservation): Long = reservationDao.insert(reservation)

    fun getStudentReservationDetails(studentId: Int): Flow<List<StudentReservationDetails>> =
        reservationDao.getStudentReservationDetails(studentId)

    suspend fun countActiveForListing(listingId: Int): Int =
        reservationDao.countActiveForListing(listingId)

    suspend fun countActiveForStudent(studentId: Int): Int =
        reservationDao.countActiveForStudent(studentId)
}
