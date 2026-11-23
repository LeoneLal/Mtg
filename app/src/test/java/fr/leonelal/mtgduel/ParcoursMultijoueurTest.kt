package fr.leonelal.mtgduel

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import fr.leonelal.mtgduel.ui.AppRoot
import fr.leonelal.mtgduel.ui.AppViewModel
import fr.leonelal.mtgduel.ui.theme.MtgDuelTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Ajouter des joueurs, puis enregistrer une partie de Magic à quatre. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ParcoursMultijoueurTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var viewModel: AppViewModel

    @Before
    fun repartirDeZero() {
        viewModel = viewModelIsole()
    }

    private fun ouvrirApp() {
        compose.setContent { MtgDuelTheme { AppRoot(viewModel) } }
    }

    private fun ajouterJoueur(nom: String) {
        compose.onNodeWithTag("onglet-settings").performClick()
        compose.onNodeWithTag("ajouter-joueur").performScrollTo().performClick()
        compose.onNodeWithTag("nom-nouveau-joueur").performTextInput(nom)
        compose.onNodeWithText("Ajouter").performClick()
    }

    private fun creerDeck(nom: String, proprietaire: String) {
        compose.onNodeWithTag("onglet-decks").performClick()
        compose.onNodeWithTag("fab-nouveau-deck").performClick()
        compose.onNodeWithTag("nom-du-deck").performTextInput(nom)
        compose.onNodeWithText(proprietaire).performScrollTo().performClick()
        compose.onNodeWithText("Créer le deck").performScrollTo().performClick()
    }

    @Test
    fun `ajouter deux joueurs puis enregistrer une partie a quatre`() {
        ouvrirApp()

        ajouterJoueur("Camille")
        ajouterJoueur("Sam")
        compose.onNodeWithTag("nom-joueur-reglages-3").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("nom-joueur-reglages-4").performScrollTo().assertIsDisplayed()

        creerDeck("Atraxa", "Moi")
        creerDeck("Krenko", "Mon copain")

        compose.onNodeWithTag("onglet-games").performClick()
        compose.onNodeWithTag("fab-nouvelle-partie").performClick()

        // Deux joueurs par défaut, puis on passe à quatre.
        compose.onNodeWithTag("plus-joueur-mtg").performScrollTo().performClick()
        compose.onNodeWithTag("plus-joueur-mtg").performClick()

        // Les deux sièges ajoutés ont pris les joueurs encore libres.
        compose.onNodeWithTag("joueur-2").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("joueur-3").performScrollTo().assertIsDisplayed()

        listOf("Atraxa", "Krenko", "Atraxa", "Krenko").forEachIndexed { index, deck ->
            compose.onNodeWithTag("deck-$index").performScrollTo().performClick()
            compose.onNodeWithTag("option-$deck").performClick()
        }

        compose.onNodeWithTag("vainqueur-p1").performScrollTo().performClick()
        compose.onNodeWithTag("pv-2").performScrollTo().performTextReplacement("-3")

        compose.onNodeWithText("Enregistrer la partie").performScrollTo().performClick()

        // L'historique montre bien une partie à quatre.
        compose.onNodeWithText("Historique des parties").assertIsDisplayed()
        compose.onNodeWithText("1 partie jouée").assertIsDisplayed()
        compose.onNodeWithTag("liste-parties").performScrollToNode(hasText("Camille"))
        compose.onNodeWithText("Camille").assertIsDisplayed()
        compose.onNodeWithTag("liste-parties").performScrollToNode(hasText("Sam"))
        compose.onNodeWithText("Sam").assertIsDisplayed()
    }

    @Test
    fun `le tableau de score devient un classement au dela de deux joueurs`() {
        ouvrirApp()

        // À deux, le face-à-face est conservé.
        compose.onNodeWithTag("onglet-games").performClick()
        compose.onNodeWithText("Moi").assertIsDisplayed()
        compose.onNodeWithText("Mon copain").assertIsDisplayed()

        ajouterJoueur("Camille")

        compose.onNodeWithTag("onglet-games").performClick()
        // Les trois joueurs apparaissent maintenant dans le tableau de score.
        compose.onNodeWithText("Camille").assertIsDisplayed()
    }

    @Test
    fun `un joueur retire n est plus propose mais reste dans l historique`() {
        ouvrirApp()

        ajouterJoueur("Camille")
        compose.onNodeWithTag("nom-joueur-reglages-3").performScrollTo().assertIsDisplayed()

        compose.onNodeWithContentDescription("Retirer Camille").performScrollTo().performClick()
        compose.onNodeWithText("Retirer").performClick()

        // Il ne reste que les deux joueurs d'origine.
        compose.onAllNodes(hasTestTag("nom-joueur-reglages-3")).assertCountEquals(0)
    }
}
