package com.example.housefinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.housefinder.data.repository.UserRepository
import com.example.housefinder.db.entities.User
import com.example.housefinder.db.entities.hashPassword
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(private val userRepository: UserRepository) : ViewModel() {

    private val _registrationResult = MutableSharedFlow<RegistrationResult>()
    val registrationResult = _registrationResult.asSharedFlow()

    fun register(
        name: String,
        email: String,
        password: String,
        role: String,
        studentIdValue: String,
        university: String
    ) {
        viewModelScope.launch {
            try {
                // Pre-check for duplicate email
                val existingEmailUser = userRepository.getByEmail(email)
                if (existingEmailUser != null) {
                    _registrationResult.emit(RegistrationResult.Error("Email already registered"))
                    return@launch
                }

                if (role == "STUDENT") {
                    // No registration cap for students.
                }

                val generatedStudentId = if (role == "PROVIDER") {
                    generateNextProviderId()
                } else {
                    // Check for duplicate student ID
                    val existingStudentUser = userRepository.getByStudentId(studentIdValue)
                    if (existingStudentUser != null) {
                        _registrationResult.emit(RegistrationResult.Error("Student ID already registered"))
                        return@launch
                    }
                    studentIdValue
                }

                val user = User(
                    studentId = generatedStudentId,
                    name = name,
                    email = email,
                    passwordHash = hashPassword(password),
                    role = role,
                    university = if (role == "STUDENT") university else "N/A"
                )

                val resultId = userRepository.insert(user)
                if (resultId <= 0) {
                    _registrationResult.emit(RegistrationResult.Error("Registration failed"))
                    return@launch
                }

                val savedUser = userRepository.getById(resultId.toInt())
                if (savedUser != null) {
                    _registrationResult.emit(RegistrationResult.Success(savedUser))
                } else {
                    _registrationResult.emit(RegistrationResult.Error("Could not retrieve saved user"))
                }
            } catch (e: Exception) {
                _registrationResult.emit(RegistrationResult.Error("Registration error: ${e.message}"))
            }
        }
    }

    private suspend fun generateNextProviderId(): String {
        val latest = userRepository.getLatestProviderIdentifier()
        var sequence = extractProviderSequence(latest) + 1
        if (sequence < 1) sequence = 1

        while (true) {
            val candidate = "PRV${sequence.toString().padStart(3, '0')}"
            if (userRepository.getByStudentId(candidate) == null) {
                return candidate
            }
            sequence++
        }
    }

    private fun extractProviderSequence(providerId: String?): Int {
        if (providerId.isNullOrBlank()) return 0
        return providerId.removePrefix("PRV").toIntOrNull() ?: 0
    }

    sealed class RegistrationResult {
        data class Success(val user: User) : RegistrationResult()
        data class Error(val message: String) : RegistrationResult()
    }
}
