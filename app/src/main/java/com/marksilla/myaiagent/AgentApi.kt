package com.marksilla.myaiagent

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object AgentApi {

    private const val API_BASE_URL =
        "https://my-ai-agent-38a3kw.v2.appdeploy.ai/api/agent/chat"

    fun sendMessage(
        messages: List<AgentMessage>,
        memory: List<String> = emptyList()
    ): String {

        // Build messages JSON
        val jsonMessages = JSONArray()

        messages
            .takeLast(18)
            .forEach { message ->
                jsonMessages.put(
                    JSONObject().apply {
                        put(
                            "role",
                            if (message.fromUser) {
                                "user"
                            } else {
                                "assistant"
                            }
                        )

                        put(
                            "content",
                            message.text
                        )
                    }
                )
            }

        // Build memory JSON
        val jsonMemory = JSONArray()

        memory
            .takeLast(20)
            .forEach { item ->
                jsonMemory.put(item)
            }

        // Encode JSON for URL query parameters
        val encodedMessages =
            URLEncoder.encode(
                jsonMessages.toString(),
                Charsets.UTF_8.name()
            )

        val encodedMemory =
            URLEncoder.encode(
                jsonMemory.toString(),
                Charsets.UTF_8.name()
            )

        // Cache-busting value
        val nonce = System.currentTimeMillis()

        val apiUrl =
            "$API_BASE_URL" +
            "?messages=$encodedMessages" +
            "&memory=$encodedMemory" +
            "&nonce=$nonce"

        val connection =
            URL(apiUrl).openConnection() as HttpURLConnection

        try {

            connection.requestMethod = "GET"
            connection.connectTimeout = 20_000
            connection.readTimeout = 60_000
            connection.useCaches = false

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            connection.setRequestProperty(
                "Cache-Control",
                "no-cache"
            )

            connection.setRequestProperty(
                "Pragma",
                "no-cache"
            )

            val status = connection.responseCode

            val stream =
                if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val responseText =
                stream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: ""

            // HTTP error
            if (status !in 200..299) {

                val preview =
                    responseText
                        .replace("\n", " ")
                        .take(500)

                throw Exception(
                    "Server returned HTTP $status\n\n$preview"
                )
            }

            if (responseText.isBlank()) {
                throw Exception(
                    "Server returned an empty response."
                )
            }

            // Detect HTML instead of JSON
            val trimmedResponse =
                responseText.trim()

            if (
                trimmedResponse.startsWith("<!doctype", ignoreCase = true) ||
                trimmedResponse.startsWith("<html", ignoreCase = true) ||
                trimmedResponse.startsWith("<", ignoreCase = false)
            ) {
                throw Exception(
                    "The AI backend returned an HTML page instead of JSON.\n\n" +
                    trimmedResponse.take(500)
                )
            }

            // Parse JSON
            val response =
                try {
                    JSONObject(trimmedResponse)
                } catch (e: Exception) {
                    throw Exception(
                        "The AI backend returned invalid JSON.\n\n" +
                        trimmedResponse.take(500)
                    )
                }

            val text =
                response.optString("text")

            if (text.isBlank()) {
                throw Exception(
                    "The AI returned an empty response."
                )
            }

            return text

        } finally {
            connection.disconnect()
        }
    }
}
