package fr.leonelal.mtgduel.data

import kotlinx.serialization.Serializable
import java.util.UUID

/** Un des deux joueurs. L'application est pensée pour un duel récurrent entre 2 personnes. */
@Serializable
data class Player(
    val id: String,
    val name: String
)

/** Un deck utilisé par l'un des joueurs (ou partagé). */
@Serializable
data class Deck(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    /** Propriétaire du deck, ou null si le deck est commun aux deux joueurs. */
    val ownerId: String? = null,
    /** Codes de couleurs : W, U, B, R, G, C. */
    val colors: List<String> = emptyList(),
    val note: String = "",
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/** Ce qu'un joueur a fait dans une partie donnée. */
@Serializable
data class GameSide(
    val playerId: String,
    val deckId: String,
    /** Points de vie restants au moment où la partie s'est terminée. */
    val lifeRemaining: Int
)

/** Une partie jouée. */
@Serializable
data class Game(
    val id: String = UUID.randomUUID().toString(),
    val playedAt: Long,
    val format: String = Formats.COMMANDER,
    val startingLife: Int = 40,
    /** Joueur qui a commencé la partie. */
    val firstPlayerId: String,
    val winnerId: String,
    /** Toujours deux entrées : une par joueur. */
    val sides: List<GameSide>,
    val endReason: String = EndReasons.DAMAGE,
    val turns: Int? = null,
    val notes: String = ""
) {
    fun sideOf(playerId: String): GameSide? = sides.firstOrNull { it.playerId == playerId }

    val loserId: String?
        get() = sides.firstOrNull { it.playerId != winnerId }?.playerId

    val winnerSide: GameSide?
        get() = sideOf(winnerId)

    val loserSide: GameSide?
        get() = sides.firstOrNull { it.playerId != winnerId }

    /** PV restants du perdant : l'information clé du suivi. */
    val loserLifeRemaining: Int?
        get() = loserSide?.lifeRemaining

    fun deckIdOf(playerId: String): String? = sideOf(playerId)?.deckId

    fun involvesDeck(deckId: String): Boolean = sides.any { it.deckId == deckId }

    val isValid: Boolean
        get() = sides.size == 2 &&
            sides.map { it.playerId }.toSet().size == 2 &&
            sides.any { it.playerId == winnerId }
}

object Formats {
    const val COMMANDER = "Commander"
    const val STANDARD = "Standard"
    const val LIMITED = "Limité"
    const val OTHER = "Autre"

    /** Points de vie de départ habituels par format. */
    val defaults: List<Pair<String, Int>> = listOf(
        COMMANDER to 40,
        STANDARD to 20,
        LIMITED to 20,
        OTHER to 20
    )

    fun startingLifeFor(format: String): Int =
        defaults.firstOrNull { it.first == format }?.second ?: 20
}

object EndReasons {
    const val DAMAGE = "DAMAGE"
    const val CONCEDE = "CONCEDE"
    const val DECK_OUT = "DECK_OUT"
    const val POISON = "POISON"
    const val COMMANDER_DAMAGE = "COMMANDER_DAMAGE"
    const val ALT_WIN = "ALT_WIN"
    const val OTHER = "OTHER"

    val all: List<Pair<String, String>> = listOf(
        DAMAGE to "Dégâts",
        COMMANDER_DAMAGE to "Dégâts de commandant",
        POISON to "Poison",
        DECK_OUT to "Plus de cartes",
        CONCEDE to "Abandon",
        ALT_WIN to "Victoire alternative",
        OTHER to "Autre"
    )

    fun label(code: String): String = all.firstOrNull { it.first == code }?.second ?: "Autre"
}

object MtgColors {
    /** code -> (libellé, couleur d'affichage) */
    val all: List<Triple<String, String, Long>> = listOf(
        Triple("W", "Blanc", 0xFFF4EFD6),
        Triple("U", "Bleu", 0xFF9FD3F0),
        Triple("B", "Noir", 0xFF9C8FA6),
        Triple("R", "Rouge", 0xFFEFA08A),
        Triple("G", "Vert", 0xFF9FD0A8),
        Triple("C", "Incolore", 0xFFC9C9C9)
    )

    fun label(code: String): String = all.firstOrNull { it.first == code }?.second ?: code
    fun tint(code: String): Long = all.firstOrNull { it.first == code }?.third ?: 0xFFC9C9C9
}

/** L'intégralité des données de l'application, sérialisée dans un seul fichier JSON. */
@Serializable
data class AppState(
    val schemaVersion: Int = 1,
    val players: List<Player> = defaultPlayers,
    val decks: List<Deck> = emptyList(),
    val games: List<Game> = emptyList()
) {
    val playerOne: Player get() = players.getOrElse(0) { defaultPlayers[0] }
    val playerTwo: Player get() = players.getOrElse(1) { defaultPlayers[1] }

    fun playerName(id: String?): String =
        players.firstOrNull { it.id == id }?.name ?: "Joueur inconnu"

    fun deck(id: String?): Deck? = decks.firstOrNull { it.id == id }

    fun deckName(id: String?): String = deck(id)?.name ?: "Deck supprimé"

    /**
     * Decks proposés à un joueur : tous les decks actifs, les siens (puis les decks
     * communs) en tête de liste, pour ne jamais bloquer une saisie si vous échangez
     * vos decks le temps d'une partie.
     */
    fun decksFor(playerId: String, includeArchived: Boolean = false): List<Deck> =
        decks.filter { includeArchived || !it.archived }
            .sortedWith(
                compareBy<Deck>(
                    { if (it.ownerId == playerId) 0 else if (it.ownerId == null) 1 else 2 },
                    { it.name.lowercase() }
                )
            )

    val gamesNewestFirst: List<Game>
        get() = games.sortedByDescending { it.playedAt }

    companion object {
        val defaultPlayers = listOf(
            Player("p1", "Moi"),
            Player("p2", "Mon copain")
        )
    }
}
