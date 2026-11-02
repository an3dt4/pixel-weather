package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.R
import com.example.data.model.WeatherFormatter
import com.example.data.model.WidgetPalette
import com.example.data.model.WidgetStyle
import com.example.data.repository.ResolvedWidgetPayload
import com.example.data.repository.WeatherRepository
import com.example.ui.theme.WidgetPaletteColors

val PARAM_KEY_APPWIDGET_ID = ActionParameters.Key<Int>(AppWidgetManager.EXTRA_APPWIDGET_ID)
val PARAM_KEY_FOCUS_SEARCH = ActionParameters.Key<Boolean>(WidgetConfigureActivity.EXTRA_FOCUS_SEARCH)
val PARAM_KEY_CITY_ID = ActionParameters.Key<String>("extra_selected_city_id")
val PARAM_KEY_OPEN_TAB = ActionParameters.Key<String>("extra_open_tab")

private data class GlancePaletteSpec(
    val containerBg: Color,
    val innerBg: Color,
    val border: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val badgeBg: Color,
    val badgeText: Color
)

private fun resolveGlancePalette(palette: WidgetPalette): GlancePaletteSpec = when (palette) {
    WidgetPalette.LIGHT_PILL -> GlancePaletteSpec(
        containerBg = WidgetPaletteColors.LightPillBg,
        innerBg = WidgetPaletteColors.LightPillInner,
        border = WidgetPaletteColors.LightPillBorder,
        primaryText = WidgetPaletteColors.LightPillTextPrimary,
        secondaryText = WidgetPaletteColors.LightPillTextSecondary,
        badgeBg = WidgetPaletteColors.LightPillBadgeBg,
        badgeText = WidgetPaletteColors.LightPillBadgeText
    )
    WidgetPalette.DARK_PILL -> GlancePaletteSpec(
        containerBg = WidgetPaletteColors.DarkPillBg,
        innerBg = WidgetPaletteColors.DarkPillInner,
        border = WidgetPaletteColors.DarkPillBorder,
        primaryText = WidgetPaletteColors.DarkPillTextPrimary,
        secondaryText = WidgetPaletteColors.DarkPillTextSecondary,
        badgeBg = WidgetPaletteColors.DarkPillBadgeBg,
        badgeText = WidgetPaletteColors.DarkPillBadgeText
    )
    WidgetPalette.DYNAMIC_SKY -> GlancePaletteSpec(
        containerBg = WidgetPaletteColors.DynamicBg,
        innerBg = WidgetPaletteColors.DynamicInner,
        border = WidgetPaletteColors.DynamicBorder,
        primaryText = WidgetPaletteColors.DynamicTextPrimary,
        secondaryText = WidgetPaletteColors.DynamicTextSecondary,
        badgeBg = WidgetPaletteColors.DynamicBadgeBg,
        badgeText = WidgetPaletteColors.DynamicBadgeText
    )
    WidgetPalette.WARM_SUN -> GlancePaletteSpec(
        containerBg = WidgetPaletteColors.WarmBg,
        innerBg = WidgetPaletteColors.WarmInner,
        border = WidgetPaletteColors.WarmBorder,
        primaryText = WidgetPaletteColors.WarmTextPrimary,
        secondaryText = WidgetPaletteColors.WarmTextSecondary,
        badgeBg = WidgetPaletteColors.WarmBadgeBg,
        badgeText = WidgetPaletteColors.WarmBadgeText
    )
}

private fun buildOpenMainAction(cityId: String): Action =
    actionStartActivity<MainActivity>(actionParametersOf(PARAM_KEY_CITY_ID to cityId))

private fun buildConfigureAction(appWidgetId: Int, focusSearch: Boolean = false): Action =
    if (appWidgetId > 0) {
        actionStartActivity<WidgetConfigureActivity>(
            actionParametersOf(
                PARAM_KEY_APPWIDGET_ID to appWidgetId,
                PARAM_KEY_FOCUS_SEARCH to focusSearch
            )
        )
    } else {
        actionStartActivity<MainActivity>(actionParametersOf(PARAM_KEY_OPEN_TAB to "widgets"))
    }

@Composable
private fun AtAGlanceSurfaceContainer(
    widthDp: Float,
    heightDp: Float,
    colors: GlancePaletteSpec,
    alpha: Float,
    showBorder: Boolean,
    isStadiumPill: Boolean,
    cornerRadiusDp: Float = 28f,
    clickAction: Action,
    modifier: GlanceModifier = GlanceModifier.fillMaxSize(),
    content: @Composable () -> Unit
) {
    val clamped = alpha.coerceIn(0f, 1f)
    if (clamped <= 0.005f && !showBorder) {
        Box(
            modifier = modifier.clickable(clickAction),
            contentAlignment = Alignment.CenterStart
        ) {
            content()
        }
    } else {
        Box(
            modifier = modifier.clickable(clickAction),
            contentAlignment = Alignment.CenterStart
        ) {
            Image(
                provider = ImageProvider(
                    WidgetBitmapRenderer.createAtAGlanceContainerBitmap(
                        widthDp = widthDp,
                        heightDp = heightDp,
                        fillColor = colors.containerBg,
                        alpha = clamped,
                        showBorder = showBorder,
                        borderColor = colors.border,
                        isStadiumPill = isStadiumPill,
                        cornerRadiusDp = cornerRadiusDp
                    )
                ),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = GlanceModifier.fillMaxSize()
            )
            content()
        }
    }
}

@Composable
private fun WeatherSquircleBadgeGlance(
    iconRes: Int,
    tempWithUnitText: String,
    badgeBg: Color,
    badgeText: Color,
    clickAction: Action,
    widthDp: Dp = 58.dp,
    heightDp: Dp = 54.dp
) {
    Box(
        modifier = GlanceModifier
            .width(widthDp)
            .height(heightDp)
            .clickable(clickAction),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(
                WidgetBitmapRenderer.createWeatherSquircleBitmap(
                    badgeColor = badgeBg,
                    widthDp = widthDp.value,
                    heightDp = heightDp.value,
                    cornerRadiusDp = 18f
                )
            ),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier.fillMaxSize()
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = tempWithUnitText,
                modifier = GlanceModifier.size(22.dp)
            )
            Spacer(modifier = GlanceModifier.height(1.dp))
            Text(
                text = tempWithUnitText,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(badgeText),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
fun GoogleStyleWidgetRoot(payload: ResolvedWidgetPayload) {
    val size = LocalSize.current
    when (payload.style) {
        WidgetStyle.PIXEL_AT_A_GLANCE -> {
            if (size.width < 190.dp && size.height >= 115.dp) {
                RoundedSquareGlanceWidget(payload)
            } else {
                AtAGlanceCapsuleWidget(payload)
            }
        }
        WidgetStyle.FORECAST_CARD -> ForecastCardGlanceWidget(payload)
        WidgetStyle.PIXEL_MINIMAL -> PixelMinimalGlanceWidget(payload)
        WidgetStyle.ROUNDED_SQUARE -> RoundedSquareGlanceWidget(payload)
    }
}

/**
 * 1. Flagship At a Glance Capsule Widget (1:1 match to reference screenshot):
 * [ Monday, Nov 2 / 11月2日 星期一 (+ optional custom city)    ⋮   [ ⛅ / 12°C ] ]
 */
@Composable
private fun AtAGlanceCapsuleWidget(payload: ResolvedWidgetPayload) {
    val size = LocalSize.current
    val colors = resolveGlancePalette(payload.palette)
    val snapshot = payload.weather
    val tempUnitText = snapshot?.let {
        WeatherFormatter.formatTempWithUnit(it.temperatureC, payload.useFahrenheit)
    } ?: "--°C"
    val conditionText = snapshot?.let {
        WeatherFormatter.conditionLabel(it.weatherCode, it.isDay)
    } ?: "同步中"
    val iconRes = snapshot?.let {
        WeatherFormatter.conditionIconRes(it.weatherCode, it.isDay)
    } ?: R.drawable.ic_weather_partly_cloudy_day

    val openMainAction = buildOpenMainAction(payload.cityId)
    val configureAction = buildConfigureAction(payload.appWidgetId, focusSearch = false)
    val pillWidthDp = size.width.value.coerceAtLeast(240f)
    val pillHeightDp = size.height.value.coerceIn(64f, 84f)

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AtAGlanceSurfaceContainer(
            widthDp = pillWidthDp,
            heightDp = pillHeightDp,
            colors = colors,
            alpha = payload.backgroundAlpha,
            showBorder = payload.showBorder,
            isStadiumPill = true,
            clickAction = openMainAction,
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(pillHeightDp.dp)
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(start = 22.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(configureAction)
                ) {
                    if (payload.showDate) {
                        Text(
                            text = payload.dateLongText,
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(colors.primaryText),
                                fontSize = if (size.width >= 250.dp) 19.sp else 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        if (payload.showCityOnWidget) {
                            Spacer(modifier = GlanceModifier.height(1.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    provider = ImageProvider(
                                        if (payload.isGpsLocation) R.drawable.ic_widget_gps
                                        else R.drawable.ic_widget_location_pin
                                    ),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(ColorProvider(colors.secondaryText)),
                                    modifier = GlanceModifier.size(10.dp)
                                )
                                Spacer(modifier = GlanceModifier.width(3.dp))
                                Text(
                                    text = "${payload.cityName} · $conditionText",
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = ColorProvider(colors.secondaryText),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                )
                            }
                        }
                    } else {
                        Text(
                            text = if (payload.showCityOnWidget) {
                                "${payload.cityName} · $conditionText"
                            } else {
                                conditionText
                            },
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(colors.primaryText),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                // Vertical 3-dots customization button (⋮)
                Box(
                    modifier = GlanceModifier
                        .width(28.dp)
                        .height(48.dp)
                        .clickable(configureAction),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_more_vert),
                        contentDescription = "自定义微件",
                        colorFilter = ColorFilter.tint(ColorProvider(colors.primaryText)),
                        modifier = GlanceModifier.size(19.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.width(6.dp))

                // Deep-blue weather squircle badge ([ ⛅ / 12°C ])
                WeatherSquircleBadgeGlance(
                    iconRes = iconRes,
                    tempWithUnitText = tempUnitText,
                    badgeBg = colors.badgeBg,
                    badgeText = colors.badgeText,
                    clickAction = openMainAction
                )
            }
        }
    }
}

/**
 * 2. Minimal Borderless Pixel Launcher Two-Line At a Glance
 */
@Composable
private fun PixelMinimalGlanceWidget(payload: ResolvedWidgetPayload) {
    val size = LocalSize.current
    val colors = resolveGlancePalette(payload.palette)
    val snapshot = payload.weather
    val tempUnitText = snapshot?.let {
        WeatherFormatter.formatTempWithUnit(it.temperatureC, payload.useFahrenheit)
    } ?: "--°C"
    val conditionText = snapshot?.let {
        WeatherFormatter.conditionLabel(it.weatherCode, it.isDay)
    } ?: "同步中"
    val iconRes = snapshot?.let {
        WeatherFormatter.conditionIconRes(it.weatherCode, it.isDay)
    } ?: R.drawable.ic_weather_partly_cloudy_day

    val openMainAction = buildOpenMainAction(payload.cityId)
    val configureAction = buildConfigureAction(payload.appWidgetId, focusSearch = false)
    val hPad = if (payload.backgroundAlpha <= 0.01f && !payload.showBorder) 8.dp else 18.dp

    AtAGlanceSurfaceContainer(
        widthDp = size.width.value.coerceAtLeast(220f),
        heightDp = size.height.value.coerceAtLeast(62f),
        colors = colors,
        alpha = payload.backgroundAlpha,
        showBorder = payload.showBorder,
        isStadiumPill = true,
        clickAction = openMainAction
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = hPad, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (payload.showDate) {
                Text(
                    text = payload.dateLongText,
                    maxLines = 1,
                    modifier = GlanceModifier.clickable(configureAction),
                    style = TextStyle(
                        color = ColorProvider(colors.primaryText),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = GlanceModifier.height(3.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(iconRes),
                    contentDescription = conditionText,
                    modifier = GlanceModifier.size(20.dp)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = buildString {
                        append("$tempUnitText · $conditionText")
                        if (payload.showCityOnWidget) {
                            append(" · ${payload.cityName}")
                        }
                    },
                    maxLines = 1,
                    modifier = GlanceModifier.clickable(configureAction),
                    style = TextStyle(
                        color = ColorProvider(colors.secondaryText),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

/**
 * 3. Compact 2×2 At a Glance Square Card
 */
@Composable
private fun RoundedSquareGlanceWidget(payload: ResolvedWidgetPayload) {
    val size = LocalSize.current
    val colors = resolveGlancePalette(payload.palette)
    val snapshot = payload.weather
    val tempUnitText = snapshot?.let {
        WeatherFormatter.formatTempWithUnit(it.temperatureC, payload.useFahrenheit)
    } ?: "--°C"
    val conditionText = snapshot?.let {
        WeatherFormatter.conditionLabel(it.weatherCode, it.isDay)
    } ?: "同步中"
    val highLowText = snapshot?.let {
        "${WeatherFormatter.formatTemp(it.maxTempC, payload.useFahrenheit)} / ${
            WeatherFormatter.formatTemp(it.minTempC, payload.useFahrenheit)
        }"
    } ?: ""
    val iconRes = snapshot?.let {
        WeatherFormatter.conditionIconRes(it.weatherCode, it.isDay)
    } ?: R.drawable.ic_weather_partly_cloudy_day

    val openMainAction = buildOpenMainAction(payload.cityId)
    val configureAction = buildConfigureAction(payload.appWidgetId, focusSearch = false)

    AtAGlanceSurfaceContainer(
        widthDp = size.width.value.coerceAtLeast(150f),
        heightDp = size.height.value.coerceAtLeast(150f),
        colors = colors,
        alpha = payload.backgroundAlpha,
        showBorder = payload.showBorder,
        isStadiumPill = false,
        cornerRadiusDp = 28f,
        clickAction = openMainAction
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(configureAction)
                ) {
                    if (payload.showDate) {
                        Text(
                            text = payload.dateShortText,
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(colors.primaryText),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    if (payload.showCityOnWidget || !payload.showDate) {
                        Text(
                            text = payload.cityName,
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(colors.secondaryText),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Box(
                    modifier = GlanceModifier
                        .size(28.dp)
                        .clickable(configureAction),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_more_vert),
                        contentDescription = "设置",
                        colorFilter = ColorFilter.tint(ColorProvider(colors.primaryText)),
                        modifier = GlanceModifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = conditionText,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(colors.primaryText),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    if (highLowText.isNotEmpty()) {
                        Text(
                            text = highLowText,
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(colors.secondaryText),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                WeatherSquircleBadgeGlance(
                    iconRes = iconRes,
                    tempWithUnitText = tempUnitText,
                    badgeBg = colors.badgeBg,
                    badgeText = colors.badgeText,
                    clickAction = openMainAction
                )
            }
        }
    }
}

/**
 * 4. Expanded At a Glance Hourly & Multi-Day Forecast Card (4×2 / 4×3)
 */
@Composable
private fun ForecastCardGlanceWidget(payload: ResolvedWidgetPayload) {
    val size = LocalSize.current
    val colors = resolveGlancePalette(payload.palette)
    val snapshot = payload.weather
    val tempUnitText = snapshot?.let {
        WeatherFormatter.formatTempWithUnit(it.temperatureC, payload.useFahrenheit)
    } ?: "--°C"
    val conditionText = snapshot?.let {
        WeatherFormatter.conditionLabel(it.weatherCode, it.isDay)
    } ?: "正在同步天气"
    val highLowText = snapshot?.let {
        "${WeatherFormatter.formatTemp(it.maxTempC, payload.useFahrenheit)} / ${
            WeatherFormatter.formatTemp(it.minTempC, payload.useFahrenheit)
        }"
    } ?: ""
    val iconRes = snapshot?.let {
        WeatherFormatter.conditionIconRes(it.weatherCode, it.isDay)
    } ?: R.drawable.ic_weather_partly_cloudy_day

    val openMainAction = buildOpenMainAction(payload.cityId)
    val configureAction = buildConfigureAction(payload.appWidgetId, focusSearch = false)
    val hourlyCount = if (size.width >= 260.dp) 5 else 4
    val showDailyRows = size.height >= 195.dp

    AtAGlanceSurfaceContainer(
        widthDp = size.width.value.coerceAtLeast(260f),
        heightDp = size.height.value.coerceAtLeast(130f),
        colors = colors,
        alpha = payload.backgroundAlpha,
        showBorder = payload.showBorder,
        isStadiumPill = false,
        cornerRadiusDp = 30f,
        clickAction = openMainAction
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top At a Glance Bar inside Forecast Card
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(configureAction)
                ) {
                    Text(
                        text = if (payload.showDate) payload.dateLongText else payload.cityName,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(colors.primaryText),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = buildString {
                            if (payload.showCityOnWidget && payload.showDate) {
                                append("${payload.cityName} · ")
                            }
                            append(conditionText)
                            if (highLowText.isNotEmpty()) {
                                append(" · $highLowText")
                            }
                        },
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(colors.secondaryText),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }

                Box(
                    modifier = GlanceModifier
                        .width(28.dp)
                        .height(48.dp)
                        .clickable(configureAction),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_more_vert),
                        contentDescription = "设置",
                        colorFilter = ColorFilter.tint(ColorProvider(colors.primaryText)),
                        modifier = GlanceModifier.size(19.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.width(6.dp))

                WeatherSquircleBadgeGlance(
                    iconRes = iconRes,
                    tempWithUnitText = tempUnitText,
                    badgeBg = colors.badgeBg,
                    badgeText = colors.badgeText,
                    clickAction = openMainAction
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            val hourlyItems = snapshot?.hourly?.take(hourlyCount).orEmpty()
            if (hourlyItems.isNotEmpty()) {
                val stripModifier = if (
                    payload.backgroundAlpha > 0.12f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ) {
                    val innerAlpha = (payload.backgroundAlpha * 0.65f).coerceIn(0.10f, 0.85f)
                    GlanceModifier
                        .fillMaxWidth()
                        .cornerRadius(18.dp)
                        .background(ColorProvider(colors.innerBg.copy(alpha = innerAlpha)))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                } else {
                    GlanceModifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                }

                Row(
                    modifier = stripModifier,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    hourlyItems.forEach { hr ->
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = WeatherFormatter.formatTemp(hr.temperatureC, payload.useFahrenheit),
                                maxLines = 1,
                                style = TextStyle(
                                    color = ColorProvider(colors.primaryText),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Image(
                                provider = ImageProvider(
                                    WeatherFormatter.conditionIconRes(hr.weatherCode, hr.isDay)
                                ),
                                contentDescription = null,
                                modifier = GlanceModifier.size(20.dp)
                            )
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                text = hr.timeLabel,
                                maxLines = 1,
                                style = TextStyle(
                                    color = ColorProvider(colors.secondaryText),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }

            if (showDailyRows && snapshot != null && snapshot.daily.size > 1) {
                Spacer(modifier = GlanceModifier.height(8.dp))
                snapshot.daily.drop(1).take(3).forEach { day ->
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = day.dayLabel,
                            modifier = GlanceModifier.width(48.dp),
                            style = TextStyle(
                                color = ColorProvider(colors.primaryText),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Image(
                            provider = ImageProvider(
                                WeatherFormatter.conditionIconRes(day.weatherCode, true)
                            ),
                            contentDescription = null,
                            modifier = GlanceModifier.size(20.dp)
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "${WeatherFormatter.formatTemp(day.maxTempC, payload.useFahrenheit)} / ${
                                WeatherFormatter.formatTemp(day.minTempC, payload.useFahrenheit)
                            }",
                            style = TextStyle(
                                color = ColorProvider(colors.secondaryText),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

class RefreshWeatherActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val repository = WeatherRepository.getInstance(context)
        val cityId = parameters[PARAM_KEY_CITY_ID]
        if (!cityId.isNullOrBlank()) {
            repository.refreshCityWeather(cityId)
        } else {
            repository.refreshAllSavedCities()
        }
        WeatherWidgetUpdater.updateAllWidgets(context)
    }
}
