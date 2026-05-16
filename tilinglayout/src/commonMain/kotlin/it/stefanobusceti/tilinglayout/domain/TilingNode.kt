package it.stefanobusceti.tilinglayout.domain

import androidx.compose.runtime.Stable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Stable
/** Tree structure describing how to partition a rectangular area into tiles. */
sealed class TilingNode {
    data object EmptyNode : TilingNode()
    /** Terminal node that holds actual content, identified by [id]. */
    data class Leaf(val id: String) : TilingNode()

    /**
     * Splits the area horizontally: [leftNode] gets [ratio] of the width, [rightNode] gets the rest.
     * [id] is auto-generated and used by [TilingLayout][it.stefanobusceti.tilinglayout.presentation.TilingLayout]
     * to track runtime ratio overrides when the user drags the divider.
     */
    @OptIn(ExperimentalUuidApi::class)
    data class HSplit(
        val id: String = Uuid.random().toString(),
        val leftNode: TilingNode,
        val rightNode: TilingNode,
        val ratio: Float = 0.5f
    ) : TilingNode()

    /**
     * Splits the area vertically: [topNode] gets [ratio] of the height, [bottomNode] gets the rest.
     * [id] is auto-generated and used by [TilingLayout][it.stefanobusceti.tilinglayout.presentation.TilingLayout]
     * to track runtime ratio overrides when the user drags the divider.
     */
    @OptIn(ExperimentalUuidApi::class)
    data class VSplit(
        val id: String = Uuid.random().toString(),
        val topNode: TilingNode,
        val bottomNode: TilingNode,
        val ratio: Float = 0.5f
    ) : TilingNode()

    /**
     * Returns a new tree with the [Leaf] identified by [id] removed.
     * When a split loses one child, it is replaced by the remaining child.
     * Returns `null` if the tree reduces to nothing (i.e. this node itself is the target leaf).
     */
    fun removeLeaf(id: String): TilingNode =
        when (this) {
            is Leaf -> if (this.id == id) EmptyNode else this
            is HSplit -> {
                val newLeft = leftNode.removeLeaf(id)
                val newRight = rightNode.removeLeaf(id)
                when {
                    newLeft is EmptyNode -> newRight
                    newRight is EmptyNode -> newLeft
                    else -> HSplit(this.id, newLeft, newRight, this.ratio)
                }
            }

            is VSplit -> {
                val newTop = topNode.removeLeaf(id)
                val newBottom = bottomNode.removeLeaf(id)
                when {
                    newTop is EmptyNode -> newBottom
                    newBottom is EmptyNode -> newTop
                    else -> VSplit(this.id, newTop, newBottom, this.ratio)
                }
            }

            EmptyNode -> this
        }

    /**
     * Returns a new tree with the [Leaf] nodes identified by [srcId] and [destId] having their IDs exchanged.
     * Split structure and ratios are preserved unchanged.
     * If either ID is not found the tree is returned unmodified.
     */
    fun swapLeaves(srcId: String, destId: String): TilingNode =
        when (this) {
            is HSplit -> {
                val newLeft = leftNode.swapLeaves(srcId, destId)
                val newRight = rightNode.swapLeaves(srcId, destId)
                HSplit(this.id, newLeft, newRight, ratio)
            }

            is Leaf -> when (this.id) {
                srcId -> Leaf(destId)
                destId -> Leaf(srcId)
                else -> this
            }

            is VSplit -> {
                val newTop = topNode.swapLeaves(srcId, destId)
                val newBottom = bottomNode.swapLeaves(srcId, destId)
                VSplit(this.id, newTop, newBottom, ratio)
            }

            EmptyNode -> this
        }

    /**
     * Returns a new tree with a new [Leaf] identified by [id] inserted in the given [splitArea] direction.
     *
     * If [leafDestId] is `null`, wraps the entire tree in a new split at the specified edge
     * (e.g. [SplitArea.Right] produces `HSplit(leftNode = this, rightNode = Leaf(id))`).
     * If [leafDestId] is provided, finds that specific leaf and replaces it with a split containing
     * both the original leaf and the new one, arranged according to [splitArea].
     */
    fun addLeaf(id: String, splitArea: SplitArea, leafDestId: String? = null): TilingNode {
        if (leafDestId == null) {
            return when (splitArea) {
                SplitArea.Top -> VSplit(topNode = Leaf(id), bottomNode = this)
                SplitArea.Bottom -> VSplit(topNode = this, bottomNode = Leaf(id))
                SplitArea.Left -> HSplit(leftNode = Leaf(id), rightNode = this)
                SplitArea.Right -> HSplit(leftNode = this, rightNode = Leaf(id))
            }
        }
        return when (this) {
            is HSplit -> {
                val leftNode = leftNode.addLeaf(id, splitArea, leafDestId)
                val rightNode = rightNode.addLeaf(id, splitArea, leafDestId)
                HSplit(this.id, leftNode, rightNode)
            }

            is Leaf -> when (this.id) {
                leafDestId -> when (splitArea) {
                    SplitArea.Top -> VSplit(
                        topNode = Leaf(id),
                        bottomNode = Leaf(leafDestId),
                    )

                    SplitArea.Bottom -> VSplit(
                        topNode = Leaf(leafDestId),
                        bottomNode = Leaf(id),
                    )

                    SplitArea.Left -> HSplit(
                        leftNode = Leaf(id),
                        rightNode = Leaf(leafDestId),
                    )

                    SplitArea.Right -> HSplit(
                        leftNode = Leaf(leafDestId),
                        rightNode = Leaf(id),
                    )
                }

                else -> this
            }

            is VSplit -> {
                val topNode = topNode.addLeaf(id, splitArea, leafDestId)
                val bottomNode = bottomNode.addLeaf(id, splitArea, leafDestId)
                VSplit(this.id, topNode, bottomNode)
            }

            EmptyNode -> this
        }
    }
}

fun TilingNode.leafIds(): List<String> = when (this) {
    is TilingNode.Leaf -> listOf(id)
    is TilingNode.HSplit -> leftNode.leafIds() + rightNode.leafIds()
    is TilingNode.VSplit -> topNode.leafIds() + bottomNode.leafIds()
    TilingNode.EmptyNode -> emptyList()
}
