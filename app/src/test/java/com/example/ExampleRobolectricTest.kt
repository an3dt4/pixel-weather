package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SavedCityEntity
import com.example.data.local.WeatherDatabase
import com.example.data.local.WidgetConfigEntity
import com.example.data.model.WeatherFormatter
import com.example.data.model.WidgetPalette
import com.example.data.model.WidgetStyle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name and at a glance widget metadata strings from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("晴空天气", context.getString(R.string.app_name))
        assertTrue(context.getString(R.string.widget_at_a_glance_name).contains("At a Glance"))
        assertTrue(context.getString(R.string.widget_forecast_name).contains("预报"))
    }

    @Test
    fun `at a glance widget style, legacy mapping, date format, and opacity labels resolve accurately`() {
        assertEquals(WidgetStyle.PIXEL_AT_A_GLANCE, WidgetStyle.fromKey("PIXEL_AT_A_GLANCE"))
        assertEquals(WidgetStyle.PIXEL_AT_A_GLANCE, WidgetStyle.fromKey("DIAGONAL_PILL"))
        assertEquals(WidgetStyle.ROUNDED_SQUARE, WidgetStyle.fromKey("ROUNDED_SQUARE"))
        assertEquals(WidgetStyle.FORECAST_CARD, WidgetStyle.fromKey("FORECAST_CARD"))
        assertEquals(WidgetPalette.LIGHT_PILL, WidgetPalette.fromKey("LIGHT_PILL"))
        assertEquals(WidgetPalette.DARK_PILL, WidgetPalette.fromKey("MIDNIGHT_INDIGO"))

        // Verify Chinese and English At a Glance date formatting (2026-11-02 UTC is Monday, Nov 2)
        val nov2EpochMs = 1793616000000L // 2026-11-02T10:40:00Z
        assertEquals("Monday, Nov 2", WeatherFormatter.formatAtAGlanceDate("UTC", useEnglish = true, shortForm = false, nowEpochMs = nov2EpochMs))
        assertEquals("11月2日 星期一", WeatherFormatter.formatAtAGlanceDate("UTC", useEnglish = false, shortForm = false, nowEpochMs = nov2EpochMs))
        assertEquals("12°C", WeatherFormatter.formatTempWithUnit(12.2, useFahrenheit = false))
        assertTrue(WeatherFormatter.formatOpacityModeLabel(0.0f, showBorder = true).contains("Transparent"))
        assertTrue(WeatherFormatter.formatOpacityModeLabel(0.68f, showBorder = false).contains("Semi-transparent"))
        assertTrue(WeatherFormatter.formatOpacityModeLabel(1.0f, showBorder = false).contains("Solid"))
    }

    @Test
    fun `saved locations and widget opacity border date configs persist in Room`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WeatherDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.weatherDao()

        dao.upsertCity(
            SavedCityEntity(
                id = "city_tokyo",
                name = "Tokyo",
                adminArea = "Tokyo · Japan",
                latitude = 35.6762,
                longitude = 139.6503,
                lastAccessedEpochMs = 1000L
            )
        )
        dao.upsertCity(
            SavedCityEntity(
                id = "city_london",
                name = "London",
                adminArea = "England · United Kingdom",
                latitude = 51.5074,
                longitude = -0.1278,
                lastAccessedEpochMs = 2000L
            )
        )

        val initialOrder = dao.getAllCitiesOnce()
        assertEquals(listOf("London", "Tokyo"), initialOrder.map { it.name })

        dao.setCityFavorite("city_tokyo", true)
        val afterPinOrder = dao.getAllCitiesOnce()
        assertEquals(listOf("Tokyo", "London"), afterPinOrder.map { it.name })

        dao.upsertWidgetConfig(
            WidgetConfigEntity(
                appWidgetId = 42,
                cityId = "city_tokyo",
                widgetStyle = WidgetStyle.PIXEL_AT_A_GLANCE.key,
                colorPalette = WidgetPalette.LIGHT_PILL.key,
                useFahrenheit = false,
                backgroundAlpha = 0.0f,
                showBorder = true,
                showDate = true,
                useEnglishDate = true,
                showCityOnWidget = true
            )
        )
        val loadedConfig = dao.getWidgetConfig(42)
        assertNotNull(loadedConfig)
        assertEquals(0.0f, loadedConfig!!.backgroundAlpha, 0.001f)
        assertEquals(true, loadedConfig.showBorder)
        assertEquals(true, loadedConfig.useEnglishDate)
        assertEquals("city_tokyo", loadedConfig.cityId)

        db.close()
    }

    @Test
    fun `launcher activity is registered and discoverable in package manager`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            setPackage(context.packageName)
        }
        val resolved = context.packageManager.queryIntentActivities(launcherIntent, 0)
        assertTrue(resolved.any { it.activityInfo.name.endsWith("MainActivity") })
    }
}
