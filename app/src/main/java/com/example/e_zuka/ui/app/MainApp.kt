package com.example.e_zuka.ui.app

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.e_zuka.data.model.AuthState
import com.example.e_zuka.data.model.RegionAuthState
import com.example.e_zuka.ui.auth.AuthScreen
import com.example.e_zuka.ui.auth.RegionVerificationScreen
import com.example.e_zuka.ui.auth.UserNameVerificationScreen
import com.example.e_zuka.ui.home.HomeScreen
import com.example.e_zuka.ui.members.RegionMembersScreen
import com.example.e_zuka.ui.settings.SettingsScreen
import com.example.e_zuka.viewmodel.AuthViewModel
import com.example.e_zuka.viewmodel.RegionMembersViewModel
import com.example.e_zuka.viewmodel.ThemeSettingsViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay

data class NavigationItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: AuthViewModel,
    themeViewModel: ThemeSettingsViewModel,
    modifier: Modifier = Modifier
) {
    val authState by viewModel.authState.collectAsState()
    val regionAuthState by viewModel.regionAuthState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // アニメーション用の状態
    var isContentVisible by remember { mutableStateOf(false) }

    // メッセージの表示処理
    LaunchedEffect(successMessage, errorMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // テーマ設定の監視
    val themeConfig by themeViewModel.themeConfig.collectAsState()

    // 高コントラストモードの適用
    val colors = if (themeConfig.isHighContrast) {
        MaterialTheme.colorScheme.copy(
            surface = MaterialTheme.colorScheme.background,
            onSurface = MaterialTheme.colorScheme.onBackground,
            surfaceVariant = MaterialTheme.colorScheme.background,
            onSurfaceVariant = MaterialTheme.colorScheme.onBackground
        )
    } else {
        MaterialTheme.colorScheme
    }

    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography.copy(
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(
                fontSize = MaterialTheme.typography.bodyLarge.fontSize * themeConfig.fontScale
            ),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(
                fontSize = MaterialTheme.typography.bodyMedium.fontSize * themeConfig.fontScale
            ),
            bodySmall = MaterialTheme.typography.bodySmall.copy(
                fontSize = MaterialTheme.typography.bodySmall.fontSize * themeConfig.fontScale
            )
        )
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isContentVisible,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (val currentAuthState = authState) {
                            is AuthState.Loading -> {
                                LoadingScreen(
                                    message = when (regionAuthState) {
                                        is RegionAuthState.Loading -> "地域認証状態を確認中..."
                                        else -> "認証状態を確認中..."
                                    }
                                )
                            }
                            is AuthState.Authenticated -> {
                                MainScreenWithNavigation(
                                    user = currentAuthState.user,
                                    viewModel = viewModel
                                )
                            }
                            is AuthState.UserNameVerificationRequired -> {
                                UserNameVerificationScreen(
                                    user = currentAuthState.user,
                                    viewModel = viewModel
                                )
                            }
                            is AuthState.RegionVerificationRequired -> {
                                if (regionAuthState is RegionAuthState.Verified) {
                                    // 地域認証が完了している場合は必ずホーム画面に遷移
                                    MainScreenWithNavigation(
                                        user = currentAuthState.user,
                                        viewModel = viewModel
                                    )
                                } else {
                                    RegionVerificationScreen(
                                        user = currentAuthState.user,
                                        viewModel = viewModel
                                    )
                                }
                            }
                            is AuthState.Unauthenticated -> {
                                AuthScreen(viewModel = viewModel)
                            }
                            is AuthState.Error -> {
                                ErrorScreen(
                                    message = currentAuthState.message,
                                    onRetry = { viewModel.signOut() }
                                )
                            }
                        }
                    }

                    // 初期表示時のアニメーション
                    LaunchedEffect(Unit) {
                        delay(100) // 少し遅延を入れて、アニメーションをより自然に
                        isContentVisible = true
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun MainScreenWithNavigation(
    user: FirebaseUser,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val membersViewModel: RegionMembersViewModel = viewModel()
    val regionAuthState by viewModel.regionAuthState.collectAsState()
    val regionCodeId = (regionAuthState as? RegionAuthState.Verified)?.regionData?.codeId ?: ""

    val navigationItems = listOf(
        NavigationItem(
            title = "ホーム",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            route = "home"
        ),
        NavigationItem(
            title = "メンバー",
            selectedIcon = Icons.Filled.Groups,
            unselectedIcon = Icons.Outlined.Groups,
            route = "members"
        ),
        NavigationItem(
            title = "設定",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            route = "settings"
        )
    )

    var selectedItemIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                navigationItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedItemIndex == index,
                        onClick = { selectedItemIndex = index },
                        label = {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        alwaysShowLabel = true,
                        icon = {
                            Icon(
                                imageVector = if (selectedItemIndex == index) {
                                    item.selectedIcon
                                } else {
                                    item.unselectedIcon
                                },
                                contentDescription = item.title
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                when (selectedItemIndex) {
                    0 -> {
                        HomeScreen(
                            user = user,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    1 -> {
                        RegionMembersScreen(
                            user = user,
                            authViewModel = viewModel,
                            membersViewModel = membersViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    2 -> {
                        SettingsScreen(
                            user = user,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            Text(
                text = message,
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = onRetry) {
                Text("再試行")
            }
        }
    }
}