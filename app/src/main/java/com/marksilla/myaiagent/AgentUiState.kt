package com.marksilla.myaiagent

data class AgentMessage(
    val text: String,
    val fromUser: Boolean,
    val timeLabel: String = "Now"
)

fun canSendMessage(input: String): Boolean = input.trim().isNotEmpty()
