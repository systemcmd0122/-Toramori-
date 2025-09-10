@file:Suppress("DEPRECATION")
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
import com.example.e_zuka.data.model.UserData
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    // one-shot events (SharedFlow) to emit messages exactly once to UI
    private val _successEvents = MutableSharedFlow<String>()
    val successEvents: SharedFlow<String> = _successEvents.asSharedFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    // 最近の操作情報を保持してUIから再試行できるようにする
    private var lastSignInEmail: String? = null
    private var lastSignInPassword: String? = null

    private var lastSignUpEmail: String? = null
    private var lastSignUpPassword: String? = null
    private var lastSignUpConfirm: String? = null

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

        // Forward any StateFlow messages to one-shot SharedFlows and clear the StateFlow
        viewModelScope.launch {
            _successMessage.collect { msg ->
                msg?.let {
                    _successEvents.emit(it)
                    _successMessage.value = null
                }
            }
        }

        viewModelScope.launch {
            _errorMessage.collect { msg ->
                msg?.let {
                    _errorEvents.emit(it)
                    _errorMessage.value = null
                }
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
                _errorMessage.value = "認証状態の確認中にエラーが発生しました: ${e.message} 。ネットワークを確認して再試行してください。"
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
                name.length >= 2 && // 最小限の長さを2文字に緩和
                // 日本語文字に加えて一般的な記号も許可
                name.matches(Regex("^[ぁ-んァ-ヶ一-龯々〆〤ー・.\\s]+$"))
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
                _errorMessage.value = "本名の更新に失敗しました: ${e.message}。ネットワークを確認し、再試行してください。"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 認証関連のメソッド
    fun signInWithEmail(email: String, password: String) {
        lastSignInEmail = email
        lastSignInPassword = password

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = authRepository.signInWithEmail(email.trim(), password)
                if (result.success) {
                    clearMessages()
                    // 成功時は保存された再試行情報をクリア
                    lastSignInEmail = null
                    lastSignInPassword = null
                } else {
                    _errorMessage.value = (result.errorMessage ?: "ログインに失敗しました") + "。ネットワークエラーの場合は再試行してください。"
                }
            } catch (e: Exception) {
                _errorMessage.value = "ログインに失敗しました: ${e.message}。ネットワークを確認して再試行してください。"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun retrySignIn() {
        val email = lastSignInEmail
        val password = lastSignInPassword
        if (email != null && password != null) {
            signInWithEmail(email, password)
        } else {
            _errorMessage.value = "再試行するサインイン情報がありません"
        }
    }

    fun signUpWithEmail(email: String, password: String, confirmPassword: String) {
        lastSignUpEmail = email
        lastSignUpPassword = password
        lastSignUpConfirm = confirmPassword

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
                    // 成功時は再試行情報をクリア
                    lastSignUpEmail = null
                    lastSignUpPassword = null
                    lastSignUpConfirm = null
                } else {
                    _errorMessage.value = (result.errorMessage ?: "アカウント作成に失敗しました") + "。ネットワークエラーの場合は再試行してください。"
                }
            } catch (e: Exception) {
                _errorMessage.value = "アカウント作成に失敗しました: ${e.message}。ネットワークを確認して再試行してください。"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun retrySignUp() {
        val email = lastSignUpEmail
        val password = lastSignUpPassword
        val confirm = lastSignUpConfirm
        if (email != null && password != null && confirm != null) {
            signUpWithEmail(email, password, confirm)
        } else {
            _errorMessage.value = "再試行する登録情報がありません"
        }
    }

    // One Tap 用ユーティリティ（UI から呼び出す）
    fun getOneTapClient(context: Context): SignInClient = authRepository.getOneTapClient(context)

    fun buildBeginSignInRequest(): BeginSignInRequest = authRepository.buildBeginSignInRequest()

    // IDトークンで直接 Firebase にサインイン
    fun signInWithIdToken(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = authRepository.signInWithIdToken(idToken)
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

    @Deprecated("Use One Tap sign-in (signInWithIdToken) instead")
    fun signInWithGoogle(account: GoogleSignInAccount) {
        // 互換性保持のため残す（既存の GoogleSignInAccount を扱う場合）
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = account.idToken
                if (token != null) {
                    signInWithIdToken(token)
                } else {
                    _errorMessage.value = "Google ID トークンが取得できませんでした"
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
                    // メールが認証され次第、認証フローを進める
                    checkUserAuthFlow(currentUser)
                } else {
                    _errorMessage.value = "まだメールアドレスが認証されていません。メール内のリンクを確認してください。"
                }
            } catch (e: Exception) {
                _errorMessage.value = "認証状態の確認に失敗しました: ${e.message}。ネットワークを確認して再試行してください。"
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
                // 安全に型チェックして skills を取り出す
                val skills = if (userDoc != null) {
                    val raw = userDoc["skills"]
                    if (raw is List<*>) raw.mapNotNull { it as? String } else emptyList()
                } else {
                    emptyList()
                }
                onLoaded(skills)
            } catch (e: Exception) {
                _errorMessage.value = "得意なことの読み込みに失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 住所情報を更新
    fun updateAddress(prefecture: String, city: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _errorMessage.value = "ユーザーがログインしていません"
                    return@launch
                }
                authRepository.updateAddress(currentUser.uid, prefecture, city)
                _successMessage.value = "住所情報を更新しました"
            } catch (e: Exception) {
                _errorMessage.value = "住所情報の更新に失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 住所の公開設定を更新
    fun updateAddressVisibility(isPublic: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _errorMessage.value = "ユーザーがログインしていません"
                    return@launch
                }
                authRepository.updateAddressVisibility(currentUser.uid, isPublic)
                _successMessage.value = "住所の公開設定を更新しました"
            } catch (e: Exception) {
                _errorMessage.value = "公開設定の更新に失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ユーティリティメソッド
    // fun getGoogleSignInClient(): GoogleSignInClient = authRepository.getGoogleSignInClient()

    private fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun clearEmailVerificationSent() {
        _emailVerificationSent.value = false
    }

    // ユーザーデータを取得
    fun loadUserData(onLoaded: (UserData?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _errorMessage.value = "ユーザーがログインしていません"
                    onLoaded(null)
                    return@launch
                }
                val userDoc = authRepository.getUserDocument(currentUser.uid)
                if (userDoc != null) {
                    onLoaded(UserData(
                        userId = userDoc["userId"] as? String ?: "",
                        displayName = userDoc["displayName"] as? String ?: "",
                        email = userDoc["email"] as? String ?: "",
                        prefecture = userDoc["prefecture"] as? String ?: "",
                        city = userDoc["city"] as? String ?: "",
                        isAddressPublic = userDoc["isAddressPublic"] as? Boolean ?: false,
                        createdAt = userDoc["createdAt"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now(),
                        updatedAt = userDoc["updatedAt"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now(),
                        skills = (userDoc["skills"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    ))
                } else {
                    onLoaded(null)
                }
            } catch (e: Exception) {
                _errorMessage.value = "ユーザーデータの読み込みに失敗しました: ${e.message}"
                onLoaded(null)
            } finally {
                _isLoading.value = false
            }
        }
    }

}