package fr.leonelal.mtgduel.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Orchestre la synchronisation : au lancement, puis après chaque modification (avec un délai,
 * pour ne pas appeler le serveur à chaque frappe), et à la demande depuis les Réglages.
 */
class SyncService(
    private val repository: GameRepository,
    private val settingsStore: SyncSettingsStore,
    private val client: SyncClient = SyncClient(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state.asStateFlow()

    private val enCours = Mutex()

    @OptIn(FlowPreview::class)
    fun start() {
        scope.launch { syncIfActive() }
        scope.launch {
            repository.state
                .drop(1)
                .debounce(DELAI_APRES_MODIFICATION_MS)
                .collect { syncIfActive() }
        }
    }

    private suspend fun syncIfActive() {
        if (settingsStore.settings.value.isActive) sync()
    }

    /** Lance une synchronisation. Retourne un message lisible, succès ou échec. */
    suspend fun sync(): Result<String> {
        val settings = settingsStore.settings.value
        if (!settings.isConfigured) {
            val message = "Renseigne l'adresse et le jeton de synchronisation."
            _state.value = SyncState.Failure(message)
            return Result.failure(IllegalStateException(message))
        }

        if (!enCours.tryLock()) return Result.success("Synchronisation déjà en cours.")
        return try {
            _state.value = SyncState.Running
            val avant = repository.state.value
            val distant = client.push(settings, avant)

            // L'état a pu changer pendant l'aller-retour : on refusionne avec l'état courant.
            val fusionne = SyncMerge.merge(repository.state.value, distant)
            val recus = compterNouveautes(reference = avant, apres = fusionne)
            repository.replaceWith(fusionne)

            val instant = System.currentTimeMillis()
            settingsStore.update { it.copy(lastSyncAt = instant) }
            _state.value = SyncState.Success(
                at = instant,
                received = recus,
                sent = avant.allDecks.size + avant.allGames.size +
                    avant.allCustomGames.size + avant.allCustomMatches.size
            )
            Result.success(
                if (recus == 0) "Déjà à jour." else "$recus élément(s) récupéré(s)."
            )
        } catch (erreur: Throwable) {
            val message = erreur.message ?: "Synchronisation impossible."
            _state.value = SyncState.Failure(message)
            Result.failure(erreur)
        } finally {
            enCours.unlock()
        }
    }

    private fun compterNouveautes(reference: AppState, apres: AppState): Int {
        fun <T : Any> nouveautes(avant: List<T>, apres: List<T>, id: (T) -> String): Int {
            val index = avant.associateBy(id)
            return apres.count { index[id(it)] != it }
        }
        return nouveautes(reference.allDecks, apres.allDecks) { it.id } +
            nouveautes(reference.allGames, apres.allGames) { it.id } +
            nouveautes(reference.allCustomGames, apres.allCustomGames) { it.id } +
            nouveautes(reference.allCustomMatches, apres.allCustomMatches) { it.id }
    }

    private companion object {
        const val DELAI_APRES_MODIFICATION_MS = 3_000L
    }
}
