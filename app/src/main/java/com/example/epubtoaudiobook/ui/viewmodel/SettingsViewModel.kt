package com.example.epubtoaudiobook.ui.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

data class TtsVoiceInfo(val name: String, val locale: Locale, val displayName: String)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("tts_prefs", android.content.Context.MODE_PRIVATE)

    private val _voices = MutableStateFlow<List<TtsVoiceInfo>>(emptyList())
    val voices: StateFlow<List<TtsVoiceInfo>> = _voices

    private val _selectedVoiceName = MutableStateFlow(prefs.getString("tts_voice", null))
    val selectedVoiceName: StateFlow<String?> = _selectedVoiceName

    private val _selectedLocale = MutableStateFlow(prefs.getString("tts_locale", Locale.getDefault().toLanguageTag()))
    val selectedLocale: StateFlow<String?> = _selectedLocale

    private val _previewText = MutableStateFlow("Bonjour, ceci est un test de la voix sélectionnée.")
    val previewText: StateFlow<String> = _previewText

    private var tts: TextToSpeech? = null

    init {
        loadVoices()
    }

    private fun loadVoices() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = suspendCancellableCoroutine<List<TtsVoiceInfo>> { cont ->
                var ttsInstance: TextToSpeech? = null
                ttsInstance = TextToSpeech(context) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        val voices = ttsInstance?.voices
                            ?.filter { !it.isNetworkConnectionRequired }
                            ?.map { TtsVoiceInfo(it.name, it.locale, "${it.locale.displayName} (${it.name})") }
                            ?.sortedBy { it.displayName }
                            ?: emptyList()
                        ttsInstance?.shutdown()
                        cont.resume(voices)
                    } else {
                        cont.resume(emptyList())
                    }
                }
            }
            _voices.value = result
        }
    }

    fun selectVoice(voiceName: String, locale: String) {
        _selectedVoiceName.value = voiceName
        _selectedLocale.value = locale
        prefs.edit()
            .putString("tts_voice", voiceName)
            .putString("tts_locale", locale)
            .apply()
    }

    fun previewVoice() {
        val context = getApplication<Application>()
        tts?.shutdown()
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = _selectedLocale.value?.let { Locale.forLanguageTag(it) } ?: Locale.getDefault()
                tts?.language = locale
                tts?.speak(_previewText.value, TextToSpeech.QUEUE_FLUSH, null, "preview")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
    }
}

