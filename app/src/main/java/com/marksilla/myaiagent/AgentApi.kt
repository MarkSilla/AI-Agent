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

        // URL-encode the JSON so it can safely be sent
        // through the GET query parameters.
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

        // AppDeploy GET endpoint
        val apiUrl =
            "$API_BASE_URL?messages=$encodedMessages&memory=$encodedMemory"

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
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36"
            )

            // Get HTTP status
            val status = connection.responseCode

            // Read normal response or error response
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

            // Handle HTTP errors
            if (status !in 200..299) {

                val serverMessage =
                    try {
                        if (responseText.isNotBlank()) {

                            val errorJson =
                                JSONObject(responseText)

                            errorJson.optString(
                                "message",
                                responseText
                            )

                        } else {
                            "No error message returned by server."
                        }

                    } catch (_: Exception) {

                        responseText.ifBlank {
                            "No error message returned by server."
                        }
                    }

                throw Exception(
                    "Server returned HTTP $status\n\n$serverMessage"
                )
            }

            // Handle empty response
            if (responseText.isBlank()) {
                throw Exception(
                    "Server returned an empty response."
                )
            }

            // Parse JSON response
            val response =
                JSONObject(responseText)

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
