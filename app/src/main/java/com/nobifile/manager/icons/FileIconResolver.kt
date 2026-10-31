package com.nobifile.manager.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.nobifile.manager.data.FileItem
import com.nobifile.manager.data.categoryFor

data class FileIconStyle(val icon: ImageVector, val color: Color, val chip: String)

object FileIconResolver {
    fun styleFor(item: FileItem): FileIconStyle {
        if (item.isDirectory) return FileIconStyle(Icons.Rounded.Folder, Color(0xFF3B82F6), "DIR")
        val ext = item.extension
        val cat = categoryFor(item)
        return when (cat) {
            "image" -> FileIconStyle(Icons.Rounded.Image, Color(0xFF10B981), ext.uppercase().ifBlank { "IMG" })
            "video" -> FileIconStyle(Icons.Rounded.Movie, Color(0xFF8B5CF6), ext.uppercase().ifBlank { "VID" })
            "audio" -> FileIconStyle(Icons.Rounded.MusicNote, Color(0xFFEC4899), ext.uppercase().ifBlank { "AUD" })
            "archive" -> FileIconStyle(Icons.Rounded.Archive, Color(0xFFF59E0B), ext.uppercase().ifBlank { "ZIP" })
            "document" -> when (ext) {
                "pdf" -> FileIconStyle(Icons.Rounded.PictureAsPdf, Color(0xFFEF4444), "PDF")
                else -> FileIconStyle(Icons.Rounded.Description, Color(0xFF3B82F6), ext.uppercase().ifBlank { "DOC" })
            }
            "code" -> FileIconStyle(Icons.Rounded.Code, Color(0xFF06B6D4), ext.uppercase().ifBlank { "CODE" })
            "app" -> FileIconStyle(Icons.Rounded.Android, Color(0xFF22C55E), ext.uppercase().ifBlank { "APP" })
            else -> when (ext) {
                "apk", "aab" -> FileIconStyle(Icons.Rounded.Android, Color(0xFF22C55E), ext.uppercase())
                "db", "sqlite" -> FileIconStyle(Icons.Rounded.Storage, Color(0xFF64748B), ext.uppercase())
                "log" -> FileIconStyle(Icons.Rounded.Article, Color(0xFF64748B), "LOG")
                "iso", "bin", "exe", "so", "dll" -> FileIconStyle(Icons.Rounded.Memory, Color(0xFF64748B), ext.uppercase())
                else -> FileIconStyle(Icons.Rounded.InsertDriveFile, Color(0xFF64748B), ext.uppercase().ifBlank { "FILE" }.take(4))
            }
        }
    }

    fun folderBadge(item: FileItem): String? {
        if (!item.isDirectory) return null
        if (!item.canWrite) return "RO"
        if (item.isHidden) return "HIDDEN"
        return null
    }
}
