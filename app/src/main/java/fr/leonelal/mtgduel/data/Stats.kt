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
    val gamesWonByStarter: Int,
    val avgLoserLife: Double?,
    val avgTurns: Double?,
    val firstGame: Long?,
    val lastGame: Long?,
    val endReasonCounts: List<Pair<String, Int>>
) {
    val starterWinRate: Double get() = if (totalGames == 0) 0.0 else gamesWonByStarter.toDouble() / totalGames
}

object StatsEngine {

    fun overall(games: List<Game>): OverallStats {
        val valid = games.filter { it.isValid }
        val loserLives = valid.mapNotNull { it.loserLifeRemaining }
        val turns = valid.mapNotNull { it.turns }
        val reasons = valid.groupingBy { it.endReason }.eachCount()
            .toList().sortedByDescending { it.second }
        return OverallStats(
            totalGames = valid.size,
            gamesWonByStarter = valid.count { it.winnerId == it.firstPlayerId },
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
            val lifeOnWin = played.mapNotNull { game ->
                game.winnerSide?.takeIf { it.deckId == deck.id }?.lifeRemaining
            }
            val lifeOnLoss = played.mapNotNull { game ->
                game.loserSide?.takeIf { it.deckId == deck.id }?.lifeRemaining
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
