package fr.leonelal.mtgduel.data

/**
 * Fusion de deux états provenant de deux téléphones.
 *
 * Règle unique : pour un même identifiant, la version portant le `updatedAt` le plus récent
 * gagne. Les suppressions voyagent sous forme de pierres tombales (`deleted = true`), donc
 * supprimer une partie chez l'un la supprime aussi chez l'autre, au lieu de la voir revenir.
 *
 * Limite assumée : l'arbitrage repose sur l'horloge des téléphones. Si les deux modifient
 * la *même* partie sans s'être synchronisés entre-temps, celle enregistrée en dernier gagne,
 * et l'autre version est perdue. Pour deux joueurs qui saisissent leurs propres parties,
 * le cas ne se présente quasiment jamais.
 */
object SyncMerge {

    fun merge(local: AppState, remote: AppState): AppState = AppState(
        schemaVersion = maxOf(local.schemaVersion, remote.schemaVersion),
        allPlayers = mergeById(local.allPlayers, remote.allPlayers, { it.id }, { it.updatedAt }),
        allDecks = mergeById(local.allDecks, remote.allDecks, { it.id }, { it.updatedAt }),
        allGames = mergeById(local.allGames, remote.allGames, { it.id }, { it.updatedAt }),
        allCustomGames = mergeById(local.allCustomGames, remote.allCustomGames, { it.id }, { it.updatedAt }),
        allCustomMatches = mergeById(local.allCustomMatches, remote.allCustomMatches, { it.id }, { it.updatedAt })
    )

    private fun <T> mergeById(
        local: List<T>,
        remote: List<T>,
        id: (T) -> String,
        updatedAt: (T) -> Long
    ): List<T> {
        val fusionne = LinkedHashMap<String, T>(local.size + remote.size)
        local.forEach { fusionne[id(it)] = it }
        remote.forEach { distant ->
            val existant = fusionne[id(distant)]
            if (existant == null || updatedAt(distant) > updatedAt(existant)) {
                fusionne[id(distant)] = distant
            }
        }
        return fusionne.values.toList()
    }
}
