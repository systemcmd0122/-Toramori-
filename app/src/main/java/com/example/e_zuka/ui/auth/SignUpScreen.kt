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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun SignUpScreen(
    viewModel: AuthViewModel,
    onNavigateToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var isFormValid by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val emailVerificationSent by viewModel.emailVerificationSent.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

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
            !value.matches(Regex(".*[A-Z].*")) -> "大文字を含めてください"
            !value.matches(Regex(".*[a-z].*")) -> "小文字を含めてください"
            !value.matches(Regex(".*\\d.*")) -> "数字を含めてください"
            else -> null
        }
    }

    val validateConfirmPassword: (String) -> String? = { value ->
        when {
            value.isBlank() -> "確認用パスワードを入力してください"
            value != password -> "パスワードが一致しません"
            else -> null
        }
    }

    // リアルタイムバリデーション
    LaunchedEffect(email, password, confirmPassword) {
        emailError = validateEmail(email)
        passwordError = validatePassword(password)
        confirmPasswordError = validateConfirmPassword(confirmPassword)
        isFormValid = emailError == null && passwordError == null && confirmPasswordError == null &&
                email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()
    }

    // メッセージ表示
    val errorMessage by viewModel.errorMessage.collectAsState()
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Google Sign-In設定
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
                // Handle Google Sign-In error
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
                        text = "新規登録",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    if (emailVerificationSent) {
                        EmailVerificationSentCard(
                            onNavigateToSignIn = {
                                viewModel.clearEmailVerificationSent()
                                onNavigateToSignIn()
                            }
                        )
                    } else {
                        // メールアドレス入力
                        ValidatedTextField(
                            value = email,
                            onValueChange = { email = it.trim() },
                            label = "メールアドレス",
                            hint = "example@example.com",
                            error = emailError,
                            isRequired = true,
                            leadingIcon = Icons.Default.Email,
                            info = "メールアドレス認証が必要になります",
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
                            hint = "6文字以上の英数字",
                            error = passwordError,
                            isRequired = true,
                            leadingIcon = Icons.Default.Lock,
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible }
                                ) {
                                    Icon(
                                        if (passwordVisible) {
                                            Icons.Default.Visibility
                                        } else {
                                            Icons.Default.VisibilityOff
                                        },
                                        contentDescription = if (passwordVisible) {
                                            "パスワードを隠す"
                                        } else {
                                            "パスワードを表示"
                                        }
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            info = "大文字、小文字、数字を含む6文字以上",
                            validateOnFocusChange = true,
                            validate = validatePassword
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // パスワード確認入力
                        ValidatedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = "パスワード（確認）",
                            hint = "同じパスワードを入力",
                            error = confirmPasswordError,
                            isRequired = true,
                            leadingIcon = Icons.Default.Lock,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        confirmPasswordVisible = !confirmPasswordVisible
                                    }
                                ) {
                                    Icon(
                                        if (confirmPasswordVisible) {
                                            Icons.Default.Visibility
                                        } else {
                                            Icons.Default.VisibilityOff
                                        },
                                        contentDescription = if (confirmPasswordVisible) {
                                            "パスワードを隠す"
                                        } else {
                                            "パスワードを表示"
                                        }
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (isFormValid) {
                                        viewModel.signUpWithEmail(
                                            email,
                                            password,
                                            confirmPassword
                                        )
                                    }
                                }
                            ),
                            validateOnFocusChange = true,
                            validate = validateConfirmPassword
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // アカウント作成ボタン
                        LoadingButton(
                            text = "アカウント作成",
                            isLoading = isLoading,
                            onClick = {
                                if (isFormValid) {
                                    viewModel.signUpWithEmail(
                                        email,
                                        password,
                                        confirmPassword
                                    )
                                }
                            },
                            enabled = isFormValid,
                            error = if (!isFormValid) {
                                "すべての項目を正しく入力してください"
                            } else null
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Googleログインボタン
                        LoadingButton(
                            text = "Googleでログイン",
                            isLoading = isLoading,
                            onClick = {
                                val signInIntent = viewModel.getGoogleSignInClient().signInIntent
                                googleSignInLauncher.launch(signInIntent)
                            },
                            enabled = !isLoading,
                            isOutlined = true
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // サインインリンク
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "既にアカウントをお持ちの場合は",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = {
                                    viewModel.clearEmailVerificationSent()
                                    onNavigateToSignIn()
                                },
                                enabled = !isLoading
                            ) {
                                Text("ログイン")
                            }
                        }
                    }
                }
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { snackbarData ->
            Snackbar(
                action = {
                    TextButton(onClick = { snackbarData.dismiss() }) {
                        Text("閉じる")
                    }
                }
            ) {
                Text(snackbarData.visuals.message)
            }
        }
    }
}

@Composable
private fun EmailVerificationSentCard(
    onNavigateToSignIn: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "アカウント作成完了",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "確認メールを送信しました。メール内のリンクをクリックしてメールアドレスを認証後、ログインしてください。",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateToSignIn,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("ログイン画面へ")
            }
        }
    }
}