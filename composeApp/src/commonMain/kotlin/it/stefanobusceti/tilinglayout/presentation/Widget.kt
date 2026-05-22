package it.stefanobusceti.tilinglayout.presentation

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun Widget(
    modifier: Modifier = Modifier,
    title: String,
    onCloseRequest: () -> Unit,
    onDragStart: (initialOffset: Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDrag: (delta: Offset) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    var globalPosition by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        WidgetTopBar(
            title = title,
            onCloseRequest = onCloseRequest,
            onDragStart = { initialOffset ->
                onDragStart(initialOffset + globalPosition)
            },
            onDragEnd = onDragEnd,
            onDrag = onDrag,
            modifier = Modifier.onGloballyPositioned { globalPosition = it.localToWindow(Offset.Zero) }
        )
        content()
    }
}

@Composable
private fun WidgetTopBar(
    modifier: Modifier = Modifier,
    title: String,
    onCloseRequest: () -> Unit,
    onDragStart: (initialOffset: Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDrag: (delta: Offset) -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = onDragStart,
                    onDragEnd = onDragEnd,
                    onDrag = { change, delta ->
                        change.consume()
                        onDrag(delta)
                    }
                )
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title)
        IconButton(onClick = onCloseRequest) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WidgetTopBarPreview() {
    WidgetTopBar(title = "Title", onCloseRequest = {}, onDragStart = {}, onDragEnd = {})
}

@Preview(showBackground = true)
@Composable
fun WidgetPreview() {
    Widget(title = "Title", onCloseRequest = {}) {}
}