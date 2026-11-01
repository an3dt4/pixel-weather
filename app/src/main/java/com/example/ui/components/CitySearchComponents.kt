package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CitySearchResult
import com.example.data.model.SavedCityUiModel
import com.example.data.model.WeatherFormatter
import com.example.data.repository.WeatherRepository
import com.example.ui.SearchUiState
import com.example.ui.theme.OutfitFontFamily

@Composable
fun CitySearchBarInput(
    query: String,
    isSearching: Boolean,
    placeholderText: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSubmitSearch: (() -> Unit)? = null,
    autoFocus: Boolean = false,
    testTagName: String = "city_search_input",
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = placeholderText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.weather_search_city_cd),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            if (isSearching) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.testTag("${testTagName}_clear")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "清空搜索输入"
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSubmitSearch?.invoke()
                focusManager.clearFocus()
            }
        ),
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .testTag(testTagName)
    )
}

@Composable
fun MatchingLocationResultCard(
    result: CitySearchResult,
    existingSavedCity: SavedCityUiModel?,
    useFahrenheit: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAlreadySaved = existingSavedCity != null
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(20.dp),
        color = if (isAlreadySaved) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .testTag("search_result_${result.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (result.countryCode.isNotBlank()) {
                        Text(
                            text = result.countryCode.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = result.displaySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = result.coordinatesLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (existingSavedCity?.weather != null) {
                Text(
                    text = WeatherFormatter.formatTemp(
                        existingSavedCity.weather.temperatureC,
                        useFahrenheit
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Surface(
                shape = CircleShape,
                color = if (isAlreadySaved) {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                } else {
                    MaterialTheme.colorScheme.primary
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isAlreadySaved) Icons.Filled.Check else Icons.Filled.Add,
                        contentDescription = null,
                        tint = if (isAlreadySaved) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        },
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isAlreadySaved) {
                            stringResource(R.string.cities_saved_badge)
                        } else {
                            stringResource(R.string.cities_select_and_save)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isAlreadySaved) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        }
                    )
                }
            }
        }
    }
}

/**
 * Horizontal quick-access pill strip displaying the user's stored locations for 1-tap switching.
 */
@Composable
fun QuickAccessLocationsStrip(
    cities: List<SavedCityUiModel>,
    activeCityId: String?,
    useFahrenheit: Boolean,
    onSelectCity: (String) -> Unit,
    onOpenCitySearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quick_access_locations_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(
            items = cities,
            key = { "quick_chip_${it.id}" }
        ) { city ->
            val selected = city.id == activeCityId
            Surface(
                onClick = { onSelectCity(city.id) },
                shape = CircleShape,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .then(
                        if (!selected) {
                            Modifier.border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                        } else {
                            Modifier
                        }
                    )
                    .testTag("quick_access_chip_${city.id}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (city.weather != null) {
                        Image(
                            painter = painterResource(
                                id = WeatherFormatter.conditionIconRes(
                                    city.weather.weatherCode,
                                    city.weather.isDay
                                )
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Icon(
                            imageVector = if (city.isCurrentLocation) {
                                Icons.Filled.NearMe
                            } else {
                                Icons.Filled.LocationOn
                            },
                            contentDescription = null,
                            tint = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = city.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    if (city.isPinnedFavorite && !city.isCurrentLocation) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (selected) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.tertiary
                            },
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    city.weather?.let { snapshot ->
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = WeatherFormatter.formatTemp(snapshot.temperatureC, useFahrenheit),
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }

        item(key = "quick_chip_search_add") {
            Surface(
                onClick = onOpenCitySearch,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("quick_access_add_city_chip")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.weather_add_city_chip),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CitySearchBottomSheet(
    searchState: SearchUiState,
    savedCities: List<SavedCityUiModel>,
    activeCityId: String?,
    useFahrenheit: Boolean,
    autoFocusSearch: Boolean,
    onDismiss: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSubmitSearch: () -> Unit,
    onSelectSearchResult: (CitySearchResult) -> Unit,
    onSelectSavedCity: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeleteSavedCity: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.cities_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                CitySearchBarInput(
                    query = searchState.query,
                    isSearching = searchState.isSearching,
                    placeholderText = stringResource(R.string.cities_search_placeholder),
                    onQueryChange = onQueryChange,
                    onClear = onClearSearch,
                    onSubmitSearch = onSubmitSearch,
                    autoFocus = autoFocusSearch,
                    testTagName = "modal_city_search_input"
                )
            }

            if (searchState.errorMessage != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = searchState.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (searchState.results.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.cities_matching_results_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(
                    items = searchState.results,
                    key = { "sheet_res_${it.id}" }
                ) { result ->
                    MatchingLocationResultCard(
                        result = result,
                        existingSavedCity = savedCities.firstOrNull { it.id == result.id },
                        useFahrenheit = useFahrenheit,
                        onSelect = { onSelectSearchResult(result) }
                    )
                }
            } else if (searchState.query.isBlank()) {
                // Quick-Access Stored Locations Section
                item {
                    Text(
                        text = stringResource(R.string.cities_saved_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(
                    items = savedCities,
                    key = { "sheet_saved_${it.id}" }
                ) { city ->
                    val selected = city.id == activeCityId
                    Surface(
                        onClick = { onSelectSavedCity(city.id) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onToggleFavorite(city.id) },
                                modifier = Modifier.size(40.dp)
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

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = city.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (city.isCurrentLocation) {
                                        "实时定位 · ${city.adminArea}"
                                    } else {
                                        city.adminArea
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            city.weather?.let { w ->
                                Image(
                                    painter = painterResource(
                                        id = WeatherFormatter.conditionIconRes(w.weatherCode, w.isDay)
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = WeatherFormatter.formatTemp(w.temperatureC, useFahrenheit),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (savedCities.size > 1) {
                                IconButton(
                                    onClick = { onDeleteSavedCity(city.id) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "删除已保存城市",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Popular Cities Quick Chips
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.cities_popular_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            WeatherRepository.POPULAR_PRESET_CITIES.take(10).forEach { preset ->
                                val isSaved = savedCities.any { it.id == preset.id }
                                AssistChip(
                                    onClick = { onSelectSearchResult(preset) },
                                    label = { Text(preset.name) },
                                    leadingIcon = if (isSaved) {
                                        {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    } else null,
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
