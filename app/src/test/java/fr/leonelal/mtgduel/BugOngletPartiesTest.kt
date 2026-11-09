package fr.leonelal.mtgduel

import fr.leonelal.mtgduel.MtgDuelApp
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import fr.leonelal.mtgduel.data.AppState
import fr.leonelal.mtgduel.data.Deck
import fr.leonelal.mtgduel.data.Player
import fr.leonelal.mtgduel.ui.AppRoot
import fr.leonelal.mtgduel.ui.theme.MtgDuelTheme
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Reproduction du blocage signalé : des decks existent, aucune partie n'a encore été jouée. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BugOngletPartiesTest {

    @get:Rule
    val compose = createComposeRule()

    private var viewModel: fr.leonelal.mtgduel.ui.AppViewModel = viewModelIsole()

    @Before
    fun repartirDeZero() {
        viewModel = viewModelIsole()
    }

    /** Simule un lancement de l'app sur des données déjà présentes. */
    private fun ecrireEtat(state: AppState) {
        viewModel = viewModelIsole(state)
    }

    @Test
    fun `au lancement avec des decks mais aucune partie l historique s affiche`() {
        ecrireEtat(
            AppState(
                players = listOf(Player("p1", "Moi"), Player("p2", "Mon copain")),
                allDecks = listOf(
                    Deck("d1", "Atraxa", "p1", listOf("W", "U", "B", "G")),
                    Deck("d2", "Krenko", "p2", listOf("R"))
                ),
                allGames = emptyList()
            )
        )
        compose.setContent { MtgDuelTheme { AppRoot(viewModel) } }
        compose.onNodeWithText("Historique des parties").assertIsDisplayed()
        compose.onNodeWithText("Aucune partie enregistrée").assertIsDisplayed()
        compose.onNodeWithText("Première partie à enregistrer").assertIsDisplayed()
    }

    @Test
    fun `apres creation de deux decks on peut revenir sur l onglet parties`() {
        compose.setContent { MtgDuelTheme { AppRoot(viewModel) } }

        listOf("Atraxa" to "Moi", "Krenko" to "Mon copain").forEach { (nom, proprietaire) ->
            compose.onNodeWithTag("onglet-decks").performClick()
            compose.onNodeWithTag("fab-nouveau-deck").performClick()
            compose.onNodeWithTag("nom-du-deck").performTextInput(nom)
            compose.onNodeWithText(proprietaire).performClick()
            compose.onNodeWithText("Créer le deck").performScrollTo().performClick()
        }

        compose.onNodeWithTag("onglet-games").performClick()
        compose.onNodeWithText("Historique des parties").assertIsDisplayed()
        compose.onNodeWithTag("fab-nouvelle-partie").assertIsDisplayed()
    }

    @Test
    fun `les deux joueurs peuvent porter le meme nom sans bloquer l ecran des decks`() {
        ecrireEtat(
            AppState(
                players = listOf(Player("p1", "Leo"), Player("p2", "Leo")),
                allDecks = listOf(Deck("d1", "Atraxa", "p1"), Deck("d2", "Krenko", "p2")),
                allGames = emptyList()
            )
        )
        compose.setContent { MtgDuelTheme { AppRoot(viewModel) } }
        compose.onNodeWithTag("onglet-decks").performClick()
        compose.onNodeWithText("Atraxa").assertIsDisplayed()
        compose.onNodeWithText("Krenko").assertIsDisplayed()
    }
}
