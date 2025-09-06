package com.example.e_zuka.data.model

import com.google.firebase.Timestamp

// 地域メンバー情報
data class RegionMemberData(
    val userId: String = "",
    val displayName: String = "",
    val regionName: String = "",
    val joinedAt: Timestamp = Timestamp.now()
)

