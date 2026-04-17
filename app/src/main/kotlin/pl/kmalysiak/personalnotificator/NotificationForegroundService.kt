package pl.kmalysiak.personalnotificator

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.annotation.RequiresPermission
import org.slf4j.LoggerFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import pl.kmalysiak.personalnotificator.model.dao.NotificationEntityDao
import pl.kmalysiak.personalnotificator.model.data.NotificationEntity
import pl.kmalysiak.personalnotificator.model.event.EventWithDateTimeAndStatus
import pl.kmalysiak.personalnotificator.preference.PreferencesDataUtil
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.util.LinkedList
import java.util.Locale
import java.util.Queue
import javax.inject.Inject

@AndroidEntryPoint
class NotificationForegroundService : Service() {

    private val log = LoggerFactory.getLogger(NotificationForegroundService::class.java)

    @Inject lateinit var notificationHandler: NotificationHandler
    @Inject lateinit var prefsDataUtil: PreferencesDataUtil
    @Inject lateinit var dao: NotificationEntityDao

    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private val queue: Queue<String> = LinkedList()
    private var isPlaying = false
    private var ttsInitialized = false
    private var wavFile: File? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onCreate() {
        log.info("NotificationForegroundService, onCreate")
        super.onCreate()
        startForeground(1, createNotification())
        initTTS()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val safeIntent = intent ?: return START_NOT_STICKY
        serviceScope.launch {
            val notification = if (safeIntent.getStringExtra("source").isNullOrBlank()) {
                NotificationEntity(
                    entFriendlyName = "usługa powiadomień",
                    entType = "usługa",
                    content = "uruchomiona",
                    timestamp = Instant.now().epochSecond,
                    timestampReceived = Instant.now().epochSecond
                )
            } else {
                NotificationEntity(
                    entFriendlyName = safeIntent.getStringExtra("entFriendlyName") ?: "nieznany",
                    entType = safeIntent.getStringExtra("entType") ?: "nieznany",
                    content = safeIntent.getStringExtra("notificationContent") ?: "",
                    timestamp = safeIntent.getLongExtra("notificationTimestamp", Instant.now().epochSecond),
                    timestampReceived = Instant.now().epochSecond
                )
            }
            dao.insert(notification)
            dao.keepLatest500()

            val text = notification.entFriendlyName + ", " + notification.content
            val shouldPlay = prefsDataUtil.getByName(notification.entType)

            EventBus.getDefault().post(
                EventWithDateTimeAndStatus("Event", DateTimeUtil.toWarsawDt(notification.timestamp), text)
            )

            if (shouldPlay) {
                mainHandler.post {
                    queue.offer(text)
                    processQueue()
                }
            } else {
                log.info("TTS dla powiadomienia:{} jest wyłączone w preferencjach", notification.entFriendlyName)
            }
        }
        return START_NOT_STICKY
    }

    private fun initTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("pl", "PL")
                log.info("TTS on, locale PL")
                ttsInitialized = true
                processQueue()
                EventBus.getDefault().post(EventWithDateTimeAndStatus("TTS", LocalDateTime.now(), "OK"))
            } else {
                ttsInitialized = false
                log.error("TTS FAILURE: {}", status)
                EventBus.getDefault().post(EventWithDateTimeAndStatus("TTS", LocalDateTime.now(), "NOK"))
            }
        }
    }

    private fun processQueue() {
        if (!ttsInitialized) {
            log.warn("błąd inicjalizacji TTS, nie zagram dźwięku")
            return
        }
        if (isPlaying || queue.isEmpty()) return

        val text = queue.poll() ?: return
        isPlaying = true

        val file = File(getExternalFilesDir(null), "notification.wav")
        wavFile = file

        tts?.synthesizeToFile(text, Bundle(), file, "ttsWav")
        mainHandler.postDelayed({ playWav() }, 2000)
    }

    private fun playWav() {
        val file = wavFile
        if (file == null || !file.exists()) {
            log.warn("plik WAV nie istnieje, pomijam odtwarzanie")
            isPlaying = false
            processQueue()
            return
        }

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)

        val focusRequest = AudioFocusRequest
            .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setOnAudioFocusChangeListener {}
            .build()

        audioManager.requestAudioFocus(focusRequest)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer()
        mediaPlayer?.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )

        try {
            mediaPlayer?.setDataSource(file.absolutePath)
            mediaPlayer?.prepare()
            mediaPlayer?.setOnCompletionListener {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)
                audioManager.abandonAudioFocusRequest(focusRequest)
                isPlaying = false
                processQueue()
            }
            mediaPlayer?.start()
        } catch (e: IOException) {
            log.error("błąd odtwarzania WAV", e)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)
            audioManager.abandonAudioFocusRequest(focusRequest)
            isPlaying = false
            processQueue()
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun createNotification(): Notification {
        return notificationHandler.getNotificationForTtsStart("TTS")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null
}