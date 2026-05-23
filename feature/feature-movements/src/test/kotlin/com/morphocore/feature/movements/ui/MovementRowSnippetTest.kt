package com.morphocore.feature.movements.ui

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MovementRowSnippetTest {

    @Test
    fun `blank query returns null`() {
        assertNull(descriptionMatchSnippet("Some description text", ""))
    }

    @Test
    fun `whitespace-only query returns null`() {
        assertNull(descriptionMatchSnippet("Some description text", "   "))
    }

    @Test
    fun `no match in description returns null`() {
        assertNull(descriptionMatchSnippet("A front kick technique", "spinning"))
    }

    @Test
    fun `snippet contains the query text`() {
        val result = descriptionMatchSnippet("A powerful roundhouse kick.", "roundhouse")
        assertNotNull(result)
        assertTrue(result!!.contains("roundhouse"))
    }

    @Test
    fun `match near start has no leading ellipsis`() {
        val result = descriptionMatchSnippet("front kick technique", "front")
        assertNotNull(result)
        assertTrue("Expected no leading ellipsis", !result!!.startsWith("…"))
    }

    @Test
    fun `match far from start adds leading ellipsis`() {
        val desc = "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z match here end"
        val result = descriptionMatchSnippet(desc, "match here")
        assertNotNull(result)
        assertTrue("Expected leading ellipsis", result!!.startsWith("…"))
    }

    @Test
    fun `match far from end adds trailing ellipsis`() {
        val desc = "match here and then lots and lots and lots of trailing text beyond the window"
        val result = descriptionMatchSnippet(desc, "match here")
        assertNotNull(result)
        assertTrue("Expected trailing ellipsis", result!!.endsWith("…"))
    }

    @Test
    fun `match at end of description has no trailing ellipsis`() {
        val result = descriptionMatchSnippet("A front kick", "kick")
        assertNotNull(result)
        assertTrue("Expected no trailing ellipsis", !result!!.endsWith("…"))
    }

    @Test
    fun `case insensitive match`() {
        val result = descriptionMatchSnippet("A FRONT kick technique", "front")
        assertNotNull(result)
        assertTrue(result!!.contains("FRONT"))
    }
}
