package fr.leonelal.mtgduel.data

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * L'application étant installée hors store, aucun rapport de plantage ne remonte nulle part.
 * On enregistre donc la dernière erreur fatale sur le téléphone pour pouvoir la relire
 * (et la partager) depuis l'écran Réglages.
 */
class CrashReporter(context: Context) {

    private val file = File(context.applicationContext.filesDir, FILE_NAME)
    private val versionName: String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "?"

    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { write(thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun write(thread: Thread, throwable: Throwable) {
        val trace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val horodatage = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())
        file.writeText(
            buildString {
                appendLine("MTG Duel $versionName")
                appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("Le $horodatage, fil « ${thread.name} »")
                appendLine()
                append(trace)
            }
        )
    }

    /** Le contenu de la dernière erreur fatale, ou null si l'app n'a jamais planté. */
    fun lastCrash(): String? = runCatching {
        if (file.exists() && file.length() > 0) file.readText() else null
    }.getOrNull()

    fun clear() {
        runCatching { file.delete() }
    }

    private companion object {
        const val FILE_NAME = "derniere-erreur.txt"
    }
}
