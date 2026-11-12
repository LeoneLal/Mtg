package fr.leonelal.mtgduel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import fr.leonelal.mtgduel.data.CustomGame
import fr.leonelal.mtgduel.data.CustomMatch
import fr.leonelal.mtgduel.data.JeuxStats
import fr.leonelal.mtgduel.ui.SectionCard
import fr.leonelal.mtgduel.ui.clickableIf
import fr.leonelal.mtgduel.ui.formatAverage
import fr.leonelal.mtgduel.ui.formatDayLabel
import fr.leonelal.mtgduel.ui.formatPercent
import fr.leonelal.mtgduel.ui.formatTime
import fr.leonelal.mtgduel.ui.parties
import fr.leonelal.mtgduel.ui.toLocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JeuDetailScreen(
    state: AppState,
    gameId: String?,
    onRetour: () -> Unit,
    onModifierJeu: (String) -> Unit,
    onNouvellePartie: (String) -> Unit,
    onOuvrirPartie: (String, String) -> Unit
) {
    val jeu = remember(gameId, state.allCustomGames) { state.customGame(gameId) }
    if (jeu == null || jeu.deleted) {
        JeuIntrouvable(onRetour)
        return
    }

    val partiesDuJeu = remember(state.allCustomMatches, jeu.id) { state.matchesOf(jeu.id) }
    val classement = remember(partiesDuJeu, jeu) { JeuxStats.classement(jeu, partiesDuJeu) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(jeu.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onRetour) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { onModifierJeu(jeu.id) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Modifier le jeu")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.testTag("fab-nouvelle-partie-jeu"),
                onClick = { onNouvellePartie(jeu.id) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nouvelle partie") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("liste-parties-jeu"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SectionCard(title = "Classement") {
                    if (classement.isEmpty()) {
                        Text(
                            "Enregistre une première partie : le classement se construira tout seul.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            jeu.scoring.libelle + " · " + parties(partiesDuJeu.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        classement.forEachIndexed { index, ligne ->
                            if (index > 0) HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        ligne.nom,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        buildString {
                                            append("${ligne.victoires} V / ${ligne.defaites} D")
                                            if (ligne.egalites > 0) append(" / ${ligne.egalites} nul")
                                            append(" · ${jeu.scoring.libelleScore.lowercase()} en moyenne : ")
                                            append(formatAverage(ligne.scoreMoyen))
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    formatPercent(ligne.tauxVictoire),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            if (partiesDuJeu.isEmpty()) {
                item {
                    Text(
                        "Aucune partie enregistrée.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            var dernierJour: java.time.LocalDate? = null
            partiesDuJeu.forEach { partie ->
                val jour = partie.playedAt.toLocalDate()
                if (jour != dernierJour) {
                    dernierJour = jour
                    item(key = "jour-${partie.id}") {
                        Text(
                            text = formatDayLabel(partie.playedAt),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                        )
                    }
                }
                item(key = partie.id) {
                    CartePartie(
                        jeu = jeu,
                        partie = partie,
                        onClick = { onOuvrirPartie(jeu.id, partie.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CartePartie(jeu: CustomGame, partie: CustomMatch, onClick: () -> Unit) {
    val vainqueurs = remember(partie, jeu.scoring) { partie.vainqueurs(jeu.scoring).map { it.name } }
    val egalite = vainqueurs.size > 1
    val ordonnes = remember(partie, jeu.scoring) {
        if (jeu.scoring.plusHautGagne) partie.participants.sortedByDescending { it.score }
        else partie.participants.sortedBy { it.score }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickableIf(true, onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatTime(partie.playedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (egalite) "Égalité" else "${partie.participants.size} joueurs",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (egalite) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))

            ordonnes.forEach { participant ->
                val gagnant = participant.name in vainqueurs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .background(
                            color = if (gagnant && !egalite) {
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                        if (gagnant && !egalite) {
                            Icon(
                                Icons.Filled.EmojiEvents,
                                contentDescription = "Vainqueur",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = participant.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (gagnant) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${participant.score}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (gagnant) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (partie.notes.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    partie.notes,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JeuIntrouvable(onRetour: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jeu introuvable") },
                navigationIcon = {
                    IconButton(onClick = onRetour) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Text(
            "Ce jeu a été supprimé.",
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        )
    }
}
