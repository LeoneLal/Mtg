package fr.leonelal.mtgduel

import fr.leonelal.mtgduel.data.AppState
import fr.leonelal.mtgduel.data.Deck
import fr.leonelal.mtgduel.data.Game
import fr.leonelal.mtgduel.data.GameSide
import fr.leonelal.mtgduel.data.Player
import fr.leonelal.mtgduel.data.SyncMerge
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class SyncMergeTest {

    private fun deck(id: String, nom: String, maj: Long, supprime: Boolean = false) =
        Deck(id = id, name = nom, ownerId = "p1", createdAt = maj, updatedAt = maj, deleted = supprime)

    private fun partie(id: String, maj: Long, supprime: Boolean = false, vainqueur: String = "p1") = Game(
        id = id,
        playedAt = maj,
        firstPlayerId = "p1",
        winnerId = vainqueur,
        sides = listOf(GameSide("p1", "d1", 5), GameSide("p2", "d2", 0)),
        updatedAt = maj,
        deleted = supprime
    )

    private fun etat(decks: List<Deck> = emptyList(), parties: List<Game> = emptyList(), majJoueurs: Long = 0) =
        AppState(
            players = listOf(Player("p1", "Moi", majJoueurs), Player("p2", "Lui", majJoueurs)),
            allDecks = decks,
            allGames = parties
        )

    @Test
    fun `les elements presents d un seul cote sont conserves des deux`() {
        val local = etat(decks = listOf(deck("d1", "Atraxa", 10)))
        val distant = etat(decks = listOf(deck("d2", "Krenko", 20)))
        val fusion = SyncMerge.merge(local, distant)
        assertEquals(listOf("d1", "d2"), fusion.allDecks.map { it.id }.sorted())
    }

    @Test
    fun `pour un meme element la version la plus recente gagne`() {
        val local = etat(decks = listOf(deck("d1", "Ancien nom", 10)))
        val distant = etat(decks = listOf(deck("d1", "Nouveau nom", 50)))
        assertEquals("Nouveau nom", SyncMerge.merge(local, distant).allDecks.single().name)
        // ...quel que soit le sens de la fusion
        assertEquals("Nouveau nom", SyncMerge.merge(distant, local).allDecks.single().name)
    }

    @Test
    fun `une version distante plus ancienne n ecrase pas la locale`() {
        val local = etat(decks = listOf(deck("d1", "À jour", 100)))
        val distant = etat(decks = listOf(deck("d1", "Périmé", 10)))
        assertEquals("À jour", SyncMerge.merge(local, distant).allDecks.single().name)
    }

    @Test
    fun `une suppression distante se propage en local`() {
        val local = etat(parties = listOf(partie("g1", 10)))
        val distant = etat(parties = listOf(partie("g1", 99, supprime = true)))
        val fusion = SyncMerge.merge(local, distant)
        assertTrue(fusion.allGames.single().deleted)
        assertTrue(fusion.games.isEmpty(), "la partie supprimée ne doit plus être visible")
    }

    @Test
    fun `une partie supprimee ne ressuscite pas si l autre renvoie une vieille copie`() {
        val local = etat(parties = listOf(partie("g1", 99, supprime = true)))
        val distant = etat(parties = listOf(partie("g1", 10)))
        assertTrue(SyncMerge.merge(local, distant).allGames.single().deleted)
    }

    @Test
    fun `le nom de joueur le plus recent gagne`() {
        val local = AppState(players = listOf(Player("p1", "Leo", 500), Player("p2", "Lui", 10)))
        val distant = AppState(players = listOf(Player("p1", "Ancien", 100), Player("p2", "Copain", 300)))
        val fusion = SyncMerge.merge(local, distant)
        assertEquals("Leo", fusion.playerName("p1"))
        assertEquals("Copain", fusion.playerName("p2"))
    }

    @Test
    fun `fusionner avec un serveur vide ne perd rien`() {
        val local = etat(decks = listOf(deck("d1", "Atraxa", 10)), parties = listOf(partie("g1", 20)))
        val fusion = SyncMerge.merge(local, AppState(players = emptyList()))
        assertEquals(1, fusion.allDecks.size)
        assertEquals(1, fusion.allGames.size)
        assertEquals("Moi", fusion.playerName("p1"))
    }

    @Test
    fun `la fusion est idempotente`() {
        val local = etat(decks = listOf(deck("d1", "Atraxa", 10)), parties = listOf(partie("g1", 20)))
        val distant = etat(decks = listOf(deck("d2", "Krenko", 30)))
        val une = SyncMerge.merge(local, distant)
        val deux = SyncMerge.merge(une, distant)
        assertEquals(une, deux)
    }
}
