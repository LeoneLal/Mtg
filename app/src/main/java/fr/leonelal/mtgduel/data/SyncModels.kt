package fr.leonelal.mtgduel.data

import kotlinx.serialization.Serializable

/**
 * Réglages de synchronisation. Volontairement stockés à part des données de jeu :
 * ils contiennent un secret, qui n'a donc rien à faire dans les sauvegardes exportées.
 */
@Serializable
data class SyncSettings(
    val enabled: Boolean = false,
    val url: String = "",
    val token: String = "",
    val lastSyncAt: Long = 0L
) {
    val isConfigured: Boolean
        get() = url.isNotBlank() && token.isNotBlank()

    val isActive: Boolean
        get() = enabled && isConfigured
}

sealed interface SyncState {
    data object Idle : SyncState
    data object Running : SyncState
    data class Success(val at: Long, val received: Int, val sent: Int) : SyncState
    data class Failure(val message: String) : SyncState
}
