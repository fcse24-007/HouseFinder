package com.example.housefinder.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.example.housefinder.db.dao.ListingDao
import com.example.housefinder.db.dao.ReceiptDao
import com.example.housefinder.db.dao.ReservationDao
import com.example.housefinder.db.entities.AppDatabase
import com.example.housefinder.db.entities.Receipt
import com.example.housefinder.db.entities.Reservation
import com.example.housefinder.db.entities.StudentReservationDetails
import kotlinx.coroutines.flow.Flow

import javax.inject.Inject

class ReservationRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val reservationDao: ReservationDao,
    private val listingDao: ListingDao,
    private val receiptDao: ReceiptDao
) {

    suspend fun insert(reservation: Reservation): Long = reservationDao.insert(reservation)

    fun getStudentReservationDetails(studentId: Int): Flow<List<StudentReservationDetails>> =
        reservationDao.getStudentReservationDetails(studentId)

    suspend fun countActiveForListing(listingId: Int): Int =
        reservationDao.countActiveForListing(listingId)

    suspend fun countActiveForStudent(studentId: Int): Int =
        reservationDao.countActiveForStudent(studentId)

    suspend fun getByReference(reference: String): Reservation? =
        reservationDao.getByReference(reference)

    suspend fun createSimulatedReservation(
        studentId: Int,
        listingId: Int,
        paymentAlias: String
    ): BookingResult {
        val referenceNumber = "SIM-${System.currentTimeMillis().toString().takeLast(8)}"
        return try {
            var bookingResult: BookingResult = BookingResult.Error("Payment failed")

            appDatabase.withTransaction {
                val listing = listingDao.getByIdOnce(listingId)
                if (listing == null || listing.status != "AVAILABLE") {
                    bookingResult = BookingResult.ListingUnavailable
                    return@withTransaction
                }

                if (reservationDao.countActiveForStudent(studentId) > 0) {
                    bookingResult = BookingResult.StudentAlreadyReserved
                    return@withTransaction
                }

                val reservationId = reservationDao.insert(
                    Reservation(
                        referenceNumber = referenceNumber,
                        studentId = studentId,
                        listingId = listingId,
                        status = "ACTIVE"
                    )
                ).toInt()

                receiptDao.insert(
                    Receipt(
                        reservationId = reservationId,
                        amountPaid = listing.depositAmount.toFloat(),
                        paymentMethod = "SIMULATED_${paymentAlias.uppercase()}"
                    )
                )

                val updatedRows = listingDao.updateStatusIfCurrent(
                    listingId = listingId,
                    expectedStatus = "AVAILABLE",
                    newStatus = "RESERVED"
                )
                if (updatedRows == 0) {
                    throw IllegalStateException("Listing status changed")
                }

                bookingResult = BookingResult.Success(referenceNumber)
            }

            bookingResult
        } catch (_: SQLiteConstraintException) {
            BookingResult.ListingUnavailable
        } catch (_: IllegalStateException) {
            BookingResult.ListingUnavailable
        }
    }

    sealed class BookingResult {
        data class Success(val referenceNumber: String) : BookingResult()
        object ListingUnavailable : BookingResult()
        object StudentAlreadyReserved : BookingResult()
        data class Error(val message: String) : BookingResult()
    }
}
