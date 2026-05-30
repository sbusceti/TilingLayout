package it.stefanobusceti.tilinglayout

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import it.stefanobusceti.tilinglayout.presentation.DashboardScreen

@Composable
fun App() {
    MaterialTheme {
        Scaffold { innerPadding ->
            DashboardScreen(
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
