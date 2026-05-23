package com.morphocore.feature.browse.ui

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BrowseHighlightTest {

    @Test
    fun `returns null when query is blank`() {
        assertNull(findHighlightRange("Front Kick", ""))
    }

    @Test
    fun `returns null when query is whitespace only`() {
        assertNull(findHighlightRange("Front Kick", "   "))
    }

    @Test
    fun `returns null when query is not found in text`() {
        assertNull(findHighlightRange("Front Kick", "yoga"))
    }

    @Test
    fun `returns correct range for exact case-sensitive match`() {
        assertEquals(6..9, findHighlightRange("Front Kick", "Kick"))
    }

    @Test
    fun `match is case-insensitive`() {
        assertEquals(6..9, findHighlightRange("Front Kick", "kick"))
        assertEquals(6..9, findHighlightRange("Front Kick", "KICK"))
    }

    @Test
    fun `returns first occurrence when query appears multiple times`() {
        assertEquals(0..3, findHighlightRange("Kick Kick", "kick"))
    }

    @Test
    fun `highlights entire string when full text matches`() {
        assertEquals(0..4, findHighlightRange("Squat", "squat"))
    }

    @Test
    fun `highlights at start of text`() {
        assertEquals(0..2, findHighlightRange("Mae Geri", "mae"))
    }

    @Test
    fun `partial word match is highlighted`() {
        assertEquals(0..3, findHighlightRange("Deadlift", "dead"))
    }

    // browseDescriptionMatchSnippet tests

    @Test
    fun `snippet returns null for blank query`() {
        assertNull(browseDescriptionMatchSnippet("Some description text", ""))
    }

    @Test
    fun `snippet returns null when query not in description`() {
        assertNull(browseDescriptionMatchSnippet("A front kick technique", "spinning"))
    }

    @Test
    fun `snippet contains the query text`() {
        val result = browseDescriptionMatchSnippet("A powerful roundhouse kick.", "roundhouse")
        assertNotNull(result)
        assertTrue(result!!.contains("roundhouse"))
    }

    @Test
    fun `snippet adds leading ellipsis when match is far from start`() {
        val desc = "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z match here end"
        val result = browseDescriptionMatchSnippet(desc, "match here")
        assertNotNull(result)
        assertTrue(result!!.startsWith("…"))
    }

    @Test
    fun `snippet adds trailing ellipsis when match is far from end`() {
        val desc = "match here and then lots and lots and lots of trailing text beyond the window"
        val result = browseDescriptionMatchSnippet(desc, "match here")
        assertNotNull(result)
        assertTrue(result!!.endsWith("…"))
    }

    @Test
    fun `snippet has no ellipsis when description is short`() {
        val result = browseDescriptionMatchSnippet("A front kick", "front")
        assertNotNull(result)
        assertTrue(!result!!.startsWith("…"))
        assertTrue(!result.endsWith("…"))
    }
}
