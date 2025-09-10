package com.example.e_zuka.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class UserData(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val skills: List<String> = emptyList(), // 得意なこと・資格
    val prefecture: String = "", // 都道府県
    val city: String = "", // 市町村
    @field:PropertyName("isAddressPublic")
    val isAddressPublic: Boolean = false // 住所を公開するかどうか
)
