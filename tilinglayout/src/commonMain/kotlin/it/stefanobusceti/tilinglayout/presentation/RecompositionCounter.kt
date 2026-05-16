package it.stefanobusceti.tilinglayout.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Debug composable that prints a running recomposition count to stdout, tagged with [text].
 * Marked [NonRestartableComposable] so it does not introduce an extra recomposition scope.
 */
@NonRestartableComposable
@Composable
fun RecompositionCounter(
    text: String
) {
    var count by remember { mutableStateOf(0) }
    SideEffect {
        count++
        println("$text recompositions= $count")
    }
}
