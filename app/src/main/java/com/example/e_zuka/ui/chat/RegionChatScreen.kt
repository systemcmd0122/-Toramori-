package com.example.e_zuka.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.e_zuka.data.model.ChatMessageData
import com.example.e_zuka.viewmodel.RegionChatViewModel
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RegionChatScreen(
    user: FirebaseUser,
    regionCodeId: String,
    viewModel: RegionChatViewModel,
    modifier: Modifier = Modifier
) {
    val messagesFlow = remember(regionCodeId) { viewModel.getMessagesFlow(regionCodeId) }
    val messages by messagesFlow.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var editingMessageId by remember { mutableStateOf<String?>(null) }
    var editingContent by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
    var showOptions by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // 検索結果のフィルタリング
    val filteredMessages = remember(messages, searchQuery) {
        if (searchQuery.isBlank()) {
            messages
        } else {
            messages.filter { message ->
                message.content.contains(searchQuery, ignoreCase = true) ||
                message.displayName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // スクロール位置を最下部に移動（新しいメッセージが来た時）
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && !isSearchActive) {
            lazyListState.scrollToItem(messages.size - 1)
        }
    }

    // エラーと成功メッセージの表示
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    LaunchedEffect(errorMessage, successMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "地域チャット",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.height(48.dp)
            )
        },
        bottomBar = {
            Column {
                // ヘルパー情報の表示（存在する場合）
                AnimatedVisibility(
                    visible = showOptions,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    // 編集モードのヘッダー
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "メッセージを編集中",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = {
                                editingMessageId = null
                                editingContent = ""
                                showOptions = false
                            }
                        ) {
                            Text("キャンセル")
                        }
                    }
                }

                // メッセージ入力エリア
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .imePadding(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        TextField(
                            value = if (editingMessageId != null) editingContent else input.text,
                            onValueChange = { value ->
                                if (editingMessageId != null) {
                                    editingContent = value
                                } else {
                                    input = TextFieldValue(value)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            placeholder = {
                                Text(
                                    if (editingMessageId != null)
                                        "編集内容を入力..."
                                    else
                                        "メッセージを入力..."
                                )
                            },
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledIconButton(
                            onClick = {
                                if (editingMessageId != null) {
                                    if (editingContent.isNotBlank()) {
                                        viewModel.editMessage(editingMessageId!!, editingContent)
                                        editingMessageId = null
                                        editingContent = ""
                                        showOptions = false
                                    }
                                } else {
                                    if (input.text.isNotBlank()) {
                                        viewModel.sendMessage(
                                            regionCodeId,
                                            user.uid,
                                            user.displayName ?: "",
                                            input.text
                                        )
                                        input = TextFieldValue("")
                                    }
                                }
                            },
                            enabled = if (editingMessageId != null) {
                                editingContent.isNotBlank() && !isLoading
                            } else {
                                input.text.isNotBlank() && !isLoading
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                if (editingMessageId != null) Icons.Default.Check else Icons.Default.Send,
                                contentDescription = if (editingMessageId != null) "保存" else "送信",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = 4.dp,
                    bottom = padding.calculateBottomPadding(),
                    start = 8.dp,
                    end = 8.dp
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 検索バー
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {
                        isSearchActive = true
                        keyboardController?.hide()
                    },
                    active = isSearchActive,
                    onActiveChange = { isSearchActive = it },
                    placeholder = { Text("メッセージを検索...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "検索") },
                    trailingIcon = {
                        if (isSearchActive) {
                            IconButton(onClick = {
                                searchQuery = ""
                                isSearchActive = false
                                keyboardController?.hide()
                            }) {
                                Icon(Icons.Default.Close, "検索をクリア")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 検索結果のプレビュー（必要に応じて実装）
                }

                // メッセージ一覧
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredMessages,
                        key = { it.messageId }
                    ) { message ->
                        val isMine = message.userId == user.uid
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            MessageItem(
                                message = message,
                                isMine = isMine,
                                onEdit = { messageId, content ->
                                    editingMessageId = messageId
                                    editingContent = content
                                },
                                onDelete = { messageId ->
                                    showDeleteConfirm = messageId
                                }
                            )
                        }
                    }
                }
            }

            // 削除確認ダイアログ
            if (showDeleteConfirm != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = null },
                    title = { Text("メッセージの削除") },
                    text = { Text("このメッセージを削除しますか？") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteMessage(showDeleteConfirm!!)
                                showDeleteConfirm = null
                            }
                        ) {
                            Text("削除", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = null }) {
                            Text("キャンセル")
                        }
                    }
                )
            }

            // Snackbarホスト
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun MessageItem(
    message: ChatMessageData,
    isMine: Boolean,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showOptions by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        // メッセージ情報（送信者と時刻）
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isMine) {
                Text(
                    message.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    " • ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.createdAt.toDate()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isMine) {
                Text(
                    " • ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    message.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // メッセージ本体
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            // オプションボタン（編集・削除）
            if (isMine) {
                AnimatedVisibility(
                    visible = showOptions,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        IconButton(
                            onClick = { onEdit(message.messageId, message.content) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "編集",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { onDelete(message.messageId) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "削除",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // メッセージバブル
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isMine) 16.dp else 4.dp,
                    topEnd = if (isMine) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                color = if (isMine)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.combinedClickable(
                    onClick = { showOptions = !showOptions },
                    onLongClick = { showOptions = !showOptions }
                )
            ) {
                Text(
                    message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMine)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            // オプションボタン（編集・削除）
            if (!isMine) {
                AnimatedVisibility(
                    visible = showOptions,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        IconButton(
                            onClick = { onEdit(message.messageId, message.content) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "編集",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { onDelete(message.messageId) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "削除",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
