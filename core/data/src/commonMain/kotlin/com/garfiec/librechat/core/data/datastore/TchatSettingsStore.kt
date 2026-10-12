package com.garfiec.librechat.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.garfiec.librechat.core.model.config.ImageChoice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Tchat-only preferences, in the same DataStore as [SettingsDataStore] but kept out of that
 * upstream file so syncs don't conflict on it.
 */
class TchatSettingsStore(
    private val dataStore: DataStore<Preferences>,
) {
    /**
     * The Image Gen selection, app-wide like [SettingsDataStore.enabledTools]. Null when Image Gen
     * is off, [IMAGE_GEN_DEFAULT] for the server's default `imageList` entry, else an entry's name.
     */
    val imageGenSelection: Flow<String?> = dataStore.data.map { prefs -> prefs[KEY_IMAGE_GEN] }

    suspend fun setImageGenSelection(selection: String?) {
        dataStore.edit { prefs ->
            if (selection == null) prefs.remove(KEY_IMAGE_GEN) else prefs[KEY_IMAGE_GEN] = selection
        }
    }

    /**
     * The image parameters pinned per `imageList` entry name, app-wide like the selection. Keyed
     * by entry so one model's sizes never ride onto another that doesn't offer them.
     */
    val imageChoices: Flow<Map<String, ImageChoice>> = dataStore.data.map { prefs ->
        prefs[KEY_IMAGE_CHOICES]
            ?.let { runCatching { json.decodeFromString(choicesSerializer, it) }.getOrNull() }
            .orEmpty()
    }

    suspend fun setImageChoice(name: String, choice: ImageChoice?) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_IMAGE_CHOICES]
                ?.let { runCatching { json.decodeFromString(choicesSerializer, it) }.getOrNull() }
                .orEmpty()
            val next = if (choice == null || choice.isEmpty) current - name else current + (name to choice)
            if (next.isEmpty()) prefs.remove(KEY_IMAGE_CHOICES) else prefs[KEY_IMAGE_CHOICES] = json.encodeToString(choicesSerializer, next)
        }
    }

    companion object {
        private val KEY_IMAGE_GEN = stringPreferencesKey("tchat_image_gen")
        private val KEY_IMAGE_CHOICES = stringPreferencesKey("tchat_image_choices")
        private val json = Json { ignoreUnknownKeys = true }
        private val choicesSerializer = MapSerializer(String.serializer(), ImageChoice.serializer())

        /** [imageGenSelection]'s value for the server's default image entry. */
        const val IMAGE_GEN_DEFAULT: String = "__default__"
    }
}
