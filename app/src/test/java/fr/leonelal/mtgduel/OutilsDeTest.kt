package fr.leonelal.mtgduel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.leonelal.mtgduel.data.AppState
import fr.leonelal.mtgduel.data.CrashReporter
import fr.leonelal.mtgduel.data.GameRepository
import fr.leonelal.mtgduel.ui.AppViewModel
import java.io.File
import java.util.UUID
import kotlinx.serialization.json.Json

/**
 * Chaque test compose l'application avec son propre dépôt, stocké dans un fichier qui lui est
 * réservé : aucun état ne peut fuiter d'un test à l'autre, quel que soit l'ordre d'exécution.
 */
fun contexteDeTest(): Context = ApplicationProvider.getApplicationContext()

fun viewModelIsole(donnees: AppState? = null): AppViewModel {
    val context = contexteDeTest()
    val dossier = File(context.cacheDir, "test-${UUID.randomUUID()}").apply { mkdirs() }
    val repository = GameRepository(context, File(dossier, "data.json"))
    val viewModel = AppViewModel(repository, CrashReporter(context))
    if (donnees != null) {
        viewModel.importFromJson(
            Json { encodeDefaults = true }.encodeToString(AppState.serializer(), donnees)
        ).getOrThrow()
    }
    return viewModel
}
