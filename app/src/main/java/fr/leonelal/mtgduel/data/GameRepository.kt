package fr.leonelal.mtgduel.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Stockage local des données, dans un unique fichier JSON du dossier privé de l'app.
 * Le volume de données (quelques centaines de parties) rend une base de données inutile,
 * et le format JSON permet une sauvegarde/restauration lisible.
 */
class GameRepository(
    context: Context,
    /** Emplacement du fichier de données ; paramétrable pour les tests. */
    private val file: File = File(context.applicationContext.filesDir, FILE_NAME)
) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val _state = MutableStateFlow(readFromDisk())
    val state: StateFlow<AppState> = _state.asStateFlow()

    // ---------------------------------------------------------------- joueurs

    fun renamePlayer(playerId: String, name: String) = mutate { current ->
        val cleaned = name.trim().ifEmpty { current.playerName(playerId) }
        current.copy(
            players = current.players.map {
                if (it.id == playerId) it.copy(name = cleaned, updatedAt = now()) else it
            }
        )
    }

    // ------------------------------------------------------------------ decks

    fun upsertDeck(deck: Deck) = mutate { current ->
        val horodate = deck.copy(updatedAt = now())
        val exists = current.allDecks.any { it.id == deck.id }
        current.copy(
            allDecks = if (exists) {
                current.allDecks.map { if (it.id == deck.id) horodate else it }
            } else {
                current.allDecks + horodate
            }
        )
    }

    /**
     * Marque le deck comme supprimé au lieu de l'effacer : sans cette trace, l'autre
     * téléphone le réinjecterait à la synchronisation suivante.
     */
    fun deleteDeck(deckId: String) = mutate { current ->
        current.copy(
            allDecks = current.allDecks.map {
                if (it.id == deckId) it.copy(deleted = true, updatedAt = now()) else it
            }
        )
    }

    fun deckIsUsed(deckId: String): Boolean = _state.value.games.any { it.involvesDeck(deckId) }

    // ---------------------------------------------------------------- parties

    fun upsertGame(game: Game) = mutate { current ->
        val horodate = game.copy(updatedAt = now())
        val exists = current.allGames.any { it.id == game.id }
        current.copy(
            allGames = if (exists) {
                current.allGames.map { if (it.id == game.id) horodate else it }
            } else {
                current.allGames + horodate
            }
        )
    }

    fun deleteGame(gameId: String) = mutate { current ->
        current.copy(
            allGames = current.allGames.map {
                if (it.id == gameId) it.copy(deleted = true, updatedAt = now()) else it
            }
        )
    }

    fun deleteAllGames() = mutate { current ->
        val instant = now()
        current.copy(
            allGames = current.allGames.map {
                if (it.deleted) it else it.copy(deleted = true, updatedAt = instant)
            }
        )
    }

    /** Remplace l'état complet, par exemple après une synchronisation. */
    fun replaceWith(state: AppState) = mutate { state }

    /** Remet l'application à neuf : plus aucune partie ni aucun deck, noms par défaut. */
    fun resetAll() = mutate { current ->
        val instant = now()
        AppState(
            players = AppState.defaultPlayers.map { it.copy(updatedAt = instant) },
            allDecks = current.allDecks.map { if (it.deleted) it else it.copy(deleted = true, updatedAt = instant) },
            allGames = current.allGames.map { if (it.deleted) it else it.copy(deleted = true, updatedAt = instant) },
            allCustomGames = current.allCustomGames.map { if (it.deleted) it else it.copy(deleted = true, updatedAt = instant) },
            allCustomMatches = current.allCustomMatches.map { if (it.deleted) it else it.copy(deleted = true, updatedAt = instant) }
        )
    }

    // ----------------------------------------------------- autres jeux

    fun upsertCustomGame(jeu: CustomGame) = mutate { current ->
        val horodate = jeu.copy(updatedAt = now())
        val exists = current.allCustomGames.any { it.id == jeu.id }
        current.copy(
            allCustomGames = if (exists) {
                current.allCustomGames.map { if (it.id == jeu.id) horodate else it }
            } else {
                current.allCustomGames + horodate
            }
        )
    }

    /** Supprimer un jeu retire aussi ses parties : les garder n'aurait aucun sens. */
    fun deleteCustomGame(gameId: String) = mutate { current ->
        val instant = now()
        current.copy(
            allCustomGames = current.allCustomGames.map {
                if (it.id == gameId) it.copy(deleted = true, updatedAt = instant) else it
            },
            allCustomMatches = current.allCustomMatches.map {
                if (it.gameId == gameId && !it.deleted) it.copy(deleted = true, updatedAt = instant) else it
            }
        )
    }

    fun upsertCustomMatch(partie: CustomMatch) = mutate { current ->
        val horodate = partie.copy(updatedAt = now())
        val exists = current.allCustomMatches.any { it.id == partie.id }
        current.copy(
            allCustomMatches = if (exists) {
                current.allCustomMatches.map { if (it.id == partie.id) horodate else it }
            } else {
                current.allCustomMatches + horodate
            }
        )
    }

    fun deleteCustomMatch(matchId: String) = mutate { current ->
        current.copy(
            allCustomMatches = current.allCustomMatches.map {
                if (it.id == matchId) it.copy(deleted = true, updatedAt = now()) else it
            }
        )
    }

    // ----------------------------------------------------- export / import

    fun exportToJson(): String = json.encodeToString(AppState.serializer(), _state.value)

    /** Remplace toutes les données par le contenu d'une sauvegarde. */
    fun importFromJson(raw: String): Result<Int> = runCatching {
        val imported = json.decodeFromString(AppState.serializer(), raw)
        val sanitized = imported.copy(
            players = imported.players.take(2).ifEmpty { AppState.defaultPlayers }
        )
        mutate { sanitized }
        sanitized.games.size
    }

    /** Ajoute les parties et decks absents, sans toucher à l'existant. */
    fun mergeFromJson(raw: String): Result<Int> = runCatching {
        val imported = json.decodeFromString(AppState.serializer(), raw)
        var added = 0
        mutate { current ->
            val knownDecks = current.allDecks.map { it.id }.toSet()
            val knownGames = current.allGames.map { it.id }.toSet()
            val knownCustomGames = current.allCustomGames.map { it.id }.toSet()
            val knownCustomMatches = current.allCustomMatches.map { it.id }.toSet()
            val newDecks = imported.allDecks.filterNot { it.id in knownDecks }
            val newGames = imported.allGames.filterNot { it.id in knownGames }
            val newCustomGames = imported.allCustomGames.filterNot { it.id in knownCustomGames }
            val newCustomMatches = imported.allCustomMatches.filterNot { it.id in knownCustomMatches }
            added = newGames.count { !it.deleted } + newCustomMatches.count { !it.deleted }
            current.copy(
                allDecks = current.allDecks + newDecks,
                allGames = current.allGames + newGames,
                allCustomGames = current.allCustomGames + newCustomGames,
                allCustomMatches = current.allCustomMatches + newCustomMatches
            )
        }
        added
    }

    // --------------------------------------------------------------- interne

    private fun now(): Long = System.currentTimeMillis()

    private fun mutate(transform: (AppState) -> AppState) {
        val updated = transform(_state.value)
        _state.value = updated
        scope.launch {
            writeMutex.withLock { writeToDisk(updated) }
        }
    }

    private fun readFromDisk(): AppState = runCatching {
        if (!file.exists()) return@runCatching AppState()
        val raw = file.readText()
        if (raw.isBlank()) return@runCatching AppState()
        json.decodeFromString(AppState.serializer(), raw)
    }.getOrElse {
        // Fichier illisible : on le met de côté plutôt que de perdre silencieusement les données.
        runCatching { file.copyTo(File(file.parentFile, file.name + ".corrupt"), overwrite = true) }
        AppState()
    }

    private fun writeToDisk(state: AppState) {
        runCatching {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(AppState.serializer(), state))
            if (file.exists()) file.delete()
            tmp.renameTo(file)
        }
    }

    companion object {
        const val FILE_NAME = "mtg-duel-data.json"
    }
}
