package com.example.e_zuka.service

import android.util.Log
import com.example.e_zuka.utils.NotificationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MessagingService"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")

        // 現在のユーザーIDを取得してトークンを保存
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    NotificationUtils.saveFCMToken(userId, token)
                    Log.d(TAG, "FCM token saved successfully for user: $userId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save FCM token", e)
                }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        // 通知データの取得
        val title = message.notification?.title ?: "新着メッセージ"
        val body = message.notification?.body ?: "新しい通知があります"
        val type = message.data["type"] ?: "general"
        val id = message.data["id"]
        val threadId = message.data["threadId"]
        val problemId = message.data["problemId"]
        val regionId = message.data["regionId"]

        // 通知タイプに応じた処理
        when (type) {
            "chat" -> {
                NotificationUtils.showChatNotification(
                    context = this,
                    title = title,
                    message = body,
                    threadId = threadId
                )
            }
            "problem" -> {
                NotificationUtils.showProblemNotification(
                    context = this,
                    title = title,
                    message = body,
                    problemId = problemId
                )
            }
            "region" -> {
                NotificationUtils.showRegionNotification(
                    context = this,
                    title = title,
                    message = body,
                    regionId = regionId
                )
            }
            else -> {
                NotificationUtils.showGeneralNotification(
                    context = this,
                    title = title,
                    message = body,
                    notificationId = id?.toIntOrNull() ?: 0
                )
            }
        }
    }
}
