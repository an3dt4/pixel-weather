package com.example

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.WeatherViewModel
import com.example.ui.screens.CitySelectionScreen
import com.example.ui.screens.WeatherDashboardScreen
import com.example.ui.screens.WidgetStudioScreen
import com.example.ui.theme.SkyPillWeatherTheme

enum class AppDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    WEATHER(
        route = "weather",
        labelRes = R.string.nav_weather,
        selectedIcon = Icons.Filled.WbSunny,
        unselectedIcon = Icons.Outlined.WbSunny,
        tag = "nav_tab_weather"
    ),
    WIDGET_STUDIO(
        route = "widget_studio",
        labelRes = R.string.nav_widgets,
        selectedIcon = Icons.Filled.Widgets,
        unselectedIcon = Icons.Outlined.Widgets,
        tag = "nav_tab_widgets"
    ),
    CITIES(
        route = "cities",
        labelRes = R.string.nav_cities,
        selectedIcon = Icons.Filled.LocationCity,
        unselectedIcon = Icons.Outlined.LocationCity,
        tag = "nav_tab_cities"
    )
}

class MainActivity : ComponentActivity() {

    private val viewModel: WeatherViewModel by viewModels()
    private val requestedTabState = mutableStateOf<AppDestination?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingWidgetIntent(intent)

        setContent {
            SkyPillWeatherTheme {
                SkyPillAppRoot(
                    viewModel = viewModel,
                    externalTabRequest = requestedTabState.value,
                    onConsumeExternalTab = { requestedTabState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingWidgetIntent(intent)
    }

    private fun handleIncomingWidgetIntent(intent: Intent?) {
        if (intent == null) return
        val openTab = intent.getStringExtra("extra_open_tab")
        if (openTab == "cities") {
            requestedTabState.value = AppDestination.CITIES
        } else if (openTab == "widgets") {
            requestedTabState.value = AppDestination.WIDGET_STUDIO
        }

        val selectedCityExtra = intent.getStringExtra("extra_selected_city_id")
        if (!selectedCityExtra.isNullOrBlank()) {
            viewModel.selectPrimaryCity(selectedCityExtra)
        }

        val pinnedWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val pinnedStyle = intent.getStringExtra("pinned_style")
        if (pinnedWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && pinnedStyle != null) {
            viewModel.handlePinnedWidgetCallback(
                appWidgetId = pinnedWidgetId,
                styleKey = pinnedStyle,
                paletteKey = intent.getStringExtra("pinned_palette"),
                cityId = intent.getStringExtra("pinned_city_id"),
                useFahrenheit = intent.getBooleanExtra("pinned_fahrenheit", false)
            )
        }
    }
}

@Composable
fun SkyPillAppRoot(
    viewModel: WeatherViewModel,
    externalTabRequest: AppDestination? = null,
    onConsumeExternalTab: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by rememberSaveable { mutableStateOf(AppDestination.WEATHER) }
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(externalTabRequest) {
        if (externalTabRequest != null) {
            currentTab = externalTabRequest
            onConsumeExternalTab()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshActiveWidgetInstances()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeSnackbar()
        }
    }

    BackHandler(enabled = currentTab != AppDestination.WEATHER) {
        currentTab = AppDestination.WEATHER
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar {
                        AppDestination.entries.forEach { dest ->
                            val selected = currentTab == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = dest },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        AppDestination.entries.forEach { dest ->
                            val selected = currentTab == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = dest },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.tag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        AppDestination.WEATHER -> WeatherDashboardScreen(
                            uiState = uiState,
                            onSelectCity = viewModel::selectPrimaryCity,
                            onSearchQueryChange = viewModel::onSearchQueryChange,
                            onClearSearch = viewModel::clearSearch,
                            onSubmitSearch = viewModel::submitImmediateSearch,
                            onAddSearchedCity = { city ->
                                viewModel.addCityAndSelect(city, alsoApplyStudioDraft = false)
                            },
                            onToggleFavoriteCity = viewModel::toggleFavoriteCity,
                            onDeleteSavedCity = viewModel::deleteSavedCity,
                            onToggleUnit = viewModel::toggleTemperatureUnit,
                            onRefresh = viewModel::refreshCurrentWeather,
                            onNavigateToWidgets = { currentTab = AppDestination.WIDGET_STUDIO }
                        )

                        AppDestination.WIDGET_STUDIO -> WidgetStudioScreen(
                            uiState = uiState,
                            onSelectStyle = viewModel::updateStudioStyle,
                            onSelectPalette = viewModel::updateStudioPalette,
                            onSelectStudioCity = viewModel::updateStudioCity,
                            onSearchQueryChange = viewModel::onSearchQueryChange,
                            onClearSearch = viewModel::clearSearch,
                            onSubmitSearch = viewModel::submitImmediateSearch,
                            onSelectSearchResultForWidget = { city ->
                                viewModel.addCityAndSelect(city, alsoApplyStudioDraft = true)
                            },
                            onToggleTallPreview = viewModel::toggleStudioTallForecastPreview,
                            onPinToHome = viewModel::requestPinCurrentStudioWidget,
                            onPinAppIconToHome = viewModel::requestPinAppShortcutToHomeScreen,
                            onRequestOptionalGps = {
                                viewModel.syncOptionalCurrentLocation(onSuccessSelectForStudio = true)
                            },
                            onPermissionDenied = viewModel::onLocationPermissionDenied
                        )

                        AppDestination.CITIES -> CitySelectionScreen(
                            uiState = uiState,
                            onSearchQueryChange = viewModel::onSearchQueryChange,
                            onClearSearch = viewModel::clearSearch,
                            onSubmitSearch = viewModel::submitImmediateSearch,
                            onAddCity = { city ->
                                viewModel.addCityAndSelect(city, alsoApplyStudioDraft = true)
                                currentTab = AppDestination.WEATHER
                            },
                            onSelectPrimaryCity = { id ->
                                viewModel.selectPrimaryCity(id)
                                currentTab = AppDestination.WEATHER
                            },
                            onToggleFavoriteCity = viewModel::toggleFavoriteCity,
                            onApplyCityToAllWidgets = viewModel::applyCityToAllWidgets,
                            onDeleteCity = viewModel::deleteSavedCity,
                            onRequestOptionalGps = {
                                viewModel.syncOptionalCurrentLocation(onSuccessSelectForStudio = false)
                            },
                            onPermissionDenied = viewModel::onLocationPermissionDenied
                        )
                    }
                }
            }
        }
    }
}
