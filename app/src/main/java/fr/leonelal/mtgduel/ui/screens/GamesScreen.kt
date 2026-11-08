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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.leonelal.mtgduel.data.AppState
import fr.leonelal.mtgduel.data.EndReasons
import fr.leonelal.mtgduel.data.Game
import fr.leonelal.mtgduel.ui.ColorPips
import fr.leonelal.mtgduel.ui.clickableIf
import fr.leonelal.mtgduel.ui.formatDayLabel
import fr.leonelal.mtgduel.ui.parties
import fr.leonelal.mtgduel.ui.pluriel
import fr.leonelal.mtgduel.ui.formatTime
import fr.leonelal.mtgduel.ui.toLocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(
    state: AppState,
    contentPadding: PaddingValues,
    onNewGame: () -> Unit,
    onOpenGame: (String) -> Unit,
    onOpenDecks: () -> Unit
) {
    var deckFilter by remember { mutableStateOf<String?>(null) }

    val allGames = state.gamesNewestFirst
    val games = remember(allGames, deckFilter) {
        if (deckFilter == null) allGames else allGames.filter { it.involvesDeck(deckFilter!!) }
    }

    Scaffold(
        modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Historique des parties") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.testTag("fab-nouvelle-partie"),
                onClick = onNewGame,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nouvelle partie") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ScoreBoard(state = state, games = allGames)
            }

            if (state.decks.isNotEmpty()) {
                item {
                    DeckFilterRow(
                        state = state,
                        selected = deckFilter,
                        onSelect = { deckFilter = it }
                    )
                }
            }

            if (allGames.isEmpty()) {
                item {
                    EmptyGames(
                        hasDecks = state.decks.isNotEmpty(),
                        onNewGame = onNewGame,
                        onOpenDecks = onOpenDecks
                    )
                }
            } else if (games.isEmpty()) {
                item {
                    Text(
                        text = "Aucune partie avec ce deck.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }

            var lastDay: java.time.LocalDate? = null
            games.forEach { game ->
                val day = game.playedAt.toLocalDate()
                if (day != lastDay) {
                    lastDay = day
                    item(key = "header-${game.id}") {
                        Text(
                            text = formatDayLabel(game.playedAt),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                        )
                    }
                }
                item(key = game.id) {
                    GameCard(
                        state = state,
                        game = game,
                        onClick = { onOpenGame(game.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreBoard(state: AppState, games: List<Game>) {
    val p1 = state.playerOne
    val p2 = state.playerTwo
    val winsP1 = games.count { it.winnerId == p1.id }
    val winsP2 = games.count { it.winnerId == p2.id }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (games.isEmpty()) "Aucune partie enregistrée"
                else parties(games.size) + if (games.size > 1) " jouées" else " jouée",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScoreSide(name = p1.name, score = winsP1, leading = winsP1 > winsP2, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .size(width = 2.dp, height = 28.dp)
                        .background(
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.35f),
                            RoundedCornerShape(1.dp)
                        )
                )
                ScoreSide(name = p2.name, score = winsP2, leading = winsP2 > winsP1, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ScoreSide(name: String, score: Int, leading: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading) {
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = "En tête",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(4.dp))
            }
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeckFilterRow(
    state: AppState,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    val decks = remember(state.decks) { state.decks.sortedBy { it.name.lowercase() } }
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("Tous les decks") }
            )
        }
        items(decks, key = { it.id }) { deck ->
            FilterChip(
                selected = selected == deck.id,
                onClick = { onSelect(if (selected == deck.id) null else deck.id) },
                label = { Text(deck.name) }
            )
        }
    }
}

@Composable
private fun GameCard(state: AppState, game: Game, onClick: () -> Unit) {
    val winner = game.winnerSide
    val loser = game.loserSide

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickableIf(true, onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(game.playedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${game.format} · ${game.startingLife} PV",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))

            if (winner != null) {
                PlayerLine(
                    state = state,
                    playerId = winner.playerId,
                    deckId = winner.deckId,
                    life = winner.lifeRemaining,
                    isWinner = true,
                    startedFirst = game.firstPlayerId == winner.playerId
                )
            }
            if (loser != null) {
                Spacer(Modifier.height(6.dp))
                PlayerLine(
                    state = state,
                    playerId = loser.playerId,
                    deckId = loser.deckId,
                    life = loser.lifeRemaining,
                    isWinner = false,
                    startedFirst = game.firstPlayerId == loser.playerId
                )
            }

            Spacer(Modifier.height(10.dp))
            val details = buildList {
                add("Fin : ${EndReasons.label(game.endReason)}")
                game.turns?.let { add("$it tours") }
            }
            Text(
                text = details.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (game.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = game.notes,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PlayerLine(
    state: AppState,
    playerId: String,
    deckId: String,
    life: Int,
    isWinner: Boolean,
    startedFirst: Boolean
) {
    val deck = state.deck(deckId)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isWinner) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            if (isWinner) {
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = "Vainqueur",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    Icons.Filled.SportsEsports,
                    contentDescription = "Perdant",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.size(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.playerName(playerId),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isWinner) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (startedFirst) {
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = "a commencé",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = deck?.name ?: "Deck supprimé",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (deck != null && deck.colors.isNotEmpty()) {
                    Spacer(Modifier.size(6.dp))
                    ColorPips(colors = deck.colors, size = 9)
                }
            }
        }
        Spacer(Modifier.size(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$life",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isWinner) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
            )
            Text(
                text = "PV",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyGames(hasDecks: Boolean, onNewGame: () -> Unit, onOpenDecks: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Première partie à enregistrer", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (hasDecks) {
                    "Appuie sur « Nouvelle partie » après chaque duel : qui a commencé, les decks joués, le vainqueur et les PV restants du perdant."
                } else {
                    "Commence par créer vos decks dans l'onglet « Decks », puis enregistre vos parties ici."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.material3.FilledTonalButton(
                onClick = if (hasDecks) onNewGame else onOpenDecks
            ) {
                Text(if (hasDecks) "Enregistrer une partie" else "Créer un deck")
            }
        }
    }
}
