package it.stefanobusceti.tilinglayout.domain

/** Direction in which a new leaf is inserted relative to a target when calling [TilingNode.addLeaf]. */
enum class SplitArea {
    Top,
    Bottom,
    Left,
    Right,
}
