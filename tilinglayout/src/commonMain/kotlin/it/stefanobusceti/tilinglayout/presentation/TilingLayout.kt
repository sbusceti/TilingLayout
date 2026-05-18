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
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import it.stefanobusceti.tilinglayout.domain.TilingNode
import it.stefanobusceti.tilinglayout.domain.leafIds
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
    leafContent: @Composable (id: String) -> Unit,
) {

    val ratios = remember { mutableStateMapOf<String, Float>() }
    val leafs = remember { mutableMapOf<String, @Composable () -> Unit>() }

    leafs.keys.retainAll(node.leafIds().toSet())

    node.leafIds().forEach { id ->
        if (id !in leafs) {
            leafs[id] = movableContentOf { leafContent(id) }
        }
    }

    /** Emits leaf content and gap composables for every node in the tree, preserving traversal order. */
    @Composable
    fun Node(node: TilingNode) {
        when (node) {
            is TilingNode.HSplit -> {
                Node(node.leftNode)
                Gap(
                    color = gap.color,
                    gapType = GapType.VERTICAL,
                    currentRatio = { ratios[node.id] ?: node.ratio },
                    onRatioChanged = { newRatio ->
                        ratios[node.id] = newRatio
                    },
                    gapThickness = gap.thickness,
                    nodeId = node.id
                )
                Node(node.rightNode)
            }

            is TilingNode.Leaf -> {
                leafs[node.id]?.invoke()
            }

            is TilingNode.VSplit -> {
                Node(node.topNode)
                Gap(
                    color = gap.color,
                    gapType = GapType.HORIZONTAL,
                    currentRatio = { ratios[node.id] ?: node.ratio },
                    onRatioChanged = { newRatio -> ratios[node.id] = newRatio },
                    gapThickness = gap.thickness,
                    nodeId = node.id
                )
                Node(node.bottomNode)
            }

            TilingNode.EmptyNode -> {
                // no-op
            }
        }
    }

    val gapThicknessPx = with(LocalDensity.current) { gap.thickness.roundToPx() }

    Layout(
        content = { Node(node) }, modifier = modifier
    ) { measurables, constraints ->
        val leavesQueue = ArrayDeque(measurables.filter { it.layoutId != GAP_ID })
        val gapsQueue = ArrayDeque(measurables.filter { it.layoutId == GAP_ID })
        val placeableList = measureLeaf(node, constraints, leavesQueue, gapsQueue, gapThicknessPx, ratios)
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
 * [ratios] overrides the static ratio declared in each split node with the value dragged by the user;
 * falls back to [TilingNode.HSplit.ratio] / [TilingNode.VSplit.ratio] when no override exists.
 * Returns a flat list of (Placeable, offset) pairs ready to be placed by the parent layout.
 */
private fun measureLeaf(
    node: TilingNode,
    constraints: Constraints,
    leaves: ArrayDeque<Measurable>,
    gaps: ArrayDeque<Measurable>,
    gapThicknessPx: Int,
    ratios: Map<String, Float>
): List<PlacedNode> {
    val maxWidth = constraints.maxWidth
    val maxHeight = constraints.maxHeight

    when (node) {
        is TilingNode.HSplit -> {
            val currentRatio = ratios[node.id] ?: node.ratio
            val maxLeftWidth = (currentRatio * (maxWidth - gapThicknessPx)).roundToInt()
            val maxRightWidth = maxWidth - maxLeftWidth - gapThicknessPx
            val leftConstraints = Constraints(
                minWidth = 0,
                minHeight = 0,
                maxWidth = maxLeftWidth,
                maxHeight = constraints.maxHeight
            )
            val rightConstraints = leftConstraints.copy(maxWidth = maxRightWidth)
            val leftNode = measureLeaf(node.leftNode, leftConstraints, leaves, gaps, gapThicknessPx, ratios)
            val gap = measureGap(
                gapType = GapType.VERTICAL,
                gaps.removeFirst(),
                constraints,
                IntOffset(x = maxLeftWidth, y = 0),
                gapThicknessPx
            )
            val rightNode = measureLeaf(node.rightNode, rightConstraints, leaves, gaps, gapThicknessPx, ratios)
            val leftOffset = IntOffset(x = 0, y = 0)
            val rightOffset = IntOffset(x = maxLeftWidth + gapThicknessPx, y = 0)
            val translatedLeft = leftNode.map { node ->
                PlacedNode(node.placeable, node.offset + leftOffset)
            }
            val translatedRight = rightNode.map { node ->
                PlacedNode(node.placeable, node.offset + rightOffset)
            }
            return translatedLeft + listOf(gap) + translatedRight
        }

        is TilingNode.VSplit -> {
            val currentRatio = ratios[node.id] ?: node.ratio
            val maxTopHeight = (currentRatio * (maxHeight - gapThicknessPx)).roundToInt()
            val maxBottomHeight = maxHeight - maxTopHeight - gapThicknessPx
            val topConstraints = Constraints(
                minWidth = 0,
                minHeight = 0,
                maxWidth = constraints.maxWidth,
                maxHeight = maxTopHeight
            )
            val bottomConstraints = topConstraints.copy(maxHeight = maxBottomHeight)
            val topNode = measureLeaf(node.topNode, topConstraints, leaves, gaps, gapThicknessPx, ratios)
            val gap = measureGap(
                gapType = GapType.HORIZONTAL,
                gaps.removeFirst(),
                constraints,
                IntOffset(x = 0, y = maxTopHeight),
                gapThicknessPx
            )
            val bottomNode = measureLeaf(node.bottomNode, bottomConstraints, leaves, gaps, gapThicknessPx, ratios)
            val topOffset = IntOffset(x = 0, y = 0)
            val bottomOffset = IntOffset(x = 0, y = maxTopHeight + gapThicknessPx)
            val translatedTop = topNode.map { node ->
                PlacedNode(node.placeable, node.offset + topOffset)
            }
            val translatedBottom = bottomNode.map { node ->
                PlacedNode(node.placeable, node.offset + bottomOffset)
            }
            return translatedTop + listOf(gap) + translatedBottom
        }

        is TilingNode.Leaf -> {
            val measurable = leaves.removeFirst()
            val placeable = measurable.measure(constraints)
            return listOf(PlacedNode(placeable, IntOffset(0, 0)))
        }

        TilingNode.EmptyNode -> {
            return emptyList()
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
): PlacedNode = when (gapType) {
    GapType.HORIZONTAL -> {
        val placeable = measurable.measure(
            Constraints(
                minWidth = 0,
                minHeight = gapThicknessPx,
                maxWidth = constraints.maxWidth,
                maxHeight = gapThicknessPx
            )
        )
        PlacedNode(placeable, offset)
    }

    GapType.VERTICAL -> {
        val placeable = measurable.measure(
            Constraints(
                minWidth = gapThicknessPx,
                maxWidth = gapThicknessPx,
                minHeight = 0,
                maxHeight = constraints.maxHeight
            )
        )
        PlacedNode(placeable, offset)
    }
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
    currentRatio: () -> Float,
    color: Color = Color.Transparent,
    onRatioChanged: (Float) -> Unit,
    gapThickness: Dp
) {
    val modifier = Modifier.layoutId(GAP_ID)
    when (gapType) {
        GapType.HORIZONTAL -> {
            var totalHeight by remember { mutableStateOf(0) }
            HorizontalDivider(
                thickness = gapThickness,
                color = color,
                modifier = modifier.fillMaxWidth().onGloballyPositioned {
                    totalHeight = it.parentLayoutCoordinates?.size?.height ?: 0
                }.pointerHoverIcon(
                    icon = horizontalResizeCursor
                ).pointerInput(nodeId) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newRatio = (currentRatio() + dragAmount.y / totalHeight).coerceIn(0.1f, 0.9f)
                            onRatioChanged(newRatio)
                        })
                })
        }

        GapType.VERTICAL -> {
            var totalWidth by remember { mutableStateOf(0) }
            VerticalDivider(
                thickness = gapThickness,
                color = color,
                modifier = modifier.fillMaxHeight().onGloballyPositioned {
                    totalWidth = it.parentLayoutCoordinates?.size?.width ?: 0
                }.pointerHoverIcon(
                    icon = verticalResizeCursor
                ).pointerInput(nodeId) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newRatio = (currentRatio() + dragAmount.x / totalWidth).coerceIn(0.1f, 0.9f)
                            onRatioChanged(newRatio)
                        })
                })
        }
    }
}

/**
 * DSL-style overload of [TilingLayout] for statically-defined layouts.
 *
 * Use this overload when the pane structure is fixed at build time and you don't need
 * to mutate the tree at runtime. For dynamic layouts driven by external state, prefer
 * the `TilingLayout(node, leafContent)` overload, which lets you manage the [TilingNode]
 * tree yourself (add/remove/swap leaves, persist ratios, etc.).
 *
 * The [content] lambda is re-executed on every recomposition; leaf identity is based on
 * insertion order, so reordering leaves causes their content to be remapped accordingly.
 */
@Composable
fun TilingLayout(
    modifier: Modifier = Modifier.fillMaxSize(),
    gap: GapDefaults = GapDefaults(),
    content: TilingLayoutScope.() -> Unit,
) {
    val scope = remember { TilingLayoutScopeImpl() }
    scope.content()

    TilingLayout(
        node = scope.buildNode(),
        modifier = modifier,
        gap = gap,
        leafContent = { id -> scope.getLeafContent(id)?.invoke() }
    )
}

/** Direction of the divider between two adjacent panes. */
private enum class GapType {
    HORIZONTAL, VERTICAL
}
