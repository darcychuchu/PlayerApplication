package org.vlog.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.vlog.app.data.AppContainer

@HiltAndroidApp
class VlogApplication : Application(){

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}