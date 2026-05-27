package com.example.housefinder.data.repository

import com.example.housefinder.db.dao.UserDao
import com.example.housefinder.db.entities.User

import javax.inject.Inject

class UserRepository @Inject constructor(private val userDao: UserDao) {

    suspend fun insert(user: User): Long = userDao.insert(user)

    suspend fun getByEmail(email: String): User? = userDao.getByEmail(email)

    suspend fun getByStudentId(studentId: String): User? = userDao.getByStudentId(studentId)

    suspend fun getLatestProviderIdentifier(): String? = userDao.getLatestProviderIdentifier()

    suspend fun getById(id: Int): User? = userDao.getById(id)

    suspend fun countByRole(role: String): Int = userDao.countByRole(role)
}
