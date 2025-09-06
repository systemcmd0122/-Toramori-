package com.example.e_zuka.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.e_zuka.data.model.RegionAuthState
import com.example.e_zuka.data.model.RegionData
import com.example.e_zuka.data.region.RegionAuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegionAuthViewModel : ViewModel() {
    private val regionAuthRepository = RegionAuthRepository()

    private val _regionAuthState = MutableStateFlow<RegionAuthState>(RegionAuthState.NotVerified)
    val regionAuthState: StateFlow<RegionAuthState> = _regionAuthState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    companion object {
        private const val TAG = "RegionAuthViewModel"
    }

    fun verifyRegionCode(code: String, user: FirebaseUser) {
        if (code.isBlank()) {
            _errorMessage.value = "地域認証コードを入力してください"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _regionAuthState.value = RegionAuthState.Loading

            try {
                Log.d(TAG, "Starting region verification for code: $code")

                val result = regionAuthRepository.verifyRegionCode(code.trim(), user.uid)

                if (result.success && result.regionData != null) {
                    Log.d(TAG, "Region verification successful: ${result.regionData.regionName}")

                    _regionAuthState.value = RegionAuthState.Verified(result.regionData)
                    _successMessage.value = "地域認証が完了しました（${result.regionData.regionName}）"
                    clearError()
                } else {
                    Log.w(TAG, "Region verification failed: ${result.errorMessage}")

                    _regionAuthState.value = RegionAuthState.Error(result.errorMessage ?: "地域認証に失敗しました")
                    _errorMessage.value = result.errorMessage ?: "地域認証に失敗しました"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Region verification error", e)

                val errorMessage = "地域認証中にエラーが発生しました: ${e.message}"
                _regionAuthState.value = RegionAuthState.Error(errorMessage)
                _errorMessage.value = errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkRegionAuthStatus(user: FirebaseUser) {
        viewModelScope.launch {
            // 既にVerified状態の場合は再確認をスキップ
            if (_regionAuthState.value is RegionAuthState.Verified) {
                Log.d(TAG, "Region auth already verified, skipping check")
                return@launch
            }

            _isLoading.value = true
            _regionAuthState.value = RegionAuthState.Loading

            try {
                Log.d(TAG, "Checking region auth status for user: ${user.uid}")

                val regionData = regionAuthRepository.getUserRegionData(user.uid)

                if (regionData?.isVerified == true &&
                    regionData.regionCodeId.isNotBlank() &&
                    regionData.regionName.isNotBlank()
                ) {
                    // 既存の地域データから完全なRegionDataを構築
                    val completeRegionData = RegionData(
                        codeId = regionData.regionCodeId,
                        regionName = regionData.regionName,
                        createdAt = regionData.verifiedAt,
                        currentUsageCount = 1
                    )

                    Log.d(TAG, "Found valid region auth: ${regionData.regionName}")
                    _regionAuthState.value = RegionAuthState.Verified(completeRegionData)
                } else {
                    Log.d(TAG, "No valid region auth found")
                    _regionAuthState.value = RegionAuthState.NotVerified
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking region auth status", e)
                val errorMessage = "地域認証状態の確認に失敗しました: ${e.message}"
                _regionAuthState.value = RegionAuthState.Error(errorMessage)
                _errorMessage.value = errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetToNotVerified() {
        Log.d(TAG, "Resetting region auth state to NotVerified")
        _regionAuthState.value = RegionAuthState.NotVerified
        _isLoading.value = false
        clearMessages()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    private fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}