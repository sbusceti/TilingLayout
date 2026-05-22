package it.stefanobusceti.tilinglayout.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Visual configuration for the draggable dividers rendered between adjacent panes in [TilingLayout].
 *
 * @property thickness Width (for vertical dividers) or height (for horizontal dividers) of each gap.
 *   Defaults to `8.dp` — enough hit area for comfortable dragging without being visually prominent.
 * @property color Fill color of the divider. Defaults to [Color.Transparent] so the gap acts as
 *   invisible dead space; set a visible color (e.g. `MaterialTheme.colorScheme.outlineVariant`)
 *   to render a visible separator.
 */
data class GapDefaults(
    val thickness: Dp = 8.dp,
    val color: Color = Color.Transparent
)
