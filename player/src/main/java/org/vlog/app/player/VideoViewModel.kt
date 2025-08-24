package org.vlog.app.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem

class VideoViewModel(
    application: Application,
    val compositionLayout: String
) : AndroidViewModel(application) {

    var toastMessage: MutableLiveData<String?> = MutableLiveData(null)

    companion object {
        const val SAME_AS_INPUT_OPTION = "same as input"
        const val LAYOUT_EXTRA = "composition_layout"
        private const val TAG = "CompPreviewVM"
    }
}


class VideoViewModelFactory(
    private val application: Application,
    private val compositionLayout: String,
) : ViewModelProvider.Factory {

}