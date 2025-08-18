package com.example.e_zuka.data.region

import android.util.Log
import com.example.e_zuka.data.model.ChatMessageData
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RegionChatRepository {
    private val firestore = FirebaseFirestore.getInstance()
    companion object {
        private const val TAG = "RegionChatRepository"
        private const val CHATS_COLLECTION = "regionChats"
    }

    // チャットメッセージ一覧（リアルタイム）
    fun getMessagesFlow(regionCodeId: String): Flow<List<ChatMessageData>> = callbackFlow {
        val query = firestore.collection(CHATS_COLLECTION)
            .whereEqualTo("regionCodeId", regionCodeId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Chat snapshot error", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val messages = snapshot?.documents?.mapNotNull { it.toObject(ChatMessageData::class.java)?.copy(messageId = it.id) } ?: emptyList()
            trySend(messages)
        }
        awaitClose { listener.remove() }
    }

    // メッセージ送信
    suspend fun sendMessage(regionCodeId: String, userId: String, displayName: String, content: String): Boolean {
        return try {
            val docRef = firestore.collection(CHATS_COLLECTION).document()
            val message = ChatMessageData(
                messageId = docRef.id,
                regionCodeId = regionCodeId,
                userId = userId,
                displayName = displayName,
                content = content,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            docRef.set(message).await()

            // 送信者以外のメンバーに通知
            val regionMembers = firestore.collection("userRegions")
                .whereEqualTo("regionCodeId", regionCodeId)
                .whereEqualTo("verified", true)
                .get()
                .await()

            // 通知用のデータ
            val notificationData = mapOf(
                "type" to "chat",
                "threadId" to regionCodeId,
                "senderId" to userId,
                "senderName" to displayName
            )

            // FCMトークンを取得して通知を送信
            regionMembers.documents.forEach { doc ->
                val memberId = doc.getString("userId")
                if (memberId != null && memberId != userId) {
                    val userDoc = firestore.collection("users").document(memberId).get().await()
                    val fcmToken = userDoc.getString("fcmToken")
                    if (fcmToken != null) {
                        // FCM通知を送信（実際のFCM実装に応じて）
                    }
                }
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send chat message", e)
            false
        }
    }

    // メッセージ編集
    suspend fun editMessage(messageId: String, newContent: String): Boolean {
        return try {
            val docRef = firestore.collection(CHATS_COLLECTION).document(messageId)
            docRef.update(mapOf(
                "content" to newContent,
                "updatedAt" to Timestamp.now()
            )).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to edit chat message", e)
            false
        }
    }

    // メッセージ削除
    suspend fun deleteMessage(messageId: String): Boolean {
        return try {
            firestore.collection(CHATS_COLLECTION).document(messageId).delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete chat message", e)
            false
        }
    }
}
