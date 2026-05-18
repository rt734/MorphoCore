package com.morphocore.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.morphocore.rendering.sceneview.SceneViewportImpl
import com.morphocore.rendering.sceneview.SceneViewportSurface

// Manual device smoke-test. Not the launcher activity.
// Launch via: adb shell am start -n com.morphocore.app/.SmokeTestActivity
class SmokeTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewport = remember { SceneViewportImpl() }
            LaunchedEffect(viewport) {
                viewport.loadModel("content/karate/roundhouse_kick.glb")
            }
            SceneViewportSurface(
                viewport = viewport,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
