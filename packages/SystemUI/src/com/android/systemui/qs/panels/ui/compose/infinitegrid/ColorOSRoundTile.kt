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

import android.content.Context
import android.service.quicksettings.Tile.STATE_ACTIVE
import android.service.quicksettings.Tile.STATE_INACTIVE
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.systemui.common.shared.model.Icon
import com.android.systemui.qs.panels.ui.viewmodel.TileUiState
import com.android.systemui.res.R

/*
 * The round tiles of the panel: a circle that carries the icon and a label under it.
 *
 *  - a tile that is off is glass, a tile that is on is a white disc with the red icon, the only
 *    color of the panel, no tile has one of its own
 *  - the label is a little tighter and heavier than the stock label, and dims with the tile
 */

/** Corner of a round tile, as a share of its side. 50 is a circle, less is a squircle. */
private const val RoundTileCornerPercent = 50

/** The shape of a round tile. */
internal val ColorOSRoundTileShape = RoundedCornerShape(percent = RoundTileCornerPercent)

/** How much of a tile that cannot be used is left. */
internal const val ColorOSUnavailableAlpha = 0.45f

/**
 * Colors of a tile of the panel. A tile that is on is white with the red accent icon on both
 * themes, which keeps it readable on a light wallpaper as well. A tile that is off is glass.
 */
@Composable
@ReadOnlyComposable
internal fun colorOSColorsForState(uiState: TileUiState, iconOnly: Boolean): TileColors {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val darkPanel = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val glass = if (darkPanel) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.55f)
    val bubbleOff =
        if (darkPanel) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.06f)
    val primary = if (darkPanel) onSurface.copy(alpha = 0.92f) else onSurface
    val secondary = onSurface.copy(alpha = if (darkPanel) 0.60f else 0.70f)
    val inactive =
        TileColors(
            background = glass,
            iconBackground = bubbleOff,
            label = primary,
            secondaryLabel = secondary,
            icon = onSurface,
        )
    return when (uiState.visualState) {
        // A tile that is on is a white disc with the red icon, on both themes.
        STATE_ACTIVE ->
            TileColors(
                background = if (iconOnly) ColorOSActiveFill else glass,
                iconBackground = ColorOSActiveFill,
                label = primary,
                secondaryLabel = secondary,
                icon = colorOSAccent(),
            )
        STATE_INACTIVE -> inactive
        // A round tile that cannot be used keeps the colors of one that is off and is faded as a
        // whole by the caller. The wide tiles keep the greyed out colors.
        else -> if (iconOnly) inactive else inactive.copy(icon = onSurface.copy(alpha = 0.38f))
    }
}

/**
 * A round tile: the circle with its icon, and the label under it unless [labelHide].
 *
 * The tile is [LocalColorOSTileSize] wide and tall. The label comes on top of that, in the
 * [CommonTileDefaults.ClassicLabelHeight] that the cell leaves for it.
 */
@Composable
internal fun ColorOSRoundTileContent(
    label: String,
    iconProvider: Context.() -> Icon,
    colors: TileColors,
    spec: String,
    active: Boolean,
    labelHide: Boolean,
    chevron: Boolean,
    modifier: Modifier = Modifier,
) {
    val size = LocalColorOSTileSize.current
    val glass = LocalColorOSGlassBlur.current

    // The fill of a tile that is on fades in over the glass instead of replacing it, so the rim of
    // the glass still shows around the accent.
    val onAmount by animateFloatAsState(if (active) 1f else 0f, label = "ColorOSRoundTileOn")
    val animatedOffFill by animateColorAsState(colors.iconBackground, label = "ColorOSRoundTileOff")
    val glassModifier =
        if (glass != null) {
            Modifier.colorOSGlassBlur(
                ColorOSRoundTileShape,
                if (active) colorOSGlassActiveTileMaterial() else colorOSGlassTileMaterial(),
            )
        } else {
            Modifier
        }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.size(size)
                    .then(glassModifier)
                    .clip(ColorOSRoundTileShape)
                    .drawBehind {
                        // A flat disc under the accent only where there is no glass to show.
                        if (glass == null) drawRect(animatedOffFill)
                        if (onAmount > 0f) drawRect(ColorOSActiveFill, alpha = onAmount)
                    }
                    .border(1.dp, ColorOSTileRim, ColorOSRoundTileShape),
        ) {
            // A bigger tile (the collapsed panel) gets a proportionally bigger icon.
            val iconSize = maxOf(CommonTileDefaults.SmallTileIconSize, size * 0.38f)
            val animated =
                colorOSTileLottie(
                    spec = spec,
                    active = active,
                    offColor = MaterialTheme.colorScheme.onSurface,
                    onColor = colors.icon,
                    size = iconSize,
                    modifier = Modifier.align(Alignment.Center),
                )
            if (!animated) {
                SmallTileContent(
                    iconProvider = iconProvider,
                    color = colors.icon,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }

        if (!labelHide) {
            val labelColor by animateColorAsState(colors.label, label = "ColorOSRoundTileLabel")
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Text(
                    text = label,
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style =
                        TextStyle(
                            fontSize = 11.sp,
                            lineHeight = CommonTileDefaults.ColorOSLabelLineHeight,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.1.sp,
                        ),
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (chevron) {
                    androidx.compose.material3.Icon(
                        painter = androidx.compose.ui.res.painterResource(
                            R.drawable.ic_coloros_tile_chevron
                        ),
                        contentDescription = null,
                        tint = labelColor,
                        modifier = Modifier.padding(start = 1.dp, top = 1.dp).size(10.dp),
                    )
                }
            }
        } else {
            Box(Modifier.height(0.dp))
        }
    }
}
