package org.vlog.app.data

import android.app.Application


class AppContainer(private val context: Application){
	val localMediaProvider by lazy {
		LocalMediaProvider(applicationContext = context)
	}
}