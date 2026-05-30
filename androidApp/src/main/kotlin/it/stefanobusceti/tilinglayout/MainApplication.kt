package it.stefanobusceti.tilinglayout

import android.app.Application
import it.stefanobusceti.tilinglayout.di.initKoin

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin()
    }
}