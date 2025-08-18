package com.example.e_zuka.data.region

import android.content.Context
import android.util.Log
import com.example.e_zuka.data.base.BaseRepository
import com.example.e_zuka.data.base.NetworkResult
import com.example.e_zuka.data.model.ProblemData
import com.example.e_zuka.data.model.ChatMessageData
import com.example.e_zuka.utils.NotificationUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RegionProblemRepository(context: Context) : BaseRepository(context) {
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "RegionProblemRepository"
        private const val PROBLEMS_COLLECTION = "regionProblems"
        private const val CHAT_COLLECTION = "regionChats"
        private const val CACHE_KEY_PROBLEMS = "problems_"
        private const val CACHE_DURATION_MINUTES = 5L
    }

    // 困りごと投稿
    suspend fun postProblem(problem: ProblemData): NetworkResult<Boolean> {
        return try {
            Log.d(TAG, "Posting new problem: ${problem.title}")

            // 新しいthreadIdを生成
            val threadId = UUID.randomUUID().toString()
            val docRef = firestore.collection(PROBLEMS_COLLECTION).document()

            // 投稿データを作成
            val problemWithId = problem.copy(
                problemId = docRef.id,
                threadId = threadId,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            // Firestoreに保存
            docRef.set(problemWithId).await()

            // 地域メンバーに通知
            val regionMembers = firestore.collection("userRegions")
                .whereEqualTo("regionCodeId", problem.regionCodeId)
                .whereEqualTo("verified", true)
                .get()
                .await()

            // 通知を送信
            NotificationUtils.showRegionNotification(
                context = context,
                title = "新しい困りごと",
                message = "${problem.displayName}さんが「${problem.title}」を投稿しました",
                regionId = problem.regionCodeId
            )

            NetworkResult.Success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to post problem", e)
            NetworkResult.Error(e)
        }
    }

    // 地域の困りごと一覧取得（未解決・解決済みすべて取得）
    suspend fun getProblems(regionCodeId: String): List<ProblemData> {
        return try {
            Log.d(TAG, "Getting problems for region: $regionCodeId")

            // 投稿を取得
            val snapshot = firestore.collection(PROBLEMS_COLLECTION)
                .whereEqualTo("regionCodeId", regionCodeId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val problems = snapshot.documents.mapNotNull {
                it.toObject(ProblemData::class.java)
            }

            // ユーザー情報を取得して補完
            val userIds = problems.map { it.userId }.distinct()
            val helperIds = problems.mapNotNull { it.helperUserId }.distinct()

            val userMap = mutableMapOf<String, String>()
            val helperMap = mutableMapOf<String, String>()

            // ユーザー名を取得
            for (userId in userIds) {
                val userDoc = firestore.collection("users").document(userId).get().await()
                userMap[userId] = userDoc.getString("displayName") ?: "名前未設定"
            }

            // ヘルパー名を取得
            for (helperId in helperIds) {
                val helperDoc = firestore.collection("users").document(helperId).get().await()
                helperMap[helperId] = helperDoc.getString("displayName") ?: "名前未設定"
            }

            // 問題データを更新
            problems.map { problem ->
                problem.copy(
                    displayName = userMap[problem.userId] ?: "名前未設定",
                    helperDisplayName = if (problem.helperUserId != null) {
                        helperMap[problem.helperUserId] ?: "名前未設定"
                    } else ""
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get problems", e)
            emptyList()
        }
    }

    // 問題一覧の取得（キャッシング対応）
    suspend fun getProblemsFlow(regionCodeId: String): Flow<NetworkResult<List<ProblemData>>> {
        return safeApiCall(
            cacheKey = "${CACHE_KEY_PROBLEMS}${regionCodeId}",
            cacheDuration = CACHE_DURATION_MINUTES
        ) {
            val snapshot = firestore.collection("regionProblems")
                .whereEqualTo("regionCodeId", regionCodeId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { it.toObject(ProblemData::class.java) }
        }
    }

    // 困りごとを助ける（helperUserIdをセット）
    suspend fun helpProblem(problemId: String, helperUserId: String): NetworkResult<Boolean> {
        return try {
            Log.d(TAG, "User $helperUserId helping problem $problemId")

            val docRef = firestore.collection(PROBLEMS_COLLECTION).document(problemId)
            val problem = docRef.get().await().toObject(ProblemData::class.java)
                ?: throw Exception("Problem not found")

            // ヘルパー情報を更新
            val helperDoc = firestore.collection("users").document(helperUserId).get().await()
            val helperDisplayName = helperDoc.getString("displayName") ?: "名前未設定"

            docRef.update(
                mapOf(
                    "helperUserId" to helperUserId,
                    "helperDisplayName" to helperDisplayName,
                    "helperAcceptedAt" to Timestamp.now(),
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            // 投稿者に通知
            NotificationUtils.showProblemNotification(
                context = context,
                title = "困りごとへの応答",
                message = "${helperDisplayName}さんが「${problem.title}」を助けることになりました",
                problemId = problemId
            )

            NetworkResult.Success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to help problem", e)
            NetworkResult.Error(e)
        }
    }

    // 困りごと解決（isSolvedをtrueに）
    suspend fun solveProblem(problemId: String): NetworkResult<Boolean> {
        return try {
            Log.d(TAG, "Marking problem $problemId as solved")

            val docRef = firestore.collection(PROBLEMS_COLLECTION).document(problemId)
            val problem = docRef.get().await().toObject(ProblemData::class.java)
                ?: throw Exception("Problem not found")

            docRef.update(
                mapOf(
                    "isSolved" to true,
                    "solvedAt" to Timestamp.now(),
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            // 解決通知
            NotificationUtils.showProblemNotification(
                context = context,
                title = "困りごとが解決しました",
                message = "「${problem.title}」が解決されました",
                problemId = problemId
            )

            NetworkResult.Success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to solve problem", e)
            NetworkResult.Error(e)
        }
    }

    // 初期メッセージの作成
    private fun createInitialMessage(problem: ProblemData): String {
        val sb = StringBuilder()
        sb.append("【困りごと投稿】\n")
        sb.append("${problem.title}\n")
        sb.append("\n${problem.description}\n")

        if (problem.category.isNotEmpty()) {
            sb.append("\nカテゴリー: ${problem.category}")
        }

        val priorityText = when (problem.priority) {
            2 -> "高"
            1 -> "中"
            else -> "低"
        }
        sb.append("\n優先度: $priorityText")

        if (problem.requiredPeople > 1) {
            sb.append("\n必要な人数: ${problem.requiredPeople}人")
        }

        if (problem.estimatedTime.isNotEmpty()) {
            sb.append("\n予想所要時間: ${problem.estimatedTime}")
        }

        if (problem.reward.isNotEmpty()) {
            sb.append("\nお礼: ${problem.reward}")
        }

        if (problem.tags.isNotEmpty()) {
            sb.append("\nタグ: ${problem.tags.joinToString(", ")}")
        }

        return sb.toString()
    }
}
