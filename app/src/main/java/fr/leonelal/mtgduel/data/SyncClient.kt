package fr.leonelal.mtgduel.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Dialogue avec l'API de synchronisation : on envoie tout l'état local, le serveur fusionne
 * avec ce qu'il a et renvoie l'état complet. Les volumes en jeu (quelques centaines de parties,
 * soit une poignée de kilo-octets) rendent inutile une synchronisation incrémentale, et cette
 * simplicité élimine toute une classe de bugs.
 */
class SyncClient(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {

    suspend fun push(settings: SyncSettings, local: AppState): AppState = withContext(Dispatchers.IO) {
        require(settings.isConfigured) { "Adresse ou jeton manquant" }

        val connection = (URL(settings.url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer ${settings.token}")
        }

        try {
            connection.outputStream.use { sortie ->
                sortie.write(json.encodeToString(AppState.serializer(), local).toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            if (code !in 200..299) {
                val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException(messageForStatus(code, detail))
            }

            val corps = connection.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString(AppState.serializer(), corps)
        } finally {
            connection.disconnect()
        }
    }

    private fun messageForStatus(code: Int, detail: String): String = when (code) {
        401, 403 -> "Jeton refusé par le serveur (vérifie qu'il est identique des deux côtés)."
        404 -> "Adresse introuvable : vérifie l'URL de synchronisation."
        500, 502, 503 -> "Le serveur a répondu une erreur. ${detail.take(200)}".trim()
        else -> "Réponse inattendue du serveur (code $code). ${detail.take(200)}".trim()
    }
}
