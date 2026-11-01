package com.example.widget

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.location.OptionalLocationHelper
import com.example.data.model.CitySearchResult
import com.example.data.model.WeatherFormatter
import com.example.data.model.WidgetPalette
import com.example.data.model.WidgetStyle
import com.example.data.repository.WeatherRepository
import com.example.ui.components.CitySearchBarInput
import com.example.ui.components.MatchingLocationResultCard
import com.example.ui.components.PixelWallpaperWidgetStage
import com.example.ui.components.resolveComposeWidgetColors
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.SkyPillWeatherTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val focusSearchInitially = intent?.extras?.getBoolean(EXTRA_FOCUS_SEARCH, false) ?: false

        val canceledIntent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_CANCELED, canceledIntent)

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val repository = WeatherRepository.getInstance(applicationContext)
        val defaultStyle = if (WeatherWidgetUpdater.isForecastWidgetId(this, appWidgetId)) {
            WidgetStyle.FORECAST_CARD
        } else {
            WidgetStyle.DIAGONAL_PILL
        }

        setContent {
            SkyPillWeatherTheme {
                WidgetConfigureScreen(
                    appWidgetId = appWidgetId,
                    defaultStyle = defaultStyle,
                    autoFocusSearch = focusSearchInitially,
                    repository = repository,
                    onCancel = { finish() },
                    onSaveComplete = {
                        val resultValue = Intent().putExtra(
                            AppWidgetManager.EXTRA_APPWIDGET_ID,
                            appWidgetId
                        )
                        setResult(Activity.RESULT_OK, resultValue)
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_FOCUS_SEARCH = "extra_focus_city_search"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun WidgetConfigureScreen(
    appWidgetId: Int,
    defaultStyle: WidgetStyle,
    autoFocusSearch: Boolean,
    repository: WeatherRepository,
    onCancel: () -> Unit,
    onSaveComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val savedCities by repository.savedCitiesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val globalFahrenheit by repository.useFahrenheitFlow.collectAsStateWithLifecycle(initialValue = false)

    var selectedCityId by remember { mutableStateOf(WeatherRepository.FOLLOW_PRIMARY_CITY_ID) }
    var selectedStyle by remember { mutableStateOf(defaultStyle) }
    var selectedPalette by remember { mutableStateOf(WidgetPalette.DYNAMIC_SKY) }
    var useFahrenheit by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<CitySearchResult>>(emptyList()) }
    var searchError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(appWidgetId) {
        repository.ensureSeededAndFresh()
        val existing = repository.getWidgetConfigOnce(appWidgetId)
        if (existing != null) {
            selectedCityId = existing.cityId
            selectedStyle = WidgetStyle.fromKey(existing.widgetStyle)
            selectedPalette = WidgetPalette.fromKey(existing.colorPalette)
            useFahrenheit = existing.useFahrenheit
        } else {
            useFahrenheit = globalFahrenheit
        }
    }

    suspend fun runSearch(query: String) {
        if (query.isBlank()) {
            searchResults = emptyList()
            searchError = null
            isSearching = false
            return
        }
        isSearching = true
        searchError = null
        repository.searchGlobalCities(query)
            .onSuccess { list ->
                searchResults = list
                searchError = if (list.isEmpty()) "未找到匹配「$query」的地点" else null
            }
            .onFailure {
                searchResults = emptyList()
                searchError = "搜索网络异常，可从下方常用或热门城市选择"
            }
        isSearching = false
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            searchError = null
            isSearching = false
            return@LaunchedEffect
        }
        delay(250L)
        runSearch(searchQuery)
    }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                repository.syncOptionalGpsCity(makePrimary = false)
                    .onSuccess { gpsEntity ->
                        selectedCityId = gpsEntity.id
                        snackbarHostState.showSnackbar("已绑定实时定位：${gpsEntity.name}")
                    }
                    .onFailure {
                        snackbarHostState.showSnackbar("暂无法获取实时位置，请搜索或选择常用城市")
                    }
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("未授予位置权限，请直接搜索或选择常用城市")
            }
        }
    }

    val primaryCity = savedCities.firstOrNull { it.isSelectedPrimary } ?: savedCities.firstOrNull()
    val previewCity = when (selectedCityId) {
        WeatherRepository.FOLLOW_PRIMARY_CITY_ID -> primaryCity
        else -> savedCities.firstOrNull { it.id == selectedCityId } ?: primaryCity
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.widget_configure_header),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.widget_cancel)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            if (isSaving) return@Button
                            isSaving = true
                            scope.launch {
                                repository.saveWidgetConfig(
                                    appWidgetId = appWidgetId,
                                    cityId = selectedCityId,
                                    style = selectedStyle,
                                    palette = selectedPalette,
                                    useFahrenheit = useFahrenheit
                                )
                                WeatherWidgetUpdater.updateAllWidgets(context)
                                onSaveComplete()
                            }
                        },
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                            .heightIn(min = 52.dp)
                            .testTag("widget_config_save_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.widget_save_apply),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 700.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Live Preview of Selected City's Weather on the Widget
                item {
                    PixelWallpaperWidgetStage(
                        cityName = previewCity?.name ?: "北京",
                        isGpsLocation = previewCity?.isCurrentLocation == true,
                        weather = previewCity?.weather,
                        style = selectedStyle,
                        palette = selectedPalette,
                        useFahrenheit = useFahrenheit
                    )
                }

                // 1. City Search & Quick-Access Stored Locations Card
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
                                text = "1. 搜索城市或从常用地点快速选择",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            CitySearchBarInput(
                                query = searchQuery,
                                isSearching = isSearching,
                                placeholderText = stringResource(R.string.cities_search_placeholder),
                                onQueryChange = { searchQuery = it },
                                onClear = {
                                    searchQuery = ""
                                    searchResults = emptyList()
                                    searchError = null
                                },
                                onSubmitSearch = {
                                    scope.launch { runSearch(searchQuery) }
                                },
                                autoFocus = autoFocusSearch,
                                testTagName = "widget_config_city_search"
                            )

                            if (searchError != null) {
                                Text(
                                    text = searchError.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (searchResults.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = stringResource(R.string.cities_matching_results_title),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    searchResults.take(8).forEach { item ->
                                        MatchingLocationResultCard(
                                            result = item,
                                            existingSavedCity = savedCities.firstOrNull { it.id == item.id },
                                            useFahrenheit = useFahrenheit,
                                            onSelect = {
                                                scope.launch {
                                                    val entity = repository.addOrSelectCity(
                                                        cityResult = item,
                                                        makePrimary = false
                                                    )
                                                    selectedCityId = entity.id
                                                    searchQuery = ""
                                                    searchResults = emptyList()
                                                    snackbarHostState.showSnackbar("已保存「${entity.name}」并同步实时天气")
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            Text(
                                text = stringResource(R.string.cities_saved_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedCityId == WeatherRepository.FOLLOW_PRIMARY_CITY_ID,
                                    onClick = { selectedCityId = WeatherRepository.FOLLOW_PRIMARY_CITY_ID },
                                    label = { Text("跟随主城市 (${primaryCity?.name ?: "北京"})") },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                                savedCities.forEach { city ->
                                    FilterChip(
                                        selected = selectedCityId == city.id,
                                        onClick = { selectedCityId = city.id },
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
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = WeatherFormatter.formatTemp(
                                                            w.temperatureC,
                                                            useFahrenheit
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
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    )
                                }
                            }

                            if (searchQuery.isBlank()) {
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
                                            onClick = {
                                                scope.launch {
                                                    val entity = repository.addOrSelectCity(
                                                        cityResult = preset,
                                                        makePrimary = false
                                                    )
                                                    selectedCityId = entity.id
                                                }
                                            },
                                            label = { Text(preset.name) },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        )
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    if (OptionalLocationHelper.hasCoarseLocationPermission(context)) {
                                        scope.launch {
                                            repository.syncOptionalGpsCity(makePrimary = false)
                                                .onSuccess { gpsEntity -> selectedCityId = gpsEntity.id }
                                                .onFailure {
                                                    snackbarHostState.showSnackbar("暂无法获取实时位置")
                                                }
                                        }
                                    } else {
                                        gpsPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Filled.NearMe, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("可选：使用当前实时定位")
                            }
                        }
                    }
                }

                // 2. Style & Palette Card
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
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "2. 选择微件版式与配色",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                WidgetStyle.entries.forEach { style ->
                                    val selected = selectedStyle == style
                                    Surface(
                                        onClick = { selectedStyle = style },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                                        ) {
                                            Text(
                                                text = style.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.onPrimary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                WidgetPalette.entries.forEach { palette ->
                                    val selected = selectedPalette == palette
                                    val swatch = resolveComposeWidgetColors(palette)
                                    Surface(
                                        onClick = { selectedPalette = palette },
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(
                                                width = if (selected) 2.5.dp else 1.dp,
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.outlineVariant
                                                },
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(swatch.bg)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = palette.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "使用华氏度 (°F)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = useFahrenheit,
                                    onCheckedChange = { useFahrenheit = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
