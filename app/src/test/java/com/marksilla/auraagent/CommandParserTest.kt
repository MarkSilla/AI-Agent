package com.marksilla.auraagent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CommandParserTest {
    @Test
    fun extractsEnglishOpenCommands() {
        assertEquals("facebook", extractOpenCommand("Open Facebook"))
        assertEquals("facebook", extractOpenCommand("Please open Facebook"))
        assertEquals("facebook", extractOpenCommand("Can you open Facebook?"))
        assertEquals("messenger", extractOpenCommand("Could you please open Messenger?"))
        assertEquals("facebook", extractOpenCommand("Launch Facebook"))
        assertEquals("messenger", extractOpenCommand("Start Messenger"))
        assertEquals("youtube", extractOpenCommand("Run YouTube"))
        assertEquals("facebook", extractOpenCommand("Take me to Facebook"))
    }

    @Test
    fun extractsTaglishAndFilipinoOpenCommands() {
        assertEquals("facebook", extractOpenCommand("Buksan mo Facebook"))
        assertEquals("facebook", extractOpenCommand("Buksan mo yung Facebook"))
        assertEquals("messenger", extractOpenCommand("Paki buksan ang Messenger"))
        assertEquals("youtube", extractOpenCommand("Paki open yung YouTube"))
        assertEquals("facebook", extractOpenCommand("Pwede mo bang buksan ang Facebook"))
        assertEquals("messenger", extractOpenCommand("Maaari mo bang buksan ang Messenger"))
        assertEquals("youtube", extractOpenCommand("Gusto kong buksan ang YouTube"))
    }

    @Test
    fun stripsAssistantPrefixAndTrailingFillers() {
        assertEquals("facebook", extractOpenCommand("Hey AURA, open Facebook please"))
        assertEquals("messenger", extractOpenCommand("Hi AURA launch Messenger for me"))
        assertEquals("youtube", extractOpenCommand("AURA paki open yung YouTube naman"))
        assertEquals("facebook", extractOpenCommand("Hey Aura buksan mo Facebook nga"))
    }

    @Test
    fun returnsNullWhenCommandIsNotAnOpenCommand() {
        assertNull(extractOpenCommand("What time is it?"))
    }
}
