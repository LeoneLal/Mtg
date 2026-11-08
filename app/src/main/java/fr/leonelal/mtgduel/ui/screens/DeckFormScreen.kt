package fr.leonelal.mtgduel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import fr.leonelal.mtgduel.data.Deck
import fr.leonelal.mtgduel.data.MtgColors
import fr.leonelal.mtgduel.ui.AppViewModel
import fr.leonelal.mtgduel.ui.ColorPips
import fr.leonelal.mtgduel.ui.SectionCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeckFormScreen(
    state: AppState,
    viewModel: AppViewModel,
    deckId: String?,
    onDone: () -> Unit
) {
    val existing = remember(deckId, state.decks) { state.decks.firstOrNull { it.id == deckId } }
    val p1 = state.playerOne
    val p2 = state.playerTwo

    var name by rememberSaveable(deckId) { mutableStateOf(existing?.name ?: "") }
    var ownerId by rememberSaveable(deckId) { mutableStateOf<String?>(existing?.ownerId ?: p1.id) }
    var colors by rememberSaveable(deckId) { mutableStateOf(existing?.colors?.joinToString(",") ?: "") }
    var note by rememberSaveable(deckId) { mutableStateOf(existing?.note ?: "") }
    var archived by rememberSaveable(deckId) { mutableStateOf(existing?.archived ?: false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    var showNameError by rememberSaveable { mutableStateOf(false) }

    val selectedColors = colors.split(",").filter { it.isNotBlank() }
    val isUsed = existing != null && viewModel.deckIsUsed(existing.id)

    fun toggleColor(code: String) {
        val set = selectedColors.toMutableList()
        if (set.contains(code)) set.remove(code) else set.add(code)
        val ordered = MtgColors.all.map { it.first }.filter { set.contains(it) }
        colors = ordered.joinToString(",")
    }

    fun save() {
        if (name.isBlank()) {
            showNameError = true
            return
        }
        viewModel.saveDeck(
            Deck(
                id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                name = name.trim(),
                ownerId = ownerId,
                colors = selectedColors,
                note = note.trim(),
                archived = archived,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
        )
        onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Nouveau deck" else "Modifier le deck") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (existing != null) {
                        IconButton(onClick = { showDelete = true }) {
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
            SectionCard(title = "Identité du deck") {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; showNameError = false },
                    label = { Text("Nom du deck") },
                    singleLine = true,
                    isError = showNameError,
                    supportingText = if (showNameError) {
                        { Text("Donne un nom au deck.") }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nom-du-deck")
                )

                Text("À qui appartient ce deck ?", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(p1.id to p1.name, p2.id to p2.name, null to "Commun").forEach { (id, label) ->
                        FilterChip(
                            selected = ownerId == id,
                            onClick = { ownerId = id },
                            label = { Text(label, maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            SectionCard(title = "Couleurs") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MtgColors.all.forEach { (code, label, _) ->
                        FilterChip(
                            selected = selectedColors.contains(code),
                            onClick = { toggleColor(code) },
                            label = { Text(label) },
                            leadingIcon = { ColorPips(colors = listOf(code), size = 12) }
                        )
                    }
                }
                if (selectedColors.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Identité : ", style = MaterialTheme.typography.bodySmall)
                        ColorPips(colors = selectedColors, size = 12)
                    }
                }
            }

            SectionCard(title = "Divers") {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (commandant, archétype…)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Archiver ce deck", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Il reste dans l'historique et les stats mais n'est plus proposé pour les nouvelles parties.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.size(10.dp))
                    Switch(checked = archived, onCheckedChange = { archived = it })
                }
            }

            Button(
                onClick = { save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (existing == null) "Créer le deck" else "Enregistrer")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(if (isUsed) "Deck utilisé" else "Supprimer ce deck ?") },
            text = {
                Text(
                    if (isUsed) {
                        "Ce deck apparaît dans des parties enregistrées. Le supprimer rendrait ces parties incomplètes : préfère l'archiver."
                    } else {
                        "Ce deck n'est utilisé dans aucune partie, il peut être supprimé."
                    }
                )
            },
            confirmButton = {
                if (isUsed) {
                    TextButton(onClick = {
                        archived = true
                        showDelete = false
                    }) { Text("Archiver") }
                } else {
                    TextButton(onClick = {
                        viewModel.deleteDeck(existing.id)
                        showDelete = false
                        onDone()
                    }) { Text("Supprimer") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Annuler") }
            }
        )
    }
}
