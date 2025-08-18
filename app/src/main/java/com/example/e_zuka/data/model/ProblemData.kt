package com.example.e_zuka.data.model

import com.google.firebase.Timestamp

// 困りごとデータ
data class ProblemData(
    val problemId: String = "",
    val userId: String = "",
    val displayName: String = "", // 投稿者名
    val regionCodeId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",  // カテゴリー（急ぎ、力仕事、買い物、移動、その他）
    val priority: Int = 0,      // 優先度（0: 低, 1: 中, 2: 高）
    val tags: List<String> = emptyList(), // タグ
    val requiredPeople: Int = 1, // 必要な人数
    val reward: String = "", // お礼（例：お茶でも飲みながら会話、感謝の気持ち、など）
    val estimatedTime: String = "", // 予想所要時間（例：30分程度、1時間程度）
    val photoUrls: List<String> = emptyList(), // 添付写真のURL
    val scheduledDate: Timestamp? = null, // 予定日時
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val isSolved: Boolean = false,
    val solvedAt: Timestamp? = null, // 解決日時
    val helperUserId: String? = null, // 助けているユーザーID
    val helperDisplayName: String = "", // 助けている人の表示名
    val helperAcceptedAt: Timestamp? = null, // 助けることを承諾した日時
    val threadId: String = "", // 困りごと専用チャットスレッドID
)
