package it.stefanobusceti.tilinglayout.di

import it.stefanobusceti.tilinglayout.presentation.DashboardViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    viewModelOf(::DashboardViewModel)
}