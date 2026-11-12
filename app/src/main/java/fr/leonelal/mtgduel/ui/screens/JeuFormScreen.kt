package fr.leonelal.mtgduel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import fr.leonelal.mtgduel.data.AppState
import fr.leonelal.mtgduel.data.CustomGame
import fr.leonelal.mtgduel.data.ScoringMode
import fr.leonelal.mtgduel.ui.AppViewModel
import fr.leonelal.mtgduel.ui.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JeuFormScreen(
    state: AppState,
    viewModel: AppViewModel,
    gameId: String?,
    onDone: () -> Unit,
    onSupprime: () -> Unit
) {
    val existant = remember(gameId, state.allCustomGames) { state.customGame(gameId) }

    var nom by rememberSaveable(gameId) { mutableStateOf(existant?.name ?: "") }
    var comptage by rememberSaveable(gameId) {
        mutableStateOf(existant?.scoring?.name ?: ScoringMode.MANCHES.name)
    }
    var note by rememberSaveable(gameId) { mutableStateOf(existant?.note ?: "") }
    var erreurNom by rememberSaveable { mutableStateOf(false) }
    var confirmerSuppression by rememberSaveable { mutableStateOf(false) }

    val nbParties = remember(state.allCustomMatches, gameId) {
        if (gameId == null) 0 else state.matchesOf(gameId).size
    }

    fun enregistrer() {
        if (nom.isBlank()) {
            erreurNom = true
            return
        }
        viewModel.saveCustomGame(
            CustomGame(
                id = existant?.id ?: java.util.UUID.randomUUID().toString(),
                name = nom.trim(),
                scoring = ScoringMode.valueOf(comptage),
                note = note.trim(),
                createdAt = existant?.createdAt ?: System.currentTimeMillis()
            )
        )
        onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existant == null) "Nouveau jeu" else "Modifier le jeu") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (existant != null) {
                        IconButton(onClick = { confirmerSuppression = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard(title = "Le jeu") {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it; erreurNom = false },
                    label = { Text("Nom du jeu") },
                    placeholder = { Text("Skull King, Mixmo, Monopoly Deal…") },
                    singleLine = true,
                    isError = erreurNom,
                    supportingText = if (erreurNom) {
                        { Text("Donne un nom au jeu.") }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nom-du-jeu")
                )
            }

            SectionCard(title = "Comment compte-t-on les points ?") {
                ScoringMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = comptage == mode.name,
                                onClick = { comptage = mode.name }
                            )
                            .testTag("comptage-${mode.name}")
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        RadioButton(
                            selected = comptage == mode.name,
                            onClick = { comptage = mode.name }
                        )
                        Spacer(Modifier.size(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mode.libelle, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                mode.explication,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (existant != null && nbParties > 0) {
                    Text(
                        "Changer ce réglage recalcule les vainqueurs des $nbParties partie(s) déjà enregistrées.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            SectionCard(title = "Divers") {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (règle maison, variante…)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = { enregistrer() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (existant == null) "Créer le jeu" else "Enregistrer")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmerSuppression && existant != null) {
        AlertDialog(
            onDismissRequest = { confirmerSuppression = false },
            title = { Text("Supprimer « ${existant.name} » ?") },
            text = {
                Text(
                    if (nbParties == 0) "Ce jeu n'a aucune partie enregistrée."
                    else "Les $nbParties partie(s) de ce jeu seront supprimées avec lui."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCustomGame(existant.id)
                    confirmerSuppression = false
                    onSupprime()
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { confirmerSuppression = false }) { Text("Annuler") }
            }
        )
    }
}
