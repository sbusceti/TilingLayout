package it.stefanobusceti.tilinglayout.domain

/**
 * Direction in which a new [TilingNode.Leaf] is inserted relative to an existing target leaf
 * when calling [TilingNode.add].
 *
 * The direction also determines the [SplitDirection] of the new [TilingNode.Split] that wraps
 * the two leaves:
 * - [Top] / [Bottom] create a [SplitDirection.Vertical] split.
 * - [Left] / [Right] create a [SplitDirection.Horizontal] split.
 */
enum class SplitArea {
    /** New leaf is placed above the target; creates a vertical split. */
    Top,
    /** New leaf is placed below the target; creates a vertical split. */
    Bottom,
    /** New leaf is placed to the left of the target; creates a horizontal split. */
    Left,
    /** New leaf is placed to the right of the target; creates a horizontal split. */
    Right,
}
