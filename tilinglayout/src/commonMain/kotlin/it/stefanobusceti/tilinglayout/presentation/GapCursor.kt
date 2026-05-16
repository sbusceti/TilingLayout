package it.stefanobusceti.tilinglayout.presentation

import androidx.compose.ui.input.pointer.PointerIcon

// JVM provides AWT resize cursors; all other targets fall back to PointerIcon.Default.
internal expect val horizontalResizeCursor: PointerIcon
internal expect val verticalResizeCursor: PointerIcon
