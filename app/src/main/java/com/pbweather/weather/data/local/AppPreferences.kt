package com.pbweather.weather.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pbweather.weather.domain.Region
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AppPreferences(
    val selected: Region? = null,
    val favorites: List<Region> = emptyList(),
    val primaryId: String? = null,
    val theme: String = "LIGHT",
    val palette: String = "INDIGO",
    val font: String = "NORMAL",
    val weatherMotion: Boolean = true,
    val permissionExplained: Boolean = false,
    val widgetError: Boolean = false,
    val quotaDates: Map<String, String> = emptyMap()
)
data class DeletedFavorite(val region: Region, val index: Int, val wasPrimary: Boolean)
object FavoriteRules {
    fun add(prefs: AppPreferences, region: Region): AppPreferences {
        if (prefs.favorites.any { it.id == region.id } || prefs.favorites.size >= 10) return prefs
        return prefs.copy(favorites = prefs.favorites + region, primaryId = prefs.primaryId ?: region.id)
    }
    fun remove(prefs: AppPreferences, id: String): Pair<AppPreferences, DeletedFavorite?> {
        val index = prefs.favorites.indexOfFirst { it.id == id }
        if (index < 0) return prefs to null
        val region = prefs.favorites[index]
        val remaining = prefs.favorites.filterNot { it.id == id }
        val next = if (prefs.primaryId == id) remaining.firstOrNull()?.id else prefs.primaryId
        return prefs.copy(favorites = remaining, primaryId = next) to DeletedFavorite(region, index, prefs.primaryId == id)
    }
    fun restore(prefs: AppPreferences, deleted: DeletedFavorite): AppPreferences {
        if (prefs.favorites.size >= 10 || prefs.favorites.any { it.id == deleted.region.id }) return prefs
        val list = prefs.favorites.toMutableList().apply { add(deleted.index.coerceAtMost(size), deleted.region) }
        val primary = if (deleted.wasPrimary) deleted.region.id else prefs.primaryId
        return prefs.copy(favorites = list, primaryId = primary)
    }
}
class PreferencesRepository(private val store: DataStore<Preferences>, private val json: Json) {
    private val key = stringPreferencesKey("app_preferences_v1")
    val preferences = store.data.catch { e ->
        if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw e
    }
        .map { values -> values[key]?.let { runCatching { json.decodeFromString<AppPreferences>(it) }.getOrNull() } ?: AppPreferences() }
    suspend fun update(transform: (AppPreferences) -> AppPreferences) {
        store.edit { values ->
            val old = values[key]?.let { runCatching { json.decodeFromString<AppPreferences>(it) }.getOrNull() } ?: AppPreferences()
            values[key] = json.encodeToString(transform(old))
        }
    }
    suspend fun select(region: Region) = update { it.copy(selected = region) }
    suspend fun addFavorite(region: Region) = update { FavoriteRules.add(it, region) }
    suspend fun removeFavorite(id: String): DeletedFavorite? {
        var deletion: DeletedFavorite? = null
        update { old ->
            val (next, removed) = FavoriteRules.remove(old, id)
            deletion = removed
            next
        }
        return deletion
    }
    suspend fun restore(deleted: DeletedFavorite): Boolean {
        var restored = false
        update { old ->
            val next = FavoriteRules.restore(old, deleted)
            restored = next != old
            next
        }
        return restored
    }
    suspend fun setPrimary(id: String) = update { if (it.favorites.any { r -> r.id == id }) it.copy(primaryId = id) else it }
}
