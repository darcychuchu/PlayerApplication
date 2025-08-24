package org.vlog.app.player

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi

class VideoViewModel(
    application: Application,
    val compositionLayout: String
) : AndroidViewModel(application) {

    var toastMessage: MutableLiveData<String?> = MutableLiveData(null)

    override fun onCleared() {
        super.onCleared()
    }

    companion object {
        const val SAME_AS_INPUT_OPTION = "same as input"
        const val LAYOUT_EXTRA = "video_layout"
        private const val TAG = "CompPreviewVM"
    }
}

@OptIn(UnstableApi::class)
class VideoViewModelFactory (
    private val application: Application,
    private val compositionLayout: String,
) : ViewModelProvider.Factory {
    init {
        Log.d(TAG, "-----------------------------------------------Creating ViewModel with $compositionLayout")
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return VideoViewModel(application, compositionLayout) as T
    }

    companion object {
        private const val TAG = "CPVMF"
    }
}