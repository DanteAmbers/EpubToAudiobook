package com.example.epubtoaudiobook.data.repository

import android.content.Context
import com.example.epubtoaudiobook.data.db.AppDatabase
import com.example.epubtoaudiobook.data.model.*
import kotlinx.coroutines.flow.Flow

class EpubRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).epubBookDao()

    fun getAllBooks(): Flow<List<EpubBookEntity>> = dao.getAllBooks()
    suspend fun getById(id: Long): EpubBookEntity? = dao.getById(id)
    suspend fun insert(book: EpubBookEntity): Long = dao.insert(book)
    suspend fun update(book: EpubBookEntity) = dao.update(book)
    suspend fun delete(id: Long) = dao.delete(id)
}

class AudiobookRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).audiobookDao()

    fun getAllAudiobooks(): Flow<List<AudiobookEntity>> = dao.getAllAudiobooks()
    suspend fun getById(id: Long): AudiobookEntity? = dao.getById(id)
    suspend fun insert(audiobook: AudiobookEntity): Long = dao.insert(audiobook)
    suspend fun update(audiobook: AudiobookEntity) = dao.update(audiobook)
    suspend fun delete(id: Long) = dao.delete(id)
}

class PlaybackRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).playbackStateDao()

    suspend fun getState(audiobookId: Long): PlaybackStateEntity? = dao.getState(audiobookId)
    suspend fun saveState(state: PlaybackStateEntity) = dao.upsert(state)
    suspend fun deleteState(audiobookId: Long) = dao.delete(audiobookId)
}

