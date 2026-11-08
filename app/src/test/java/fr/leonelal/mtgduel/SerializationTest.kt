package fr.leonelal.mtgduel

import fr.leonelal.mtgduel.data.AppState
import fr.leonelal.mtgduel.data.Deck
import fr.leonelal.mtgduel.data.Game
import fr.leonelal.mtgduel.data.GameSide
import fr.leonelal.mtgduel.data.Player
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.junit.Test

class SerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val state = AppState(
        players = listOf(Player("p1", "Moi"), Player("p2", "Lui")),
        decks = listOf(Deck(id = "d1", name = "Atraxa", ownerId = "p1", colors = listOf("W", "U", "B", "G"))),
        games = listOf(
            Game(
                id = "g1",
                playedAt = 1_700_000_000_000,
                firstPlayerId = "p1",
                winnerId = "p1",
                sides = listOf(GameSide("p1", "d1", 17), GameSide("p2", "d1", -2)),
                turns = 11,
                notes = "Partie serrée"
            )
        )
    )

    @Test
    fun `une sauvegarde se relit a l identique`() {
        val encoded = json.encodeToString(AppState.serializer(), state)
        val decoded = json.decodeFromString(AppState.serializer(), encoded)
        assertEquals(state, decoded)
        assertEquals(-2, decoded.games.first().loserLifeRemaining)
    }

    @Test
    fun `un fichier minimal est accepte grace aux valeurs par defaut`() {
        val minimal = """
            {
              "players": [{"id":"p1","name":"A"},{"id":"p2","name":"B"}],
              "games": [
                {
                  "id":"g1",
                  "playedAt": 1700000000000,
                  "firstPlayerId":"p1",
                  "winnerId":"p2",
                  "sides":[
                    {"playerId":"p1","deckId":"d1","lifeRemaining":0},
                    {"playerId":"p2","deckId":"d2","lifeRemaining":9}
                  ]
                }
              ]
            }
        """.trimIndent()
        val decoded = json.decodeFromString(AppState.serializer(), minimal)
        assertEquals(1, decoded.games.size)
        assertEquals("Commander", decoded.games.first().format)
        assertEquals(40, decoded.games.first().startingLife)
        assertTrue(decoded.decks.isEmpty())
        assertEquals(0, decoded.games.first().loserLifeRemaining)
    }

    @Test
    fun `les champs inconnus d une version future sont ignores`() {
        val futur = """
            {"schemaVersion": 99, "players": [], "decks": [], "games": [], "nouveauChamp": true}
        """.trimIndent()
        val decoded = json.decodeFromString(AppState.serializer(), futur)
        assertEquals(99, decoded.schemaVersion)
        // playerOne/playerTwo restent utilisables même si la liste est vide
        assertEquals("Moi", decoded.playerOne.name)
        assertEquals("Mon copain", decoded.playerTwo.name)
    }
}
