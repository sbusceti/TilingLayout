package it.stefanobusceti.tilinglayout.presentation

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import it.stefanobusceti.tilinglayout.domain.SplitDirection
import it.stefanobusceti.tilinglayout.domain.TilingNode
import it.stefanobusceti.tilinglayout.domain.collectSplitIds
import it.stefanobusceti.tilinglayout.domain.leavesId
import kotlin.math.roundToInt

private const val GAP_ID = "gap"

private data class PlacedNode(
    val placeable: Placeable,
    val offset: IntOffset
)

/**
 * Lays out composable content according to a [TilingNode] tree.
 *
 * [leafContent] is called once per [TilingNode.Leaf], receiving its [TilingNode.Leaf.id].
 * Leaves are visited depth-first, left-before-right and top-before-bottom.
 * A draggable Material3 divider of [gapThickness] and [gapColor] is inserted between each pair of
 * adjacent panes; dragging it updates the split ratio at runtime without modifying the original [node] tree.
 * Passing `null` renders an empty layout, useful when all leaves have been removed.
 */
@Composable
fun TilingLayout(
    node: TilingNode,
    modifier: Modifier = Modifier.fillMaxSize(),
    gap: GapDefaults = GapDefaults(),
    onRatiosChanged: (Map<String, List<Float>>) -> Unit = {},
    leafContent: @Composable (id: String) -> Unit,
) {

    val ratios = remember { mutableStateMapOf<String, List<Float>>() }
    val leafs = remember { mutableStateMapOf<String, @Composable () -> Unit>() }
    val splitSizes = remember { mutableStateMapOf<String, Int>() }

    LaunchedEffect(node) {
        val splitIds = node.collectSplitIds()
        ratios.keys.retainAll(splitIds)
        splitSizes.keys.retainAll(splitIds)
        leafs.keys.retainAll(node.leavesId().toSet())
    }


    node.leavesId().forEach { id ->
        key(id) {
            if (id !in leafs) {
                leafs[id] = movableContentOf {
                    leafContent(id)
                }
            }
        }
    }

    /** Emits leaf content and gap composables for every node in the tree, preserving traversal order. */
    @Composable
    fun Node(node: TilingNode) {
        when (node) {
            is TilingNode.Leaf -> {
                leafs[node.id]?.invoke()
            }

            is TilingNode.EmptyNode -> {
                // no-op
            }

            is TilingNode.Split -> {
                node.children.forEachIndexed { index, child ->
                    Node(child)
                    if (index < node.children.size - 1) {
                        Gap(
                            nodeId = node.id,
                            gapType = when (node.splitDirection) {
                                SplitDirection.Horizontal -> GapType.VERTICAL
                                SplitDirection.Vertical -> GapType.HORIZONTAL
                            },
                            onRatioChanged = { delta ->
                                val currentRatios = ratios[node.id]
                                    ?.takeIf { it.size == node.children.size }
                                    ?: node.children.map { it.ratio }
                                val newRatios = currentRatios.toMutableList()
                                newRatios[index] = (newRatios[index] + delta).coerceAtLeast(0.05f)
                                newRatios[index + 1] = (newRatios[index + 1] - delta).coerceAtLeast(0.05f)
                                ratios[node.id] = newRatios
                            },
                            gapThickness = gap.thickness,
                            sizeProvider = { splitSizes[node.id] ?: 1 },
                            onDragEnd = {
                                onRatiosChanged(ratios)
                            }
                        )
                    }
                }
            }
        }
    }

    val gapThicknessPx = with(LocalDensity.current) { gap.thickness.roundToPx() }

    Layout(
        content = { Node(node) }, modifier = modifier
    ) { measurables, constraints ->
        val leavesQueue = ArrayDeque(measurables.filter { it.layoutId != GAP_ID })
        val gapsQueue = ArrayDeque(measurables.filter { it.layoutId == GAP_ID })
        val placeableList = measureLeaf(
            node = node,
            constraints = constraints,
            leaves = leavesQueue,
            gaps = gapsQueue,
            gapThicknessPx = gapThicknessPx,
            ratios = ratios,
            splitSizes = splitSizes
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeableList.forEach { node ->
                node.placeable.placeRelative(node.offset.x, node.offset.y)
            }
        }
    }
}

/**
 * Recursively measures and positions every leaf and gap in [node] within the given [constraints].
 *
 * [leaves] and [gaps] are consumed depth-first (left/top first) and must each contain
 * exactly one entry per [TilingNode.Leaf] and per split node respectively.
 * Gap space ([gapThicknessPx]) is subtracted from the available area before distributing
 * the remainder according to each split's ratio.
 * [ratios] overrides the static ratio declared in each [TilingNode.Split]'s children with the
 * user-dragged values; falls back to each child's [TilingNode.ratio] when no override exists.
 * Returns a flat list of (Placeable, offset) pairs ready to be placed by the parent layout.
 */
private fun measureLeaf(
    node: TilingNode,
    constraints: Constraints,
    leaves: ArrayDeque<Measurable>,
    gaps: ArrayDeque<Measurable>,
    gapThicknessPx: Int,
    ratios: Map<String, List<Float>>,
    offsetX: Int = 0,
    offsetY: Int = 0,
    splitSizes: MutableMap<String, Int>
): List<PlacedNode> {
    val maxWidth = constraints.maxWidth
    val maxHeight = constraints.maxHeight

    when (node) {
        is TilingNode.Leaf -> {
            val measurable = leaves.removeFirst()
            val placeable = measurable.measure(constraints)
            return listOf(PlacedNode(placeable, IntOffset(offsetX, offsetY)))
        }

        is TilingNode.EmptyNode -> {
            return emptyList()
        }

        is TilingNode.Split -> {
            val isHorizontal = node.splitDirection == SplitDirection.Horizontal
            splitSizes[node.id] = if (isHorizontal) maxWidth else maxHeight
            val availableSpace = if (isHorizontal) {
                maxWidth - gapThicknessPx * (node.children.count() - 1)
            } else {
                maxHeight - gapThicknessPx * (node.children.count() - 1)
            }

            val placedNodes = mutableListOf<PlacedNode>()
            var currentOffset = if (isHorizontal) offsetX else offsetY
            val currentRatios = ratios[node.id]
                ?.takeIf { it.size == node.children.size }
                ?: node.children.map { it.ratio }

            val ratioSum = currentRatios.sum()
            val normalizedRatios = currentRatios.map { it / ratioSum }

            node.children.forEachIndexed { index, childNode ->
                val childSize = (availableSpace * normalizedRatios[index]).roundToInt()
                val childConstraints = if (isHorizontal) {
                    Constraints(minWidth = 0, minHeight = 0, maxWidth = childSize, maxHeight = maxHeight)
                } else {
                    Constraints(minWidth = 0, minHeight = 0, maxWidth = maxWidth, maxHeight = childSize)
                }
                val nodes = measureLeaf(
                    node = childNode,
                    constraints = childConstraints,
                    leaves = leaves,
                    gaps = gaps,
                    gapThicknessPx = gapThicknessPx,
                    ratios = ratios,
                    offsetX = if (isHorizontal) currentOffset else offsetX,
                    offsetY = if (isHorizontal) offsetY else currentOffset,
                    splitSizes = splitSizes
                )
                placedNodes.addAll(nodes)
                if (index < node.children.count() - 1) {
                    currentOffset += childSize
                    val gapOffset = if (isHorizontal) {
                        IntOffset(currentOffset, offsetY)
                    } else {
                        IntOffset(offsetX, currentOffset)
                    }
                    placedNodes.add(
                        measureGap(
                            gapType = if (isHorizontal) GapType.VERTICAL else GapType.HORIZONTAL,
                            measurable = gaps.removeFirst(),
                            constraints = Constraints(
                                minWidth = 0,
                                minHeight = 0,
                                maxWidth = if (isHorizontal) gapThicknessPx else maxWidth,
                                maxHeight = if (isHorizontal) maxHeight else gapThicknessPx
                            ),
                            gapThicknessPx = gapThicknessPx,
                            offset = gapOffset,
                        )
                    )
                    currentOffset += gapThicknessPx
                }
            }
            return placedNodes
        }
    }
}

/** Measures a divider composable and returns it paired with its absolute [offset]. */
private fun measureGap(
    gapType: GapType,
    measurable: Measurable,
    constraints: Constraints,
    offset: IntOffset,
    gapThicknessPx: Int,
): PlacedNode {
    val placeable = when (gapType) {
        GapType.HORIZONTAL -> measurable.measure(
            Constraints(
                minWidth = 0,
                minHeight = gapThicknessPx,
                maxWidth = constraints.maxWidth,
                maxHeight = gapThicknessPx
            )
        )

        GapType.VERTICAL -> measurable.measure(
            Constraints(
                minWidth = gapThicknessPx,
                minHeight = 0,
                maxWidth = gapThicknessPx,
                maxHeight = constraints.maxHeight
            )
        )
    }
    return PlacedNode(placeable, offset)
}

/**
 * Emits a draggable [HorizontalDivider] or [VerticalDivider] tagged with [GAP_ID].
 * Dragging updates [currentRatio] via [onRatioChanged], clamped to [0.1, 0.9].
 * The cursor changes to a resize icon on hover to signal interactivity.
 */
@Composable
private fun Gap(
    nodeId: String,
    gapType: GapType,
    color: Color = Color.Transparent,
    onRatioChanged: (Float) -> Unit,
    gapThickness: Dp,
    sizeProvider: () -> Int,
    onDragEnd: () -> Unit,
) {
    val currentOnRatioChanged by rememberUpdatedState(onRatioChanged)

    val modifier = Modifier.layoutId(GAP_ID)
    when (gapType) {
        GapType.HORIZONTAL -> {
            HorizontalDivider(
                thickness = gapThickness,
                color = color,
                modifier = modifier.fillMaxWidth()
                    .pointerHoverIcon(
                        icon = horizontalResizeCursor
                    ).pointerInput(nodeId) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentOnRatioChanged(dragAmount.y / sizeProvider())
                            },
                            onDragEnd = onDragEnd,
                        )
                    })
        }

        GapType.VERTICAL -> {
            VerticalDivider(
                thickness = gapThickness,
                color = color,
                modifier = modifier.fillMaxHeight()
                    .pointerHoverIcon(
                        icon = verticalResizeCursor
                    ).pointerInput(nodeId) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentOnRatioChanged(dragAmount.x / sizeProvider())
                            },
                            onDragEnd = onDragEnd,
                        )
                    })
        }
    }
}

/** Direction of the divider between two adjacent panes. */
private enum class GapType {
    HORIZONTAL, VERTICAL
}
