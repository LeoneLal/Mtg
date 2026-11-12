package fr.leonelal.mtgduel

import fr.leonelal.mtgduel.data.CustomGame
import fr.leonelal.mtgduel.data.CustomMatch
import fr.leonelal.mtgduel.data.JeuxStats
import fr.leonelal.mtgduel.data.MatchParticipant
import fr.leonelal.mtgduel.data.ScoringMode
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class JeuxStatsTest {

    private fun partie(id: String, jour: Int, vararg scores: Pair<String, Int>) = CustomMatch(
        id = id,
        gameId = "j1",
        playedAt = jour * 86_400_000L,
        participants = scores.map { MatchParticipant(it.first, it.second) }
    )

    private val skullKing = CustomGame(id = "j1", name = "Skull King", scoring = ScoringMode.POINTS_MAX)
    private val golf = CustomGame(id = "j1", name = "Jeu à malus", scoring = ScoringMode.POINTS_MIN)
    private val manches = CustomGame(id = "j1", name = "Monopoly Deal", scoring = ScoringMode.MANCHES)

    @Test
    fun `en mode points le plus haut score gagne`() {
        val p = partie("m1", 1, "Léo" to 72, "Alex" to 95, "Camille" to 41)
        assertEquals("Alex", p.vainqueurUnique(ScoringMode.POINTS_MAX)?.name)
        assertEquals(95, p.meilleurScore(ScoringMode.POINTS_MAX))
    }

    @Test
    fun `en mode malus le plus bas score gagne`() {
        val p = partie("m1", 1, "Léo" to 72, "Alex" to 95, "Camille" to 41)
        assertEquals("Camille", p.vainqueurUnique(ScoringMode.POINTS_MIN)?.name)
        assertEquals(41, p.meilleurScore(ScoringMode.POINTS_MIN))
    }

    @Test
    fun `en mode manches le plus de manches gagnees l emporte`() {
        val p = partie("m1", 1, "Léo" to 3, "Alex" to 5)
        assertEquals("Alex", p.vainqueurUnique(ScoringMode.MANCHES)?.name)
    }

    @Test
    fun `une egalite ne designe aucun vainqueur unique`() {
        val p = partie("m1", 1, "Léo" to 50, "Alex" to 50, "Camille" to 10)
        assertTrue(p.estEgalite(ScoringMode.POINTS_MAX))
        assertNull(p.vainqueurUnique(ScoringMode.POINTS_MAX))
        assertEquals(listOf("Léo", "Alex"), p.vainqueurs(ScoringMode.POINTS_MAX).map { it.name })
    }

    @Test
    fun `les scores negatifs sont geres`() {
        val p = partie("m1", 1, "Léo" to -30, "Alex" to -10)
        assertEquals("Alex", p.vainqueurUnique(ScoringMode.POINTS_MAX)?.name)
        assertEquals("Léo", p.vainqueurUnique(ScoringMode.POINTS_MIN)?.name)
    }

    @Test
    fun `une partie a moins de deux joueurs est invalide`() {
        assertTrue(!partie("m1", 1, "Léo" to 10).isValid)
        assertTrue(!partie("m1", 1, "Léo" to 10, "" to 5).isValid)
        assertTrue(partie("m1", 1, "Léo" to 10, "Alex" to 5).isValid)
    }

    @Test
    fun `le classement compte victoires defaites et egalites`() {
        val parties = listOf(
            partie("m1", 1, "Léo" to 90, "Alex" to 40, "Camille" to 10),
            partie("m2", 2, "Léo" to 20, "Alex" to 80),
            partie("m3", 3, "Léo" to 50, "Alex" to 50)
        )
        val classement = JeuxStats.classement(skullKing, parties)

        val leo = classement.first { it.nom == "Léo" }
        assertEquals(3, leo.parties)
        assertEquals(1, leo.victoires)
        assertEquals(1, leo.egalites)
        assertEquals(1, leo.defaites)

        val camille = classement.first { it.nom == "Camille" }
        assertEquals(1, camille.parties)
        assertEquals(0, camille.victoires)
    }

    @Test
    fun `le classement est trie par victoires`() {
        val parties = listOf(
            partie("m1", 1, "Léo" to 90, "Alex" to 40),
            partie("m2", 2, "Léo" to 90, "Alex" to 40),
            partie("m3", 3, "Léo" to 10, "Alex" to 80)
        )
        assertEquals(listOf("Léo", "Alex"), JeuxStats.classement(skullKing, parties).map { it.nom })
    }

    @Test
    fun `le meilleur score suit le sens du comptage`() {
        val parties = listOf(partie("m1", 1, "Léo" to 90, "Alex" to 40))
        // En mode « le plus haut gagne », le meilleur score est le maximum…
        assertEquals(90, JeuxStats.classement(skullKing, parties).first { it.nom == "Léo" }.meilleurScore)
        // …et en mode malus, c'est le minimum.
        assertEquals(40, JeuxStats.classement(golf, parties).first { it.nom == "Alex" }.meilleurScore)
    }

    @Test
    fun `une difference de casse ou d espaces ne cree pas deux joueurs`() {
        val parties = listOf(
            partie("m1", 1, "Léo" to 90, "Alex" to 40),
            partie("m2", 2, " léo " to 10, "Alex" to 80)
        )
        val classement = JeuxStats.classement(skullKing, parties)
        assertEquals(2, classement.size)
        assertEquals(2, classement.first { it.nom.lowercase().trim() == "léo" }.parties)
    }

    @Test
    fun `les parties supprimees sont exclues du classement`() {
        val parties = listOf(
            partie("m1", 1, "Léo" to 90, "Alex" to 40),
            partie("m2", 2, "Léo" to 90, "Alex" to 40).copy(deleted = true)
        )
        assertEquals(1, JeuxStats.classement(skullKing, parties).first().parties)
    }

    @Test
    fun `les parties d un autre jeu ne polluent pas le classement`() {
        val parties = listOf(
            partie("m1", 1, "Léo" to 90, "Alex" to 40),
            partie("m2", 2, "Léo" to 90, "Alex" to 40).copy(gameId = "autre")
        )
        assertEquals(1, JeuxStats.classement(skullKing, parties).first().parties)
    }

    @Test
    fun `les noms connus servent de suggestions sans doublon`() {
        val parties = listOf(
            partie("m1", 1, "Léo" to 1, "Alex" to 2),
            partie("m2", 2, "léo" to 1, "Camille" to 2)
        )
        val noms = JeuxStats.nomsConnus(parties)
        assertEquals(3, noms.size)
        assertTrue(noms.contains("Camille"))
    }

    @Test
    fun `le mode manches se decrit comme un comptage de manches`() {
        assertEquals("Manches", manches.scoring.libelleScore)
        assertEquals("Points", skullKing.scoring.libelleScore)
        assertTrue(manches.scoring.plusHautGagne)
        assertTrue(!golf.scoring.plusHautGagne)
    }
}
