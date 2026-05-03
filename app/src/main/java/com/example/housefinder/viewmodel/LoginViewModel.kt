package com.example.housefinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.housefinder.data.repository.UserRepository
import com.example.housefinder.db.entities.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.mindrot.jbcrypt.BCrypt

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(private val userRepository: UserRepository) : ViewModel() {

    private val _loginResult = MutableSharedFlow<LoginResult>()
    val loginResult = _loginResult.asSharedFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            viewModelScope.launch { _loginResult.emit(LoginResult.Error("Enter email and password")) }
            return
        }

        viewModelScope.launch {
            val user = userRepository.getByEmail(email)
            if (user == null || !BCrypt.checkpw(password, user.passwordHash)) {
                _loginResult.emit(LoginResult.Error("Invalid credentials"))
                return@launch
            }

            _loginResult.emit(LoginResult.Success(user))
        }
    }

    sealed class LoginResult {
        data class Success(val user: User) : LoginResult()
        data class Error(val message: String) : LoginResult()
    }
}
