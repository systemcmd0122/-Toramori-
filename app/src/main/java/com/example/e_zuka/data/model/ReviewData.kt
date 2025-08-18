package com.example.e_zuka.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class ReviewData(
    val id: String = "",
    val problemId: String = "",
    val reviewerId: String = "", // レビューを書いた人
    val reviewedId: String = "", // レビューされた人
    val reviewerRole: String = "", // "poster" (投稿者) or "helper" (支援者)
    val rating: Float = 0.0f,
    val comment: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
)