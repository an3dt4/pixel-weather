package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.SavedCityEntity
import com.example.data.local.WeatherDatabase
import com.example.data.local.WidgetConfigEntity
import com.example.data.location.OptionalLocationHelper
import com.example.data.model.CitySearchResult
import com.example.data.model.SavedCityUiModel
import com.example.data.model.WeatherFormatter
import com.example.data.model.WeatherSnapshot
import com.example.data.model.WidgetPalette
import com.example.data.model.WidgetStyle
import com.example.data.remote.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.weatherPrefsDataStore by preferencesDataStore(name = "skypill_prefs")

data class ResolvedWidgetPayload(
    val appWidgetId: Int,
    val cityName: String,
    val cityId: String,
    val isGpsLocation: Boolean,
    val style: WidgetStyle,
    val palette: WidgetPalette,
    val backgroundAlpha: Float,
    val showBorder: Boolean,
    val showDate: Boolean,
    val useEnglishDate: Boolean,
    val showCityOnWidget: Boolean,
    val dateLongText: String,
    val dateShortText: String,
    val useFahrenheit: Boolean,
    val weather: WeatherSnapshot?
)

class WeatherRepository private constructor(private val appContext: Context) {

    private val database = WeatherDatabase.getInstance(appContext)
    private val dao = database.weatherDao()
    private val snapshotAdapter = NetworkModule.moshi.adapter(WeatherSnapshot::class.java)

    val useFahrenheitFlow: Flow<Boolean> = appContext.weatherPrefsDataStore.data.map { prefs ->
        prefs[KEY_USE_FAHRENHEIT] ?: false
    }

    val savedCitiesFlow: Flow<List<SavedCityUiModel>> = dao.observeAllCities().map { entities ->
        entities.map { it.toUiModel() }
    }

    val widgetConfigsFlow: Flow<List<WidgetConfigEntity>> = dao.observeWidgetConfigs()

    suspend fun ensureSeededAndFresh() = withContext(Dispatchers.IO) {
        val existing = dao.getAllCitiesOnce()
        if (existing.isEmpty()) {
            dao.upsertCities(DEFAULT_SEED_CITIES)
            DEFAULT_SEED_CITIES.forEach { seed ->
                runCatching { refreshCityWeather(seed.id) }
            }
        } else {
            val primary = existing.firstOrNull { it.isSelectedPrimary } ?: existing.first()
            if (!primary.isSelectedPrimary) {
                dao.setPrimaryCity(primary.id)
            }
            val now = System.currentTimeMillis()
            if (primary.cachedWeatherJson == null || now - primary.lastUpdatedEpochMs > 15 * 60_000L) {
                runCatching { refreshCityWeather(primary.id) }
            }
        }
    }

    suspend fun setUseFahrenheit(useFahrenheit: Boolean) {
        appContext.weatherPrefsDataStore.edit { prefs ->
            prefs[KEY_USE_FAHRENHEIT] = useFahrenheit
        }
    }

    suspend fun getUseFahrenheitOnce(): Boolean = useFahrenheitFlow.first()

    suspend fun refreshCityWeather(cityId: String): Result<WeatherSnapshot> =
        withContext(Dispatchers.IO) {
            runCatching {
                var targetCity = dao.getCityById(cityId) ?: error("未找到指定城市")
                if (targetCity.isCurrentLocation && OptionalLocationHelper.hasCoarseLocationPermission(appContext)) {
                    val freshLoc = OptionalLocationHelper.fetchCurrentCoarseLocation(appContext)
                    if (freshLoc != null) {
                        targetCity = targetCity.copy(
                            name = freshLoc.cityName,
                            adminArea = freshLoc.adminSubtitle,
                            latitude = freshLoc.latitude,
                            longitude = freshLoc.longitude
                        )
                        dao.upsertCity(targetCity)
                    }
                }
                val dto = NetworkModule.weatherApi.getForecast(
                    latitude = targetCity.latitude,
                    longitude = targetCity.longitude
                )
                val snapshot = NetworkModule.mapForecastToSnapshot(dto)
                val json = snapshotAdapter.toJson(snapshot)
                dao.updateCityWeatherCache(
                    cityId = targetCity.id,
                    weatherJson = json,
                    updatedAt = snapshot.fetchedAtEpochMs
                )
                snapshot
            }
        }

    suspend fun refreshAllSavedCities() = withContext(Dispatchers.IO) {
        ensureSeededAndFresh()
        val cities = dao.getAllCitiesOnce()
        cities.forEach { city ->
            runCatching { refreshCityWeather(city.id) }
        }
    }

    suspend fun searchGlobalCities(rawQuery: String): Result<List<CitySearchResult>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val trimmed = rawQuery.trim()
                if (trimmed.isEmpty()) return@runCatching emptyList()

                val lang = NetworkModule.detectQueryLanguage(trimmed)
                val localMatches = POPULAR_PRESET_CITIES.filter { preset ->
                    preset.name.contains(trimmed, ignoreCase = true) ||
                        preset.adminArea.contains(trimmed, ignoreCase = true) ||
                        preset.country.contains(trimmed, ignoreCase = true)
                }

                val remoteResults = coroutineScope {
                    val primaryDeferred = async {
                        runCatching {
                            NetworkModule.mapGeocodingResults(
                                NetworkModule.geocodingApi.searchCities(
                                    query = trimmed,
                                    count = 20,
                                    language = lang
                                )
                            )
                        }.getOrNull()
                    }

                    val alternateQuery: String? = when {
                        lang == "zh" && trimmed.length == 2 && !trimmed.endsWith("市") -> "${trimmed}市"
                        lang == "zh" && trimmed.length > 2 &&
                            (trimmed.endsWith("市") || trimmed.endsWith("省") || trimmed.endsWith("区")) ->
                            trimmed.removeSuffix("市").removeSuffix("省").removeSuffix("区")
                        else -> null
                    }

                    val secondaryDeferred = alternateQuery?.let { alt ->
                        async {
                            runCatching {
                                NetworkModule.mapGeocodingResults(
                                    NetworkModule.geocodingApi.searchCities(
                                        query = alt,
                                        count = 15,
                                        language = lang
                                    )
                                )
                            }.getOrNull()
                        }
                    }

                    val firstBatch = primaryDeferred.await()
                    val secondBatch = secondaryDeferred?.await()
                    if (firstBatch == null && secondBatch == null && localMatches.isEmpty()) {
                        error("城市搜索网络连接异常")
                    }
                    (firstBatch.orEmpty() + secondBatch.orEmpty())
                }

                (remoteResults + localMatches).distinctBy { it.id }.take(20)
            }
        }

    suspend fun addOrSelectCity(
        cityResult: CitySearchResult,
        makePrimary: Boolean = true
    ): SavedCityEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val currentList = dao.getAllCitiesOnce()
        val existing = currentList.firstOrNull { it.id == cityResult.id }
        val entity = existing?.copy(
            lastAccessedEpochMs = now
        ) ?: SavedCityEntity(
            id = cityResult.id,
            name = cityResult.name.removeSuffix("市").ifBlank { cityResult.name },
            adminArea = cityResult.displaySubtitle,
            latitude = cityResult.latitude,
            longitude = cityResult.longitude,
            isCurrentLocation = false,
            isSelectedPrimary = makePrimary,
            isPinnedFavorite = false,
            sortOrder = (currentList.maxOfOrNull { it.sortOrder } ?: 0) + 1,
            lastAccessedEpochMs = now
        )
        dao.upsertCity(entity)
        if (makePrimary) {
            dao.setPrimaryCity(entity.id)
        }
        runCatching { refreshCityWeather(entity.id) }
        dao.getCityById(entity.id) ?: entity
    }

    suspend fun syncOptionalGpsCity(makePrimary: Boolean = true): Result<SavedCityEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                val loc = OptionalLocationHelper.fetchCurrentCoarseLocation(appContext)
                    ?: error("无法获取当前粗略位置")
                val now = System.currentTimeMillis()
                val existingGps = dao.getCityById(GPS_CITY_ID)
                val entity = SavedCityEntity(
                    id = GPS_CITY_ID,
                    name = loc.cityName,
                    adminArea = loc.adminSubtitle,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    isCurrentLocation = true,
                    isSelectedPrimary = makePrimary || (existingGps?.isSelectedPrimary == true),
                    isPinnedFavorite = true,
                    sortOrder = -1,
                    lastAccessedEpochMs = now,
                    cachedWeatherJson = existingGps?.cachedWeatherJson,
                    lastUpdatedEpochMs = existingGps?.lastUpdatedEpochMs ?: 0L
                )
                dao.upsertCity(entity)
                if (makePrimary) {
                    dao.setPrimaryCity(GPS_CITY_ID)
                }
                refreshCityWeather(GPS_CITY_ID).getOrThrow()
                dao.getCityById(GPS_CITY_ID) ?: entity
            }
        }

    suspend fun selectPrimaryCity(cityId: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dao.setPrimaryCity(cityId)
        dao.touchCityAccess(cityId, now)
        val city = dao.getCityById(cityId)
        if (city != null && (city.cachedWeatherJson == null || now - city.lastUpdatedEpochMs > 10 * 60_000L)) {
            runCatching { refreshCityWeather(cityId) }
        }
    }

    suspend fun toggleCityFavorite(cityId: String) = withContext(Dispatchers.IO) {
        val city = dao.getCityById(cityId) ?: return@withContext
        dao.setCityFavorite(cityId, !city.isPinnedFavorite)
    }

    suspend fun removeCity(cityId: String) = withContext(Dispatchers.IO) {
        val all = dao.getAllCitiesOnce()
        if (all.size <= 1) return@withContext
        val target = all.firstOrNull { it.id == cityId } ?: return@withContext
        dao.deleteCityById(cityId)
        if (target.isSelectedPrimary) {
            val nextPrimary = dao.getAllCitiesOnce().firstOrNull()
            if (nextPrimary != null) {
                dao.setPrimaryCity(nextPrimary.id)
            }
        }
    }

    suspend fun saveWidgetConfig(
        appWidgetId: Int,
        cityId: String,
        style: WidgetStyle,
        palette: WidgetPalette,
        useFahrenheit: Boolean,
        backgroundAlpha: Float = 0.68f,
        showBorder: Boolean = false,
        showDate: Boolean = true,
        useEnglishDate: Boolean = false,
        showCityOnWidget: Boolean = true
    ) = withContext(Dispatchers.IO) {
        dao.upsertWidgetConfig(
            WidgetConfigEntity(
                appWidgetId = appWidgetId,
                cityId = cityId,
                widgetStyle = style.key,
                colorPalette = palette.key,
                useFahrenheit = useFahrenheit,
                backgroundAlpha = backgroundAlpha.coerceIn(0f, 1f),
                showBorder = showBorder,
                showDate = showDate,
                useEnglishDate = useEnglishDate,
                showCityOnWidget = showCityOnWidget
            )
        )
        val resolvedCityId = if (cityId == FOLLOW_PRIMARY_CITY_ID) {
            dao.getPrimaryCityOnce()?.id
        } else {
            cityId
        }
        if (resolvedCityId != null) {
            dao.touchCityAccess(resolvedCityId, System.currentTimeMillis())
            val city = dao.getCityById(resolvedCityId)
            if (city != null && city.cachedWeatherJson == null) {
                runCatching { refreshCityWeather(resolvedCityId) }
            }
        }
    }

    suspend fun applyCityToAllActiveWidgets(cityId: String) = withContext(Dispatchers.IO) {
        dao.updateAllActiveWidgetsCity(cityId)
        dao.touchCityAccess(cityId, System.currentTimeMillis())
        val template = dao.getWidgetConfig(STUDIO_TEMPLATE_WIDGET_ID)
        if (template != null) {
            dao.upsertWidgetConfig(template.copy(cityId = cityId))
        }
    }

    suspend fun syncStyleAndConfigToWidgetIds(
        widgetIds: List<Int>,
        cityId: String,
        style: WidgetStyle,
        palette: WidgetPalette,
        useFahrenheit: Boolean,
        backgroundAlpha: Float,
        showBorder: Boolean,
        showDate: Boolean,
        useEnglishDate: Boolean,
        showCityOnWidget: Boolean
    ) = withContext(Dispatchers.IO) {
        for (id in widgetIds) {
            if (id > 0) {
                dao.upsertWidgetConfig(
                    WidgetConfigEntity(
                        appWidgetId = id,
                        cityId = cityId,
                        widgetStyle = style.key,
                        colorPalette = palette.key,
                        useFahrenheit = useFahrenheit,
                        backgroundAlpha = backgroundAlpha.coerceIn(0f, 1f),
                        showBorder = showBorder,
                        showDate = showDate,
                        useEnglishDate = useEnglishDate,
                        showCityOnWidget = showCityOnWidget
                    )
                )
            }
        }
    }

    suspend fun removeWidgetConfig(appWidgetId: Int) = withContext(Dispatchers.IO) {
        dao.deleteWidgetConfig(appWidgetId)
    }

    suspend fun getWidgetConfigOnce(appWidgetId: Int): WidgetConfigEntity? =
        withContext(Dispatchers.IO) {
            dao.getWidgetConfig(appWidgetId)
        }

    suspend fun resolveWidgetPayload(
        appWidgetId: Int,
        defaultStyle: WidgetStyle
    ): ResolvedWidgetPayload = withContext(Dispatchers.IO) {
        ensureSeededAndFresh()
        val instanceConfig = dao.getWidgetConfig(appWidgetId)
        val templateConfig = dao.getWidgetConfig(STUDIO_TEMPLATE_WIDGET_ID)
        val globalFahrenheit = getUseFahrenheitOnce()

        val requestedCityId = instanceConfig?.cityId
            ?: templateConfig?.cityId
            ?: FOLLOW_PRIMARY_CITY_ID
        val allCities = dao.getAllCitiesOnce()
        val resolvedCity = when {
            requestedCityId == FOLLOW_PRIMARY_CITY_ID ->
                allCities.firstOrNull { it.isSelectedPrimary } ?: allCities.firstOrNull()
            else ->
                allCities.firstOrNull { it.id == requestedCityId }
                    ?: allCities.firstOrNull { it.isSelectedPrimary }
                    ?: allCities.firstOrNull()
        }

        val weather = if (resolvedCity != null) {
            val cached = decodeWeather(resolvedCity.cachedWeatherJson)
            cached ?: refreshCityWeather(resolvedCity.id).getOrNull()
        } else {
            null
        }

        val resolvedStyle = when {
            instanceConfig != null -> WidgetStyle.fromKey(instanceConfig.widgetStyle)
            templateConfig != null && defaultStyle == WidgetStyle.PIXEL_AT_A_GLANCE ->
                WidgetStyle.fromKey(templateConfig.widgetStyle)
            else -> defaultStyle
        }
        val templateMatchesStyle = templateConfig?.let {
            WidgetStyle.fromKey(it.widgetStyle) == resolvedStyle
        } ?: false

        val resolvedPalette = when {
            instanceConfig != null -> WidgetPalette.fromKey(instanceConfig.colorPalette)
            templateMatchesStyle && templateConfig != null -> WidgetPalette.fromKey(templateConfig.colorPalette)
            resolvedStyle == WidgetStyle.PIXEL_MINIMAL -> WidgetPalette.DARK_PILL
            else -> WidgetPalette.LIGHT_PILL
        }
        val resolvedAlpha = when {
            instanceConfig != null -> instanceConfig.backgroundAlpha
            templateMatchesStyle && templateConfig != null -> templateConfig.backgroundAlpha
            resolvedStyle == WidgetStyle.PIXEL_MINIMAL -> 0.0f
            else -> 0.68f
        }.coerceIn(0f, 1f)
        val resolvedShowBorder = when {
            instanceConfig != null -> instanceConfig.showBorder
            templateMatchesStyle && templateConfig != null -> templateConfig.showBorder
            else -> false
        }
        val resolvedShowDate = instanceConfig?.showDate ?: templateConfig?.showDate ?: true
        val resolvedUseEnglishDate = instanceConfig?.useEnglishDate ?: templateConfig?.useEnglishDate ?: false
        val resolvedShowCity = instanceConfig?.showCityOnWidget ?: templateConfig?.showCityOnWidget ?: true

        val tzId = weather?.timezoneId
        val nowMs = System.currentTimeMillis()

        ResolvedWidgetPayload(
            appWidgetId = appWidgetId,
            cityName = resolvedCity?.name ?: "北京",
            cityId = resolvedCity?.id ?: DEFAULT_SEED_CITIES.first().id,
            isGpsLocation = resolvedCity?.isCurrentLocation == true,
            style = resolvedStyle,
            palette = resolvedPalette,
            backgroundAlpha = resolvedAlpha,
            showBorder = resolvedShowBorder,
            showDate = resolvedShowDate,
            useEnglishDate = resolvedUseEnglishDate,
            showCityOnWidget = resolvedShowCity,
            dateLongText = WeatherFormatter.formatAtAGlanceDate(
                timezoneId = tzId,
                useEnglish = resolvedUseEnglishDate,
                shortForm = false,
                nowEpochMs = nowMs
            ),
            dateShortText = WeatherFormatter.formatAtAGlanceDate(
                timezoneId = tzId,
                useEnglish = resolvedUseEnglishDate,
                shortForm = true,
                nowEpochMs = nowMs
            ),
            useFahrenheit = instanceConfig?.useFahrenheit ?: globalFahrenheit,
            weather = weather
        )
    }

    private fun SavedCityEntity.toUiModel(): SavedCityUiModel = SavedCityUiModel(
        id = id,
        name = name,
        adminArea = adminArea,
        latitude = latitude,
        longitude = longitude,
        isCurrentLocation = isCurrentLocation,
        isSelectedPrimary = isSelectedPrimary,
        isPinnedFavorite = isPinnedFavorite,
        lastAccessedEpochMs = lastAccessedEpochMs,
        weather = decodeWeather(cachedWeatherJson)
    )

    private fun decodeWeather(json: String?): WeatherSnapshot? {
        if (json.isNullOrBlank()) return null
        return runCatching { snapshotAdapter.fromJson(json) }.getOrNull()
    }

    companion object {
        const val FOLLOW_PRIMARY_CITY_ID = "FOLLOW_PRIMARY"
        const val GPS_CITY_ID = "gps_current_location"
        const val STUDIO_TEMPLATE_WIDGET_ID = 0

        private val KEY_USE_FAHRENHEIT = booleanPreferencesKey("use_fahrenheit")

        val POPULAR_PRESET_CITIES: List<CitySearchResult> = listOf(
            CitySearchResult("city_39.904_116.407", "北京", "北京市", country = "中国", countryCode = "CN", latitude = 39.9042, longitude = 116.4074),
            CitySearchResult("city_31.230_121.474", "上海", "上海市", country = "中国", countryCode = "CN", latitude = 31.2304, longitude = 121.4737),
            CitySearchResult("city_22.543_114.058", "深圳", "广东省", country = "中国", countryCode = "CN", latitude = 22.5431, longitude = 114.0579),
            CitySearchResult("city_23.129_113.264", "广州", "广东省", country = "中国", countryCode = "CN", latitude = 23.1291, longitude = 113.2644),
            CitySearchResult("city_30.274_120.155", "杭州", "浙江省", country = "中国", countryCode = "CN", latitude = 30.2741, longitude = 120.1551),
            CitySearchResult("city_30.573_104.067", "成都", "四川省", country = "中国", countryCode = "CN", latitude = 30.5728, longitude = 104.0668),
            CitySearchResult("city_30.593_114.305", "武汉", "湖北省", country = "中国", countryCode = "CN", latitude = 30.5928, longitude = 114.3055),
            CitySearchResult("city_34.342_108.940", "西安", "陕西省", country = "中国", countryCode = "CN", latitude = 34.3416, longitude = 108.9398),
            CitySearchResult("city_22.319_114.169", "香港", "香港特别行政区", country = "中国", countryCode = "HK", latitude = 22.3193, longitude = 114.1694),
            CitySearchResult("city_25.033_121.565", "台北", "台湾", country = "中国", countryCode = "TW", latitude = 25.0330, longitude = 121.5654),
            CitySearchResult("city_35.676_139.650", "Tokyo", "Tokyo", country = "Japan", countryCode = "JP", latitude = 35.6762, longitude = 139.6503),
            CitySearchResult("city_1.352_103.820", "Singapore", "Singapore", country = "Singapore", countryCode = "SG", latitude = 1.3521, longitude = 103.8198),
            CitySearchResult("city_51.507_-0.128", "London", "England", country = "United Kingdom", countryCode = "GB", latitude = 51.5074, longitude = -0.1278),
            CitySearchResult("city_40.713_-74.006", "New York", "New York", country = "United States", countryCode = "US", latitude = 40.7128, longitude = -74.0060)
        )

        private val DEFAULT_SEED_CITIES: List<SavedCityEntity> = listOf(
            SavedCityEntity(
                id = "city_39.904_116.407",
                name = "北京",
                adminArea = "北京市 · 中国",
                latitude = 39.9042,
                longitude = 116.4074,
                isCurrentLocation = false,
                isSelectedPrimary = true,
                isPinnedFavorite = true,
                sortOrder = 1,
                lastAccessedEpochMs = 4000L
            ),
            SavedCityEntity(
                id = "city_31.230_121.474",
                name = "上海",
                adminArea = "上海市 · 中国",
                latitude = 31.2304,
                longitude = 121.4737,
                isCurrentLocation = false,
                isSelectedPrimary = false,
                isPinnedFavorite = false,
                sortOrder = 2,
                lastAccessedEpochMs = 3000L
            ),
            SavedCityEntity(
                id = "city_22.543_114.058",
                name = "深圳",
                adminArea = "广东省 · 中国",
                latitude = 22.5431,
                longitude = 114.0579,
                isCurrentLocation = false,
                isSelectedPrimary = false,
                isPinnedFavorite = false,
                sortOrder = 3,
                lastAccessedEpochMs = 2000L
            )
        )

        @Volatile
        private var INSTANCE: WeatherRepository? = null

        fun getInstance(context: Context): WeatherRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WeatherRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
