package com.morphocore.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.morphocore.app.navigation.MorphoNavHost
import com.morphocore.content.api.ContentRegistry
import com.morphocore.designsystem.MorphoTheme
import com.morphocore.theme.api.ThemeProvider
import com.morphocore.theme.api.ThemeRegistry
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themeProvider: ThemeProvider
    @Inject lateinit var themeRegistry: ThemeRegistry
    @Inject lateinit var contentRegistry: ContentRegistry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            themeRegistry.refresh()
            contentRegistry.refresh()
        }
        setContent {
            val theme by themeProvider.activeTheme.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            MorphoTheme(theme = theme) {
                MorphoNavHost(navController = navController)
            }
        }
    }
}
