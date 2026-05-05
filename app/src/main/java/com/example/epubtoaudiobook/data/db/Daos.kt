package com.example.epubtoaudiobook.data.db

import androidx.room.*
import com.example.epubtoaudiobook.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EpubBookDao {
    @Query("SELECT * FROM epub_books ORDER BY importedAt DESC")
    fun getAllBooks(): Flow<List<EpubBookEntity>>

    @Query("SELECT * FROM epub_books WHERE id = :id")
    suspend fun getById(id: Long): EpubBookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: EpubBookEntity): Long

    @Update
    suspend fun update(book: EpubBookEntity)

    @Query("DELETE FROM epub_books WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface AudiobookDao {
    @Query("SELECT * FROM audiobooks ORDER BY createdAt DESC")
    fun getAllAudiobooks(): Flow<List<AudiobookEntity>>

    @Query("SELECT * FROM audiobooks WHERE id = :id")
    suspend fun getById(id: Long): AudiobookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(audiobook: AudiobookEntity): Long

    @Update
    suspend fun update(audiobook: AudiobookEntity)

    @Query("DELETE FROM audiobooks WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface PlaybackStateDao {
    @Query("SELECT * FROM playback_states WHERE audiobookId = :audiobookId")
    suspend fun getState(audiobookId: Long): PlaybackStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: PlaybackStateEntity)

    @Query("DELETE FROM playback_states WHERE audiobookId = :audiobookId")
    suspend fun delete(audiobookId: Long)
}

