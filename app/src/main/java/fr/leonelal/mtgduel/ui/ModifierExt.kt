package fr.leonelal.mtgduel.ui

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier

fun Modifier.clickableIf(enabled: Boolean, onClick: () -> Unit): Modifier =
    if (enabled) this.clickable(onClick = onClick) else this
