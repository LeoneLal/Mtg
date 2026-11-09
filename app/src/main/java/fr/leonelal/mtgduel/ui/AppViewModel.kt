package fr.leonelal.mtgduel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.leonelal.mtgduel.MtgDuelApp
import fr.leonelal.mtgduel.data.AppState
import fr.leonelal.mtgduel.data.CrashReporter
import fr.leonelal.mtgduel.data.Deck
import fr.leonelal.mtgduel.data.Game
import fr.leonelal.mtgduel.data.GameRepository
import kotlinx.coroutines.flow.StateFlow

class AppViewModel(
    private val repository: GameRepository,
    private val crashReporter: CrashReporter
) : ViewModel() {

    val state: StateFlow<AppState> = repository.state

    fun renamePlayer(playerId: String, name: String) = repository.renamePlayer(playerId, name)

    fun saveDeck(deck: Deck) = repository.upsertDeck(deck)

    fun deleteDeck(deckId: String) = repository.deleteDeck(deckId)

    fun deckIsUsed(deckId: String) = repository.deckIsUsed(deckId)

    fun saveGame(game: Game) = repository.upsertGame(game)

    fun deleteGame(gameId: String) = repository.deleteGame(gameId)

    fun deleteAllGames() = repository.deleteAllGames()

    fun resetAll() = repository.resetAll()

    fun exportToJson(): String = repository.exportToJson()

    fun importFromJson(raw: String): Result<Int> = repository.importFromJson(raw)

    fun mergeFromJson(raw: String): Result<Int> = repository.mergeFromJson(raw)

    fun lastCrash(): String? = crashReporter.lastCrash()

    fun clearCrash() = crashReporter.clear()

    companion object {
        fun factory(app: MtgDuelApp): ViewModelProvider.Factory = viewModelFactory {
            initializer { AppViewModel(app.repository, app.crashReporter) }
        }
    }
}
