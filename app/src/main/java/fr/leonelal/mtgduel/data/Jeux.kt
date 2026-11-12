package fr.leonelal.mtgduel.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/** Façon dont un jeu désigne son vainqueur. */
@Serializable
enum class ScoringMode {
    /** Une manche gagnée vaut un point ; on arrête quand on veut, le plus de manches l'emporte. */
    @SerialName("manches")
    MANCHES,

    /** Chacun a un total de points ; le plus élevé l'emporte. */
    @SerialName("points_max")
    POINTS_MAX,

    /** Chacun a un total de points ; le plus bas l'emporte. */
    @SerialName("points_min")
    POINTS_MIN;

    val libelle: String
        get() = when (this) {
            MANCHES -> "Manches gagnées"
            POINTS_MAX -> "Points, le plus haut gagne"
            POINTS_MIN -> "Points, le plus bas gagne"
        }

    val explication: String
        get() = when (this) {
            MANCHES -> "Une manche gagnée vaut un point. Vous arrêtez quand vous voulez : celui qui a le plus de manches l'emporte."
            POINTS_MAX -> "Vous saisissez le total de points de chacun. Le plus élevé gagne."
            POINTS_MIN -> "Vous saisissez le total de points de chacun. Le plus bas gagne."
        }

    /** Intitulé du champ de saisie pour un joueur. */
    val libelleScore: String
        get() = if (this == MANCHES) "Manches" else "Points"

    val plusHautGagne: Boolean
        get() = this != POINTS_MIN
}

/** Un jeu créé par l'utilisateur : Skull King, Mixmo, Monopoly Deal… */
@Serializable
data class CustomGame(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val scoring: ScoringMode = ScoringMode.MANCHES,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    /** Date de dernière modification, en millis : arbitre la fusion entre appareils. */
    val updatedAt: Long = createdAt,
    /** Pierre tombale : voir [Deck.deleted]. */
    val deleted: Boolean = false
)

/** Un joueur dans une partie d'un jeu : un nom libre, donc les invités sont possibles. */
@Serializable
data class MatchParticipant(
    val name: String,
    val score: Int = 0
)

/** Une partie jouée à l'un des jeux créés. */
@Serializable
data class CustomMatch(
    val id: String = UUID.randomUUID().toString(),
    val gameId: String,
    val playedAt: Long,
    val participants: List<MatchParticipant> = emptyList(),
    val notes: String = "",
    /** Date de dernière modification, en millis : arbitre la fusion entre appareils. */
    val updatedAt: Long = playedAt,
    /** Pierre tombale : voir [Deck.deleted]. */
    val deleted: Boolean = false
) {
    val isValid: Boolean
        get() = gameId.isNotBlank() &&
            participants.size >= 2 &&
            participants.all { it.name.isNotBlank() }

    /** Le meilleur score de la partie, au sens du mode de comptage. */
    fun meilleurScore(mode: ScoringMode): Int? =
        if (participants.isEmpty()) null
        else if (mode.plusHautGagne) participants.maxOf { it.score }
        else participants.minOf { it.score }

    /** Les participants à égalité au meilleur score : un seul en général, plusieurs si égalité. */
    fun vainqueurs(mode: ScoringMode): List<MatchParticipant> {
        val meilleur = meilleurScore(mode) ?: return emptyList()
        return participants.filter { it.score == meilleur }
    }

    fun estEgalite(mode: ScoringMode): Boolean = vainqueurs(mode).size > 1

    /** Le vainqueur unique, ou null en cas d'égalité. */
    fun vainqueurUnique(mode: ScoringMode): MatchParticipant? =
        vainqueurs(mode).singleOrNull()
}

/** Classement d'un joueur sur l'ensemble des parties d'un jeu. */
data class ClassementJoueur(
    val nom: String,
    val parties: Int,
    val victoires: Int,
    val egalites: Int,
    val meilleurScore: Int?,
    val scoreMoyen: Double?
) {
    val defaites: Int get() = parties - victoires - egalites
    val tauxVictoire: Double get() = if (parties == 0) 0.0 else victoires.toDouble() / parties
}

object JeuxStats {

    /** Deux saisies d'un même prénom ne doivent pas créer deux joueurs. */
    fun cle(nom: String): String = nom.trim().lowercase()

    fun classement(jeu: CustomGame, parties: List<CustomMatch>): List<ClassementJoueur> {
        val valides = parties.filter { it.isValid && !it.deleted && it.gameId == jeu.id }
            .sortedBy { it.playedAt }
        if (valides.isEmpty()) return emptyList()

        val noms = LinkedHashMap<String, String>()
        val jouees = mutableMapOf<String, Int>()
        val gagnees = mutableMapOf<String, Int>()
        val nulles = mutableMapOf<String, Int>()
        val scores = mutableMapOf<String, MutableList<Int>>()

        valides.forEach { partie ->
            val vainqueurs = partie.vainqueurs(jeu.scoring).map { cle(it.name) }.toSet()
            val egalite = partie.estEgalite(jeu.scoring)
            partie.participants.forEach { participant ->
                val cle = cle(participant.name)
                // Le nom affiché suit la dernière orthographe saisie.
                noms[cle] = participant.name.trim()
                jouees[cle] = (jouees[cle] ?: 0) + 1
                if (cle in vainqueurs) {
                    if (egalite) nulles[cle] = (nulles[cle] ?: 0) + 1
                    else gagnees[cle] = (gagnees[cle] ?: 0) + 1
                }
                scores.getOrPut(cle) { mutableListOf() }.add(participant.score)
            }
        }

        return noms.map { (cle, nom) ->
            val sesScores = scores[cle].orEmpty()
            ClassementJoueur(
                nom = nom,
                parties = jouees[cle] ?: 0,
                victoires = gagnees[cle] ?: 0,
                egalites = nulles[cle] ?: 0,
                meilleurScore = if (sesScores.isEmpty()) null
                else if (jeu.scoring.plusHautGagne) sesScores.max() else sesScores.min(),
                scoreMoyen = if (sesScores.isEmpty()) null else sesScores.average()
            )
        }.sortedWith(
            compareByDescending<ClassementJoueur> { it.victoires }
                .thenByDescending { it.tauxVictoire }
                .thenBy { it.nom.lowercase() }
        )
    }

    /** Noms déjà utilisés, les plus récents d'abord : sert à proposer des suggestions de saisie. */
    fun nomsConnus(parties: List<CustomMatch>): List<String> =
        parties.filter { !it.deleted }
            .sortedByDescending { it.playedAt }
            .flatMap { it.participants }
            .map { it.name.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
}
