package com.example.e_zuka.data.model

import com.google.firebase.auth.FirebaseUser

sealed class AuthState {
    data object Loading : AuthState()
    data object Unauthenticated : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class UserNameVerificationRequired(val user: FirebaseUser) : AuthState() // 本名確認が必要
    data class RegionVerificationRequired(val user: FirebaseUser) : AuthState() // 地域認証が必要
    data class Error(val message: String) : AuthState()
}

data class AuthResult(
    val success: Boolean,
    val errorMessage: String? = null,
    val user: FirebaseUser? = null
)

// 認証の完全な状態を管理
data class CompleteAuthState(
    val firebaseAuthState: AuthState,
    val regionAuthState: RegionAuthState,
    val isFullyAuthenticated: Boolean
) {
    companion object {
        fun create(
            firebaseAuthState: AuthState,
            regionAuthState: RegionAuthState
        ): CompleteAuthState {
            val isFullyAuthenticated = when (firebaseAuthState) {
                is AuthState.Authenticated -> when (regionAuthState) {
                    is RegionAuthState.Verified -> true
                    else -> false
                }
                else -> false
            }

            return CompleteAuthState(
                firebaseAuthState = firebaseAuthState,
                regionAuthState = regionAuthState,
                isFullyAuthenticated = isFullyAuthenticated
            )
        }
    }
}