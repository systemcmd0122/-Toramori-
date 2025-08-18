package com.example.e_zuka.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.e_zuka.data.model.RegionAuthState
import com.example.e_zuka.ui.components.LoadingButton
import com.example.e_zuka.ui.components.ValidatedTextField
import com.example.e_zuka.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseUser

@Composable
fun RegionVerificationScreen(
    user: FirebaseUser,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    var regionCode by remember { mutableStateOf("") }
    var regionCodeError by remember { mutableStateOf<String?>(null) }
    var isFormValid by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val regionAuthState by viewModel.regionAuthState.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // リアルタイムバリデーション関数
    val validateRegionCode: (String) -> String? = { code ->
        when {
            code.isBlank() -> "地域認証コードを入力してください"
            code.length < 6 -> "地域認証コードは6文字以上で入力してください"
            !code.matches(Regex("^[A-Z0-9]+$")) ->
                "地域認証コードは英大文字と数字のみ使用できます"
            else -> null
        }
    }

    // リアルタイムバリデーション
    LaunchedEffect(regionCode) {
        regionCodeError = validateRegionCode(regionCode)
        isFormValid = regionCodeError == null && regionCode.isNotBlank()
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

    // 認証完了時のアニメーション用状態
    var isVerificationComplete by remember { mutableStateOf(false) }

    // 地域認証成功の判定とフィードバック
    LaunchedEffect(regionAuthState) {
        when (regionAuthState) {
            is RegionAuthState.Verified -> {
                isVerificationComplete = true
                snackbarHostState.showSnackbar("地域認証が完了しました！ホーム画面に移動します...")
            }
            is RegionAuthState.Error -> {
                snackbarHostState.showSnackbar("地域認証に失敗しました: ${(regionAuthState as RegionAuthState.Error).message}")
            }
            else -> {
                // その他の状態では何もしない
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
                    // ヘッダー - 地域認証状態に応じてアイコンを変更
                    when (regionAuthState) {
                        is RegionAuthState.Verified -> {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        is RegionAuthState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(64.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp
                            )
                        }
                        else -> {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = when (regionAuthState) {
                            is RegionAuthState.Verified -> "認証完了"
                            is RegionAuthState.Loading -> "地域認証中..."
                            else -> "地域認証"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when (regionAuthState) {
                            is RegionAuthState.Verified -> {
                                "地域認証が完了しました\nホーム画面に移動します"
                            }
                            is RegionAuthState.Loading -> {
                                "地域認証コードを確認中です\nしばらくお待ちください"
                            }
                            else -> {
                                "アプリをご利用いただくには\n地域認証コードが必要です"
                            }
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 認証完了時以外は説明カードを表示
                    if (!isVerificationComplete) {
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
                                            text = "地域認証コードについて",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "• 自治体または地域代表者から配布されます\n" +
                                                    "• 例：「佐土原地域用：TORA2025」\n" +
                                                    "• お住まいの地域のコードをご入力ください",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // ユーザー情報カード
                        UserInfoCard(user = user)

                        Spacer(modifier = Modifier.height(24.dp))

                        // 地域認証コード入力フィールド（ローディング中以外で表示）
                        if (regionAuthState !is RegionAuthState.Loading) {
                            ValidatedTextField(
                                value = regionCode,
                                onValueChange = { code ->
                                    regionCode = code.uppercase().trim()
                                },
                                label = "地域認証コード",
                                hint = "例: TORA2025",
                                error = regionCodeError,
                                isRequired = true,
                                leadingIcon = Icons.Default.Security,
                                info = "英大文字と数字で構成された6桁のコード",
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (isFormValid) {
                                            viewModel.verifyRegionCode(regionCode)
                                        }
                                    }
                                ),
                                validateOnFocusChange = true,
                                validate = validateRegionCode
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // 認証ボタン
                            LoadingButton(
                                text = "地域認証を実行",
                                isLoading = isLoading,
                                onClick = {
                                    if (isFormValid) {
                                        viewModel.verifyRegionCode(regionCode)
                                    }
                                },
                                enabled = isFormValid && !isLoading,
                                error = if (!isFormValid) {
                                    "正しい地域認証コードを入力してください"
                                } else null
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // ログアウトリンク
                            TextButton(
                                onClick = { viewModel.signOut() },
                                enabled = !isLoading
                            ) {
                                Text("ログアウト")
                            }
                        } else {
                            // ローディング中のメッセージ
                            LoadingMessage()
                        }
                    } else {
                        // 認証完了カード
                        VerificationCompleteCard(regionAuthState = regionAuthState as RegionAuthState.Verified)
                    }
                }
            }
        }

        // Snackbarホスト
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
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
private fun UserInfoCard(user: FirebaseUser) {
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
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ログイン中のユーザー",
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
            Text(
                text = "表示名: ${user.displayName ?: "未設定"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun LoadingMessage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "地域認証コードを確認しています...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "しばらくお待ちください",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun VerificationCompleteCard(regionAuthState: RegionAuthState.Verified) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "地域認証完了",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "認証地域: ${regionAuthState.regionData.regionName}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "地域認証が正常に完了しました。\nアプリの全機能をご利用いただけます。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                strokeWidth = 2.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "ホーム画面へ移動中...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}