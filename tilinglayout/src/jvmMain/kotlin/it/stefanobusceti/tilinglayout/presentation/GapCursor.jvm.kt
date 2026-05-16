package it.stefanobusceti.tilinglayout.presentation

import androidx.compose.ui.input.pointer.PointerIcon
import java.awt.Cursor

internal actual val horizontalResizeCursor: PointerIcon = PointerIcon(Cursor(Cursor.S_RESIZE_CURSOR))
internal actual val verticalResizeCursor: PointerIcon = PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))
