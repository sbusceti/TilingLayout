package it.stefanobusceti.tilinglayout

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import it.stefanobusceti.tilinglayout.domain.TilingNode
import it.stefanobusceti.tilinglayout.presentation.TilingLayout
import kotlin.test.Test

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
    fun testRemoveLeaf() = runComposeUiTest {
        var node by mutableStateOf<TilingNode>(TilingNode.Leaf("Win-1"))
        setContent {
            TilingLayout(
                node = node
            ) { id ->
                Text(id)
            }
        }
        node = node.removeLeaf("Win-1")

        onNodeWithText("Win-1").assertDoesNotExist()
    }
}
