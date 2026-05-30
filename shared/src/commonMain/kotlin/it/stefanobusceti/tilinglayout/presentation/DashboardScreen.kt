package it.stefanobusceti.tilinglayout.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

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
    Box(
        modifier = modifier.fillMaxSize().padding(4.dp),
    ) {
        var offset by remember { mutableStateOf(Offset.Zero) }
        var draggingNodeId by remember { mutableStateOf("") }
        val widgetRects = remember { mutableStateMapOf<String, Rect>() }

        val targetNodeId by remember {
            derivedStateOf {
                widgetRects.entries
                    .firstOrNull { (id, rect) -> id != draggingNodeId && rect.contains(offset) }
                    ?.key ?: ""
            }
        }

        TilingLayout(
            modifier = Modifier.fillMaxSize(),
            node = state.node,
            onRatiosChanged = { ratios ->
                onAction(DashboardScreenAction.UpdateRatios(ratios))
            }
        ) { nodeId ->
    
            val widgetColor = when (nodeId) {
                draggingNodeId -> Color.Red
                targetNodeId -> Color.Gray
                else -> Color.Red
            }

            Widget(
                title = "Widget $nodeId",
                onCloseRequest = { onAction(DashboardScreenAction.RemoveWidget(nodeId)) },
                modifier = Modifier
                    .onGloballyPositioned { coords ->
                        widgetRects[nodeId] = Rect(
                            offset = coords.positionInWindow(),
                            size = coords.size.toSize()
                        )
                    }
                    .background(
                        color = widgetColor,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                onDragStart = {
                    draggingNodeId = nodeId
                    offset = it
                },
                onDrag = { delta ->
                    offset += delta
                },
                onDragEnd = {
                    onAction(DashboardScreenAction.SwapLeaves(draggingNodeId, targetNodeId))
                    offset = Offset.Zero
                    draggingNodeId = ""
                },
            ) {
                Text("Content of widget $nodeId")
            }
        }

        if (draggingNodeId.isNotEmpty()) {
            Text(
                text = draggingNodeId,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = offset.x.roundToInt(),
                            y = offset.y.roundToInt()
                        )
                    }
                    .size(50.dp)
                    .background(Color.Green)
            )
        }
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
