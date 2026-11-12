package fr.leonelal.mtgduel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.leonelal.mtgduel.data.AppState
import fr.leonelal.mtgduel.data.CustomGame
import fr.leonelal.mtgduel.ui.clickableIf
import fr.leonelal.mtgduel.ui.formatShortDate
import fr.leonelal.mtgduel.ui.parties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JeuxScreen(
    state: AppState,
    contentPadding: PaddingValues,
    onNouveauJeu: () -> Unit,
    onOuvrirJeu: (String) -> Unit
) {
    val jeux = state.customGames

    Scaffold(
        modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Autres jeux") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.testTag("fab-nouveau-jeu"),
                onClick = onNouveauJeu,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nouveau jeu") }
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
            if (jeux.isEmpty()) {
                item { AucunJeu() }
            }
            items(jeux, key = { it.id }) { jeu ->
                CarteJeu(
                    state = state,
                    jeu = jeu,
                    onClick = { onOuvrirJeu(jeu.id) }
                )
            }
        }
    }
}

@Composable
private fun AucunJeu() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Aucun jeu pour l'instant", style = MaterialTheme.typography.titleMedium)
            Text(
                "Ajoutez ici tous vos autres jeux — Skull King, Mixmo, Monopoly Deal… " +
                    "Vous indiquez une fois comment on compte les points, puis vous " +
                    "enregistrez chaque partie avec le nombre de joueurs que vous voulez.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Magic garde sa propre section, avec ses decks et ses points de vie.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CarteJeu(state: AppState, jeu: CustomGame, onClick: () -> Unit) {
    val partiesDuJeu = remember(state.allCustomMatches, jeu.id) { state.matchesOf(jeu.id) }
    val derniere = partiesDuJeu.firstOrNull()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickableIf(true, onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = jeu.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = jeu.scoring.libelle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (derniere == null) {
                        "Aucune partie enregistrée"
                    } else {
                        parties(partiesDuJeu.size) + " · dernière le " + formatShortDate(derniere.playedAt)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (jeu.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = jeu.note,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.size(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
