package com.example.e_zuka.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * 困りごと投稿のデータモデル
 */
data class HelpRequest(
    val id: String = "", // Firestoreのドキュメントid
    val userId: String = "", // 投稿者のユーザーID
    val title: String = "", // タイトル
    val description: String = "", // 詳細内容
    val preferredDateTime: Timestamp? = null, // 希望日時（任意）
    val estimatedDuration: Int? = null, // 所要時間（分）（任意）
    val priority: RequestPriority = RequestPriority.MEDIUM, // 優先度
    val category: RequestCategory = RequestCategory.OTHER, // カテゴリ
    val status: RequestStatus = RequestStatus.OPEN, // ステータス
    val regionId: String = "", // 地域ID
    @field:PropertyName("created_at")
    val createdAt: Timestamp = Timestamp.now(), // 作成日時
    @field:PropertyName("updated_at")
    val updatedAt: Timestamp = Timestamp.now(), // 更新日時
    @field:PropertyName("helper_id")
    val helperId: String? = null, // 支援者のユーザーID（マッチング後）
    @field:PropertyName("match_timestamp")
    val matchTimestamp: Timestamp? = null, // マッチング日時
    @field:PropertyName("completion_timestamp")
    val completionTimestamp: Timestamp? = null, // 完了日時
    val tags: List<String> = emptyList() // 検索用タグ
)

/**
 * 困りごとの優先度
 */
enum class RequestPriority {
    HIGH,   // 高
    MEDIUM, // 中
    LOW     // 低
}

/**
 * 困りごとのカテゴリ
 */
enum class RequestCategory {
    HOUSEWORK,       // 家事・掃除
    SHOPPING,        // 買い物代行
    TRANSPORTATION,  // 車での送迎
    IT_SUPPORT,      // ITサポート
    EDUCATION,       // 勉強・学習支援
    CONVERSATION,    // 会話・話し相手
    OTHER           // その他
}

/**
 * 困りごとのステータス
 */
enum class RequestStatus {
    OPEN,        // 未マッチング
    MATCHED,     // マッチング済み
    IN_PROGRESS, // 進行中
    COMPLETED,   // 完了
    CANCELLED    // キャンセル
}
