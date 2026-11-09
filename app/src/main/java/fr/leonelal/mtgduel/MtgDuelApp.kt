package fr.leonelal.mtgduel

import android.app.Application
import fr.leonelal.mtgduel.data.CrashReporter
import fr.leonelal.mtgduel.data.GameRepository
import fr.leonelal.mtgduel.data.SyncService
import fr.leonelal.mtgduel.data.SyncSettingsStore

class MtgDuelApp : Application() {

    val repository: GameRepository by lazy { GameRepository(this) }
    val crashReporter: CrashReporter by lazy { CrashReporter(this) }
    val syncSettingsStore: SyncSettingsStore by lazy { SyncSettingsStore(this) }
    val syncService: SyncService by lazy { SyncService(repository, syncSettingsStore) }

    override fun onCreate() {
        super.onCreate()
        crashReporter.install()
        syncService.start()
    }
}
