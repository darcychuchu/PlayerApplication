package org.vlog.app

import android.app.Application
import org.vlog.app.data.AppContainer

class VlogApplication : Application(){

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}