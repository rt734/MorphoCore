package com.morphocore.rendering.sceneview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.sceneview.SceneView

@Composable
fun SceneViewportSurface(
    viewport: SceneViewportImpl,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            SceneView(context).also { viewport.onViewAttached(it) }
        },
        modifier = modifier,
        onRelease = { viewport.onViewDetached() }
    )
}
