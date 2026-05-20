package it.stefanobusceti.tilinglayout.presentation

import androidx.lifecycle.ViewModel
import it.stefanobusceti.tilinglayout.domain.SplitDirection
import it.stefanobusceti.tilinglayout.domain.TilingNode
import it.stefanobusceti.tilinglayout.domain.remove
import it.stefanobusceti.tilinglayout.domain.updateRatios
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DashboardViewModel : ViewModel() {
    private var _state = MutableStateFlow(
        DashboardScreenState(
            node = TilingNode.Split(
                splitDirection = SplitDirection.Horizontal,
                children = listOf(
                    TilingNode.Leaf("1"),
                    TilingNode.Split(
                        splitDirection = SplitDirection.Vertical,
                        children = listOf(
                            TilingNode.Leaf("2"),
                            TilingNode.Leaf("3"),
                            TilingNode.Leaf("4"),
                        )
                    ),
                )
            )
        )
    )
    val state = _state.asStateFlow()

    fun onAction(action: DashboardScreenAction) {
        when (action) {
            is DashboardScreenAction.AddWidget -> {}
            is DashboardScreenAction.RemoveWidget -> {
                _state.update { it.copy(node = state.value.node.remove(action.widgetId)) }
            }

            is DashboardScreenAction.UpdateRatios -> {
                _state.update { it.copy(node = state.value.node.updateRatios(action.ratios)) }
                //_state.value.node.updateRatios(action.ratios)
            }
        }
    }
}