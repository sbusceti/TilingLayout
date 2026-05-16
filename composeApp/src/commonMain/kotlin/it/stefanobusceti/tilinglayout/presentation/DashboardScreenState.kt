package it.stefanobusceti.tilinglayout.presentation

import it.stefanobusceti.tilinglayout.domain.TilingNode

data class DashboardScreenState(
    val isLoading: Boolean = false,
    val node: TilingNode = TilingNode.EmptyNode
)
