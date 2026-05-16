package it.stefanobusceti.tilinglayout.presentation

import androidx.lifecycle.ViewModel
import it.stefanobusceti.tilinglayout.domain.TilingNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DashboardViewModel : ViewModel() {
    private var _state = MutableStateFlow(
        DashboardScreenState(
            node = TilingNode.HSplit(
                leftNode = TilingNode.Leaf("1"),
                rightNode = TilingNode.VSplit(
                    topNode = TilingNode.Leaf("2"),
                    bottomNode = TilingNode.Leaf("3"),
                )
            )
        )
    )
    val state = _state.asStateFlow()

    fun onAction(action: DashboardScreenAction) {
        when (action) {
            is DashboardScreenAction.AddWidget -> {}
            is DashboardScreenAction.RemoveWidget -> {
                _state.update { it.copy(node = state.value.node.removeLeaf(action.widgetId)) }
            }
        }
    }
}