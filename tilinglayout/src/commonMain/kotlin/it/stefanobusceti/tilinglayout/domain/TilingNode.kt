package it.stefanobusceti.tilinglayout.domain

import androidx.compose.runtime.Stable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Immutable sealed class hierarchy representing a tiling layout tree.
 *
 * A tree is built from [Leaf] terminal nodes (rendered panes) and [Split] internal nodes
 * (divides space among 2 or more children). [EmptyNode] represents the absence of content.
 *
 * All tree operations (e.g., [remove], [updateRatios]) are pure functions that return a new
 * tree without mutating the original.
 *
 * @property ratio Relative weight within the parent [Split]. Only the ratios of siblings matter
 * relative to each other; they are normalised at layout time so absolute values are arbitrary.
 */
@Stable
sealed class TilingNode {
    abstract val ratio: Float

    /** Signals that no layout should be rendered. Used as the initial/empty state. */
    data class EmptyNode(override val ratio: Float = 0f) : TilingNode()

    /**
     * A terminal pane identified by [id].
     *
     * [id] must be unique within the tree; it is used to key leaf content in
     * `movableContentOf` so that content survives structural tree changes.
     */
    data class Leaf(
        val id: String,
        override val ratio: Float = 0.5f
    ) : TilingNode()

    /**
     * An internal node that splits its available space among [children] along [splitDirection].
     *
     * [id] defaults to a random UUID and is used to key runtime state such as drag-overridden
     * ratios and measured split sizes.
     * [children] must contain at least 2 nodes; a split with fewer children is collapsed by
     * tree operations such as [remove].
     */
    @OptIn(ExperimentalUuidApi::class)
    data class Split(
        val id: String = Uuid.random().toString(),
        val children: List<TilingNode>,
        val splitDirection: SplitDirection,
        override val ratio: Float = 0.5f,
    ) : TilingNode()
}

/** Axis along which a [TilingNode.Split] divides its available space. */
enum class SplitDirection {
    /** Children are stacked top-to-bottom; each child fills the full width. */
    Vertical,

    /** Children are placed left-to-right; each child fills the full height. */
    Horizontal,
}
