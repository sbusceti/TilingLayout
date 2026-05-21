package it.stefanobusceti.tilinglayout.domain

/** Returns the IDs of all [TilingNode.Leaf] nodes reachable from this node. */
fun TilingNode.leavesId(): Set<String> = when (this) {
    is TilingNode.EmptyNode -> emptySet()
    is TilingNode.Leaf -> setOf(this.id)
    is TilingNode.Split -> this.children.flatMap { it.leavesId() }.toSet()
}

/**
 * Returns a new tree with a fresh [TilingNode.Leaf] identified by [id] inserted adjacent to the
 * leaf identified by [targetNodeId].
 *
 * [splitArea] controls which side of the target pane the new leaf appears on:
 * - [SplitArea.Left] / [SplitArea.Right] — the target leaf is replaced by a horizontal [TilingNode.Split]
 *   whose children are the new leaf and the original leaf, in left-to-right order.
 * - [SplitArea.Top] / [SplitArea.Bottom] — the target leaf is replaced by a vertical [TilingNode.Split]
 *   whose children are the new leaf and the original leaf, in top-to-bottom order.
 *
 * If [targetNodeId] is not found in the tree the original tree is returned unchanged.
 * Must be called on the root node so the recursive search covers the entire tree.
 */
fun TilingNode.add(id: String, targetNodeId: String, splitArea: SplitArea): TilingNode = when (this) {
    is TilingNode.EmptyNode -> this
    is TilingNode.Leaf -> {
        if (targetNodeId != this.id) return this
        when (splitArea) {
            SplitArea.Top -> TilingNode.Split(
                splitDirection = SplitDirection.Vertical,
                children = listOf(
                    TilingNode.Leaf(id),
                    this
                )
            )

            SplitArea.Bottom -> TilingNode.Split(
                splitDirection = SplitDirection.Vertical,
                children = listOf(
                    this,
                    TilingNode.Leaf(id)
                )
            )

            SplitArea.Left -> TilingNode.Split(
                splitDirection = SplitDirection.Horizontal,
                children = listOf(
                    TilingNode.Leaf(id),
                    this
                )
            )

            SplitArea.Right -> TilingNode.Split(
                splitDirection = SplitDirection.Horizontal,
                children = listOf(
                    this,
                    TilingNode.Leaf(id)
                )
            )
        }
    }

    is TilingNode.Split -> {
        copy(children = children.map { it.add(id, targetNodeId, splitArea) })
    }
}

/**
 * Returns a new tree with the [TilingNode.Leaf] identified by [id] removed.
 *
 * When removal leaves a [TilingNode.Split] with a single child, the split is collapsed:
 * the surviving child inherits the split's [TilingNode.ratio].
 * When removal leaves an empty split, [TilingNode.EmptyNode] is returned.
 *
 * Must be called on the root node to ensure correct ratio propagation.
 */
fun TilingNode.remove(id: String): TilingNode = when (this) {
    is TilingNode.EmptyNode -> this
    is TilingNode.Leaf -> if (this.id == id) TilingNode.EmptyNode() else this
    is TilingNode.Split -> {
        val newChildren = children
            .map { it.remove(id) }
            .filter { it !is TilingNode.EmptyNode }
        when {
            newChildren.isEmpty() -> TilingNode.EmptyNode()
            newChildren.size == 1 -> when (val survivor = newChildren.first()) {
                is TilingNode.Leaf -> survivor.copy(ratio = this.ratio)
                is TilingNode.Split -> survivor.copy(ratio = this.ratio)
                is TilingNode.EmptyNode -> survivor
            }

            else -> copy(children = newChildren)
        }
    }
}

fun TilingNode.swapLeaves(srcId: String, dstId: String): TilingNode = when (this) {
    is TilingNode.EmptyNode -> this
    is TilingNode.Leaf -> when {
        id == dstId -> copy(id = srcId)
        id == srcId -> copy(id = dstId)
        else -> this
    }

    is TilingNode.Split -> copy(children = children.map { it.swapLeaves(srcId, dstId) })
}

/**
 * Returns a new tree with each [TilingNode.Split]'s child ratios replaced by values from [ratios].
 *
 * [ratios] is keyed by [TilingNode.Split.id]; the value is a list of ratios matching the number
 * of children in that split. Entries with a mismatched size are ignored.
 *
 * Primary use: persist user-dragged ratios back into the canonical tree after receiving them via
 * `TilingLayout`'s `onRatiosChanged` callback. Because the updated tree is a plain data class,
 * it can also be serialized to disk (e.g. JSON / DataStore) alongside the node structure so the
 * entire layout — including pane sizes — survives app restarts.
 *
 * Must be called on the root node to ensure correct ratio propagation.
 */
fun TilingNode.updateRatios(
    ratios: Map<String, List<Float>>
)
        : TilingNode = when (this) {
    is TilingNode.EmptyNode -> this
    is TilingNode.Leaf -> this
    is TilingNode.Split -> {
        val newRatios = ratios[this.id]?.takeIf { it.size == children.size }
        val newChildren = children.mapIndexed { index, child ->
            val updatedChild = if (newRatios != null) {
                when (child) {
                    is TilingNode.Leaf -> child.copy(ratio = newRatios[index])
                    is TilingNode.Split -> child.copy(ratio = newRatios[index])
                    is TilingNode.EmptyNode -> child
                }
            } else child
            updatedChild.updateRatios(ratios)
        }
        copy(children = newChildren)
    }
}

/**
 * Returns the IDs of all [TilingNode.Split] nodes in the subtree.
 * Used to prune stale runtime state (ratio overrides, measured sizes) when the tree changes.
 */
fun TilingNode.collectSplitIds(): Set<String> = when (this) {
    is TilingNode.Split -> setOf(id) + children.flatMap { it.collectSplitIds() }
    else -> emptySet()
}