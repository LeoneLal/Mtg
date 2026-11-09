package fr.leonelal.mtgduel

import fr.leonelal.mtgduel.MtgDuelApp
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import fr.leonelal.mtgduel.ui.AppRoot
import fr.leonelal.mtgduel.ui.theme.MtgDuelTheme
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var viewModel: fr.leonelal.mtgduel.ui.AppViewModel

    @Before
    fun repartirDeZero() {
        viewModel = viewModelIsole()
    }

    private fun lancerApp() {
        compose.setContent { MtgDuelTheme { AppRoot(viewModel) } }
    }

    @Test
    fun `l application s ouvre sur l historique des parties`() {
        lancerApp()
        compose.onNodeWithText("Historique des parties").assertIsDisplayed()
        compose.onNodeWithText("Aucune partie enregistrée").assertIsDisplayed()
    }

    @Test
    fun `les quatre onglets s affichent sans erreur`() {
        lancerApp()
        compose.onNodeWithTag("onglet-decks").performClick()
        compose.onNodeWithText("Aucun deck").assertIsDisplayed()

        compose.onNodeWithTag("onglet-stats").performClick()
        compose.onNodeWithText("Pas encore de données").assertIsDisplayed()

        compose.onNodeWithTag("onglet-settings").performClick()
        compose.onNodeWithText("Les joueurs").assertIsDisplayed()

        compose.onNodeWithTag("onglet-games").performClick()
        compose.onNodeWithText("Historique des parties").assertIsDisplayed()
    }
}
