package fr.leonelal.mtgduel

import fr.leonelal.mtgduel.MtgDuelApp
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import fr.leonelal.mtgduel.ui.AppRoot
import fr.leonelal.mtgduel.ui.theme.MtgDuelTheme
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Parcours complet tel qu'il sera vécu après une partie :
 * créer les decks, enregistrer le duel, le retrouver dans l'historique et dans les stats.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ParcoursCompletTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var viewModel: fr.leonelal.mtgduel.ui.AppViewModel

    @Before
    fun repartirDeZero() {
        viewModel = viewModelIsole()
    }

    private fun creerDeck(nom: String, proprietaire: String) {
        compose.onNodeWithTag("onglet-decks").performClick()
        compose.onNodeWithTag("fab-nouveau-deck").performClick()
        compose.onNodeWithTag("nom-du-deck").performTextInput(nom)
        compose.onNodeWithText(proprietaire).performClick()
        compose.onNodeWithText("Créer le deck").performScrollTo().performClick()
    }

    @Test
    fun `enregistrer une partie la fait apparaitre dans l historique et les statistiques`() {
        compose.setContent { MtgDuelTheme { AppRoot(viewModel) } }

        creerDeck("Atraxa", "Moi")
        creerDeck("Krenko", "Mon copain")
        compose.onNodeWithText("Atraxa").assertIsDisplayed()
        compose.onNodeWithText("Krenko").assertIsDisplayed()

        // ---------------------------------------------------- saisie du duel
        compose.onNodeWithTag("onglet-games").performClick()
        compose.onNodeWithTag("fab-nouvelle-partie").performClick()

        // C'est le copain qui a commencé.
        compose.onNodeWithTag("premier-p2").performScrollTo().performClick()

        compose.onNodeWithTag("deck-joueur-1").performScrollTo().performClick()
        compose.onNodeWithText("Atraxa").performClick()
        compose.onNodeWithTag("deck-joueur-2").performScrollTo().performClick()
        compose.onNodeWithText("Krenko").performClick()

        // Victoire de « Moi » : les PV se pré-remplissent (40 pour le gagnant, 0 pour le perdant).
        compose.onNodeWithTag("vainqueur-p1").performScrollTo().performClick()
        compose.onNodeWithTag("pv-joueur-1").performScrollTo().assertIsDisplayed()

        // Le perdant était en fait à 3 PV au moment de la défaite.
        compose.onNodeWithTag("pv-joueur-2").performScrollTo().performTextReplacement("3")

        compose.onNodeWithText("Enregistrer la partie").performScrollTo().performClick()

        // ------------------------------------------------------- historique
        compose.onNodeWithText("Historique des parties").assertIsDisplayed()
        compose.onNodeWithText("1 partie jouée").assertIsDisplayed()
        compose.onNodeWithText("Aujourd'hui").assertIsDisplayed()
        // Chaque deck apparaît deux fois : dans le filtre et dans la carte de la partie.
        compose.onAllNodesWithText("Atraxa").assertCountEquals(2)
        compose.onAllNodesWithText("Krenko").assertCountEquals(2)
        // PV restants affichés : 40 pour le vainqueur, 3 pour le perdant.
        compose.onNodeWithText("40").assertIsDisplayed()
        compose.onNodeWithText("3").assertIsDisplayed()
        compose.onNodeWithText("a commencé").assertIsDisplayed()

        // ----------------------------------------------------- statistiques
        compose.onNodeWithTag("onglet-stats").performClick()
        compose.onNodeWithText("Vue d'ensemble").assertIsDisplayed()
        compose.onNodeWithText("Parties jouées").assertIsDisplayed()
        // Le joueur qui a commencé (le copain) a perdu : 0 victoire sur 1 pour le premier joueur.
        compose.onNodeWithTag("liste-stats").performScrollToNode(hasText("0 / 1"))
        compose.onNodeWithText("0 / 1").assertIsDisplayed()
    }

    @Test
    fun `une partie incomplete n est pas enregistree`() {
        compose.setContent { MtgDuelTheme { AppRoot(viewModel) } }

        creerDeck("Atraxa", "Moi")

        compose.onNodeWithTag("onglet-games").performClick()
        compose.onNodeWithTag("fab-nouvelle-partie").performClick()
        // Aucun vainqueur ni deck choisi : l'enregistrement doit être refusé.
        compose.onNodeWithText("Enregistrer la partie").performScrollTo().performClick()
        compose.onNodeWithText("Sélectionne le vainqueur.").performScrollTo().assertIsDisplayed()
    }
}
