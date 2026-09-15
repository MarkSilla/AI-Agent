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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marksilla.myaiagent.ui.AgentComposer
import com.marksilla.myaiagent.ui.AgentDrawerContent
import com.marksilla.myaiagent.ui.AgentHeader
import com.marksilla.myaiagent.ui.AgentTheme
import com.marksilla.myaiagent.ui.MessageList
import com.marksilla.myaiagent.ui.WelcomeState
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
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
        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "agent_response"
        )
    }

    private fun launchVoiceInput() {
        startActivityForResult(
            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    Locale.getDefault()
                )
            },
            100
        )
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun AgentApp(
    onSpeak: (String) -> Unit,
    onVoice: () -> Unit
) {
    var drawerOpen by remember {
        mutableStateOf(false)
    }

    var input by remember {
        mutableStateOf("")
    }

    var showSettings by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    val messages = remember {
        mutableStateListOf<AgentMessage>()
    }

    val scope = rememberCoroutineScope()

    fun send() {

        if (!canSendMessage(input) || isLoading) {
            return
        }

        val text = input.trim()

        messages += AgentMessage(
            text = text,
            fromUser = true
        )

        input = ""
        isLoading = true

        scope.launch {

            val response = withContext(Dispatchers.IO) {

                try {
                    AgentApi.sendMessage(
                        messages = messages.toList()
                    )
                } catch (e: Exception) {
                    "I couldn't connect to the AI backend.\n\n" +
                            (e.message ?: "Unknown network error.")
                }
            }

            messages += AgentMessage(
                text = response,
                fromUser = false
            )

            isLoading = false
        }
    }

    ModalNavigationDrawer(
        drawerState = rememberDrawerState(
            if (drawerOpen) {
                DrawerValue.Open
            } else {
                DrawerValue.Closed
            }
        ),

        drawerContent = {
            AgentDrawerContent(
                onNewChat = {
                    messages.clear()
                    drawerOpen = false
                },

                onSettings = {
                    drawerOpen = false
                    showSettings = true
                }
            )
        }
    ) {

        Scaffold(

            containerColor =
                MaterialTheme.colorScheme.background,

            topBar = {
                AgentHeader(
                    onMenu = {
                        drawerOpen = true
                    },

                    onSettings = {
                        showSettings = true
                    }
                )
            },

            bottomBar = {

                if (!showSettings) {

                    AgentComposer(
                        input = input,

                        onInput = {
                            input = it
                        },

                        onSend = ::send,

                        onVoice = onVoice,

                        enabled = !isLoading
                    )
                }
            }

        ) { padding ->

            if (showSettings) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(22.dp)
                ) {

                    Text(
                        text = "Settings",
                        style =
                            MaterialTheme.typography.headlineMedium
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Text(
                        text = "Permission Center",
                        fontWeight =
                            androidx.compose.ui.text.font.FontWeight.SemiBold
                    )

                    Text(
                        text =
                            "Android permissions and agent capabilities will appear here as they are connected.",

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )

                    Spacer(
                        modifier = Modifier.height(18.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            showSettings = false
                        }
                    ) {
                        Text("Back to chat")
                    }
                }

            } else if (messages.isEmpty()) {

                WelcomeState(
                    onPrompt = {
                        input = it
                    }
                )

            } else {

                MessageList(
                    messages = messages,

                    onSpeak = onSpeak,

                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }

            if (isLoading) {

                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}
