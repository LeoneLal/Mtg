package fr.leonelal.mtgduel

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import fr.leonelal.mtgduel.ui.AppViewModel
import fr.leonelal.mtgduel.ui.AppRoot
import fr.leonelal.mtgduel.ui.theme.MtgDuelTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Le parcours demandé : créer un jeu en choisissant son mode de comptage, puis
 * enregistrer une partie en fixant soi-même le nombre de joueurs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ParcoursJeuxTest {

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

    private fun creerJeu(nom: String, comptage: String) {
        compose.onNodeWithTag("onglet-jeux").performClick()
        compose.onNodeWithTag("fab-nouveau-jeu").performClick()
        compose.onNodeWithTag("nom-du-jeu").performTextInput(nom)
        compose.onNodeWithTag("comptage-$comptage").performScrollTo().performClick()
        compose.onNodeWithText("Créer le jeu").performScrollTo().performClick()
    }

    @Test
    fun `creer un jeu puis une partie a quatre joueurs`() {
        ouvrirApp()

        creerJeu("Skull King", "POINTS_MAX")
        compose.onNodeWithText("Skull King").assertIsDisplayed()
        compose.onNodeWithText("Points, le plus haut gagne").assertIsDisplayed()

        // On ouvre le jeu, puis on saisit une partie à quatre.
        compose.onNodeWithText("Skull King").performClick()
        compose.onNodeWithTag("fab-nouvelle-partie-jeu").performClick()

        compose.onNodeWithTag("nombre-joueurs").assertIsDisplayed()
        compose.onNodeWithTag("plus-joueur").performScrollTo().performClick()
        compose.onNodeWithTag("plus-joueur").performClick()

        val scores = listOf("Léo" to "72", "Alex" to "95", "Camille" to "41", "Sam" to "88")
        scores.forEachIndexed { index, (nom, score) ->
            compose.onNodeWithTag("nom-joueur-$index").performScrollTo().performTextReplacement(nom)
            compose.onNodeWithTag("score-joueur-$index").performScrollTo().performTextReplacement(score)
        }

        // Le vainqueur est annoncé avant même d'enregistrer.
        compose.onNodeWithText("Vainqueur : Alex").performScrollTo().assertIsDisplayed()

        compose.onNodeWithText("Enregistrer la partie").performScrollTo().performClick()

        // De retour sur le jeu : la partie et le classement sont là.
        compose.onNodeWithText("Classement").assertIsDisplayed()
        compose.onNodeWithTag("liste-parties-jeu").performScrollToNode(hasText("4 joueurs"))
        compose.onNodeWithText("4 joueurs").assertIsDisplayed()
        compose.onNodeWithTag("liste-parties-jeu").performScrollToNode(hasText("Camille"))
        compose.onNodeWithText("Camille").assertIsDisplayed()
    }

    @Test
    fun `le mode de comptage choisi determine le vainqueur`() {
        ouvrirApp()

        creerJeu("Jeu à malus", "POINTS_MIN")
        compose.onNodeWithText("Jeu à malus").performClick()
        compose.onNodeWithTag("fab-nouvelle-partie-jeu").performClick()

        compose.onNodeWithTag("nom-joueur-0").performScrollTo().performTextReplacement("Léo")
        compose.onNodeWithTag("score-joueur-0").performScrollTo().performTextReplacement("30")
        compose.onNodeWithTag("nom-joueur-1").performScrollTo().performTextReplacement("Alex")
        compose.onNodeWithTag("score-joueur-1").performScrollTo().performTextReplacement("90")

        // Avec « le plus bas gagne », c'est Léo et non Alex.
        compose.onNodeWithText("Vainqueur : Léo").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `deux joueurs portant le meme nom sont refuses`() {
        ouvrirApp()

        creerJeu("Mixmo", "POINTS_MAX")
        compose.onNodeWithText("Mixmo").performClick()
        compose.onNodeWithTag("fab-nouvelle-partie-jeu").performClick()

        compose.onNodeWithTag("nom-joueur-0").performScrollTo().performTextReplacement("Léo")
        compose.onNodeWithTag("nom-joueur-1").performScrollTo().performTextReplacement("léo")
        compose.onNodeWithText("Enregistrer la partie").performScrollTo().performClick()

        compose.onNodeWithText("Deux joueurs portent le même nom.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `Magic reste accessible et independant des autres jeux`() {
        ouvrirApp()

        creerJeu("Skull King", "POINTS_MAX")

        compose.onNodeWithTag("onglet-games").performClick()
        compose.onNodeWithText("Historique des parties").assertIsDisplayed()
        compose.onNodeWithText("Aucune partie enregistrée").assertIsDisplayed()

        compose.onNodeWithTag("onglet-decks").performClick()
        compose.onNodeWithText("Aucun deck").assertIsDisplayed()
    }
}
