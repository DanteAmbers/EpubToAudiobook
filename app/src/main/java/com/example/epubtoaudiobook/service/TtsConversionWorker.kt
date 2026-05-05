package com.example.epubtoaudiobook.service

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.work.*
import com.example.epubtoaudiobook.data.model.*
import com.example.epubtoaudiobook.data.repository.*
import com.google.gson.Gson
import nl.siegmann.epublib.epub.EpubReader
import org.jsoup.Jsoup
import java.io.*
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TtsConversionWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_EPUB_ID = "epub_id"
        const val KEY_EPUB_URI = "epub_uri"
        const val PROGRESS_KEY = "progress"
        const val TAG = "TtsConversion"

        fun buildRequest(epubId: Long, epubUri: String): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<TtsConversionWorker>()
                .setInputData(workDataOf(KEY_EPUB_ID to epubId, KEY_EPUB_URI to epubUri))
                .addTag(TAG)
                .build()
    }

    private val epubRepo = EpubRepository(context)
    private val audiobookRepo = AudiobookRepository(context)
    private val gson = Gson()

    override suspend fun doWork(): Result {
        val epubId = inputData.getLong(KEY_EPUB_ID, -1L)
        val epubUri = inputData.getString(KEY_EPUB_URI) ?: return Result.failure()

        val epubEntity = epubRepo.getById(epubId) ?: return Result.failure()
        epubRepo.update(epubEntity.copy(conversionStatus = ConversionStatus.CONVERTING, conversionProgress = 0))

        return try {
            val chapters = parseEpub(Uri.parse(epubUri), epubEntity)
            if (chapters.isEmpty()) {
                epubRepo.update(epubEntity.copy(conversionStatus = ConversionStatus.ERROR))
                return Result.failure()
            }

            val outputDir = getAudiobookDir()
            val safeTitle = epubEntity.title.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").take(50)
            val outputFile = File(outputDir, "$safeTitle.wav")

            val tts = initTts() ?: run {
                epubRepo.update(epubEntity.copy(conversionStatus = ConversionStatus.ERROR))
                return Result.failure()
            }

            // Apply saved TTS settings
            val prefs = context.getSharedPreferences("tts_prefs", Context.MODE_PRIVATE)
            val engine = prefs.getString("tts_engine", null)
            val locale = prefs.getString("tts_locale", null)
            if (locale != null) {
                tts.language = Locale.forLanguageTag(locale)
            }

            val tempFiles = mutableListOf<File>()
            val chapterMarks = mutableListOf<ChapterMark>()
            var cumulativeMs = 0L

            for ((index, chapter) in chapters.withIndex()) {
                val progress = ((index.toFloat() / chapters.size) * 100).toInt()
                setProgress(workDataOf(PROGRESS_KEY to progress))
                epubRepo.update(epubEntity.copy(conversionStatus = ConversionStatus.CONVERTING, conversionProgress = progress))

                val tempFile = File(context.cacheDir, "chapter_${epubId}_$index.wav")
                val success = synthesizeChapter(tts, chapter.text, tempFile)
                if (!success) continue

                chapterMarks.add(ChapterMark(index, chapter.title, cumulativeMs))
                cumulativeMs += getWavDuration(tempFile)
                tempFiles.add(tempFile)
            }

            tts.shutdown()

            // Concatenate all WAV files
            if (tempFiles.isEmpty()) {
                epubRepo.update(epubEntity.copy(conversionStatus = ConversionStatus.ERROR))
                return Result.failure()
            }
            concatenateWavFiles(tempFiles, outputFile)
            tempFiles.forEach { it.delete() }

            val audiobookId = audiobookRepo.insert(
                AudiobookEntity(
                    epubId = epubId,
                    title = epubEntity.title,
                    author = epubEntity.author,
                    filePath = outputFile.absolutePath,
                    coverPath = epubEntity.coverPath,
                    durationMs = cumulativeMs,
                    chaptersJson = gson.toJson(chapterMarks)
                )
            )

            epubRepo.update(epubEntity.copy(
                conversionStatus = ConversionStatus.DONE,
                conversionProgress = 100,
                audiobookId = audiobookId
            ))

            setProgress(workDataOf(PROGRESS_KEY to 100))
            Result.success(workDataOf("audiobook_id" to audiobookId))
        } catch (e: Exception) {
            Log.e(TAG, "Conversion failed", e)
            epubRepo.update(epubEntity.copy(conversionStatus = ConversionStatus.ERROR))
            Result.failure()
        }
    }

    private data class Chapter(val title: String, val text: String)

    private fun parseEpub(uri: Uri, entity: EpubBookEntity): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return chapters
            val book = EpubReader().readEpub(inputStream)

            // Save cover image
            book.coverImage?.let { resource ->
                try {
                    val coverFile = File(context.filesDir, "cover_${entity.id}.jpg")
                    coverFile.writeBytes(resource.data)
                } catch (_: Exception) {}
            }

            for ((index, item) in book.spine.spineReferences.withIndex()) {
                val resource = item.resource ?: continue
                val html = String(resource.data, Charsets.UTF_8)
                val text = Jsoup.parse(html).text().trim()
                if (text.isBlank()) continue
                val title = resource.title?.takeIf { it.isNotBlank() }
                    ?: book.tableOfContents.tocReferences.getOrNull(index)?.title
                    ?: "Chapitre ${index + 1}"
                chapters.add(Chapter(title, text))
            }
        } catch (e: Exception) {
            Log.e(TAG, "EPUB parse error", e)
        }
        return chapters
    }

    private suspend fun initTts(): TextToSpeech? = suspendCoroutine { cont ->
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) cont.resume(tts)
            else cont.resume(null)
        }
    }

    private suspend fun synthesizeChapter(tts: TextToSpeech, text: String, outputFile: File): Boolean =
        suspendCoroutine { cont ->
            val utteranceId = UUID.randomUUID().toString()
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) {}
                override fun onDone(utteranceId: String) { cont.resume(true) }
                override fun onError(utteranceId: String) { cont.resume(false) }
            })
            val result = tts.synthesizeToFile(text, Bundle(), outputFile, utteranceId)
            if (result != TextToSpeech.SUCCESS) cont.resume(false)
        }

    private fun getWavDuration(file: File): Long {
        if (!file.exists()) return 0L
        try {
            val player = MediaPlayer()
            player.setDataSource(file.absolutePath)
            player.prepare()
            val duration = player.duration.toLong()
            player.release()
            return duration
        } catch (e: Exception) {
            return 0L
        }
    }

    private fun concatenateWavFiles(files: List<File>, output: File) {
        if (files.size == 1) {
            files[0].copyTo(output, overwrite = true)
            return
        }
        // Read first file header to get format
        val firstBytes = files[0].readBytes()
        if (firstBytes.size < 44) {
            files[0].copyTo(output, overwrite = true)
            return
        }

        // Collect all PCM data (skip 44-byte WAV header for all files)
        val allPcm = mutableListOf<ByteArray>()
        for (f in files) {
            val bytes = f.readBytes()
            if (bytes.size > 44) allPcm.add(bytes.copyOfRange(44, bytes.size))
        }
        val totalPcm = allPcm.sumOf { it.size }

        FileOutputStream(output).use { fos ->
            // Write WAV header from first file
            fos.write(firstBytes.copyOfRange(0, 44))
            // Patch data chunk size at offset 40
            val dataSize = totalPcm
            val fileSizeBytes = ByteArray(4)
            writeInt32LE(fileSizeBytes, 0, dataSize)
            fos.channel.position(40)
            fos.write(fileSizeBytes)
            // Also patch RIFF chunk size at offset 4
            val riffSize = dataSize + 36
            val riffSizeBytes = ByteArray(4)
            writeInt32LE(riffSizeBytes, 0, riffSize)
            fos.channel.position(4)
            fos.write(riffSizeBytes)
            // Write all PCM data
            fos.channel.position(44)
            for (pcm in allPcm) fos.write(pcm)
        }
    }

    private fun writeInt32LE(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = (value and 0xFF).toByte()
        buf[offset + 1] = ((value shr 8) and 0xFF).toByte()
        buf[offset + 2] = ((value shr 16) and 0xFF).toByte()
        buf[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun getAudiobookDir(): File {
        val musicDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_MUSIC
        )
        val dir = File(musicDir, "EpubToAudiobook")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}

