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

class GooglePillWeatherWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(180.dp, 110.dp),
            DpSize(180.dp, 180.dp),
            DpSize(250.dp, 140.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val manager = GlanceAppWidgetManager(context)
        val appWidgetId = runCatching { manager.getAppWidgetId(id) }.getOrDefault(0)
        val repository = WeatherRepository.getInstance(context)
        val initialPayload = repository.resolveWidgetPayload(
            appWidgetId = appWidgetId,
            defaultStyle = WidgetStyle.DIAGONAL_PILL
        )

        val livePayloadFlow = combine(
            repository.savedCitiesFlow,
            repository.widgetConfigsFlow,
            repository.useFahrenheitFlow
        ) { _, _, _ ->
            repository.resolveWidgetPayload(
                appWidgetId = appWidgetId,
                defaultStyle = WidgetStyle.DIAGONAL_PILL
            )
        }

        provideContent {
            // Read state revision so updateAppWidgetState triggers recomposition in active sessions
            val prefs = currentState<Preferences>()
            val ignoredRev = prefs[KEY_WIDGET_REVISION] ?: 0L
            val payload by livePayloadFlow.collectAsState(initial = initialPayload)
            if (ignoredRev >= 0L) {
                GoogleStyleWidgetRoot(payload = payload)
            }
        }
    }
}

class GoogleForecastWeatherWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(200.dp, 110.dp),
            DpSize(270.dp, 130.dp),
            DpSize(270.dp, 220.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val manager = GlanceAppWidgetManager(context)
        val appWidgetId = runCatching { manager.getAppWidgetId(id) }.getOrDefault(0)
        val repository = WeatherRepository.getInstance(context)
        val initialPayload = repository.resolveWidgetPayload(
            appWidgetId = appWidgetId,
            defaultStyle = WidgetStyle.FORECAST_CARD
        )

        val livePayloadFlow = combine(
            repository.savedCitiesFlow,
            repository.widgetConfigsFlow,
            repository.useFahrenheitFlow
        ) { _, _, _ ->
            repository.resolveWidgetPayload(
                appWidgetId = appWidgetId,
                defaultStyle = WidgetStyle.FORECAST_CARD
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
}

class GooglePillWeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GooglePillWeatherWidget()

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

class GoogleForecastWeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GoogleForecastWeatherWidget()

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

object WeatherWidgetUpdater {

    suspend fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext
        val manager = GlanceAppWidgetManager(appContext)
        val now = System.currentTimeMillis()

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

        val pillIds = appWidgetManager.getAppWidgetIds(
            ComponentName(appContext, GooglePillWeatherWidgetReceiver::class.java)
        )
        val forecastIds = appWidgetManager.getAppWidgetIds(
            ComponentName(appContext, GoogleForecastWeatherWidgetReceiver::class.java)
        )

        val results = mutableListOf<ActiveWidgetInstanceUi>()

        for (id in pillIds) {
            val payload = repo.resolveWidgetPayload(id, WidgetStyle.DIAGONAL_PILL)
            val rawConfig = repo.getWidgetConfigOnce(id)
            results.add(
                ActiveWidgetInstanceUi(
                    appWidgetId = id,
                    isForecastProvider = false,
                    cityId = rawConfig?.cityId ?: WeatherRepository.FOLLOW_PRIMARY_CITY_ID,
                    resolvedCityName = payload.cityName,
                    isGpsCity = payload.isGpsLocation,
                    style = payload.style,
                    palette = payload.palette,
                    useFahrenheit = payload.useFahrenheit
                )
            )
        }

        for (id in forecastIds) {
            val payload = repo.resolveWidgetPayload(id, WidgetStyle.FORECAST_CARD)
            val rawConfig = repo.getWidgetConfigOnce(id)
            results.add(
                ActiveWidgetInstanceUi(
                    appWidgetId = id,
                    isForecastProvider = true,
                    cityId = rawConfig?.cityId ?: WeatherRepository.FOLLOW_PRIMARY_CITY_ID,
                    resolvedCityName = payload.cityName,
                    isGpsCity = payload.isGpsLocation,
                    style = payload.style,
                    palette = payload.palette,
                    useFahrenheit = payload.useFahrenheit
                )
            )
        }

        return results.sortedBy { it.appWidgetId }
    }

    fun requestPinWidgetToHomeScreen(
        context: Context,
        style: WidgetStyle,
        palette: WidgetPalette,
        cityId: String,
        useFahrenheit: Boolean
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (!appWidgetManager.isRequestPinAppWidgetSupported) return false

        val receiverClass = if (style == WidgetStyle.FORECAST_CARD) {
            GoogleForecastWeatherWidgetReceiver::class.java
        } else {
            GooglePillWeatherWidgetReceiver::class.java
        }
        val provider = ComponentName(context, receiverClass)

        val callbackIntent = Intent(context, MainActivityPinCallbackHelper.targetClass).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("pinned_style", style.key)
            putExtra("pinned_palette", palette.key)
            putExtra("pinned_city_id", cityId)
            putExtra("pinned_fahrenheit", useFahrenheit)
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

    fun isForecastWidgetId(context: Context, appWidgetId: Int): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val forecastIds = manager.getAppWidgetIds(
            ComponentName(context, GoogleForecastWeatherWidgetReceiver::class.java)
        )
        return forecastIds.contains(appWidgetId)
    }
}

object MainActivityPinCallbackHelper {
    val targetClass = com.example.MainActivity::class.java
}
