package com.example.e_zuka.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.e_zuka.data.model.UserNotification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationsViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _notifications = MutableStateFlow<List<UserNotification>>(emptyList())
    val notifications: StateFlow<List<UserNotification>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadNotifications(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = firestore.collection("notifications")
                    .whereEqualTo("userId", userId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                _notifications.value = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserNotification::class.java)
                }
            } catch (e: Exception) {
                // エラー処理
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("notifications")
                    .document(notificationId)
                    .update("isRead", true)
                    .await()

                // 既存のリストから該当の通知を更新
                _notifications.value = _notifications.value.map { notification ->
                    if (notification.id == notificationId) {
                        notification.copy(isRead = true)
                    } else {
                        notification
                    }
                }
            } catch (e: Exception) {
                // エラー処理
            }
        }
    }

    fun clearNotifications(userId: String) {
        viewModelScope.launch {
            try {
                val batch = firestore.batch()
                val notifications = firestore.collection("notifications")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                notifications.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                batch.commit().await()
                _notifications.value = emptyList()
            } catch (e: Exception) {
                // エラー処理
            }
        }
    }
}
