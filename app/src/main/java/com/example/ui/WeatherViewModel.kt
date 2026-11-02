package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ActiveWidgetInstanceUi
import com.example.data.model.CitySearchResult
import com.example.data.model.SavedCityUiModel
import com.example.data.model.WidgetPalette
import com.example.data.model.WidgetStyle
import com.example.data.repository.WeatherRepository
import com.example.widget.WeatherSyncWorker
import com.example.widget.WeatherWidgetUpdater
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudioDraftState(
    val selectedStyle: WidgetStyle = WidgetStyle.PIXEL_AT_A_GLANCE,
    val selectedPalette: WidgetPalette = WidgetPalette.LIGHT_PILL,
    val selectedCityId: String = WeatherRepository.FOLLOW_PRIMARY_CITY_ID,
    val backgroundAlpha: Float = 0.68f,
    val showBorder: Boolean = false,
    val showDate: Boolean = true,
    val useEnglishDate: Boolean = false,
    val showCityOnWidget: Boolean = true,
    val previewTallForecast: Boolean = false
)

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<CitySearchResult> = emptyList(),
    val errorMessage: String? = null
)

data class WeatherUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val cities: List<SavedCityUiModel> = emptyList(),
    val primaryCity: SavedCityUiModel? = null,
    val useFahrenheit: Boolean = false,
    val studioDraft: StudioDraftState = StudioDraftState(),
    val activeWidgets: List<ActiveWidgetInstanceUi> = emptyList(),
    val searchState: SearchUiState = SearchUiState(),
    val snackbarMessage: String? = null
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repository = WeatherRepository.getInstance(appContext)

    private val isRefreshingFlow = MutableStateFlow(false)
    private val studioDraftFlow = MutableStateFlow(StudioDraftState())
    private val activeWidgetsFlow = MutableStateFlow<List<ActiveWidgetInstanceUi>>(emptyList())
    private val searchStateFlow = MutableStateFlow(SearchUiState())
    private val snackbarFlow = MutableStateFlow<String?>(null)

    private var searchJob: Job? = null

    val uiState: StateFlow<WeatherUiState> = combine(
        repository.savedCitiesFlow,
        repository.useFahrenheitFlow,
        isRefreshingFlow,
        studioDraftFlow,
        combine(activeWidgetsFlow, searchStateFlow, snackbarFlow) { widgets, search, snack ->
            Triple(widgets, search, snack)
        }
    ) { cities, useFahrenheit, refreshing, draft, (widgets, search, snack) ->
        val primary = cities.firstOrNull { it.isSelectedPrimary } ?: cities.firstOrNull()
        WeatherUiState(
            isLoading = cities.isEmpty(),
            isRefreshing = refreshing,
            cities = cities,
            primaryCity = primary,
            useFahrenheit = useFahrenheit,
            studioDraft = draft,
            activeWidgets = widgets,
            searchState = search,
            snackbarMessage = snack
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = WeatherUiState()
    )

    init {
        WeatherSyncWorker.schedulePeriodicSync(appContext)
        viewModelScope.launch {
            isRefreshingFlow.value = true
            repository.ensureSeededAndFresh()
            val savedTemplate = repository.getWidgetConfigOnce(WeatherRepository.STUDIO_TEMPLATE_WIDGET_ID)
            if (savedTemplate != null) {
                studioDraftFlow.update {
                    it.copy(
                        selectedStyle = WidgetStyle.fromKey(savedTemplate.widgetStyle),
                        selectedPalette = WidgetPalette.fromKey(savedTemplate.colorPalette),
                        selectedCityId = savedTemplate.cityId,
                        backgroundAlpha = savedTemplate.backgroundAlpha,
                        showBorder = savedTemplate.showBorder,
                        showDate = savedTemplate.showDate,
                        useEnglishDate = savedTemplate.useEnglishDate,
                        showCityOnWidget = savedTemplate.showCityOnWidget
                    )
                }
            }
            refreshActiveWidgetInstances()
            WeatherWidgetUpdater.updateAllWidgets(appContext)
            isRefreshingFlow.value = false
        }
    }

    fun refreshActiveWidgetInstances() {
        viewModelScope.launch {
            activeWidgetsFlow.value = WeatherWidgetUpdater.getActiveWidgetInstances(appContext)
        }
    }

    fun refreshCurrentWeather() {
        viewModelScope.launch {
            isRefreshingFlow.value = true
            val primaryId = uiState.value.primaryCity?.id
            if (primaryId != null) {
                repository.refreshCityWeather(primaryId)
                    .onFailure {
                        snackbarFlow.value = "网络同步暂不可用，正在显示已缓存天气数据"
                    }
            } else {
                repository.refreshAllSavedCities()
            }
            WeatherWidgetUpdater.updateAllWidgets(appContext)
            refreshActiveWidgetInstances()
            isRefreshingFlow.value = false
        }
    }

    fun toggleTemperatureUnit() {
        viewModelScope.launch {
            val next = !uiState.value.useFahrenheit
            repository.setUseFahrenheit(next)
            WeatherWidgetUpdater.updateAllWidgets(appContext)
            refreshActiveWidgetInstances()
        }
    }

    fun selectPrimaryCity(cityId: String) {
        viewModelScope.launch {
            isRefreshingFlow.value = true
            repository.selectPrimaryCity(cityId)
            repository.refreshCityWeather(cityId)
            WeatherWidgetUpdater.updateAllWidgets(appContext)
            refreshActiveWidgetInstances()
            isRefreshingFlow.value = false
        }
    }

    fun toggleFavoriteCity(cityId: String) {
        viewModelScope.launch {
            repository.toggleCityFavorite(cityId)
        }
    }

    fun applyCityToAllWidgets(cityId: String, cityName: String) {
        viewModelScope.launch {
            repository.applyCityToAllActiveWidgets(cityId)
            studioDraftFlow.update { it.copy(selectedCityId = cityId) }
            persistStudioTemplateInternal(cityIdOverride = cityId)
            WeatherWidgetUpdater.updateAllWidgets(appContext)
            refreshActiveWidgetInstances()
            snackbarFlow.value = "已将「$cityName」同步至桌面天气微件"
        }
    }

    fun deleteSavedCity(cityId: String) {
        viewModelScope.launch {
            repository.removeCity(cityId)
            if (studioDraftFlow.value.selectedCityId == cityId) {
                studioDraftFlow.update {
                    it.copy(selectedCityId = WeatherRepository.FOLLOW_PRIMARY_CITY_ID)
                }
            }
            WeatherWidgetUpdater.updateAllWidgets(appContext)
            refreshActiveWidgetInstances()
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        searchStateFlow.update { it.copy(query = newQuery, errorMessage = null) }
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            searchStateFlow.update { it.copy(isSearching = false, results = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(250L)
            executeCitySearch(newQuery)
        }
    }

    fun submitImmediateSearch() {
        val currentQuery = searchStateFlow.value.query
        if (currentQuery.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            executeCitySearch(currentQuery)
        }
    }

    private suspend fun executeCitySearch(query: String) {
        searchStateFlow.update { it.copy(isSearching = true, errorMessage = null) }
        repository.searchGlobalCities(query)
            .onSuccess { list ->
                searchStateFlow.update {
                    it.copy(
                        isSearching = false,
                        results = list,
                        errorMessage = if (list.isEmpty()) "未找到匹配「$query」的城市，请尝试其他中英文名称" else null
                    )
                }
            }
            .onFailure {
                searchStateFlow.update {
                    it.copy(
                        isSearching = false,
                        errorMessage = "搜索网络连接异常，您也可直接选择下方常用或热门城市"
                    )
                }
            }
    }

    fun clearSearch() {
        searchJob?.cancel()
        searchStateFlow.value = SearchUiState()
    }

    fun addCityAndSelect(
        cityResult: CitySearchResult,
        alsoApplyStudioDraft: Boolean = false
    ) {
        viewModelScope.launch {
            isRefreshingFlow.value = true
            val entity = repository.addOrSelectCity(cityResult, makePrimary = true)
            if (alsoApplyStudioDraft) {
                studioDraftFlow.update { it.copy(selectedCityId = entity.id) }
                persistStudioTemplateInternal(cityIdOverride = entity.id)
            }
            clearSearch()
            WeatherWidgetUpdater.updateAllWidgets(appContext)
            refreshActiveWidgetInstances()
            isRefreshingFlow.value = false
            snackbarFlow.value = if (alsoApplyStudioDraft) {
                "已为微件选定并保存「${entity.name}」"
            } else {
                "已保存「${entity.name}」至常用城市并切换天气"
            }
        }
    }

    fun syncOptionalCurrentLocation(onSuccessSelectForStudio: Boolean = false) {
        viewModelScope.launch {
            isRefreshingFlow.value = true
            repository.syncOptionalGpsCity(makePrimary = true)
                .onSuccess { gpsEntity ->
                    if (onSuccessSelectForStudio) {
                        studioDraftFlow.update { it.copy(selectedCityId = gpsEntity.id) }
                        persistStudioTemplateInternal(cityIdOverride = gpsEntity.id)
                    }
                    WeatherWidgetUpdater.updateAllWidgets(appContext)
                    refreshActiveWidgetInstances()
                    snackbarFlow.value = "已更新实时定位：${gpsEntity.name}"
                }
                .onFailure {
                    snackbarFlow.value = "暂无法获取实时定位，您可直接搜索任意城市"
                }
            isRefreshingFlow.value = false
        }
    }

    fun onLocationPermissionDenied() {
        snackbarFlow.value = "未开启定位权限，完全不影响使用——可直接搜索并保存任意城市"
    }

    fun updateStudioStyle(style: WidgetStyle) {
        studioDraftFlow.update { current ->
            when {
                style == WidgetStyle.PIXEL_MINIMAL && current.backgroundAlpha == 0.68f ->
                    current.copy(selectedStyle = style, selectedPalette = WidgetPalette.DARK_PILL, backgroundAlpha = 0.0f, showBorder = false)
                style != WidgetStyle.PIXEL_MINIMAL && current.selectedStyle == WidgetStyle.PIXEL_MINIMAL && current.backgroundAlpha == 0.0f && !current.showBorder ->
                    current.copy(selectedStyle = style, selectedPalette = WidgetPalette.LIGHT_PILL, backgroundAlpha = 0.68f)
                else -> current.copy(selectedStyle = style)
            }
        }
        persistStudioTemplate()
    }

    fun updateStudioPalette(palette: WidgetPalette) {
        studioDraftFlow.update { it.copy(selectedPalette = palette) }
        persistStudioTemplate()
    }

    fun updateStudioCity(cityId: String) {
        studioDraftFlow.update { it.copy(selectedCityId = cityId) }
        persistStudioTemplate()
    }

    fun updateStudioOpacity(alpha: Float) {
        val clamped = alpha.coerceIn(0f, 1f)
        studioDraftFlow.update {
            it.copy(
                backgroundAlpha = clamped,
                showBorder = if (clamped >= 0.99f) false else it.showBorder
            )
        }
        persistStudioTemplate()
    }

    fun applyStudioOpacityPreset(alpha: Float, showBorder: Boolean) {
        studioDraftFlow.update {
            it.copy(backgroundAlpha = alpha.coerceIn(0f, 1f), showBorder = showBorder)
        }
        persistStudioTemplate()
    }

    fun updateStudioShowBorder(show: Boolean) {
        studioDraftFlow.update { it.copy(showBorder = show) }
        persistStudioTemplate()
    }

    fun updateStudioShowDate(show: Boolean) {
        studioDraftFlow.update { it.copy(showDate = show) }
        persistStudioTemplate()
    }

    fun updateStudioUseEnglishDate(useEnglish: Boolean) {
        studioDraftFlow.update { it.copy(useEnglishDate = useEnglish) }
        persistStudioTemplate()
    }

    fun updateStudioShowCityOnWidget(show: Boolean) {
        studioDraftFlow.update { it.copy(showCityOnWidget = show) }
        persistStudioTemplate()
    }

    fun toggleStudioTallForecastPreview() {
        studioDraftFlow.update { it.copy(previewTallForecast = !it.previewTallForecast) }
    }

    fun syncDraftToAllActiveWidgets() {
        viewModelScope.launch {
            val draft = studioDraftFlow.value
            val useF = uiState.value.useFahrenheit
            val activeIds = activeWidgetsFlow.value.map { it.appWidgetId }
            persistStudioTemplateInternal()
            if (activeIds.isNotEmpty()) {
                repository.syncStyleAndConfigToWidgetIds(
                    widgetIds = activeIds,
                    cityId = draft.selectedCityId,
                    style = draft.selectedStyle,
                    palette = draft.selectedPalette,
                    useFahrenheit = useF,
                    backgroundAlpha = draft.backgroundAlpha,
                    showBorder = draft.showBorder,
                    showDate = draft.showDate,
                    useEnglishDate = draft.useEnglishDate,
                    showCityOnWidget = draft.showCityOnWidget
                )
            }
            WeatherWidgetUpdater.updateAllWidgets(appContext)
            refreshActiveWidgetInstances()
            snackbarFlow.value = "已将当前 At a Glance 样式与透明度应用到桌面所有微件！"
        }
    }

    private fun persistStudioTemplate() {
        viewModelScope.launch {
            persistStudioTemplateInternal()
            WeatherWidgetUpdater.updateAllWidgets(appContext)
            refreshActiveWidgetInstances()
        }
    }

    private suspend fun persistStudioTemplateInternal(cityIdOverride: String? = null) {
        val draft = studioDraftFlow.value
        val useF = uiState.value.useFahrenheit
        repository.saveWidgetConfig(
            appWidgetId = WeatherRepository.STUDIO_TEMPLATE_WIDGET_ID,
            cityId = cityIdOverride ?: draft.selectedCityId,
            style = draft.selectedStyle,
            palette = draft.selectedPalette,
            useFahrenheit = useF,
            backgroundAlpha = draft.backgroundAlpha,
            showBorder = draft.showBorder,
            showDate = draft.showDate,
            useEnglishDate = draft.useEnglishDate,
            showCityOnWidget = draft.showCityOnWidget
        )
    }

    fun requestPinCurrentStudioWidget() {
        viewModelScope.launch {
            val draft = studioDraftFlow.value
            val useF = uiState.value.useFahrenheit
            persistStudioTemplateInternal()
            val supported = WeatherWidgetUpdater.requestPinWidgetToHomeScreen(
                context = appContext,
                style = draft.selectedStyle,
                palette = draft.selectedPalette,
                cityId = draft.selectedCityId,
                useFahrenheit = useF,
                backgroundAlpha = draft.backgroundAlpha,
                showBorder = draft.showBorder,
                showDate = draft.showDate,
                useEnglishDate = draft.useEnglishDate,
                showCityOnWidget = draft.showCityOnWidget
            )
            snackbarFlow.value = if (supported) {
                "已发起添加 At a Glance 微件请求，请在系统弹窗中点击「添加」"
            } else {
                "请返回手机桌面 -> 长按空白处 -> 微件 -> 选择「晴空天气」添加"
            }
        }
    }

    fun requestPinAppShortcutToHomeScreen() {
        val supported = WeatherWidgetUpdater.requestPinAppIconToHomeScreen(appContext)
        snackbarFlow.value = if (supported) {
            "已发起固定「晴空天气」桌面图标请求，请在弹窗中点击「添加」"
        } else {
            "在桌面从屏幕底部向上滑动打开应用抽屉，长按「晴空天气」即可拖到主屏幕"
        }
    }

    fun handlePinnedWidgetCallback(
        appWidgetId: Int,
        styleKey: String?,
        paletteKey: String?,
        cityId: String?,
        useFahrenheit: Boolean,
        backgroundAlpha: Float,
        showBorder: Boolean,
        showDate: Boolean,
        useEnglishDate: Boolean,
        showCityOnWidget: Boolean
    ) {
        if (appWidgetId <= 0) return
        viewModelScope.launch {
            repository.saveWidgetConfig(
                appWidgetId = appWidgetId,
                cityId = cityId ?: studioDraftFlow.value.selectedCityId,
                style = WidgetStyle.fromKey(styleKey),
                palette = WidgetPalette.fromKey(paletteKey),
                useFahrenheit = useFahrenheit,
                backgroundAlpha = backgroundAlpha,
                showBorder = showBorder,
                showDate = showDate,
                useEnglishDate = useEnglishDate,
                showCityOnWidget = showCityOnWidget
            )
            WeatherWidgetUpdater.updateAllWidgets(appContext)
            refreshActiveWidgetInstances()
            snackbarFlow.value = "At a Glance 桌面微件已添加并同步完成！"
        }
    }

    fun consumeSnackbar() {
        snackbarFlow.value = null
    }
}
