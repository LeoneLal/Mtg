package fr.leonelal.mtgduel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import fr.leonelal.mtgduel.data.AppState
import fr.leonelal.mtgduel.data.Deck
import fr.leonelal.mtgduel.data.EndReasons
import fr.leonelal.mtgduel.data.Game
import fr.leonelal.mtgduel.data.GameSide
import fr.leonelal.mtgduel.data.Player
import fr.leonelal.mtgduel.ui.AppRoot
import fr.leonelal.mtgduel.ui.theme.MtgDuelTheme
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Rend chaque écran dans un fichier PNG : permet de relire l'interface sans appareil.
 * Les captures atterrissent dans `app/build/screenshots` (ou dans le dossier passé via
 * `-Dmtg.shots.dir=…`).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class ScreenshotTest {

    private val outDir = File(System.getProperty("mtg.shots.dir") ?: "build/screenshots")

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val moi = Player("p1", "Moi")
    private val copain = Player("p2", "Mon copain")
    private val atraxa = Deck("d1", "Atraxa Superfriends", "p1", listOf("W", "U", "B", "G"), "Commandant : Atraxa, Praetors' Voice")
    private val krenko = Deck("d2", "Krenko Gobelins", "p2", listOf("R"), "Commandant : Krenko, Mob Boss")
    private val edgar = Deck("d3", "Edgar Markov", "p2", listOf("W", "B", "R"))

    private fun jours(n: Long) = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(n)

    private val donnees = AppState(
        players = listOf(moi, copain),
        decks = listOf(atraxa, krenko, edgar),
        games = listOf(
            Game(
                id = "g1", playedAt = jours(0), firstPlayerId = "p2", winnerId = "p1",
                sides = listOf(GameSide("p1", "d1", 17), GameSide("p2", "d2", 0)),
                endReason = EndReasons.DAMAGE, turns = 9, notes = "Retournée au dernier tour."
            ),
            Game(
                id = "g2", playedAt = jours(1), firstPlayerId = "p1", winnerId = "p2",
                sides = listOf(GameSide("p1", "d1", -3), GameSide("p2", "d3", 22)),
                endReason = EndReasons.COMMANDER_DAMAGE, turns = 7
            ),
            Game(
                id = "g3", playedAt = jours(4), firstPlayerId = "p1", winnerId = "p1",
                sides = listOf(GameSide("p1", "d1", 8), GameSide("p2", "d2", 2)),
                endReason = EndReasons.DAMAGE, turns = 12
            ),
            Game(
                id = "g4", playedAt = jours(6), firstPlayerId = "p2", winnerId = "p2",
                sides = listOf(GameSide("p1", "d1", 4), GameSide("p2", "d2", 31)),
                endReason = EndReasons.CONCEDE
            )
        )
    )

    @Before
    fun preparerLesDonnees() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.filesDir.listFiles()?.forEach { it.deleteRecursively() }
        File(context.filesDir, "mtg-duel-data.json")
            .writeText(Json { encodeDefaults = true }.encodeToString(AppState.serializer(), donnees))
        outDir.mkdirs()
    }

    private fun capture(nom: String) {
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        compose.runOnUiThread {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(
                view.width.coerceAtLeast(1),
                view.height.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            view.draw(Canvas(bitmap))
            File(outDir, "$nom.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    @Test
    fun capturer_tous_les_ecrans() {
        compose.setContent { MtgDuelTheme { AppRoot() } }

        capture("01-historique")

        compose.onNodeWithTag("onglet-decks").performClick()
        capture("02-decks")

        compose.onNodeWithTag("onglet-stats").performClick()
        capture("03-stats")

        compose.onNodeWithTag("onglet-settings").performClick()
        capture("04-reglages")

        compose.onNodeWithTag("onglet-games").performClick()
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithTag("fab-nouvelle-partie").performClick()
        capture("05-nouvelle-partie")
    }
}
