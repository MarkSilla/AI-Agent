package com.marksilla.auraagent

import java.util.Locale

fun containsWakeWord(text: String): Boolean {
    val normalized = text
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    return Regex("""(^|\s)hey\s+aura(\s|$)""")
        .containsMatchIn(normalized)
}
