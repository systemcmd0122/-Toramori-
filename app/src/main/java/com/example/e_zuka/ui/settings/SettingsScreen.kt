package com.example.e_zuka.ui.settings

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.e_zuka.ui.components.AppTopBar
import com.example.e_zuka.ui.components.LoadingButton
import com.example.e_zuka.ui.components.PrivacyPolicyDialog
import com.example.e_zuka.ui.settings.SettingComponents.ConfirmationDialog
import com.example.e_zuka.ui.settings.SettingComponents.SkillDialog
import com.example.e_zuka.viewmodel.AuthViewModel
import com.example.e_zuka.viewmodel.ThemeSettingsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: FirebaseUser,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    themeViewModel: ThemeSettingsViewModel? = null
) {
    // themeViewModel をオプション引数として扱い、null の場合は内部で取得
    val resolvedThemeViewModel: ThemeSettingsViewModel = themeViewModel
        ?: viewModel(factory = ThemeSettingsViewModel.Factory(LocalContext.current))

    val isLoading by viewModel.isLoading.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ユーザー情報の状態管理
    var currentUser by remember { mutableStateOf(user) }
    var dialogState by remember { mutableStateOf<SettingsDialogState>(SettingsDialogState.None) }

    // プロフィール更新用の状態
    var isUpdatingProfile by remember { mutableStateOf(false) }

    // 得意なこと・資格の管理用のmutableStateList
    val skills = remember { mutableStateListOf<String>() }

    // メッセージの監視
    LaunchedEffect(viewModel) {
        viewModel.errorMessage.collect { message ->
            message?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearError()
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.successMessage.collect { message ->
            message?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearSuccessMessage()
            }
        }
    }

    // 初回ロード時に得意なことを取得
    LaunchedEffect(Unit) {
        viewModel.loadSkills { loadedSkills ->
            skills.clear()
            skills.addAll(loadedSkills)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = "設定画面" },
        topBar = {
            AppTopBar(titleText = "設定")
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // プロフィールカード
            EnhancedProfileCard(
                user = currentUser,
                isUpdatingProfile = isUpdatingProfile,
                onProfileUpdate = { updatedUser ->
                    currentUser = updatedUser
                },
                onUpdateStart = { isUpdatingProfile = true },
                onUpdateEnd = { isUpdatingProfile = false },
                onShowMessage = { message ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                }
            )

            // メール認証カード（必要な場合）
            if (currentUser.providerData.any { it.providerId == "password" } && !currentUser.isEmailVerified) {
                EmailVerificationCard(
                    viewModel = viewModel,
                    isLoading = isLoading,
                    coroutineScope = coroutineScope,
                    onUserUpdate = { updatedUser ->
                        currentUser = updatedUser
                    },
                    onShowMessage = { message ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(message)
                        }
                    }
                )
            }

            // 表示設定カード
            AppearanceSettingsCard(
                themeViewModel = resolvedThemeViewModel,
                modifier = Modifier.semantics { contentDescription = "表示設定" }
            )

            // 得意なこと・資格セクション
            SettingsSection(
                title = "得意なこと・資格",
                icon = Icons.Default.CheckCircle
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    skills.forEachIndexed { index, skill ->
                        SkillItem(
                            skill = skill,
                            onEdit = {
                                dialogState = SettingsDialogState.EditSkill(index, skill)
                            },
                            onDelete = {
                                dialogState = SettingsDialogState.DeleteSkill(index, skill)
                            },
                            modifier = Modifier.semantics { contentDescription = "得意なこと: $skill" }
                        )
                    }
                    OutlinedButton(
                        onClick = { dialogState = SettingsDialogState.AddSkill() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "得意なことを追加" }
                    ) {
                        Text("得意なことを追加")
                    }
                }
            }

            // 設定セクション
            SettingsSection(
                title = "その他の設定",
                icon = Icons.Default.Settings
            ) {
                SettingItem(
                    title = "プライバシーポリシー",
                    subtitle = "アプリのプライバシーポリシーを確認",
                    icon = Icons.Default.Lock,
                    onClick = { dialogState = SettingsDialogState.PrivacyPolicy },
                    modifier = Modifier.semantics { contentDescription = "プライバシーポリシー" }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ログアウトボタン
            LoadingButton(
                text = "ログアウト",
                isLoading = isLoading,
                onClick = { dialogState = SettingsDialogState.LogoutConfirm },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "ログアウト" }
            )
        }

        // ダイアログの表示
        when (val state = dialogState) {
            is SettingsDialogState.AddSkill -> {
                SkillDialog(
                    title = "得意なことを追加",
                    initialValue = state.initialValue,
                    onConfirm = { value: String ->
                        val trimmed = value.trim()
                        if (trimmed.isNotBlank()) {
                            skills.add(trimmed)
                            viewModel.updateSkills(skills.toList())
                        }
                        dialogState = SettingsDialogState.None
                    },
                    onDismiss = { dialogState = SettingsDialogState.None },
                    validateSkill = { skill: String ->
                        val trimmed = skill.trim()
                        when {
                            trimmed.isBlank() -> "得意なことを入力してください"
                            trimmed.length > 50 -> "50文字以内で入力してください"
                            skills.any { it.equals(trimmed, ignoreCase = true) } -> "既に登録されています"
                            else -> null
                        }
                    }
                )
            }
            is SettingsDialogState.EditSkill -> {
                SkillDialog(
                    title = "得意なことを編集",
                    initialValue = state.currentValue,
                    onConfirm = { value: String ->
                        val trimmed = value.trim()
                        if (trimmed.isNotBlank()) {
                            skills[state.skillId] = trimmed
                            viewModel.updateSkills(skills.toList())
                        }
                        dialogState = SettingsDialogState.None
                    },
                    onDismiss = { dialogState = SettingsDialogState.None },
                    validateSkill = { skill: String ->
                        val trimmed = skill.trim()
                        when {
                            trimmed.isBlank() -> "得意なことを入力してください"
                            trimmed.length > 50 -> "50文字以内で入力してください"
                            skills.anyIndexed { i, s -> i != state.skillId && s.equals(trimmed, ignoreCase = true) } -> "既に登録されています"
                            else -> null
                        }
                    }
                )
            }
            is SettingsDialogState.DeleteSkill -> {
                ConfirmationDialog(
                    title = "削除の確認",
                    message = "「${state.skillName}」を削除しますか？",
                    confirmText = "削除",
                    onConfirm = {
                        skills.removeAt(state.skillId)
                        viewModel.updateSkills(skills.toList())
                        dialogState = SettingsDialogState.None
                    },
                    onDismiss = { dialogState = SettingsDialogState.None }
                )
            }
            SettingsDialogState.LogoutConfirm -> {
                ConfirmationDialog(
                    title = "ログアウトの確認",
                    message = "本当にログアウトしますか？",
                    confirmText = "ログアウト",
                    onConfirm = {
                        viewModel.signOut()
                        dialogState = SettingsDialogState.None
                    },
                    onDismiss = { dialogState = SettingsDialogState.None }
                )
            }
            SettingsDialogState.PrivacyPolicy -> {
                PrivacyPolicyDialog(
                    onDismiss = { dialogState = SettingsDialogState.None }
                )
            }
            SettingsDialogState.None -> {
                // 何も表示しない
            }
        }
    }
}

@Composable
private fun SkillItem(
    skill: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = skill,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onEdit) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "編集",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Close,
                contentDescription = "削除",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun EnhancedProfileCard(
    user: FirebaseUser,
    isUpdatingProfile: Boolean,
    onProfileUpdate: (FirebaseUser) -> Unit,
    onUpdateStart: () -> Unit,
    onUpdateEnd: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditingDisplayName by remember { mutableStateOf(false) }
    var tempDisplayName by remember { mutableStateOf(user.displayName ?: "") }
    var displayNameError by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    // アニメーション
    val profileAlpha by animateFloatAsState(
        targetValue = if (isUpdatingProfile) 0.7f else 1f,
        animationSpec = tween(300),
        label = "profileAlpha"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .alpha(profileAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // プロフィール画像
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "プロフィール画像",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )

                    // 更新中のローディング表示
                    if (isUpdatingProfile) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.3f)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(28.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 表示名の編集可能フィールド
            if (isEditingDisplayName) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextField(
                        value = tempDisplayName,
                        onValueChange = { value: String ->
                            tempDisplayName = value
                            displayNameError = when {
                                value.isBlank() -> "表示名を入力してください"
                                value.length > 20 -> "表示名は20文字以内で入力してください"
                                else -> null
                            }
                        },
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .fillMaxWidth(),
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        singleLine = true,
                        isError = displayNameError != null,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    displayNameError?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                isEditingDisplayName = false
                                tempDisplayName = user.displayName ?: ""
                                displayNameError = null
                            }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "キャンセル",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }

                        IconButton(
                            onClick = {
                                if (tempDisplayName.isNotBlank() && displayNameError == null && tempDisplayName != user.displayName) {
                                    onUpdateStart()
                                    coroutineScope.launch {
                                        try {
                                            user.updateProfile(
                                                com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                                    .setDisplayName(tempDisplayName)
                                                    .build()
                                            ).addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    user.reload().addOnCompleteListener { reloadTask ->
                                                        onUpdateEnd()
                                                        if (reloadTask.isSuccessful) {
                                                            // FirebaseAuthから最新のユーザー情報を取得
                                                            val updatedUser = FirebaseAuth.getInstance().currentUser
                                                            if (updatedUser != null) {
                                                                onProfileUpdate(updatedUser)
                                                            }
                                                            onShowMessage("表示名を更新しました")
                                                        } else {
                                                            onShowMessage("表示名の更新に失敗しました")
                                                        }
                                                    }
                                                } else {
                                                    onUpdateEnd()
                                                    onShowMessage("表示名の更新に失敗しました")
                                                }
                                            }
                                            isEditingDisplayName = false
                                        } catch (e: Exception) {
                                            onUpdateEnd()
                                            onShowMessage("表示名の更新に失敗しました: ${e.message}")
                                        }
                                    }
                                }
                            },
                            enabled = tempDisplayName.isNotBlank() && displayNameError == null && tempDisplayName != user.displayName
                        ) {
                            Icon(
                                Icons.Default.Done,
                                contentDescription = "保存",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                LaunchedEffect(isEditingDisplayName) {
                    if (isEditingDisplayName) {
                        focusRequester.requestFocus()
                    }
                }
            } else {
                // 通常の表示名表示（クリックで編集モード）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = !isUpdatingProfile) {
                        isEditingDisplayName = true
                        tempDisplayName = user.displayName ?: ""
                    }
                ) {
                    Text(
                        text = user.displayName ?: "未設定",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    if (!isUpdatingProfile) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "編集",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = user.email ?: "未設定",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 認証ステータス
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(
                    if (user.isEmailVerified) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (user.isEmailVerified) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (user.isEmailVerified) "認証済み" else "未認証",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (user.isEmailVerified) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}

@Composable
private fun EmailVerificationCard(
    viewModel: AuthViewModel,
    isLoading: Boolean,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onUserUpdate: (FirebaseUser) -> Unit,
    onShowMessage: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "メール認証警告アイコン",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "メールアドレスの認証が必要です",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "アプリの全機能を利用するには、メールアドレスの認証が必要です。" +
                        "確認メールが届いていない場合は、再送信してください。",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.sendEmailVerification()
                        onShowMessage("確認メールを送信しました")
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "確認メールを再送信",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("再送信")
                }

                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.checkEmailVerification()
                            // 認証状態が更新された場合の処理
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            if (currentUser != null) {
                                onUserUpdate(currentUser)
                                if (currentUser.isEmailVerified) {
                                    onShowMessage("メールアドレスが認証されました")
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "メール認証状態を更新",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("更新")
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = "$title アイコン",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(
                icon,
                contentDescription = "$title アイコン",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "次へ",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
    )
}

@Composable
private fun ErrorStateView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "エラー",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.Button(
                    onClick = onRetry,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "再試行",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    message: String,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Groups,
                contentDescription = "メンバーがいません",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.OutlinedButton(
                onClick = onRefresh
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "メンバー一覧を更新",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("更新")
            }
        }
    }
}

fun <T> List<T>.anyIndexed(predicate: (Int, T) -> Boolean): Boolean {
    for (i in indices) {
        if (predicate(i, this[i])) return true
    }
    return false
}
