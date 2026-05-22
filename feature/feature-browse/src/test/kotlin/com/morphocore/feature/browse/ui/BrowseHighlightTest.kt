package com.morphocore.feature.browse.ui

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
}
