package com.example.housefinder.db.dao

import androidx.room.*
import com.example.housefinder.db.entities.ProviderReservationDetails
import com.example.housefinder.db.entities.Reservation
import com.example.housefinder.db.entities.StudentReservationDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ReservationDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(reservation: Reservation): Long

    @Update
    suspend fun update(reservation: Reservation)

    @Query("SELECT * FROM reservations WHERE studentId = :studentId ORDER BY reservedAt DESC")
    fun getForStudent(studentId: Int): Flow<List<Reservation>>

    @Query("""
        SELECT * FROM reservations
        WHERE studentId = :studentId AND status IN ('PENDING','ACTIVE')
        ORDER BY reservedAt DESC
    """)
    fun getActiveForStudent(studentId: Int): Flow<List<Reservation>>

    // Used by Providers to see who has booked their properties
    @Query("""
        SELECT * FROM reservations 
        WHERE listingId IN (SELECT id FROM listings WHERE providerId = :providerId)
    """)
    fun getForProvider(providerId: Int): Flow<List<Reservation>>

    @Query("SELECT * FROM reservations WHERE referenceNumber = :ref LIMIT 1")
    suspend fun getByReference(ref: String): Reservation?

    // CRITICAL: Checks if a room is already taken before allowing a new booking
    @Query("SELECT COUNT(*) FROM reservations WHERE listingId = :listingId AND status IN ('PENDING','ACTIVE')")
    suspend fun countActiveForListing(listingId: Int): Int

    @Query("SELECT COUNT(*) FROM reservations WHERE studentId = :studentId AND status IN ('PENDING','ACTIVE')")
    suspend fun countActiveForStudent(studentId: Int): Int

    @Query("""
        SELECT COUNT(*) FROM reservations
        WHERE studentId = :studentId AND listingId = :listingId AND status IN ('PENDING','ACTIVE')
    """)
    suspend fun countActiveForStudentAndListing(studentId: Int, listingId: Int): Int

    @Query("""
        SELECT r.id AS reservationId,
               r.referenceNumber,
               r.status,
               r.reservedAt,
               l.id AS listingId,
               l.title AS listingTitle,
               l.location AS location,
               l.price AS monthlyRent,
               u.id AS providerId,
               u.name AS providerName
        FROM reservations r
        INNER JOIN listings l ON l.id = r.listingId
        INNER JOIN users u ON u.id = l.providerId
        WHERE r.studentId = :studentId
        ORDER BY r.reservedAt DESC
    """)
    fun getStudentReservationDetails(studentId: Int): Flow<List<StudentReservationDetails>>

    @Query("""
        SELECT r.id AS reservationId,
               r.referenceNumber,
               r.status,
               r.reservedAt,
               l.id AS listingId,
               l.title AS listingTitle,
               s.id AS studentInternalId,
               s.studentId AS studentIdentifier,
               s.name AS studentName
        FROM reservations r
        INNER JOIN listings l ON l.id = r.listingId
        INNER JOIN users s ON s.id = r.studentId
        WHERE l.providerId = :providerId
        ORDER BY r.reservedAt DESC
    """)
    fun getProviderReservationDetails(providerId: Int): Flow<List<ProviderReservationDetails>>

    @Query("SELECT * FROM reservations WHERE studentId = :studentId AND studentNotified = 0 AND status IN ('PENDING','ACTIVE')")
    fun getPendingStudentNotifications(studentId: Int): Flow<List<Reservation>>

    @Query("""
        SELECT r.* FROM reservations r
        INNER JOIN listings l ON l.id = r.listingId
        WHERE l.providerId = :providerId
        AND r.providerNotified = 0
        AND r.status IN ('PENDING','ACTIVE')
    """)
    fun getPendingProviderNotifications(providerId: Int): Flow<List<Reservation>>

    @Query("UPDATE reservations SET studentNotified = 1 WHERE id = :reservationId")
    suspend fun markStudentNotified(reservationId: Int)

    @Query("UPDATE reservations SET providerNotified = 1 WHERE id = :reservationId")
    suspend fun markProviderNotified(reservationId: Int)

    @Query("UPDATE reservations SET studentNotified = 1 WHERE id IN (:reservationIds)")
    suspend fun markStudentNotifiedBatch(reservationIds: List<Int>)

    @Query("UPDATE reservations SET providerNotified = 1 WHERE id IN (:reservationIds)")
    suspend fun markProviderNotifiedBatch(reservationIds: List<Int>)
}
