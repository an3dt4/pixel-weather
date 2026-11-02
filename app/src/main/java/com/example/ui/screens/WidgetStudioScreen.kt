package com.example.ui.screens

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.location.OptionalLocationHelper
import com.example.data.model.ActiveWidgetInstanceUi
import com.example.data.model.CitySearchResult
import com.example.data.model.WeatherFormatter
import com.example.data.model.WidgetPalette
import com.example.data.model.WidgetStyle
import com.example.data.repository.WeatherRepository
import com.example.ui.WeatherUiState
import com.example.ui.components.CitySearchBarInput
import com.example.ui.components.MatchingLocationResultCard
import com.example.ui.components.PixelWallpaperWidgetStage
import com.example.ui.components.WidgetOpacityAndDateCard
import com.example.ui.components.WidgetStyleAndPaletteCard
import com.example.ui.theme.OutfitFontFamily
import com.example.widget.WidgetConfigureActivity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WidgetStudioScreen(
    uiState: WeatherUiState,
    onSelectStyle: (WidgetStyle) -> Unit,
    onSelectPalette: (WidgetPalette) -> Unit,
    onChangeOpacity: (Float) -> Unit,
    onSelectOpacityPreset: (Float, Boolean) -> Unit,
    onToggleShowBorder: (Boolean) -> Unit,
    onToggleShowDate: (Boolean) -> Unit,
    onToggleEnglishDate: (Boolean) -> Unit,
    onToggleShowCityOnWidget: (Boolean) -> Unit,
    onSyncToActiveWidgets: () -> Unit,
    onSelectStudioCity: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSubmitSearch: () -> Unit,
    onSelectSearchResultForWidget: (CitySearchResult) -> Unit,
    onToggleTallPreview: () -> Unit,
    onPinToHome: () -> Unit,
    onPinAppIconToHome: () -> Unit = {},
    onRequestOptionalGps: () -> Unit,
    onPermissionDenied: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val draft = uiState.studioDraft
    val searchState = uiState.searchState
    var focusInlineSearch by remember { mutableStateOf(false) }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onRequestOptionalGps()
        } else {
            onPermissionDenied()
        }
    }

    val previewCity = when (draft.selectedCityId) {
        WeatherRepository.FOLLOW_PRIMARY_CITY_ID -> uiState.primaryCity
        else -> uiState.cities.firstOrNull { it.id == draft.selectedCityId } ?: uiState.primaryCity
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.widget_studio_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.widget_studio_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Interactive "Select a widget style" Sky-Wave Stage (Semi-transparent / Transparent / Solid)
            item {
                PixelWallpaperWidgetStage(
                    cityName = previewCity?.name ?: "北京",
                    isGpsLocation = previewCity?.isCurrentLocation == true,
                    weather = previewCity?.weather,
                    style = draft.selectedStyle,
                    palette = draft.selectedPalette,
                    backgroundAlpha = draft.backgroundAlpha,
                    showBorder = draft.showBorder,
                    showDate = draft.showDate,
                    useEnglishDate = draft.useEnglishDate,
                    showCityOnWidget = draft.showCityOnWidget,
                    useFahrenheit = uiState.useFahrenheit,
                    expandedTallForecast = draft.previewTallForecast,
                    onSelectPresetOpacityAndBorder = onSelectOpacityPreset,
                    onCityPillClick = { focusInlineSearch = true }
                )
            }

            // Primary Pin to Home + Sync Existing Widgets + Pin App Shortcut
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onPinToHome,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .testTag("pin_widget_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.AddToHomeScreen,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.widget_pin_to_home),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (uiState.activeWidgets.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = onSyncToActiveWidgets,
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("sync_all_active_widgets_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "将当前样式与透明度同步到桌面已有的 ${uiState.activeWidgets.size} 个微件",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onPinAppIconToHome,
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("pin_app_shortcut_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AppShortcut,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.pin_app_icon_to_home),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 1. Opacity Slider, Outline Border, & Date Controls
            item {
                WidgetOpacityAndDateCard(
                    backgroundAlpha = draft.backgroundAlpha,
                    showBorder = draft.showBorder,
                    showDate = draft.showDate,
                    useEnglishDate = draft.useEnglishDate,
                    showCityOnWidget = draft.showCityOnWidget,
                    onChangeOpacity = onChangeOpacity,
                    onSelectPreset = onSelectOpacityPreset,
                    onToggleShowBorder = onToggleShowBorder,
                    onToggleShowDate = onToggleShowDate,
                    onToggleEnglishDate = onToggleEnglishDate,
                    onToggleShowCityOnWidget = onToggleShowCityOnWidget
                )
            }

            // 2. Custom Location Picker (Search + Saved Quick-Access Cities + Optional GPS)
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.widget_location_section),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        CitySearchBarInput(
                            query = searchState.query,
                            isSearching = searchState.isSearching,
                            placeholderText = stringResource(R.string.cities_search_placeholder),
                            onQueryChange = onSearchQueryChange,
                            onClear = onClearSearch,
                            onSubmitSearch = onSubmitSearch,
                            autoFocus = focusInlineSearch,
                            testTagName = "widget_studio_city_search_input"
                        )

                        if (searchState.errorMessage != null) {
                            Text(
                                text = searchState.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        if (searchState.results.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.cities_matching_results_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                searchState.results.take(8).forEach { result ->
                                    MatchingLocationResultCard(
                                        result = result,
                                        existingSavedCity = uiState.cities.firstOrNull { it.id == result.id },
                                        useFahrenheit = uiState.useFahrenheit,
                                        onSelect = {
                                            focusInlineSearch = false
                                            onSelectSearchResultForWidget(result)
                                        }
                                    )
                                }
                            }
                        }

                        Text(
                            text = stringResource(R.string.cities_saved_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val followSelected = draft.selectedCityId == WeatherRepository.FOLLOW_PRIMARY_CITY_ID
                            FilterChip(
                                selected = followSelected,
                                onClick = { onSelectStudioCity(WeatherRepository.FOLLOW_PRIMARY_CITY_ID) },
                                label = { Text("跟随主城市 (${uiState.primaryCity?.name ?: "北京"})") },
                                leadingIcon = if (followSelected) {
                                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.heightIn(min = 48.dp)
                            )

                            uiState.cities.forEach { city ->
                                val isSelected = draft.selectedCityId == city.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectStudioCity(city.id) },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(city.name, fontWeight = FontWeight.SemiBold)
                                            if (city.isPinnedFavorite && !city.isCurrentLocation) {
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Icon(
                                                    imageVector = Icons.Filled.Star,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                            city.weather?.let { w ->
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = WeatherFormatter.formatTempWithUnit(
                                                        w.temperatureC,
                                                        uiState.useFahrenheit
                                                    ),
                                                    fontFamily = OutfitFontFamily,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    },
                                    leadingIcon = {
                                        if (city.weather != null) {
                                            Image(
                                                painter = painterResource(
                                                    id = WeatherFormatter.conditionIconRes(
                                                        city.weather.weatherCode,
                                                        city.weather.isDay
                                                    )
                                                ),
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (city.isCurrentLocation) {
                                                    Icons.Filled.NearMe
                                                } else {
                                                    Icons.Filled.LocationOn
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("studio_city_chip_${city.id}")
                                )
                            }
                        }

                        if (searchState.query.isBlank()) {
                            Text(
                                text = stringResource(R.string.cities_popular_title),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                WeatherRepository.POPULAR_PRESET_CITIES.take(8).forEach { preset ->
                                    AssistChip(
                                        onClick = { onSelectSearchResultForWidget(preset) },
                                        label = { Text(preset.name) },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (OptionalLocationHelper.hasCoarseLocationPermission(context)) {
                                    onRequestOptionalGps()
                                } else {
                                    gpsPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                }
                            },
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("studio_optional_gps_button")
                        ) {
                            Icon(Icons.Filled.NearMe, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("可选：使用当前实时定位（无需长开 GPS）")
                        }
                    }
                }
            }

            // 3. Layout Size & Palette Card
            item {
                WidgetStyleAndPaletteCard(
                    selectedStyle = draft.selectedStyle,
                    selectedPalette = draft.selectedPalette,
                    previewTallForecast = draft.previewTallForecast,
                    onSelectStyle = onSelectStyle,
                    onSelectPalette = onSelectPalette,
                    onToggleTallPreview = onToggleTallPreview
                )
            }

            // 4. Active Placed Home Screen Widgets List
            if (uiState.activeWidgets.isNotEmpty()) {
                item {
                    ActivePlacedWidgetsSection(
                        instances = uiState.activeWidgets,
                        onReconfigureInstance = { widgetId ->
                            val intent = Intent(context, WidgetConfigureActivity::class.java).apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                                putExtra(WidgetConfigureActivity.EXTRA_FOCUS_SEARCH, false)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivePlacedWidgetsSection(
    instances: List<ActiveWidgetInstanceUi>,
    onReconfigureInstance: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.widget_active_instances_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            instances.forEach { instance ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${instance.resolvedCityName} · ${instance.style.shortTag}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "#${instance.appWidgetId} · ${
                                    WeatherFormatter.formatOpacityShortTag(
                                        instance.backgroundAlpha,
                                        instance.showBorder
                                    )
                                } · ${instance.palette.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { onReconfigureInstance(instance.appWidgetId) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EditLocationAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.widget_reconfigure_btn))
                        }
                    }
                }
            }
        }
    }
}
