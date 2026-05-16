package it.stefanobusceti.tilinglayout.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Visual configuration for the draggable dividers between panes. Transparent by default so the gap acts as invisible dead space. */
data class GapDefaults(
    val thickness: Dp = 8.dp,
    val color: Color = Color.Transparent
)
