package com.shell.epubtoaudiobook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "epub_books")
data class EpubBookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String,
    val uriString: String,
    val coverPath: String? = null,
    val importedAt: Long = System.currentTimeMillis(),
    val conversionStatus: ConversionStatus = ConversionStatus.PENDING,
    val conversionProgress: Int = 0, // 0-100
    val audiobookId: Long? = null
)

@Entity(tableName = "audiobooks")
data class AudiobookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epubId: Long,
    val title: String,
    val author: String,
    val filePath: String,
    val coverPath: String? = null,
    val durationMs: Long = 0,
    val chaptersJson: String = "[]", // JSON array of ChapterMark
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_states")
data class PlaybackStateEntity(
    @PrimaryKey val audiobookId: Long,
    val positionMs: Long = 0,
    val chapterIndex: Int = 0,
    val lastListenedAt: Long = System.currentTimeMillis()
)

enum class ConversionStatus {
    PENDING, CONVERTING, DONE, ERROR
}

data class ChapterMark(
    val index: Int,
    val title: String,
    val startMs: Long
)

