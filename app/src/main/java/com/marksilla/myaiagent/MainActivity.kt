package com.marksilla.myaiagent

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.marksilla.auraagent.AuraService
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
    private var pendingAuraStart = false
    private var onAuraStarted: (() -> Unit)? = null

    private val microphonePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                continueAuraStartAfterPermission()
            } else {
                pendingAuraStart = false
                onAuraStarted = null
                Toast
                    .makeText(
                        this,
                        "Microphone permission is required for AURA.",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }

        setContent {
            AgentTheme {
                var isAuraEnabled by remember {
                    mutableStateOf(isAuraEnabled())
                }

                AgentApp(
                    isAuraEnabled = isAuraEnabled,
                    onSpeak = { speak(it) },
                    onVoice = { launchVoiceInput() },
                    onStartAura = {
                        startAuraWithPermissions {
                            isAuraEnabled = true
                        }
                    },
                    onStopAura = {
                        stopAuraService()
                        isAuraEnabled = false
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (
            pendingAuraStart &&
            hasRecordAudioPermission() &&
            Settings.canDrawOverlays(this)
        ) {
            startAuraService()
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

    private fun startAuraWithPermissions(onStarted: () -> Unit) {
        pendingAuraStart = true
        onAuraStarted = onStarted

        if (!hasRecordAudioPermission()) {
            microphonePermissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
            return
        }

        continueAuraStartAfterPermission()
    }

    private fun continueAuraStartAfterPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Toast
                .makeText(
                    this,
                    "Allow display over other apps to show AURA.",
                    Toast.LENGTH_SHORT
                )
                .show()

            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            return
        }

        startAuraService()
    }

    private fun startAuraService() {
        pendingAuraStart = false

        ContextCompat.startForegroundService(
            this,
            Intent(this, AuraService::class.java).apply {
                action = AuraService.ACTION_START
            }
        )

        setAuraEnabled(true)
        onAuraStarted?.invoke()
        onAuraStarted = null
    }

    private fun stopAuraService() {
        pendingAuraStart = false
        onAuraStarted = null

        stopService(
            Intent(this, AuraService::class.java).apply {
                action = AuraService.ACTION_STOP
            }
        )

        setAuraEnabled(false)
    }

    private fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    private fun isAuraEnabled(): Boolean =
        getSharedPreferences(AURA_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_AURA_ENABLED, false)

    private fun setAuraEnabled(enabled: Boolean) {
        getSharedPreferences(AURA_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AURA_ENABLED, enabled)
            .apply()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val AURA_PREFS = "aura_preferences"
        private const val KEY_AURA_ENABLED = "aura_enabled"
    }
}

@Composable
private fun AgentApp(
    isAuraEnabled: Boolean,
    onSpeak: (String) -> Unit,
    onVoice: () -> Unit,
    onStartAura: () -> Unit,
    onStopAura: () -> Unit
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
                        text = "AURA Voice Activation",
                        fontWeight =
                            androidx.compose.ui.text.font.FontWeight.SemiBold
                    )

                    Text(
                        text = if (isAuraEnabled) {
                            "AURA is active"
                        } else {
                            "AURA is off"
                        },

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )

                    Spacer(
                        modifier = Modifier.height(18.dp)
                    )

                    Button(
                        onClick = {
                            if (isAuraEnabled) {
                                onStopAura()
                            } else {
                                onStartAura()
                            }
                        }
                    ) {
                        Text(
                            if (isAuraEnabled) {
                                "Stop AURA"
                            } else {
                                "Enable AURA"
                            }
                        )
                    }

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
