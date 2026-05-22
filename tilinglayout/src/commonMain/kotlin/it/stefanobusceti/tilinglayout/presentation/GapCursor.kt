package it.stefanobusceti.tilinglayout.presentation

import androidx.compose.ui.input.pointer.PointerIcon

/**
 * Platform-specific resize cursor shown when hovering over a horizontal divider (between vertically
 * stacked panes). JVM uses the AWT `S_RESIZE_CURSOR`; all other targets fall back to
 * [PointerIcon.Default].
 */
internal expect val horizontalResizeCursor: PointerIcon

/**
 * Platform-specific resize cursor shown when hovering over a vertical divider (between
 * side-by-side panes). JVM uses the AWT `E_RESIZE_CURSOR`; all other targets fall back to
 * [PointerIcon.Default].
 */
internal expect val verticalResizeCursor: PointerIcon
