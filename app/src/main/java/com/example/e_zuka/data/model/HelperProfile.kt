package com.example.e_zuka.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * サポーター（助け手）のプロフィールデータモデル
 */
data class HelperProfile(
    val userId: String = "", // ユーザーID
    val skills: List<Skill> = emptyList(), // スキル一覧
    val availability: List<TimeSlot> = emptyList(), // 活動可能な時間帯
    @field:PropertyName("is_certified")
    val isCertified: Boolean = false, // 公認サポーター認定状態
    @field:PropertyName("certification_organization")
    val certificationOrganization: String? = null, // 認定組織（自治体など）
    @field:PropertyName("total_helps")
    val totalHelps: Int = 0, // 支援回数
    @field:PropertyName("thanks_count")
    val thanksCount: Int = 0, // 感謝された回数
    val rating: Float = 0f, // 評価平均（0-5）
    @field:PropertyName("created_at")
    val createdAt: Timestamp = Timestamp.now(),
    @field:PropertyName("updated_at")
    val updatedAt: Timestamp = Timestamp.now()
)

/**
 * スキルデータモデル
 */
data class Skill(
    val name: String = "", // スキル名
    val description: String = "", // 説明
    val category: SkillCategory = SkillCategory.OTHER, // カテゴリ
    @field:PropertyName("experience_years")
    val experienceYears: Int? = null, // 経験年数
    val certifications: List<String> = emptyList(), // 関連資格
    @field:PropertyName("created_at")
    val createdAt: Timestamp = Timestamp.now()
)

/**
 * スキルカテゴリ
 */
enum class SkillCategory {
    HOUSEWORK,       // 家事・掃除
    SHOPPING,        // 買い物代行
    TRANSPORTATION,  // 車での送迎
    IT_SUPPORT,      // ITサポート
    EDUCATION,       // 勉強・学習支援
    CONVERSATION,    // 会話・話し相手
    OTHER           // その他
}

/**
 * 活動可能時間帯
 */
data class TimeSlot(
    val dayOfWeek: Int, // 曜日（0=日曜）
    @field:PropertyName("start_hour")
    val startHour: Int, // 開始時間（0-23）
    @field:PropertyName("end_hour")
    val endHour: Int, // 終了時間（0-23）
    val note: String = "" // 備考
)
