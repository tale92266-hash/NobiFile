package com.nobifile.manager.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nobifile_prefs")

class PrefsStore(private val context: Context) {
    companion object {
        val THEME = stringPreferencesKey("theme") // system, light, dark, amoled
        val ACCENT = stringPreferencesKey("accent") // blue, violet, teal, amber
        val SORT = stringPreferencesKey("sort")
        val VIEW = stringPreferencesKey("view")
        val SHOW_HIDDEN = booleanPreferencesKey("show_hidden")
        val FOLDERS_FIRST = booleanPreferencesKey("folders_first")
        val SHOW_EXTENSIONS = booleanPreferencesKey("show_extensions")
        val CONFIRM_DELETE = booleanPreferencesKey("confirm_delete")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val THUMBNAILS = booleanPreferencesKey("thumbnails")
    }

    val theme: Flow<String> = context.dataStore.data.map { it[THEME] ?: "system" }
    val accent: Flow<String> = context.dataStore.data.map { it[ACCENT] ?: "blue" }
    val sort: Flow<String> = context.dataStore.data.map { it[SORT] ?: SortOrder.NAME_ASC.name }
    val view: Flow<String> = context.dataStore.data.map { it[VIEW] ?: ViewMode.LIST.name }
    val showHidden: Flow<Boolean> = context.dataStore.data.map { it[SHOW_HIDDEN] ?: false }
    val foldersFirst: Flow<Boolean> = context.dataStore.data.map { it[FOLDERS_FIRST] ?: true }
    val showExtensions: Flow<Boolean> = context.dataStore.data.map { it[SHOW_EXTENSIONS] ?: true }
    val confirmDelete: Flow<Boolean> = context.dataStore.data.map { it[CONFIRM_DELETE] ?: true }
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_DONE] ?: false }
    val gridColumns: Flow<Int> = context.dataStore.data.map { it[GRID_COLUMNS] ?: 3 }
    val thumbnails: Flow<Boolean> = context.dataStore.data.map { it[THUMBNAILS] ?: true }

    suspend fun set(key: Preferences.Key<String>, value: String) { context.dataStore.edit { it[key] = value } }
    suspend fun setBool(key: Preferences.Key<Boolean>, value: Boolean) { context.dataStore.edit { it[key] = value } }
    suspend fun setInt(key: Preferences.Key<Int>, value: Int) { context.dataStore.edit { it[key] = value } }
}
