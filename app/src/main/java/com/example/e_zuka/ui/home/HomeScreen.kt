package com.example.e_zuka.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.e_zuka.data.model.ProblemData
import com.example.e_zuka.data.model.RegionAuthState
import com.example.e_zuka.viewmodel.AuthViewModel
import com.example.e_zuka.viewmodel.RegionProblemViewModel
import com.example.e_zuka.viewmodel.RegionProblemViewModelFactory
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    user: FirebaseUser,
    viewModel: AuthViewModel,
    onThreadSelect: (ProblemData) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val regionAuthState by viewModel.regionAuthState.collectAsState()
    val context = LocalContext.current
    val problemViewModel: RegionProblemViewModel = viewModel(factory = RegionProblemViewModelFactory(context))
    val problems by problemViewModel.problems.collectAsState()
    val isProblemLoading by problemViewModel.isLoading.collectAsState()
    val problemError by problemViewModel.errorMessage.collectAsState()
    val problemSuccess by problemViewModel.successMessage.collectAsState()
    var showPostDialog by remember { mutableStateOf(false) }
    var selectedProblem by remember { mutableStateOf<ProblemData?>(null) }
    val regionCodeId = (regionAuthState as? RegionAuthState.Verified)?.regionData?.codeId ?: ""

    // 検索・フィルタリング用の状態
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyUnsolved by remember { mutableStateOf(true) }
    var showOnlyMine by remember { mutableStateOf(false) }

    // フィルタリングされた問題リスト
    val filteredProblems = remember(problems, searchQuery, showOnlyUnsolved, showOnlyMine) {
        problems.filter { problem ->
            val matchesQuery = problem.title.contains(searchQuery, ignoreCase = true) ||
                    problem.description.contains(searchQuery, ignoreCase = true)
            val matchesUnsolved = !showOnlyUnsolved || !problem.isSolved
            val matchesMine = !showOnlyMine || problem.userId == user.uid
            matchesQuery && matchesUnsolved && matchesMine
        }.sortedByDescending { it.createdAt }
    }

    // スナックバーのホスト状態
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(regionCodeId) {
        if (regionCodeId.isNotBlank()) {
            problemViewModel.loadProblems(regionCodeId)
        }
    }
    // 投稿・助ける・解決済みの成功時は再取得
    LaunchedEffect(problemSuccess) {
        if (regionCodeId.isNotBlank() && problemSuccess != null) {
            onThreadSelect(problems.firstOrNull() ?: return@LaunchedEffect)
            problemViewModel.clearSuccessMessage()
        }
    }
    // エラー時はクリアのみ
    LaunchedEffect(problemError) {
        if (problemError != null) {
            snackbarHostState.showSnackbar(problemError!!)
            problemViewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "地域助け合いマッチング",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { problemViewModel.loadProblems(regionCodeId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "更新")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showPostDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, "投稿を作成")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isProblemLoading -> LoadingStateView()
                problemError != null -> ErrorStateView(
                    message = problemError!!,
                    onRetry = { problemViewModel.loadProblems(regionCodeId) }
                )
                problems.isEmpty() -> EmptyStateView()
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = filteredProblems,
                            key = { it.problemId }
                        ) { problem ->
                            ProblemCard(
                                problem = problem,
                                currentUserId = user.uid,
                                onProblemClick = { selectedProblem = it },
                                onHelp = { problemViewModel.helpProblem(it, user.uid) },
                                onSolve = { problemViewModel.solveProblem(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    // 投稿ダイアログ
    if (showPostDialog) {
        EnhancedPostProblemDialog(
            user = user,
            regionCodeId = regionCodeId,
            onDismiss = { showPostDialog = false },
            onPost = { problem ->
                problemViewModel.postProblem(problem)
                showPostDialog = false
            }
        )
    }

    // 問題詳細画面
    if (selectedProblem != null) {
        EnhancedProblemDetailScreen(
            user = user,
            problem = selectedProblem!!,
            onBack = { selectedProblem = null }
        )
    }
}

@Composable
private fun LoadingStateView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "データを読み込み中です"
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = "読み込み中を示すインジケーター"
                    },
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "データを読み込んでいます...",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
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
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "エラーが発生しました: $message"
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "エラーアイコン",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.semantics {
                    contentDescription = "再試行ボタン"
                }
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("再試行")
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "データがありません"
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = "情報アイコン",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "まだ投稿がありません",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "右下の「+」ボタンから投稿を作成できます",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProblemCard(
    problem: ProblemData,
    currentUserId: String,
    onProblemClick: (ProblemData) -> Unit,
    onHelp: (String) -> Unit,
    onSolve: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = { onProblemClick(problem) },
                role = Role.Button
            )
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append("困りごと: ${problem.title}. ")
                    append("投稿者: ${problem.displayName}. ")
                    if (problem.isSolved) {
                        append("状態: 解決済み. ")
                    } else if (problem.helperUserId != null) {
                        append("${problem.helperDisplayName}さんが助けています. ")
                    } else {
                        append("状態: 未解決. ")
                    }
                }
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // ヘッダー部分（タイトルと状態）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // カテゴリーとタグ
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // カテゴリーチップ
                        if (problem.category.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    problem.category,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // 優先度表示
                        val priorityColor = when (problem.priority) {
                            2 -> MaterialTheme.colorScheme.errorContainer
                            1 -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val priorityText = when (problem.priority) {
                            2 -> "優先度：高"
                            1 -> "優先度：中"
                            else -> "優先度：低"
                        }
                        Surface(
                            color = priorityColor,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                priorityText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // タイトル
                    Text(
                        problem.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 状態バッジ
                if (problem.isSolved) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            "解決済み",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 本文
            Text(
                problem.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // タグ一覧
            if (problem.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    problem.tags.forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                tag,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // フッター（投稿者情報とアクション）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    // 投稿者情報
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            problem.displayName,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                                .format(problem.createdAt.toDate()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (problem.requiredPeople > 1) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${problem.requiredPeople}人必要",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // アクションボタン
                if (!problem.isSolved) {
                    if (problem.helperUserId == null && currentUserId != problem.userId) {
                        Button(
                            onClick = { onHelp(problem.problemId) },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.semantics {
                                contentDescription = "この困りごとを助ける"
                            }
                        ) {
                            Icon(Icons.Default.Handshake, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("助ける")
                        }
                    } else if (problem.helperUserId == currentUserId) {
                        Button(
                            onClick = { onSolve(problem.problemId) },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.semantics {
                                contentDescription = "この困りごとを解決済みにする"
                            }
                        ) {
                            Icon(Icons.Default.Done, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("解決済みにする")
                        }
                    }
                }
            }

            // ヘルパー情報（存在する場合）
            if (problem.helperUserId != null && !problem.isSolved) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Handshake,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${problem.helperDisplayName}さんが助けています",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedPostProblemDialog(
    user: FirebaseUser,
    regionCodeId: String,
    onDismiss: () -> Unit,
    onPost: (ProblemData) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var priority by remember { mutableIntStateOf(0) }
    var requiredPeople by remember { mutableIntStateOf(1) }
    var reward by remember { mutableStateOf("") }
    var estimatedTime by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }

    val categories = listOf("急ぎ", "力仕事", "買い物", "移動", "その他")
    val commonTags = listOf("重い", "2人以上必要", "車必要", "工具必要", "高所作業")

    // カスタムタグ用の状態
    var customTags by remember { mutableStateOf(setOf<String>()) }
    var customTagInput by remember { mutableStateOf("") }
    var showAddCustomTagDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // ヘッダー
                Text(
                    "困りごとを投稿",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // タイトル入力
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("タイトル") },
                    placeholder = { Text("例: 家具の移動をお願いしたい") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    supportingText = { Text("具体的なタイトルを入力してください") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 詳細説明
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("詳細") },
                    placeholder = { Text("例: 2階の部屋にタンスを運びたいのですが、一人では難しいです。") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    supportingText = { Text("困りごとの詳細な説明を入力してください") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // カテゴリー選択
                Text(
                    "カテゴリー",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            leadingIcon = {
                                if (selectedCategory == category) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 優先度選択
                Text(
                    "優先度",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = priority == 0,
                        onClick = { priority = 0 },
                        label = { Text("低") }
                    )
                    FilterChip(
                        selected = priority == 1,
                        onClick = { priority = 1 },
                        label = { Text("中") }
                    )
                    FilterChip(
                        selected = priority == 2,
                        onClick = { priority = 2 },
                        label = { Text("高") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 必要な人数
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "必要な人数",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { if (requiredPeople > 1) requiredPeople-- },
                        enabled = requiredPeople > 1
                    ) {
                        Icon(Icons.Default.RemoveCircle, contentDescription = "減らす")
                    }
                    Text(
                        "${requiredPeople}人",
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(
                        onClick = { if (requiredPeople < 5) requiredPeople++ },
                        enabled = requiredPeople < 5
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "増やす")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // タグ選択
                Text(
                    "タグ（複数選択可）",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    commonTags.forEach { tag ->
                        FilterChip(
                            selected = selectedTags.contains(tag),
                            onClick = {
                                selectedTags = if (selectedTags.contains(tag)) {
                                    selectedTags - tag
                                } else {
                                    selectedTags + tag
                                }
                            },
                            label = { Text(tag) }
                        )
                    }

                    customTags.forEach { tag ->
                        FilterChip(
                            selected = true,
                            onClick = {
                                customTags = customTags - tag
                            },
                            label = { Text(tag) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "削除",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }

                    // タグ追加ボタン
                    AssistChip(
                        onClick = { showAddCustomTagDialog = true },
                        label = { Text("タグを追加") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "追加",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                // タグ追加ダイアログ
                if (showAddCustomTagDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showAddCustomTagDialog = false
                            customTagInput = ""
                        },
                        title = { Text("タグを追加") },
                        text = {
                            OutlinedTextField(
                                value = customTagInput,
                                onValueChange = { customTagInput = it },
                                label = { Text("新しいタグ") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (customTagInput.isNotBlank()) {
                                        customTags = customTags + customTagInput.trim()
                                        customTagInput = ""
                                    }
                                    showAddCustomTagDialog = false
                                }
                            ) {
                                Text("追加")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showAddCustomTagDialog = false
                                    customTagInput = ""
                                }
                            ) {
                                Text("キャンセル")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 所要時間
                OutlinedTextField(
                    value = estimatedTime,
                    onValueChange = { estimatedTime = it },
                    label = { Text("予想所要時間") },
                    placeholder = { Text("例：30分程度") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(16.dp))

                // お礼
                OutlinedTextField(
                    value = reward,
                    onValueChange = { reward = it },
                    label = { Text("お礼") },
                    placeholder = { Text("例：お茶でも飲みながら会話") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(24.dp))

                // アクションボタン
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("キャンセル")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && description.isNotBlank() && selectedCategory != null) {
                                onPost(
                                    ProblemData(
                                        userId = user.uid,
                                        displayName = user.displayName ?: "",
                                        regionCodeId = regionCodeId,
                                        title = title,
                                        description = description,
                                        category = selectedCategory ?: "",
                                        priority = priority,
                                        tags = (selectedTags + customTags).toList(),
                                        requiredPeople = requiredPeople,
                                        reward = reward,
                                        estimatedTime = estimatedTime
                                    )
                                )
                            }
                        },
                        enabled = title.isNotBlank() && description.isNotBlank() && selectedCategory != null
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("投稿")
                    }
                }
            }
        }
    }
}
