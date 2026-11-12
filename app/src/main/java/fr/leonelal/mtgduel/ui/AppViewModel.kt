package fr.leonelal.mtgduel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.leonelal.mtgduel.MtgDuelApp
import fr.leonelal.mtgduel.data.AppState
import fr.leonelal.mtgduel.data.CrashReporter
import fr.leonelal.mtgduel.data.CustomGame
import fr.leonelal.mtgduel.data.CustomMatch
import fr.leonelal.mtgduel.data.Deck
import fr.leonelal.mtgduel.data.Game
import fr.leonelal.mtgduel.data.GameRepository
import fr.leonelal.mtgduel.data.SyncService
import fr.leonelal.mtgduel.data.SyncSettings
import fr.leonelal.mtgduel.data.SyncSettingsStore
import fr.leonelal.mtgduel.data.SyncState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppViewModel(
    private val repository: GameRepository,
    private val crashReporter: CrashReporter,
    private val syncSettingsStore: SyncSettingsStore? = null,
    private val syncService: SyncService? = null
) : ViewModel() {

    val state: StateFlow<AppState> = repository.state

    fun renamePlayer(playerId: String, name: String) = repository.renamePlayer(playerId, name)

    fun saveDeck(deck: Deck) = repository.upsertDeck(deck)

    fun deleteDeck(deckId: String) = repository.deleteDeck(deckId)

    fun deckIsUsed(deckId: String) = repository.deckIsUsed(deckId)

    fun saveGame(game: Game) = repository.upsertGame(game)

    fun deleteGame(gameId: String) = repository.deleteGame(gameId)

    fun deleteAllGames() = repository.deleteAllGames()

    // ---------------------------------------------------------- autres jeux

    fun saveCustomGame(jeu: CustomGame) = repository.upsertCustomGame(jeu)

    fun deleteCustomGame(gameId: String) = repository.deleteCustomGame(gameId)

    fun saveCustomMatch(partie: CustomMatch) = repository.upsertCustomMatch(partie)

    fun deleteCustomMatch(matchId: String) = repository.deleteCustomMatch(matchId)

    fun resetAll() = repository.resetAll()

    fun exportToJson(): String = repository.exportToJson()

    fun importFromJson(raw: String): Result<Int> = repository.importFromJson(raw)

    fun mergeFromJson(raw: String): Result<Int> = repository.mergeFromJson(raw)

    // ------------------------------------------------------- synchronisation

    val syncSettings: StateFlow<SyncSettings>? = syncSettingsStore?.settings
    val syncState: StateFlow<SyncState>? = syncService?.state

    fun updateSyncSettings(transform: (SyncSettings) -> SyncSettings) {
        syncSettingsStore?.update(transform)
    }

    fun syncNow(onResult: (Result<String>) -> Unit = {}) {
        val service = syncService ?: return
        viewModelScope.launch { onResult(service.sync()) }
    }

    // -------------------------------------------------------------- erreurs

    fun lastCrash(): String? = crashReporter.lastCrash()

    fun clearCrash() = crashReporter.clear()

    companion object {
        fun factory(app: MtgDuelApp): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AppViewModel(
                    repository = app.repository,
                    crashReporter = app.crashReporter,
                    syncSettingsStore = app.syncSettingsStore,
                    syncService = app.syncService
                )
            }
        }
    }
}
