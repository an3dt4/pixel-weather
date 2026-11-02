package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.WeatherFormatter
import com.example.data.model.WeatherSnapshot
import com.example.data.model.WidgetPalette
import com.example.data.model.WidgetStyle
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.WidgetPaletteColors
import kotlin.math.roundToInt

data class ComposeWidgetColors(
    val bg: Color,
    val inner: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val badgeBg: Color,
    val badgeText: Color
)

fun resolveComposeWidgetColors(palette: WidgetPalette): ComposeWidgetColors = when (palette) {
    WidgetPalette.LIGHT_PILL -> ComposeWidgetColors(
        bg = WidgetPaletteColors.LightPillBg,
        inner = WidgetPaletteColors.LightPillInner,
        border = WidgetPaletteColors.LightPillBorder,
        textPrimary = WidgetPaletteColors.LightPillTextPrimary,
        textSecondary = WidgetPaletteColors.LightPillTextSecondary,
        badgeBg = WidgetPaletteColors.LightPillBadgeBg,
        badgeText = WidgetPaletteColors.LightPillBadgeText
    )
    WidgetPalette.DARK_PILL -> ComposeWidgetColors(
        bg = WidgetPaletteColors.DarkPillBg,
        inner = WidgetPaletteColors.DarkPillInner,
        border = WidgetPaletteColors.DarkPillBorder,
        textPrimary = WidgetPaletteColors.DarkPillTextPrimary,
        textSecondary = WidgetPaletteColors.DarkPillTextSecondary,
        badgeBg = WidgetPaletteColors.DarkPillBadgeBg,
        badgeText = WidgetPaletteColors.DarkPillBadgeText
    )
    WidgetPalette.DYNAMIC_SKY -> ComposeWidgetColors(
        bg = WidgetPaletteColors.DynamicBg,
        inner = WidgetPaletteColors.DynamicInner,
        border = WidgetPaletteColors.DynamicBorder,
        textPrimary = WidgetPaletteColors.DynamicTextPrimary,
        textSecondary = WidgetPaletteColors.DynamicTextSecondary,
        badgeBg = WidgetPaletteColors.DynamicBadgeBg,
        badgeText = WidgetPaletteColors.DynamicBadgeText
    )
    WidgetPalette.WARM_SUN -> ComposeWidgetColors(
        bg = WidgetPaletteColors.WarmBg,
        inner = WidgetPaletteColors.WarmInner,
        border = WidgetPaletteColors.WarmBorder,
        textPrimary = WidgetPaletteColors.WarmTextPrimary,
        textSecondary = WidgetPaletteColors.WarmTextSecondary,
        badgeBg = WidgetPaletteColors.WarmBadgeBg,
        badgeText = WidgetPaletteColors.WarmBadgeText
    )
}

/**
 * Interactive "Select a widget style" Sky-Wave Stage modeled 1:1 on Google's At a Glance selector:
 * - Organic two-tone sky wave backdrop (`#96C4FA` + `#DDEBFD` / dark slate wave for `DARK_PILL`)
 * - For `PIXEL_AT_A_GLANCE`, renders all 3 iconic interactive At a Glance pills (`Semi-transparent`, `Transparent`, `Solid`)
 *   so users can compare and tap directly on the canvas OR drag the 0%..100% slider below!
 */
@Composable
fun PixelWallpaperWidgetStage(
    cityName: String,
    isGpsLocation: Boolean,
    weather: WeatherSnapshot?,
    style: WidgetStyle,
    palette: WidgetPalette,
    backgroundAlpha: Float = 0.68f,
    showBorder: Boolean = false,
    showDate: Boolean = true,
    useEnglishDate: Boolean = false,
    showCityOnWidget: Boolean = true,
    useFahrenheit: Boolean,
    expandedTallForecast: Boolean = false,
    onSelectPresetOpacityAndBorder: ((Float, Boolean) -> Unit)? = null,
    onCityPillClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dateLong = WeatherFormatter.formatAtAGlanceDate(
        timezoneId = weather?.timezoneId,
        useEnglish = useEnglishDate,
        shortForm = false
    )
    val dateShort = WeatherFormatter.formatAtAGlanceDate(
        timezoneId = weather?.timezoneId,
        useEnglish = useEnglishDate,
        shortForm = true
    )
    val clampedAlpha = backgroundAlpha.coerceIn(0f, 1f)
    val isDarkBackdrop = palette == WidgetPalette.DARK_PILL

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(30.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Organic Pixel "Select a widget style" Sky-Wave Canvas Backdrop
        Canvas(modifier = Modifier.matchParentSize()) {
            val baseSky = if (isDarkBackdrop) Color(0xFF1E293B) else Color(0xFF9AC5FA)
            val waveCloud = if (isDarkBackdrop) Color(0xFF334155) else Color(0xFFDCEBFE)
            drawRect(color = baseSky)

            val wavePath = Path().apply {
                moveTo(size.width * 1.0f, size.height * 0.04f)
                cubicTo(
                    size.width * 0.52f, size.height * 0.08f,
                    size.width * 0.44f, size.height * 0.30f,
                    size.width * 0.24f, size.height * 0.44f
                )
                cubicTo(
                    size.width * 0.05f, size.height * 0.56f,
                    size.width * 0.14f, size.height * 0.78f,
                    size.width * 0.42f, size.height * 0.96f
                )
                lineTo(size.width, size.height)
                close()
            }
            drawPath(path = wavePath, color = waveCloud)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = if (isDarkBackdrop) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.72f),
                shape = CircleShape
            ) {
                Text(
                    text = "${if (isGpsLocation) "实时定位" else "自选位置"}：$cityName · ${
                        WeatherFormatter.formatOpacityModeLabel(clampedAlpha, showBorder)
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkBackdrop) Color.White else Color(0xFF191C20),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = Triple(style, palette, expandedTallForecast),
                transitionSpec = {
                    fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                        fadeOut(spring(stiffness = Spring.StiffnessMedium))
                },
                label = "widget_preview_transition"
            ) { (targetStyle, targetPalette, isTall) ->
                when (targetStyle) {
                    WidgetStyle.PIXEL_AT_A_GLANCE -> {
                        if (onSelectPresetOpacityAndBorder != null) {
                            AtAGlanceThreeTierStyleShowcase(
                                cityName = cityName,
                                isGpsLocation = isGpsLocation,
                                weather = weather,
                                palette = targetPalette,
                                currentAlpha = clampedAlpha,
                                currentShowBorder = showBorder,
                                showDate = showDate,
                                dateLongText = dateLong,
                                showCityOnWidget = showCityOnWidget,
                                useFahrenheit = useFahrenheit,
                                isDarkBackdrop = isDarkBackdrop,
                                onSelectPreset = onSelectPresetOpacityAndBorder,
                                onMoreVertClick = onCityPillClick
                            )
                        } else {
                            AtAGlanceCapsulePreview(
                                cityName = cityName,
                                isGpsLocation = isGpsLocation,
                                weather = weather,
                                palette = targetPalette,
                                backgroundAlpha = clampedAlpha,
                                showBorder = showBorder,
                                showDate = showDate,
                                dateLongText = dateLong,
                                showCityOnWidget = showCityOnWidget,
                                useFahrenheit = useFahrenheit,
                                onMoreVertClick = onCityPillClick
                            )
                        }
                    }
                    WidgetStyle.PIXEL_MINIMAL -> PixelMinimalComposePreview(
                        cityName = cityName,
                        isGpsLocation = isGpsLocation,
                        weather = weather,
                        palette = targetPalette,
                        backgroundAlpha = clampedAlpha,
                        showBorder = showBorder,
                        showDate = showDate,
                        dateLongText = dateLong,
                        showCityOnWidget = showCityOnWidget,
                        useFahrenheit = useFahrenheit,
                        onCityClick = onCityPillClick
                    )
                    WidgetStyle.ROUNDED_SQUARE -> GoogleRoundedSquarePreview(
                        cityName = cityName,
                        weather = weather,
                        palette = targetPalette,
                        backgroundAlpha = clampedAlpha,
                        showBorder = showBorder,
                        showDate = showDate,
                        dateShortText = dateShort,
                        showCityOnWidget = showCityOnWidget,
                        useFahrenheit = useFahrenheit,
                        onCityClick = onCityPillClick
                    )
                    WidgetStyle.FORECAST_CARD -> GoogleForecastCardPreview(
                        cityName = cityName,
                        weather = weather,
                        palette = targetPalette,
                        backgroundAlpha = clampedAlpha,
                        showBorder = showBorder,
                        showDate = showDate,
                        dateLongText = dateLong,
                        showCityOnWidget = showCityOnWidget,
                        useFahrenheit = useFahrenheit,
                        showDailyRows = isTall,
                        onCityClick = onCityPillClick
                    )
                }
            }
        }
    }
}

/**
 * Renders the 3 stacked interactive At a Glance pills directly inspired by "Select a widget style":
 * 1) Semi-transparent (reflects custom translucent slider when between 1%..99%, default 68%)
 * 2) Transparent (0% fill + crisp stadium outline border)
 * 3) Solid (100% fill)
 */
@Composable
private fun AtAGlanceThreeTierStyleShowcase(
    cityName: String,
    isGpsLocation: Boolean,
    weather: WeatherSnapshot?,
    palette: WidgetPalette,
    currentAlpha: Float,
    currentShowBorder: Boolean,
    showDate: Boolean,
    dateLongText: String,
    showCityOnWidget: Boolean,
    useFahrenheit: Boolean,
    isDarkBackdrop: Boolean,
    onSelectPreset: (Float, Boolean) -> Unit,
    onMoreVertClick: (() -> Unit)?
) {
    val isTransparentSelected = currentAlpha <= 0.01f
    val isSolidSelected = currentAlpha >= 0.99f
    val isSemiSelected = !isTransparentSelected && !isSolidSelected

    val semiDisplayAlpha = if (isSemiSelected) currentAlpha else 0.68f
    val semiPct = (semiDisplayAlpha * 100f).roundToInt()
    val captionColor = if (isDarkBackdrop) Color(0xFFE2E8F0) else Color(0xFF1F2937)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Tier 1: Semi-transparent
        AtAGlanceCapsulePreview(
            cityName = cityName,
            isGpsLocation = isGpsLocation,
            weather = weather,
            palette = palette,
            backgroundAlpha = semiDisplayAlpha,
            showBorder = if (isSemiSelected) currentShowBorder else false,
            showDate = showDate,
            dateLongText = dateLongText,
            showCityOnWidget = showCityOnWidget,
            useFahrenheit = useFahrenheit,
            isSelectedHighlight = isSemiSelected,
            onCardClick = { onSelectPreset(semiDisplayAlpha, false) },
            onMoreVertClick = onMoreVertClick,
            modifier = Modifier.testTag("preset_card_semi_transparent")
        )
        StyleTierCaption(
            label = "半透明 · Semi-transparent ($semiPct%)",
            selected = isSemiSelected,
            captionColor = captionColor
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Tier 2: Transparent (outlined pill by default, or borderless if user toggled off)
        val transparentBorder = if (isTransparentSelected) currentShowBorder else true
        AtAGlanceCapsulePreview(
            cityName = cityName,
            isGpsLocation = isGpsLocation,
            weather = weather,
            palette = palette,
            backgroundAlpha = 0.0f,
            showBorder = transparentBorder,
            showDate = showDate,
            dateLongText = dateLongText,
            showCityOnWidget = showCityOnWidget,
            useFahrenheit = useFahrenheit,
            isSelectedHighlight = isTransparentSelected,
            onCardClick = { onSelectPreset(0.0f, true) },
            onMoreVertClick = onMoreVertClick,
            modifier = Modifier.testTag("preset_card_transparent")
        )
        StyleTierCaption(
            label = if (transparentBorder) "透明描边 · Transparent" else "全透无框 · Transparent",
            selected = isTransparentSelected,
            captionColor = captionColor
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Tier 3: Solid
        AtAGlanceCapsulePreview(
            cityName = cityName,
            isGpsLocation = isGpsLocation,
            weather = weather,
            palette = palette,
            backgroundAlpha = 1.0f,
            showBorder = false,
            showDate = showDate,
            dateLongText = dateLongText,
            showCityOnWidget = showCityOnWidget,
            useFahrenheit = useFahrenheit,
            isSelectedHighlight = isSolidSelected,
            onCardClick = { onSelectPreset(1.0f, false) },
            onMoreVertClick = onMoreVertClick,
            modifier = Modifier.testTag("preset_card_solid")
        )
        StyleTierCaption(
            label = "纯色实心 · Solid",
            selected = isSolidSelected,
            captionColor = captionColor
        )
    }
}

@Composable
private fun StyleTierCaption(
    label: String,
    selected: Boolean,
    captionColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF0B57D0),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = captionColor
        )
    }
}

@Composable
fun AtAGlanceCapsulePreview(
    cityName: String,
    isGpsLocation: Boolean,
    weather: WeatherSnapshot?,
    palette: WidgetPalette,
    backgroundAlpha: Float,
    showBorder: Boolean,
    showDate: Boolean,
    dateLongText: String,
    showCityOnWidget: Boolean,
    useFahrenheit: Boolean,
    isSelectedHighlight: Boolean = false,
    onCardClick: (() -> Unit)? = null,
    onMoreVertClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = resolveComposeWidgetColors(palette)
    val tempUnitStr = weather?.let {
        WeatherFormatter.formatTempWithUnit(it.temperatureC, useFahrenheit)
    } ?: if (useFahrenheit) "54°F" else "12°C"
    val conditionStr = weather?.let {
        WeatherFormatter.conditionLabel(it.weatherCode, it.isDay)
    } ?: "晴间多云"
    val iconRes = weather?.let {
        WeatherFormatter.conditionIconRes(it.weatherCode, it.isDay)
    } ?: R.drawable.ic_weather_partly_cloudy_day
    val pillShape = RoundedCornerShape(percent = 50)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(pillShape)
            .background(colors.bg.copy(alpha = backgroundAlpha))
            .then(
                when {
                    showBorder -> Modifier.border(1.5.dp, colors.border.copy(alpha = 0.85f), pillShape)
                    isSelectedHighlight -> Modifier.border(2.dp, Color(0xFF0B57D0).copy(alpha = 0.65f), pillShape)
                    else -> Modifier
                }
            )
            .then(if (onCardClick != null) Modifier.clickable { onCardClick() } else Modifier)
            .padding(start = 22.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (showDate) {
                Text(
                    text = dateLongText,
                    fontFamily = OutfitFontFamily,
                    color = colors.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (showCityOnWidget) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isGpsLocation) Icons.Filled.NearMe else Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$cityName · $conditionStr",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Text(
                    text = if (showCityOnWidget) "$cityName · $conditionStr" else conditionStr,
                    color = colors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }

        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier
                .clip(CircleShape)
                .then(if (onMoreVertClick != null) Modifier.clickable { onMoreVertClick() } else Modifier)
                .padding(6.dp)
                .size(20.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        WeatherSquircleBadgeCompose(
            iconRes = iconRes,
            tempWithUnit = tempUnitStr,
            badgeBg = colors.badgeBg,
            badgeText = colors.badgeText
        )
    }
}

@Composable
private fun WeatherSquircleBadgeCompose(
    iconRes: Int,
    tempWithUnit: String,
    badgeBg: Color,
    badgeText: Color
) {
    Column(
        modifier = Modifier
            .width(58.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(badgeBg)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = tempWithUnit,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = tempWithUnit,
            fontFamily = OutfitFontFamily,
            color = badgeText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun PixelMinimalComposePreview(
    cityName: String,
    isGpsLocation: Boolean,
    weather: WeatherSnapshot?,
    palette: WidgetPalette,
    backgroundAlpha: Float,
    showBorder: Boolean,
    showDate: Boolean,
    dateLongText: String,
    showCityOnWidget: Boolean,
    useFahrenheit: Boolean,
    onCityClick: (() -> Unit)?
) {
    val colors = resolveComposeWidgetColors(palette)
    val tempUnitStr = weather?.let {
        WeatherFormatter.formatTempWithUnit(it.temperatureC, useFahrenheit)
    } ?: "12°C"
    val conditionStr = weather?.let {
        WeatherFormatter.conditionLabel(it.weatherCode, it.isDay)
    } ?: "晴间多云"
    val iconRes = weather?.let {
        WeatherFormatter.conditionIconRes(it.weatherCode, it.isDay)
    } ?: R.drawable.ic_weather_partly_cloudy_day
    val shape = RoundedCornerShape(percent = 50)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.bg.copy(alpha = backgroundAlpha))
            .then(
                if (showBorder) Modifier.border(1.5.dp, colors.border.copy(alpha = 0.85f), shape)
                else Modifier
            )
            .then(if (onCityClick != null) Modifier.clickable { onCityClick() } else Modifier)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        if (showDate) {
            Text(
                text = dateLongText,
                fontFamily = OutfitFontFamily,
                color = colors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = conditionStr,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = buildString {
                    append("$tempUnitStr · $conditionStr")
                    if (showCityOnWidget) {
                        append(" · ")
                        append(if (isGpsLocation) "定位 $cityName" else cityName)
                    }
                },
                color = colors.textSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun GoogleRoundedSquarePreview(
    cityName: String,
    weather: WeatherSnapshot?,
    palette: WidgetPalette,
    backgroundAlpha: Float,
    showBorder: Boolean,
    showDate: Boolean,
    dateShortText: String,
    showCityOnWidget: Boolean,
    useFahrenheit: Boolean,
    onCityClick: (() -> Unit)?
) {
    val colors = resolveComposeWidgetColors(palette)
    val tempUnitStr = weather?.let {
        WeatherFormatter.formatTempWithUnit(it.temperatureC, useFahrenheit)
    } ?: "12°C"
    val conditionStr = weather?.let {
        WeatherFormatter.conditionLabel(it.weatherCode, it.isDay)
    } ?: "晴间多云"
    val highLowStr = weather?.let {
        "${WeatherFormatter.formatTemp(it.maxTempC, useFahrenheit)} / ${
            WeatherFormatter.formatTemp(it.minTempC, useFahrenheit)
        }"
    } ?: "16° / 8°"
    val iconRes = weather?.let {
        WeatherFormatter.conditionIconRes(it.weatherCode, it.isDay)
    } ?: R.drawable.ic_weather_partly_cloudy_day
    val shape = RoundedCornerShape(28.dp)

    Column(
        modifier = Modifier
            .size(188.dp)
            .clip(shape)
            .background(colors.bg.copy(alpha = backgroundAlpha))
            .then(
                if (showBorder) Modifier.border(1.5.dp, colors.border.copy(alpha = 0.85f), shape)
                else Modifier
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(if (onCityClick != null) Modifier.clickable { onCityClick() } else Modifier)
            ) {
                if (showDate) {
                    Text(
                        text = dateShortText,
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                if (showCityOnWidget || !showDate) {
                    Text(
                        text = cityName,
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conditionStr,
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = highLowStr,
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
            }
            WeatherSquircleBadgeCompose(
                iconRes = iconRes,
                tempWithUnit = tempUnitStr,
                badgeBg = colors.badgeBg,
                badgeText = colors.badgeText
            )
        }
    }
}

@Composable
private fun GoogleForecastCardPreview(
    cityName: String,
    weather: WeatherSnapshot?,
    palette: WidgetPalette,
    backgroundAlpha: Float,
    showBorder: Boolean,
    showDate: Boolean,
    dateLongText: String,
    showCityOnWidget: Boolean,
    useFahrenheit: Boolean,
    showDailyRows: Boolean,
    onCityClick: (() -> Unit)?
) {
    val colors = resolveComposeWidgetColors(palette)
    val tempUnitStr = weather?.let {
        WeatherFormatter.formatTempWithUnit(it.temperatureC, useFahrenheit)
    } ?: "12°C"
    val conditionStr = weather?.let {
        WeatherFormatter.conditionLabel(it.weatherCode, it.isDay)
    } ?: "晴间多云"
    val highLowStr = weather?.let {
        "${WeatherFormatter.formatTemp(it.maxTempC, useFahrenheit)} / ${
            WeatherFormatter.formatTemp(it.minTempC, useFahrenheit)
        }"
    } ?: "16° / 8°"
    val iconRes = weather?.let {
        WeatherFormatter.conditionIconRes(it.weatherCode, it.isDay)
    } ?: R.drawable.ic_weather_partly_cloudy_day
    val shape = RoundedCornerShape(30.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.bg.copy(alpha = backgroundAlpha))
            .then(
                if (showBorder) Modifier.border(1.5.dp, colors.border.copy(alpha = 0.85f), shape)
                else Modifier
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(if (onCityClick != null) Modifier.clickable { onCityClick() } else Modifier)
            ) {
                Text(
                    text = if (showDate) dateLongText else cityName,
                    fontFamily = OutfitFontFamily,
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = buildString {
                        if (showCityOnWidget && showDate) append("$cityName · ")
                        append("$conditionStr · $highLowStr")
                    },
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            WeatherSquircleBadgeCompose(
                iconRes = iconRes,
                tempWithUnit = tempUnitStr,
                badgeBg = colors.badgeBg,
                badgeText = colors.badgeText
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val hourlyList = weather?.hourly?.take(5).orEmpty()
        if (hourlyList.isNotEmpty()) {
            val innerAlpha = if (backgroundAlpha > 0.12f) {
                (backgroundAlpha * 0.65f).coerceIn(0.10f, 0.85f)
            } else 0f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.inner.copy(alpha = innerAlpha))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                hourlyList.forEach { hr ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = WeatherFormatter.formatTemp(hr.temperatureC, useFahrenheit),
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Image(
                            painter = painterResource(
                                id = WeatherFormatter.conditionIconRes(hr.weatherCode, hr.isDay)
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = hr.timeLabel,
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        if (showDailyRows && weather != null && weather.daily.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            weather.daily.drop(1).take(3).forEach { day ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = day.dayLabel,
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(52.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Image(
                        painter = painterResource(
                            id = WeatherFormatter.conditionIconRes(day.weatherCode, true)
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${WeatherFormatter.formatTemp(day.maxTempC, useFahrenheit)} / ${
                            WeatherFormatter.formatTemp(day.minTempC, useFahrenheit)
                        }",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
