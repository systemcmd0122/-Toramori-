package com.example.e_zuka.ui.members

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.e_zuka.data.model.RegionAuthState
import com.example.e_zuka.data.model.RegionMemberData
import com.example.e_zuka.ui.components.AppTopBar
import com.example.e_zuka.viewmodel.AuthViewModel
import com.example.e_zuka.viewmodel.RegionMembersViewModel
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

// 補完的な状態管理用の変数を追加
private var selectedMemberName: String? by mutableStateOf(null)
private var showSkillsDialog: Boolean by mutableStateOf(false)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun RegionMembersScreen(
    user: FirebaseUser,
    authViewModel: AuthViewModel,
    membersViewModel: RegionMembersViewModel,
    modifier: Modifier = Modifier
) {
    val regionAuthState by authViewModel.regionAuthState.collectAsState()
    val members by membersViewModel.members.collectAsState()
    val isLoading by membersViewModel.isLoading.collectAsState()
    val errorMessage by membersViewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // コルーチンスコープの初期化
    val coroutineScope = rememberCoroutineScope()

    // メンバー情報の初回読み込み
    LaunchedEffect(regionAuthState) {
        if (regionAuthState is RegionAuthState.Verified) {
            membersViewModel.loadRegionMembers((regionAuthState as RegionAuthState.Verified).regionData.codeId)
        }
    }

    // エラーメッセージの表示
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            membersViewModel.clearError()
        }
    }

    // 検索結果のフィルタリング
    val filteredMembers = remember(members, searchQuery) {
        if (searchQuery.isBlank()) {
            members
        } else {
            members.filter { member ->
                member.displayName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // skillsダイアログ用状態
    var showProfileDialog by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf<RegionMemberData?>(null) }
    var selectedSkills by remember { mutableStateOf<List<String>>(emptyList()) }
    // userDocument用の状態
    var selectedUserDocument: Map<String, Any>? by remember { mutableStateOf(null) }

    // スキルキャッシュ
    val skillsCache = remember { mutableStateMapOf<String, List<String>>() }

    Box(modifier = modifier.fillMaxSize()) {
        // 標準化したTopBarを使うためにScaffoldでラップ
        Scaffold(
            topBar = {
                AppTopBar(
                    titleText = "地域のみなさん",
                    actions = {
                        IconButton(
                            onClick = {
                                (regionAuthState as? RegionAuthState.Verified)?.let {
                                    membersViewModel.loadRegionMembers(it.regionData.codeId)
                                }
                            },
                            enabled = !isLoading
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "更新"
                            )
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            when (regionAuthState) {
                is RegionAuthState.Verified -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(
                                top = 0.dp, // 追加のトップパディングは不要
                                start = 16.dp,
                                end = 16.dp
                            )
                    ) {
                        // 地域情報カード
                        RegionInfoCard(
                            regionName = (regionAuthState as RegionAuthState.Verified).regionData.regionName,
                            memberCount = members.size,
                            isLoading = isLoading
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 検索バー
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { newQuery -> searchQuery = newQuery },
                            onSearch = { _ ->
                                searchActive = false
                                keyboardController?.hide()
                            },
                            active = searchActive,
                            onActiveChange = { isActive ->
                                searchActive = isActive
                                if (!isActive) keyboardController?.hide()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = true,
                            placeholder = {
                                Text(
                                    text = "メンバーを検索...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "検索アイコン",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            content = {
                                if (filteredMembers.isNotEmpty()) {
                                    filteredMembers.take(3).forEach { member ->
                                        ListItem(
                                            headlineContent = {
                                                Text(
                                                    text = member.displayName,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            },
                                            leadingContent = {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = "ユーザーアイコン",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            modifier = Modifier.clickable {
                                                searchQuery = member.displayName
                                                searchActive = false
                                                keyboardController?.hide()
                                            }
                                        )
                                    }
                                }
                            },
                            tonalElevation = 0.dp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // メンバー一覧
                        MembersList(
                            members = filteredMembers,
                            currentUserId = user.uid,
                            isLoading = isLoading,
                            onRefresh = {
                                membersViewModel.loadRegionMembers((regionAuthState as RegionAuthState.Verified).regionData.codeId)
                            },
                            onMemberClick = { member ->
                                selectedMember = member
                                val cached = skillsCache[member.userId]
                                if (cached != null) {
                                    selectedSkills = cached
                                } else {
                                    membersViewModel.getUserSkills(member.userId) { skills ->
                                        skillsCache[member.userId] = skills
                                        selectedSkills = skills
                                    }
                                }
                                // ユーザードキュメントを取得
                                coroutineScope.launch {
                                    selectedUserDocument = membersViewModel.getUserDocument(member.userId)
                                }
                                showProfileDialog = true
                            },
                            onShowSkills = { memberName, skills ->
                                selectedMemberName = memberName
                                selectedSkills = skills
                                showSkillsDialog = true
                            },
                            membersViewModel = membersViewModel,
                            skillsCache = skillsCache
                        )
                    }
                }
                is RegionAuthState.Loading -> {
                    LoadingStateView("地域情報を確認中...")
                }
                is RegionAuthState.Error -> {
                    ErrorStateView(
                        message = "地域認証エラー: ${(regionAuthState as RegionAuthState.Error).message}",
                        onRetry = {
                            authViewModel.checkRegionAuthStatus()
                        }
                    )
                }
                else -> {
                    ErrorStateView(
                        message = "地域認証が必要です",
                        onRetry = {
                            authViewModel.checkRegionAuthStatus()
                        }
                    )
                }
            }
        }

        // 更新ボタン（フローティングアクションボタン）
        if (regionAuthState is RegionAuthState.Verified && !isLoading) {
            FloatingActionButton(
                onClick = {
                    membersViewModel.loadRegionMembers((regionAuthState as RegionAuthState.Verified).regionData.codeId)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "更新",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // プロフィールダイアログ
        if (showProfileDialog && selectedMember != null) {
            val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN) }
            AlertDialog(
                onDismissRequest = {
                    showProfileDialog = false
                    selectedMember = null
                    selectedUserDocument = null
                },
                icon = {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = "${selectedMember?.displayName}さんのプロフィール",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 参加情報カード
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "参加情報",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text = "参加日: ${selectedMember?.joinedAt?.toDate()?.let { dateFormatter.format(it) } ?: "不明"}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // 住所情報（公開設定が有効な場合のみ表示）
                        if (selectedUserDocument?.get("isAddressPublic") as? Boolean == true) {
                            val prefecture = selectedUserDocument?.get("prefecture") as? String
                            val city = selectedUserDocument?.get("city") as? String
                            if (!prefecture.isNullOrBlank() && !city.isNullOrBlank()) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "住所",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        Text(
                                            text = "$prefecture $city",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        // 得意なことカード
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "得意なこと",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                                if (selectedSkills.isNotEmpty()) {
                                    selectedSkills.forEach { skill ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = skill,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "得意なことは未登録です",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showProfileDialog = false
                        selectedMember = null
                        selectedUserDocument = null
                    }) {
                        Text("閉じる")
                    }
                }
            )
        }

        // スキル一覧ダイアログ
        if (showSkillsDialog && selectedMemberName != null) {
            AlertDialog(
                onDismissRequest = {
                    showSkillsDialog = false
                    selectedMemberName = null
                },
                title = { Text("${selectedMemberName}さんの得意なこと") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (selectedSkills.isNotEmpty()) {
                            selectedSkills.forEach { skill ->
                                Text("・$skill", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            Text(
                                text = "得意なことは未登録です",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showSkillsDialog = false
                        selectedMemberName = null
                    }) {
                        Text("閉じる")
                    }
                }
            )
        }
    }
}

@Composable
private fun RegionInfoCard(
    regionName: String,
    memberCount: Int,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 地域アイコン
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "地域アイコン",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 地域情報
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = regionName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = "メンバー数アイコン",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "読み込み中...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Text(
                            text = "${memberCount}人のメンバー",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MembersList(
    members: List<RegionMemberData>,
    currentUserId: String,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onMemberClick: (RegionMemberData) -> Unit,
    onShowSkills: (String, List<String>) -> Unit,
    membersViewModel: RegionMembersViewModel,
    skillsCache: MutableMap<String, List<String>>,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !isLoading,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier.fillMaxSize()
    ) {
        if (members.isEmpty()) {
            EmptyStateView(
                message = "この地域にはまだメンバーがいません",
                onRefresh = onRefresh
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp), // FABのためのパディング
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = members,
                    key = { it.userId }
                ) { member ->
                    MemberListItem(
                        member = member,
                        isCurrentUser = member.userId == currentUserId,
                        onClick = { onMemberClick(member) },
                        onShowSkills = { name, skills -> onShowSkills(name, skills) },
                        membersViewModel = membersViewModel,
                        skillsCache = skillsCache
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberListItem(
    member: RegionMemberData,
    isCurrentUser: Boolean,
    onClick: () -> Unit = {},
    onShowSkills: (String, List<String>) -> Unit,
    membersViewModel: RegionMembersViewModel,
    skillsCache: MutableMap<String, List<String>>,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN) }

    // Ensure skills are loaded into the cache lazily
    LaunchedEffect(member.userId) {
        if (!skillsCache.containsKey(member.userId)) {
            membersViewModel.getUserSkills(member.userId) { skills ->
                skillsCache[member.userId] = skills
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentUser) 4.dp else 2.dp)
    ) {
        ListItem(
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.displayName.ifBlank { "名前未設定" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary) {
                            Text(
                                text = "あなた",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            },
            supportingContent = {
                Column {
                    Text(
                        text = "参加日: ${dateFormatter.format(member.joinedAt.toDate())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // preview chips
                    val cachedSkills = skillsCache[member.userId] ?: emptyList()
                    if (cachedSkills.isEmpty()) {
                        Text(
                            text = "得意なこと: 未登録",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val preview = cachedSkills.take(3)
                            preview.forEach { skill ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.clickable { onShowSkills(member.displayName.ifBlank { "名前未設定" }, cachedSkills) }
                                ) {
                                    Text(
                                        text = skill,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            if (cachedSkills.size > 3) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { onShowSkills(member.displayName.ifBlank { "名前未設定" }, cachedSkills) }
                                ) {
                                    Text(
                                        text = "+${cachedSkills.size - 3}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            leadingContent = {
                Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isCurrentUser) Icons.Default.Person else Icons.Default.AccountCircle,
                            contentDescription = "プロフィール",
                            modifier = Modifier.size(24.dp),
                            tint = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun LoadingStateView(
    message: String,
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
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
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
                    contentDescription = null,
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
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
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
            OutlinedButton(
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