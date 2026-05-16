package it.stefanobusceti.tilinglayout.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
) {
    val viewModel: DashboardViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    DashboardScreenContent(
        modifier = modifier,
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun DashboardScreenContent(
    state: DashboardScreenState,
    onAction: (DashboardScreenAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TilingLayout(
        modifier = modifier,
        node = state.node,
    ) { nodeId ->
        Widget(
            title = "Widget $nodeId",
            onCloseRequest = { onAction(DashboardScreenAction.RemoveWidget(nodeId)) },
        )
    }
}

@Preview
@Composable
fun DashboardScreenContentPreview() {
    DashboardScreenContent(
        state = DashboardScreenState(),
        onAction = {}
    )
}