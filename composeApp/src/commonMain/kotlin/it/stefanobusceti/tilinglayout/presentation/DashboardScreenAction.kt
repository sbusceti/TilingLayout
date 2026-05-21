package it.stefanobusceti.tilinglayout.presentation

import it.stefanobusceti.tilinglayout.domain.SplitArea

sealed interface DashboardScreenAction {
    data class AddWidget(val widgetId: String, val targetId: String, val splitArea: SplitArea) : DashboardScreenAction
    data class RemoveWidget(val widgetId: String) : DashboardScreenAction
    data class UpdateRatios(val ratios: Map<String, List<Float>>) : DashboardScreenAction
}