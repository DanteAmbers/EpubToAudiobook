package com.shell.epubtoaudiobook.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.shell.epubtoaudiobook.data.model.AudiobookEntity
import com.shell.epubtoaudiobook.data.model.ChapterMark
import com.shell.epubtoaudiobook.data.model.PlaybackStateEntity
import com.shell.epubtoaudiobook.data.repository.AudiobookRepository
import com.shell.epubtoaudiobook.data.repository.PlaybackRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

class PlayerViewModel(app: Application) : AndroidViewModel(app) {
    val player: ExoPlayer = ExoPlayer.Builder(app).build()
    private val audiobookRepo = AudiobookRepository(app)
    private val playbackRepo = PlaybackRepository(app)
    private val gson = Gson()

    private val _audiobook = MutableStateFlow<AudiobookEntity?>(null)
    val audiobook: StateFlow<AudiobookEntity?> = _audiobook

    private val _chapters = MutableStateFlow<List<ChapterMark>>(emptyList())
    val chapters: StateFlow<List<ChapterMark>> = _chapters

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs

    private val _currentChapterIndex = MutableStateFlow(0)
    val currentChapterIndex: StateFlow<Int> = _currentChapterIndex

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed

    private var positionSaveJob: Job? = null
    private var currentAudiobookId: Long = -1L

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) startSavingPosition() else stopSavingPosition()
            }
        })

        viewModelScope.launch {
            while (true) {
                if (player.isPlaying) {
                    val pos = player.currentPosition
                    _currentPositionMs.value = pos
                    _currentChapterIndex.value = getCurrentChapterIndex(pos)
                }
                delay(500)
            }
        }
    }

    fun loadAudiobook(audiobookId: Long) {
        if (currentAudiobookId == audiobookId) return
        viewModelScope.launch {
            val ab = audiobookRepo.getById(audiobookId) ?: return@launch
            _audiobook.value = ab
            currentAudiobookId = audiobookId

            val type = object : TypeToken<List<ChapterMark>>() {}.type
            val chaps: List<ChapterMark> = gson.fromJson(ab.chaptersJson, type) ?: emptyList()
            _chapters.value = chaps

            val coverUri = ab.coverPath?.let { Uri.fromFile(File(it)) }
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse("file://${ab.filePath}"))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(ab.title)
                        .setArtist(ab.author)
                        .setArtworkUri(coverUri)
                        .build()
                )
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()

            // Restore position
            val state = playbackRepo.getState(audiobookId)
            if (state != null && state.positionMs > 0) {
                player.seekTo(state.positionMs)
                _currentPositionMs.value = state.positionMs
                _currentChapterIndex.value = state.chapterIndex
            }
        }
    }

    fun playPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekForward10s() {
        player.seekTo((player.currentPosition + 10_000).coerceAtMost(player.duration.coerceAtLeast(0)))
    }

    fun seekBack10s() {
        player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0))
    }

    fun seekToChapter(index: Int) {
        val chaps = _chapters.value
        val chap = chaps.getOrNull(index) ?: return
        player.seekTo(chap.startMs)
        _currentChapterIndex.value = index
    }

    fun previousChapter() {
        val chaps = _chapters.value
        val currentIndex = getCurrentChapterIndex(player.currentPosition)
        val chap = chaps.getOrNull(currentIndex) ?: return
        // If more than 3s into chapter, go to its start; otherwise go to previous
        if (player.currentPosition - chap.startMs > 3000 && currentIndex > 0) {
            player.seekTo(chap.startMs)
        } else {
            seekToChapter((currentIndex - 1).coerceAtLeast(0))
        }
    }

    fun nextChapter() {
        val currentIndex = getCurrentChapterIndex(player.currentPosition)
        seekToChapter((currentIndex + 1).coerceAtMost(_chapters.value.size - 1))
    }

    fun seekToBeginningOfChapter() {
        val chaps = _chapters.value
        val index = getCurrentChapterIndex(player.currentPosition)
        chaps.getOrNull(index)?.let { player.seekTo(it.startMs) }
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        player.setPlaybackSpeed(speed)
    }

    fun seekTo(posMs: Long) {
        player.seekTo(posMs)
    }

    private fun getCurrentChapterIndex(positionMs: Long): Int {
        val chaps = _chapters.value
        if (chaps.isEmpty()) return 0
        var result = 0
        for ((i, ch) in chaps.withIndex()) {
            if (positionMs >= ch.startMs) result = i else break
        }
        return result
    }

    private fun startSavingPosition() {
        positionSaveJob?.cancel()
        positionSaveJob = viewModelScope.launch {
            while (true) {
                delay(5000)
                savePosition()
            }
        }
    }

    private fun stopSavingPosition() {
        positionSaveJob?.cancel()
        viewModelScope.launch { savePosition() }
    }

    private suspend fun savePosition() {
        if (currentAudiobookId < 0) return
        playbackRepo.saveState(
            PlaybackStateEntity(
                audiobookId = currentAudiobookId,
                positionMs = player.currentPosition,
                chapterIndex = getCurrentChapterIndex(player.currentPosition),
                lastListenedAt = System.currentTimeMillis()
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { savePosition() }
        player.release()
    }
}

