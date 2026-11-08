package fr.leonelal.mtgduel

import fr.leonelal.mtgduel.data.AppState
import fr.leonelal.mtgduel.data.Deck
import fr.leonelal.mtgduel.data.EndReasons
import fr.leonelal.mtgduel.data.Game
import fr.leonelal.mtgduel.data.GameSide
import fr.leonelal.mtgduel.data.Player
import fr.leonelal.mtgduel.data.StatsEngine
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class StatsEngineTest {

    private val moi = Player("p1", "Moi")
    private val lui = Player("p2", "Lui")
    private val atraxa = Deck(id = "d1", name = "Atraxa", ownerId = "p1")
    private val krenko = Deck(id = "d2", name = "Krenko", ownerId = "p2")
    private val edgar = Deck(id = "d3", name = "Edgar", ownerId = "p2")

    private fun game(
        id: String,
        day: Int,
        first: String,
        winner: String,
        deckOne: String = "d1",
        deckTwo: String = "d2",
        lifeOne: Int = 0,
        lifeTwo: Int = 0
    ) = Game(
        id = id,
        playedAt = day * 86_400_000L,
        format = "Commander",
        startingLife = 40,
        firstPlayerId = first,
        winnerId = winner,
        sides = listOf(GameSide("p1", deckOne, lifeOne), GameSide("p2", deckTwo, lifeTwo)),
        endReason = EndReasons.DAMAGE
    )

    /** 5 parties : p1 gagne 3 (dont 2 en commençant), p2 en gagne 2. */
    private val games = listOf(
        game("g1", 1, first = "p1", winner = "p1", lifeOne = 12, lifeTwo = 0),
        game("g2", 2, first = "p2", winner = "p2", lifeOne = -3, lifeTwo = 25),
        game("g3", 3, first = "p1", winner = "p1", lifeOne = 8, lifeTwo = 0),
        game("g4", 4, first = "p2", winner = "p2", deckTwo = "d3", lifeOne = 2, lifeTwo = 31),
        game("g5", 5, first = "p2", winner = "p1", lifeOne = 1, lifeTwo = 0)
    )

    @Test
    fun `le vainqueur et le perdant sont correctement identifies`() {
        val g = games[1]
        assertEquals("p2", g.winnerId)
        assertEquals("p1", g.loserId)
        assertEquals(-3, g.loserLifeRemaining)
        assertEquals(25, g.winnerSide?.lifeRemaining)
        assertTrue(g.isValid)
    }

    @Test
    fun `une partie sans joueur gagnant valide est rejetee`() {
        val invalide = games[0].copy(winnerId = "inconnu")
        assertTrue(!invalide.isValid)
        assertEquals(0, StatsEngine.overall(listOf(invalide)).totalGames)
    }

    @Test
    fun `le total et l avantage du premier joueur sont comptes`() {
        val overall = StatsEngine.overall(games)
        assertEquals(5, overall.totalGames)
        // g1, g2, g3, g4 gagnées par celui qui commence ; g5 non.
        assertEquals(4, overall.gamesWonByStarter)
        assertEquals(0.8, overall.starterWinRate, 0.0001)
    }

    @Test
    fun `les PV moyens du perdant sont calcules`() {
        val overall = StatsEngine.overall(games)
        // perdants : 0, -3, 0, 2, 0  -> moyenne -0,2
        assertEquals(-0.2, overall.avgLoserLife ?: 0.0, 0.0001)
    }

    @Test
    fun `les statistiques par joueur distinguent le fait de commencer`() {
        val stats = StatsEngine.forPlayer(moi, games)
        assertEquals(5, stats.games)
        assertEquals(3, stats.wins)
        assertEquals(2, stats.losses)
        assertEquals(0.6, stats.winRate, 0.0001)
        assertEquals(2, stats.gamesStarted)
        assertEquals(2, stats.winsWhenStarting)
        assertEquals(1.0, stats.winRateStarting, 0.0001)
        assertEquals(3, stats.gamesOnTheDraw)
        assertEquals(1, stats.winsOnTheDraw)
        // PV restants de p1 quand il gagne : 12, 8, 1
        assertEquals(7.0, stats.avgLifeOnWin ?: 0.0, 0.0001)
        // PV restants de p1 quand il perd : -3, 2
        assertEquals(-0.5, stats.avgLifeOnLoss ?: 0.0, 0.0001)
    }

    @Test
    fun `l echantillon alterne donc aucune serie ne depasse une victoire`() {
        val stats = StatsEngine.forPlayer(moi, games)
        assertEquals(1, stats.bestStreak)
        assertEquals(1, stats.currentStreak)

        val adversaire = StatsEngine.forPlayer(lui, games)
        assertEquals(1, adversaire.bestStreak)
        // la dernière partie a été perdue par p2 : plus de série en cours
        assertEquals(0, adversaire.currentStreak)
    }

    @Test
    fun `les series de victoires consecutives sont suivies`() {
        // p1 gagne 3 fois d'affilée, perd, puis gagne 2 fois.
        val serie = listOf(
            game("s1", 1, first = "p1", winner = "p1"),
            game("s2", 2, first = "p2", winner = "p1"),
            game("s3", 3, first = "p1", winner = "p1"),
            game("s4", 4, first = "p2", winner = "p2"),
            game("s5", 5, first = "p1", winner = "p1"),
            game("s6", 6, first = "p2", winner = "p1")
        )
        val stats = StatsEngine.forPlayer(moi, serie)
        assertEquals(3, stats.bestStreak)
        assertEquals(2, stats.currentStreak)

        val adversaire = StatsEngine.forPlayer(lui, serie)
        assertEquals(1, adversaire.bestStreak)
        assertEquals(0, adversaire.currentStreak)
    }

    @Test
    fun `les statistiques par deck ignorent les decks jamais joues`() {
        val jamaisJoue = Deck(id = "d4", name = "Jamais joué", ownerId = "p1")
        val stats = StatsEngine.forDecks(listOf(atraxa, krenko, edgar, jamaisJoue), games)
        assertTrue(stats.none { it.deck.id == "d4" })

        val atraxaStats = stats.first { it.deck.id == "d1" }
        assertEquals(5, atraxaStats.games)
        assertEquals(3, atraxaStats.wins)
        assertEquals(2, atraxaStats.losses)

        val edgarStats = stats.first { it.deck.id == "d3" }
        assertEquals(1, edgarStats.games)
        assertEquals(1, edgarStats.wins)
        assertEquals(31.0, edgarStats.avgLifeOnWin ?: 0.0, 0.0001)
        assertNull(edgarStats.avgLifeOnLoss)
    }

    @Test
    fun `les confrontations regroupent les paires de decks`() {
        val matchups = StatsEngine.matchups(games)
        assertEquals(2, matchups.size)

        val principal = matchups.first { setOf(it.deckAId, it.deckBId) == setOf("d1", "d2") }
        assertEquals(4, principal.games)
        // d1 (Atraxa) a gagné 3 des 4 parties face à Krenko
        val winsAtraxa = if (principal.deckAId == "d1") principal.winsA else principal.winsB
        assertEquals(3, winsAtraxa)
    }

    @Test
    fun `les noms de decks et de joueurs sont resolus depuis l etat`() {
        val state = AppState(
            players = listOf(moi, lui),
            decks = listOf(atraxa, krenko),
            games = games
        )
        assertEquals("Moi", state.playerName("p1"))
        assertEquals("Atraxa", state.deckName("d1"))
        assertEquals("Deck supprimé", state.deckName("inexistant"))
        assertEquals("Joueur inconnu", state.playerName("p9"))
    }

    @Test
    fun `les decks du joueur sont proposes en premier`() {
        val state = AppState(
            players = listOf(moi, lui),
            decks = listOf(krenko, atraxa, Deck(id = "d5", name = "Commun", ownerId = null)),
            games = emptyList()
        )
        val pourMoi = state.decksFor("p1").map { it.id }
        assertEquals(listOf("d1", "d5", "d2"), pourMoi)
    }

    @Test
    fun `les decks archives ne sont plus proposes`() {
        val state = AppState(
            players = listOf(moi, lui),
            decks = listOf(atraxa.copy(archived = true), krenko),
            games = emptyList()
        )
        assertEquals(listOf("d2"), state.decksFor("p1").map { it.id })
        assertEquals(listOf("d1", "d2"), state.decksFor("p1", includeArchived = true).map { it.id })
    }
}
