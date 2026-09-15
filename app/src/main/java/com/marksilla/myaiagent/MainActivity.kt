package com.marksilla.myaiagent

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marksilla.myaiagent.ui.AgentComposer
import com.marksilla.myaiagent.ui.AgentDrawerContent
import com.marksilla.myaiagent.ui.AgentHeader
import com.marksilla.myaiagent.ui.AgentTheme
import com.marksilla.myaiagent.ui.MessageList
import com.marksilla.myaiagent.ui.WelcomeState
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US
        }
        setContent {
            AgentTheme {
                AgentApp(
                    onSpeak = { speak(it) },
                    onVoice = { launchVoiceInput() }
                )
            }
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "agent")
    }

    private fun launchVoiceInput() {
        startActivityForResult(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }, 100
        )
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun AgentApp(onSpeak: (String) -> Unit, onVoice: () -> Unit) {
    var drawerOpen by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<AgentMessage>() }

    fun send() {
        if (!canSendMessage(input)) return
        val text = input.trim()
        messages += AgentMessage(text, true)
        messages += AgentMessage(
            "I’m ready. The premium Android shell is connected to the native voice layer. The live AI backend will be connected next.",
            false
        )
        input = ""
    }

    ModalNavigationDrawer(
        drawerState = rememberDrawerState(if (drawerOpen) DrawerValue.Open else DrawerValue.Closed),
        drawerContent = {
            AgentDrawerContent(
                onNewChat = { messages.clear(); drawerOpen = false },
                onSettings = { drawerOpen = false; showSettings = true }
            )
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { AgentHeader(onMenu = { drawerOpen = true }, onSettings = { showSettings = true }) },
            bottomBar = {
                if (!showSettings) AgentComposer(
                    input = input,
                    onInput = { input = it },
                    onSend = ::send,
                    onVoice = onVoice
                )
            }
        ) { padding ->
            if (showSettings) {
                Column(Modifier.fillMaxSize().padding(padding).padding(22.dp)) {
                    Text("Settings", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(10.dp))
                    Text("Permission Center", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    Text("Android permissions and agent capabilities will appear here as they are connected.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(18.dp))
                    OutlinedButton(onClick = { showSettings = false }) { Text("Back to chat") }
                }
            } else if (messages.isEmpty()) {
                WelcomeState(onPrompt = { input = it })
            } else {
                MessageList(messages, onSpeak, Modifier.fillMaxSize().padding(padding))
            }
        }
    }
}
