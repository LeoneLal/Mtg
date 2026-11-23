package fr.leonelal.mtgduel

import fr.leonelal.mtgduel.data.AppState
import fr.leonelal.mtgduel.data.Deck
import fr.leonelal.mtgduel.data.Game
import fr.leonelal.mtgduel.data.GameSide
import fr.leonelal.mtgduel.data.Player
import fr.leonelal.mtgduel.data.StatsEngine
import fr.leonelal.mtgduel.data.SyncMerge
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

/** Les parties de Magic à plus de deux joueurs. */
class MultijoueurTest {

    private val joueurs = listOf(
        Player("p1", "Léo", 1), Player("p2", "Alex", 1),
        Player("p3", "Camille", 1), Player("p4", "Sam", 1)
    )

    private fun partie(
        id: String,
        jour: Int,
        premier: String,
        vainqueur: String,
        vararg sieges: Triple<String, String, Int>
    ) = Game(
        id = id,
        playedAt = jour * 86_400_000L,
        startingLife = 40,
        firstPlayerId = premier,
        winnerId = vainqueur,
        sides = sieges.map { GameSide(it.first, it.second, it.third) }
    )

    private val commander4 = partie(
        "g1", 1, premier = "p3", vainqueur = "p1",
        Triple("p1", "d1", 23), Triple("p2", "d2", 0),
        Triple("p3", "d3", -4), Triple("p4", "d4", 6)
    )

    @Test
    fun `une partie a quatre joueurs est valide`() {
        assertTrue(commander4.isValid)
        assertTrue(commander4.isMultiplayer)
        assertEquals(4, commander4.sides.size)
        assertEquals(3, commander4.loserSides.size)
    }

    @Test
    fun `le vainqueur est affiche en premier`() {
        assertEquals("p1", commander4.sidesOrdered.first().playerId)
        assertEquals(4, commander4.sidesOrdered.size)
    }

    @Test
    fun `un meme joueur ne peut pas occuper deux sieges`() {
        val doublon = commander4.copy(
            sides = listOf(
                GameSide("p1", "d1", 10), GameSide("p1", "d2", 0), GameSide("p3", "d3", 0)
            )
        )
        assertFalse(doublon.isValid)
    }

    @Test
    fun `une partie dont le vainqueur n est pas a table est invalide`() {
        assertFalse(commander4.copy(winnerId = "p9").isValid)
    }

    @Test
    fun `les PV moyens comptent tous les perdants et pas seulement le premier`() {
        val overall = StatsEngine.overall(listOf(commander4))
        // perdants : 0, -4 et 6 → moyenne 2/3
        assertEquals(0.666, overall.avgLoserLife ?: 0.0, 0.01)
    }

    @Test
    fun `l avantage du premier joueur ne se calcule que sur les duels`() {
        val duel = partie(
            "g2", 2, premier = "p1", vainqueur = "p1",
            Triple("p1", "d1", 12), Triple("p2", "d2", 0)
        )
        val overall = StatsEngine.overall(listOf(commander4, duel))
        assertEquals(2, overall.totalGames)
        assertEquals(1, overall.duels)
        assertEquals(1, overall.multiplayerGames)
        // Seul le duel compte, et il a été gagné par celui qui commençait.
        assertEquals(1, overall.duelsWonByStarter)
        assertEquals(1.0, overall.starterWinRate, 0.0001)
    }

    @Test
    fun `les statistiques d un joueur fonctionnent a plusieurs`() {
        val stats = StatsEngine.forPlayer(joueurs[2], listOf(commander4))
        assertEquals(1, stats.games)
        assertEquals(0, stats.wins)
        assertEquals(1, stats.gamesStarted)
        assertEquals(0, stats.winsWhenStarting)
        assertEquals(-4.0, stats.avgLifeOnLoss ?: 0.0, 0.0001)
    }

    @Test
    fun `un deck qui perd en troisieme position est bien compte`() {
        val decks = listOf(
            Deck(id = "d1", name = "Atraxa", ownerId = "p1"),
            Deck(id = "d3", name = "Edgar", ownerId = "p3")
        )
        val stats = StatsEngine.forDecks(decks, listOf(commander4))
        val edgar = stats.first { it.deck.id == "d3" }
        assertEquals(1, edgar.games)
        assertEquals(0, edgar.wins)
        // Son siège affichait -4 PV, pas ceux du premier perdant venu.
        assertEquals(-4.0, edgar.avgLifeOnLoss ?: 0.0, 0.0001)
    }

    @Test
    fun `les confrontations de decks ignorent les parties a plusieurs`() {
        val duel = partie(
            "g2", 2, premier = "p1", vainqueur = "p1",
            Triple("p1", "d1", 12), Triple("p2", "d2", 0)
        )
        val matchups = StatsEngine.matchups(listOf(commander4, duel))
        assertEquals(1, matchups.size)
        assertEquals(setOf("d1", "d2"), setOf(matchups.single().deckAId, matchups.single().deckBId))
    }

    // ------------------------------------------------------- synchronisation

    @Test
    fun `un joueur ajoute sur un telephone arrive sur l autre`() {
        val local = AppState(allPlayers = joueurs.take(2))
        val distant = AppState(allPlayers = joueurs)
        val fusion = SyncMerge.merge(local, distant)
        assertEquals(listOf("p1", "p2", "p3", "p4"), fusion.players.map { it.id })
        assertEquals("Camille", fusion.playerName("p3"))
    }

    @Test
    fun `le retrait d un joueur se propage`() {
        val local = AppState(allPlayers = joueurs)
        val distant = AppState(
            allPlayers = listOf(joueurs[3].copy(deleted = true, updatedAt = 99))
        )
        val fusion = SyncMerge.merge(local, distant)
        assertEquals(3, fusion.players.size)
        // Son nom reste résolvable pour les parties déjà enregistrées.
        assertEquals("Sam", fusion.playerName("p4"))
    }

    @Test
    fun `un renommage plus ancien n ecrase pas un nom plus recent`() {
        val local = AppState(allPlayers = listOf(Player("p1", "Léo", 500)))
        val distant = AppState(allPlayers = listOf(Player("p1", "Ancien", 100)))
        assertEquals("Léo", SyncMerge.merge(local, distant).playerName("p1"))
    }
}
