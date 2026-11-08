package fr.leonelal.mtgduel.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import fr.leonelal.mtgduel.MtgDuelApp
import fr.leonelal.mtgduel.data.AppState
import fr.leonelal.mtgduel.data.Deck
import fr.leonelal.mtgduel.data.Game
import kotlinx.coroutines.flow.StateFlow

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MtgDuelApp).repository

    val state: StateFlow<AppState> = repository.state

    fun renamePlayer(playerId: String, name: String) = repository.renamePlayer(playerId, name)

    fun saveDeck(deck: Deck) = repository.upsertDeck(deck)

    fun deleteDeck(deckId: String) = repository.deleteDeck(deckId)

    fun deckIsUsed(deckId: String) = repository.deckIsUsed(deckId)

    fun saveGame(game: Game) = repository.upsertGame(game)

    fun deleteGame(gameId: String) = repository.deleteGame(gameId)

    fun deleteAllGames() = repository.deleteAllGames()

    fun exportToJson(): String = repository.exportToJson()

    fun importFromJson(raw: String): Result<Int> = repository.importFromJson(raw)

    fun mergeFromJson(raw: String): Result<Int> = repository.mergeFromJson(raw)
}
