package it.stefanobusceti.tilinglayout.domain

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
            newChildren.size == 1 -> when (val survivor = newChildren.first()) {
                is TilingNode.Leaf -> survivor.copy(ratio = this.ratio)
                is TilingNode.Split -> survivor.copy(ratio = this.ratio)
                is TilingNode.EmptyNode -> survivor
            }

            else -> copy(children = newChildren)
        }
    }
}

fun TilingNode.updateRatios(ratios: Map<String, List<Float>>): TilingNode = when (this) {
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

fun TilingNode.collectSplitIds(): Set<String> = when (this) {
    is TilingNode.Split -> setOf(id) + children.flatMap { it.collectSplitIds() }
    else -> emptySet()
}