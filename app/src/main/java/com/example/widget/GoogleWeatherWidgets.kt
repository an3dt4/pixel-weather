package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.example.R
import com.example.data.model.ActiveWidgetInstanceUi
import com.example.data.model.WidgetPalette
import com.example.data.model.WidgetStyle
import com.example.data.repository.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private val KEY_WIDGET_REVISION = longPreferencesKey("widget_revision_ts")

private val DATE_TIME_CHANGE_ACTIONS = setOf(
    Intent.ACTION_DATE_CHANGED,
    Intent.ACTION_TIME_CHANGED,
    Intent.ACTION_TIMEZONE_CHANGED
)

class PixelAtAGlanceWeatherWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(180.dp, 62.dp),
            DpSize(270.dp, 74.dp),
            DpSize(330.dp, 76.dp),
            DpSize(330.dp, 100.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideWeatherGlanceContent(context, id, WidgetStyle.PIXEL_AT_A_GLANCE)
    }
}

class GooglePillWeatherWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(140.dp, 140.dp),
            DpSize(180.dp, 180.dp),
            DpSize(260.dp, 74.dp),
            DpSize(320.dp, 76.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideWeatherGlanceContent(context, id, WidgetStyle.PIXEL_AT_A_GLANCE)
    }
}

class GoogleForecastWeatherWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(220.dp, 110.dp),
            DpSize(280.dp, 140.dp),
            DpSize(320.dp, 220.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideWeatherGlanceContent(context, id, WidgetStyle.FORECAST_CARD)
    }
}

private suspend fun GlanceAppWidget.provideWeatherGlanceContent(
    context: Context,
    id: GlanceId,
    defaultStyle: WidgetStyle
) {
    val manager = GlanceAppWidgetManager(context)
    val appWidgetId = runCatching { manager.getAppWidgetId(id) }.getOrDefault(0)
    val repository = WeatherRepository.getInstance(context)
    val initialPayload = repository.resolveWidgetPayload(
        appWidgetId = appWidgetId,
        defaultStyle = defaultStyle
    )

    val livePayloadFlow = combine(
        repository.savedCitiesFlow,
        repository.widgetConfigsFlow,
        repository.useFahrenheitFlow
    ) { _, _, _ ->
        repository.resolveWidgetPayload(
            appWidgetId = appWidgetId,
            defaultStyle = defaultStyle
        )
    }

    provideContent {
        val prefs = currentState<Preferences>()
        val ignoredRev = prefs[KEY_WIDGET_REVISION] ?: 0L
        val payload by livePayloadFlow.collectAsState(initial = initialPayload)
        if (ignoredRev >= 0L) {
            GoogleStyleWidgetRoot(payload = payload)
        }
    }
}

abstract class BasePixelWeatherWidgetReceiver : GlanceAppWidgetReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in DATE_TIME_CHANGE_ACTIONS) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                WeatherWidgetUpdater.updateAllWidgets(context)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WeatherSyncWorker.schedulePeriodicSync(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val repo = WeatherRepository.getInstance(context)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            appWidgetIds.forEach { id -> repo.removeWidgetConfig(id) }
        }
    }
}

class PixelAtAGlanceWidgetReceiver : BasePixelWeatherWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PixelAtAGlanceWeatherWidget()
}

class GooglePillWeatherWidgetReceiver : BasePixelWeatherWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GooglePillWeatherWidget()
}

class GoogleForecastWeatherWidgetReceiver : BasePixelWeatherWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GoogleForecastWeatherWidget()
}

object WeatherWidgetUpdater {

    suspend fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext
        val manager = GlanceAppWidgetManager(appContext)
        val now = System.currentTimeMillis()

        val atAGlanceWidget = PixelAtAGlanceWeatherWidget()
        for (glanceId in manager.getGlanceIds(PixelAtAGlanceWeatherWidget::class.java)) {
            updateAppWidgetState(appContext, glanceId) { prefs ->
                prefs[KEY_WIDGET_REVISION] = now
            }
            atAGlanceWidget.update(appContext, glanceId)
        }

        val pillWidget = GooglePillWeatherWidget()
        for (glanceId in manager.getGlanceIds(GooglePillWeatherWidget::class.java)) {
            updateAppWidgetState(appContext, glanceId) { prefs ->
                prefs[KEY_WIDGET_REVISION] = now
            }
            pillWidget.update(appContext, glanceId)
        }

        val forecastWidget = GoogleForecastWeatherWidget()
        for (glanceId in manager.getGlanceIds(GoogleForecastWeatherWidget::class.java)) {
            updateAppWidgetState(appContext, glanceId) { prefs ->
                prefs[KEY_WIDGET_REVISION] = now
            }
            forecastWidget.update(appContext, glanceId)
        }
    }

    suspend fun getActiveWidgetInstances(context: Context): List<ActiveWidgetInstanceUi> {
        val appContext = context.applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(appContext)
        val repo = WeatherRepository.getInstance(appContext)

        val glanceIds = appWidgetManager.getAppWidgetIds(
            ComponentName(appContext, PixelAtAGlanceWidgetReceiver::class.java)
        )
        val pillIds = appWidgetManager.getAppWidgetIds(
            ComponentName(appContext, GooglePillWeatherWidgetReceiver::class.java)
        )
        val forecastIds = appWidgetManager.getAppWidgetIds(
            ComponentName(appContext, GoogleForecastWeatherWidgetReceiver::class.java)
        )

        val results = mutableListOf<ActiveWidgetInstanceUi>()

        suspend fun appendInstances(
            ids: IntArray,
            defaultStyle: WidgetStyle,
            isForecast: Boolean
        ) {
            for (id in ids) {
                val payload = repo.resolveWidgetPayload(id, defaultStyle)
                val rawConfig = repo.getWidgetConfigOnce(id)
                results.add(
                    ActiveWidgetInstanceUi(
                        appWidgetId = id,
                        isForecastProvider = isForecast,
                        cityId = rawConfig?.cityId ?: WeatherRepository.FOLLOW_PRIMARY_CITY_ID,
                        resolvedCityName = payload.cityName,
                        isGpsCity = payload.isGpsLocation,
                        style = payload.style,
                        palette = payload.palette,
                        backgroundAlpha = payload.backgroundAlpha,
                        showBorder = payload.showBorder,
                        showDate = payload.showDate,
                        useEnglishDate = payload.useEnglishDate,
                        showCityOnWidget = payload.showCityOnWidget,
                        useFahrenheit = payload.useFahrenheit
                    )
                )
            }
        }

        appendInstances(glanceIds, WidgetStyle.PIXEL_AT_A_GLANCE, isForecast = false)
        appendInstances(pillIds, WidgetStyle.PIXEL_AT_A_GLANCE, isForecast = false)
        appendInstances(forecastIds, WidgetStyle.FORECAST_CARD, isForecast = true)

        return results.sortedBy { it.appWidgetId }
    }

    fun requestPinWidgetToHomeScreen(
        context: Context,
        style: WidgetStyle,
        palette: WidgetPalette,
        cityId: String,
        useFahrenheit: Boolean,
        backgroundAlpha: Float = 0.68f,
        showBorder: Boolean = false,
        showDate: Boolean = true,
        useEnglishDate: Boolean = false,
        showCityOnWidget: Boolean = true
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (!appWidgetManager.isRequestPinAppWidgetSupported) return false

        val receiverClass = when (style) {
            WidgetStyle.PIXEL_AT_A_GLANCE, WidgetStyle.PIXEL_MINIMAL ->
                PixelAtAGlanceWidgetReceiver::class.java
            WidgetStyle.FORECAST_CARD ->
                GoogleForecastWeatherWidgetReceiver::class.java
            WidgetStyle.ROUNDED_SQUARE ->
                GooglePillWeatherWidgetReceiver::class.java
        }
        val provider = ComponentName(context, receiverClass)

        val callbackIntent = Intent(context, MainActivityPinCallbackHelper.targetClass).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("pinned_style", style.key)
            putExtra("pinned_palette", palette.key)
            putExtra("pinned_city_id", cityId)
            putExtra("pinned_fahrenheit", useFahrenheit)
            putExtra("pinned_alpha", backgroundAlpha)
            putExtra("pinned_show_border", showBorder)
            putExtra("pinned_show_date", showDate)
            putExtra("pinned_english_date", useEnglishDate)
            putExtra("pinned_show_city", showCityOnWidget)
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        val successCallback = PendingIntent.getActivity(
            context,
            style.ordinal + 200,
            callbackIntent,
            pendingFlags
        )

        return appWidgetManager.requestPinAppWidget(provider, null, successCallback)
    }

    fun requestPinAppIconToHomeScreen(context: Context): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
        val launchIntent = Intent(context, MainActivityPinCallbackHelper.targetClass).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val appLabel = context.getString(R.string.app_name)
        val shortcutInfo = ShortcutInfoCompat.Builder(context, "skypill_home_launcher_icon")
            .setShortLabel(appLabel)
            .setLongLabel(appLabel)
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(launchIntent)
            .build()
        return ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
    }

    fun defaultStyleForWidgetId(context: Context, appWidgetId: Int): WidgetStyle {
        val manager = AppWidgetManager.getInstance(context)
        val forecastIds = manager.getAppWidgetIds(
            ComponentName(context, GoogleForecastWeatherWidgetReceiver::class.java)
        )
        if (forecastIds.contains(appWidgetId)) return WidgetStyle.FORECAST_CARD

        val pillIds = manager.getAppWidgetIds(
            ComponentName(context, GooglePillWeatherWidgetReceiver::class.java)
        )
        if (pillIds.contains(appWidgetId)) return WidgetStyle.ROUNDED_SQUARE

        return WidgetStyle.PIXEL_AT_A_GLANCE
    }
}

object MainActivityPinCallbackHelper {
    val targetClass = com.example.MainActivity::class.java
}
