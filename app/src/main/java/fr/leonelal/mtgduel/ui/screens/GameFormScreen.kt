package fr.leonelal.mtgduel.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.leonelal.mtgduel.data.AppState
import fr.leonelal.mtgduel.data.Deck
import fr.leonelal.mtgduel.data.EndReasons
import fr.leonelal.mtgduel.data.Formats
import fr.leonelal.mtgduel.data.Game
import fr.leonelal.mtgduel.data.GameSide
import fr.leonelal.mtgduel.ui.AppViewModel
import fr.leonelal.mtgduel.ui.ColorPips
import fr.leonelal.mtgduel.ui.PickerField
import fr.leonelal.mtgduel.ui.SectionCard
import fr.leonelal.mtgduel.ui.formatFullDate
import fr.leonelal.mtgduel.ui.formatTime
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GameFormScreen(
    state: AppState,
    viewModel: AppViewModel,
    gameId: String?,
    onDone: () -> Unit,
    onManageDecks: () -> Unit
) {
    val context = LocalContext.current
    val existing = remember(gameId, state.games) { state.games.firstOrNull { it.id == gameId } }
    val p1 = state.playerOne
    val p2 = state.playerTwo
    val lastGame = remember(state.games) { state.gamesNewestFirst.firstOrNull() }

    fun defaultDeck(playerId: String): String? {
        val previous = lastGame?.deckIdOf(playerId)
        return previous?.takeIf { id -> state.decks.any { it.id == id && !it.archived } }
    }

    var playedAt by rememberSaveable(gameId) {
        mutableStateOf(existing?.playedAt ?: System.currentTimeMillis())
    }
    var format by rememberSaveable(gameId) {
        mutableStateOf(existing?.format ?: lastGame?.format ?: Formats.COMMANDER)
    }
    var startingLife by rememberSaveable(gameId) {
        mutableStateOf(existing?.startingLife ?: lastGame?.startingLife ?: 40)
    }
    var firstPlayerId by rememberSaveable(gameId) {
        mutableStateOf(existing?.firstPlayerId ?: p1.id)
    }
    var deckOne by rememberSaveable(gameId) {
        mutableStateOf(existing?.deckIdOf(p1.id) ?: defaultDeck(p1.id))
    }
    var deckTwo by rememberSaveable(gameId) {
        mutableStateOf(existing?.deckIdOf(p2.id) ?: defaultDeck(p2.id))
    }
    var winnerId by rememberSaveable(gameId) { mutableStateOf(existing?.winnerId) }
    var lifeOne by rememberSaveable(gameId) {
        mutableStateOf(existing?.sideOf(p1.id)?.lifeRemaining?.toString() ?: "")
    }
    var lifeTwo by rememberSaveable(gameId) {
        mutableStateOf(existing?.sideOf(p2.id)?.lifeRemaining?.toString() ?: "")
    }
    var lifeOneTouched by rememberSaveable(gameId) { mutableStateOf(existing != null) }
    var lifeTwoTouched by rememberSaveable(gameId) { mutableStateOf(existing != null) }
    var endReason by rememberSaveable(gameId) {
        mutableStateOf(existing?.endReason ?: EndReasons.DAMAGE)
    }
    var turnsText by rememberSaveable(gameId) {
        mutableStateOf(existing?.turns?.toString() ?: "")
    }
    var notes by rememberSaveable(gameId) { mutableStateOf(existing?.notes ?: "") }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showErrors by rememberSaveable { mutableStateOf(false) }

    fun refreshLifeDefaults(winner: String?, life: Int) {
        if (winner == null) return
        if (!lifeOneTouched) lifeOne = (if (winner == p1.id) life else 0).toString()
        if (!lifeTwoTouched) lifeTwo = (if (winner == p2.id) life else 0).toString()
    }

    val decksForOne = remember(state.decks, deckOne) {
        val list = state.decksFor(p1.id)
        val current = state.deck(deckOne)
        if (current != null && list.none { it.id == current.id }) list + current else list
    }
    val decksForTwo = remember(state.decks, deckTwo) {
        val list = state.decksFor(p2.id)
        val current = state.deck(deckTwo)
        if (current != null && list.none { it.id == current.id }) list + current else list
    }

    val deckOneMissing = deckOne == null
    val deckTwoMissing = deckTwo == null
    val winnerMissing = winnerId == null
    val canSave = !deckOneMissing && !deckTwoMissing && !winnerMissing

    fun save() {
        if (!canSave) {
            showErrors = true
            return
        }
        val game = Game(
            id = existing?.id ?: java.util.UUID.randomUUID().toString(),
            playedAt = playedAt,
            format = format,
            startingLife = startingLife,
            firstPlayerId = firstPlayerId,
            winnerId = winnerId!!,
            sides = listOf(
                GameSide(p1.id, deckOne!!, lifeOne.toIntOrNull() ?: 0),
                GameSide(p2.id, deckTwo!!, lifeTwo.toIntOrNull() ?: 0)
            ),
            endReason = endReason,
            turns = turnsText.toIntOrNull(),
            notes = notes.trim()
        )
        viewModel.saveGame(game)
        onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Nouvelle partie" else "Modifier la partie") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (existing != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
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

            if (state.decks.isEmpty()) {
                SectionCard(title = "Aucun deck enregistré") {
                    Text(
                        "Ajoute d'abord au moins un deck pour chacun de vous.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = onManageDecks) { Text("Gérer les decks") }
                }
            }

            // ----------------------------------------------------------- date
            SectionCard(title = "Quand ?") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = playedAt }
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    playedAt = Calendar.getInstance().apply {
                                        timeInMillis = playedAt
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, day)
                                    }.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(formatFullDate(playedAt)) }

                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = playedAt }
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    playedAt = Calendar.getInstance().apply {
                                        timeInMillis = playedAt
                                        set(Calendar.HOUR_OF_DAY, hour)
                                        set(Calendar.MINUTE, minute)
                                        set(Calendar.SECOND, 0)
                                    }.timeInMillis
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                true
                            ).show()
                        }
                    ) { Text(formatTime(playedAt)) }
                }
            }

            // --------------------------------------------------------- format
            SectionCard(title = "Format") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Formats.defaults.forEach { (name, life) ->
                        FilterChip(
                            selected = format == name,
                            onClick = {
                                format = name
                                startingLife = life
                                refreshLifeDefaults(winnerId, life)
                            },
                            label = { Text(name) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "PV de départ",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Stepper(
                        value = startingLife.toString(),
                        onValueChange = { raw ->
                            startingLife = raw.toIntOrNull()?.coerceIn(1, 999) ?: startingLife
                            refreshLifeDefaults(winnerId, startingLife)
                        },
                        step = 1
                    )
                }
            }

            // ---------------------------------------------------- premier tour
            SectionCard(title = "Qui a commencé ?") {
                ChoiceRow(
                    options = listOf(p1.id to p1.name, p2.id to p2.name),
                    selectedId = firstPlayerId,
                    onSelect = { firstPlayerId = it },
                    tagPrefix = "premier"
                )
            }

            // ---------------------------------------------------------- decks
            SectionCard(title = "Decks joués") {
                DeckPicker(
                    label = p1.name,
                    decks = decksForOne,
                    selectedId = deckOne,
                    onSelect = { deckOne = it },
                    isError = showErrors && deckOneMissing,
                    testTag = "deck-joueur-1"
                )
                DeckPicker(
                    label = p2.name,
                    decks = decksForTwo,
                    selectedId = deckTwo,
                    onSelect = { deckTwo = it },
                    isError = showErrors && deckTwoMissing,
                    testTag = "deck-joueur-2"
                )
                TextButton(onClick = onManageDecks) { Text("Ajouter / modifier un deck") }
            }

            // ------------------------------------------------------- vainqueur
            SectionCard(title = "Qui a gagné ?") {
                ChoiceRow(
                    options = listOf(p1.id to p1.name, p2.id to p2.name),
                    selectedId = winnerId,
                    onSelect = {
                        winnerId = it
                        refreshLifeDefaults(it, startingLife)
                    },
                    tagPrefix = "vainqueur"
                )
                if (showErrors && winnerMissing) {
                    Text(
                        "Sélectionne le vainqueur.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // ------------------------------------------------------------- PV
            SectionCard(title = "Points de vie restants") {
                LifeRow(
                    name = p1.name,
                    isLoser = winnerId == p2.id,
                    value = lifeOne,
                    onValueChange = { lifeOne = it; lifeOneTouched = true },
                    testTag = "pv-joueur-1"
                )
                LifeRow(
                    name = p2.name,
                    isLoser = winnerId == p1.id,
                    value = lifeTwo,
                    onValueChange = { lifeTwo = it; lifeTwoTouched = true },
                    testTag = "pv-joueur-2"
                )
                Text(
                    text = "Les PV du perdant au moment de la défaite sont la donnée suivie ; ceux du vainqueur permettent de mesurer l'écart.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // --------------------------------------------------------- détails
            SectionCard(title = "Détails") {
                Text("Comment la partie s'est terminée", style = MaterialTheme.typography.bodyMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EndReasons.all.forEach { (code, label) ->
                        FilterChip(
                            selected = endReason == code,
                            onClick = { endReason = code },
                            label = { Text(label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = turnsText,
                    onValueChange = { raw -> turnsText = raw.filter { it.isDigit() }.take(3) },
                    label = { Text("Nombre de tours (optionnel)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optionnel)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = { save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (existing == null) "Enregistrer la partie" else "Enregistrer les modifications")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDeleteDialog && existing != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer cette partie ?") },
            text = { Text("Cette partie sera définitivement retirée de l'historique.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGame(existing.id)
                    showDeleteDialog = false
                    onDone()
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun DeckPicker(
    label: String,
    decks: List<Deck>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    isError: Boolean,
    testTag: String
) {
    val selected = decks.firstOrNull { it.id == selectedId }
    PickerField(
        label = label,
        options = decks,
        selected = selected,
        optionLabel = { it.name + if (it.archived) " (archivé)" else "" },
        onSelect = { onSelect(it.id) },
        isError = isError,
        placeholder = "Choisir un deck…",
        optionContent = { deck ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(deck.name + if (deck.archived) " (archivé)" else "")
                if (deck.colors.isNotEmpty()) {
                    Spacer(Modifier.size(8.dp))
                    ColorPips(colors = deck.colors, size = 10)
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

@Composable
private fun ChoiceRow(
    options: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    tagPrefix: String = ""
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEach { (id, label) ->
            FilterChip(
                selected = selectedId == id,
                onClick = { onSelect(id) },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("$tagPrefix-$id")
            )
        }
    }
}

@Composable
private fun LifeRow(
    name: String,
    isLoser: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            if (isLoser) {
                Text(
                    "PV à la défaite",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        Stepper(value = value, onValueChange = onValueChange, step = 1, testTag = testTag)
    }
}

@Composable
private fun Stepper(
    value: String,
    onValueChange: (String) -> Unit,
    step: Int = 1,
    testTag: String? = null
) {
    val current = value.toIntOrNull() ?: 0
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onValueChange((current - step).toString()) }) {
            Icon(Icons.Filled.Remove, contentDescription = "Diminuer")
        }
        OutlinedTextField(
            value = value,
            onValueChange = { raw ->
                val cleaned = buildString {
                    raw.forEachIndexed { index, c ->
                        if (c.isDigit() || (c == '-' && index == 0)) append(c)
                    }
                }.take(4)
                onValueChange(cleaned)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            modifier = Modifier
                .width(86.dp)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        )
        IconButton(onClick = { onValueChange((current + step).toString()) }) {
            Icon(Icons.Filled.Add, contentDescription = "Augmenter")
        }
    }
}
