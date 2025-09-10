package com.example.e_zuka.viewmodel

import android.content.Context
import android.location.Address
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.e_zuka.data.location.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationViewModel(context: Context) : ViewModel() {
    private val locationRepository = LocationRepository(context)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _address = MutableStateFlow<Address?>(null)
    val address: StateFlow<Address?> = _address.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun getCurrentLocation(onSuccess: (String, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (!locationRepository.hasLocationPermission()) {
                    _errorMessage.value = "位置情報の権限が必要です"
                    return@launch
                }

                val location = locationRepository.getCurrentLocation()
                val address = locationRepository.getAddressFromLocation(location)

                if (address != null) {
                    _address.value = address
                    // 都道府県と市町村を抽出
                    val prefecture = address.adminArea ?: ""
                    val city = address.locality ?: ""
                    if (prefecture.isNotBlank() && city.isNotBlank()) {
                        onSuccess(prefecture, city)
                    } else {
                        _errorMessage.value = "住所情報を取得できませんでした"
                    }
                } else {
                    _errorMessage.value = "住所情報を取得できませんでした"
                }
            } catch (e: Exception) {
                _errorMessage.value = "位置情報の取得に失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LocationViewModel::class.java)) {
                return LocationViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
