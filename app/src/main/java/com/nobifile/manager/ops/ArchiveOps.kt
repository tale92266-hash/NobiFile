package com.nobifile.manager.ops

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

data class ArchiveEntry(val name: String, val isDirectory: Boolean, val size: Long)

object ArchiveOps {
    fun isArchive(name: String): Boolean {
        val n = name.lowercase()
        return n.endsWith(".zip") || n.endsWith(".tar") || n.endsWith(".tar.gz") || n.endsWith(".tgz") || n.endsWith(".gz")
    }

    fun listZip(file: File): List<ArchiveEntry> {
        val out = mutableListOf<ArchiveEntry>()
        ZipFile(file).use { z ->
            val en = z.entries()
            while (en.hasMoreElements()) {
                val e = en.nextElement()
                out.add(ArchiveEntry(e.name, e.isDirectory, e.size))
            }
        }
        return out
    }

    fun listTar(file: File): List<ArchiveEntry> {
        val input: InputStream = if (file.name.lowercase().endsWith(".gz") || file.name.lowercase().endsWith(".tgz")) {
            GZIPInputStream(FileInputStream(file))
        } else FileInputStream(file)
        val out = mutableListOf<ArchiveEntry>()
        BufferedInputStream(input).use { bis ->
            while (true) {
                val header = ByteArray(512)
                var read = 0
                while (read < 512) {
                    val n = bis.read(header, read, 512 - read)
                    if (n == -1) break
                    read += n
                }
                if (read < 512) break
                if (header.all { it == 0.toByte() }) break
                val name = String(header, 0, 100, Charsets.UTF_8).trimEnd('\u0000')
                if (name.isBlank()) break
                val sizeStr = String(header, 124, 12, Charsets.UTF_8).trim().trimEnd('\u0000').trim()
                val size = sizeStr.toLongOrNull(8) ?: 0L
                val type = header[156].toInt().toChar()
                out.add(ArchiveEntry(name, type == '5', size))
                var remaining = ((size + 511) / 512) * 512
                val skipBuf = ByteArray(8192)
                while (remaining > 0) {
                    val toRead = minOf(remaining, skipBuf.size.toLong()).toInt()
                    val n = bis.read(skipBuf, 0, toRead)
                    if (n == -1) break
                    remaining -= n
                }
            }
        }
        return out
    }

    fun listArchive(file: File): List<ArchiveEntry> {
        val n = file.name.lowercase()
        return when {
            n.endsWith(".zip") -> listZip(file)
            n.endsWith(".tar") || n.endsWith(".tar.gz") || n.endsWith(".tgz") -> listTar(file)
            n.endsWith(".gz") -> listOf(ArchiveEntry(file.nameWithoutExtension, false, file.length()))
            else -> emptyList()
        }
    }

    fun extractZip(file: File, destDir: File, onProgress: (String) -> Unit) {
        destDir.mkdirs()
        ZipFile(file).use { z ->
            val en = z.entries()
            while (en.hasMoreElements()) {
                val e = en.nextElement()
                val target = File(destDir, e.name)
                val canonDest = destDir.canonicalFile
                val canonTarget = target.canonicalFile
                if (!canonTarget.path.startsWith(canonDest.path)) throw IllegalStateException("Unsafe archive entry: ${e.name}")
                if (e.isDirectory) { target.mkdirs(); continue }
                target.parentFile?.mkdirs()
                z.getInputStream(e).use { input ->
                    FileOutputStream(target).use { output -> input.copyTo(output) }
                }
                onProgress(e.name)
            }
        }
    }

    fun extractTar(file: File, destDir: File, onProgress: (String) -> Unit) {
        destDir.mkdirs()
        val input: InputStream = if (file.name.lowercase().endsWith(".gz") || file.name.lowercase().endsWith(".tgz")) {
            GZIPInputStream(FileInputStream(file))
        } else FileInputStream(file)
        BufferedInputStream(input).use { bis ->
            while (true) {
                val header = ByteArray(512)
                var read = 0
                while (read < 512) {
                    val n = bis.read(header, read, 512 - read)
                    if (n == -1) break
                    read += n
                }
                if (read < 512) break
                if (header.all { it == 0.toByte() }) break
                val name = String(header, 0, 100, Charsets.UTF_8).trimEnd('\u0000')
                if (name.isBlank()) break
                val sizeStr = String(header, 124, 12, Charsets.UTF_8).trim().trimEnd('\u0000').trim()
                val size = sizeStr.toLongOrNull(8) ?: 0L
                val type = header[156].toInt().toChar()
                val target = File(destDir, name)
                val canonDest = destDir.canonicalFile
                if (!target.canonicalFile.path.startsWith(canonDest.path)) throw IllegalStateException("Unsafe archive entry: $name")
                if (type == '5' || name.endsWith("/")) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { out ->
                        var remaining = size
                        val buf = ByteArray(8192)
                        while (remaining > 0) {
                            val toRead = minOf(remaining, buf.size.toLong()).toInt()
                            val n = bis.read(buf, 0, toRead)
                            if (n == -1) break
                            out.write(buf, 0, n)
                            remaining -= n
                        }
                    }
                    onProgress(name)
                }
                var pad = ((size + 511) / 512) * 512 - size
                // For directory entries size is 0, pad is 0. For files we already consumed exactly size bytes;
                // the remaining padding bytes are still in the stream and must be skipped.
                // Note: when we read file content above we read exactly size bytes, so skip pad bytes now,
                // but only if this entry was a file (directories have no data block).
                if (type != '5' && !name.endsWith("/")) {
                    val skipBuf = ByteArray(8192)
                    while (pad > 0) {
                        val toRead = minOf(pad, skipBuf.size.toLong()).toInt()
                        val n = bis.read(skipBuf, 0, toRead)
                        if (n == -1) break
                        pad -= n
                    }
                }
            }
        }
    }

    fun extractArchive(file: File, destDir: File, onProgress: (String) -> Unit) {
        val n = file.name.lowercase()
        when {
            n.endsWith(".zip") -> extractZip(file, destDir, onProgress)
            n.endsWith(".tar") || n.endsWith(".tar.gz") || n.endsWith(".tgz") -> extractTar(file, destDir, onProgress)
            n.endsWith(".gz") -> {
                destDir.mkdirs()
                val outFile = File(destDir, file.name.removeSuffix(".gz"))
                GZIPInputStream(FileInputStream(file)).use { input ->
                    FileOutputStream(outFile).use { output -> input.copyTo(output) }
                }
                onProgress(outFile.name)
            }
            else -> throw IllegalStateException("Unsupported archive format.")
        }
    }

    fun createZip(sources: List<File>, dest: File, baseDir: File?) {
        dest.parentFile?.mkdirs()
        ZipOutputStream(BufferedOutputStream(FileOutputStream(dest))).use { zos ->
            for (src in sources) addToZip(src, baseDir, zos)
        }
    }

    private fun addToZip(file: File, baseDir: File?, zos: ZipOutputStream) {
        val rel = try {
            if (baseDir != null) file.relativeTo(baseDir).path else file.name
        } catch (e: Exception) { file.name }
        if (file.isDirectory) {
            zos.putNextEntry(ZipEntry("$rel/"))
            zos.closeEntry()
            file.listFiles()?.forEach { addToZip(it, baseDir, zos) }
        } else {
            zos.putNextEntry(ZipEntry(rel))
            FileInputStream(file).use { it.copyTo(zos) }
            zos.closeEntry()
        }
    }

    fun createTarGz(sources: List<File>, dest: File, baseDir: File?) {
        dest.parentFile?.mkdirs()
        GZIPOutputStream(BufferedOutputStream(FileOutputStream(dest))).use { gz ->
            for (src in sources) addToTar(src, baseDir, gz)
            // Two zero blocks mark end of tar.
            gz.write(ByteArray(1024))
        }
    }

    private fun writeTarHeader(out: java.io.OutputStream, name: String, size: Long, isDir: Boolean) {
        val header = ByteArray(512)
        fun putStr(offset: Int, len: Int, value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            val n = minOf(bytes.size, len)
            System.arraycopy(bytes, 0, header, offset, n)
        }
        putStr(0, 100, name)
        putStr(100, 8, "0000644\u0000")
        putStr(108, 8, "0000000\u0000")
        putStr(116, 8, "0000000\u0000")
        putStr(124, 12, "%011o\u0000".format(size))
        putStr(136, 12, "%011o\u0000".format(System.currentTimeMillis() / 1000))
        for (i in 148 until 156) header[i] = ' '.code.toByte()
        header[156] = if (isDir) '5'.code.toByte() else '0'.code.toByte()
        putStr(257, 6, "ustar\u0000")
        putStr(263, 2, "00")
        var sum = 0
        for (b in header) sum += b.toInt() and 0xFF
        putStr(148, 8, "%06o\u0000 ".format(sum))
        out.write(header)
    }

    private fun addToTar(file: File, baseDir: File?, out: java.io.OutputStream) {
        val rel = try {
            if (baseDir != null) file.relativeTo(baseDir).path else file.name
        } catch (e: Exception) { file.name }
        if (file.isDirectory) {
            writeTarHeader(out, "$rel/", 0, true)
            file.listFiles()?.forEach { addToTar(it, baseDir, out) }
        } else {
            writeTarHeader(out, rel, file.length(), false)
            FileInputStream(file).use { input ->
                val buf = ByteArray(8192)
                var n: Int
                while (input.read(buf).also { n = it } != -1) out.write(buf, 0, n)
            }
            val pad = ((file.length() + 511) / 512) * 512 - file.length()
            if (pad > 0) out.write(ByteArray(pad.toInt()))
        }
    }
}
