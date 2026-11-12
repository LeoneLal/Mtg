package fr.leonelal.mtgduel.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import fr.leonelal.mtgduel.data.CustomMatch
import fr.leonelal.mtgduel.data.JeuxStats
import fr.leonelal.mtgduel.data.MatchParticipant
import fr.leonelal.mtgduel.ui.AppViewModel
import fr.leonelal.mtgduel.ui.SectionCard
import fr.leonelal.mtgduel.ui.formatFullDate
import fr.leonelal.mtgduel.ui.formatTime
import java.util.Calendar

private const val JOUEURS_MIN = 2
private const val JOUEURS_MAX = 12

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartieJeuFormScreen(
    state: AppState,
    viewModel: AppViewModel,
    gameId: String,
    matchId: String?,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val jeu = remember(gameId, state.allCustomGames) { state.customGame(gameId) }
    val existante = remember(matchId, state.allCustomMatches) {
        state.allCustomMatches.firstOrNull { it.id == matchId }
    }

    if (jeu == null) {
        JeuIntrouvablePourPartie(onDone)
        return
    }

    val precedente = remember(state.allCustomMatches, gameId, matchId) {
        state.matchesOf(gameId).firstOrNull { it.id != matchId }
    }
    val nomsConnus = remember(state.allCustomMatches) { JeuxStats.nomsConnus(state.customMatches) }

    /** Noms proposés d'emblée : ceux de la partie précédente, sinon les deux joueurs de Magic. */
    val nomsInitiaux = remember(existante, precedente, state.players) {
        when {
            existante != null -> existante.participants.map { it.name }
            precedente != null -> precedente.participants.map { it.name }
            else -> listOf(state.playerOne.name, state.playerTwo.name)
        }
    }

    var playedAt by rememberSaveable(matchId) {
        mutableStateOf(existante?.playedAt ?: System.currentTimeMillis())
    }
    var noms by rememberSaveable(matchId) { mutableStateOf(ArrayList(nomsInitiaux)) }
    var scores by rememberSaveable(matchId) {
        mutableStateOf(
            ArrayList(
                existante?.participants?.map { it.score.toString() }
                    ?: List(nomsInitiaux.size) { "0" }
            )
        )
    }
    var notes by rememberSaveable(matchId) { mutableStateOf(existante?.notes ?: "") }
    var erreur by rememberSaveable(matchId) { mutableStateOf("") }
    var confirmerSuppression by rememberSaveable { mutableStateOf(false) }

    fun changerNombreDeJoueurs(cible: Int) {
        val borne = cible.coerceIn(JOUEURS_MIN, JOUEURS_MAX)
        val nouveauxNoms = ArrayList(noms)
        val nouveauxScores = ArrayList(scores)
        while (nouveauxNoms.size < borne) {
            nouveauxNoms.add("")
            nouveauxScores.add("0")
        }
        while (nouveauxNoms.size > borne) {
            nouveauxNoms.removeAt(nouveauxNoms.size - 1)
            nouveauxScores.removeAt(nouveauxScores.size - 1)
        }
        noms = nouveauxNoms
        scores = nouveauxScores
        erreur = ""
    }

    fun modifierNom(index: Int, valeur: String) {
        noms = ArrayList(noms).also { it[index] = valeur }
        erreur = ""
    }

    fun modifierScore(index: Int, valeur: String) {
        scores = ArrayList(scores).also { it[index] = valeur }
    }

    val participants = noms.mapIndexed { index, nom ->
        MatchParticipant(name = nom.trim(), score = scores.getOrNull(index)?.toIntOrNull() ?: 0)
    }
    val apercu = CustomMatch(gameId = gameId, playedAt = playedAt, participants = participants)

    /**
     * Sur un formulaire vierge tout le monde est à zéro : annoncer « égalité entre tous »
     * et coiffer chacun d'une coupe n'aurait aucun sens. On n'affiche le résultat qu'une
     * fois au moins un score saisi.
     */
    val scoresSaisis = participants.any { it.score != 0 }
    val vainqueurs = if (scoresSaisis && participants.all { it.name.isNotBlank() }) {
        apercu.vainqueurs(jeu.scoring).map { it.name }
    } else {
        emptyList()
    }

    fun enregistrer() {
        val nomsRemplis = participants.map { it.name }
        when {
            nomsRemplis.any { it.isBlank() } ->
                erreur = "Chaque joueur doit avoir un nom."
            nomsRemplis.map { it.lowercase() }.toSet().size != nomsRemplis.size ->
                erreur = "Deux joueurs portent le même nom."
            else -> {
                viewModel.saveCustomMatch(
                    CustomMatch(
                        id = existante?.id ?: java.util.UUID.randomUUID().toString(),
                        gameId = gameId,
                        playedAt = playedAt,
                        participants = participants,
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
                title = {
                    Text(if (existante == null) "Nouvelle partie" else "Modifier la partie")
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (existante != null) {
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
            SectionCard(title = jeu.name) {
                Text(
                    jeu.scoring.explication,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard(title = "Quand ?") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = playedAt }
                            DatePickerDialog(
                                context,
                                { _, annee, mois, jour ->
                                    playedAt = Calendar.getInstance().apply {
                                        timeInMillis = playedAt
                                        set(Calendar.YEAR, annee)
                                        set(Calendar.MONTH, mois)
                                        set(Calendar.DAY_OF_MONTH, jour)
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
                                { _, heure, minute ->
                                    playedAt = Calendar.getInstance().apply {
                                        timeInMillis = playedAt
                                        set(Calendar.HOUR_OF_DAY, heure)
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

            SectionCard(title = "Combien de joueurs ?") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Nombre de joueurs",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { changerNombreDeJoueurs(noms.size - 1) },
                        enabled = noms.size > JOUEURS_MIN,
                        modifier = Modifier.testTag("moins-joueur")
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Un joueur de moins")
                    }
                    Text(
                        text = "${noms.size}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .width(40.dp)
                            .testTag("nombre-joueurs"),
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = { changerNombreDeJoueurs(noms.size + 1) },
                        enabled = noms.size < JOUEURS_MAX,
                        modifier = Modifier.testTag("plus-joueur")
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Un joueur de plus")
                    }
                }
            }

            SectionCard(title = "Joueurs et ${jeu.scoring.libelleScore.lowercase()}") {
                noms.forEachIndexed { index, nom ->
                    LigneJoueur(
                        index = index,
                        nom = nom,
                        score = scores.getOrElse(index) { "0" },
                        libelleScore = jeu.scoring.libelleScore,
                        suggestions = nomsConnus.filterNot { connu ->
                            noms.any { it.trim().equals(connu, ignoreCase = true) }
                        },
                        estVainqueur = nom.isNotBlank() && nom.trim() in vainqueurs,
                        onNom = { modifierNom(index, it) },
                        onScore = { modifierScore(index, it) }
                    )
                }

                when {
                    vainqueurs.size == 1 -> Text(
                        "Vainqueur : ${vainqueurs.first()}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    vainqueurs.size > 1 -> Text(
                        "Égalité entre ${vainqueurs.joinToString(", ")}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )

                    else -> Text(
                        "Le vainqueur s'affichera dès que les " +
                            jeu.scoring.libelleScore.lowercase() + " seront saisis.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionCard(title = "Divers") {
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
                Text(if (existante == null) "Enregistrer la partie" else "Enregistrer les modifications")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmerSuppression && existante != null) {
        AlertDialog(
            onDismissRequest = { confirmerSuppression = false },
            title = { Text("Supprimer cette partie ?") },
            text = { Text("Elle sera retirée de l'historique et du classement.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCustomMatch(existante.id)
                    confirmerSuppression = false
                    onDone()
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { confirmerSuppression = false }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun LigneJoueur(
    index: Int,
    nom: String,
    score: String,
    libelleScore: String,
    suggestions: List<String>,
    estVainqueur: Boolean,
    onNom: (String) -> Unit,
    onScore: (String) -> Unit
) {
    var menuOuvert by remember { mutableStateOf(false) }
    val valeur = score.toIntOrNull() ?: 0

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            if (estVainqueur) {
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = "En tête",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.size(4.dp))
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = nom,
                onValueChange = onNom,
                label = { Text("Joueur ${index + 1}") },
                singleLine = true,
                trailingIcon = if (suggestions.isNotEmpty()) {
                    {
                        IconButton(onClick = { menuOuvert = true }) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = "Noms déjà utilisés")
                        }
                    }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nom-joueur-$index")
            )
            DropdownMenu(expanded = menuOuvert, onDismissRequest = { menuOuvert = false }) {
                suggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            onNom(suggestion)
                            menuOuvert = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.size(6.dp))
        IconButton(onClick = { onScore((valeur - 1).toString()) }) {
            Icon(Icons.Filled.Remove, contentDescription = "Diminuer")
        }
        OutlinedTextField(
            value = score,
            onValueChange = { brut ->
                onScore(
                    buildString {
                        brut.forEachIndexed { position, caractere ->
                            if (caractere.isDigit() || (caractere == '-' && position == 0)) append(caractere)
                        }
                    }.take(5)
                )
            },
            label = { Text(libelleScore) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .width(96.dp)
                .testTag("score-joueur-$index")
        )
        IconButton(onClick = { onScore((valeur + 1).toString()) }) {
            Icon(Icons.Filled.Add, contentDescription = "Augmenter")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JeuIntrouvablePourPartie(onDone: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jeu introuvable") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Text("Ce jeu n'existe plus.", modifier = Modifier.padding(padding).padding(16.dp))
    }
}
