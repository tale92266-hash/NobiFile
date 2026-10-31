package com.nobifile.manager.ui.screens

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nobifile.manager.data.FileItem
import com.nobifile.manager.data.SortOrder
import com.nobifile.manager.data.ViewMode
import com.nobifile.manager.storage.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ExplorerViewModel(
    app: Application,
    val initialPath: String,
    val kind: String,
    val treeUriString: String
) : AndroidViewModel(app) {
    private val _items = MutableStateFlow<List<FileItem>>(emptyList())
    val items: StateFlow<List<FileItem>> = _items.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _currentPath = MutableStateFlow(initialPath)
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()
    private val _sort = MutableStateFlow(SortOrder.NAME_ASC)
    val sort: StateFlow<SortOrder> = _sort.asStateFlow()
    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()
    private val _showHidden = MutableStateFlow(false)
    val showHidden: StateFlow<Boolean> = _showHidden.asStateFlow()
    private val _foldersFirst = MutableStateFlow(true)
    val foldersFirst: StateFlow<Boolean> = _foldersFirst.asStateFlow()
    private val _selection = MutableStateFlow<Set<String>>(emptySet())
    val selection: StateFlow<Set<String>> = _selection.asStateFlow()
    private val _backStack = MutableStateFlow<List<String>>(emptyList())
    val backStack: StateFlow<List<String>> = _backStack.asStateFlow()

    private var loadJob: Job? = null

    fun setSort(s: SortOrder) { _sort.value = s; load() }
    fun setViewMode(v: ViewMode) { _viewMode.value = v }
    fun setShowHidden(b: Boolean) { _showHidden.value = b; load() }
    fun setFoldersFirst(b: Boolean) { _foldersFirst.value = b; load() }

    fun toggleSelect(item: FileItem) {
        val cur = _selection.value.toMutableSet()
        if (cur.contains(item.key)) cur.remove(item.key) else cur.add(item.key)
        _selection.value = cur
    }
    fun selectOnly(item: FileItem) { _selection.value = setOf(item.key) }
    fun clearSelection() { _selection.value = emptySet() }
    fun selectAll() { _selection.value = _items.value.map { it.key }.toSet() }
    fun invertSelection() {
        val all = _items.value.map { it.key }.toSet()
        _selection.value = all - _selection.value
    }
    fun selectByExtension(ext: String) {
        _selection.value = _items.value.filter { it.extension == ext }.map { it.key }.toSet()
    }
    fun selectByType(isDir: Boolean) {
        _selection.value = _items.value.filter { it.isDirectory == isDir }.map { it.key }.toSet()
    }
    fun selectedItems(): List<FileItem> {
        val sel = _selection.value
        return _items.value.filter { sel.contains(it.key) }
    }

    fun load() { loadPath(_currentPath.value, pushHistory = false) }

    fun loadPath(path: String, pushHistory: Boolean = true) {
        loadJob?.cancel()
        _loading.value = true
        _error.value = null
        loadJob = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    when (kind) {
                        "saf" -> {
                            val uri = Uri.parse(treeUriString.ifBlank { path })
                            StorageRepository.listSafTree(getApplication(), uri, _showHidden.value, _sort.value)
                        }
                        else -> {
                            val dir = File(path)
                            if (!dir.exists()) throw IllegalStateException("Folder does not exist: $path")
                            if (!dir.canRead()) throw IllegalStateException("Cannot read this folder. Storage permission may be missing.")
                            StorageRepository.listDirectory(dir, _showHidden.value, _sort.value, _foldersFirst.value)
                        }
                    }
                }
                if (pushHistory && _currentPath.value != path && _currentPath.value.isNotBlank()) {
                    _backStack.value = _backStack.value + _currentPath.value
                }
                _currentPath.value = path
                _items.value = result
                _selection.value = emptySet()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _error.value = e.message ?: "Could not load folder."
                _items.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    fun navigateUp(): Boolean {
        // Internal Storage (/storage/emulated/0) is the top of the file
        // hierarchy for browsing. Going above it (to /storage/emulated, /storage
        // or /) confused users and made Back look like it closed the app, so
        // back at the root returns false and the caller goes Home instead.
        val current = _currentPath.value.trimEnd('/')
        if (kind != "saf" && (current == "/storage/emulated/0" || current.isEmpty())) return false
        val stack = _backStack.value
        if (stack.isEmpty()) {
            val parent = File(_currentPath.value).parentFile
            if (parent != null && parent.exists()) {
                val parentPath = parent.absolutePath.trimEnd('/')
                if (kind != "saf" && !parentPath.startsWith("/storage/emulated/0")) return false
                loadPath(parent.absolutePath, pushHistory = false); return true
            }
            return false
        }
        val prev = stack.last()
        _backStack.value = stack.dropLast(1)
        loadPath(prev, pushHistory = false)
        return true
    }

    fun openItem(item: FileItem): Boolean {
        if (item.isDirectory) {
            if (item.provider == "saf") {
                // SAF subfolders are opened via their own tree URI in a full implementation;
                // for file-backed items this branch is not used. Keep navigation for files only.
                return false
            }
            loadPath(item.displayPath, pushHistory = true)
            return true
        }
        return false
    }
}

class ExplorerViewModelFactory(
    private val app: Application,
    private val path: String,
    private val kind: String,
    private val uri: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ExplorerViewModel(app, path, kind, uri) as T
    }
}
