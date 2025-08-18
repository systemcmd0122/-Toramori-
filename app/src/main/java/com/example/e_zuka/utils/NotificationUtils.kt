package com.example.e_zuka.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.e_zuka.MainActivity
import com.sadowara.e_zuka.R
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object NotificationUtils {
    private const val TAG = "NotificationUtils"
    private val firestore = FirebaseFirestore.getInstance()

    // チャンネルID
    const val CHANNEL_CHAT = "chat_notifications"
    const val CHANNEL_PROBLEM = "problem_notifications"
    const val CHANNEL_REGION = "region_notifications"
    const val CHANNEL_GENERAL = "default"

    // 通知ID範囲
    private const val CHAT_NOTIFICATION_ID_START = 1000
    private const val PROBLEM_NOTIFICATION_ID_START = 2000
    private const val REGION_NOTIFICATION_ID_START = 3000
    private const val GENERAL_NOTIFICATION_ID_START = 4000

    // FCMトークンの保存
    suspend fun saveFCMToken(userId: String, token: String) {
        try {
            firestore.collection("users").document(userId)
                .update("fcmToken", token)
                .await()
            Log.d(TAG, "FCM token saved for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save FCM token", e)
        }
    }

    // 通知チャンネルの作成
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_CHAT,
                    "チャットの通知",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "チャットメッセージの通知です"
                    setShowBadge(true)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 100, 200, 300)
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
                },
                NotificationChannel(
                    CHANNEL_PROBLEM,
                    "困りごとの通知",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "困りごとに関する通知です"
                    setShowBadge(true)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 200, 100, 200)
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
                },
                NotificationChannel(
                    CHANNEL_REGION,
                    "地域の通知",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "地域に関する通知です"
                    setShowBadge(true)
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_GENERAL,
                    "一般の通知",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "一般的な通知です"
                    setShowBadge(true)
                }
            )

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
            Log.d(TAG, "Notification channels created")
        }
    }

    // チャット通知の表示
    fun showChatNotification(
        context: Context,
        title: String,
        message: String,
        threadId: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            threadId?.let { putExtra("thread_id", it) }
        }
        showNotification(
            context = context,
            channelId = CHANNEL_CHAT,
            title = title,
            message = message,
            notificationId = CHAT_NOTIFICATION_ID_START + (threadId?.hashCode() ?: 0) % 1000,
            intent = intent
        )
    }

    // 困りごと通知の表示
    fun showProblemNotification(
        context: Context,
        title: String,
        message: String,
        problemId: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            problemId?.let { putExtra("problem_id", it) }
        }
        showNotification(
            context = context,
            channelId = CHANNEL_PROBLEM,
            title = title,
            message = message,
            notificationId = PROBLEM_NOTIFICATION_ID_START + (problemId?.hashCode() ?: 0) % 1000,
            intent = intent
        )
    }

    // 地域通知の表示
    fun showRegionNotification(
        context: Context,
        title: String,
        message: String,
        regionId: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            regionId?.let { putExtra("region_id", it) }
        }
        showNotification(
            context = context,
            channelId = CHANNEL_REGION,
            title = title,
            message = message,
            notificationId = REGION_NOTIFICATION_ID_START + (regionId?.hashCode() ?: 0) % 1000,
            intent = intent
        )
    }

    // 一般通知の表示
    fun showGeneralNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = GENERAL_NOTIFICATION_ID_START
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        showNotification(
            context = context,
            channelId = CHANNEL_GENERAL,
            title = title,
            message = message,
            notificationId = notificationId,
            intent = intent
        )
    }

    // 通知の共通処理
    private fun showNotification(
        context: Context,
        channelId: String,
        title: String,
        message: String,
        notificationId: Int,
        intent: Intent
    ) {
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)

        // チャンネルごとの設定
        when (channelId) {
            CHANNEL_CHAT, CHANNEL_PROBLEM -> {
                notificationBuilder
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            }
            CHANNEL_REGION -> {
                notificationBuilder
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            }
            else -> {
                notificationBuilder
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())
        Log.d(TAG, "Notification shown: $title")
    }

    // 特定の通知のキャンセル
    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

    // 特定のチャンネルの通知をすべてキャンセル
    fun cancelNotificationsByChannel(context: Context, channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.deleteNotificationChannel(channelId)
            createNotificationChannels(context)  // チャンネルを再作成
        }
    }
}
