package com.example.epubtoaudiobook.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.epubtoaudiobook.data.model.ChapterMark
import com.example.epubtoaudiobook.ui.viewmodel.PlayerViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    audiobookId: Long,
    onBack: () -> Unit,
    vm: PlayerViewModel = viewModel()
) {
    LaunchedEffect(audiobookId) { vm.loadAudiobook(audiobookId) }

    val audiobook by vm.audiobook.collectAsState()
    val chapters by vm.chapters.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val positionMs by vm.currentPositionMs.collectAsState()
    val currentChapterIndex by vm.currentChapterIndex.collectAsState()
    val speed by vm.playbackSpeed.collectAsState()

    var showChapters by remember { mutableStateOf(false) }
    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(audiobook?.title ?: "Lecteur", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { showChapters = !showChapters }) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = "Chapitres")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Cover art
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (audiobook?.coverPath != null) {
                    AsyncImage(
                        model = File(audiobook!!.coverPath!!),
                        contentDescription = "Couverture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Headphones,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center).size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Title & chapter
            Text(audiobook?.title ?: "", fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(audiobook?.author ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            chapters.getOrNull(currentChapterIndex)?.let { chap ->
                Text(chap.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(16.dp))

            // Seek bar
            val duration = audiobook?.durationMs ?: 1L
            Slider(
                value = positionMs.coerceIn(0, duration).toFloat(),
                onValueChange = { vm.seekTo(it.toLong()) },
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(positionMs), style = MaterialTheme.typography.labelSmall)
                Text(formatDuration(duration), style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(16.dp))

            // Main controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // -10s
                IconButton(onClick = { vm.seekBack10s() }) {
                    Icon(Icons.Default.Replay10, contentDescription = "-10s", modifier = Modifier.size(32.dp))
                }
                // Previous chapter
                IconButton(onClick = { vm.previousChapter() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Chapitre précédent", modifier = Modifier.size(36.dp))
                }
                // Play/Pause
                FilledIconButton(
                    onClick = { vm.playPause() },
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Lecture",
                        modifier = Modifier.size(36.dp)
                    )
                }
                // Next chapter
                IconButton(onClick = { vm.nextChapter() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Chapitre suivant", modifier = Modifier.size(36.dp))
                }
                // +10s
                IconButton(onClick = { vm.seekForward10s() }) {
                    Icon(Icons.Default.Forward10, contentDescription = "+10s", modifier = Modifier.size(32.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Debut du chapitre
            TextButton(onClick = { vm.seekToBeginningOfChapter() }) {
                Icon(Icons.Default.FirstPage, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Début du chapitre", style = MaterialTheme.typography.labelMedium)
            }

            Spacer(Modifier.height(12.dp))

            // Speed selector
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vitesse :", style = MaterialTheme.typography.labelMedium)
                speeds.forEach { s ->
                    val selected = speed == s
                    FilterChip(
                        selected = selected,
                        onClick = { vm.setSpeed(s) },
                        label = { Text("${s}x", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Chapter list
            if (showChapters) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Chapitres", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                val listState = rememberLazyListState()
                LaunchedEffect(currentChapterIndex) {
                    listState.animateScrollToItem(currentChapterIndex)
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    itemsIndexed(chapters) { index, chapter ->
                        ChapterListItem(
                            chapter = chapter,
                            isCurrent = index == currentChapterIndex,
                            onClick = { vm.seekToChapter(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterListItem(chapter: ChapterMark, isCurrent: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCurrent) {
            Icon(
                Icons.Default.VolumeUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
        } else {
            Spacer(Modifier.width(26.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                chapter.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            formatDuration(chapter.startMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

