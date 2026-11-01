package com.example

import com.example.data.model.WeatherFormatter
import com.example.data.remote.CurrentDto
import com.example.data.remote.DailyDto
import com.example.data.remote.GeocodingItemDto
import com.example.data.remote.HourlyDto
import com.example.data.remote.NetworkModule
import com.example.data.remote.OpenMeteoForecastResponse
import com.example.data.remote.OpenMeteoGeocodingResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `temperature formatting converts Celsius and Fahrenheit accurately`() {
        assertEquals("25°", WeatherFormatter.formatTemp(25.2, useFahrenheit = false))
        assertEquals("77°", WeatherFormatter.formatTemp(25.0, useFahrenheit = true))
        assertEquals("晴间多云", WeatherFormatter.conditionLabel(2, isDay = true))
        assertEquals("晴夜", WeatherFormatter.conditionLabel(0, isDay = false))
    }

    @Test
    fun `detectQueryLanguage selects zh for CJK and en for Latin queries`() {
        assertEquals("zh", NetworkModule.detectQueryLanguage("北京"))
        assertEquals("zh", NetworkModule.detectQueryLanguage("杭州市"))
        assertEquals("en", NetworkModule.detectQueryLanguage("London"))
        assertEquals("en", NetworkModule.detectQueryLanguage("San Francisco"))
    }

    @Test
    fun `mapGeocodingResults ranks by population and formats administrative subtitles`() {
        val dto = OpenMeteoGeocodingResponse(
            results = listOf(
                GeocodingItemDto(
                    id = 1L,
                    name = "London",
                    latitude = 42.9834,
                    longitude = -81.233,
                    country = "Canada",
                    countryCode = "CA",
                    admin1 = "Ontario",
                    population = 422_000L
                ),
                GeocodingItemDto(
                    id = 2L,
                    name = "London",
                    latitude = 51.5085,
                    longitude = -0.1257,
                    country = "United Kingdom",
                    countryCode = "GB",
                    admin1 = "England",
                    admin2 = "Greater London",
                    population = 8_982_000L
                )
            )
        )

        val mapped = NetworkModule.mapGeocodingResults(dto)
        assertEquals(2, mapped.size)
        assertEquals("GB", mapped[0].countryCode)
        assertEquals("Greater London · England · United Kingdom", mapped[0].displaySubtitle)
        assertEquals("Ontario · Canada", mapped[1].displaySubtitle)
    }

    @Test
    fun `mapForecastToSnapshot maps OpenMeteo response cleanly`() {
        val dto = OpenMeteoForecastResponse(
            timezone = "Asia/Shanghai",
            current = CurrentDto(
                time = "2026-11-01T14:00",
                temperature2m = 22.4,
                relativeHumidity2m = 45,
                apparentTemperature = 21.8,
                isDay = 1,
                weatherCode = 1,
                surfacePressure = 1016.0,
                windSpeed10m = 11.0,
                windDirection10m = 90
            ),
            hourly = HourlyDto(
                time = listOf("2026-11-01T14:00", "2026-11-01T15:00"),
                temperature2m = listOf(22.4, 23.1),
                precipitationProbability = listOf(10, 20),
                weatherCode = listOf(1, 2),
                isDay = listOf(1, 1),
                visibility = listOf(15000.0, 14000.0),
                uvIndex = listOf(4.2, 3.8),
                dewPoint2m = listOf(9.5, 9.8)
            ),
            daily = DailyDto(
                time = listOf("2026-11-01"),
                weatherCode = listOf(1),
                temperatureMax = listOf(24.0),
                temperatureMin = listOf(12.0),
                sunrise = listOf("2026-11-01T06:42"),
                sunset = listOf("2026-11-01T17:13"),
                uvIndexMax = listOf(4.5),
                precipProbabilityMax = listOf(20)
            )
        )

        val snapshot = NetworkModule.mapForecastToSnapshot(dto)
        assertEquals(22.4, snapshot.temperatureC, 0.01)
        assertEquals("06:42", snapshot.sunriseTime)
        assertEquals("17:13", snapshot.sunsetTime)
        assertEquals(2, snapshot.hourly.size)
        assertEquals("现在", snapshot.hourly[0].timeLabel)
        assertEquals("15时", snapshot.hourly[1].timeLabel)
    }
}
