/*
 * Copyright (C) 2025 The Android Open Source Project
 * Copyright (C) 2026 crDroid Android Project
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

package com.android.systemui.shade.ui

import android.content.Context
import android.content.res.Configuration
import com.android.internal.graphics.ColorUtils
import com.android.internal.graphics.cam.Cam
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Glass colors of the shade, the lock screen bouncer and the notification scrim: near-neutral and
 * translucent, so the blurred wallpaper behind the shade carries the color.
 *
 * All numbers are in CAM16/HCT space (tone = L*, 0..100), so the visual weight is the same for
 * every wallpaper. sRGB blending (blendARGB) can't guarantee that.
 */
object ShadeColors {

    private class Spec(
        // Blur path: translucent panel laid over blurred content
        val panelTone: Float,
        val panelChroma: Float,
        val panelAlpha: Float,
        val maxAlpha: Float,
        // No-blur path: opaque panel
        val fallbackTone: Float,
        val fallbackChroma: Float,
        // Notification card/scrim tone offset from the panel, keeps the two layers apart
        val cardToneDelta: Float,
        val cardAlpha: Float,
    )

    private val NIGHT = Spec(8f, 4f, 0.40f, 0.72f, 10f, 4f, 8f, 0.30f)
    private val DAY = Spec(98f, 2f, 0.55f, 0.80f, 95f, 3f, -5f, 0.45f)

    private fun spec(context: Context): Spec = if (isNight(context)) NIGHT else DAY

    @JvmStatic
    fun shadePanel(context: Context, blurSupported: Boolean, withScrim: Boolean): Int {
        val s = spec(context)
        if (!blurSupported) return hct(context, s.fallbackChroma, s.fallbackTone, 1f)
        return legible(context, hct(context, s.panelChroma, s.panelTone, s.panelAlpha), s)
    }

    @JvmStatic
    fun notificationScrim(context: Context, blurSupported: Boolean): Int {
        val s = spec(context)
        return if (blurSupported) {
            hct(context, s.panelChroma, s.panelTone + s.cardToneDelta, s.cardAlpha)
        } else {
            // Opaque, and always a visible step away from the panel
            hct(context, s.fallbackChroma, s.fallbackTone + s.cardToneDelta, 1f)
        }
    }

    @JvmStatic
    fun shadePanelScrimBehind(context: Context): Int =
        context.resources.getColor(com.android.internal.R.color.shade_panel_scrim, context.theme)

    @JvmStatic
    fun classicShadeNotificationScrimBg(
        context: Context,
        blurSupported: Boolean,
        composited: Boolean,
    ): Int {
        val card = notificationScrim(context, blurSupported)
        if (!composited) return card
        return ColorUtils.compositeColors(card, shadePanel(context, blurSupported, true))
    }

    /** Raises the panel alpha until the text on it reaches 4.5:1 against the darkest backdrop. */
    private fun legible(context: Context, panel: Int, s: Spec): Int {
        val night = isNight(context)
        val text = context.getColor(
            if (night) android.R.color.system_neutral1_50 else android.R.color.system_neutral1_900)
        val backdrop = Cam.getInt(neutralHue(context), 6f, if (night) 82f else 22f)
        var alpha = (panel ushr 24) / 255f
        val opaque = ColorUtils.setAlphaComponent(panel, 255)
        while (alpha < s.maxAlpha) {
            val c = ColorUtils.setAlphaComponent(opaque, (alpha * 255).roundToInt())
            if (ColorUtils.calculateContrast(text, ColorUtils.compositeColors(c, backdrop)) >= 4.5) {
                break
            }
            alpha += 0.03f
        }
        return ColorUtils.setAlphaComponent(opaque, (min(alpha, s.maxAlpha) * 255).roundToInt())
    }

    private fun hct(context: Context, chroma: Float, tone: Float, alpha: Float): Int {
        val argb = Cam.getInt(neutralHue(context), chroma, tone.coerceIn(0f, 100f))
        return ColorUtils.setAlphaComponent(argb, (max(0f, min(1f, alpha)) * 255).roundToInt())
    }

    private fun neutralHue(context: Context): Float =
        Cam.fromInt(context.getColor(android.R.color.system_neutral1_500)).hue

    private fun isNight(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
}
