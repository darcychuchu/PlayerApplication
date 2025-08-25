package org.vlog.app.player

import android.app.Application
import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor(
) : ViewModel() {

    var toastMessage: MutableLiveData<String?> = MutableLiveData(null)

    companion object {
        const val SAME_AS_INPUT_OPTION = "same as input"
        const val LAYOUT_EXTRA = "video_layout"
        private const val TAG = "CompPreviewVM"
    }
}