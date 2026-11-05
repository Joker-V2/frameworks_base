/*
 * Copyright (C) 2026 The Joker Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.qs.panels.ui.compose.infinitegrid

import android.database.ContentObserver
import android.os.UserHandle
import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

/*
 * Shared pieces of the Quick Settings panel style. The style is the only one of this build, so
 * there is no setting that selects it.
 */

/** White of an active bubble on a dark panel, a touch short of opaque. */
internal val ColorOSActiveFill = Color(0xF2FFFFFF)

/**
 * The one accent of the panel: the icon of a tile that is on, the volume icon and the ringer.
 * It follows the wallpaper (Monet) like the rest of the system colors, and is never hardcoded.
 * Everything else is glass and white. It always sits on a white fill, so it is the dark tone of the
 * palette: the primary of a light panel, the primary container of a dark one.
 */
@Composable
@ReadOnlyComposable
internal fun colorOSAccent(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (scheme.surface.luminance() < 0.5f) scheme.primaryContainer else scheme.primary
}

/** White fill of the sliders, which lets a little of the glass show through. */
internal val ColorOSSliderFill = Color(0xF7FFFFFF)

/**
 * Glass rim of the tiles, lit from the top left and the bottom right with the sides fading out in
 * between. Created once instead of on every frame of the QS animation.
 */
internal val ColorOSTileRim =
    Brush.linearGradient(
        0f to Color.White.copy(alpha = 0.30f),
        0.3f to Color.White.copy(alpha = 0.05f),
        0.7f to Color.White.copy(alpha = 0.05f),
        1f to Color.White.copy(alpha = 0.22f),
    )

/**
 * Diameter of the round tiles, which is also the height of the pills and the width of the sliders.
 * The expanded panel sets it to the width of one of its four columns. Everywhere else (QQS, edit
 * mode) it keeps the fixed default.
 */
internal val LocalColorOSTileSize = compositionLocalOf { CommonTileDefaults.ColorOSTileHeight }

/** The panel style is always on. Kept as a function so the call sites read the same everywhere. */
@Composable fun rememberQSColorOSStyle(): Boolean = true

/** The labels below the round tiles are always shown. */
@Composable fun rememberQSTileLabelHide(): Boolean = false

private const val QS_COLOROS_MEDIA_ART_FILL = "qs_coloros_media_art_fill"

/** Whether the media card fills with the album art (1) or shows it as a thumbnail (0). */
@Composable
fun rememberQSColorOSMediaArtFill(): Boolean {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    fun read(): Boolean =
        runCatching {
                Settings.System.getIntForUser(
                    contentResolver,
                    QS_COLOROS_MEDIA_ART_FILL,
                    0,
                    UserHandle.USER_CURRENT,
                ) != 0
            }
            .getOrDefault(false)

    var enabled by remember { mutableStateOf(read()) }

    DisposableEffect(contentResolver) {
        val observer =
            object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    context.mainExecutor.execute { enabled = read() }
                }
            }
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(QS_COLOROS_MEDIA_ART_FILL),
            false,
            observer,
            UserHandle.USER_ALL,
        )
        onDispose { contentResolver.unregisterContentObserver(observer) }
    }

    return enabled
}
