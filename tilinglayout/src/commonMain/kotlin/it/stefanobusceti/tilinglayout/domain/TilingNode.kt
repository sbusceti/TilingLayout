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

enum class SplitDirection {
    Vertical, Horizontal
}
