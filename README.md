# TilingLayout

A **Compose Multiplatform** layout that arranges content as a tree of recursive horizontal and vertical splits — like a tiling window manager, but for Compose UI.

Designed primarily for **desktop applications** that need a user-customisable dashboard (think IDE panels, analytics dashboards, or any multi-pane workspace), but it runs on every platform Compose Multiplatform supports.

![Demo](tiling_layout.mp4)

---

## Supported platforms

| Platform | Target |
|---|---|
| JVM / Desktop | Compose for Desktop — primary target |
| Android | minSdk 24 |
| iOS | iosX64 · iosArm64 · iosSimulatorArm64 |
| JS | Kotlin/JS (browser) |
| WasmJS | Kotlin/Wasm (browser) |

---

## Installation

The library is published on [JitPack](https://jitpack.io).

Add the JitPack repository to your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        // ...
        maven("https://jitpack.io")
    }
}
```

Then add the dependency in your module's `build.gradle.kts`:

```kotlin
commonMain.dependencies {
    implementation("com.github.sbusceti.TilingLayout:tilinglayout:2.0.2")
}
```

---

## Quick start

Pass a `TilingNode` tree and a lambda that renders each leaf pane:

```kotlin
var node by remember {
    mutableStateOf<TilingNode>(
        TilingNode.Split(
            splitDirection = SplitDirection.Horizontal,
            children = listOf(
                TilingNode.Leaf("editor"),
                TilingNode.Leaf("preview"),
            )
        )
    )
}

TilingLayout(
    node = node,
    onRatiosChanged = { ratios -> node = node.updateRatios(ratios) },
) { id ->
    Pane(
        title = id,
        onClose = { node = node.remove(id) },
    )
}
```

---

## TilingNode tree

`TilingNode` is an **immutable sealed class** that describes the pane layout as a tree.

| Node | Description |
|---|---|
| `Leaf(id, ratio)` | A terminal pane. `id` links it to its composable content. |
| `Split(children, splitDirection, ratio)` | An internal node that divides space among 2 or more children. `splitDirection` is `Horizontal` (left-to-right) or `Vertical` (top-to-bottom). |
| `EmptyNode` | An empty layout — useful as the initial state before any pane is added. |

`ratio` on each node is its relative weight within its parent `Split`; siblings are normalised at layout time so absolute values are arbitrary.

### Tree operations

All operations return a **new tree**; the original is never mutated.

```kotlin
// Add a new pane adjacent to an existing leaf
node = node.add(
    id = "terminal",
    targetNodeId = "editor",   // ID of the leaf to split
    splitArea = SplitArea.Bottom,  // Top | Bottom | Left | Right
)

// Remove a pane — its sibling absorbs the freed space
node = node.remove("terminal")

// Swap two leaf IDs (structure and ratios are preserved; only IDs move)
node = node.swapLeaves("editor", "preview")

// Apply user-dragged ratios back into the tree
node = node.updateRatios(ratios)

// Collect all leaf IDs
val ids: Set<String> = node.leavesId()
```

---

## Draggable dividers

A draggable Material3 divider is rendered between each pair of adjacent panes. Dragging it updates the split ratio at runtime **without modifying the `TilingNode` tree** — overridden ratios are stored in internal `remember` state, keyed by each `Split`'s auto-generated `id`.

When the user finishes dragging, the `onRatiosChanged` callback fires with the full updated ratio map (`Map<String, List<Float>>`, keyed by `Split.id`). Call `node.updateRatios(ratios)` to write the new ratios back into the canonical tree.

Divider appearance is controlled via `GapDefaults`:

```kotlin
TilingLayout(
    node = node,
    gap = GapDefaults(
        thickness = 8.dp,
        color = Color.Transparent,
    ),
    onRatiosChanged = { ratios -> node = node.updateRatios(ratios) },
) { id -> /* ... */ }
```

The cursor changes to a resize icon on hover on JVM/Desktop. On other platforms (Android, iOS, JS, Wasm) the cursor falls back to the default pointer.

---

## Persisting the layout

Because `TilingNode` is a plain data class hierarchy, the full layout — structure and pane sizes — can be serialised to disk (e.g. JSON or DataStore) and restored on the next app launch:

```kotlin
// After a drag ends, persist the updated tree
onRatiosChanged = { ratios ->
    val updatedNode = node.updateRatios(ratios)
    node = updatedNode
    saveLayoutToDisk(updatedNode)   // serialise to JSON / DataStore / etc.
}

// On startup, reload the saved tree instead of the default
node = loadLayoutFromDisk() ?: defaultNode
```

---

## Example: ViewModel-driven layout (MVI)

```kotlin
// State
data class DashboardState(
    val node: TilingNode = TilingNode.EmptyNode(),
)

// Actions
sealed interface DashboardAction {
    data class AddPane(val id: String, val targetId: String, val area: SplitArea) : DashboardAction
    data class RemovePane(val id: String) : DashboardAction
    data class SwapPanes(val srcId: String, val dstId: String) : DashboardAction
    data class UpdateRatios(val ratios: Map<String, List<Float>>) : DashboardAction
}

// ViewModel
class DashboardViewModel : ViewModel() {
    private val _state = MutableStateFlow(
        DashboardState(
            node = TilingNode.Split(
                splitDirection = SplitDirection.Horizontal,
                children = listOf(
                    TilingNode.Leaf("sidebar"),
                    TilingNode.Leaf("main"),
                )
            )
        )
    )
    val state = _state.asStateFlow()

    fun onAction(action: DashboardAction) {
        _state.update {
            it.copy(
                node = when (action) {
                    is DashboardAction.AddPane ->
                        it.node.add(action.id, action.targetId, action.area)
                    is DashboardAction.RemovePane ->
                        it.node.remove(action.id)
                    is DashboardAction.SwapPanes ->
                        it.node.swapLeaves(action.srcId, action.dstId)
                    is DashboardAction.UpdateRatios ->
                        it.node.updateRatios(action.ratios)
                }
            )
        }
    }
}
```

Wire it to your composable:

```kotlin
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    TilingLayout(
        node = state.node,
        onRatiosChanged = { viewModel.onAction(DashboardAction.UpdateRatios(it)) },
    ) { id ->
        Pane(
            title = id,
            onClose = { viewModel.onAction(DashboardAction.RemovePane(id)) }
        )
    }
}
```

---

## License

[Apache 2.0](LICENSE)

---

> This library was built as a final project for the [Advanced Compose](https://kt.academy) course on **kt.academy**.
