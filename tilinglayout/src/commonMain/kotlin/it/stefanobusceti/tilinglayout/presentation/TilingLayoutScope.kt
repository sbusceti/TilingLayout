package it.stefanobusceti.tilinglayout.presentation

import androidx.compose.runtime.Composable
import it.stefanobusceti.tilinglayout.domain.TilingNode
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * DSL scope for the builder-style [TilingLayout] overload.
 *
 * A valid layout has exactly one top-level call ([leaf], [hSplit], or [vSplit]).
 * Each [hSplit]/[vSplit] block must contain exactly 2 children; providing any other
 * count throws [IllegalArgumentException] at composition time.
 *
 * Example:
 * ```kotlin
 * TilingLayout {
 *     hSplit {
 *         leaf { LeftPane() }
 *         vSplit {
 *             leaf { TopRightPane() }
 *             leaf { BottomRightPane() }
 *         }
 *     }
 * }
 * ```
 */
interface TilingLayoutScope {
    fun leaf(content: @Composable () -> Unit)

    /** Splits horizontally; [ratio] is the fraction of width given to the first child (clamped to 0.1–0.9 on drag). Requires exactly 2 children. */
    fun hSplit(ratio: Float = 0.5f, content: TilingLayoutScope.() -> Unit)

    /** Splits vertically; [ratio] is the fraction of height given to the first child (clamped to 0.1–0.9 on drag). Requires exactly 2 children. */
    fun vSplit(ratio: Float = 0.5f, content: TilingLayoutScope.() -> Unit)
}

internal class TilingLayoutScopeImpl : TilingLayoutScope {

    private val children = mutableListOf<TilingNode>()
    val leafContents = mutableMapOf<String, @Composable () -> Unit>()

    @OptIn(ExperimentalUuidApi::class)
    override fun leaf(content: @Composable (() -> Unit)) {
        val id = Uuid.random().toString()
        children.add(TilingNode.Leaf(id))
        leafContents[id] = content
    }

    override fun hSplit(
        ratio: Float,
        content: TilingLayoutScope.() -> Unit
    ) {
        val childScope = TilingLayoutScopeImpl()
        childScope.content()

        require(childScope.children.size == 2) {
            "hSplit requires exactly 2 children, got ${childScope.children.size}"
        }

        val left = childScope.children[0]
        val right = childScope.children[1]
        children.add(TilingNode.HSplit(leftNode = left, rightNode = right, ratio = ratio))
        leafContents.putAll(childScope.leafContents)
    }

    override fun vSplit(
        ratio: Float,
        content: TilingLayoutScope.() -> Unit
    ) {
        val childScope = TilingLayoutScopeImpl()
        childScope.content()

        require(childScope.children.size == 2) {
            "vSplit requires exactly 2 children, got ${childScope.children.size}"
        }

        val top = childScope.children[0]
        val bottom = childScope.children[1]
        children.add(TilingNode.VSplit(topNode = top, bottomNode = bottom, ratio = ratio))
        leafContents.putAll(childScope.leafContents)
    }

    fun buildNode() = children.firstOrNull() ?: TilingNode.EmptyNode
}