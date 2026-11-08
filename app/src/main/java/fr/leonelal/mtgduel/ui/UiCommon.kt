package fr.leonelal.mtgduel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.leonelal.mtgduel.data.MtgColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ------------------------------------------------------------------- formats

private val dayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)
private val shortDayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH)
private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH)

fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

fun formatFullDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dayFormatter)
        .replaceFirstChar { it.uppercase() }

fun formatShortDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(shortDayFormatter)

fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(timeFormatter)

/** "Aujourd'hui", "Hier", sinon la date courte. */
fun formatDayLabel(epochMillis: Long): String {
    val date = epochMillis.toLocalDate()
    val today = LocalDate.now()
    return when (date) {
        today -> "Aujourd'hui"
        today.minusDays(1) -> "Hier"
        else -> formatFullDate(epochMillis)
    }
}

fun formatPercent(ratio: Double): String =
    String.format(Locale.FRANCE, "%.0f %%", ratio * 100)

fun formatAverage(value: Double?): String =
    if (value == null) "–" else String.format(Locale.FRANCE, "%.1f", value)

/** Accord français : 0 et 1 restent au singulier. */
fun pluriel(nombre: Int, singulier: String, pluriel: String = singulier + "s"): String =
    "$nombre " + if (nombre > 1) pluriel else singulier

fun parties(nombre: Int): String = pluriel(nombre, "partie")

fun victoires(nombre: Int): String = pluriel(nombre, "victoire")

// --------------------------------------------------------------- composants

@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

@Composable
fun StatLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasis: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f, fill = false)
        )
        Text(
            text = value,
            style = if (emphasis) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = if (emphasis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Pastilles de couleurs d'un deck (W/U/B/R/G/C). */
@Composable
fun ColorPips(colors: List<String>, modifier: Modifier = Modifier, size: Int = 12) {
    if (colors.isEmpty()) return
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        colors.forEach { code ->
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .background(Color(MtgColors.tint(code)), CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
        }
    }
}

/** Champ de sélection générique : un bouton qui ouvre un menu déroulant. */
@Composable
fun <T> PickerField(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Choisir…",
    enabled: Boolean = true,
    isError: Boolean = false,
    optionContent: (@Composable (T) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .border(
                        width = if (isError) 2.dp else 1.dp,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .clickableIf(enabled) { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selected?.let(optionLabel) ?: placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (selected == null) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (options.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Aucune option disponible") },
                        onClick = { expanded = false },
                        enabled = false
                    )
                }
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            if (optionContent != null) optionContent(option) else Text(optionLabel(option))
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
