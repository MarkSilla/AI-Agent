package com.marksilla.myaiagent

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AgentApi {

    private const val API_URL =
        "https://my-ai-agent-38a3kw.v2.appdeploy.ai/api/agent/run"

    fun sendMessage(
        messages: List<AgentMessage>,
        memory: List<String> = emptyList()
    ): String {

        val connection =
            URL(API_URL).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20_000
            connection.readTimeout = 60_000
            connection.doOutput = true

            // Request headers
            connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=UTF-8"
            )

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36"
            )

            connection.setRequestProperty(
                "Origin",
                "https://my-ai-agent-38a3kw.v2.appdeploy.ai"
            )

            connection.setRequestProperty(
                "Referer",
                "https://my-ai-agent-38a3kw.v2.appdeploy.ai/"
            )

            connection.setRequestProperty(
                "Accept-Language",
                "en-US,en;q=0.9"
            )

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

            // Build request body
            val requestBody = JSONObject().apply {
                put("messages", jsonMessages)
                put("memory", jsonMemory)
            }

            // Send request
            connection.outputStream.use { output ->
                output.write(
                    requestBody
                        .toString()
                        .toByteArray(Charsets.UTF_8)
                )

                output.flush()
            }

            // Read HTTP status
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

            // Parse successful response
            if (responseText.isBlank()) {
                throw Exception(
                    "Server returned an empty response."
                )
            }

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
