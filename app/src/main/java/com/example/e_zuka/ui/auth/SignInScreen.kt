package com.example.e_zuka.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.e_zuka.ui.components.LoadingButton
import com.example.e_zuka.ui.components.ValidatedTextField
import com.example.e_zuka.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(
    viewModel: AuthViewModel,
    onNavigateToSignUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isFormValid by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // バリデーション関数
    val validateEmail: (String) -> String? = { value ->
        when {
            value.isBlank() -> "メールアドレスを入力してください"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches() ->
                "有効なメールアドレスを入力してください"
            else -> null
        }
    }
    val validatePassword: (String) -> String? = { value ->
        when {
            value.isBlank() -> "パスワードを入力してください"
            value.length < 6 -> "パスワードは6文字以上で入力してください"
            else -> null
        }
    }

    // リアルタイムバリデーション
    LaunchedEffect(email, password) {
        emailError = validateEmail(email)
        passwordError = validatePassword(password)
        isFormValid = emailError == null && passwordError == null &&
                email.isNotBlank() && password.isNotBlank()
    }

    // エラーメッセージ表示
    val errorMessage by viewModel.errorMessage.collectAsState()
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Google Sign-Inの設定
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    viewModel.signInWithGoogle(account)
                }
            } catch (_: ApiException) {
                // suspend関数はコルーチン内で呼ぶ
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Googleサインインに失敗しました")
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ログイン",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // 情報カード
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "メール認証について",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "メール/パスワードでのアカウント作成後は、メールアドレスの認証が必要です。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // メールアドレス入力
                    ValidatedTextField(
                        value = email,
                        onValueChange = { email = it.trim() },
                        label = "メールアドレス",
                        hint = "example@example.com",
                        error = emailError,
                        isRequired = true,
                        leadingIcon = Icons.Default.Email,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        validateOnFocusChange = true,
                        validate = validateEmail
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // パスワード入力
                    ValidatedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "パスワード",
                        hint = "6文字以上",
                        error = passwordError,
                        isRequired = true,
                        leadingIcon = Icons.Default.Lock,
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "パスワード非表示" else "パスワード表示"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        validateOnFocusChange = true,
                        validate = validatePassword
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ログインボタン
                    LoadingButton(
                        text = "ログイン",
                        isLoading = isLoading,
                        onClick = {
                            viewModel.signInWithEmail(email, password)
                        },
                        enabled = isFormValid && !isLoading,
                        error = if (emailError != null) emailError else passwordError
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Googleサインインボタン
                    LoadingButton(
                        text = "Googleでログイン",
                        isLoading = isLoading,
                        onClick = {
                            val intent = viewModel.getGoogleSignInClient().signInIntent
                            googleSignInLauncher.launch(intent)
                        },
                        enabled = !isLoading,
                        isOutlined = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 新規登録への導線
                    TextButton(onClick = onNavigateToSignUp, modifier = Modifier.semantics { contentDescription = "新規登録へ" }) {
                        Text("アカウントをお持ちでない方はこちら")
                    }
                }
            }
        }
        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}