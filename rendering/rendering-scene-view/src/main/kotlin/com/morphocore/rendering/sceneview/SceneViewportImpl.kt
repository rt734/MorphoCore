package com.morphocore.rendering.sceneview

import com.morphocore.domain.CameraPreset
import com.morphocore.domain.SceneEnvironment
import com.morphocore.rendering.api.LoadedModelFactory
import com.morphocore.rendering.api.ModelLoadResult
import com.morphocore.rendering.api.SceneViewport
import io.github.sceneview.SceneView
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.withTimeout
import java.io.FileNotFoundException

class SceneViewportImpl : SceneViewport {

    private var sceneView: SceneView? = null
    private var currentModelNode: ModelNode? = null

    internal fun onViewAttached(view: SceneView) {
        sceneView = view
    }

    internal fun onViewDetached() {
        currentModelNode = null
        sceneView = null
    }

    override suspend fun loadModel(modelPath: String): ModelLoadResult {
        val view = sceneView ?: return ModelLoadResult.Failure.FileNotFound
        return try {
            withTimeout(5_000L) {
                val node = ModelNode(view.engine)
                node.loadModelGlb(
                    context       = view.context,
                    glbFileSource = modelPath,
                    autoAnimate   = false
                )
                currentModelNode = node
                view.addChildNode(node)
                ModelLoadResult.Success(LoadedModelFactory.create(modelPath))
            }
        } catch (e: FileNotFoundException) {
            ModelLoadResult.Failure.FileNotFound
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            ModelLoadResult.Failure.Timeout
        } catch (e: OutOfMemoryError) {
            ModelLoadResult.Failure.GpuOutOfMemory
        } catch (e: Exception) {
            ModelLoadResult.Failure.ParseError(e)
        }
    }

    override fun play(clipName: String, loop: Boolean) {
        currentModelNode?.playAnimation(clipName, loop = loop)
    }

    override fun pause() {
        currentModelNode?.stopAnimation()
    }

    override fun seekTo(timeSeconds: Float) {
        currentModelNode?.animationTime = timeSeconds
    }

    override fun setCamera(preset: CameraPreset, animated: Boolean) {
        // Camera preset wiring deferred to Sprint 3.
    }

    override fun applySceneEnvironment(env: SceneEnvironment) {
        // IBL + light update deferred to Sprint 3.
    }
}
