package com.marksilla.auraagent

import java.util.Locale

fun extractOpenCommand(command: String): String? {
    val normalized = normalizeCommand(command)
    val withoutAssistantPrefix = stripAssistantPrefix(normalized)

    if (withoutAssistantPrefix.isBlank()) {
        return null
    }

    val patterns = listOf(
        Regex("""^take me to\s+(.+)$"""),
        Regex("""^(?:can you(?: please)?|could you(?: please)?)\s+(?:open|launch|start|run)\s+(.+)$"""),
        Regex("""^(?:please\s+)?(?:open|launch|start|run)\s+(.+)$"""),
        Regex("""^(?:pwede mo bang|maaari mo bang|gusto kong)\s+buksan\s+(?:(?:ang|yung)\s+)?(.+)$"""),
        Regex("""^(?:paki\s+)?(?:open|buksan)\s+(?:mo\s+)?(?:(?:ang|yung)\s+)?(.+)$""")
    )

    return patterns
        .asSequence()
        .mapNotNull { pattern -> pattern.find(withoutAssistantPrefix)?.groupValues?.getOrNull(1) }
        .mapNotNull(::cleanRequestedAppName)
        .firstOrNull()
}

private fun normalizeCommand(command: String): String =
    command
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9\\s]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

private fun stripAssistantPrefix(command: String): String {
    val prefixes = listOf("hey aura", "hi aura", "aura")

    for (prefix in prefixes) {
        if (command == prefix) {
            return ""
        }

        if (command.startsWith("$prefix ")) {
            return command.removePrefix(prefix).trim()
        }
    }

    return command
}

private fun cleanRequestedAppName(rawName: String): String? {
    var cleaned = normalizeCommand(rawName)

    cleaned = cleaned
        .removePrefix("the ")
        .removePrefix("ang ")
        .removePrefix("yung ")
        .trim()

    val trailingFillers = listOf(
        "for me",
        "please",
        "naman",
        "nga",
        "na"
    )

    var changed: Boolean
    do {
        changed = false

        for (filler in trailingFillers) {
            when {
                cleaned == filler -> {
                    cleaned = ""
                    changed = true
                }

                cleaned.endsWith(" $filler") -> {
                    cleaned = cleaned
                        .removeSuffix(" $filler")
                        .trim()
                    changed = true
                }
            }
        }
    } while (changed && cleaned.isNotBlank())

    return cleaned.takeIf { it.isNotBlank() }
}
