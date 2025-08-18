package com.example.e_zuka.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.e_zuka.data.model.ChatMessageData
import com.example.e_zuka.data.region.RegionChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RegionChatViewModel : ViewModel() {
    private val repository = RegionChatRepository()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // チャットメッセージ一覧（regionCodeIdごと）
    fun getMessagesFlow(regionCodeId: String) = repository.getMessagesFlow(regionCodeId)

    fun sendMessage(regionCodeId: String, userId: String, displayName: String, content: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.sendMessage(regionCodeId, userId, displayName, content)
                if (result) {
                    _successMessage.value = "メッセージを送信しました"
                } else {
                    _errorMessage.value = "送信に失敗しました"
                }
            } catch (e: Exception) {
                _errorMessage.value = "送信に失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.editMessage(messageId, newContent)
                if (result) {
                    _successMessage.value = "メッセージを編集しました"
                } else {
                    _errorMessage.value = "編集に失敗しました"
                }
            } catch (e: Exception) {
                _errorMessage.value = "編集に失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.deleteMessage(messageId)
                if (result) {
                    _successMessage.value = "メッセージを削除しました"
                } else {
                    _errorMessage.value = "削除に失敗しました"
                }
            } catch (e: Exception) {
                _errorMessage.value = "削除に失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() { _errorMessage.value = null }
    fun clearSuccessMessage() { _successMessage.value = null }
}

