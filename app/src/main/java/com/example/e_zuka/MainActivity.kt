package com.example.e_zuka

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.e_zuka.data.model.ThemeMode
import com.example.e_zuka.ui.app.MainApp
import com.example.e_zuka.ui.theme.EzukaTheme
import com.example.e_zuka.viewmodel.AuthViewModel
import com.example.e_zuka.viewmodel.AuthViewModelFactory
import com.example.e_zuka.viewmodel.ThemeSettingsViewModel

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val viewModel: AuthViewModel = viewModel(
                factory = AuthViewModelFactory(context)
            )
            val themeViewModel: ThemeSettingsViewModel = viewModel(
                factory = ThemeSettingsViewModel.Factory(context)
            )

            // テーマ設定の監視
            val themeConfig by themeViewModel.themeConfig.collectAsState()
            val systemInDarkTheme = isSystemInDarkTheme()

            // テーマモードの決定
            val darkTheme = when (themeConfig.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemInDarkTheme
            }

            // アプリ全体のテーマ適用
            EzukaTheme(
                darkTheme = darkTheme,
                dynamicColor = true,
                content = {
                    // テーマ設定に応じたCompositionLocalの提供
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize * themeConfig.fontScale
                        )
                    ) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            contentWindowInsets = WindowInsets(0, 0, 0, 0)
                        ) { innerPadding ->
                            MainScreen(
                                modifier = Modifier.padding(
                                    bottom = innerPadding.calculateBottomPadding()
                                ),
                                themeViewModel = themeViewModel
                            )
                        }
                    }
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    themeViewModel: ThemeSettingsViewModel
) {
    val context = LocalContext.current
    val viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(context)
    )

    MainApp(
        viewModel = viewModel,
        themeViewModel = themeViewModel,
        modifier = modifier
    )
}