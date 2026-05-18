package com.morphocore.content.impl.registry

import com.morphocore.content.impl.FakeAssetSource
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManifestScannerTest {

    private val karateJson = checkNotNull(javaClass.getResourceAsStream("/fixtures/karate-sample.json"))
        .bufferedReader().readText()
    private val yogaJson = checkNotNull(javaClass.getResourceAsStream("/fixtures/yoga-sample.json"))
        .bufferedReader().readText()

    @Test
    fun `scans one discipline from a single-entry source`() = runTest {
        val source = FakeAssetSource(manifests = mapOf("karate" to karateJson))
        val result = scanSource(source)
        assertEquals(1, result.disciplines.size)
        assertEquals("karate", result.disciplines.first().id)
        assertEquals(0, result.failures.size)
    }

    @Test
    fun `scans multiple disciplines from a multi-entry source`() = runTest {
        val source = FakeAssetSource(manifests = mapOf("karate" to karateJson, "yoga" to yogaJson))
        val result = scanSource(source)
        assertEquals(2, result.disciplines.size)
        assertEquals(0, result.failures.size)
    }

    @Test
    fun `collects movements from all disciplines`() = runTest {
        val source = FakeAssetSource(manifests = mapOf("karate" to karateJson, "yoga" to yogaJson))
        val result = scanSource(source)
        assertEquals(5, result.movements.size) // 3 karate + 2 yoga
    }

    @Test
    fun `one bad manifest does not abort scan of other manifests`() = runTest {
        val source = FakeAssetSource(
            manifests = mapOf(
                "karate" to karateJson,
                "bad" to "{ invalid json }"
            )
        )
        val result = scanSource(source)
        assertEquals(1, result.disciplines.size)
        assertEquals("karate", result.disciplines.first().id)
        assertEquals(1, result.failures.size)
    }

    @Test
    fun `empty source returns empty result`() = runTest {
        val source = FakeAssetSource(manifests = emptyMap())
        val result = scanSource(source)
        assertTrue(result.disciplines.isEmpty())
        assertTrue(result.movements.isEmpty())
        assertTrue(result.failures.isEmpty())
    }
}
