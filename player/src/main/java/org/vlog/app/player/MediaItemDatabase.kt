/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vlog.app.player

import androidx.media3.common.MediaItem

class MediaItemDatabase {
  val mediaUris =
    mutableListOf(
      "https://v14.daayee.com/yyv14/202508/10/93CaCjXnHL19/video/index.m3u8",
      "https://v14.daayee.com/yyv14/202508/10/q1PhJADdat20/video/index.m3u8",
      "https://v13.daayee.com/yyv13/202508/10/uqpX2URe4D21/video/index.m3u8",
      "https://v13.daayee.com/yyv13/202508/05/YDH9XSQWeZ20/video/index.m3u8",
      "https://v13.daayee.com/yyv13/202507/29/F2srUMD1rK22/video/index.m3u8",
      "https://v14.daayee.com/yyv14/202507/23/uWqUJnXsH522/video/index.m3u8"
    )

  fun get(index: Int): MediaItem {
    val uri = mediaUris[index.mod(mediaUris.size)]
    return MediaItem.Builder().setUri(uri).setMediaId(index.toString()).build()
  }
}
