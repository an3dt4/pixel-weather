package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
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
    val primaryText: Color,
    val secondaryText: Color,
    val accent: Color
)

private fun resolveGlancePalette(palette: WidgetPalette): GlancePaletteSpec = when (palette) {
    WidgetPalette.DYNAMIC_SKY -> GlancePaletteSpec(
        WidgetPaletteColors.DynamicBg,
        WidgetPaletteColors.DynamicInner,
        WidgetPaletteColors.DynamicTextPrimary,
        WidgetPaletteColors.DynamicTextSecondary,
        WidgetPaletteColors.DynamicAccent
    )
    WidgetPalette.DAYLIGHT_BLUE -> GlancePaletteSpec(
        WidgetPaletteColors.DaylightBg,
        WidgetPaletteColors.DaylightInner,
        WidgetPaletteColors.DaylightTextPrimary,
        WidgetPaletteColors.DaylightTextSecondary,
        WidgetPaletteColors.DaylightAccent
    )
    WidgetPalette.MIDNIGHT_INDIGO -> GlancePaletteSpec(
        WidgetPaletteColors.MidnightBg,
        WidgetPaletteColors.MidnightInner,
        WidgetPaletteColors.MidnightTextPrimary,
        WidgetPaletteColors.MidnightTextSecondary,
        WidgetPaletteColors.MidnightAccent
    )
    WidgetPalette.WARM_SUN -> GlancePaletteSpec(
        WidgetPaletteColors.WarmBg,
        WidgetPaletteColors.WarmInner,
        WidgetPaletteColors.WarmTextPrimary,
        WidgetPaletteColors.WarmTextSecondary,
        WidgetPaletteColors.WarmAccent
    )
}

@Composable
fun GoogleStyleWidgetRoot(payload: ResolvedWidgetPayload) {
    when (payload.style) {
        WidgetStyle.DIAGONAL_PILL -> DiagonalPillGlanceWidget(payload)
        WidgetStyle.ROUNDED_SQUARE -> RoundedSquareGlanceWidget(payload)
        WidgetStyle.FORECAST_CARD -> ForecastCardGlanceWidget(payload)
    }
}

@Composable
private fun DiagonalPillGlanceWidget(payload: ResolvedWidgetPayload) {
    val colors = resolveGlancePalette(payload.palette)
    val snapshot = payload.weather
    val tempText = if (snapshot != null) {
        WeatherFormatter.formatTemp(snapshot.temperatureC, payload.useFahrenheit)
    } else {
        "--°"
    }
    val iconRes = if (snapshot != null) {
        WeatherFormatter.conditionIconRes(snapshot.weatherCode, snapshot.isDay)
    } else {
        R.drawable.ic_weather_partly_cloudy_day
    }

    val openMainAction = actionStartActivity<MainActivity>(
        actionParametersOf(PARAM_KEY_CITY_ID to payload.cityId)
    )
    val searchOrConfigAction = if (payload.appWidgetId > 0) {
        actionStartActivity<WidgetConfigureActivity>(
            actionParametersOf(
                PARAM_KEY_APPWIDGET_ID to payload.appWidgetId,
                PARAM_KEY_FOCUS_SEARCH to true
            )
        )
    } else {
        actionStartActivity<MainActivity>(
            actionParametersOf(PARAM_KEY_OPEN_TAB to "cities")
        )
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(2.dp)
            .clickable(openMainAction),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(payload.palette.diagonalPillDrawable),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = GlanceModifier.fillMaxSize()
        )

        // Upper-right quadrant of tilted Google Pill: Weather Icon + Tappable City Search Pill
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(top = 18.dp, end = 22.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = GlanceModifier
                    .cornerRadius(14.dp)
                    .clickable(searchOrConfigAction)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Image(
                    provider = ImageProvider(iconRes),
                    contentDescription = payload.cityName,
                    modifier = GlanceModifier.size(34.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(
                            if (payload.isGpsLocation) R.drawable.ic_widget_gps
                            else R.drawable.ic_widget_location_pin
                        ),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(ColorProvider(colors.secondaryText)),
                        modifier = GlanceModifier.size(11.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(2.dp))
                    Text(
                        text = payload.cityName,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(colors.secondaryText),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(2.dp))
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_search),
                        contentDescription = "搜索城市",
                        colorFilter = ColorFilter.tint(ColorProvider(colors.accent)),
                        modifier = GlanceModifier.size(11.dp)
                    )
                }
            }
        }

        // Lower-left quadrant of tilted Google Pill: Giant Temperature
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(start = 24.dp, bottom = 20.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = tempText,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(colors.primaryText),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun RoundedSquareGlanceWidget(payload: ResolvedWidgetPayload) {
    val colors = resolveGlancePalette(payload.palette)
    val snapshot = payload.weather
    val tempText = snapshot?.let {
        WeatherFormatter.formatTemp(it.temperatureC, payload.useFahrenheit)
    } ?: "--°"
    val conditionText = snapshot?.let {
        WeatherFormatter.conditionLabel(it.weatherCode, it.isDay)
    } ?: "点击刷新"
    val highLowText = snapshot?.let {
        "${WeatherFormatter.formatTemp(it.maxTempC, payload.useFahrenheit)} / ${
            WeatherFormatter.formatTemp(it.minTempC, payload.useFahrenheit)
        }"
    } ?: ""
    val iconRes = snapshot?.let {
        WeatherFormatter.conditionIconRes(it.weatherCode, it.isDay)
    } ?: R.drawable.ic_weather_partly_cloudy_day

    val openMainAction = actionStartActivity<MainActivity>(
        actionParametersOf(PARAM_KEY_CITY_ID to payload.cityId)
    )
    val searchOrConfigAction = if (payload.appWidgetId > 0) {
        actionStartActivity<WidgetConfigureActivity>(
            actionParametersOf(
                PARAM_KEY_APPWIDGET_ID to payload.appWidgetId,
                PARAM_KEY_FOCUS_SEARCH to true
            )
        )
    } else {
        actionStartActivity<MainActivity>(
            actionParametersOf(PARAM_KEY_OPEN_TAB to "cities")
        )
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(ColorProvider(colors.containerBg))
            .clickable(openMainAction)
            .padding(14.dp)
    ) {
        // Top Row: City Search Pill (tap to search/switch city) + Refresh Button
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier
                    .cornerRadius(12.dp)
                    .background(ColorProvider(colors.innerBg))
                    .clickable(searchOrConfigAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Image(
                    provider = ImageProvider(
                        if (payload.isGpsLocation) R.drawable.ic_widget_gps
                        else R.drawable.ic_widget_location_pin
                    ),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(ColorProvider(colors.accent)),
                    modifier = GlanceModifier.size(12.dp)
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = payload.cityName,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(colors.primaryText),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_search),
                    contentDescription = "搜索城市",
                    colorFilter = ColorFilter.tint(ColorProvider(colors.accent)),
                    modifier = GlanceModifier.size(12.dp)
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            Box(
                modifier = GlanceModifier
                    .size(28.dp)
                    .cornerRadius(14.dp)
                    .clickable(
                        actionRunCallback<RefreshWeatherActionCallback>(
                            actionParametersOf(PARAM_KEY_CITY_ID to payload.cityId)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_refresh),
                    contentDescription = "刷新",
                    colorFilter = ColorFilter.tint(ColorProvider(colors.secondaryText)),
                    modifier = GlanceModifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = GlanceModifier.defaultWeight())

        // Middle Row: Giant Temp + Multi-tone Icon
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tempText,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(colors.primaryText),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = conditionText,
                modifier = GlanceModifier.size(42.dp)
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // Bottom Row: Condition + High/Low
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = conditionText,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(colors.secondaryText),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = highLowText,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(colors.secondaryText),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun ForecastCardGlanceWidget(payload: ResolvedWidgetPayload) {
    val size = LocalSize.current
    val colors = resolveGlancePalette(payload.palette)
    val snapshot = payload.weather
    val tempText = snapshot?.let {
        WeatherFormatter.formatTemp(it.temperatureC, payload.useFahrenheit)
    } ?: "--°"
    val conditionText = snapshot?.let {
        WeatherFormatter.conditionLabel(it.weatherCode, it.isDay)
    } ?: "正在同步天气"
    val highLowText = snapshot?.let {
        "最高 ${WeatherFormatter.formatTemp(it.maxTempC, payload.useFahrenheit)} · 最低 ${
            WeatherFormatter.formatTemp(it.minTempC, payload.useFahrenheit)
        }"
    } ?: ""
    val iconRes = snapshot?.let {
        WeatherFormatter.conditionIconRes(it.weatherCode, it.isDay)
    } ?: R.drawable.ic_weather_partly_cloudy_day

    val openMainAction = actionStartActivity<MainActivity>(
        actionParametersOf(PARAM_KEY_CITY_ID to payload.cityId)
    )
    val quickSwitchAction = if (payload.appWidgetId > 0) {
        actionStartActivity<WidgetConfigureActivity>(
            actionParametersOf(
                PARAM_KEY_APPWIDGET_ID to payload.appWidgetId,
                PARAM_KEY_FOCUS_SEARCH to false
            )
        )
    } else {
        actionStartActivity<MainActivity>(
            actionParametersOf(PARAM_KEY_OPEN_TAB to "cities")
        )
    }
    val directSearchAction = if (payload.appWidgetId > 0) {
        actionStartActivity<WidgetConfigureActivity>(
            actionParametersOf(
                PARAM_KEY_APPWIDGET_ID to payload.appWidgetId,
                PARAM_KEY_FOCUS_SEARCH to true
            )
        )
    } else {
        actionStartActivity<MainActivity>(
            actionParametersOf(PARAM_KEY_OPEN_TAB to "cities")
        )
    }

    val hourlyCount = if (size.width >= 260.dp) 5 else 4
    val showDailyRows = size.height >= 195.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(ColorProvider(colors.containerBg))
            .clickable(openMainAction)
            .padding(14.dp)
    ) {
        // Top Header: Quick-Access City Chip + Dedicated Search Icon Button + Refresh Button
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier
                    .cornerRadius(14.dp)
                    .background(ColorProvider(colors.innerBg))
                    .clickable(quickSwitchAction)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Image(
                    provider = ImageProvider(
                        if (payload.isGpsLocation) R.drawable.ic_widget_gps
                        else R.drawable.ic_widget_location_pin
                    ),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(ColorProvider(colors.accent)),
                    modifier = GlanceModifier.size(13.dp)
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = payload.cityName,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(colors.primaryText),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = if (payload.isGpsLocation) "定位" else "换城",
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(colors.accent),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = GlanceModifier.width(6.dp))

            // Dedicated Search City Button right on the Forecast Widget
            Box(
                modifier = GlanceModifier
                    .size(28.dp)
                    .cornerRadius(14.dp)
                    .background(ColorProvider(colors.innerBg))
                    .clickable(directSearchAction),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_search),
                    contentDescription = "搜索城市",
                    colorFilter = ColorFilter.tint(ColorProvider(colors.accent)),
                    modifier = GlanceModifier.size(15.dp)
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            Box(
                modifier = GlanceModifier
                    .size(28.dp)
                    .cornerRadius(14.dp)
                    .clickable(
                        actionRunCallback<RefreshWeatherActionCallback>(
                            actionParametersOf(PARAM_KEY_CITY_ID to payload.cityId)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_refresh),
                    contentDescription = "刷新天气",
                    colorFilter = ColorFilter.tint(ColorProvider(colors.secondaryText)),
                    modifier = GlanceModifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // Main Current Row
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tempText,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(colors.primaryText),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(iconRes),
                        contentDescription = conditionText,
                        modifier = GlanceModifier.size(26.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = conditionText,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(colors.primaryText),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                if (highLowText.isNotEmpty()) {
                    Text(
                        text = highLowText,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(colors.secondaryText),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Hourly Forecast Inner Pill Strip
        val hourlyItems = snapshot?.hourly?.take(hourlyCount).orEmpty()
        if (hourlyItems.isNotEmpty()) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .cornerRadius(16.dp)
                    .background(ColorProvider(colors.innerBg))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
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

        // Expanded Daily Forecast Rows when resized vertically (4x3 / 4x4)
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
