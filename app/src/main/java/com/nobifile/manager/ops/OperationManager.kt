package com.nobifile.manager.ops

import android.content.Context
import android.os.Environment
import com.nobifile.manager.data.AppDatabase
import com.nobifile.manager.data.ConflictChoice
import com.nobifile.manager.data.FileItem
import com.nobifile.manager.data.TrashEntry
import com.nobifile.manager.storage.StorageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

data class ClipboardState(val items: List<FileItem>, val isMove: Boolean)
data class OperationState(
    val id: Long,
    val kind: String,
    val detail: String,
    val totalBytes: Long,
    val doneBytes: Long,
    val totalFiles: Int,
    val doneFiles: Int,
    val status: String, // RUNNING, DONE, FAILED, CANCELLED
    val error: String?
)

class OperationManager(private val context: Context, private val db: AppDatabase) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _operations = MutableStateFlow<List<OperationState>>(emptyList())
    val operations: StateFlow<List<OperationState>> = _operations.asStateFlow()
    private val _clipboard = MutableStateFlow<ClipboardState?>(null)
    val clipboard: StateFlow<ClipboardState?> = _clipboard.asStateFlow()
    private var currentJob: Job? = null
    private var opCounter = 0L

    fun setClipboard(items: List<FileItem>, isMove: Boolean) {
        _clipboard.value = if (items.isEmpty()) null else ClipboardState(items, isMove)
    }

    fun clearClipboard() { _clipboard.value = null }

    fun cancelCurrent() { currentJob?.cancel() }

    private fun updateOp(state: OperationState) {
        _operations.value = _operations.value.filter { it.id != state.id } + state
    }

    fun trashRoot(): File {
        val root = File(Environment.getExternalStorageDirectory(), ".NobiFileTrash")
        return try {
            if (!root.exists()) root.mkdirs()
            if (root.canWrite()) root else File(context.getExternalFilesDir(null), "Trash").apply { mkdirs() }
        } catch (e: Exception) {
            File(context.getExternalFilesDir(null), "Trash").apply { mkdirs() }
        }
    }

    fun copyItems(
        sources: List<FileItem>,
        destDir: File,
        resolve: (File, File) -> ConflictChoice,
        onDone: (Boolean, String) -> Unit
    ) {
        runOp("Copy", sources, destDir, isMove = false, resolve, onDone)
    }

    fun moveItems(
        sources: List<FileItem>,
        destDir: File,
        resolve: (File, File) -> ConflictChoice,
        onDone: (Boolean, String) -> Unit
    ) {
        runOp("Move", sources, destDir, isMove = true, resolve, onDone)
    }

    private fun runOp(
        kind: String,
        sources: List<FileItem>,
        destDir: File,
        isMove: Boolean,
        resolve: (File, File) -> ConflictChoice,
        onDone: (Boolean, String) -> Unit
    ) {
        val id = ++opCounter
        val totalBytes = sources.sumOf { countBytes(it) }
        val totalFiles = sources.sumOf { countFiles(it) }
        val state0 = OperationState(id, kind, "${sources.size} items to ${destDir.absolutePath}", totalBytes, 0, totalFiles, 0, "RUNNING", null)
        updateOp(state0)
        currentJob = scope.launch {
            var doneBytes = 0L
            var doneFiles = 0
            var failed = 0
            var lastError: String? = null
            try {
                if (!destDir.exists()) destDir.mkdirs()
                if (!destDir.canWrite()) throw IllegalStateException("Destination is not writable: ${destDir.absolutePath}")
                for (src in sources) {
                    val srcFile = src.file ?: continue
                    var target = File(destDir, srcFile.name)
                    if (target.exists() && target.absolutePath != srcFile.absolutePath) {
                        when (resolve(srcFile, target)) {
                            ConflictChoice.SKIP -> { failed++; continue }
                            ConflictChoice.REPLACE -> { deleteRecursively(target) }
                            ConflictChoice.RENAME, ConflictChoice.KEEP_BOTH -> { target = uniqueTarget(target) }
                        }
                    }
                    if (isMove) {
                        val moved = moveFileOrDir(srcFile, target)
                        if (moved) { doneFiles++; doneBytes += src.size } else {
                            // Cross-filesystem fallback: copy then verify then delete.
                            copyRecursively(srcFile, target) { b -> doneBytes += b; updateOp(state0.copy(doneBytes = doneBytes, doneFiles = doneFiles)) }
                            if (verifyCopy(srcFile, target)) { deleteRecursively(srcFile); doneFiles++ } else { failed++; lastError = "Move verification failed for ${srcFile.name}" }
                        }
                    } else {
                        copyRecursively(srcFile, target) { b -> doneBytes += b; updateOp(state0.copy(doneBytes = doneBytes, doneFiles = doneFiles)) }
                        doneFiles++
                    }
                    updateOp(state0.copy(doneBytes = doneBytes, doneFiles = doneFiles))
                }
                val ok = failed == 0
                val msg = if (ok) "$kind completed: $doneFiles files" else "$kind finished with $failed skipped/failed"
                updateOp(state0.copy(doneBytes = doneBytes, doneFiles = doneFiles, status = if (ok) "DONE" else "FAILED", error = lastError))
                db.logOperation(kind, state0.detail, msg)
                if (isMove) clearClipboard()
                withContext(Dispatchers.Main) { onDone(ok, msg) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    updateOp(state0.copy(status = "CANCELLED", error = "Cancelled by user"))
                    db.logOperation(kind, state0.detail, "Cancelled")
                    withContext(Dispatchers.Main) { onDone(false, "Cancelled") }
                } else {
                    lastError = e.message ?: "Unknown error"
                    updateOp(state0.copy(status = "FAILED", error = lastError))
                    db.logOperation(kind, state0.detail, "Failed: $lastError")
                    withContext(Dispatchers.Main) { onDone(false, lastError ?: "Failed") }
                }
            }
        }
    }

    fun deleteItems(items: List<FileItem>, useTrash: Boolean, onDone: (Boolean, String) -> Unit) {
        scope.launch {
            var ok = true
            var msg = ""
            try {
                for (item in items) {
                    val f = item.file ?: continue
                    if (useTrash) {
                        val trash = trashRoot()
                        var target = File(trash, f.name)
                        target = uniqueTarget(target)
                        if (f.renameTo(target)) {
                            db.addTrash(TrashEntry(0, f.name, f.absolutePath, target.absolutePath, System.currentTimeMillis(), f.isDirectory))
                        } else {
                            // Rename failed (cross-volume): copy then delete.
                            copyRecursively(f, target) {}
                            if (verifyCopy(f, target)) { deleteRecursively(f); db.addTrash(TrashEntry(0, f.name, f.absolutePath, target.absolutePath, System.currentTimeMillis(), f.isDirectory)) } else ok = false
                        }
                    } else {
                        deleteRecursively(f)
                    }
                }
                msg = if (useTrash) "Moved ${items.size} items to Trash" else "Deleted ${items.size} items"
                db.logOperation("Delete", items.joinToString(", ") { it.name }, msg)
                withContext(Dispatchers.Main) { onDone(ok, msg) }
            } catch (e: Exception) {
                msg = e.message ?: "Delete failed"
                db.logOperation("Delete", items.joinToString(", ") { it.name }, "Failed: $msg")
                withContext(Dispatchers.Main) { onDone(false, msg) }
            }
        }
    }

    fun restoreTrash(entry: TrashEntry, onDone: (Boolean, String) -> Unit) {
        scope.launch {
            try {
                val trashFile = File(entry.trashPath)
                val dest = File(entry.originalPath)
                dest.parentFile?.mkdirs()
                val target = if (dest.exists()) uniqueTarget(dest) else dest
                val ok = trashFile.renameTo(target) || run {
                    copyRecursively(trashFile, target) {}
                    if (verifyCopy(trashFile, target)) { deleteRecursively(trashFile); true } else false
                }
                if (ok) db.removeTrash(entry.id)
                withContext(Dispatchers.Main) { onDone(ok, if (ok) "Restored ${entry.name}" else "Restore failed") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onDone(false, e.message ?: "Restore failed") }
            }
        }
    }

    fun permanentDeleteTrash(entry: TrashEntry, onDone: (Boolean, String) -> Unit) {
        scope.launch {
            try {
                deleteRecursively(File(entry.trashPath))
                db.removeTrash(entry.id)
                withContext(Dispatchers.Main) { onDone(true, "Permanently deleted") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onDone(false, e.message ?: "Failed") }
            }
        }
    }

    fun renameItem(item: FileItem, newName: String, onDone: (Boolean, String) -> Unit) {
        scope.launch {
            try {
                val f = item.file ?: throw IllegalStateException("This item cannot be renamed here.")
                if (newName.isBlank() || newName.contains("/") || newName.contains("\u0000")) throw IllegalStateException("Invalid file name.")
                val target = File(f.parentFile, newName)
                if (target.exists()) throw IllegalStateException("A file named \"$newName\" already exists.")
                val ok = f.renameTo(target)
                if (!ok) throw IllegalStateException("Rename failed. The folder may be read-only.")
                db.logOperation("Rename", "${f.name} -> $newName", "Done")
                withContext(Dispatchers.Main) { onDone(true, "Renamed to $newName") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onDone(false, e.message ?: "Rename failed") }
            }
        }
    }

    fun createFile(dir: File, name: String, content: String, onDone: (Boolean, String) -> Unit) {
        scope.launch {
            try {
                if (name.isBlank() || name.contains("/")) throw IllegalStateException("Invalid file name.")
                val target = File(dir, name)
                if (target.exists()) throw IllegalStateException("A file named \"$name\" already exists.")
                target.writeText(content)
                db.logOperation("Create", target.absolutePath, "Done")
                withContext(Dispatchers.Main) { onDone(true, "Created $name") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onDone(false, e.message ?: "Create failed") }
            }
        }
    }

    fun createFolder(dir: File, name: String, onDone: (Boolean, String) -> Unit) {
        scope.launch {
            try {
                if (name.isBlank() || name.contains("/")) throw IllegalStateException("Invalid folder name.")
                val target = File(dir, name)
                if (target.exists()) throw IllegalStateException("A folder named \"$name\" already exists.")
                if (!target.mkdirs()) throw IllegalStateException("Could not create folder. Check write permission.")
                db.logOperation("Create folder", target.absolutePath, "Done")
                withContext(Dispatchers.Main) { onDone(true, "Created folder $name") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onDone(false, e.message ?: "Create failed") }
            }
        }
    }

    fun sha256(file: File, onDone: (String) -> Unit) {
        scope.launch {
            val hash = try {
                val digest = MessageDigest.getInstance("SHA-256")
                FileInputStream(file).use { input ->
                    val buf = ByteArray(8192)
                    var n: Int
                    while (input.read(buf).also { n = it } != -1) digest.update(buf, 0, n)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            } catch (e: Exception) { "Error: ${e.message}" }
            withContext(Dispatchers.Main) { onDone(hash) }
        }
    }

    private fun countBytes(item: FileItem): Long {
        val f = item.file ?: return 0L
        if (f.isFile) return f.length()
        var total = 0L
        f.listFiles()?.forEach { total += countBytes(StorageRepository.fileToItem(it)) }
        return total
    }

    private fun countFiles(item: FileItem): Int {
        val f = item.file ?: return 0
        if (f.isFile) return 1
        var n = 0
        f.listFiles()?.forEach { n += countFiles(StorageRepository.fileToItem(it)) }
        return n
    }

    fun uniqueTarget(target: File): File {
        if (!target.exists()) return target
        val parent = target.parentFile ?: return target
        val name = target.nameWithoutExtension
        val ext = target.extension
        var i = 1
        while (true) {
            val candidate = if (target.isDirectory || ext.isBlank()) File(parent, "$name ($i)") else File(parent, "$name ($i).$ext")
            if (!candidate.exists()) return candidate
            i++
        }
    }

    private fun deleteRecursively(file: File) {
        if (file.isDirectory) file.listFiles()?.forEach { deleteRecursively(it) }
        file.delete()
    }

    private fun moveFileOrDir(src: File, dest: File): Boolean {
        return try { src.renameTo(dest) } catch (e: Exception) { false }
    }

    fun copyRecursively(src: File, dest: File, onBytes: (Long) -> Unit) {
        if (src.isDirectory) {
            dest.mkdirs()
            src.listFiles()?.forEach { child -> copyRecursively(child, File(dest, child.name), onBytes) }
        } else {
            dest.parentFile?.mkdirs()
            FileInputStream(src).use { input ->
                FileOutputStream(dest).use { output ->
                    val buf = ByteArray(8192)
                    var n: Int
                    while (input.read(buf).also { n = it } != -1) { output.write(buf, 0, n); onBytes(n.toLong()) }
                }
            }
            dest.setLastModified(src.lastModified())
        }
    }

    private fun verifyCopy(src: File, dest: File): Boolean {
        if (src.isFile) return dest.exists() && dest.length() == src.length()
        if (src.isDirectory) {
            if (!dest.isDirectory) return false
            val s = src.listFiles() ?: emptyArray()
            val d = dest.listFiles() ?: emptyArray()
            if (s.size != d.size) return false
            return s.all { child -> verifyCopy(child, File(dest, child.name)) }
        }
        return false
    }
}
