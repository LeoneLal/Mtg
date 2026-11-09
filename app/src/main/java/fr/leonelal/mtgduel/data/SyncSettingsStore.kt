package fr.leonelal.mtgduel.data

import android.content.Context
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/** Les réglages de synchronisation vivent dans leur propre fichier, jamais exporté. */
class SyncSettingsStore(
    context: Context,
    private val file: File = File(context.applicationContext.filesDir, FILE_NAME)
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<SyncSettings> = _settings.asStateFlow()

    fun update(transform: (SyncSettings) -> SyncSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        runCatching { file.writeText(json.encodeToString(SyncSettings.serializer(), updated)) }
    }

    private fun read(): SyncSettings = runCatching {
        if (!file.exists()) return@runCatching SyncSettings()
        json.decodeFromString(SyncSettings.serializer(), file.readText())
    }.getOrElse { SyncSettings() }

    companion object {
        const val FILE_NAME = "mtg-duel-sync.json"
    }
}
