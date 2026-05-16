package it.stefanobusceti.tilinglayout

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import it.stefanobusceti.tilinglayout.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "TilingLayout",
        ) {
            App()
        }
    }
}
