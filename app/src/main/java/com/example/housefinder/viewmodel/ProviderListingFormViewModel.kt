package com.example.housefinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.housefinder.data.repository.ListingImageRepository
import com.example.housefinder.data.repository.ListingRepository
import com.example.housefinder.data.repository.UserRepository
import com.example.housefinder.db.entities.Listing
import com.example.housefinder.ui.common.HouseDateFormatter
import com.example.housefinder.ui.common.ListingInputOptions
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProviderListingFormViewModel @Inject constructor(
    private val listingRepository: ListingRepository,
    private val listingImageRepository: ListingImageRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _listing = MutableStateFlow<Listing?>(null)
    val listing: StateFlow<Listing?> = _listing

    private val _coverImage = MutableStateFlow<String?>(null)
    val coverImage: StateFlow<String?> = _coverImage

    private val _saveResult = MutableSharedFlow<SaveResult>()
    val saveResult: SharedFlow<SaveResult> = _saveResult

    fun loadListing(id: Int) {
        if (id <= 0) return
        viewModelScope.launch {
            val l = listingRepository.getByIdOnce(id)
            _listing.value = l
            _coverImage.value = listingImageRepository.getCoverImage(id)?.imagePath
        }
    }

    fun setCoverImage(path: String?) {
        _coverImage.value = path
    }

    fun saveListing(
        listingId: Int,
        providerId: Int,
        title: String,
        description: String,
        price: Float,
        location: String,
        type: String,
        amenities: String,
        deposit: Int,
        availability: String
    ) {
        val availabilityStorage = HouseDateFormatter.toStorageDate(availability)
        if (availabilityStorage == null) {
            viewModelScope.launch { _saveResult.emit(SaveResult.Error("Invalid date format")) }
            return
        }
        val normalizedType = type.trim().uppercase()
        if (normalizedType !in ListingInputOptions.supportedRoomTypes) {
            viewModelScope.launch {
                _saveResult.emit(
                    SaveResult.Error(
                        "Type must be one of: ${ListingInputOptions.supportedRoomTypes.joinToString()}"
                    )
                )
            }
            return
        }
        if (amenities.isBlank()) {
            viewModelScope.launch { _saveResult.emit(SaveResult.Error("Amenities are required")) }
            return
        }

        viewModelScope.launch {
            try {
                val provider = userRepository.getById(providerId)
                if (provider?.role != "PROVIDER") {
                    _saveResult.emit(SaveResult.Error("Only providers can manage listings"))
                    return@launch
                }

                val imagePathToSave = _coverImage.value

                if (listingId > 0) {
                    val existing = listingRepository.getByIdOnce(listingId)
                    if (existing == null || existing.providerId != providerId) {
                        _saveResult.emit(SaveResult.Error("You are not allowed to update this listing"))
                        return@launch
                    }
                    val existingCoverImage = listingImageRepository.getCoverImage(listingId)?.imagePath
                    if (imagePathToSave.isNullOrBlank() && existingCoverImage.isNullOrBlank()) {
                        _saveResult.emit(SaveResult.Error("At least one image is required"))
                        return@launch
                    }

                    val updated = Listing(
                        id = listingId,
                        providerId = providerId,
                        title = title,
                        description = description.ifBlank { existing.description },
                        price = price,
                        location = location,
                        type = normalizedType,
                        amenities = amenities,
                        depositAmount = deposit,
                        availabilityDate = availabilityStorage,
                        status = existing.status,
                        distanceToCampusKm = existing.distanceToCampusKm,
                        createdAt = existing.createdAt
                    )
                    listingRepository.update(updated)
                    if (!imagePathToSave.isNullOrBlank()) {
                        listingImageRepository.replaceCoverImage(listingId, imagePathToSave)
                    }
                    _saveResult.emit(SaveResult.Success("Listing updated"))
                } else {
                    if (imagePathToSave.isNullOrBlank()) {
                        _saveResult.emit(SaveResult.Error("At least one image is required"))
                        return@launch
                    }
                    val distanceToCampus = (Math.random() * 5 + 0.5).toFloat()
                    val newListing = Listing(
                        providerId = providerId,
                        title = title,
                        description = description.ifBlank { "No description" },
                        price = price,
                        location = location,
                        type = normalizedType,
                        amenities = amenities,
                        depositAmount = deposit,
                        availabilityDate = availabilityStorage,
                        status = "AVAILABLE",
                        distanceToCampusKm = distanceToCampus
                    )
                    val insertedId = listingRepository.insert(newListing).toInt()
                    if (!imagePathToSave.isNullOrBlank()) {
                        listingImageRepository.replaceCoverImage(insertedId, imagePathToSave)
                    }
                    _saveResult.emit(SaveResult.Success("Listing created"))
                }
            } catch (e: Exception) {
                _saveResult.emit(SaveResult.Error(e.message ?: "Unknown error"))
            }
        }
    }

    sealed class SaveResult {
        data class Success(val message: String) : SaveResult()
        data class Error(val message: String) : SaveResult()
    }

}
