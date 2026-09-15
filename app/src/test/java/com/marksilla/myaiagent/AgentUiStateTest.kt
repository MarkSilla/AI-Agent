package com.marksilla.myaiagent

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentUiStateTest {
    @Test
    fun blankMessageCannotBeSent() {
        assertFalse(canSendMessage("   "))
    }

    @Test
    fun nonBlankMessageCanBeSent() {
        assertTrue(canSendMessage("Hello agent"))
    }
}
