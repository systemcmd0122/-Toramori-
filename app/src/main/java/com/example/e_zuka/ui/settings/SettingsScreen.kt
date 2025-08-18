package com.example.e_zuka.ui.settings

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.e_zuka.ui.components.LoadingButton
import com.example.e_zuka.ui.components.PrivacyPolicyDialog
import com.example.e_zuka.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: FirebaseUser,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ユーザー情報の状態管理
    var currentUser by remember { mutableStateOf(user) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    // プロフィール更新用の状態
    var isUpdatingProfile by remember { mutableStateOf(false) }

    // 得意なこと・資格の編集用状態
    var skills by remember { mutableStateOf(user.metadata?.let { (user as? com.example.e_zuka.data.model.UserData)?.skills } ?: emptyList<String>()) }

    // 初回ロード時に得意なことを取得
    LaunchedEffect(Unit) {
        viewModel.loadSkills { loadedSkills ->
            skills = loadedSkills
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = "設定",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Section with enhanced UI
                EnhancedProfileCard(
                    user = currentUser,
                    isUpdatingProfile = isUpdatingProfile,
                    onProfileUpdate = { updatedUser: FirebaseUser ->
                        currentUser = updatedUser
                    },
                    onUpdateStart = { isUpdatingProfile = true },
                    onUpdateEnd = { isUpdatingProfile = false },
                    onShowMessage = { message: String ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(message)
                        }
                    }
                )

                // Email Verification Section
                AnimatedVisibility(
                    visible = currentUser.providerData.any { it.providerId == "password" } && !currentUser.isEmailVerified,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    EmailVerificationCard(
                        viewModel = viewModel,
                        isLoading = isLoading,
                        coroutineScope = coroutineScope,
                        onUserUpdate = { updatedUser: FirebaseUser ->
                            currentUser = updatedUser
                        },
                        onShowMessage = { message: String ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    )
                }

                // Account Settings Section
                SettingsSection(
                    title = "アカウント設定",
                    icon = Icons.Default.AccountBox
                ) {
                    // 表示名変更は上記のプロフィールカードで直接編集可能にしたので削除
                }

                // Privacy Settings Section
                SettingsSection(
                    title = "プライバシー設定",
                    icon = Icons.Default.Lock
                ) {
                    SettingItem(
                        title = "プライバシーポリシー",
                        subtitle = "当アプリのプライバシーポリシーを確認",
                        icon = Icons.Outlined.Info,
                        onClick = { showPrivacyPolicyDialog = true }
                    )
                }

                // 得意なこと・資格セクション
                SettingsSection(
                    title = "得意なこと・資格",
                    icon = Icons.Default.CheckCircle
                ) {
                    var showAddDialog by remember { mutableStateOf(false) }
                    var showEditDialog by remember { mutableStateOf(false) }
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    var skillToEdit by remember { mutableStateOf("") }
                    var skillToDeleteIndex by remember { mutableStateOf(-1) }
                    var editIndex by remember { mutableStateOf(-1) }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        skills.forEachIndexed { index, skill ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(skill, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    skillToEdit = skill
                                    editIndex = index
                                    showEditDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "編集")
                                }
                                IconButton(onClick = {
                                    skillToDeleteIndex = index
                                    showDeleteDialog = true
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "削除")
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("得意なことを追加")
                        }
                    }

                    // 追加ダイアログ
                    if (showAddDialog) {
                        SkillInputDialog(
                            title = "得意なことを追加",
                            initialValue = "",
                            onConfirm = { value ->
                                if (value.isNotBlank()) {
                                    viewModel.addSkill(value) { success ->
                                        if (success) {
                                            skills = skills + value
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("得意なことを追加しました")
                                            }
                                        } else {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("得意なことの追加に失敗しました")
                                            }
                                        }
                                    }
                                }
                                showAddDialog = false
                            },
                            onDismiss = { showAddDialog = false }
                        )
                    }
                    // 編集ダイアログ
                    if (showEditDialog) {
                        SkillInputDialog(
                            title = "得意なことを編集",
                            initialValue = skillToEdit,
                            onConfirm = { value ->
                                if (value.isNotBlank() && editIndex >= 0) {
                                    skills = skills.toMutableList().apply { set(editIndex, value) }
                                    viewModel.updateSkills(skills)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("得意なことを編集しました")
                                    }
                                }
                                showEditDialog = false
                            },
                            onDismiss = { showEditDialog = false }
                        )
                    }
                    // 削除確認ダイアログ
                    if (showDeleteDialog && skillToDeleteIndex >= 0) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("削除の確認") },
                            text = { Text("本当に削除しますか？") },
                            confirmButton = {
                                TextButton(onClick = {
                                    skills = skills.toMutableList().apply { removeAt(skillToDeleteIndex) }
                                    viewModel.updateSkills(skills)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("削除しました")
                                    }
                                    showDeleteDialog = false
                                }) {
                                    Text("削除")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("キャンセル")
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Logout Button
                LoadingButton(
                    text = "ログアウト",
                    isLoading = isLoading,
                    onClick = { showLogoutConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Dialogs
    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(
            onDismiss = { showPrivacyPolicyDialog = false }
        )
    }

    if (showLogoutConfirmDialog) {
        LogoutConfirmDialog(
            onDismiss = { showLogoutConfirmDialog = false },
            onConfirm = {
                viewModel.signOut()
                showLogoutConfirmDialog = false
            }
        )
    }
}

@Composable
private fun EnhancedProfileCard(
    user: FirebaseUser,
    isUpdatingProfile: Boolean,
    onProfileUpdate: (FirebaseUser) -> Unit,
    onUpdateStart: () -> Unit,
    onUpdateEnd: () -> Unit,
    onShowMessage: (String) -> Unit
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
        modifier = Modifier.fillMaxWidth(),
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
                    contentDescription = null,
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
                        contentDescription = null,
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
                        contentDescription = null,
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                    contentDescription = null,
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
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
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
    )
}

@Composable
private fun LogoutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("ログアウトの確認")
        },
        text = {
            Text("本当にログアウトしますか？")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("ログアウト")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

// ダイアログ用Composable
@Composable
fun SkillInputDialog(
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                TextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = { Text("例: 日曜大工、電気工事士") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }, enabled = value.isNotBlank()) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
