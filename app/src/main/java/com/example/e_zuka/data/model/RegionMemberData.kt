package com.example.e_zuka.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

// 地域メンバー情報
data class RegionMemberData(
    val userId: String = "",
    val displayName: String = "",
    val regionName: String = "",
    val joinedAt: Timestamp = Timestamp.now()
)

// 地域メンバー統計情報
data class RegionMemberStats(
    val totalMembers: Int = 0,
    val newMembersThisWeek: Int = 0,
    val newMembersThisMonth: Int = 0
)