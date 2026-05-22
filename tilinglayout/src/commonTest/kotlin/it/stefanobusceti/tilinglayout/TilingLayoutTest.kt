package it.stefanobusceti.tilinglayout

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import it.stefanobusceti.tilinglayout.domain.*
import it.stefanobusceti.tilinglayout.presentation.TilingLayout
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalComposeUiApi::class)
class TilingLayoutUiTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testLayoutRendersLeaves() = runComposeUiTest {
        setContent {
            TilingLayout(
                node = TilingNode.Leaf("Win-1")
            ) { id ->
                Text(id)
            }
        }

        onNodeWithText("Win-1").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testAddLeaf() = runComposeUiTest {
        var node by mutableStateOf<TilingNode>(TilingNode.Leaf("Win-1"))
        setContent {
            TilingLayout(
                node = node
            ) { id ->
                Text(id)
            }
        }
        node = node.add("Win-2", "Win-1", SplitArea.Left)
        onNodeWithText("Win-2").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testRemoveLeaf() = runComposeUiTest {
        var node by mutableStateOf<TilingNode>(TilingNode.Leaf("Win-1"))
        setContent {
            TilingLayout(
                node = node
            ) { id ->
                Text(id)
            }
        }
        node = node.remove("Win-1")

        onNodeWithText("Win-1").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testSwapLeaves() = runComposeUiTest {
        var node by mutableStateOf<TilingNode>(
            TilingNode.Split(
                splitDirection = SplitDirection.Horizontal,
                children = listOf(
                    TilingNode.Leaf("1"),
                    TilingNode.Leaf("2"),
                )
            )
        )
        setContent {
            TilingLayout(
                node = node
            ) { id ->
                Text(id)
            }
        }

        val initialPos1 = onNodeWithText("1").fetchSemanticsNode().boundsInRoot.left
        val initialPos2 = onNodeWithText("2").fetchSemanticsNode().boundsInRoot.left

        node = node.swapLeaves("1", "2")

        val finalPos1 = onNodeWithText("1").fetchSemanticsNode().boundsInRoot.left
        val finalPos2 = onNodeWithText("2").fetchSemanticsNode().boundsInRoot.left

        assertEquals(initialPos1, finalPos2)
        assertEquals(initialPos2, finalPos1)
    }
}
