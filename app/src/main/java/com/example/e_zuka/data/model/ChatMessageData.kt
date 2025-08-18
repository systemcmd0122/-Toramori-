package com.example.e_zuka.data.model

import com.google.firebase.Timestamp

// チャットメッセージデータ
 data class ChatMessageData(
    val messageId: String = "",
    val regionCodeId: String = "",
    val threadId: String = "", // スレッドID（困りごと用）
    val userId: String = "",
    val displayName: String = "",
    val content: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)
