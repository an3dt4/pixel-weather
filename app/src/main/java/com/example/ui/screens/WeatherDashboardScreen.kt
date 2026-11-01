package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CitySearchResult
import com.example.data.model.DailyForecastItem
import com.example.data.model.HourlyForecastItem
import com.example.data.model.SavedCityUiModel
import com.example.data.model.WeatherFormatter
import com.example.data.model.WeatherSnapshot
import com.example.ui.WeatherUiState
import com.example.ui.components.CitySearchBottomSheet
import com.example.ui.components.QuickAccessLocationsStrip
import com.example.ui.theme.OutfitFontFamily
import kotlin.math.roundToInt

@Composable
fun WeatherDashboardScreen(
    uiState: WeatherUiState,
    onSelectCity: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSubmitSearch: () -> Unit,
    onAddSearchedCity: (CitySearchResult) -> Unit,
    onToggleFavoriteCity: (String) -> Unit,
    onDeleteSavedCity: (String) -> Unit,
    onToggleUnit: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToWidgets: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSearchSheet by rememberSaveable { mutableStateOf(false) }
    var autoFocusSheetInput by rememberSaveable { mutableStateOf(false) }

    val primaryCity = uiState.primaryCity
    val snapshot = primaryCity?.weather

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Top Google Weather Action Header
            item {
                WeatherTopHeader(
                    primaryCity = primaryCity,
                    isRefreshing = uiState.isRefreshing,
                    useFahrenheit = uiState.useFahrenheit,
                    onOpenCitySheet = {
                        autoFocusSheetInput = false
                        showSearchSheet = true
                    },
                    onOpenDirectSearch = {
                        autoFocusSheetInput = true
                        showSearchSheet = true
                    },
                    onToggleUnit = onToggleUnit,
                    onRefresh = onRefresh
                )
            }

            // 2. Quick Access Stored Locations Strip (1-tap access to saved cities + Search)
            item {
                QuickAccessLocationsStrip(
                    cities = uiState.cities,
                    activeCityId = primaryCity?.id,
                    useFahrenheit = uiState.useFahrenheit,
                    onSelectCity = onSelectCity,
                    onOpenCitySearch = {
                        autoFocusSheetInput = true
                        showSearchSheet = true
                    }
                )
            }

            // 3. Hero Weather Card (Pixel Weather Atmospheric Card)
            item {
                HeroWeatherBannerCard(
                    city = primaryCity,
                    snapshot = snapshot,
                    useFahrenheit = uiState.useFahrenheit,
                    isLoading = uiState.isRefreshing && snapshot == null
                )
            }

            // 4. Quick Google Widget Studio Banner CTA
            item {
                WidgetQuickShowcaseCard(
                    cityName = primaryCity?.name ?: "北京",
                    isGps = primaryCity?.isCurrentLocation == true,
                    snapshot = snapshot,
                    useFahrenheit = uiState.useFahrenheit,
                    onClick = onNavigateToWidgets
                )
            }

            // 5. 24-Hour Hourly Forecast Card
            if (snapshot != null && snapshot.hourly.isNotEmpty()) {
                item {
                    HourlyTimelineCard(
                        hourly = snapshot.hourly,
                        useFahrenheit = uiState.useFahrenheit
                    )
                }
            }

            // 6. 7-Day Forecast Trend Card with Proportional Range Bars
            if (snapshot != null && snapshot.daily.isNotEmpty()) {
                item {
                    SevenDayForecastCard(
                        daily = snapshot.daily,
                        useFahrenheit = uiState.useFahrenheit
                    )
                }
            }

            // 7. Google Material You 2x3 Weather Metrics Bento Grid
            if (snapshot != null) {
                item {
                    WeatherMetricsBentoSection(
                        snapshot = snapshot,
                        useFahrenheit = uiState.useFahrenheit
                    )
                }
            }
        }
    }

    if (showSearchSheet) {
        CitySearchBottomSheet(
            searchState = uiState.searchState,
            savedCities = uiState.cities,
            activeCityId = primaryCity?.id,
            useFahrenheit = uiState.useFahrenheit,
            autoFocusSearch = autoFocusSheetInput,
            onDismiss = {
                showSearchSheet = false
                onClearSearch()
            },
            onQueryChange = onSearchQueryChange,
            onClearSearch = onClearSearch,
            onSubmitSearch = onSubmitSearch,
            onSelectSearchResult = { result ->
                onAddSearchedCity(result)
                showSearchSheet = false
            },
            onSelectSavedCity = { cityId ->
                onSelectCity(cityId)
                showSearchSheet = false
            },
            onToggleFavorite = onToggleFavoriteCity,
            onDeleteSavedCity = onDeleteSavedCity
        )
    }
}

@Composable
private fun WeatherTopHeader(
    primaryCity: SavedCityUiModel?,
    isRefreshing: Boolean,
    useFahrenheit: Boolean,
    onOpenCitySheet: () -> Unit,
    onOpenDirectSearch: () -> Unit,
    onToggleUnit: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onOpenCitySheet,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .weight(1f, fill = false)
                .testTag("city_switcher_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (primaryCity?.isCurrentLocation == true) {
                        Icons.Filled.NearMe
                    } else {
                        Icons.Filled.LocationOn
                    },
                    contentDescription = stringResource(R.string.weather_switch_city_cd),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = primaryCity?.name ?: "选择城市",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (primaryCity?.isCurrentLocation == true) {
                            stringResource(R.string.weather_gps_badge)
                        } else {
                            stringResource(R.string.weather_manual_badge)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        FilledTonalIconButton(
            onClick = onOpenDirectSearch,
            modifier = Modifier
                .size(48.dp)
                .testTag("dashboard_search_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.weather_search_city_cd)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Surface(
            onClick = onToggleUnit,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("unit_toggle_button")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = if (useFahrenheit) "°F" else "°C",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        FilledTonalIconButton(
            onClick = onRefresh,
            modifier = Modifier
                .size(48.dp)
                .testTag("refresh_weather_button")
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    strokeWidth = 2.2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.weather_refresh_cd)
                )
            }
        }
    }
}

@Composable
private fun HeroWeatherBannerCard(
    city: SavedCityUiModel?,
    snapshot: WeatherSnapshot?,
    useFahrenheit: Boolean,
    isLoading: Boolean
) {
    Card(
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_weather_hero_banner_1793542593812),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xCC0D2340),
                                Color(0xB3123158),
                                Color(0xE60B1D36)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else {
                    val tempStr = snapshot?.let {
                        WeatherFormatter.formatTemp(it.temperatureC, useFahrenheit)
                    } ?: "--°"
                    val conditionStr = snapshot?.let {
                        WeatherFormatter.conditionLabel(it.weatherCode, it.isDay)
                    } ?: "正在同步气象数据"
                    val feelsLikeStr = snapshot?.let {
                        stringResource(
                            R.string.weather_feels_like,
                            WeatherFormatter.formatTemp(it.feelsLikeC, useFahrenheit)
                        )
                    } ?: ""
                    val highLowStr = snapshot?.let {
                        stringResource(
                            R.string.weather_high_low,
                            WeatherFormatter.formatTemp(it.maxTempC, useFahrenheit),
                            WeatherFormatter.formatTemp(it.minTempC, useFahrenheit)
                        )
                    } ?: ""
                    val iconRes = snapshot?.let {
                        WeatherFormatter.conditionIconRes(it.weatherCode, it.isDay)
                    } ?: R.drawable.ic_weather_partly_cloudy_day

                    Text(
                        text = city?.adminArea ?: "全球城市气象",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.80f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tempStr,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 82.sp,
                            lineHeight = 86.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Image(
                                painter = painterResource(id = iconRes),
                                contentDescription = conditionStr,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = conditionStr,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = highLowStr,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                        if (feelsLikeStr.isNotEmpty()) {
                            Text(
                                text = "  ·  $feelsLikeStr",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White.copy(alpha = 0.82f)
                            )
                        }
                    }

                    if (snapshot != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White.copy(alpha = 0.16f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = WeatherFormatter.buildSmartInsight(snapshot, useFahrenheit),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetQuickShowcaseCard(
    cityName: String,
    isGps: Boolean,
    snapshot: WeatherSnapshot?,
    useFahrenheit: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("open_widget_studio_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Material You 桌面天气微件",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                val tempHint = snapshot?.let {
                    WeatherFormatter.formatTemp(it.temperatureC, useFahrenheit)
                } ?: "--°"
                Text(
                    text = "当前城市：$cityName ($tempHint · ${if (isGps) "实时定位" else "自选已存"}) · 点击定制微件并绑定城市",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun HourlyTimelineCard(
    hourly: List<HourlyForecastItem>,
    useFahrenheit: Boolean
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 18.dp)) {
            Text(
                text = stringResource(R.string.weather_hourly_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(hourly) { index, item ->
                    val isNow = index == 0
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(68.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                if (isNow) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                            )
                            .padding(vertical = 12.dp, horizontal = 6.dp)
                    ) {
                        Text(
                            text = WeatherFormatter.formatTemp(item.temperatureC, useFahrenheit),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (isNow) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Image(
                            painter = painterResource(
                                id = WeatherFormatter.conditionIconRes(item.weatherCode, item.isDay)
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (item.precipProbability >= 15) {
                            Text(
                                text = "${item.precipProbability}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.timeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SevenDayForecastCard(
    daily: List<DailyForecastItem>,
    useFahrenheit: Boolean
) {
    val weekMin = daily.minOfOrNull { it.minTempC } ?: 0.0
    val weekMax = (daily.maxOfOrNull { it.maxTempC } ?: 30.0).coerceAtLeast(weekMin + 1.0)

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.weather_daily_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            daily.forEach { day ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = day.dayLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(52.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.width(68.dp)
                    ) {
                        Image(
                            painter = painterResource(
                                id = WeatherFormatter.conditionIconRes(day.weatherCode, true)
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                        if (day.precipProbabilityMax >= 20) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${day.precipProbabilityMax}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = WeatherFormatter.formatTemp(day.minTempC, useFahrenheit),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(38.dp)
                    )

                    val startFraction = ((day.minTempC - weekMin) / (weekMax - weekMin))
                        .toFloat()
                        .coerceIn(0f, 1f)
                    val endFraction = ((day.maxTempC - weekMin) / (weekMax - weekMin))
                        .toFloat()
                        .coerceIn(startFraction + 0.08f, 1f)
                    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    val barStartColor = MaterialTheme.colorScheme.primary
                    val barEndColor = Color(0xFFF59E0B)

                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .padding(horizontal = 6.dp)
                    ) {
                        drawRoundRect(
                            color = trackColor,
                            size = size,
                            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                        )
                        val barLeft = size.width * startFraction
                        val barWidth = (size.width * (endFraction - startFraction))
                            .coerceAtLeast(size.height)
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(barStartColor, barEndColor),
                                startX = barLeft,
                                endX = barLeft + barWidth
                            ),
                            topLeft = Offset(barLeft, 0f),
                            size = Size(barWidth.coerceAtMost(size.width - barLeft), size.height),
                            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                        )
                    }

                    Text(
                        text = WeatherFormatter.formatTemp(day.maxTempC, useFahrenheit),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(38.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherMetricsBentoSection(
    snapshot: WeatherSnapshot,
    useFahrenheit: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.weather_metrics_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricPillTile(
                icon = Icons.Filled.WbSunny,
                title = stringResource(R.string.metric_uv_title),
                value = "${snapshot.uvIndex.roundToInt()}",
                subtitle = WeatherFormatter.uvLevelLabel(snapshot.uvIndex),
                modifier = Modifier.weight(1f)
            )
            MetricPillTile(
                icon = Icons.Filled.WaterDrop,
                title = stringResource(R.string.metric_humidity_title),
                value = "${snapshot.humidityPercent}%",
                subtitle = "露点 ${WeatherFormatter.formatTemp(snapshot.dewPointC, useFahrenheit)}",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricPillTile(
                icon = Icons.Filled.Air,
                title = stringResource(R.string.metric_wind_title),
                value = "${snapshot.windSpeedKmh.roundToInt()} km/h",
                subtitle = WeatherFormatter.windDirectionLabel(snapshot.windDirectionDeg),
                modifier = Modifier.weight(1f)
            )
            MetricPillTile(
                icon = Icons.Filled.Compress,
                title = stringResource(R.string.metric_pressure_title),
                value = "${snapshot.pressureHpa.roundToInt()} hPa",
                subtitle = if (snapshot.pressureHpa >= 1013.0) "气压平稳偏高" else "标准大气压附近",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricPillTile(
                icon = Icons.Filled.Visibility,
                title = stringResource(R.string.metric_visibility_title),
                value = "${"%.1f".format(snapshot.visibilityKm)} km",
                subtitle = if (snapshot.visibilityKm >= 10.0) "视野通透极佳" else "请留意道路能见度",
                modifier = Modifier.weight(1f)
            )
            MetricPillTile(
                icon = Icons.Filled.WbTwilight,
                title = stringResource(R.string.metric_sun_title),
                value = snapshot.sunriseTime,
                subtitle = "日落 ${snapshot.sunsetTime}",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricPillTile(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
