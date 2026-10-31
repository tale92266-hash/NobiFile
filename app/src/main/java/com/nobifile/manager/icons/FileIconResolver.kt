package com.nobifile.manager.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.nobifile.manager.data.FileItem
import com.nobifile.manager.data.categoryFor
import java.util.Locale

data class FileIconStyle(val icon: ImageVector, val color: Color, val chip: String)

// File-format icons mirror NobiIDE/NobiCode FileIconProvider semantics:
// exact filename first (AndroidManifest.xml, build.gradle, package.json,
// Dockerfile, README, .gitignore, .env), then extension. XML files always get
// the green Android robot (Sam's SVGRepo choice, olive #AAC148), exactly as in
// NobiCode. HTML/JS/CSS/TS get their own icons and colours, as do all other
// formats below. Unknown formats fall back to a generic file icon, never a
// wrong format's icon.
object FileIconResolver {
    fun styleFor(item: FileItem): FileIconStyle {
        if (item.isDirectory) {
            return FileIconStyle(Icons.Rounded.Folder, folderTint(item.name), "DIR")
        }
        val lowerName = item.name.lowercase(Locale.ROOT)
        exactNameStyle(lowerName)?.let { return it }
        val ext = item.extension
        val cat = categoryFor(item)
        return when (ext) {
            // Code / config — NobiCode mapping
            "xml" -> FileIconStyle(Icons.Rounded.Android, Color(0xFF8AB82E), "XML")
            "kt" -> FileIconStyle(Icons.Rounded.Code, Color(0xFF7C4DFF), "KT")
            "kts" -> FileIconStyle(Icons.Rounded.Code, Color(0xFF7C4DFF), "KTS")
            "java" -> FileIconStyle(Icons.Rounded.Code, Color(0xFFEF5350), "JAVA")
            "py", "pyw", "pyi" -> FileIconStyle(Icons.Rounded.Terminal, Color(0xFF3776AB), "PY")
            "js", "mjs", "cjs" -> FileIconStyle(Icons.Rounded.Javascript, Color(0xFFF7DF1E), "JS")
            "jsx" -> FileIconStyle(Icons.Rounded.Javascript, Color(0xFF61DAFB), "JSX")
            "ts" -> FileIconStyle(Icons.Rounded.DataObject, Color(0xFF3178C6), "TS")
            "tsx" -> FileIconStyle(Icons.Rounded.DataObject, Color(0xFF61DAFB), "TSX")
            "html", "htm", "xhtml" -> FileIconStyle(Icons.Rounded.Html, Color(0xFFE34F26), "HTML")
            "css" -> FileIconStyle(Icons.Rounded.Css, Color(0xFF1572B6), "CSS")
            "scss", "sass" -> FileIconStyle(Icons.Rounded.Css, Color(0xFFCC6699), "SCSS")
            "json" -> FileIconStyle(Icons.Rounded.DataObject, Color(0xFFF59E0B), "JSON")
            "md", "markdown" -> FileIconStyle(Icons.Rounded.Article, Color(0xFF64748B), "MD")
            "sh", "bash", "zsh" -> FileIconStyle(Icons.Rounded.Terminal, Color(0xFF22C55E), "SH")
            "bat", "cmd" -> FileIconStyle(Icons.Rounded.Terminal, Color(0xFF64748B), "BAT")
            "sql", "db", "sqlite" -> FileIconStyle(Icons.Rounded.Storage, Color(0xFF0EA5E9), "SQL")
            "dart" -> FileIconStyle(Icons.Rounded.Code, Color(0xFF0175C2), "DART")
            "rs" -> FileIconStyle(Icons.Rounded.Code, Color(0xFFDEA584), "RS")
            "go" -> FileIconStyle(Icons.Rounded.Code, Color(0xFF00ADD8), "GO")
            "php" -> FileIconStyle(Icons.Rounded.Code, Color(0xFF777BB4), "PHP")
            "c", "h" -> FileIconStyle(Icons.Rounded.Memory, Color(0xFF5C8DBC), "C")
            "cpp", "cc", "cxx", "hpp", "hxx" -> FileIconStyle(Icons.Rounded.Memory, Color(0xFF649AD2), "C++")
            "cs" -> FileIconStyle(Icons.Rounded.Code, Color(0xFF512BD4), "C#")
            "swift" -> FileIconStyle(Icons.Rounded.Code, Color(0xFFF05138), "SWIFT")
            "rb" -> FileIconStyle(Icons.Rounded.Code, Color(0xFFCC342D), "RB")
            "lua" -> FileIconStyle(Icons.Rounded.Code, Color(0xFF000080), "LUA")
            "toml" -> FileIconStyle(Icons.Rounded.Tune, Color(0xFF9C4221), "TOML")
            "properties" -> FileIconStyle(Icons.Rounded.Tune, Color(0xFF64748B), "PROP")
            "yaml", "yml" -> FileIconStyle(Icons.Rounded.Tune, Color(0xFFCB171E), "YAML")
            "gradle" -> FileIconStyle(Icons.Rounded.Build, Color(0xFF02303A), "GRADLE")
            "svg" -> FileIconStyle(Icons.Rounded.Image, Color(0xFFFFB13D), "SVG")
            "csv" -> FileIconStyle(Icons.Rounded.TableChart, Color(0xFF22C55E), "CSV")
            "diff", "patch" -> FileIconStyle(Icons.Rounded.CompareArrows, Color(0xFF64748B), "DIFF")
            "env" -> FileIconStyle(Icons.Rounded.Tune, Color(0xFFF59E0B), "ENV")
            "lock" -> FileIconStyle(Icons.Rounded.Lock, Color(0xFFF59E0B), "LOCK")
            else -> when (cat) {
                "image" -> FileIconStyle(Icons.Rounded.Image, Color(0xFF10B981), ext.uppercase().ifBlank { "IMG" })
                "video" -> FileIconStyle(Icons.Rounded.Movie, Color(0xFF8B5CF6), ext.uppercase().ifBlank { "VID" })
                "audio" -> FileIconStyle(Icons.Rounded.MusicNote, Color(0xFFEC4899), ext.uppercase().ifBlank { "AUD" })
                "archive" -> FileIconStyle(Icons.Rounded.Archive, Color(0xFFF59E0B), ext.uppercase().ifBlank { "ZIP" })
                "document" -> when (ext) {
                    "pdf" -> FileIconStyle(Icons.Rounded.PictureAsPdf, Color(0xFFEF4444), "PDF")
                    "doc", "docx", "odt", "rtf" -> FileIconStyle(Icons.Rounded.Description, Color(0xFF2563EB), ext.uppercase())
                    "xls", "xlsx", "ods" -> FileIconStyle(Icons.Rounded.TableChart, Color(0xFF16A34A), ext.uppercase())
                    "ppt", "pptx" -> FileIconStyle(Icons.Rounded.Slideshow, Color(0xFFEA580C), ext.uppercase())
                    "epub" -> FileIconStyle(Icons.Rounded.MenuBook, Color(0xFF7C3AED), "EPUB")
                    else -> FileIconStyle(Icons.Rounded.Description, Color(0xFF3B82F6), ext.uppercase().ifBlank { "DOC" })
                }
                "app" -> FileIconStyle(Icons.Rounded.Android, Color(0xFF22C55E), ext.uppercase().ifBlank { "APP" })
                "code" -> FileIconStyle(Icons.Rounded.Code, Color(0xFF06B6D4), ext.uppercase().ifBlank { "CODE" })
                else -> when (ext) {
                    "apk", "aab" -> FileIconStyle(Icons.Rounded.Android, Color(0xFF22C55E), ext.uppercase())
                    "exe" -> FileIconStyle(Icons.Rounded.DesktopWindows, Color(0xFF64748B), "EXE")
                    "dll", "so", "bin", "iso" -> FileIconStyle(Icons.Rounded.Memory, Color(0xFF64748B), ext.uppercase())
                    "txt", "log", "text" -> FileIconStyle(Icons.Rounded.Description, Color(0xFF64748B), ext.uppercase().ifBlank { "TXT" })
                    else -> FileIconStyle(Icons.Rounded.InsertDriveFile, Color(0xFF64748B), ext.uppercase().ifBlank { "FILE" }.take(4))
                }
            }
        }
    }

    private fun exactNameStyle(lowerName: String): FileIconStyle? {
        return when (lowerName) {
            "androidmanifest.xml", "sitemap.xml" -> FileIconStyle(Icons.Rounded.Android, Color(0xFF8AB82E), "XML")
            "dockerfile", "docker-compose.yml", "docker-compose.yaml" -> FileIconStyle(Icons.Rounded.Dns, Color(0xFF2496ED), "DOCKER")
            ".gitignore", ".gitattributes", ".gitmodules" -> FileIconStyle(Icons.Rounded.Code, Color(0xFFF05033), "GIT")
            "package.json", "package-lock.json" -> FileIconStyle(Icons.Rounded.DataObject, Color(0xFFCB3837), "NPM")
            "tsconfig.json" -> FileIconStyle(Icons.Rounded.DataObject, Color(0xFF3178C6), "TS")
            "build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts", "gradlew", "gradlew.bat" -> FileIconStyle(Icons.Rounded.Build, Color(0xFF02303A), "GRADLE")
            "gradle.properties" -> FileIconStyle(Icons.Rounded.Tune, Color(0xFF64748B), "PROP")
            "libs.versions.toml" -> FileIconStyle(Icons.Rounded.Tune, Color(0xFF9C4221), "TOML")
            "proguard-rules.pro" -> FileIconStyle(Icons.Rounded.Shield, Color(0xFF64748B), "PRO")
            "tailwind.config.js", "tailwind.config.ts" -> FileIconStyle(Icons.Rounded.Palette, Color(0xFF38BDF8), "TW")
            "vite.config.js", "vite.config.ts" -> FileIconStyle(Icons.Rounded.Build, Color(0xFF646CFF), "VITE")
            "next.config.js", "next.config.mjs" -> FileIconStyle(Icons.Rounded.Language, Color(0xFF111111), "NEXT")
            "readme.md", "readme.txt", "readme" -> FileIconStyle(Icons.Rounded.Article, Color(0xFF64748B), "MD")
            "changelog.md", "changelog" -> FileIconStyle(Icons.Rounded.Article, Color(0xFF64748B), "LOG")
            "license", "license.md", "license.txt" -> FileIconStyle(Icons.Rounded.Verified, Color(0xFF16A34A), "LIC")
            ".eslintrc", ".eslintrc.js", ".eslintrc.json" -> FileIconStyle(Icons.Rounded.Code, Color(0xFF4B32C3), "ESLINT")
            ".prettierrc", ".prettierrc.json" -> FileIconStyle(Icons.Rounded.Code, Color(0xFFF7B93E), "PRET")
            ".editorconfig" -> FileIconStyle(Icons.Rounded.Tune, Color(0xFF64748B), "CFG")
            ".env", ".env.local", ".env.production", ".env.development" -> FileIconStyle(Icons.Rounded.Tune, Color(0xFFF59E0B), "ENV")
            else -> null
        }
    }

    /** Folder tint mirrors NobiIDE folder-name semantics: special folders get their own colour. */
    fun folderTint(folderName: String): Color {
        return when (folderName.lowercase(Locale.ROOT)) {
            "res", "resources", "resource", "assets", "static", "public" -> Color(0xFFF59E0B)
            "src", "source", "sources", "app", "apps" -> Color(0xFF8B5CF6)
            "java", "kotlin", "kt" -> Color(0xFF7C4DFF)
            "js", "javascript" -> Color(0xFFEAB308)
            "database", "db", "models", "entities", "schemas", "sql" -> Color(0xFF0EA5E9)
            "docs", "documentation", "doc", "guides" -> Color(0xFF64748B)
            "node_modules" -> Color(0xFF22C55E)
            "build", "dist", "out", "target", "release", "output" -> Color(0xFF94A3B8)
            "backup", "backups", "archive", "archives" -> Color(0xFFB45309)
            ".git", ".github", ".gitlab", "ci", "workflows" -> Color(0xFFF05033)
            "android" -> Color(0xFF8AB82E)
            "download", "downloads" -> Color(0xFF2563EB)
            "pictures", "dcim", "camera" -> Color(0xFF10B981)
            "music", "audio" -> Color(0xFFEC4899)
            "movies", "videos" -> Color(0xFF8B5CF6)
            else -> Color(0xFF3B82F6)
        }
    }

    fun folderBadge(item: FileItem): String? {
        if (!item.isDirectory) return null
        if (!item.canWrite) return "RO"
        if (item.isHidden) return "HIDDEN"
        return null
    }
}
