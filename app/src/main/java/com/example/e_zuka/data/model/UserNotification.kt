package com.example.e_zuka.data.model

import com.google.firebase.Timestamp

enum class NotificationType {
    PROBLEM,  // 困りごと関連の通知
    CHAT,     // チャット関連の通知
    SYSTEM    // システム通知
}

data class UserNotification(
    val id: String = "",
    val userId: String = "",
    val type: NotificationType = NotificationType.SYSTEM,
    val title: String = "",
    val message: String = "",
    val isRead: Boolean = false,
    val referenceId: String? = null,  // 関連する困りごとやチャットのID
    val createdAt: Timestamp = Timestamp.now()
)
