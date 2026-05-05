package com.example.epubtoaudiobook.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.epubtoaudiobook.data.model.*
import com.example.epubtoaudiobook.data.repository.EpubRepository
import com.example.epubtoaudiobook.service.TtsConversionWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import nl.siegmann.epublib.epub.EpubReader
import java.io.File

class EpubLibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = EpubRepository(app)
    val books: StateFlow<List<EpubBookEntity>> = repo.getAllBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun importEpub(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val stream = context.contentResolver.openInputStream(uri) ?: return@launch
                val book = EpubReader().readEpub(stream)
                val title = book.title?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment ?: "Livre inconnu"
                val author = book.metadata.authors.firstOrNull()?.let {
                    "${it.firstname} ${it.lastname}".trim()
                } ?: "Auteur inconnu"

                // Save cover
                var coverPath: String? = null
                book.coverImage?.let { res ->
                    try {
                        val coverFile = File(context.filesDir, "cover_import_${System.currentTimeMillis()}.jpg")
                        coverFile.writeBytes(res.data)
                        coverPath = coverFile.absolutePath
                    } catch (_: Exception) {}
                }

                // Take persistent URI permission
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                val id = repo.insert(
                    EpubBookEntity(
                        title = title,
                        author = author,
                        uriString = uri.toString(),
                        coverPath = coverPath
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startConversion(book: EpubBookEntity) {
        val context = getApplication<Application>()
        val request = TtsConversionWorker.buildRequest(book.id, book.uriString)
        val workManager = WorkManager.getInstance(context)
        workManager.enqueue(request)

        // Observe progress
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(request.id).collect { info ->
                if (info == null) return@collect
                val progress = info.progress.getInt(TtsConversionWorker.PROGRESS_KEY, 0)
                // DB is updated by the worker itself, nothing extra needed here
            }
        }
    }

    fun deleteBook(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }
}

