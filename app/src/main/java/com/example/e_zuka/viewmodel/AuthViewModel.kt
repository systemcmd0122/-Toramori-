package com.example.e_zuka.viewmodel

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.e_zuka.data.auth.AuthRepository
import com.example.e_zuka.data.model.AuthState
import com.example.e_zuka.data.model.CompleteAuthState
import com.example.e_zuka.data.model.RegionAuthState
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(context: Context) : ViewModel() {
    private val authRepository = AuthRepository(context)
    private val regionAuthViewModel = RegionAuthViewModel()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val regionAuthState: StateFlow<RegionAuthState> = regionAuthViewModel.regionAuthState

    private val _completeAuthState = MutableStateFlow(
        CompleteAuthState.create(AuthState.Loading, RegionAuthState.NotVerified)
    )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _emailVerificationSent = MutableStateFlow(false)
    val emailVerificationSent: StateFlow<Boolean> = _emailVerificationSent.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            // Firebase認証状態の監視
            authRepository.getAuthStateFlow().collect { user ->
                if (user != null) {
                    // ユーザーがログインしている場合、認証フローを確認
                    _authState.value = AuthState.Loading
                    checkUserAuthFlow(user)
                } else {
                    // ユーザーがログアウトした場合
                    _authState.value = AuthState.Unauthenticated
                    regionAuthViewModel.resetToNotVerified()
                }
            }
        }

        // 地域認証状態の監視と状態更新
        viewModelScope.launch {
            regionAuthState.collect { regionAuth ->
                updateAuthState(regionAuth)
            }
        }

        // 地域認証ViewModelのメッセージを監視
        viewModelScope.launch {
            regionAuthViewModel.successMessage.collect { message ->
                message?.let { _successMessage.value = it }
            }
        }

        viewModelScope.launch {
            regionAuthViewModel.errorMessage.collect { message ->
                message?.let { _errorMessage.value = it }
            }
        }
    }

    private fun checkUserAuthFlow(user: FirebaseUser) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // 1. displayNameが設定されているかチェック
                if (user.displayName.isNullOrBlank() || !isValidJapaneseName(user.displayName)) {
                    _authState.value = AuthState.UserNameVerificationRequired(user)
                    return@launch
                }

                // 2. 地域認証状態をチェック
                val hasRegionAuth = authRepository.checkRegionAuthStatus(user)
                if (hasRegionAuth) {
                    // 地域認証が既に完了している場合、RegionAuthStateを更新
                    regionAuthViewModel.checkRegionAuthStatus(user)
                    // ここで Verified 状態なら Authenticated に遷移
                    val regionState = regionAuthViewModel.regionAuthState.value
                    if (regionState is RegionAuthState.Verified) {
                        _authState.value = AuthState.Authenticated(user)
                    }
                } else {
                    _authState.value = AuthState.RegionVerificationRequired(user)
                }
            } catch (e: Exception) {
                _errorMessage.value = "認証状態の確認中にエラーが発生しました: ${e.message}"
                _authState.value = AuthState.RegionVerificationRequired(user)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun updateAuthState(regionAuth: RegionAuthState) {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            val newAuthState = when {
                currentUser == null -> {
                    AuthState.Unauthenticated
                }
                // displayNameが設定されていない場合は本名確認が必要
                currentUser.displayName.isNullOrBlank() || !isValidJapaneseName(currentUser.displayName) -> {
                    AuthState.UserNameVerificationRequired(currentUser)
                }
                regionAuth is RegionAuthState.Verified -> {
                    // 地域認証が完了している場合は必ず認証済み状態
                    AuthState.Authenticated(currentUser)
                }
                regionAuth is RegionAuthState.Loading -> {
                    AuthState.Loading
                }
                else -> {
                    AuthState.RegionVerificationRequired(currentUser)
                }
            }
            _authState.value = newAuthState
            _completeAuthState.value = CompleteAuthState.create(newAuthState, regionAuth)
        }
    }

    private fun isValidJapaneseName(name: String?): Boolean {
        return name != null &&
                name.isNotBlank() &&
                name.contains(" ") && // 姓名が分かれている
                name.length >= 3 && // 最小限の長さ
                name.matches(Regex("^[ぁ-んァ-ヶ一-龯々〆〤ー\\s]+$")) // 日本語文字のみ
    }

    // 本名更新メソッド
    fun updateDisplayName(fullName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _errorMessage.value = "ユーザーがログインしていません"
                    return@launch
                }

                // バリデーション
                if (!isValidJapaneseName(fullName)) {
                    _errorMessage.value = "正しい日本語の姓名を入力してください"
                    return@launch
                }

                // FirebaseのdisplayNameを更新
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName)
                    .build()

                currentUser.updateProfile(profileUpdates).await()
                currentUser.reload().await()

                // Firestoreのユーザーデータも同期
                authRepository.syncUserDataToFirestore(currentUser)

                _successMessage.value = "本名を確認しました。地域認証に進みます。"

                // 本名確認完了後、地域認証フローに進む
                checkUserAuthFlow(currentUser)

            } catch (e: Exception) {
                _errorMessage.value = "本名の更新に失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 認証関連のメソッド
    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = authRepository.signInWithEmail(email.trim(), password)
                if (result.success) {
                    clearMessages()
                } else {
                    _errorMessage.value = result.errorMessage ?: "ログインに失敗しました"
                }
            } catch (e: Exception) {
                _errorMessage.value = "ログインに失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signUpWithEmail(email: String, password: String, confirmPassword: String) {
        when {
            email.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
                _errorMessage.value = "すべての項目を入力してください"
                return
            }
            password != confirmPassword -> {
                _errorMessage.value = "パスワードが一致しません"
                return
            }
            password.length < 6 -> {
                _errorMessage.value = "パスワードは6文字以上で入力してください"
                return
            }
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = authRepository.signUpWithEmail(email.trim(), password)
                if (result.success) {
                    _emailVerificationSent.value = true
                    _successMessage.value = "アカウントを作成しました。確認メールを送信しましたので、メール内のリンクをクリックしてメールアドレスを認証してください。"
                } else {
                    _errorMessage.value = result.errorMessage ?: "アカウント作成に失敗しました"
                }
            } catch (e: Exception) {
                _errorMessage.value = "アカウント作成に失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signInWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = authRepository.signInWithGoogle(account)
                if (result.success) {
                    clearMessages()
                } else {
                    _errorMessage.value = result.errorMessage ?: "Googleログインに失敗しました"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Googleログインに失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = authRepository.signOut()
                if (!result.success) {
                    _errorMessage.value = result.errorMessage ?: "ログアウトに失敗しました"
                }
                _emailVerificationSent.value = false
                clearMessages()
            } catch (e: Exception) {
                _errorMessage.value = "ログアウトに失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendEmailVerification() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = authRepository.sendEmailVerification()
                if (result.success) {
                    _emailVerificationSent.value = true
                    _successMessage.value = "確認メールを再送信しました。メール内のリンクをクリックしてメールアドレスを認証してください。"
                } else {
                    _errorMessage.value = result.errorMessage ?: "確認メールの送信に失敗しました"
                }
            } catch (e: Exception) {
                _errorMessage.value = "確認メールの送信に失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkEmailVerification() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = authRepository.getCurrentUser()
                currentUser?.reload()?.await()
                if (currentUser?.isEmailVerified == true) {
                    _successMessage.value = "メールアドレスが認証されました"
                } else {
                    _errorMessage.value = "まだメールアドレスが認証されていません"
                }
            } catch (e: Exception) {
                _errorMessage.value = "認証状態の確認に失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _errorMessage.value = "メールアドレスを入力してください"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = authRepository.resetPassword(email.trim())
                if (result.success) {
                    _successMessage.value = "パスワードリセットメールを送信しました"
                } else {
                    _errorMessage.value = result.errorMessage ?: "パスワードリセットに失敗しました"
                }
            } catch (e: Exception) {
                _errorMessage.value = "パスワードリセットに失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 地域認証関連のメソッド
    fun verifyRegionCode(code: String) {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {
            regionAuthViewModel.verifyRegionCode(code, currentUser)
        } else {
            _errorMessage.value = "ユーザーがログインしていません"
        }
    }

    fun checkRegionAuthStatus() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {
            regionAuthViewModel.checkRegionAuthStatus(currentUser)
        } else {
            _errorMessage.value = "ユーザーがログインしていません"
        }
    }

    // 得意なこと・資格の更新
    fun updateSkills(skills: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _errorMessage.value = "ユーザーがログインしていません"
                    return@launch
                }
                authRepository.updateSkills(currentUser.uid, skills)
                _successMessage.value = "得意なこと・資格を更新しました"
            } catch (e: Exception) {
                _errorMessage.value = "得意なこと・資格の更新に失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 得意なことをFirestoreから読み込む
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadSkills(onLoaded: (List<String>) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _errorMessage.value = "ユーザーがログインしていません"
                    return@launch
                }
                val userDoc = authRepository.getUserDocument(currentUser.uid)
                val skills = userDoc?.get("skills") as? List<String> ?: emptyList()
                onLoaded(skills)
            } catch (e: Exception) {
                _errorMessage.value = "得意なことの読み込みに失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 得意なことをFirestoreに追加
    fun addSkill(skill: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _errorMessage.value = "ユーザーがログインしていません"
                    onComplete(false)
                    return@launch
                }
                authRepository.addSkillToFirestore(currentUser.uid, skill)
                onComplete(true)
            } catch (e: Exception) {
                _errorMessage.value = "得意なことの追加に失敗しました: ${e.message}"
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }


    // ユーティリティメソッド
    fun getGoogleSignInClient(): GoogleSignInClient = authRepository.getGoogleSignInClient()

    fun clearError() {
        _errorMessage.value = null
        regionAuthViewModel.clearError()
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
        regionAuthViewModel.clearSuccessMessage()
    }

    private fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun clearEmailVerificationSent() {
        _emailVerificationSent.value = false
    }
}