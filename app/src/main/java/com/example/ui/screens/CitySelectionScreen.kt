package com.example.ui.screens

import android.Manifest
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.model.CitySearchResult
import com.example.data.model.SavedCityUiModel
import com.example.data.model.WeatherFormatter
import com.example.data.repository.WeatherRepository
import com.example.ui.WeatherUiState
import com.example.ui.components.CitySearchBarInput
import com.example.ui.components.MatchingLocationResultCard
import com.example.ui.theme.OutfitFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CitySelectionScreen(
    uiState: WeatherUiState,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSubmitSearch: () -> Unit,
    onAddCity: (CitySearchResult) -> Unit,
    onSelectPrimaryCity: (String) -> Unit,
    onToggleFavoriteCity: (String) -> Unit,
    onApplyCityToAllWidgets: (String, String) -> Unit,
    onDeleteCity: (String) -> Unit,
    onRequestOptionalGps: () -> Unit,
    onPermissionDenied: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchState = uiState.searchState

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onRequestOptionalGps()
        } else {
            onPermissionDenied()
        }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.cities_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.cities_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Global City Search Input
            item {
                CitySearchBarInput(
                    query = searchState.query,
                    isSearching = searchState.isSearching,
                    placeholderText = stringResource(R.string.cities_search_placeholder),
                    onQueryChange = onSearchQueryChange,
                    onClear = onClearSearch,
                    onSubmitSearch = onSubmitSearch,
                    testTagName = "city_search_input"
                )
            }

            // Search Error / Empty Feedback
            if (searchState.errorMessage != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = searchState.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Live Matching Locations Results
            if (searchState.results.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.cities_matching_results_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(
                    items = searchState.results,
                    key = { "search_${it.id}" }
                ) { result ->
                    MatchingLocationResultCard(
                        result = result,
                        existingSavedCity = uiState.cities.firstOrNull { it.id == result.id },
                        useFahrenheit = uiState.useFahrenheit,
                        onSelect = { onAddCity(result) }
                    )
                }
            }

            // Popular Preset Cities Quick Chips
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.cities_popular_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            WeatherRepository.POPULAR_PRESET_CITIES.forEach { preset ->
                                val alreadySaved = uiState.cities.any { it.id == preset.id }
                                AssistChip(
                                    onClick = { onAddCity(preset) },
                                    label = { Text(preset.name) },
                                    leadingIcon = if (alreadySaved) {
                                        {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("popular_city_${preset.name}")
                                )
                            }
                        }
                    }
                }
            }

            // Strictly Optional Real-Time GPS Card
            item {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NearMe,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.cities_optional_gps_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.cities_optional_gps_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                if (OptionalLocationHelper.hasCoarseLocationPermission(context)) {
                                    onRequestOptionalGps()
                                } else {
                                    gpsPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                }
                            },
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("optional_gps_button")
                        ) {
                            Text(stringResource(R.string.cities_optional_gps_btn))
                        }
                    }
                }
            }

            // Saved Quick-Access Locations List Header
            item {
                Text(
                    text = stringResource(R.string.cities_saved_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(
                items = uiState.cities,
                key = { "saved_${it.id}" }
            ) { city ->
                SavedCityItemCard(
                    city = city,
                    canDelete = uiState.cities.size > 1,
                    useFahrenheit = uiState.useFahrenheit,
                    onSelectPrimary = { onSelectPrimaryCity(city.id) },
                    onToggleFavorite = { onToggleFavoriteCity(city.id) },
                    onApplyAllWidgets = { onApplyCityToAllWidgets(city.id, city.name) },
                    onDelete = { onDeleteCity(city.id) }
                )
            }
        }
    }
}

@Composable
private fun SavedCityItemCard(
    city: SavedCityUiModel,
    canDelete: Boolean,
    useFahrenheit: Boolean,
    onSelectPrimary: () -> Unit,
    onToggleFavorite: () -> Unit,
    onApplyAllWidgets: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onSelectPrimary,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (city.isSelectedPrimary) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_city_card_${city.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (city.isCurrentLocation) {
                                Icons.Filled.NearMe
                            } else {
                                Icons.Filled.LocationOn
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = city.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                        ) {
                            Text(
                                text = if (city.isCurrentLocation) "实时定位" else "常用保存",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    val subtitle = city.weather?.let { w ->
                        "${WeatherFormatter.conditionLabel(w.weatherCode, w.isDay)} · 最高 ${
                            WeatherFormatter.formatTemp(w.maxTempC, useFahrenheit)
                        } / 最低 ${
                            WeatherFormatter.formatTemp(w.minTempC, useFahrenheit)
                        } · ${city.adminArea}"
                    } ?: city.adminArea
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                city.weather?.let { snapshot ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(
                                id = WeatherFormatter.conditionIconRes(snapshot.weatherCode, snapshot.isDay)
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = WeatherFormatter.formatTemp(snapshot.temperatureC, useFahrenheit),
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onSelectPrimary,
                    enabled = !city.isSelectedPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    if (city.isSelectedPrimary) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.cities_current_active))
                    } else {
                        Text(stringResource(R.string.cities_set_primary))
                    }
                }

                FilledTonalButton(
                    onClick = onApplyAllWidgets,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Filled.Widgets, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.cities_apply_all_widgets))
                }

                Spacer(modifier = Modifier.weight(1f))

                if (!city.isCurrentLocation) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("toggle_favorite_${city.id}")
                    ) {
                        Icon(
                            imageVector = if (city.isPinnedFavorite) {
                                Icons.Filled.Star
                            } else {
                                Icons.Outlined.StarBorder
                            },
                            contentDescription = stringResource(R.string.cities_pin_favorite_cd),
                            tint = if (city.isPinnedFavorite) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "删除城市",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
