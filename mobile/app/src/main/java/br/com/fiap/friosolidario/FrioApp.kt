package br.com.fiap.friosolidario

import android.app.Application
import br.com.fiap.friosolidario.core.AppContainer

class FrioApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
