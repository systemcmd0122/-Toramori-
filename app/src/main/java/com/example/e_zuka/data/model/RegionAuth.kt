package com.example.e_zuka.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

// 地域認証状態
sealed class RegionAuthState {
    data object NotVerified : RegionAuthState()
    data class Verified(val regionData: RegionData) : RegionAuthState()
    data object Loading : RegionAuthState()
    data class Error(val message: String) : RegionAuthState()
}

// 地域データ
data class RegionData(
    val codeId: String = "",
    val regionName: String = "",
    val code: String = "",
    @field:PropertyName("isActive")
    val isActive: Boolean = true,
    val createdAt: Timestamp = Timestamp.now(),
    val currentUsageCount: Int = 0
)

// 地域認証結果
data class RegionAuthResult(
    val success: Boolean,
    val errorMessage: String? = null,
    val regionData: RegionData? = null
)

// ユーザーの地域認証情報
data class UserRegionData(
    val userId: String = "",
    val regionCodeId: String = "",
    val regionName: String = "",
    val verifiedAt: Timestamp = Timestamp.now(),
    @field:PropertyName("verified")
    val isVerified: Boolean = false
)