package com.nobifile.manager.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppDatabase(context: Context) : SQLiteOpenHelper(context, "nobifile.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE bookmarks(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, path TEXT, kind TEXT)")
        db.execSQL("CREATE TABLE recents(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, path TEXT, lastOpened INTEGER)")
        db.execSQL("CREATE TABLE collections(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)")
        db.execSQL("CREATE TABLE collection_entries(id INTEGER PRIMARY KEY AUTOINCREMENT, collectionId INTEGER, name TEXT, path TEXT)")
        db.execSQL("CREATE TABLE project_roots(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, path TEXT)")
        db.execSQL("CREATE TABLE sidebar_locations(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, path TEXT, kind TEXT, position INTEGER, visible INTEGER)")
        db.execSQL("CREATE TABLE explorer_tabs(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, path TEXT, position INTEGER)")
        db.execSQL("CREATE TABLE operation_history(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT, detail TEXT, result TEXT, time INTEGER)")
        db.execSQL("CREATE TABLE trash_items(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, originalPath TEXT, trashPath TEXT, deletedAt INTEGER, isDirectory INTEGER)")
        db.execSQL("CREATE TABLE search_history(id INTEGER PRIMARY KEY AUTOINCREMENT, query TEXT, time INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v1 only; future versions add migrations here, never drop user data silently.
    }

    fun bookmarks(): List<BookmarkItem> {
        val out = mutableListOf<BookmarkItem>()
        readableDatabase.rawQuery("SELECT id,name,path,kind FROM bookmarks ORDER BY id ASC", null).use { c ->
            while (c.moveToNext()) out.add(BookmarkItem(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getString(3) ?: "file"))
        }
        return out
    }

    fun addBookmark(name: String, path: String, kind: String) {
        val v = ContentValues().apply { put("name", name); put("path", path); put("kind", kind) }
        writableDatabase.insert("bookmarks", null, v)
    }

    fun removeBookmark(id: Long) { writableDatabase.delete("bookmarks", "id=?", arrayOf(id.toString())) }
    fun isBookmarked(path: String): Boolean {
        readableDatabase.rawQuery("SELECT id FROM bookmarks WHERE path=? LIMIT 1", arrayOf(path)).use { return it.moveToFirst() }
    }

    fun recents(): List<RecentItem> {
        val out = mutableListOf<RecentItem>()
        readableDatabase.rawQuery("SELECT id,name,path,lastOpened FROM recents ORDER BY lastOpened DESC LIMIT 50", null).use { c ->
            while (c.moveToNext()) out.add(RecentItem(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getLong(3)))
        }
        return out
    }

    fun recordRecent(name: String, path: String) {
        val db = writableDatabase
        db.delete("recents", "path=?", arrayOf(path))
        val v = ContentValues().apply { put("name", name); put("path", path); put("lastOpened", System.currentTimeMillis()) }
        db.insert("recents", null, v)
        db.execSQL("DELETE FROM recents WHERE id NOT IN (SELECT id FROM recents ORDER BY lastOpened DESC LIMIT 50)")
    }

    fun clearRecents() { writableDatabase.delete("recents", null, null) }

    fun collections(): List<CollectionItem> {
        val out = mutableListOf<CollectionItem>()
        readableDatabase.rawQuery("SELECT id,name FROM collections ORDER BY id ASC", null).use { c ->
            while (c.moveToNext()) out.add(CollectionItem(c.getLong(0), c.getString(1) ?: ""))
        }
        return out
    }

    fun createCollection(name: String): Long {
        val v = ContentValues().apply { put("name", name) }
        return writableDatabase.insert("collections", null, v)
    }

    fun deleteCollection(id: Long) {
        writableDatabase.delete("collection_entries", "collectionId=?", arrayOf(id.toString()))
        writableDatabase.delete("collections", "id=?", arrayOf(id.toString()))
    }

    fun collectionEntries(collectionId: Long): List<CollectionEntry> {
        val out = mutableListOf<CollectionEntry>()
        readableDatabase.rawQuery("SELECT id,collectionId,name,path FROM collection_entries WHERE collectionId=? ORDER BY id ASC", arrayOf(collectionId.toString())).use { c ->
            while (c.moveToNext()) out.add(CollectionEntry(c.getLong(0), c.getLong(1), c.getString(2) ?: "", c.getString(3) ?: ""))
        }
        return out
    }

    fun addToCollection(collectionId: Long, name: String, path: String) {
        val v = ContentValues().apply { put("collectionId", collectionId); put("name", name); put("path", path) }
        writableDatabase.insert("collection_entries", null, v)
    }

    fun projectRoots(): List<ProjectRoot> {
        val out = mutableListOf<ProjectRoot>()
        readableDatabase.rawQuery("SELECT id,name,path FROM project_roots ORDER BY id ASC", null).use { c ->
            while (c.moveToNext()) out.add(ProjectRoot(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: ""))
        }
        return out
    }

    fun addProjectRoot(name: String, path: String) {
        val v = ContentValues().apply { put("name", name); put("path", path) }
        writableDatabase.insert("project_roots", null, v)
    }

    fun removeProjectRoot(id: Long) { writableDatabase.delete("project_roots", "id=?", arrayOf(id.toString())) }

    fun sidebarLocations(): List<SidebarLocation> {
        val out = mutableListOf<SidebarLocation>()
        readableDatabase.rawQuery("SELECT id,name,path,kind,position,visible FROM sidebar_locations ORDER BY position ASC, id ASC", null).use { c ->
            while (c.moveToNext()) out.add(SidebarLocation(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getString(3) ?: "custom", c.getInt(4), c.getInt(5) == 1))
        }
        return out
    }

    fun addSidebarLocation(name: String, path: String, kind: String) {
        val pos = sidebarLocations().size
        val v = ContentValues().apply { put("name", name); put("path", path); put("kind", kind); put("position", pos); put("visible", 1) }
        writableDatabase.insert("sidebar_locations", null, v)
    }

    fun updateSidebarLocation(item: SidebarLocation) {
        val v = ContentValues().apply { put("name", item.name); put("path", item.path); put("kind", item.kind); put("position", item.position); put("visible", if (item.visible) 1 else 0) }
        writableDatabase.update("sidebar_locations", v, "id=?", arrayOf(item.id.toString()))
    }

    fun removeSidebarLocation(id: Long) { writableDatabase.delete("sidebar_locations", "id=?", arrayOf(id.toString())) }

    fun tabs(): List<ExplorerTab> {
        val out = mutableListOf<ExplorerTab>()
        readableDatabase.rawQuery("SELECT id,name,path,position FROM explorer_tabs ORDER BY position ASC, id ASC", null).use { c ->
            while (c.moveToNext()) out.add(ExplorerTab(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getInt(3)))
        }
        return out
    }

    fun saveTabs(tabs: List<ExplorerTab>) {
        val db = writableDatabase
        db.delete("explorer_tabs", null, null)
        tabs.forEachIndexed { index, t ->
            val v = ContentValues().apply { put("name", t.name); put("path", t.path); put("position", index) }
            db.insert("explorer_tabs", null, v)
        }
    }

    fun logOperation(kind: String, detail: String, result: String) {
        val v = ContentValues().apply { put("kind", kind); put("detail", detail); put("result", result); put("time", System.currentTimeMillis()) }
        writableDatabase.insert("operation_history", null, v)
    }

    fun operationHistory(): List<OperationRecord> {
        val out = mutableListOf<OperationRecord>()
        readableDatabase.rawQuery("SELECT id,kind,detail,result,time FROM operation_history ORDER BY time DESC LIMIT 100", null).use { c ->
            while (c.moveToNext()) out.add(OperationRecord(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getString(3) ?: "", c.getLong(4)))
        }
        return out
    }

    fun trashEntries(): List<TrashEntry> {
        val out = mutableListOf<TrashEntry>()
        readableDatabase.rawQuery("SELECT id,name,originalPath,trashPath,deletedAt,isDirectory FROM trash_items ORDER BY deletedAt DESC", null).use { c ->
            while (c.moveToNext()) out.add(TrashEntry(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getString(3) ?: "", c.getLong(4), c.getInt(5) == 1))
        }
        return out
    }

    fun addTrash(entry: TrashEntry): Long {
        val v = ContentValues().apply {
            put("name", entry.name); put("originalPath", entry.originalPath); put("trashPath", entry.trashPath)
            put("deletedAt", entry.deletedAt); put("isDirectory", if (entry.isDirectory) 1 else 0)
        }
        return writableDatabase.insert("trash_items", null, v)
    }

    fun removeTrash(id: Long) { writableDatabase.delete("trash_items", "id=?", arrayOf(id.toString())) }
    fun clearTrash() { writableDatabase.delete("trash_items", null, null) }

    fun searchHistory(): List<SearchHistoryItem> {
        val out = mutableListOf<SearchHistoryItem>()
        readableDatabase.rawQuery("SELECT id,query,time FROM search_history ORDER BY time DESC LIMIT 30", null).use { c ->
            while (c.moveToNext()) out.add(SearchHistoryItem(c.getLong(0), c.getString(1) ?: "", c.getLong(2)))
        }
        return out
    }

    fun addSearch(query: String) {
        val db = writableDatabase
        db.delete("search_history", "query=?", arrayOf(query))
        val v = ContentValues().apply { put("query", query); put("time", System.currentTimeMillis()) }
        db.insert("search_history", null, v)
    }

    fun clearSearchHistory() { writableDatabase.delete("search_history", null, null) }
}
