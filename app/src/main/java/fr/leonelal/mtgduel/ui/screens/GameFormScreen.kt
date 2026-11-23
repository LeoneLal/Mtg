package fr.leonelal.mtgduel.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import fr.leonelal.mtgduel.data.Player
import fr.leonelal.mtgduel.ui.AppViewModel
import fr.leonelal.mtgduel.ui.ColorPips
import fr.leonelal.mtgduel.ui.PickerField
import fr.leonelal.mtgduel.ui.SectionCard
import fr.leonelal.mtgduel.ui.formatFullDate
import fr.leonelal.mtgduel.ui.formatTime
import java.util.Calendar

private const val JOUEURS_MIN = 2
private const val JOUEURS_MAX = 8

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GameFormScreen(
    state: AppState,
    viewModel: AppViewModel,
    gameId: String?,
    onDone: () -> Unit,
    onManageDecks: () -> Unit,
    onManagePlayers: () -> Unit
) {
    val context = LocalContext.current
    val existing = remember(gameId, state.allGames) { state.allGames.firstOrNull { it.id == gameId } }
    val roster = state.players
    val lastGame = remember(state.allGames) { state.gamesNewestFirst.firstOrNull() }

    /** Sièges de départ : la partie précédente si elle existe, sinon le duel habituel. */
    val siegesInitiaux = remember(existing, lastGame, roster) {
        val ids = when {
            existing != null -> existing.sides.map { it.playerId }
            lastGame != null -> lastGame.sides.map { it.playerId }.filter { id -> roster.any { it.id == id } }
            else -> emptyList()
        }
        if (ids.size >= JOUEURS_MIN) ids
        else roster.take(JOUEURS_MIN).map { it.id }.ifEmpty { listOf(state.playerOne.id, state.playerTwo.id) }
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
    var joueurs by rememberSaveable(gameId) { mutableStateOf(ArrayList(siegesInitiaux)) }
    var decks by rememberSaveable(gameId) {
        mutableStateOf(
            ArrayList(
                siegesInitiaux.map { playerId ->
                    val choisi = existing?.deckIdOf(playerId) ?: lastGame?.deckIdOf(playerId)
                    choisi?.takeIf { id -> state.decks.any { it.id == id } } ?: ""
                }
            )
        )
    }
    var pointsDeVie by rememberSaveable(gameId) {
        mutableStateOf(
            ArrayList(
                siegesInitiaux.map { playerId ->
                    existing?.sideOf(playerId)?.lifeRemaining?.toString() ?: ""
                }
            )
        )
    }
    var firstPlayerId by rememberSaveable(gameId) {
        mutableStateOf(existing?.firstPlayerId ?: siegesInitiaux.firstOrNull().orEmpty())
    }
    var winnerId by rememberSaveable(gameId) { mutableStateOf(existing?.winnerId) }
    var endReason by rememberSaveable(gameId) {
        mutableStateOf(existing?.endReason ?: EndReasons.DAMAGE)
    }
    var turnsText by rememberSaveable(gameId) { mutableStateOf(existing?.turns?.toString() ?: "") }
    var notes by rememberSaveable(gameId) { mutableStateOf(existing?.notes ?: "") }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var erreur by rememberSaveable(gameId) { mutableStateOf("") }

    /** Après le choix du vainqueur, les PV se pré-remplissent : PV de départ pour lui, 0 pour les autres. */
    fun prremplirPointsDeVie(vainqueur: String?, vie: Int) {
        if (vainqueur == null || existing != null) return
        pointsDeVie = ArrayList(
            joueurs.map { playerId -> (if (playerId == vainqueur) vie else 0).toString() }
        )
    }

    fun changerNombreDeJoueurs(cible: Int) {
        val borne = cible.coerceIn(JOUEURS_MIN, JOUEURS_MAX.coerceAtMost(maxOf(roster.size, JOUEURS_MIN)))
        val nouveauxJoueurs = ArrayList(joueurs)
        val nouveauxDecks = ArrayList(decks)
        val nouveauxPv = ArrayList(pointsDeVie)
        while (nouveauxJoueurs.size < borne) {
            val libre = roster.firstOrNull { joueur -> nouveauxJoueurs.none { it == joueur.id } }
            nouveauxJoueurs.add(libre?.id ?: "")
            nouveauxDecks.add("")
            nouveauxPv.add("")
        }
        while (nouveauxJoueurs.size > borne) {
            val retire = nouveauxJoueurs.removeAt(nouveauxJoueurs.size - 1)
            nouveauxDecks.removeAt(nouveauxDecks.size - 1)
            nouveauxPv.removeAt(nouveauxPv.size - 1)
            if (winnerId == retire) winnerId = null
            if (firstPlayerId == retire) firstPlayerId = nouveauxJoueurs.firstOrNull().orEmpty()
        }
        joueurs = nouveauxJoueurs
        decks = nouveauxDecks
        pointsDeVie = nouveauxPv
        erreur = ""
    }

    fun choisirJoueur(index: Int, playerId: String) {
        val ancien = joueurs.getOrNull(index)
        joueurs = ArrayList(joueurs).also { it[index] = playerId }
        decks = ArrayList(decks).also { it[index] = "" }
        if (winnerId == ancien) winnerId = playerId
        if (firstPlayerId == ancien) firstPlayerId = playerId
        erreur = ""
    }

    val participants = remember(joueurs, state.players) {
        joueurs.mapNotNull { id -> state.player(id) }
    }
    val tousChoisis = joueurs.all { it.isNotBlank() } && joueurs.toSet().size == joueurs.size

    fun enregistrer() {
        when {
            !tousChoisis -> erreur = "Chaque siège doit être occupé par un joueur différent."
            decks.any { it.isBlank() } -> erreur = "Choisis un deck pour chaque joueur."
            winnerId == null -> erreur = "Sélectionne le vainqueur."
            else -> {
                viewModel.saveGame(
                    Game(
                        id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                        playedAt = playedAt,
                        format = format,
                        startingLife = startingLife,
                        firstPlayerId = firstPlayerId.ifBlank { joueurs.first() },
                        winnerId = winnerId!!,
                        sides = joueurs.mapIndexed { index, playerId ->
                            GameSide(
                                playerId = playerId,
                                deckId = decks[index],
                                lifeRemaining = pointsDeVie.getOrNull(index)?.toIntOrNull() ?: 0
                            )
                        },
                        endReason = endReason,
                        turns = turnsText.toIntOrNull(),
                        notes = notes.trim()
                    )
                )
                onDone()
            }
        }
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
                        "Ajoute d'abord au moins un deck.",
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
                                prremplirPointsDeVie(winnerId, life)
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
                            prremplirPointsDeVie(winnerId, startingLife)
                        }
                    )
                }
            }

            // ------------------------------------------------ nombre de joueurs
            SectionCard(title = "Combien de joueurs ?") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Joueurs autour de la table",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { changerNombreDeJoueurs(joueurs.size - 1) },
                        enabled = joueurs.size > JOUEURS_MIN,
                        modifier = Modifier.testTag("moins-joueur-mtg")
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Un joueur de moins")
                    }
                    Text(
                        text = "${joueurs.size}",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .width(40.dp)
                            .testTag("nombre-joueurs-mtg")
                    )
                    IconButton(
                        onClick = { changerNombreDeJoueurs(joueurs.size + 1) },
                        enabled = joueurs.size < JOUEURS_MAX && joueurs.size < roster.size,
                        modifier = Modifier.testTag("plus-joueur-mtg")
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Un joueur de plus")
                    }
                }
                if (roster.size <= joueurs.size) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Pour jouer à plus, ajoute d'abord des joueurs.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onManagePlayers) { Text("Gérer les joueurs") }
                    }
                }
            }

            // -------------------------------------------------------- joueurs
            SectionCard(title = "Joueurs, decks et PV restants") {
                joueurs.forEachIndexed { index, playerId ->
                    if (index > 0) HorizontalDivider()
                    SiegeJoueur(
                        state = state,
                        index = index,
                        playerId = playerId,
                        deckId = decks.getOrElse(index) { "" },
                        pv = pointsDeVie.getOrElse(index) { "" },
                        joueursDisponibles = roster.filter { joueur ->
                            joueur.id == playerId || joueurs.none { it == joueur.id }
                        },
                        vainqueurChoisi = winnerId != null,
                        estVainqueur = winnerId == playerId,
                        onJoueur = { choisirJoueur(index, it) },
                        onDeck = { decks = ArrayList(decks).also { d -> d[index] = it }; erreur = "" },
                        onPv = { pointsDeVie = ArrayList(pointsDeVie).also { p -> p[index] = it } }
                    )
                }
                TextButton(onClick = onManageDecks) { Text("Ajouter / modifier un deck") }
            }

            // ---------------------------------------------------- premier tour
            SectionCard(title = "Qui a commencé ?") {
                ChoixParmiJoueurs(
                    joueurs = participants,
                    selectedId = firstPlayerId,
                    onSelect = { firstPlayerId = it },
                    tagPrefix = "premier"
                )
            }

            // ------------------------------------------------------- vainqueur
            SectionCard(title = "Qui a gagné ?") {
                ChoixParmiJoueurs(
                    joueurs = participants,
                    selectedId = winnerId,
                    onSelect = {
                        winnerId = it
                        prremplirPointsDeVie(it, startingLife)
                    },
                    tagPrefix = "vainqueur"
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

            if (erreur.isNotBlank()) {
                Text(
                    erreur,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = { enregistrer() },
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
private fun SiegeJoueur(
    state: AppState,
    index: Int,
    playerId: String,
    deckId: String,
    pv: String,
    joueursDisponibles: List<Player>,
    vainqueurChoisi: Boolean,
    estVainqueur: Boolean,
    onJoueur: (String) -> Unit,
    onDeck: (String) -> Unit,
    onPv: (String) -> Unit
) {
    val decksProposes = remember(state.allDecks, playerId, deckId) {
        val liste = if (playerId.isBlank()) state.decks else state.decksFor(playerId)
        val courant = state.deck(deckId)
        if (courant != null && liste.none { it.id == courant.id }) liste + courant else liste
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PickerField(
            label = "Joueur ${index + 1}",
            options = joueursDisponibles,
            selected = joueursDisponibles.firstOrNull { it.id == playerId },
            optionLabel = { it.name },
            onSelect = { onJoueur(it.id) },
            placeholder = "Choisir un joueur…",
            modifier = Modifier
                .fillMaxWidth()
                .testTag("joueur-$index")
        )
        PickerField(
            label = "Deck",
            options = decksProposes,
            selected = decksProposes.firstOrNull { it.id == deckId },
            optionLabel = { it.name + if (it.archived) " (archivé)" else "" },
            onSelect = { onDeck(it.id) },
            placeholder = "Choisir un deck…",
            optionContent = { deck: Deck ->
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
                .testTag("deck-$index")
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("PV restants", style = MaterialTheme.typography.bodyMedium)
                // Le repère « à l'élimination » n'a de sens qu'une fois le vainqueur connu.
                if (vainqueurChoisi && !estVainqueur) {
                    Text(
                        "à l'élimination",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Stepper(value = pv, onValueChange = onPv, testTag = "pv-$index")
        }
    }
}

@Composable
private fun ChoixParmiJoueurs(
    joueurs: List<Player>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    tagPrefix: String
) {
    if (joueurs.isEmpty()) {
        Text(
            "Choisis d'abord les joueurs.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        joueurs.forEach { joueur ->
            FilterChip(
                selected = selectedId == joueur.id,
                onClick = { onSelect(joueur.id) },
                label = { Text(joueur.name, maxLines = 1) },
                modifier = Modifier.testTag("$tagPrefix-${joueur.id}")
            )
        }
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
                onValueChange(
                    buildString {
                        raw.forEachIndexed { index, c ->
                            if (c.isDigit() || (c == '-' && index == 0)) append(c)
                        }
                    }.take(4)
                )
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
