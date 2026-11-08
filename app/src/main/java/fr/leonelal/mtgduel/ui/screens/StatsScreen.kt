package fr.leonelal.mtgduel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.leonelal.mtgduel.data.AppState
import fr.leonelal.mtgduel.data.EndReasons
import fr.leonelal.mtgduel.data.PlayerStats
import fr.leonelal.mtgduel.data.StatsEngine
import fr.leonelal.mtgduel.ui.ColorPips
import fr.leonelal.mtgduel.ui.SectionCard
import fr.leonelal.mtgduel.ui.StatLine
import fr.leonelal.mtgduel.ui.formatAverage
import fr.leonelal.mtgduel.ui.formatPercent
import fr.leonelal.mtgduel.ui.formatShortDate
import fr.leonelal.mtgduel.ui.parties
import fr.leonelal.mtgduel.ui.victoires

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(state: AppState, contentPadding: PaddingValues) {
    val games = state.games
    val overall = remember(games) { StatsEngine.overall(games) }
    val statsOne = remember(games, state.players) { StatsEngine.forPlayer(state.playerOne, games) }
    val statsTwo = remember(games, state.players) { StatsEngine.forPlayer(state.playerTwo, games) }
    val deckStats = remember(games, state.decks) { StatsEngine.forDecks(state.decks, games) }
    val matchups = remember(games) { StatsEngine.matchups(games).take(12) }

    Scaffold(
        modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Statistiques") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("liste-stats"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (overall.totalGames == 0) {
                item {
                    SectionCard(title = "Pas encore de données") {
                        Text(
                            "Les statistiques apparaîtront dès la première partie enregistrée.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                return@LazyColumn
            }

            item {
                SectionCard(title = "Vue d'ensemble") {
                    StatLine("Parties jouées", "${overall.totalGames}", emphasis = true)
                    WinBar(
                        leftLabel = state.playerOne.name,
                        leftValue = statsOne.wins,
                        rightLabel = state.playerTwo.name,
                        rightValue = statsTwo.wins
                    )
                    overall.firstGame?.let {
                        StatLine("Première partie", formatShortDate(it))
                    }
                    overall.lastGame?.let {
                        StatLine("Dernière partie", formatShortDate(it))
                    }
                    StatLine(
                        "PV restants moyens du perdant",
                        formatAverage(overall.avgLoserLife)
                    )
                    overall.avgTurns?.let {
                        StatLine("Durée moyenne", "${formatAverage(it)} tours")
                    }
                }
            }

            item {
                SectionCard(title = "Avantage du premier joueur") {
                    StatLine(
                        "Parties gagnées par celui qui commence",
                        "${overall.gamesWonByStarter} / ${overall.totalGames}",
                        emphasis = true
                    )
                    StatLine("Soit", formatPercent(overall.starterWinRate))
                    Text(
                        text = when {
                            overall.totalGames < 10 ->
                                "Encore trop peu de parties pour conclure."
                            overall.starterWinRate > 0.6 ->
                                "Commencer semble être un vrai avantage chez vous."
                            overall.starterWinRate < 0.4 ->
                                "Chez vous, celui qui commence perd plus souvent."
                            else ->
                                "Commencer ne change pas grand-chose à vos parties."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { PlayerStatsCard(statsOne) }
            item { PlayerStatsCard(statsTwo) }

            if (deckStats.isNotEmpty()) {
                item {
                    SectionCard(title = "Par deck") {
                        deckStats.forEachIndexed { index, stat ->
                            if (index > 0) HorizontalDivider()
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stat.deck.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (stat.deck.colors.isNotEmpty()) {
                                        ColorPips(colors = stat.deck.colors, size = 9)
                                        Spacer(Modifier.size(8.dp))
                                    }
                                    Text(
                                        text = formatPercent(stat.winRate),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "${parties(stat.games)} · ${stat.wins} V / ${stat.losses} D · " +
                                        "PV restants : ${formatAverage(stat.avgLifeOnWin)} en victoire, " +
                                        "${formatAverage(stat.avgLifeOnLoss)} en défaite",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (matchups.isNotEmpty()) {
                item {
                    SectionCard(title = "Confrontations de decks") {
                        matchups.forEachIndexed { index, matchup ->
                            if (index > 0) HorizontalDivider()
                            val nameA = state.deckName(matchup.deckAId)
                            val nameB = state.deckName(matchup.deckBId)
                            val mirror = matchup.deckAId == matchup.deckBId
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (mirror) "$nameA (miroir)" else "$nameA  vs  $nameB",
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 2
                                )
                                Text(
                                    text = if (mirror) {
                                        parties(matchup.games)
                                    } else {
                                        "${parties(matchup.games)} · ${matchup.winsA} – ${matchup.winsB}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (overall.endReasonCounts.isNotEmpty()) {
                item {
                    SectionCard(title = "Fins de partie") {
                        overall.endReasonCounts.forEach { (code, count) ->
                            StatLine(EndReasons.label(code), "$count")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerStatsCard(stats: PlayerStats) {
    SectionCard(title = stats.player.name) {
        StatLine("Victoires", "${stats.wins} / ${stats.games}", emphasis = true)
        StatLine("Taux de victoire", formatPercent(stats.winRate))
        StatLine(
            "En commençant",
            if (stats.gamesStarted == 0) "–"
            else "${stats.winsWhenStarting}/${stats.gamesStarted} (${formatPercent(stats.winRateStarting)})"
        )
        StatLine(
            "En second",
            if (stats.gamesOnTheDraw == 0) "–"
            else "${stats.winsOnTheDraw}/${stats.gamesOnTheDraw} (${formatPercent(stats.winRateOnTheDraw)})"
        )
        StatLine("PV restants moyens en victoire", formatAverage(stats.avgLifeOnWin))
        StatLine("PV restants moyens en défaite", formatAverage(stats.avgLifeOnLoss))
        StatLine("Meilleure série", victoires(stats.bestStreak))
        if (stats.currentStreak > 1) {
            StatLine("Série en cours", victoires(stats.currentStreak))
        }
    }
}

@Composable
private fun WinBar(leftLabel: String, leftValue: Int, rightLabel: String, rightValue: Int) {
    val total = (leftValue + rightValue).coerceAtLeast(1)
    val leftWeight = leftValue.toFloat() / total
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$leftLabel · $leftValue", style = MaterialTheme.typography.labelMedium, maxLines = 1)
            Text("$rightValue · $rightLabel", style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
        ) {
            if (leftWeight > 0f) {
                Box(
                    modifier = Modifier
                        .weight(leftWeight.coerceAtLeast(0.001f))
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                )
            }
            if (leftWeight < 1f) {
                Box(
                    modifier = Modifier
                        .weight((1f - leftWeight).coerceAtLeast(0.001f))
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(6.dp))
                )
            }
        }
    }
}
