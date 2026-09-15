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

            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val jsonMessages = JSONArray()

            messages.takeLast(18).forEach { message ->

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

            val jsonMemory = JSONArray()

            memory.takeLast(20).forEach {
                jsonMemory.put(it)
            }

            val requestBody = JSONObject().apply {
                put("messages", jsonMessages)
                put("memory", jsonMemory)
            }

            connection.outputStream.use { output ->
                output.write(
                    requestBody
                        .toString()
                        .toByteArray(Charsets.UTF_8)
                )
            }

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

            if (status !in 200..299) {
                throw Exception(
                    "Server returned HTTP $status"
                )
            }

            val response =
                JSONObject(responseText)

            return response.optString(
                "text",
                "The AI returned an empty response."
            )

        } finally {
            connection.disconnect()
        }
    }
}
