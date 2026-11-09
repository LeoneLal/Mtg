package fr.leonelal.mtgduel.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import fr.leonelal.mtgduel.data.Deck
import fr.leonelal.mtgduel.ui.ColorPips
import fr.leonelal.mtgduel.ui.clickableIf
import fr.leonelal.mtgduel.ui.formatPercent
import fr.leonelal.mtgduel.ui.parties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecksScreen(
    state: AppState,
    contentPadding: PaddingValues,
    onNewDeck: () -> Unit,
    onOpenDeck: (String) -> Unit
) {
    val grouped = remember(state.decks, state.players) {
        val sorted = state.decks.sortedWith(
            compareBy<Deck> { it.archived }.thenBy { it.name.lowercase() }
        )
        listOf(
            Triple("p1", state.playerOne.name, sorted.filter { it.ownerId == state.playerOne.id }),
            Triple("p2", state.playerTwo.name, sorted.filter { it.ownerId == state.playerTwo.id }),
            Triple("commun", "Decks communs", sorted.filter { it.ownerId == null })
        ).filter { it.third.isNotEmpty() }
    }

    Scaffold(
        modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Decks") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.testTag("fab-nouveau-deck"),
                onClick = onNewDeck,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nouveau deck") }
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
            if (state.decks.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Aucun deck", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Crée un deck par liste que vous jouez : ils seront proposés à chaque nouvelle partie et les statistiques seront calculées par deck.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            grouped.forEach { (groupeId, ownerName, decks) ->
                item(key = "header-$groupeId") {
                    Text(
                        text = ownerName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
                decks.forEach { deck ->
                    item(key = deck.id) {
                        DeckCard(state = state, deck = deck, onClick = { onOpenDeck(deck.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckCard(state: AppState, deck: Deck, onClick: () -> Unit) {
    val games = remember(state.games, deck.id) { state.games.filter { it.involvesDeck(deck.id) } }
    val wins = remember(games, deck.id) { games.count { it.winnerSide?.deckId == deck.id } }
    val losses = games.size - wins

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickableIf(true, onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (deck.archived) 0.18f else 0.35f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = deck.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (deck.colors.isNotEmpty()) {
                        Spacer(Modifier.size(8.dp))
                        ColorPips(colors = deck.colors, size = 10)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (games.isEmpty()) {
                        "Jamais joué"
                    } else {
                        "${parties(games.size)} · $wins V / $losses D"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (deck.archived) {
                    Text(
                        "Archivé",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (deck.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = deck.note,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (games.isNotEmpty()) {
                Spacer(Modifier.size(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatPercent(wins.toDouble() / games.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "victoires",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
