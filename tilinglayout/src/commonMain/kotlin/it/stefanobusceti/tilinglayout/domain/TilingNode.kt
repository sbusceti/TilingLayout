package it.stefanobusceti.tilinglayout.domain

import androidx.compose.runtime.Stable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Stable
sealed class TilingNode {
    abstract val ratio: Float

    data class EmptyNode(override val ratio: Float = 0f) : TilingNode()
    data class Leaf(
        val id: String,
        override val ratio: Float = 0.5f
    ) : TilingNode()

    @OptIn(ExperimentalUuidApi::class)
    data class Split(
        val id: String = Uuid.random().toString(),
        val children: List<TilingNode>,
        val splitDirection: SplitDirection,
        override val ratio: Float = 0.5f,
    ) : TilingNode()
}

fun TilingNode.leavesId(): Set<String> = when (this) {
    is TilingNode.EmptyNode -> emptySet()
    is TilingNode.Leaf -> setOf(this.id)
    is TilingNode.Split -> this.children.flatMap { it.leavesId() }.toSet()
}

fun TilingNode.remove(id: String): TilingNode = when (this) {
    is TilingNode.EmptyNode -> this
    is TilingNode.Leaf -> if (this.id == id) TilingNode.EmptyNode() else this
    is TilingNode.Split -> {
        val newChildren = children
            .map { it.remove(id) }
            .filter { it !is TilingNode.EmptyNode }
        when {
            newChildren.isEmpty() -> TilingNode.EmptyNode()
            newChildren.size == 1 -> newChildren.first()
            else -> copy(children = newChildren)
        }
    }
}

enum class SplitDirection {
    Vertical, Horizontal
}
