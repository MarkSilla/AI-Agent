package com.marksilla.myaiagent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marksilla.myaiagent.AgentMessage

private val Glass = Color(0xFF101522).copy(alpha = .88f)
private val GlassBorder = Color(0xFF252C3D)

@Composable
fun AgentHeader(onMenu: () -> Unit, onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenu) { Text("☰", fontSize = 22.sp) }
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text("My AI Agent", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF61E6B4)))
                Spacer(Modifier.width(6.dp))
                Text("Ready to help", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        IconButton(onClick = onSettings) { Text("⚙", fontSize = 21.sp) }
    }
}

@Composable
fun WelcomeState(onPrompt: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(76.dp).clip(RoundedCornerShape(24.dp)).background(
                Brush.linearGradient(listOf(Color(0xFF7E93FF), Color(0xFF6CE2C2)))
            ), contentAlignment = Alignment.Center
        ) { Text("✦", fontSize = 36.sp, color = Color(0xFF0A0D15)) }
        Spacer(Modifier.height(20.dp))
        Text("How can I help?", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Ask, create, plan, explain, or let your agent help with tasks on your device.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )
        Spacer(Modifier.height(24.dp))
        listOf("Explain something simply", "Help me write code", "Plan my next task").forEach {
            SuggestionChip(onClick = { onPrompt(it) }, label = { Text(it) })
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun MessageBubble(message: AgentMessage, onSpeak: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        horizontalAlignment = if (message.fromUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (message.fromUser) Color(0xFF283352) else Glass,
            modifier = Modifier
                .widthIn(max = 340.dp)
                .border(1.dp, if (message.fromUser) Color(0xFF3A486F) else GlassBorder, RoundedCornerShape(20.dp))
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(message.text, fontSize = 15.sp, lineHeight = 22.sp)
                if (!message.fromUser) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { onSpeak(message.text) }, contentPadding = PaddingValues(0.dp)) {
                        Text("🔊 Speak", fontSize = 12.sp)
                    }
                }
            }
        }
        Text(message.timeLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp, start = 6.dp, end = 6.dp))
    }
}

@Composable
fun MessageList(messages: List<AgentMessage>, onSpeak: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(vertical = 10.dp)) {
        items(messages) { MessageBubble(it, onSpeak) }
    }
}

@Composable
fun AgentComposer(
    input: String,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    onVoice: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        color = Color(0xFF111622),
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Column(Modifier.border(1.dp, GlassBorder, RoundedCornerShape(26.dp)).padding(8.dp)) {
            TextField(
                value = input,
                onValueChange = onInput,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Message your agent…", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                maxLines = 5,
                enabled = enabled
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onVoice) { Text("🎙 Voice") }
                Spacer(Modifier.weight(1f))
                FilledIconButton(onClick = onSend, enabled = enabled && input.trim().isNotEmpty()) {
                    Text("↑", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AgentDrawerContent(onNewChat: () -> Unit, onSettings: () -> Unit) {
    ModalDrawerSheet(drawerContainerColor = Color(0xFF0A0D14)) {
        Column(Modifier.fillMaxHeight().padding(18.dp)) {
            Text("My AI Agent", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Your personal assistant", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onNewChat, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("＋ New chat") }
            Spacer(Modifier.height(16.dp))
            Text("RECENT", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("New conversation", modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onNewChat() }.padding(12.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f))
            Text("⚙  Settings", modifier = Modifier.fillMaxWidth().clickable { onSettings() }.padding(12.dp))
        }
    }
}
