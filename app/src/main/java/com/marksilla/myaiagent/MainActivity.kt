package com.marksilla.myaiagent

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.Locale

data class Message(val text: String, val fromUser: Boolean)

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) tts?.language = Locale.US
        }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Color(0xFF090B10),
                surface = Color(0xFF11151D),
                primary = Color(0xFF7C9CFF)
            )) {
                AgentScreen(
                    onSpeak = { tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, "agent") },
                    onVoice = {
                        startActivityForResult(
                            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            }, 100
                        )
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun AgentScreen(onSpeak: (String) -> Unit, onVoice: () -> Unit) {
    var input by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(Message("Hi! I'm your Android AI Agent. Ask me something.", false))
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty()) return
        messages += Message(text, true)
        val reply = "I received: $text\n\nThe native APK shell is ready. The live AI backend can be connected next."
        messages += Message(reply, false)
        onSpeak(reply)
        input = ""
    }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("My AI Agent", style = MaterialTheme.typography.headlineMedium)
            Text("Native Android • APK", color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    Card(Modifier.fillMaxWidth()) { Text(message.text, Modifier.padding(14.dp)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input, onValueChange = { input = it },
                    Modifier.weight(1f), placeholder = { Text("Message your agent...") }
                )
                Button(onClick = { send() }) { Text("Send") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onVoice) { Text("Voice") }
                OutlinedButton(onClick = {
                    messages.lastOrNull { !it.fromUser }?.let { onSpeak(it.text) }
                }) { Text("Speak") }
            }
        }
    }
}
