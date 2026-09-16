package com.marksilla.auraagent

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.marksilla.myaiagent.MainActivity
import java.util.Locale

class AuraService : Service() {
    private enum class ListeningMode {
        WAKE_MODE,
        COMMAND_MODE
    }

    private val handler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var overlay: AuraEdgeOverlay? = null
    private var currentMode: ListeningMode? = null
    private var wakeTransitionInProgress = false
    private var consecutiveWakeErrors = 0
    private var isStopping = false
    private var wakeRestartRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        overlay = AuraEdgeOverlay(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        if (intent?.action == ACTION_STOP) {
            stopAura()
            return START_NOT_STICKY
        }

        if (!hasMicrophonePermission()) {
            stopAura()
            return START_NOT_STICKY
        }

        startAsForegroundService()

        if (speechRecognizer == null && currentMode != ListeningMode.WAKE_MODE) {
            startWakeMode()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopAura()
        super.onDestroy()
    }

    private fun startAsForegroundService() {
        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("AURA is active")
            .setContentText("Say \"Hey AURA\" to activate")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AURA voice activation",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Foreground service for AURA voice activation"
            setShowBadge(false)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun startWakeMode(delayMs: Long = 0L) {
        if (isStopping) {
            return
        }

        clearWakeRestart()

        if (delayMs > 0L) {
            val restartRunnable = Runnable {
                wakeRestartRunnable = null
                startListening(ListeningMode.WAKE_MODE)
            }

            wakeRestartRunnable = restartRunnable
            handler.postDelayed(restartRunnable, delayMs)
        } else {
            startListening(ListeningMode.WAKE_MODE)
        }
    }

    private fun startCommandMode() {
        if (isStopping) {
            return
        }

        wakeTransitionInProgress = false
        consecutiveWakeErrors = 0
        overlay?.showListening()
        startListening(ListeningMode.COMMAND_MODE)
    }

    private fun startListening(mode: ListeningMode) {
        if (isStopping || !hasMicrophonePermission()) {
            stopAura()
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            if (mode == ListeningMode.WAKE_MODE) {
                scheduleWakeRestart()
            } else {
                finishCommandAndReturnToWake("Couldn't listen")
            }
            return
        }

        releaseRecognizer()
        currentMode = mode

        val recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer = recognizer

        recognizer.setRecognitionListener(
            createRecognitionListener(mode)
        )

        runCatching {
            recognizer.startListening(createRecognizerIntent())
        }.onFailure {
            if (mode == ListeningMode.WAKE_MODE) {
                scheduleWakeRestart()
            } else {
                finishCommandAndReturnToWake("Couldn't listen")
            }
        }
    }

    private fun createRecognizerIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault().toLanguageTag()
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }

    private fun createRecognitionListener(mode: ListeningMode): RecognitionListener =
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                if (mode != currentMode || isStopping) {
                    return
                }

                if (mode == ListeningMode.WAKE_MODE) {
                    scheduleWakeRestart(error)
                } else {
                    val message =
                        if (
                            error == SpeechRecognizer.ERROR_NO_MATCH ||
                            error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                        ) {
                            "I didn't catch that"
                        } else {
                            "Couldn't listen"
                        }

                    finishCommandAndReturnToWake(message)
                }
            }

            override fun onResults(results: Bundle?) {
                if (mode != currentMode || isStopping) {
                    return
                }

                val phrases = results.recognitionPhrases()

                if (mode == ListeningMode.WAKE_MODE) {
                    handleWakeResults(phrases)
                } else {
                    handleCommandResults(phrases)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (mode != currentMode || isStopping) {
                    return
                }

                if (mode == ListeningMode.WAKE_MODE) {
                    handleWakeResults(partialResults.recognitionPhrases())
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }

    private fun handleWakeResults(phrases: List<String>) {
        if (
            wakeTransitionInProgress ||
            !phrases.any(::containsWakeWord)
        ) {
            return
        }

        wakeTransitionInProgress = true
        releaseRecognizer()
        overlay?.showListening()

        handler.postDelayed(
            { startCommandMode() },
            COMMAND_START_DELAY_MS
        )
    }

    private fun handleCommandResults(phrases: List<String>) {
        val requestedApp = phrases
            .asSequence()
            .mapNotNull(::extractOpenCommand)
            .firstOrNull()

        if (requestedApp == null) {
            finishCommandAndReturnToWake("App not found")
            return
        }

        val app = findApp(
            apps = getInstalledApps(this),
            requestedName = requestedApp
        )

        if (app == null) {
            finishCommandAndReturnToWake("App not found")
            return
        }

        val opened = openApp(this, app)

        if (opened) {
            overlay?.hide()
            startWakeMode(COMMAND_FINISH_DELAY_MS)
        } else {
            finishCommandAndReturnToWake("Couldn't open the app")
        }
    }

    private fun finishCommandAndReturnToWake(message: String) {
        releaseRecognizer()
        overlay?.showStatus(message)

        handler.postDelayed(
            {
                overlay?.hide()
                startWakeMode()
            },
            STATUS_DISPLAY_MS
        )
    }

    private fun scheduleWakeRestart(error: Int? = null) {
        releaseRecognizer()
        overlay?.hide()

        if (isStopping || isFatalWakeError(error)) {
            return
        }

        consecutiveWakeErrors =
            (consecutiveWakeErrors + 1).coerceAtMost(MAX_WAKE_ERROR_BACKOFF_STEPS)

        val delayMs = BASE_WAKE_RESTART_DELAY_MS +
            (consecutiveWakeErrors - 1) * WAKE_RESTART_BACKOFF_MS

        startWakeMode(delayMs)
    }

    private fun isFatalWakeError(error: Int?): Boolean =
        error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS

    private fun releaseRecognizer() {
        clearWakeRestart()

        speechRecognizer?.let { recognizer ->
            recognizer.setRecognitionListener(null)

            runCatching {
                recognizer.stopListening()
            }

            runCatching {
                recognizer.cancel()
            }

            runCatching {
                recognizer.destroy()
            }
        }

        speechRecognizer = null
    }

    private fun clearWakeRestart() {
        wakeRestartRunnable?.let { runnable ->
            handler.removeCallbacks(runnable)
        }

        wakeRestartRunnable = null
    }

    private fun stopAura() {
        if (isStopping) {
            return
        }

        isStopping = true
        handler.removeCallbacksAndMessages(null)
        releaseRecognizer()
        overlay?.hide()
        overlay = null
        currentMode = null
        wakeTransitionInProgress = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun hasMicrophonePermission(): Boolean =
        checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun Bundle?.recognitionPhrases(): List<String> =
        this
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            .orEmpty()

    companion object {
        const val ACTION_START = "com.marksilla.auraagent.action.START"
        const val ACTION_STOP = "com.marksilla.auraagent.action.STOP"

        private const val CHANNEL_ID = "aura_voice_activation"
        private const val NOTIFICATION_ID = 77
        private const val BASE_WAKE_RESTART_DELAY_MS = 900L
        private const val WAKE_RESTART_BACKOFF_MS = 600L
        private const val MAX_WAKE_ERROR_BACKOFF_STEPS = 5
        private const val COMMAND_START_DELAY_MS = 250L
        private const val COMMAND_FINISH_DELAY_MS = 450L
        private const val STATUS_DISPLAY_MS = 1200L
    }
}
