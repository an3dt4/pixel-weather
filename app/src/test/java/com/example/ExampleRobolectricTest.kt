package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SavedCityEntity
import com.example.data.local.WeatherDatabase
import com.example.data.model.WidgetPalette
import com.example.data.model.WidgetStyle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name and widget metadata strings from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("晴空天气", context.getString(R.string.app_name))
        assertTrue(context.getString(R.string.widget_pill_name).contains("胶囊"))
        assertTrue(context.getString(R.string.widget_forecast_name).contains("预报"))
    }

    @Test
    fun `widget style and palette key lookups resolve accurately`() {
        assertEquals(WidgetStyle.DIAGONAL_PILL, WidgetStyle.fromKey("DIAGONAL_PILL"))
        assertEquals(WidgetStyle.ROUNDED_SQUARE, WidgetStyle.fromKey("ROUNDED_SQUARE"))
        assertEquals(WidgetStyle.FORECAST_CARD, WidgetStyle.fromKey("FORECAST_CARD"))
        assertEquals(WidgetPalette.MIDNIGHT_INDIGO, WidgetPalette.fromKey("MIDNIGHT_INDIGO"))
    }

    @Test
    fun `saved locations persist and sort by favorite and recent access`() = runTest {
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

        // Most recently accessed comes first before pinning
        val initialOrder = dao.getAllCitiesOnce()
        assertEquals(listOf("London", "Tokyo"), initialOrder.map { it.name })

        // Pinning Tokyo moves it ahead of non-pinned cities for quick access
        dao.setCityFavorite("city_tokyo", true)
        val afterPinOrder = dao.getAllCitiesOnce()
        assertEquals(listOf("Tokyo", "London"), afterPinOrder.map { it.name })

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
