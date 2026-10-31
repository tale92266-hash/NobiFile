package com.nobifile.manager.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.nobifile.manager.data.FileItem
import com.nobifile.manager.data.StorageSummary
import com.nobifile.manager.data.SortOrder
import com.nobifile.manager.data.ViewMode
import com.nobifile.manager.data.mimeForExtension
import java.io.File

object StorageRepository {

    fun internalRoot(): File = Environment.getExternalStorageDirectory()

    fun hasAllFilesAccess(): Boolean = Environment.isExternalStorageManager()

    fun storageSummary(path: String): StorageSummary {
        return try {
            val stat = StatFs(path)
            val total = stat.blockCountLong * stat.blockSizeLong
            val free = stat.availableBlocksLong * stat.blockSizeLong
            StorageSummary(total, free, total - free, path)
        } catch (e: Exception) {
            StorageSummary(0, 0, 0, path)
        }
    }

    fun fileToItem(file: File): FileItem {
        val ext = file.extension.lowercase()
        return FileItem(
            name = file.name,
            displayPath = file.absolutePath,
            file = file,
            treeUri = null,
            documentId = null,
            isDirectory = file.isDirectory,
            size = if (file.isFile) file.length() else 0L,
            lastModified = file.lastModified(),
            isHidden = file.name.startsWith("."),
            canRead = file.canRead(),
            canWrite = file.canWrite(),
            mimeType = if (file.isDirectory) null else mimeForExtension(ext),
            provider = "file"
        )
    }

    fun listDirectory(
        dir: File,
        showHidden: Boolean,
        sort: SortOrder,
        foldersFirst: Boolean
    ): List<FileItem> {
        val files = try { dir.listFiles() } catch (e: Exception) { null } ?: return emptyList()
        val items = files.filter { showHidden || !it.name.startsWith(".") }.map { fileToItem(it) }
        return sortItems(items, sort, foldersFirst)
    }

    fun sortItems(items: List<FileItem>, sort: SortOrder, foldersFirst: Boolean): List<FileItem> {
        val sorted = when (sort) {
            SortOrder.NAME_ASC -> items.sortedBy { it.name.lowercase() }
            SortOrder.NAME_DESC -> items.sortedByDescending { it.name.lowercase() }
            SortOrder.DATE_NEW -> items.sortedByDescending { it.lastModified }
            SortOrder.DATE_OLD -> items.sortedBy { it.lastModified }
            SortOrder.SIZE_LARGE -> items.sortedByDescending { it.size }
            SortOrder.SIZE_SMALL -> items.sortedBy { it.size }
            SortOrder.TYPE -> items.sortedBy { it.extension }
        }
        return when (foldersFirst) {
            true -> sorted.sortedByDescending { it.isDirectory }
            false -> sorted
        }.let { list ->
            // FOLDERS_FIRST=false means mixed; FILES_FIRST handled by caller re-sort if needed.
            list
        }
    }

    fun applyFolderOrder(items: List<FileItem>, foldersFirst: Boolean, filesFirst: Boolean): List<FileItem> {
        return when {
            foldersFirst -> items.sortedByDescending { it.isDirectory }
            filesFirst -> items.sortedBy { it.isDirectory }
            else -> items
        }
    }

    fun documentToItem(context: Context, doc: DocumentFile, treeUri: Uri): FileItem {
        val name = doc.name ?: "?"
        val ext = name.substringAfterLast('.', "").lowercase()
        return FileItem(
            name = name,
            displayPath = doc.uri.toString(),
            file = null,
            treeUri = treeUri.toString(),
            documentId = try { DocumentsContract.getDocumentId(doc.uri) } catch (e: Exception) { null },
            isDirectory = doc.isDirectory,
            size = if (doc.isFile) doc.length() else 0L,
            lastModified = doc.lastModified(),
            isHidden = name.startsWith("."),
            canRead = doc.canRead(),
            canWrite = doc.canWrite(),
            mimeType = if (doc.isDirectory) null else mimeForExtension(ext),
            provider = "saf"
        )
    }

    fun listSafTree(context: Context, treeUri: Uri, showHidden: Boolean, sort: SortOrder): List<FileItem> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val children = try { root.listFiles() } catch (e: Exception) { return emptyList() }
        val items = children.filter { showHidden || !(it.name ?: "").startsWith(".") }
            .map { documentToItem(context, it, treeUri) }
        return sortItems(items, sort, true)
    }

    fun persistedTrees(context: Context): List<Uri> {
        return try { context.contentResolver.persistedUriPermissions.map { it.uri } } catch (e: Exception) { emptyList() }
    }

    data class MediaQuery(val kinds: Set<String>)

    fun mediaItems(context: Context, kind: String, limit: Int = 500): List<FileItem> {
        val resolver: ContentResolver = context.contentResolver
        val uri: Uri
        val projection: Array<String>
        val selection: String?
        when (kind) {
            "image" -> { uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI; projection = arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_MODIFIED, MediaStore.Images.Media.SIZE); selection = null }
            "video" -> { uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI; projection = arrayOf(MediaStore.Video.Media.DATA, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.DATE_MODIFIED, MediaStore.Video.Media.SIZE); selection = null }
            "audio" -> { uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI; projection = arrayOf(MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DATE_MODIFIED, MediaStore.Audio.Media.SIZE); selection = null }
            else -> { uri = MediaStore.Files.getContentUri("external"); projection = arrayOf(MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.DATE_MODIFIED, MediaStore.Files.FileColumns.SIZE); selection = MediaStore.Files.FileColumns.MIME_TYPE + " IN ('application/pdf','application/msword','application/vnd.openxmlformats-officedocument.wordprocessingml.document')" }
        }
        val out = mutableListOf<FileItem>()
        try {
            resolver.query(uri, projection, selection, null, MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC LIMIT $limit")?.use { c ->
                val dataIdx = c.getColumnIndex(projection[0])
                val nameIdx = c.getColumnIndex(projection[1])
                val dateIdx = c.getColumnIndex(projection[2])
                val sizeIdx = c.getColumnIndex(projection[3])
                while (c.moveToNext()) {
                    val path = if (dataIdx >= 0) c.getString(dataIdx) else null
                    if (path.isNullOrBlank()) continue
                    val f = File(path)
                    if (!f.exists() || f.isDirectory) continue
                    val name = if (nameIdx >= 0) (c.getString(nameIdx) ?: f.name) else f.name
                    out.add(fileToItem(f).copy(name = name, lastModified = if (dateIdx >= 0) c.getLong(dateIdx) * 1000L else f.lastModified(), size = if (sizeIdx >= 0) c.getLong(sizeIdx) else f.length()))
                }
            }
        } catch (e: Exception) {
            // MediaStore unavailable or permission denied: return what we have (possibly empty).
        }
        return out
    }

    fun mediaCount(context: Context, kind: String): Int {
        return try { mediaItems(context, kind, 10000).size } catch (e: Exception) { 0 }
    }

    fun searchFiles(root: File, query: String, showHidden: Boolean, maxResults: Int = 300, onCancel: () -> Boolean): List<FileItem> {
        val out = mutableListOf<FileItem>()
        val q = query.lowercase()
        fun walk(dir: File, depth: Int) {
            if (out.size >= maxResults || onCancel() || depth > 6) return
            val children = try { dir.listFiles() } catch (e: Exception) { null } ?: return
            for (child in children) {
                if (out.size >= maxResults || onCancel()) return
                if (!showHidden && child.name.startsWith(".")) continue
                if (child.name.lowercase().contains(q)) out.add(fileToItem(child))
                if (child.isDirectory) walk(child, depth + 1)
            }
        }
        if (root.exists()) walk(root, 0)
        return out
    }

    fun directorySize(dir: File, onCancel: () -> Boolean): Long {
        var total = 0L
        fun walk(d: File) {
            if (onCancel()) return
            val children = try { d.listFiles() } catch (e: Exception) { null } ?: return
            for (c in children) {
                if (onCancel()) return
                if (c.isFile) total += c.length() else walk(c)
            }
        }
        walk(dir)
        return total
    }

    fun largeFiles(root: File, minBytes: Long, maxResults: Int = 100, onCancel: () -> Boolean): List<FileItem> {
        val out = mutableListOf<FileItem>()
        fun walk(dir: File, depth: Int) {
            if (out.size >= maxResults || onCancel() || depth > 5) return
            val children = try { dir.listFiles() } catch (e: Exception) { null } ?: return
            for (c in children) {
                if (out.size >= maxResults || onCancel()) return
                if (c.isFile && c.length() >= minBytes) out.add(fileToItem(c))
                else if (c.isDirectory) walk(c, depth + 1)
            }
        }
        walk(root, 0)
        return out.sortedByDescending { it.size }
    }

    fun emptyFolders(root: File, maxResults: Int = 100, onCancel: () -> Boolean): List<FileItem> {
        val out = mutableListOf<FileItem>()
        fun walk(dir: File, depth: Int) {
            if (out.size >= maxResults || onCancel() || depth > 5) return
            val children = try { dir.listFiles() } catch (e: Exception) { null } ?: return
            if (children.isEmpty() && dir != root) { out.add(fileToItem(dir)); return }
            for (c in children) if (c.isDirectory) walk(c, depth + 1)
        }
        walk(root, 0)
        return out
    }
}
