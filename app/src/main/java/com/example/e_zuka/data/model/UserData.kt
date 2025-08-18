package com.example.e_zuka.data.model

import com.google.firebase.Timestamp

data class UserData(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val skills: List<String> = emptyList() // 得意なこと・資格
)
