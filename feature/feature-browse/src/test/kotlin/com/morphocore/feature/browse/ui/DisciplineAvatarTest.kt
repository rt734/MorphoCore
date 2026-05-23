package com.morphocore.feature.browse.ui

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DisciplineAvatarTest {

    @Test
    fun `single word name returns first two characters uppercased`() {
        assertEquals("KA", disciplineInitials("Karate"))
    }

    @Test
    fun `two word name returns first letter of each word`() {
        assertEquals("KF", disciplineInitials("Kung Fu"))
    }

    @Test
    fun `three word name returns first letter of first two words`() {
        assertEquals("WE", disciplineInitials("Weight Training Extended"))
    }

    @Test
    fun `weight training returns WT`() {
        assertEquals("WT", disciplineInitials("Weight Training"))
    }

    @Test
    fun `yoga returns YO`() {
        assertEquals("YO", disciplineInitials("Yoga"))
    }

    @Test
    fun `calisthenics returns CA`() {
        assertEquals("CA", disciplineInitials("Calisthenics"))
    }

    @Test
    fun `result is always uppercase`() {
        val result = disciplineInitials("kung fu")
        assertEquals(result, result.uppercase())
    }

    @Test
    fun `single letter word is handled`() {
        assertEquals("A", disciplineInitials("A"))
    }

    @Test
    fun `extra whitespace is trimmed`() {
        assertEquals("KF", disciplineInitials("  Kung  Fu  "))
    }
}
