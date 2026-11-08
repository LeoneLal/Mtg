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
class GameRepository(context: Context) {

    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, FILE_NAME)
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
        current.copy(players = current.players.map { if (it.id == playerId) it.copy(name = cleaned) else it })
    }

    // ------------------------------------------------------------------ decks

    fun upsertDeck(deck: Deck) = mutate { current ->
        val exists = current.decks.any { it.id == deck.id }
        val decks = if (exists) {
            current.decks.map { if (it.id == deck.id) deck else it }
        } else {
            current.decks + deck
        }
        current.copy(decks = decks)
    }

    fun deleteDeck(deckId: String) = mutate { current ->
        current.copy(decks = current.decks.filterNot { it.id == deckId })
    }

    fun deckIsUsed(deckId: String): Boolean = _state.value.games.any { it.involvesDeck(deckId) }

    // ---------------------------------------------------------------- parties

    fun upsertGame(game: Game) = mutate { current ->
        val exists = current.games.any { it.id == game.id }
        val games = if (exists) {
            current.games.map { if (it.id == game.id) game else it }
        } else {
            current.games + game
        }
        current.copy(games = games)
    }

    fun deleteGame(gameId: String) = mutate { current ->
        current.copy(games = current.games.filterNot { it.id == gameId })
    }

    fun deleteAllGames() = mutate { current -> current.copy(games = emptyList()) }

    // ----------------------------------------------------- export / import

    fun exportToJson(): String = json.encodeToString(AppState.serializer(), _state.value)

    /** Remplace toutes les données par le contenu d'une sauvegarde. */
    fun importFromJson(raw: String): Result<Int> = runCatching {
        val imported = json.decodeFromString(AppState.serializer(), raw)
        val sanitized = imported.copy(
            players = imported.players.take(2).ifEmpty { AppState.defaultPlayers },
            games = imported.games.filter { it.isValid }
        )
        mutate { sanitized }
        sanitized.games.size
    }

    /** Ajoute les parties et decks absents, sans toucher à l'existant. */
    fun mergeFromJson(raw: String): Result<Int> = runCatching {
        val imported = json.decodeFromString(AppState.serializer(), raw)
        var added = 0
        mutate { current ->
            val knownDecks = current.decks.map { it.id }.toSet()
            val knownGames = current.games.map { it.id }.toSet()
            val newDecks = imported.decks.filterNot { it.id in knownDecks }
            val newGames = imported.games.filter { it.isValid && it.id !in knownGames }
            added = newGames.size
            current.copy(decks = current.decks + newDecks, games = current.games + newGames)
        }
        added
    }

    // --------------------------------------------------------------- interne

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
        runCatching { file.copyTo(File(appContext.filesDir, "$FILE_NAME.corrupt"), overwrite = true) }
        AppState()
    }

    private fun writeToDisk(state: AppState) {
        runCatching {
            val tmp = File(appContext.filesDir, "$FILE_NAME.tmp")
            tmp.writeText(json.encodeToString(AppState.serializer(), state))
            if (file.exists()) file.delete()
            tmp.renameTo(file)
        }
    }

    companion object {
        private const val FILE_NAME = "mtg-duel-data.json"
    }
}
