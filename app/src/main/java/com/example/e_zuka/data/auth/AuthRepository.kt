package com.example.e_zuka.data.auth

import android.content.Context
import android.util.Log
import com.example.e_zuka.data.model.AuthResult
import com.example.e_zuka.data.model.UserData
import com.example.e_zuka.data.region.RegionAuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

class AuthRepository(context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val googleSignInClient: GoogleSignInClient
    private val regionAuthRepository: RegionAuthRepository = RegionAuthRepository()

    companion object {
        private const val TAG = "AuthRepository"
    }

    init {
        // safe lookup for default_web_client_id to avoid direct R reference
        val clientIdResId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        val clientId = if (clientIdResId != 0) context.getString(clientIdResId) else ""

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getAuthStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            Log.d(TAG, "Auth state changed - user: ${user?.uid}, email verified: ${user?.isEmailVerified}")
            trySend(user)
        }
        auth.addAuthStateListener(listener)

        // 初回の状態も送信
        val currentUser = auth.currentUser
        Log.d(TAG, "Initial auth state - user: ${currentUser?.uid}, email verified: ${currentUser?.isEmailVerified}")
        trySend(currentUser)

        awaitClose {
            Log.d(TAG, "Removing auth state listener")
            auth.removeAuthStateListener(listener)
        }
    }.distinctUntilChanged { old, new ->
        // ユーザーのUIDが同じかどうかで比較（より詳細な比較も可能）
        val oldUid = old?.uid
        val newUid = new?.uid
        val result = oldUid == newUid
        Log.d(TAG, "Auth state comparison - old: $oldUid, new: $newUid, same: $result")
        result
    }

    fun getCurrentUser(): FirebaseUser? {
        val user = auth.currentUser
        Log.d(TAG, "Getting current user: ${user?.uid}")
        return user
    }

    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            Log.d(TAG, "Starting email sign in for: $email")

            val result = auth.signInWithEmailAndPassword(email, password).await()
            if (result.user != null) {
                Log.d(TAG, "Email sign in successful for user: ${result.user!!.uid}")

                // ユーザー情報を最新に更新
                result.user!!.reload().await()

                // Firestoreのユーザーデータを同期
                syncUserDataToFirestore(result.user!!)

                AuthResult(success = true, user = result.user)
            } else {
                Log.w(TAG, "Email sign in failed - no user returned")
                AuthResult(success = false, errorMessage = "ログインに失敗しました")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Email sign in error", e)
            AuthResult(success = false, errorMessage = getLocalizedErrorMessage(e))
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): AuthResult {
        return try {
            Log.d(TAG, "Starting email sign up for: $email")

            val result = auth.createUserWithEmailAndPassword(email, password).await()
            if (result.user != null) {
                Log.d(TAG, "Email sign up successful for user: ${result.user!!.uid}")

                // デフォルトの表示名は設定しない（本名確認フローに進む）
                result.user?.reload()?.await()

                // Firestoreにユーザーデータを作成
                createUserDataInFirestore(result.user!!)

                // メール認証を送信
                sendEmailVerification()

                AuthResult(success = true, user = result.user)
            } else {
                Log.w(TAG, "Email sign up failed - no user returned")
                AuthResult(success = false, errorMessage = "アカウント作成に失敗しました")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Email sign up error", e)
            AuthResult(success = false, errorMessage = getLocalizedErrorMessage(e))
        }
    }

    suspend fun sendEmailVerification(): AuthResult {
        return try {
            val user = auth.currentUser
            if (user != null && !user.isEmailVerified) {
                Log.d(TAG, "Sending email verification to: ${user.email}")

                user.sendEmailVerification().await()
                AuthResult(success = true)
            } else {
                Log.d(TAG, "Email already verified or no user")
                AuthResult(success = false, errorMessage = "メールアドレスは既に認証済みです")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Send email verification error", e)
            AuthResult(success = false, errorMessage = getLocalizedErrorMessage(e))
        }
    }

    fun getGoogleSignInClient(): GoogleSignInClient = googleSignInClient

    suspend fun signInWithGoogle(account: GoogleSignInAccount): AuthResult {
        return try {
            Log.d(TAG, "Starting Google sign in for: ${account.email}")

            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = auth.signInWithCredential(credential).await()
            if (result.user != null) {
                Log.d(TAG, "Google sign in successful for user: ${result.user!!.uid}")

                // ユーザー情報を最新に更新
                result.user!!.reload().await()


                // Firestoreのユーザーデータを同期
                syncUserDataToFirestore(result.user!!)

                AuthResult(success = true, user = result.user)
            } else {
                Log.w(TAG, "Google sign in failed - no user returned")
                AuthResult(success = false, errorMessage = "Googleログインに失敗しました")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google sign in error", e)
            AuthResult(success = false, errorMessage = getLocalizedErrorMessage(e))
        }
    }

    suspend fun signOut(): AuthResult {
        return try {
            val currentUserId = auth.currentUser?.uid
            Log.d(TAG, "Starting sign out for user: $currentUserId")

            auth.signOut()
            googleSignInClient.signOut().await()

            Log.d(TAG, "Sign out successful")
            AuthResult(success = true)
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error", e)
            AuthResult(success = false, errorMessage = "ログアウトに失敗しました")
        }
    }

    suspend fun resetPassword(email: String): AuthResult {
        return try {
            Log.d(TAG, "Sending password reset email to: $email")

            auth.sendPasswordResetEmail(email).await()

            Log.d(TAG, "Password reset email sent successfully")
            AuthResult(success = true)
        } catch (e: Exception) {
            Log.e(TAG, "Password reset error", e)
            AuthResult(success = false, errorMessage = getLocalizedErrorMessage(e))
        }
    }

    // ユーザーの地域認証状態をチェック
    suspend fun checkRegionAuthStatus(user: FirebaseUser): Boolean {
        return try {
            Log.d(TAG, "Checking region auth status for user: ${user.uid}")

            val regionData = regionAuthRepository.getUserRegionData(user.uid)
            val hasRegionAuth = regionData?.isVerified == true

            Log.d(TAG, "User ${user.uid} region auth status: $hasRegionAuth, region: ${regionData?.regionName}")

            hasRegionAuth
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check region auth status", e)
            false
        }
    }

    private suspend fun createUserDataInFirestore(user: FirebaseUser) {
        try {
            Log.d(TAG, "Creating user data in Firestore for: ${user.uid}")

            val userData = UserData(
                userId = user.uid,
                displayName = user.displayName ?: "", // 空文字にして本名確認を促す
                email = user.email ?: "",
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            firestore.collection("users")
                .document(user.uid)
                .set(userData)
                .await()

            Log.d(TAG, "User data created successfully in Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create user data in Firestore", e)
            throw e
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun syncUserDataToFirestore(user: FirebaseUser) {
        try {
            Log.d(TAG, "Syncing user data to Firestore for: ${user.uid}")

            val userDocRef = firestore.collection("users").document(user.uid)
            val userDoc = userDocRef.get().await()

            val userData = UserData(
                userId = user.uid,
                displayName = user.displayName ?: "",
                email = user.email ?: "",
                createdAt = if (userDoc.exists()) userDoc.getTimestamp("createdAt") ?: Timestamp.now() else Timestamp.now(),
                updatedAt = Timestamp.now(),
                skills = if (userDoc.exists()) userDoc.get("skills") as? List<String> ?: emptyList() else emptyList()
            )

            userDocRef.set(userData).await()

            Log.d(TAG, "User data synced successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync user data to Firestore", e)
            throw e
        }
    }

    // 得意なこと・資格の更新
    suspend fun updateSkills(userId: String, skills: List<String>) {
        try {
            val userDocRef = firestore.collection("users").document(userId)
            userDocRef.update("skills", skills).await()
            Log.d(TAG, "Skills updated for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update skills", e)
            throw e
        }
    }

    suspend fun getUserDocument(userId: String): Map<String, Any>? {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            if (userDoc.exists()) {
                userDoc.data
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user document", e)
            null
        }
    }

    suspend fun addSkillToFirestore(userId: String, skill: String) {
        try {
            val userDocRef = firestore.collection("users").document(userId)
            val userDoc = userDocRef.get().await()

            if (!userDoc.exists()) {
                // ユーザードキュメントが存在しない場合は作成
                val user = getCurrentUser() ?: throw Exception("User not logged in")
                syncUserDataToFirestore(user)
            }

            userDocRef.update("skills", FieldValue.arrayUnion(skill)).await()
            Log.d(TAG, "Skill added to Firestore for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add skill to Firestore", e)
            throw e
        }
    }

    private fun getLocalizedErrorMessage(exception: Exception): String {
        return when (exception) {
            is FirebaseAuthException -> when (exception.errorCode) {
                "ERROR_INVALID_EMAIL" -> "メールアドレスの形式が正しくありません"
                "ERROR_WRONG_PASSWORD" -> "パスワードが間違っています"
                "ERROR_USER_NOT_FOUND" -> "このメールアドレスは登録されていません"
                "ERROR_EMAIL_ALREADY_IN_USE" -> "このメールアドレスは既に使用されています"
                "ERROR_WEAK_PASSWORD" -> "パスワードは6文字以上で入力してください"
                "ERROR_NETWORK_REQUEST_FAILED" -> "ネットワークエラーが発生しました"
                "ERROR_TOO_MANY_REQUESTS" -> "ログイン試行回数が上限に達しました。しばらく待ってから再試行してください"
                "ERROR_USER_DISABLED" -> "このアカウントは無効化されています"
                else -> exception.message ?: "認証エラーが発生しました"
            }
            else -> exception.message ?: "エラーが発生しました"
        }
    }
}