package com.example.e_zuka.ui.auth

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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.e_zuka.ui.components.LoadingButton
import com.example.e_zuka.ui.components.ValidatedTextField
import com.example.e_zuka.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseUser

@Composable
fun UserNameVerificationScreen(
    user: FirebaseUser,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    var lastName by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastNameError by remember { mutableStateOf<String?>(null) }
    var firstNameError by remember { mutableStateOf<String?>(null) }
    var isFormValid by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // バリデーション関数
    val validateName: (String) -> String? = { name ->
        when {
            name.isBlank() -> "名前を入力してください"
            name.length < 1 -> "正しい名前を入力してください"
            name.length > 10 -> "名前は10文字以内で入力してください"
            !name.matches(Regex("^[ぁ-んァ-ヶ一-龯々〆〤ー]+$")) ->
                "名前は日本語（ひらがな、カタカナ、漢字）で入力してください"
            else -> null
        }
    }

    // リアルタイムバリデーション
    LaunchedEffect(lastName, firstName) {
        lastNameError = validateName(lastName)
        firstNameError = validateName(firstName)
        isFormValid = lastNameError == null && firstNameError == null &&
                lastName.isNotBlank() && firstName.isNotBlank()
    }

    // メッセージ表示処理
    LaunchedEffect(successMessage, errorMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
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
                    // ヘッダー
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "本名の確認",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "地域認証を行う前に、本名の確認が必要です\n正しい姓名を入力してください",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 重要事項の説明カード
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "重要な注意事項",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "• 必ず本名（戸籍上の正式な名前）を入力してください\n" +
                                                "• 偽名やニックネームの使用は禁止されています\n" +
                                                "• 後から変更することはできません\n" +
                                                "• 地域認証に本名が必要です",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ユーザー情報表示
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ログイン中のアカウント",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "メールアドレス: ${user.email ?: "未設定"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (user.displayName != null && user.displayName!!.isNotBlank()) {
                                Text(
                                    text = "現在の表示名: ${user.displayName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 姓の入力フィールド
                    ValidatedTextField(
                        value = lastName,
                        onValueChange = { lastName = it.trim() },
                        label = "姓（苗字）",
                        hint = "例: 田中",
                        error = lastNameError,
                        isRequired = true,
                        leadingIcon = Icons.Default.Person,
                        info = "戸籍上の正式な姓を入力してください",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { /* フォーカスを次へ */ }
                        ),
                        validateOnFocusChange = true,
                        validate = validateName
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 名の入力フィールド
                    ValidatedTextField(
                        value = firstName,
                        onValueChange = { firstName = it.trim() },
                        label = "名（下の名前）",
                        hint = "例: 太郎",
                        error = firstNameError,
                        isRequired = true,
                        leadingIcon = Icons.Default.Person,
                        info = "戸籍上の正式な名を入力してください",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (isFormValid) {
                                    val fullName = "$lastName $firstName"
                                    viewModel.updateDisplayName(fullName)
                                }
                            }
                        ),
                        validateOnFocusChange = true,
                        validate = validateName
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 確認ボタン
                    LoadingButton(
                        text = "本名を確認して次へ",
                        isLoading = isLoading,
                        onClick = {
                            if (isFormValid) {
                                val fullName = "$lastName $firstName"
                                viewModel.updateDisplayName(fullName)
                            }
                        },
                        enabled = isFormValid,
                        error = if (!isFormValid) "すべての項目を正しく入力してください" else null
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 説明カード
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "なぜ本名が必要なのか",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "• 地域認証では身元確認が必要です\n" +
                                                "• 地域コミュニティの安全性を保つため\n" +
                                                "• 責任あるサービス利用のため\n" +
                                                "• 個人情報は適切に保護されます",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ログアウトリンク
                    TextButton(
                        onClick = { viewModel.signOut() },
                        enabled = !isLoading
                    ) {
                        Text("ログアウト")
                    }
                }
            }
        }

        // Snackbar for messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { snackbarData ->
            Snackbar(
                action = {
                    TextButton(
                        onClick = {
                            snackbarData.dismiss()
                        }
                    ) {
                        Text("閉じる")
                    }
                }
            ) {
                Text(snackbarData.visuals.message)
            }
        }
    }
}