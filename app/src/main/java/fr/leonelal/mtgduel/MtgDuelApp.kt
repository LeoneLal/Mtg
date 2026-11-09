package fr.leonelal.mtgduel

import android.app.Application
import fr.leonelal.mtgduel.data.CrashReporter
import fr.leonelal.mtgduel.data.GameRepository

class MtgDuelApp : Application() {

    val repository: GameRepository by lazy { GameRepository(this) }
    val crashReporter: CrashReporter by lazy { CrashReporter(this) }

    override fun onCreate() {
        super.onCreate()
        crashReporter.install()
    }
}
