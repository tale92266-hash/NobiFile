package com.nobifile.manager.ui.screens

import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
import android.os.ParcelFileDescriptor
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.nobifile.manager.data.isCodeExtension
import com.nobifile.manager.data.isTextExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(navController: NavController, path: String, kind: String) {
    val context = LocalContext.current
    val file = File(path)
    val scope = rememberCoroutineScope()
    // System Back closes only this viewer (one step back to the folder),
    // never the whole file manager.
    androidx.activity.compose.BackHandler { navController.popBackStack() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file.name, maxLines = 1) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = {
                        val item = com.nobifile.manager.storage.StorageRepository.fileToItem(file)
                        shareItem(context, listOf(item))
                    }) { Icon(Icons.Rounded.Share, contentDescription = "Share") }
                    IconButton(onClick = {
                        openWithChooser(context, com.nobifile.manager.storage.StorageRepository.fileToItem(file))
                    }) { Icon(Icons.Rounded.OpenInNew, contentDescription = "Open with") }
                }
            )
        }
    ) { padding ->
        if (!file.exists()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("File not found: $path")
                Text("The file may have been moved or deleted, or this location is not accessible as a file path.")
            }
            return@Scaffold
        }
        when {
            kind in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp") -> ImageViewer(file, Modifier.fillMaxSize().padding(padding))
            kind in setOf("mp4", "mkv", "webm", "mov") -> VideoViewer(file, Modifier.fillMaxSize().padding(padding))
            kind in setOf("mp3", "wav", "flac", "ogg", "m4a", "aac") -> AudioViewer(file, Modifier.fillMaxSize().padding(padding))
            kind == "pdf" -> PdfViewer(file, Modifier.fillMaxSize().padding(padding))
            isTextExtension(kind) || isCodeExtension(kind) -> TextViewer(file, kind, Modifier.fillMaxSize().padding(padding))
            else -> {
                Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    Text("Preview is not available for this format in NobiFile.")
                    Text("Use Open With to open it in another app, or Share to send it elsewhere.")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { openWithChooser(context, com.nobifile.manager.storage.StorageRepository.fileToItem(file)) }) { Text("Open with…") }
                }
            }
        }
    }
}

@Composable
fun ImageViewer(file: File, modifier: Modifier = Modifier) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var zoomed by remember { mutableStateOf(false) }
    LaunchedEffect(file.absolutePath) {
        withContext(Dispatchers.IO) {
            try {
                val opts = BitmapFactory.Options().apply { inSampleSize = 1 }
                val bmp = BitmapFactory.decodeFile(file.absolutePath, opts)
                withContext(Dispatchers.Main) { bitmap = bmp }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { error = e.message }
            }
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            error != null -> Text("Could not decode image: $error")
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = file.name,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> CircularProgressIndicator()
        }
    }
    // Zoom toggle is intentionally simple in v1: full-size decode + system viewer via Open With for pinch zoom.
    // The top-bar Open With action provides full zoom/pan in the system viewer.
}

@Composable
fun VideoViewer(file: File, modifier: Modifier = Modifier) {
    var videoView by remember { mutableStateOf<VideoView?>(null) }
    Column(modifier) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoPath(file.absolutePath)
                    setOnPreparedListener { mp ->
                        mp.isLooping = false
                        start()
                    }
                    videoView = this
                }
            },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { videoView?.let { if (it.isPlaying) it.pause() else it.start() } }) { Text("Play / Pause") }
        }
        Text("This built-in preview plays and pauses only. Use Open With for speed control, subtitles and audio selection in a full player.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp))
    }
}

@Composable
fun AudioViewer(file: File, modifier: Modifier = Modifier) {
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf(0) }
    var position by remember { mutableStateOf(0) }
    DisposableEffect(file.absolutePath) {
        val mp = MediaPlayer()
        try {
            mp.setDataSource(file.absolutePath)
            mp.prepare()
            duration = mp.duration
            player = mp
        } catch (e: Exception) {
            // Error is shown via the empty player state below.
        }
        onDispose { try { mp.release() } catch (e: Exception) { } }
    }
    Column(modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(12.dp))
        Text(file.name, style = MaterialTheme.typography.titleMedium)
        Text("Duration: ${duration / 1000}s", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            val mp = player ?: return@Button
            if (mp.isPlaying) { mp.pause(); playing = false } else { mp.start(); playing = true }
        }) { Text(if (playing) "Pause" else "Play") }
        Spacer(Modifier.height(8.dp))
        Text("Playback happens inside NobiFile while this screen is open. Background playback is not claimed in this version.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PdfViewer(file: File, modifier: Modifier = Modifier) {
    var pageCount by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(0) }
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }

    DisposableEffect(file.absolutePath) {
        try {
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pfd = fd
            val r = PdfRenderer(fd)
            renderer = r
            pageCount = r.pageCount
        } catch (e: Exception) {
            error = e.message
        }
        onDispose {
            try { renderer?.close() } catch (e: Exception) { }
            try { pfd?.close() } catch (e: Exception) { }
        }
    }

    LaunchedEffect(page, renderer) {
        val r = renderer ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                r.openPage(page).use { pg ->
                    val bmp = android.graphics.Bitmap.createBitmap(pg.width * 2, pg.height * 2, android.graphics.Bitmap.Config.ARGB_8888)
                    pg.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    withContext(Dispatchers.Main) { bitmap = bmp }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { error = e.message }
            }
        }
    }

    Column(modifier.padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { if (page > 0) page-- }, enabled = page > 0) { Text("Previous") }
            Text("Page ${page + 1} of $pageCount", modifier = Modifier.weight(1f))
            Button(onClick = { if (page < pageCount - 1) page++ }, enabled = page < pageCount - 1) { Text("Next") }
        }
        Spacer(Modifier.height(8.dp))
        if (error != null) Text("Could not open PDF: $error")
        else if (bitmap != null) {
            Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = "Page ${page + 1}", modifier = Modifier.fillMaxSize())
        } else CircularProgressIndicator()
        Text("Text search and selection are not available in this built-in preview. Use Open With for a full PDF reader.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TextViewer(file: File, kind: String, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var wrap by remember { mutableStateOf(true) }
    var fontSize by remember { mutableStateOf(14f) }
    val scroll = rememberScrollState()
    LaunchedEffect(file.absolutePath) {
        withContext(Dispatchers.IO) {
            try {
                val content = if (file.length() > 2_000_000) {
                    file.bufferedReader().use { it.readText().take(200000) } + "\n\n[Preview truncated: file is larger than 2 MB.]"
                } else file.readText()
                withContext(Dispatchers.Main) { text = content }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { error = e.message }
            }
        }
    }
    Column(modifier.padding(12.dp)) {
        OutlinedTextField(query, { query = it }, label = { Text("Find in file") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = wrap, onCheckedChange = { wrap = it })
            Text("Word wrap")
            Spacer(Modifier.width(12.dp))
            Text("Size")
            Slider(value = fontSize, onValueChange = { fontSize = it }, valueRange = 10f..22f, modifier = Modifier.weight(1f))
        }
        if (error != null) Text("Could not read file: $error")
        else {
            val display = if (query.isBlank()) text else text.lines().filter { it.contains(query, ignoreCase = true) }.joinToString("\n")
            val lines = display.lines()
            Box(Modifier.weight(1f).verticalScroll(scroll)) {
                Column {
                    lines.forEachIndexed { i, line ->
                        Row {
                            Text(String.format("%4d  ", i + 1), style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = fontSize.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(line, style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = fontSize.sp))
                        }
                    }
                }
            }
            Text("Read-only preview. Lines: ${text.lines().size}. Full editing is in Nobi Code.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
