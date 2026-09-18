package pl.kmalysiak.personalnotificator.model.appnotification

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import org.slf4j.LoggerFactory
import pl.kmalysiak.personalnotificator.appstate.AppStateRepository
import pl.kmalysiak.personalnotificator.appstate.PreferencesDataUtil
import java.io.File
import java.io.IOException
import java.util.LinkedList
import java.util.Locale
import java.util.Queue

class TtsPlayer(
    private val context: Context,
    private val prefsDataUtil: PreferencesDataUtil,
    private val appStateRepo: AppStateRepository
) {
    companion object {
        // Change this to switch TTS language, e.g. Locale.US, Locale("de", "DE")
        val LOCALE: Locale = Locale("pl", "PL")
    }
    private val log = LoggerFactory.getLogger(TtsPlayer::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private data class QueueItem(val text: String, val volume: Float)

    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private val queue: Queue<QueueItem> = LinkedList()
    private var isPlaying = false
    private var ttsInitialized = false
    private var wavFile: File? = null
    private var currentVolume = 1.0f

    fun initialize() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = LOCALE
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        mainHandler.post { playWav() }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        log.error("TTS synteza nie powiodła się: {}", utteranceId)
                        isPlaying = false
                        mainHandler.post { processQueue() }
                    }
                })
                log.info("TTS on, locale {}", LOCALE)
                ttsInitialized = true
                processQueue()
                prefsDataUtil.saveTtsReady(true)
                appStateRepo.setTtsReady(true)
            } else {
                ttsInitialized = false
                log.error("TTS FAILURE: {}", status)
                prefsDataUtil.saveTtsReady(false)
                appStateRepo.setTtsReady(false)
            }
        }
    }

    // volume: głośność już ograniczona przez master (górny limit) - patrz NotificationForegroundService
    fun enqueue(text: String, volume: Float) {
        mainHandler.post {
            queue.offer(QueueItem(text, volume))
            processQueue()
        }
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        mediaPlayer?.stop()
        mediaPlayer?.release()
    }

    private fun processQueue() {
        if (!ttsInitialized) {
            log.warn("błąd inicjalizacji TTS, nie zagram dźwięku")
            return
        }
        if (isPlaying || queue.isEmpty()) return

        val item = queue.poll() ?: return
        isPlaying = true
        currentVolume = item.volume

        val file = File(context.getExternalFilesDir(null), "notification.wav")
        wavFile = file
        tts?.synthesizeToFile(item.text, Bundle(), file, "ttsWav")
    }

    private fun playWav() {
        val file = wavFile
        if (file == null || !file.exists()) {
            log.warn("plik WAV nie istnieje, pomijam odtwarzanie")
            isPlaying = false
            processQueue()
            return
        }

        val audioManager = context.getSystemService(AudioManager::class.java)
        val originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val targetVolume = (maxVolume * currentVolume).toInt().coerceIn(0, maxVolume)

        val focusRequest = AudioFocusRequest
            .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setOnAudioFocusChangeListener {}
            .build()

        audioManager.requestAudioFocus(focusRequest)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0)

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
}