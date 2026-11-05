/*
 * SPDX-FileCopyrightText: MiraCLO
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.qs.composefragment

import android.media.AudioManager
import android.telephony.TelephonyManager
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.android.compose.animation.Expandable
import com.android.compose.animation.scene.ContentScope
import com.android.settingslib.volume.domain.interactor.AudioVolumeInteractor
import com.android.settingslib.volume.shared.model.AudioStream
import com.android.systemui.animation.Expandable
import com.android.systemui.brightness.domain.model.GammaBrightness
import com.android.systemui.brightness.ui.viewmodel.BrightnessSliderViewModel
import com.android.systemui.brightness.ui.viewmodel.Drag
import com.android.systemui.qs.panels.ui.compose.GridLayout
import com.android.systemui.qs.panels.ui.compose.TileListener
import com.android.systemui.qs.panels.ui.compose.infinitegrid.ColorOSActiveFill
import com.android.systemui.qs.panels.ui.compose.infinitegrid.ColorOSGap
import com.android.systemui.qs.panels.ui.compose.infinitegrid.ColorOSGridFooterHeight
import com.android.systemui.qs.panels.ui.compose.infinitegrid.ColorOSGridHeadroom
import com.android.systemui.qs.panels.ui.compose.infinitegrid.ColorOSGridRows
import com.android.systemui.qs.panels.ui.compose.infinitegrid.ColorOSMinGridRows
import com.android.systemui.qs.panels.ui.compose.infinitegrid.ColorOSSliderFill
import com.android.systemui.qs.panels.ui.compose.infinitegrid.ColorOSTileRim
import com.android.systemui.qs.panels.ui.compose.infinitegrid.LocalColorOSGlassBlur
import com.android.systemui.qs.panels.ui.compose.infinitegrid.colorOSGlassBlur
import com.android.systemui.qs.panels.ui.compose.infinitegrid.colorOSAccent
import com.android.systemui.qs.panels.ui.compose.infinitegrid.colorOSGlassTileMaterial
import com.android.systemui.qs.panels.ui.compose.infinitegrid.rememberColorOSGlassBlur
import com.android.systemui.qs.panels.ui.compose.infinitegrid.CommonTileDefaults
import com.android.systemui.qs.panels.ui.compose.infinitegrid.InfiniteGridLayout
import com.android.systemui.qs.panels.ui.compose.infinitegrid.LocalColorOSGridRows
import com.android.systemui.qs.panels.ui.compose.infinitegrid.LocalColorOSTileSize
import com.android.systemui.qs.panels.ui.compose.infinitegrid.colorOSIconRowGap
import com.android.systemui.qs.panels.ui.compose.infinitegrid.colorOSPress
import com.android.systemui.qs.panels.ui.compose.infinitegrid.colorOSSpring
import com.android.systemui.qs.panels.ui.compose.infinitegrid.rememberQSTileLabelHide
import com.android.systemui.qs.panels.ui.viewmodel.TileViewModel
import com.android.systemui.qs.shared.ui.QuickSettings.Elements
import com.android.systemui.res.R
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Tile specs that ColorOS pins to the fixed wide slots, in order of preference. */
private val COLOROS_WIFI_SPECS = listOf("internet", "wifi")
private val COLOROS_DATA_SPECS = listOf("cell", "mobile_data", "mobiledata")
private val COLOROS_BT_SPECS = listOf("bt", "bluetooth")

/**
 * The tiles that fill the third card on a device that cannot use mobile data, in order of
 * preference, so the row is not left with an empty slot on a Wi-Fi only device.
 */
private val COLOROS_THIRD_CARD_FALLBACK_SPECS =
    listOf("dnd", "dark", "rotation", "hotspot", "cast", "airplane")

/** Whether the device can use mobile data, which a Wi-Fi only one cannot. */
@Composable
internal fun rememberColorOSDataCapable(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        context.getSystemService(TelephonyManager::class.java)?.isDataCapable == true
    }
}

/**
 * The spec that fills the third card of a device that cannot use mobile data, or null on one that
 * can, whether or not its data tile is in the list: the card is the data tile's then.
 */
private fun colorOSFallbackCardSpec(present: Set<String>, dataCapable: Boolean): String? =
    if (dataCapable || COLOROS_DATA_SPECS.any { it in present }) {
        null
    } else {
        COLOROS_THIRD_CARD_FALLBACK_SPECS.firstOrNull { it in present }
    }

/**
 * The specs of the tiles the panel pins as cards, the Wi-Fi, Bluetooth and mobile data ones that
 * are present, or the fallback one on a device that cannot use mobile data ([dataCapable]). The
 * grid and the editor leave them out, and the editor keeps them at the front of the list.
 */
internal fun colorOSCardSpecsOf(present: Set<String>, dataCapable: Boolean): Set<String> =
    buildSet {
        COLOROS_WIFI_SPECS.firstOrNull { it in present }?.let(::add)
        COLOROS_BT_SPECS.firstOrNull { it in present }?.let(::add)
        COLOROS_DATA_SPECS.firstOrNull { it in present }?.let(::add)
        colorOSFallbackCardSpec(present, dataCapable)?.let(::add)
    }

/** Size of the icon at the foot of a ColorOS slider, the brightness sun. */
private val ColorOSSliderIconSize = 28.dp

/**
 * Size of the headphones of the volume slider, they are drawn larger than the sun, as ColorOS does,
 * and the other volume icons.
 */
private val ColorOSVolumeHeadphonesIconSize = 32.dp
private val ColorOSVolumeIconSize = 28.dp

/** Space between the start of a ColorOS slider and its icon. */
private val ColorOSSliderIconInset = 16.dp

/** The orange sun of the brightness slider, the one other color of the panel next to the red. */
private val ColorOSBrightnessSun = Color(0xFFFF9F0A)

/** Diameter of the selected-mode bubble of the ringer switch, relative to the pill height. */
private const val ColorOSRingerBubbleRatio = 0.68f

/**
 * Extra space between the ringer row and the round tiles, ColorOS leaves a bit more there. The
 * pager adds [ColorOSGridHeadroom] on top of this, inside its bounds, so the gap is 6dp in all.
 */
private val ColorOSGridTopPadding = 2.dp

/** Smallest round tile the panel shrinks to before it gives up and scrolls instead. */
private val ColorOSMinTileSize = 56.dp

/**
 * How the ColorOS panel is sized: the diameter of the round tiles (which is also the height of the
 * pills and of the sliders), the rows of round tiles per page, and whether it all fits the screen.
 */
internal data class ColorOSFit(val tileSize: Dp, val rows: Int, val fits: Boolean)

/**
 * Sizes the ColorOS panel for [availableHeight], the height the panel may take inside the Quick
 * Settings scroller. The tiles keep the width of a column, [columnSize], when that fits. On a
 * short screen they shrink to the height that does fit, but not below [ColorOSMinTileSize], and
 * the grid first drops from [ColorOSGridRows] to [ColorOSMinGridRows] rows per page. If even that
 * does not fit (landscape, tiny screens), the panel keeps its full size and the Quick Settings
 * scroller takes over, which [ColorOSFit.fits] reports.
 *
 * The panel is stacked from, in tiles: the pills (1), the ringer row (1) and the rows of the grid,
 * then the media card and the two bars of fixed height, plus the gaps, the room for the labels and
 * the pager footer.
 */
internal fun colorOSFit(
    columnSize: Dp,
    availableHeight: Dp,
    labelHeight: Dp,
    iconRowGap: Dp,
): ColorOSFit {
    if (availableHeight == Dp.Unspecified || availableHeight <= 0.dp) {
        return ColorOSFit(columnSize, ColorOSGridRows, fits = true)
    }
    for (rows in ColorOSGridRows downTo ColorOSMinGridRows) {
        // Gaps between the six blocks (pills, media, ringer, grid, two bars), and the two bars.
        val fixed =
            ColorOSGap * 5 +
                ColorOSBarHeight * 2 +
                ColorOSGridTopPadding +
                ColorOSGridHeadroom +
                labelHeight * rows +
                iconRowGap * (rows - 1) +
                ColorOSGridFooterHeight +
                ColorOSMediaHeight
        val tileSize = (availableHeight - fixed) / (2 + rows)
        if (tileSize >= ColorOSMinTileSize) {
            return ColorOSFit(minOf(columnSize, tileSize), rows, fits = true)
        }
    }
    return ColorOSFit(columnSize, ColorOSGridRows, fits = false)
}

/** Corner radius of the glass slot behind the media card, playing or idle. */
private val ColorOSMediaShape = RoundedCornerShape(32.dp)

/**
 * ColorOS-style expanded Quick Settings.
 *
 * ```
 * [ Wi-Fi         ][ Bluetooth     ]
 * [ Media                        ]   full width media card
 * [ 🔔  📳  🔕     ][ Mobile data   ]   ringer switch, mobile data card
 * ( ) ( ) ( ) ( )                       every other tile: round, label below
 *
 * [ ☀ ----------------------- ]       brightness and volume bars
 * [ 🔊 ---------------------- ]
 * ```
 *
 * The fixed tiles are the real tile view models, so they behave exactly like their stock
 * versions. Anything missing on a device (e.g. no mobile data tile) simply leaves its slot out.
 */
@Composable
internal fun ContentScope.ColorOSQuickSettingsLayout(
    tiles: List<TileViewModel>,
    gridLayout: GridLayout,
    infiniteGridLayout: InfiniteGridLayout,
    brightnessViewModel: BrightnessSliderViewModel,
    audioVolumeInteractor: AudioVolumeInteractor,
    showMedia: Boolean,
    media: @Composable (Modifier) -> Unit,
    mediaHost: @Composable (Modifier) -> Unit = {},
    listening: () -> Boolean,
    modifier: Modifier = Modifier,
    availableHeight: Dp = Dp.Unspecified,
    onFitChanged: (Boolean) -> Unit = {},
    glassAlpha: () -> Float = { 1f },
    expansion: () -> Float = { 1f },
    headerHeight: Dp = 0.dp,
    header: @Composable () -> Unit = {},
) {
    fun pick(specs: List<String>): TileViewModel? =
        specs.firstNotNullOfOrNull { spec -> tiles.firstOrNull { it.spec.spec == spec } }

    val wifi = pick(COLOROS_WIFI_SPECS)
    val bt = pick(COLOROS_BT_SPECS)
    val data = pick(COLOROS_DATA_SPECS)
    // The mobile data tile is the third card whenever it is in the list, with or without a SIM. A
    // device that cannot use mobile data (Wi-Fi only) has another tile in the card, so the slot
    // is not empty. A device that can never gets one, the card is the data tile's.
    val dataCapable = rememberColorOSDataCapable()
    val fallbackCard =
        remember(tiles, dataCapable) {
            val present = tiles.mapTo(HashSet()) { it.spec.spec }
            colorOSFallbackCardSpec(present, dataCapable)?.let { spec ->
                tiles.firstOrNull { it.spec.spec == spec }
            }
        }
    val thirdCard = data ?: fallbackCard
    val fixed = listOfNotNull(wifi, bt, thirdCard)
    val rest = remember(tiles, fixed) { tiles.filterNot { it in fixed } }

    // The fixed tiles are not part of a grid, so make them listen ourselves.
    TileListener(listOfNotNull(wifi, bt, data, fallbackCard), listening)

    // Null where SurfaceFlinger cannot blur behind the shade, the flat fills stay then.
    // The modules are revealed when the shade opens and hidden when it closes, and the glass goes
    // out quickly with it.
    val leaving = rememberColorOSLeaving(expansion)
    val glassFade = rememberColorOSGlassFade(leaving)
    val reveal: () -> Float = { colorOSExpansionReveal(expansion()) }
    val glassBlur = rememberColorOSGlassBlur { glassAlpha() * glassFade.value * reveal() }
    val glass = colorOSGlass(blurred = glassBlur != null)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // The round tiles, the pills and the media card take their size from the width of one of
        // five columns, the size of the toggles of the collapsed panel, so a tile has the same
        // size in both panels. The grid has four columns and the circles sit in the middle of
        // them. When the screen is too short for that, the tiles shrink and the grid loses a row,
        // see colorOSFit.
        val columnSize = (maxWidth - ColorOSGap * (ColorOSCollapsedToggles - 1)) /
            ColorOSCollapsedToggles
        // Without the labels the round tile rows are shorter and further apart.
        val labelsHidden = rememberQSTileLabelHide()
        val labelHeight = if (labelsHidden) 0.dp else CommonTileDefaults.ClassicLabelHeight
        val iconRowGap = colorOSIconRowGap(labelsHidden)
        val fit =
            remember(columnSize, availableHeight, labelHeight, iconRowGap) {
                colorOSFit(columnSize, availableHeight, labelHeight, iconRowGap)
            }
        SideEffect { onFitChanged(fit.fits) }
        val tileSize = fit.tileSize
        CompositionLocalProvider(
            LocalColorOSTileSize provides tileSize,
            LocalColorOSGridRows provides fit.rows,
            LocalColorOSGlassBlur provides glassBlur,
        ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = spacedBy(ColorOSGap)) {
                // The room the shade header keeps above the panel, with the two buttons at the top.
                // The status line in it is drawn by the shade header. The gap of the column is
                // part of the height, so the panel ends up where it was without the buttons.
                if (headerHeight > ColorOSGap) {
                    Box(Modifier.fillMaxWidth().height(headerHeight - ColorOSGap)) { header() }
                }

                // Row 1: Wi-Fi + Bluetooth
                if (wifi != null || bt != null) {
                    // The pills, the grid and the brightness bar are shared elements with the collapsed
                    // panel, they move from there, so they are not revealed with the modules.
                    Row(horizontalArrangement = spacedBy(ColorOSGap)) {
                        listOfNotNull(wifi, bt).forEach { tile ->
                            with(infiniteGridLayout) {
                                ColorOSWideTile(tile, listening, Modifier.weight(1f))
                            }
                        }
                        if (wifi == null || bt == null) Box(Modifier.weight(1f))
                    }
                }

                // The media card and the ringer row unfold from under the pills while the
                // toggles of the collapsed panel move down to the grid: they are cut off at the
                // top edge of the toggle row, so nothing crosses them.
                Box(Modifier.colorOSUnfold(expansion)) {
                    Column(verticalArrangement = spacedBy(ColorOSGap)) {
                        ColorOSMediaModule(
                            glass = glass,
                            height = ColorOSMediaHeight,
                            showMedia = showMedia,
                            media = media,
                            mediaHost = mediaHost,
                        )

                        // Ringer switch + third card, the mobile data tile whenever it is there,
                        // as Penguin does. A Wi-Fi only device has the fallback tile in the card.
                        Row(horizontalArrangement = spacedBy(ColorOSGap)) {
                            if (thirdCard == null) Box(Modifier.weight(1f))
                            ColorOSRingerSwitch(
                                audioVolumeInteractor = audioVolumeInteractor,
                                glass = glass,
                                height = tileSize,
                                modifier = Modifier.weight(1f),
                            )
                            if (thirdCard != null) {
                                with(infiniteGridLayout) {
                                    ColorOSWideTile(thirdCard, listening, Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Everything else: round icon tiles with labels (the grid handles paging + listening)
                if (rest.isNotEmpty()) {
                    with(gridLayout) {
                        TileGrid(
                            tiles = rest,
                            modifier =
                                Modifier.fillMaxWidth().padding(top = ColorOSGridTopPadding),
                            listening = listening,
                            enableRevealEffect = false,
                        )
                    }
                }

                // The brightness and volume bars, the same pills as in the collapsed panel.
                Element(Elements.BrightnessSlider, Modifier.fillMaxWidth().height(ColorOSBarHeight)) {
                    ColorOSBrightnessSlider(
                        viewModel = brightnessViewModel,
                        glass = glass,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                ColorOSVolumeSlider(
                    audioVolumeInteractor = audioVolumeInteractor,
                    glass = glass,
                    modifier =
                        Modifier.colorOSModuleReveal(5, leaving, reveal)
                            .fillMaxWidth()
                            .height(ColorOSBarHeight),
                )
            }
        }
    }
}

/** Colours shared by the ColorOS glass cards, matching the tile glass in Tile.kt. */
private data class ColorOSGlass(
    val fill: Color,
    val iconOff: Color,
    val accent: Color,
    val dark: Boolean,
    /** Whether the glass is drawn by SurfaceFlinger, and not with [fill] and the rim. */
    val blurred: Boolean,
    val material: Int,
)

@Composable
private fun colorOSGlass(blurred: Boolean): ColorOSGlass {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val accent = colorOSAccent()
    return ColorOSGlass(
        fill = if (dark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.55f),
        iconOff = onSurface,
        accent = accent,
        dark = dark,
        blurred = blurred,
        material = colorOSGlassTileMaterial(),
    )
}

/**
 * The glass of a pill, a slider, the ringer switch or the media card, which all use the material
 * of the tiles that are off. SurfaceFlinger draws it, with the tint, the lit rim and the glow of the
 * ColorOS material, when [glass] is [ColorOSGlass.blurred]. Otherwise it is a translucent fill
 * with a rim.
 */
@Composable
private fun Modifier.glassSurface(glass: ColorOSGlass, shape: Shape): Modifier =
    if (glass.blurred) {
        this.clip(shape).colorOSGlassBlur(shape, glass.material)
    } else {
        // No blur, which is too slow to animate: a tint that is lighter at the top and a lit rim.
        val top = glass.fill.copy(alpha = (glass.fill.alpha * 1.5f).coerceAtMost(1f))
        val bottom = glass.fill.copy(alpha = glass.fill.alpha * 0.6f)
        this.clip(shape)
            .background(Brush.verticalGradient(listOf(top, bottom)), shape)
            .border(1.dp, ColorOSTileRim, shape)
    }

/** How long a released slider waits for the value it set to be reported back. */
private const val ColorOSSliderSettleMillis = 400L

/** How far the reported value may be from the released one to count as caught up. */
private const val ColorOSSliderSettleTolerance = 0.005f

/**
 * A horizontal pill slider: the white fill grows from the start. Dragging is relative to where the
 * finger lands, like ColorOS. While dragging, the shade is asked not to steal the gesture.
 */
@Composable
private fun ColorOSSlider(
    fraction: Float,
    @DrawableRes iconRes: Int,
    iconTint: Color,
    label: String,
    glass: ColorOSGlass,
    onChange: (Float) -> Unit,
    onStop: (Float) -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = ColorOSSliderIconSize,
    snap: (Float) -> Float = { it },
    onLongPress: (() -> Unit)? = null,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var local by remember { mutableFloatStateOf(fraction) }
    var dragging by remember { mutableStateOf(false) }
    // After a release, [fraction] reports the values on the way to the one that was set, the one
    // from before the drag among them, so the fill stays where it was released until the reported
    // value gets there instead of jumping back and forward again.
    var settling by remember { mutableStateOf(false) }
    LaunchedEffect(fraction, dragging, settling) {
        when {
            dragging -> Unit
            !settling -> local = fraction
            abs(fraction - local) < ColorOSSliderSettleTolerance -> {
                settling = false
                local = fraction
            }
            else -> {
                delay(ColorOSSliderSettleMillis)
                settling = false
                local = fraction
            }
        }
    }
    val currentOnChange by rememberUpdatedState(onChange)
    val currentOnStop by rememberUpdatedState(onStop)
    val currentSnap by rememberUpdatedState(snap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val shape = RoundedCornerShape(percent = 50)

    Box(
        modifier =
            modifier
                .colorOSStretch()
                .glassSurface(glass, shape)
                .semantics { contentDescription = label }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                        dragging = true
                        down.consume()
                        val start = local
                        val width = size.width.coerceAtLeast(1)
                        // A finger that stays down without moving does not set a level, it fires
                        // the long press. Once that fired the rest of the gesture is ignored.
                        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                        var longPressed = false
                        val longPressJob =
                            if (currentOnLongPress != null) {
                                scope.launch {
                                    delay(longPressTimeout)
                                    longPressed = true
                                    currentOnLongPress?.invoke()
                                }
                            } else {
                                null
                            }
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            if (longPressed) {
                                change.consume()
                                continue
                            }
                            if ((change.position - down.position).getDistance() >
                                viewConfiguration.touchSlop
                            ) {
                                longPressJob?.cancel()
                            }
                            val delta = (change.position.x - down.position.x) / width
                            local = (start + delta).coerceIn(0f, 1f)
                            currentOnChange(local)
                            change.consume()
                        }
                        longPressJob?.cancel()
                        if (!longPressed) {
                            local = currentSnap(local)
                            settling = true
                            currentOnStop(local)
                        }
                        dragging = false
                        view.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
    ) {
        Box(
            modifier =
                Modifier.align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(local.coerceIn(0.001f, 1f))
                    .background(ColorOSSliderFill)
        )
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint =
                if (local > 0.18f) iconTint else glass.iconOff,
            modifier =
                Modifier.align(Alignment.CenterStart)
                    .padding(start = ColorOSSliderIconInset)
                    .size(iconSize),
        )
    }
}

@Composable
private fun ColorOSBrightnessSlider(
    viewModel: BrightnessSliderViewModel,
    glass: ColorOSGlass,
    modifier: Modifier = Modifier,
) {
    val gamma = viewModel.currentBrightness.value
    val min = viewModel.minBrightness.value
    val max = viewModel.maxBrightness.value
    val scope = rememberCoroutineScope()
    val span = (max - min).coerceAtLeast(1)
    val fraction =
        if (gamma < 0) 0.5f else ((gamma - min).toFloat() / span).coerceIn(0f, 1f)
    fun toGamma(f: Float) = GammaBrightness((min + f * span).toInt().coerceIn(min, max))

    ColorOSSlider(
        fraction = fraction,
        iconRes = R.drawable.ic_coloros_brightness,
        iconTint = ColorOSBrightnessSun,
        label = stringResource(R.string.quick_settings_brightness_dialog_title),
        glass = glass,
        onChange = { f -> scope.launch { viewModel.onDrag(Drag.Dragging(toGamma(f))) } },
        onStop = { f -> scope.launch { viewModel.onDrag(Drag.Stopped(toGamma(f))) } },
        modifier = modifier,
    )
}

@Composable
private fun ColorOSVolumeSlider(
    audioVolumeInteractor: AudioVolumeInteractor,
    glass: ColorOSGlass,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val audioManager = remember(context) { context.getSystemService(AudioManager::class.java) }
    val stream = remember { AudioStream(AudioManager.STREAM_MUSIC) }
    val model by
        remember(audioVolumeInteractor) { audioVolumeInteractor.getAudioStream(stream) }
            .collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val current = model
    val min = current?.minVolume ?: 0
    val max = current?.maxVolume ?: 1
    val span = (max - min).coerceAtLeast(1)
    val fraction = current?.let { ((it.volume - min).toFloat() / span).coerceIn(0f, 1f) } ?: 0f
    fun toVolume(f: Float) = (min + Math.round(f * span)).coerceIn(min, max)
    val silent = current?.isMuted == true || fraction == 0f
    val headphones = rememberColorOSHeadphonesOutput()

    ColorOSSlider(
        fraction = fraction,
        iconRes =
            when {
                silent && headphones -> R.drawable.ic_coloros_volume_headphones_mute
                headphones -> R.drawable.ic_coloros_volume_headphones
                silent -> R.drawable.ic_coloros_volume_mute
                else -> R.drawable.ic_coloros_volume
            },
        iconTint = glass.accent,
        iconSize = if (headphones) ColorOSVolumeHeadphonesIconSize else ColorOSVolumeIconSize,
        label = stringResource(R.string.stream_music),
        glass = glass,
        onChange = { f -> scope.launch { audioVolumeInteractor.setVolume(stream, toVolume(f)) } },
        onStop = { f -> scope.launch { audioVolumeInteractor.setVolume(stream, toVolume(f)) } },
        modifier = modifier,
        // The volume is in steps, so the fill rests on the step the volume gets set to.
        snap = { f -> ((toVolume(f) - min).toFloat() / span).coerceIn(0f, 1f) },
        onLongPress = {
            audioManager?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_TOGGLE_MUTE,
                0,
            )
        },
    )
}

/** How far the end buttons are pulled towards the middle, in segments, by the selected bubble. */
private const val ColorOSRingerNudge = 0.1f

private class ColorOSRingerMode(
    val mode: Int,
    @DrawableRes val icon: Int,
    @RawRes val animation: Int,
    val label: String,
)

/**
 * Which way the switch leans. The buttons and the bubble lean in, towards the middle, only while
 * the bubble sits at an end, with the middle one selected each icon stays in the middle of its
 * third.
 */
private fun colorOSRingerNudge(selected: Int, last: Int): Float =
    when (selected) {
        0 -> 1f
        last -> -1f
        else -> 0f
    }

@Composable
private fun ColorOSRingerSwitch(
    audioVolumeInteractor: AudioVolumeInteractor,
    glass: ColorOSGlass,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val audioManager = remember(context) { context.getSystemService(AudioManager::class.java) }
    val ringerMode by audioVolumeInteractor.ringerMode.collectAsStateWithLifecycle()
    val modes =
        listOf(
            ColorOSRingerMode(
                AudioManager.RINGER_MODE_NORMAL,
                R.drawable.ic_volume_ringer,
                R.raw.coloros_ringer_normal,
                stringResource(R.string.volume_ringer_status_normal),
            ),
            ColorOSRingerMode(
                AudioManager.RINGER_MODE_VIBRATE,
                R.drawable.ic_volume_ringer_vibrate,
                R.raw.coloros_ringer_vibrate,
                stringResource(R.string.volume_ringer_status_vibrate),
            ),
            ColorOSRingerMode(
                AudioManager.RINGER_MODE_SILENT,
                R.drawable.ic_volume_ringer_mute,
                R.raw.coloros_ringer_silent,
                stringResource(R.string.volume_ringer_status_silent),
            ),
        )
    val shape = RoundedCornerShape(percent = 50)
    val bubble = height * ColorOSRingerBubbleRatio
    val selected = modes.indexOfFirst { it.mode == ringerMode.value }.coerceAtLeast(0)
    val last = modes.lastIndex

    BoxWithConstraints(
        modifier = modifier.height(height).glassSurface(glass, shape),
        contentAlignment = Alignment.CenterStart,
    ) {
        val density = LocalDensity.current
        val segmentPx = with(density) { maxWidth.toPx() } / modes.size
        val bubblePx = with(density) { bubble.toPx() }
        val nudge = colorOSRingerNudge(selected, last) * ColorOSRingerNudge * segmentPx
        // One bubble slides to the selected mode, it leans a little towards the middle when it
        // sits at an end. The buttons lean with it.
        val indicatorX by
            animateFloatAsState(
                selected * segmentPx + (segmentPx - bubblePx) / 2f + nudge,
                colorOSSpring(0.25f, 0.3f),
                label = "colorOSRingerIndicator",
            )
        // requiredSize: the slot can be a little narrower than the bubble on small screens, and
        // size() would squash the circle into an oval.
        Box(
            Modifier.graphicsLayer { translationX = indicatorX }
                .requiredSize(bubble)
                .background(ColorOSActiveFill, CircleShape)
        )
        Row(Modifier.fillMaxSize()) {
            modes.forEachIndexed { index, mode ->
                val shift by
                    animateFloatAsState(
                        nudge,
                        colorOSSpring(0f, 0.35f),
                        label = "colorOSRingerNudge",
                    )
                ColorOSRingerButton(
                    mode = mode,
                    selected = index == selected,
                    glass = glass,
                    audioManager = audioManager,
                    modifier =
                        Modifier.weight(1f).fillMaxHeight().graphicsLayer { translationX = shift },
                )
            }
        }
    }
}

/**
 * One mode of the ringer switch. Its icon is red on the white bubble of a dark panel and white on
 * the red bubble of a light one, it grows a little and plays its animation when it gets selected.
 */
@Composable
private fun ColorOSRingerButton(
    mode: ColorOSRingerMode,
    selected: Boolean,
    glass: ColorOSGlass,
    audioManager: AudioManager?,
    modifier: Modifier = Modifier,
) {
    val pressSource = remember { MutableInteractionSource() }
    val scale by
        animateFloatAsState(
            if (selected) 1.2f else 1f,
            colorOSSpring(0f, 0.25f),
            label = "colorOSRingerScale",
        )
    val color by
        animateColorAsState(
            when {
                !selected -> glass.iconOff
                else -> glass.accent
            },
            colorOSSpring(0f, 0.3f),
            label = "colorOSRingerColor",
        )
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(mode.animation))
    val progress = remember { Animatable(1f) }
    var wasSelected by remember { mutableStateOf(selected) }
    LaunchedEffect(selected, composition) {
        val clip = composition ?: return@LaunchedEffect
        if (selected == wasSelected) return@LaunchedEffect
        wasSelected = selected
        if (!selected) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        progress.animateTo(1f, tween(clip.duration.toInt(), easing = LinearEasing))
    }

    Box(
        modifier =
            modifier
                .colorOSPress(
                    pressSource,
                    press = colorOSSpring(0.18f, 0.13f),
                    release = colorOSSpring(0.62f, 0.35f),
                )
                .semantics { contentDescription = mode.label }
                .pointerInput(mode.mode) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        val press = PressInteraction.Press(down.position)
                        pressSource.tryEmit(press)
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            up.consume()
                            pressSource.tryEmit(PressInteraction.Release(press))
                            audioManager?.setRingerModeInternal(mode.mode)
                        } else {
                            pressSource.tryEmit(PressInteraction.Cancel(press))
                        }
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        val iconModifier =
            Modifier.size(22.dp).graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        val clip = composition
        if (clip != null) {
            // One colour for the whole animation, so it follows the icon colour above.
            val properties =
                rememberLottieDynamicProperties(
                    rememberLottieDynamicProperty(LottieProperty.COLOR, color.toArgb(), "**"),
                    rememberLottieDynamicProperty(
                        LottieProperty.STROKE_COLOR,
                        color.toArgb(),
                        "**",
                    ),
                )
            LottieAnimation(
                composition = clip,
                progress = { progress.value },
                dynamicProperties = properties,
                modifier = iconModifier,
            )
        } else {
            Icon(
                painter = painterResource(mode.icon),
                contentDescription = null,
                tint = color,
                modifier = iconModifier,
            )
        }
    }
}

/** Diameter of the two round buttons of the ColorOS header. */
private val ColorOSHeaderButtonSize = 32.dp

/** How far the two buttons sit in from the sides of the panel, ColorOS keeps them off its edge. */
private val ColorOSHeaderButtonInset = 6.dp

/** How far the row of the buttons is moved up, ColorOS puts it higher above the status row. */
private val ColorOSHeaderButtonRaise = 2.dp

/**
 * The glyphs of the two header buttons are drawn at different sizes, as the ColorOS gear is larger
 * in its circle than the pencil is.
 */
private val ColorOSHeaderEditIconSize = 17.dp
private val ColorOSHeaderSettingsIconSize = 23.dp

/**
 * The two buttons above the panel: edit on the start side and settings on the end side, round
 * glass like the tiles that are off. A missing [onSettings], or a button that the footer settings
 * hide, leaves its place empty.
 */
@Composable
internal fun ColorOSHeaderButtons(
    onEdit: (Expandable) -> Unit,
    onSettings: ((Expandable) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val glass = colorOSGlass(blurred = LocalColorOSGlassBlur.current != null)
    // Both buttons are always shown. A missing settings callback keeps its place empty, so the
    // other button stays on its side.
    val showEdit = true
    val showSettings = true
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .offset(y = -ColorOSHeaderButtonRaise)
                .padding(horizontal = ColorOSHeaderButtonInset),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        if (showEdit) {
            ColorOSHeaderButton(
                iconRes = R.drawable.ic_edit,
                iconSize = ColorOSHeaderEditIconSize,
                description = stringResource(R.string.accessibility_quick_settings_edit),
                glass = glass,
                onClick = onEdit,
            )
        } else {
            Spacer(Modifier.size(ColorOSHeaderButtonSize))
        }
        if (showSettings && onSettings != null) {
            ColorOSHeaderButton(
                iconRes = R.drawable.ic_qs_footer_settings,
                iconSize = ColorOSHeaderSettingsIconSize,
                description = stringResource(R.string.accessibility_quick_settings_settings),
                glass = glass,
                onClick = onSettings,
            )
        } else {
            Spacer(Modifier.size(ColorOSHeaderButtonSize))
        }
    }
}

@Composable
private fun ColorOSHeaderButton(
    iconRes: Int,
    iconSize: Dp,
    description: String,
    glass: ColorOSGlass,
    onClick: (Expandable) -> Unit,
) {
    val pressSource = remember { MutableInteractionSource() }
    Expandable(
        color = Color.Unspecified,
        shape = CircleShape,
        onClick = onClick,
        interactionSource = pressSource,
        useModifierBasedImplementation = true,
        defaultMinSize = false,
        modifier =
            Modifier.size(ColorOSHeaderButtonSize)
                .colorOSPress(pressSource)
                .semantics {
                    role = Role.Button
                    contentDescription = description
                }
                .glassSurface(glass, CircleShape),
    ) {
        Box(Modifier.size(ColorOSHeaderButtonSize), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = glass.iconOff,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

/** Height of the row of the date in the collapsed panel. */
private val ColorOSCollapsedDateHeight = 34.dp

/** Thickness of the brightness and volume bars, in both panels. */
private val ColorOSBarHeight = 40.dp

/** The date shown in the collapsed panel, such as "Mon, Nov 2". Refreshed once a minute. */
@Composable
private fun rememberColorOSDateText(): String {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            now = System.currentTimeMillis()
        }
    }
    val locale = context.resources.configuration.locales[0]
    val format =
        remember(locale) {
            android.icu.text.DateFormat.getInstanceForSkeleton("EEEMMMd", locale)
        }
    return format.format(java.util.Date(now))
}

/** How many of the first tiles of the list the collapsed panel shows as round toggles. */
private const val ColorOSCollapsedToggles = 5

/** How many of the toggles are shared elements, the others fade out: the grid has four columns. */
private const val ColorOSCollapsedSharedToggles = 4

/**
 * The media module of the ColorOS panel: the glass card of [height] with the media [media] on it,
 * or the idle card when [showMedia] is false and [showIdle] is true. The stock media host stays
 * hidden under it in [mediaHost].
 */
@Composable
private fun ColorOSMediaModule(
    glass: ColorOSGlass,
    height: Dp,
    showMedia: Boolean,
    media: @Composable (Modifier) -> Unit,
    mediaHost: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    showIdle: Boolean = true,
) {
    if (!showMedia && !showIdle) return
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                // Playing and idle share the same glass slot, like the other ColorOS cards, the
                // media card only lays out its content on it.
                .glassSurface(glass, ColorOSMediaShape),
        contentAlignment = Alignment.Center,
    ) {
        mediaHost(Modifier.matchParentSize())
        if (showMedia) {
            media(Modifier.fillMaxSize())
        } else {
            ColorOSIdleMediaCard(Modifier.fillMaxSize())
        }
    }
}

/**
 * The collapsed panel: the date, the Wi-Fi and Bluetooth pills, a row of the first
 * [ColorOSCollapsedToggles] round toggles, the brightness bar and, while something plays, the media
 * card. The parts are the same composables as in the expanded panel and have the same size, shape
 * and colours.
 *
 * The pills, the toggles and the brightness bar are shared elements: when the panel expands they
 * move to their place in the expanded panel, the toggles to the first row of the grid. The date
 * and the media card are not, they fade out first.
 */
@Composable
internal fun ContentScope.ColorOSCollapsedQuickSettings(
    tiles: List<TileViewModel>,
    infiniteGridLayout: InfiniteGridLayout,
    listening: () -> Boolean,
    brightnessViewModel: BrightnessSliderViewModel,
    showMedia: Boolean,
    media: @Composable (Modifier) -> Unit,
    mediaHost: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    headerHeight: Dp = 0.dp,
) {
    fun pick(specs: List<String>): TileViewModel? =
        specs.firstNotNullOfOrNull { spec -> tiles.firstOrNull { it.spec.spec == spec } }

    val wifi = pick(COLOROS_WIFI_SPECS)
    val bt = pick(COLOROS_BT_SPECS)
    val toggles =
        remember(tiles) {
            val reserved = COLOROS_WIFI_SPECS + COLOROS_BT_SPECS + COLOROS_DATA_SPECS
            tiles.filterNot { it.spec.spec in reserved }.take(ColorOSCollapsedToggles)
        }
    TileListener(listOfNotNull(wifi, bt) + toggles, listening)

    val glass = colorOSGlass(blurred = LocalColorOSGlassBlur.current != null)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // The size of a tile of the expanded panel too, see ColorOSQuickSettingsLayout.
        val toggleSize = (maxWidth - ColorOSGap * (ColorOSCollapsedToggles - 1)) /
            ColorOSCollapsedToggles
        CompositionLocalProvider(LocalColorOSTileSize provides toggleSize) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = spacedBy(ColorOSGap),
            ) {
                Element(ColorOSCollapsedDate, Modifier.fillMaxWidth()) {
                    androidx.compose.material3.Text(
                        text = rememberColorOSDateText(),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier =
                            Modifier.fillMaxWidth()
                                // As high as the header of the expanded panel, so the pills stay where
                                // they are when it opens.
                                .height(
                                    if (headerHeight > ColorOSGap) headerHeight - ColorOSGap
                                    else ColorOSCollapsedDateHeight
                                )
                                .wrapContentHeight(Alignment.CenterVertically)
                                .padding(start = 4.dp),
                    )
                }
                if (wifi != null || bt != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = spacedBy(ColorOSGap),
                    ) {
                        listOfNotNull(wifi, bt).forEach { tile ->
                            with(infiniteGridLayout) {
                                ColorOSWideTile(tile, listening, Modifier.weight(1f))
                            }
                        }
                        if (wifi == null || bt == null) Box(Modifier.weight(1f))
                    }
                }
                if (toggles.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = spacedBy(ColorOSGap),
                    ) {
                        toggles.forEachIndexed { index, tile ->
                            // The row is as high as a toggle whatever the size of the shared
                            // element is. The element of a tile is as high as the tile with its
                            // label in the expanded panel, and the collapsed panel was laid out
                            // with that after it closed, which left it too high until the next
                            // layout: a swipe up stopped short of the top.
                            Box(Modifier.weight(1f).height(toggleSize)) {
                                with(infiniteGridLayout) {
                                    if (index < ColorOSCollapsedSharedToggles) {
                                        ColorOSCollapsedRoundTile(
                                            tile,
                                            listening,
                                            Modifier.fillMaxWidth(),
                                        )
                                    } else {
                                        // The tile has no place on the first row of the grid, it
                                        // fades out with the date.
                                        Element(ColorOSCollapsedExtraToggle, Modifier) {
                                            ColorOSCollapsedRoundTile(
                                                tile,
                                                listening,
                                                Modifier.fillMaxWidth(),
                                                sharedElement = false,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // Fewer toggles than columns keep the size of the others.
                        repeat(ColorOSCollapsedToggles - toggles.size) { Box(Modifier.weight(1f)) }
                    }
                }
                Element(Elements.BrightnessSlider, Modifier.fillMaxWidth().height(ColorOSBarHeight)) {
                    ColorOSBrightnessSlider(
                        viewModel = brightnessViewModel,
                        glass = glass,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (showMedia) {
                    Element(ColorOSCollapsedMedia, Modifier.fillMaxWidth()) {
                        ColorOSMediaModule(
                            glass = glass,
                            height = ColorOSMediaHeight,
                            showMedia = true,
                            media = media,
                            mediaHost = mediaHost,
                            showIdle = false,
                        )
                    }
                }
            }
        }
    }
}
