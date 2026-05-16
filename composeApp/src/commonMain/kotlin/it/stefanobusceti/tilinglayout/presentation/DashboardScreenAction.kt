package it.stefanobusceti.tilinglayout.presentation

sealed interface DashboardScreenAction {
    data class AddWidget(val widgetId: String) : DashboardScreenAction
    data class RemoveWidget(val widgetId: String) : DashboardScreenAction
}