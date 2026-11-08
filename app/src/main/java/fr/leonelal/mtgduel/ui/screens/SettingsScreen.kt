package fr.leonelal.mtgduel.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fr.leonelal.mtgduel.data.AppState
import fr.leonelal.mtgduel.ui.AppViewModel
import fr.leonelal.mtgduel.ui.SectionCard
import fr.leonelal.mtgduel.ui.StatLine
import fr.leonelal.mtgduel.ui.parties
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: AppState,
    viewModel: AppViewModel,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var nameOne by rememberSaveable { mutableStateOf(state.playerOne.name) }
    var nameTwo by rememberSaveable { mutableStateOf(state.playerTwo.name) }
    var pendingImport by remember { mutableStateOf<String?>(null) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.playerOne.name) { nameOne = state.playerOne.name }
    LaunchedEffect(state.playerTwo.name) { nameTwo = state.playerTwo.name }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val result = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(viewModel.exportToJson().toByteArray())
            } ?: error("Fichier illisible")
        }
        scope.launch {
            snackbarHostState.showSnackbar(
                if (result.isSuccess) "Sauvegarde enregistrée." else "Échec de la sauvegarde."
            )
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val raw = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        if (raw.isNullOrBlank()) {
            scope.launch { snackbarHostState.showSnackbar("Fichier illisible.") }
        } else {
            pendingImport = raw
        }
    }

    Scaffold(
        modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Réglages") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard(title = "Les joueurs") {
                OutlinedTextField(
                    value = nameOne,
                    onValueChange = {
                        nameOne = it
                        viewModel.renamePlayer(state.playerOne.id, it)
                    },
                    label = { Text("Joueur 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nameTwo,
                    onValueChange = {
                        nameTwo = it
                        viewModel.renamePlayer(state.playerTwo.id, it)
                    },
                    label = { Text("Joueur 2") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Les noms sont utilisés partout dans l'application ; les parties déjà enregistrées suivent automatiquement.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard(title = "Sauvegarde") {
                Text(
                    "Toutes les données restent sur le téléphone. Exporte un fichier JSON pour les conserver ou les transférer sur un autre appareil.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("mtg-duel-${LocalDate.now()}.json") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Exporter")
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Importer")
                    }
                }
            }

            SectionCard(title = "Contenu") {
                StatLine("Parties enregistrées", "${state.games.size}")
                StatLine("Decks", "${state.decks.size}")
            }

            SectionCard(title = "Zone sensible") {
                Text(
                    "Efface toutes les parties enregistrées. Les decks sont conservés.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(onClick = { showResetDialog = true }) {
                    Text("Effacer l'historique", color = MaterialTheme.colorScheme.error)
                }
            }

            SectionCard(title = "À propos") {
                Text("MTG Duel — suivi de parties en duel.", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Application personnelle, hors store, sans compte ni connexion internet.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    val importRaw = pendingImport
    if (importRaw != null) {
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Importer la sauvegarde") },
            text = {
                Text(
                    "« Remplacer » écrase les données actuelles par le fichier. " +
                        "« Fusionner » ajoute seulement les parties et decks absents."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val result = viewModel.importFromJson(importRaw)
                    pendingImport = null
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            result.fold(
                                onSuccess = { "Import terminé : ${parties(it)}." },
                                onFailure = { "Fichier invalide." }
                            )
                        )
                    }
                }) { Text("Remplacer") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        val result = viewModel.mergeFromJson(importRaw)
                        pendingImport = null
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                result.fold(
                                    onSuccess = { "${parties(it)} " + if (it > 1) "ajoutées." else "ajoutée." },
                                    onFailure = { "Fichier invalide." }
                                )
                            )
                        }
                    }) { Text("Fusionner") }
                    TextButton(onClick = { pendingImport = null }) { Text("Annuler") }
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Effacer tout l'historique ?") },
            text = { Text("Les ${parties(state.games.size)} seront définitivement supprimées. Pense à exporter une sauvegarde avant.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAllGames()
                    showResetDialog = false
                    scope.launch { snackbarHostState.showSnackbar("Historique effacé.") }
                }) { Text("Effacer") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Annuler") }
            }
        )
    }
}
