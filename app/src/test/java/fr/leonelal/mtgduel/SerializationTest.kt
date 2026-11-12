package fr.leonelal.mtgduel

import fr.leonelal.mtgduel.MtgDuelApp
import fr.leonelal.mtgduel.data.AppState
import fr.leonelal.mtgduel.data.CustomGame
import fr.leonelal.mtgduel.data.CustomMatch
import fr.leonelal.mtgduel.data.Deck
import fr.leonelal.mtgduel.data.MatchParticipant
import fr.leonelal.mtgduel.data.ScoringMode
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
        allDecks = listOf(Deck(id = "d1", name = "Atraxa", ownerId = "p1", colors = listOf("W", "U", "B", "G"))),
        allGames = listOf(
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

    @Test
    fun `un fichier anterieur aux autres jeux reste lisible`() {
        val ancien = """
            {
              "schemaVersion": 2,
              "players": [{"id":"p1","name":"Léo","updatedAt":1}],
              "decks": [],
              "games": []
            }
        """.trimIndent()
        val decode = json.decodeFromString(AppState.serializer(), ancien)
        assertTrue(decode.allCustomGames.isEmpty())
        assertTrue(decode.allCustomMatches.isEmpty())
        assertEquals("Léo", decode.playerName("p1"))
    }

    @Test
    fun `les jeux et leurs parties se relisent a l identique`() {
        val avec = state.copy(
            allCustomGames = listOf(
                CustomGame(id = "j1", name = "Skull King", scoring = ScoringMode.POINTS_MAX, createdAt = 10)
            ),
            allCustomMatches = listOf(
                CustomMatch(
                    id = "m1", gameId = "j1", playedAt = 20,
                    participants = listOf(
                        MatchParticipant("Léo", 72),
                        MatchParticipant("Alex", -15),
                        MatchParticipant("Camille", 0)
                    ),
                    notes = "Manche décisive"
                )
            )
        )
        val relu = json.decodeFromString(AppState.serializer(), json.encodeToString(AppState.serializer(), avec))
        assertEquals(avec, relu)
        assertEquals(3, relu.allCustomMatches.single().participants.size)
        assertEquals(ScoringMode.POINTS_MAX, relu.allCustomGames.single().scoring)
    }

    @Test
    fun `le mode de comptage est stocke sous un nom stable`() {
        val encode = json.encodeToString(
            AppState.serializer(),
            AppState(allCustomGames = listOf(CustomGame(id = "j1", name = "X", scoring = ScoringMode.POINTS_MIN)))
        )
        // Renommer l'enum ne doit pas invalider les données déjà enregistrées.
        assertTrue(encode.contains("\"points_min\""), encode)
    }
}
