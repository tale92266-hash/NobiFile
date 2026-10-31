package com.nobifile.manager.data

import java.io.File

data class FileItem(
    val name: String,
    val displayPath: String,
    val file: File?,
    val treeUri: String?,
    val documentId: String?,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val isHidden: Boolean,
    val canRead: Boolean,
    val canWrite: Boolean,
    val mimeType: String?,
    val provider: String
) {
    val extension: String
        get() {
            val i = name.lastIndexOf('.')
            return if (i > 0 && i < name.length - 1) name.substring(i + 1).lowercase() else ""
        }
    val key: String get() = "$provider|$displayPath|$name"
}

enum class SortOrder { NAME_ASC, NAME_DESC, DATE_NEW, DATE_OLD, SIZE_LARGE, SIZE_SMALL, TYPE }
enum class ViewMode { LIST, COMPACT, GRID, LARGE_GRID, DETAILED }
enum class FolderOrder { FOLDERS_FIRST, FILES_FIRST, MIXED }

data class BookmarkItem(val id: Long, val name: String, val path: String, val kind: String)
data class RecentItem(val id: Long, val name: String, val path: String, val lastOpened: Long)
data class CollectionItem(val id: Long, val name: String)
data class CollectionEntry(val id: Long, val collectionId: Long, val name: String, val path: String)
data class ProjectRoot(val id: Long, val name: String, val path: String)
data class SidebarLocation(val id: Long, val name: String, val path: String, val kind: String, val position: Int, val visible: Boolean)
data class ExplorerTab(val id: Long, val name: String, val path: String, val position: Int)
data class OperationRecord(val id: Long, val kind: String, val detail: String, val result: String, val time: Long)
data class TrashEntry(val id: Long, val name: String, val originalPath: String, val trashPath: String, val deletedAt: Long, val isDirectory: Boolean)
data class SearchHistoryItem(val id: Long, val query: String, val time: Long)

enum class ConflictChoice { REPLACE, SKIP, RENAME, KEEP_BOTH }

data class StorageSummary(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val path: String
)

fun formatSize(bytes: Long): String {
    if (bytes < 0) return "—"
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var v = bytes.toDouble() / 1024.0
    var u = 0
    while (v >= 1024.0 && u < units.size - 1) { v /= 1024.0; u++ }
    return String.format("%.1f %s", v, units[u])
}

fun mimeForExtension(ext: String): String {
    return when (ext.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "bmp" -> "image/bmp"
        "svg" -> "image/svg+xml"
        "heic" -> "image/heic"
        "mp4", "m4v" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "avi" -> "video/x-msvideo"
        "mov" -> "video/quicktime"
        "webm" -> "video/webm"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "flac" -> "audio/flac"
        "ogg", "opus" -> "audio/ogg"
        "m4a", "aac" -> "audio/mp4"
        "pdf" -> "application/pdf"
        "zip" -> "application/zip"
        "tar" -> "application/x-tar"
        "gz", "tgz" -> "application/gzip"
        "apk" -> "application/vnd.android.package-archive"
        "txt", "log", "md" -> "text/plain"
        "html", "htm" -> "text/html"
        "css" -> "text/css"
        "js", "mjs" -> "text/javascript"
        "json" -> "application/json"
        "xml" -> "text/xml"
        "kt", "kts" -> "text/x-kotlin"
        "java" -> "text/x-java"
        "py" -> "text/x-python"
        "sh", "bash" -> "text/x-shellscript"
        "c", "h" -> "text/x-c"
        "cpp", "cc", "cxx", "hpp" -> "text/x-c++"
        "rs" -> "text/x-rust"
        "go" -> "text/x-go"
        "yaml", "yml" -> "text/yaml"
        "toml" -> "text/toml"
        else -> "application/octet-stream"
    }
}

fun isCodeExtension(ext: String): Boolean {
    return ext.lowercase() in setOf("kt", "kts", "java", "xml", "html", "css", "js", "ts", "jsx", "tsx", "json", "yaml", "yml", "toml", "md", "py", "c", "h", "cpp", "cc", "cxx", "hpp", "cs", "go", "rs", "php", "rb", "swift", "dart", "sql", "sh", "bash", "gradle", "properties", "dockerfile")
}

fun isTextExtension(ext: String): Boolean {
    return ext.lowercase() in setOf("txt", "log", "md", "csv") || isCodeExtension(ext)
}

fun categoryFor(item: FileItem): String {
    if (item.isDirectory) return "folder"
    return when (item.extension) {
        "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg", "heic", "tiff", "ico" -> "image"
        "mp4", "mkv", "avi", "mov", "webm", "3gp", "flv", "wmv", "m4v", "ts" -> "video"
        "mp3", "wav", "aac", "flac", "ogg", "m4a", "opus", "wma", "amr", "mid", "midi" -> "audio"
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "ods", "epub" -> "document"
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "zst", "apk", "aab" -> "archive"
        "apk", "aab" -> "app"
        else -> if (isCodeExtension(item.extension)) "code" else "other"
    }
}
