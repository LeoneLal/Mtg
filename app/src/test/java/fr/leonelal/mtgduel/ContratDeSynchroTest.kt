package fr.leonelal.mtgduel

import fr.leonelal.mtgduel.data.AppState
import fr.leonelal.mtgduel.data.Deck
import fr.leonelal.mtgduel.data.Game
import fr.leonelal.mtgduel.data.GameSide
import fr.leonelal.mtgduel.data.Player
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

/**
 * Le format envoyé au serveur est un contrat : `serveur/sync.php` lit les clés
 * `players`, `decks`, `games`, et arbitre la fusion sur `id` et `updatedAt`.
 * Renommer l'un de ces champs côté application casserait silencieusement la
 * synchronisation — ce test échoue d'abord.
 */
class ContratDeSynchroTest {

    private val json = Json { encodeDefaults = true }

    private val etat = AppState(
        players = listOf(Player("p1", "Léo", 1000), Player("p2", "Mon copain", 1000)),
        allDecks = listOf(
            Deck("d1", "Atraxa", "p1", listOf("W", "U", "B", "G"), "Commandant", false, 900, 900, false),
            Deck("d2", "Krenko « Gobelins »", "p2", listOf("R"), "", false, 950, 950, true)
        ),
        allGames = listOf(
            Game(
                "g1", 1_700_000_000_000, "Commander", 40, "p2", "p1",
                listOf(GameSide("p1", "d1", 17), GameSide("p2", "d2", -3)),
                "COMMANDER_DAMAGE", 9, "Partie serrée — ✦", 1_700_000_000_000, false
            )
        )
    )

    @Test
    fun `le payload porte les cles attendues par l API`() {
        val racine = Json.parseToJsonElement(json.encodeToString(AppState.serializer(), etat)).jsonObject
        assertEquals(
            setOf("schemaVersion", "players", "decks", "games", "customGames", "customMatches"),
            racine.keys
        )

        val deck = racine["decks"]!!.jsonArray.first().jsonObject
        assertTrue("id" in deck && "updatedAt" in deck && "deleted" in deck, "clés du deck : ${deck.keys}")

        val partie = racine["games"]!!.jsonArray.first().jsonObject
        assertTrue("id" in partie && "updatedAt" in partie && "deleted" in partie, "clés de la partie : ${partie.keys}")

        val joueur = racine["players"]!!.jsonArray.first().jsonObject
        assertTrue("id" in joueur && "updatedAt" in joueur, "clés du joueur : ${joueur.keys}")
    }

    @Test
    fun `un aller-retour par le format filaire ne perd rien`() {
        val encode = json.encodeToString(AppState.serializer(), etat)
        val relu = Json { ignoreUnknownKeys = true }.decodeFromString(AppState.serializer(), encode)
        assertEquals(etat, relu)
    }

    @Test
    fun `les PV negatifs et les caracteres accentues survivent a l encodage`() {
        val encode = json.encodeToString(AppState.serializer(), etat)
        val partie = Json.parseToJsonElement(encode).jsonObject["games"]!!.jsonArray.first().jsonObject
        assertEquals(-3, partie["sides"]!!.jsonArray[1].jsonObject["lifeRemaining"]!!.jsonPrimitive.content.toInt())
        assertEquals("Partie serrée — ✦", partie["notes"]!!.jsonPrimitive.content)
    }
}
