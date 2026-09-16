package com.marksilla.auraagent

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WakeWordDetectorTest {
    @Test
    fun detectsWakeWordWithExpectedCapitalization() {
        assertTrue(containsWakeWord("Hey AURA"))
    }

    @Test
    fun detectsLowercaseWakeWord() {
        assertTrue(containsWakeWord("hey aura"))
    }

    @Test
    fun detectsUppercaseWakeWord() {
        assertTrue(containsWakeWord("HEY AURA"))
    }

    @Test
    fun detectsTitleCaseWakeWord() {
        assertTrue(containsWakeWord("Hey Aura"))
    }

    @Test
    fun detectsWakeWordBeforeCommand() {
        assertTrue(containsWakeWord("Hey AURA, open Facebook"))
    }

    @Test
    fun normalSpeechDoesNotMatchWakeWord() {
        assertFalse(containsWakeWord("Open Facebook when I get home"))
    }
}
