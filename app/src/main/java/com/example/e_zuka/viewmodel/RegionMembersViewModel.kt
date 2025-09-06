package com.example.e_zuka.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.e_zuka.data.model.RegionMemberData
import com.example.e_zuka.data.region.RegionMembersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegionMembersViewModel : ViewModel() {
    private val membersRepository = RegionMembersRepository()

    private val _members = MutableStateFlow<List<RegionMemberData>>(emptyList())
    val members: StateFlow<List<RegionMemberData>> = _members.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    companion object {
        private const val TAG = "RegionMembersViewModel"
    }

    fun loadRegionMembers(regionCodeId: String) {
        if (regionCodeId.isBlank()) {
            Log.w(TAG, "Region code ID is blank")
            _errorMessage.value = "地域情報が正しくありません"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                Log.d(TAG, "Loading region members for regionCodeId: $regionCodeId")

                val membersList = membersRepository.getRegionMembers(regionCodeId)

                Log.d(TAG, "Successfully loaded ${membersList.size} members")

                _members.value = membersList.sortedByDescending { it.joinedAt }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load region members", e)
                _errorMessage.value = "メンバー情報の読み込みに失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun getUserSkills(userId: String, onResult: (List<String>) -> Unit) {
        viewModelScope.launch {
            try {
                val skills = membersRepository.getUserSkills(userId)
                onResult(skills)
            } catch (_: Exception) {
                onResult(emptyList())
            }
        }
    }

    // ViewModelがクリアされる際のクリーンアップ
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "RegionMembersViewModel cleared")
    }
}