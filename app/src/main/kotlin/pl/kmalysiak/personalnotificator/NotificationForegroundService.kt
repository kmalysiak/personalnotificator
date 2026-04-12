package pl.kmalysiak.personalnotificator

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.ContentValues.TAG
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.annotation.RequiresPermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import pl.kmalysiak.personalnotificator.model.dao.NotificatorDatabase
import pl.kmalysiak.personalnotificator.model.data.NotificationEntity
import pl.kmalysiak.personalnotificator.model.event.EventWithDateTimeAndStatus
import pl.kmalysiak.personalnotificator.preference.PreferencesDataUtil
import java.io.File
import javax.inject.Inject
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.util.LinkedList
import java.util.Locale
import java.util.Queue

@AndroidEntryPoint
class NotificationForegroundService : Service() {

    @Inject lateinit var notificationHandler: NotificationHandler
    @Inject lateinit var prefsDataUtil: PreferencesDataUtil

    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private val queue: Queue<String> = LinkedList()
    private var isPlaying = false
    private var ttsInitialized = false
    private lateinit var wavFile: File
    private lateinit var db: NotificatorDatabase

    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onCreate() {
        Log.i(TAG, "NotificationForegroundService, onCreate")
        super.onCreate()
        startForeground(1, createNotification())
        db = NotificatorDatabase.getDatabase(applicationContext)
        initTTS()
    }

    //tutaj wchodzi wywołanie powiadomień
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val int = intent ?: return START_NOT_STICKY
        serviceScope.launch {
            val notification = if (int.getStringExtra("source").isNullOrBlank()) {
                NotificationEntity(
                    entFriendlyName = "usługa powiadomień",
                    entType = "usługa",
                    content = "uruchomiona",
                    timestamp = Instant.now().epochSecond
                )
            } else {
                NotificationEntity(
                    entFriendlyName = int.getStringExtra("entFriendlyName") ?: "nieznany",
                    entType = int.getStringExtra("entType") ?: "nieznany",
                    content = int.getStringExtra("notificationContent") ?: "",
                    timestamp = int.getLongExtra("notificationTimestamp", Instant.now().epochSecond)
                )

            }
            db.notificationEntityDao().insert(notification)
            db.notificationEntityDao().keepLatest500()

            val text = notification.entFriendlyName + ", " + notification.content

            if (prefsDataUtil.getByName(notification.entType)) {
                queue.offer(text)
                processQueue()
            } else {
                Log.i("TTS", "TTS dla powiadomienia:${notification.entFriendlyName} jest wyłączone w preferencjach")
            }
            EventBus.getDefault().post(EventWithDateTimeAndStatus("Event", DateTimeUtil.toWarsawDt(notification.timestamp), text))
        }

        return START_NOT_STICKY
    }

    private fun initTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("pl", "PL")
                Log.i(TAG, "TTS on, locale US")
                ttsInitialized = true
                processQueue()
                EventBus.getDefault().post(EventWithDateTimeAndStatus("TTS", LocalDateTime.now(), "OK"))
            } else {
                EventBus.getDefault().post(EventWithDateTimeAndStatus("TTS", LocalDateTime.now(), "NOK"))
                Log.e("TTS", "TTS FAILURE:$status")
                ttsInitialized = false

            }
        }
    }

    private fun processQueue() {
        Log.i(TAG, "Processing queue")
        if (!ttsInitialized) {
            Log.w(TAG, "błąd inicjalizacji TTS, nie zagram dźwieku")
            return
        }

        if (isPlaying || queue.isEmpty()) return

        isPlaying = true
        val text = queue.poll() ?: return
        wavFile = File(getExternalFilesDir(null), "notification.wav")
        val params = Bundle()
        tts?.synthesizeToFile(text, params, wavFile, "ttsWav")
        Handler(Looper.getMainLooper()).postDelayed({ playWav() }, 2000)
    }

    private fun playWav() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)

        val req = AudioFocusRequest
            .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setOnAudioFocusChangeListener { }
            .build()

        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
        audioManager.abandonAudioFocusRequest(req)

        if (!wavFile.exists()) {
            isPlaying = false
            processQueue()
            return
        }
        mediaPlayer = MediaPlayer()
        mediaPlayer?.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        try {
            mediaPlayer?.setDataSource(wavFile.absolutePath)
            mediaPlayer?.prepare()
            mediaPlayer?.setOnCompletionListener {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_ALARM,
                    originalVolume,
                    0
                )
                this@NotificationForegroundService.isPlaying = false
                processQueue()
                audioManager.abandonAudioFocusRequest(req)
            }

            mediaPlayer?.start()
        } catch (e: IOException) {
            e.printStackTrace()
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)
            this@NotificationForegroundService.isPlaying = false
            processQueue()
            audioManager.abandonAudioFocusRequest(req)
        }
    }


    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun createNotification(): Notification {
        return notificationHandler.getNotificationForTtsStart("TTS")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        mediaPlayer?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null
}
