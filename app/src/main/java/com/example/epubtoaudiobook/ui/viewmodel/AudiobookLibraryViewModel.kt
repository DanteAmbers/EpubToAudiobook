package com.shell.epubtoaudiobook.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shell.epubtoaudiobook.data.model.AudiobookEntity
import com.shell.epubtoaudiobook.data.model.PlaybackStateEntity
import com.shell.epubtoaudiobook.data.repository.AudiobookRepository
import com.shell.epubtoaudiobook.data.repository.PlaybackRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AudiobookLibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val audiobookRepo = AudiobookRepository(app)
    private val playbackRepo = PlaybackRepository(app)

    val audiobooks: StateFlow<List<AudiobookEntity>> = audiobookRepo.getAllAudiobooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _playbackStates = MutableStateFlow<Map<Long, PlaybackStateEntity>>(emptyMap())
    val playbackStates: StateFlow<Map<Long, PlaybackStateEntity>> = _playbackStates

    init {
        viewModelScope.launch {
            audiobooks.collect { books ->
                val states = books.mapNotNull { book ->
                    playbackRepo.getState(book.id)?.let { book.id to it }
                }.toMap()
                _playbackStates.value = states
            }
        }
    }

    fun deleteAudiobook(id: Long) {
        viewModelScope.launch {
            audiobookRepo.getById(id)?.let { ab ->
                try { java.io.File(ab.filePath).delete() } catch (_: Exception) {}
            }
            audiobookRepo.delete(id)
            playbackRepo.deleteState(id)
        }
    }
}

