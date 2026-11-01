package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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

data class ComposeWidgetColors(
    val bg: Color,
    val inner: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color
)

fun resolveComposeWidgetColors(palette: WidgetPalette): ComposeWidgetColors = when (palette) {
    WidgetPalette.DYNAMIC_SKY -> ComposeWidgetColors(
        WidgetPaletteColors.DynamicBg,
        WidgetPaletteColors.DynamicInner,
        WidgetPaletteColors.DynamicTextPrimary,
        WidgetPaletteColors.DynamicTextSecondary,
        WidgetPaletteColors.DynamicAccent
    )
    WidgetPalette.DAYLIGHT_BLUE -> ComposeWidgetColors(
        WidgetPaletteColors.DaylightBg,
        WidgetPaletteColors.DaylightInner,
        WidgetPaletteColors.DaylightTextPrimary,
        WidgetPaletteColors.DaylightTextSecondary,
        WidgetPaletteColors.DaylightAccent
    )
    WidgetPalette.MIDNIGHT_INDIGO -> ComposeWidgetColors(
        WidgetPaletteColors.MidnightBg,
        WidgetPaletteColors.MidnightInner,
        WidgetPaletteColors.MidnightTextPrimary,
        WidgetPaletteColors.MidnightTextSecondary,
        WidgetPaletteColors.MidnightAccent
    )
    WidgetPalette.WARM_SUN -> ComposeWidgetColors(
        WidgetPaletteColors.WarmBg,
        WidgetPaletteColors.WarmInner,
        WidgetPaletteColors.WarmTextPrimary,
        WidgetPaletteColors.WarmTextSecondary,
        WidgetPaletteColors.WarmAccent
    )
}

/**
 * Realistic Pixel Home Screen Wallpaper Stage displaying a 1:1 live preview of the Google Weather widget.
 */
@Composable
fun PixelWallpaperWidgetStage(
    cityName: String,
    isGpsLocation: Boolean,
    weather: WeatherSnapshot?,
    style: WidgetStyle,
    palette: WidgetPalette,
    useFahrenheit: Boolean,
    expandedTallForecast: Boolean = false,
    onCityPillClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
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
        Image(
            painter = painterResource(id = R.drawable.img_pixel_wallpaper_preview_1793542593775),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // Subtle vignette for home screen realism
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.10f),
                            Color.Black.copy(alpha = 0.04f),
                            Color.Black.copy(alpha = 0.16f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mock Pixel At-a-Glance status bar hint
            Surface(
                color = Color.Black.copy(alpha = 0.24f),
                shape = CircleShape
            ) {
                Text(
                    text = if (isGpsLocation) {
                        "桌面实时预览 · GPS 实时定位模式"
                    } else {
                        "桌面实时预览 · 自选城市（无需定位权限）"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
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
                    WidgetStyle.DIAGONAL_PILL -> GoogleDiagonalPillPreview(
                        cityName = cityName,
                        isGpsLocation = isGpsLocation,
                        weather = weather,
                        palette = targetPalette,
                        useFahrenheit = useFahrenheit,
                        onCityClick = onCityPillClick
                    )
                    WidgetStyle.ROUNDED_SQUARE -> GoogleRoundedSquarePreview(
                        cityName = cityName,
                        isGpsLocation = isGpsLocation,
                        weather = weather,
                        palette = targetPalette,
                        useFahrenheit = useFahrenheit,
                        onCityClick = onCityPillClick
                    )
                    WidgetStyle.FORECAST_CARD -> GoogleForecastCardPreview(
                        cityName = cityName,
                        isGpsLocation = isGpsLocation,
                        weather = weather,
                        palette = targetPalette,
                        useFahrenheit = useFahrenheit,
                        showDailyRows = isTall,
                        onCityClick = onCityPillClick
                    )
                }
            }
        }
    }
}

@Composable
fun GoogleDiagonalPillPreview(
    cityName: String,
    isGpsLocation: Boolean,
    weather: WeatherSnapshot?,
    palette: WidgetPalette,
    useFahrenheit: Boolean,
    onCityClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = resolveComposeWidgetColors(palette)
    val tempStr = weather?.let {
        WeatherFormatter.formatTemp(it.temperatureC, useFahrenheit)
    } ?: "23°"
    val iconRes = weather?.let {
        WeatherFormatter.conditionIconRes(it.weatherCode, it.isDay)
    } ?: R.drawable.ic_weather_partly_cloudy_day

    Box(
        modifier = modifier.size(196.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = palette.diagonalPillDrawable),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // Upper-right of tilted Google Pill
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 26.dp, end = 30.dp)
                .clip(RoundedCornerShape(12.dp))
                .then(
                    if (onCityClick != null) Modifier.clickable { onCityClick() } else Modifier
                )
                .padding(4.dp)
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = cityName,
                modifier = Modifier.size(40.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isGpsLocation) Icons.Filled.NearMe else Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = cityName,
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Lower-left of tilted Google Pill
        Text(
            text = tempStr,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 44.sp,
            color = colors.textPrimary,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 34.dp, bottom = 28.dp)
        )
    }
}

@Composable
fun GoogleRoundedSquarePreview(
    cityName: String,
    isGpsLocation: Boolean,
    weather: WeatherSnapshot?,
    palette: WidgetPalette,
    useFahrenheit: Boolean,
    onCityClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = resolveComposeWidgetColors(palette)
    val tempStr = weather?.let {
        WeatherFormatter.formatTemp(it.temperatureC, useFahrenheit)
    } ?: "23°"
    val conditionStr = weather?.let {
        WeatherFormatter.conditionLabel(it.weatherCode, it.isDay)
    } ?: "晴间多云"
    val highLowStr = weather?.let {
        "${WeatherFormatter.formatTemp(it.maxTempC, useFahrenheit)} / ${
            WeatherFormatter.formatTemp(it.minTempC, useFahrenheit)
        }"
    } ?: "26° / 15°"
    val iconRes = weather?.let {
        WeatherFormatter.conditionIconRes(it.weatherCode, it.isDay)
    } ?: R.drawable.ic_weather_partly_cloudy_day

    Column(
        modifier = modifier
            .size(190.dp)
            .shadow(8.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(colors.bg)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.inner)
                    .then(
                        if (onCityClick != null) Modifier.clickable { onCityClick() } else Modifier
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isGpsLocation) Icons.Filled.NearMe else Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = cityName,
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tempStr,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = conditionStr,
                modifier = Modifier.size(46.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = conditionStr,
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = highLowStr,
                color = colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun GoogleForecastCardPreview(
    cityName: String,
    isGpsLocation: Boolean,
    weather: WeatherSnapshot?,
    palette: WidgetPalette,
    useFahrenheit: Boolean,
    showDailyRows: Boolean = false,
    onCityClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = resolveComposeWidgetColors(palette)
    val tempStr = weather?.let {
        WeatherFormatter.formatTemp(it.temperatureC, useFahrenheit)
    } ?: "23°"
    val conditionStr = weather?.let {
        WeatherFormatter.conditionLabel(it.weatherCode, it.isDay)
    } ?: "晴间多云"
    val highLowStr = weather?.let {
        "最高 ${WeatherFormatter.formatTemp(it.maxTempC, useFahrenheit)} · 最低 ${
            WeatherFormatter.formatTemp(it.minTempC, useFahrenheit)
        }"
    } ?: "最高 26° · 最低 15°"
    val iconRes = weather?.let {
        WeatherFormatter.conditionIconRes(it.weatherCode, it.isDay)
    } ?: R.drawable.ic_weather_partly_cloudy_day

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(colors.bg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.inner)
                    .then(
                        if (onCityClick != null) Modifier.clickable { onCityClick() } else Modifier
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = if (isGpsLocation) Icons.Filled.NearMe else Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = cityName,
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isGpsLocation) "实时定位" else "点击换城",
                    color = colors.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tempStr,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 42.sp,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = conditionStr,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = conditionStr,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = highLowStr,
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val hourlyList = weather?.hourly?.take(5).orEmpty()
        if (hourlyList.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.inner)
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
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
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
                        .padding(vertical = 4.dp, horizontal = 4.dp),
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
