package fr.leonelal.mtgduel.data

data class PlayerStats(
    val player: Player,
    val games: Int,
    val wins: Int,
    val losses: Int,
    val gamesStarted: Int,
    val winsWhenStarting: Int,
    val gamesOnTheDraw: Int,
    val winsOnTheDraw: Int,
    val avgLifeOnWin: Double?,
    val avgLifeOnLoss: Double?,
    val currentStreak: Int,
    val bestStreak: Int
) {
    val winRate: Double get() = if (games == 0) 0.0 else wins.toDouble() / games
    val winRateStarting: Double get() = if (gamesStarted == 0) 0.0 else winsWhenStarting.toDouble() / gamesStarted
    val winRateOnTheDraw: Double get() = if (gamesOnTheDraw == 0) 0.0 else winsOnTheDraw.toDouble() / gamesOnTheDraw
}

data class DeckStats(
    val deck: Deck,
    val games: Int,
    val wins: Int,
    val losses: Int,
    val avgLifeOnWin: Double?,
    val avgLifeOnLoss: Double?,
    val lastPlayed: Long?
) {
    val winRate: Double get() = if (games == 0) 0.0 else wins.toDouble() / games
}

data class MatchupStats(
    val deckAId: String,
    val deckBId: String,
    val games: Int,
    val winsA: Int,
    val winsB: Int,
    val lastPlayed: Long
)

data class OverallStats(
    val totalGames: Int,
    /** Parties à deux : seules elles permettent de juger l'avantage du premier joueur. */
    val duels: Int,
    val duelsWonByStarter: Int,
    val multiplayerGames: Int,
    val avgLoserLife: Double?,
    val avgTurns: Double?,
    val firstGame: Long?,
    val lastGame: Long?,
    val endReasonCounts: List<Pair<String, Int>>
) {
    val starterWinRate: Double get() = if (duels == 0) 0.0 else duelsWonByStarter.toDouble() / duels
}

object StatsEngine {

    fun overall(games: List<Game>): OverallStats {
        val valid = games.filter { it.isValid }
        val duels = valid.filterNot { it.isMultiplayer }
        // Chaque perdant compte : à plusieurs, il y en a plus d'un par partie.
        val loserLives = valid.flatMap { partie -> partie.loserSides.map { it.lifeRemaining } }
        val turns = valid.mapNotNull { it.turns }
        val reasons = valid.groupingBy { it.endReason }.eachCount()
            .toList().sortedByDescending { it.second }
        return OverallStats(
            totalGames = valid.size,
            duels = duels.size,
            duelsWonByStarter = duels.count { it.winnerId == it.firstPlayerId },
            multiplayerGames = valid.size - duels.size,
            avgLoserLife = loserLives.averageOrNull(),
            avgTurns = turns.map { it.toDouble() }.averageOrNull(),
            firstGame = valid.minOfOrNull { it.playedAt },
            lastGame = valid.maxOfOrNull { it.playedAt },
            endReasonCounts = reasons
        )
    }

    fun forPlayer(player: Player, games: List<Game>): PlayerStats {
        val valid = games.filter { it.isValid && it.sides.any { s -> s.playerId == player.id } }
            .sortedBy { it.playedAt }
        val wins = valid.count { it.winnerId == player.id }
        val started = valid.filter { it.firstPlayerId == player.id }
        val onDraw = valid.filter { it.firstPlayerId != player.id }
        val lifeOnWin = valid.filter { it.winnerId == player.id }.mapNotNull { it.sideOf(player.id)?.lifeRemaining }
        val lifeOnLoss = valid.filter { it.winnerId != player.id }.mapNotNull { it.sideOf(player.id)?.lifeRemaining }

        var best = 0
        var running = 0
        valid.forEach { game ->
            if (game.winnerId == player.id) {
                running += 1
                if (running > best) best = running
            } else {
                running = 0
            }
        }
        var current = 0
        for (game in valid.asReversed()) {
            if (game.winnerId == player.id) current += 1 else break
        }

        return PlayerStats(
            player = player,
            games = valid.size,
            wins = wins,
            losses = valid.size - wins,
            gamesStarted = started.size,
            winsWhenStarting = started.count { it.winnerId == player.id },
            gamesOnTheDraw = onDraw.size,
            winsOnTheDraw = onDraw.count { it.winnerId == player.id },
            avgLifeOnWin = lifeOnWin.averageOrNull(),
            avgLifeOnLoss = lifeOnLoss.averageOrNull(),
            currentStreak = current,
            bestStreak = best
        )
    }

    fun forDecks(decks: List<Deck>, games: List<Game>): List<DeckStats> {
        val valid = games.filter { it.isValid }
        return decks.map { deck ->
            val played = valid.filter { it.involvesDeck(deck.id) }
            val wins = played.count { game -> game.winnerSide?.deckId == deck.id }
            // On cherche le siège qui a joué CE deck, et non le premier perdant de la partie :
            // au-delà de deux joueurs, ce ne sont pas les mêmes.
            val lifeOnWin = played.mapNotNull { game ->
                game.sides.firstOrNull { it.deckId == deck.id && it.playerId == game.winnerId }?.lifeRemaining
            }
            val lifeOnLoss = played.mapNotNull { game ->
                game.sides.firstOrNull { it.deckId == deck.id && it.playerId != game.winnerId }?.lifeRemaining
            }
            DeckStats(
                deck = deck,
                games = played.size,
                wins = wins,
                losses = played.size - wins,
                avgLifeOnWin = lifeOnWin.averageOrNull(),
                avgLifeOnLoss = lifeOnLoss.averageOrNull(),
                lastPlayed = played.maxOfOrNull { it.playedAt }
            )
        }.filter { it.games > 0 }
            .sortedWith(compareByDescending<DeckStats> { it.games }.thenByDescending { it.winRate })
    }

    /** Les confrontations n'ont de sens qu'entre deux decks : les parties à plusieurs sont exclues. */
    fun matchups(games: List<Game>): List<MatchupStats> {
        val valid = games.filter { it.isValid && it.sides.size == 2 }
        return valid.groupBy { game ->
            val ids = game.sides.map { it.deckId }.sorted()
            ids[0] to ids[1]
        }.map { (pair, list) ->
            val (a, b) = pair
            MatchupStats(
                deckAId = a,
                deckBId = b,
                games = list.size,
                winsA = list.count { it.winnerSide?.deckId == a },
                winsB = list.count { it.winnerSide?.deckId == b },
                lastPlayed = list.maxOf { it.playedAt }
            )
        }.sortedWith(compareByDescending<MatchupStats> { it.games }.thenByDescending { it.lastPlayed })
    }
}

private fun List<Int>.averageOrNull(): Double? = if (isEmpty()) null else average()

@JvmName("averageOrNullDouble")
private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()
