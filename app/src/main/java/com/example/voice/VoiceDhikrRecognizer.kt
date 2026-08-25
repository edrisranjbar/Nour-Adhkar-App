package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class VoiceDhikrRecognizer(
    context: Context,
    private val currentDhikr: () -> String,
    private val onNewMatches: (Int) -> Unit,
    private val onStatus: (String) -> Unit,
    private val onModeStopped: () -> Unit = {}
) : RecognitionListener {
    val isAvailable: Boolean = SpeechRecognizer.isRecognitionAvailable(context)
    private val handler = Handler(Looper.getMainLooper())
    // On-device availability is not language-specific. A device can report it as
    // available while lacking an Arabic model, which causes an immediate error loop.
    private val recognizer = if (isAvailable) SpeechRecognizer.createSpeechRecognizer(context) else null
    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15_000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1_500L)
    }
    private var active = false
    private var highestMatchCount = 0

    init { recognizer?.setRecognitionListener(this) }

    fun start() {
        if (!isAvailable) {
            onStatus("تشخیص گفتار در این دستگاه در دسترس نیست")
            return
        }
        active = true
        startSession()
    }

    fun stop() {
        active = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.cancel()
    }

    fun destroy() {
        stop()
        recognizer?.destroy()
    }

    private fun startSession() {
        if (!active) return
        highestMatchCount = 0
        onStatus("در حال گوش دادن؛ ذکر را با صدای بلند بخوانید")
        runCatching { recognizer?.startListening(intent) }
            .onFailure { onStatus("شروع شنیدن ممکن نشد؛ دوباره تلاش کنید") }
    }

    private fun process(bundle: Bundle?) {
        val hypotheses = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
        val count = hypotheses.maxOfOrNull { countCompleteDhikr(it, currentDhikr()) } ?: 0
        val delta = (count - highestMatchCount).coerceAtLeast(0)
        if (delta > 0) {
            onNewMatches(delta)
            onStatus("ذکر شنیده شد؛ شمارش ادامه دارد")
        }
        highestMatchCount = maxOf(highestMatchCount, count)
    }

    private fun restart(delay: Long) {
        if (active) handler.postDelayed({ startSession() }, delay)
    }

    override fun onReadyForSpeech(params: Bundle?) =
        onStatus("در حال گوش دادن؛ ذکر را با صدای بلند بخوانید")
    override fun onBeginningOfSpeech() = onStatus("صدای شما شنیده می‌شود…")
    override fun onEndOfSpeech() = onStatus("در حال بررسی ذکر…")
    override fun onPartialResults(partialResults: Bundle?) = process(partialResults)
    override fun onResults(results: Bundle?) { process(results); restart(700) }
    override fun onError(error: Int) {
        if (!active) return
        when (error) {
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                onStatus("در حال گوش دادن؛ ذکر را دوباره بخوانید")
                restart(900)
            }
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                onStatus("در حال آماده‌سازی میکروفون…")
                restart(1_500)
            }
            else -> {
                active = false
                handler.removeCallbacksAndMessages(null)
                onStatus(
                    when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "دسترسی میکروفون داده نشده است"
                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "تشخیص گفتار عربی در این دستگاه آماده نیست"
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "برای تشخیص گفتار، اتصال اینترنت را بررسی کنید"
                        else -> "تشخیص صدا متوقف شد؛ دوباره تلاش کنید"
                    }
                )
                onModeStopped()
            }
        }
    }
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}
