package com.example.housefinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.housefinder.data.repository.UserPreferenceRepository
import com.example.housefinder.db.entities.UserPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(private val repository: UserPreferenceRepository) : ViewModel() {

    private val _preference = MutableStateFlow<UserPreference?>(null)
    val preference: StateFlow<UserPreference?> = _preference

    fun loadPreference(userId: Int) {
        viewModelScope.launch {
            repository.getForUser(userId).collectLatest {
                _preference.value = it
            }
        }
    }

    fun savePreference(preference: UserPreference) {
        viewModelScope.launch {
            repository.upsert(preference)
        }
    }
}
