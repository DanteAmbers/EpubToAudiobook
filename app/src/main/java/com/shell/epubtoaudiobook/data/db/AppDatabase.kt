package com.shell.epubtoaudiobook.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.shell.epubtoaudiobook.data.model.*

@Database(
    entities = [EpubBookEntity::class, AudiobookEntity::class, PlaybackStateEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun epubBookDao(): EpubBookDao
    abstract fun audiobookDao(): AudiobookDao
    abstract fun playbackStateDao(): PlaybackStateDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "epubtoaudiobook.db"
                ).build().also { INSTANCE = it }
            }
    }
}

