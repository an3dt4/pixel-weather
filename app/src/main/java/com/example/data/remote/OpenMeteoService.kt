package com.example.data.remote

import com.example.data.model.CitySearchResult
import com.example.data.model.DailyForecastItem
import com.example.data.model.HourlyForecastItem
import com.example.data.model.WeatherSnapshot
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenMeteoForecastResponse(
    @Json(name = "timezone") val timezone: String? = "UTC",
    @Json(name = "current") val current: CurrentDto? = null,
    @Json(name = "hourly") val hourly: HourlyDto? = null,
    @Json(name = "daily") val daily: DailyDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentDto(
    @Json(name = "time") val time: String? = null,
    @Json(name = "temperature_2m") val temperature2m: Double? = null,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int? = null,
    @Json(name = "apparent_temperature") val apparentTemperature: Double? = null,
    @Json(name = "is_day") val isDay: Int? = 1,
    @Json(name = "weather_code") val weatherCode: Int? = 0,
    @Json(name = "surface_pressure") val surfacePressure: Double? = null,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double? = null,
    @Json(name = "wind_direction_10m") val windDirection10m: Int? = null
)

@JsonClass(generateAdapter = true)
data class HourlyDto(
    @Json(name = "time") val time: List<String>? = null,
    @Json(name = "temperature_2m") val temperature2m: List<Double?>? = null,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int?>? = null,
    @Json(name = "weather_code") val weatherCode: List<Int?>? = null,
    @Json(name = "is_day") val isDay: List<Int?>? = null,
    @Json(name = "visibility") val visibility: List<Double?>? = null,
    @Json(name = "uv_index") val uvIndex: List<Double?>? = null,
    @Json(name = "dew_point_2m") val dewPoint2m: List<Double?>? = null
)

@JsonClass(generateAdapter = true)
data class DailyDto(
    @Json(name = "time") val time: List<String>? = null,
    @Json(name = "weather_code") val weatherCode: List<Int?>? = null,
    @Json(name = "temperature_2m_max") val temperatureMax: List<Double?>? = null,
    @Json(name = "temperature_2m_min") val temperatureMin: List<Double?>? = null,
    @Json(name = "sunrise") val sunrise: List<String?>? = null,
    @Json(name = "sunset") val sunset: List<String?>? = null,
    @Json(name = "uv_index_max") val uvIndexMax: List<Double?>? = null,
    @Json(name = "precipitation_probability_max") val precipProbabilityMax: List<Int?>? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoGeocodingResponse(
    @Json(name = "results") val results: List<GeocodingItemDto>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingItemDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
    @Json(name = "admin1") val admin1: String? = null,
    @Json(name = "admin2") val admin2: String? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "population") val population: Long? = null
)

interface OpenMeteoWeatherApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m",
        @Query("hourly") hourly: String = "temperature_2m,precipitation_probability,weather_code,is_day,visibility,uv_index,dew_point_2m",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,precipitation_probability_max",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7
    ): OpenMeteoForecastResponse
}

interface OpenMeteoGeocodingApi {
    @GET("v1/search")
    suspend fun searchCities(
        @Query("name") query: String,
        @Query("count") count: Int = 20,
        @Query("language") language: String = "zh",
        @Query("format") format: String = "json"
    ): OpenMeteoGeocodingResponse
}

object NetworkModule {
    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    val weatherApi: OpenMeteoWeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoWeatherApi::class.java)
    }

    val geocodingApi: OpenMeteoGeocodingApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoGeocodingApi::class.java)
    }

    /**
     * Detects whether a user's city search query contains CJK ideographs/kana/hangul
     * so Open-Meteo returns matching location names in the same language script as typed.
     */
    fun detectQueryLanguage(query: String): String {
        val hasCjk = query.any { ch ->
            val code = ch.code
            code in 0x4E00..0x9FFF ||
                code in 0x3400..0x4DBF ||
                code in 0x3040..0x30FF ||
                code in 0xAC00..0xD7AF
        }
        return if (hasCjk) "zh" else "en"
    }

    fun mapForecastToSnapshot(dto: OpenMeteoForecastResponse): WeatherSnapshot {
        val current = dto.current
        val daily = dto.daily
        val hourly = dto.hourly
        val tzId = dto.timezone ?: "UTC"

        val currentIsoPrefix = current?.time?.take(13)
        val hourlyTimes = hourly?.time.orEmpty()
        val startIndex = if (currentIsoPrefix != null) {
            hourlyTimes.indexOfFirst { it.startsWith(currentIsoPrefix) }.coerceAtLeast(0)
        } else {
            0
        }

        val currentVisibilityKm = ((hourly?.visibility?.getOrNull(startIndex) ?: 10000.0) / 1000.0)
            .coerceIn(0.5, 50.0)
        val currentUv = hourly?.uvIndex?.getOrNull(startIndex)
            ?: daily?.uvIndexMax?.firstOrNull()
            ?: 0.0
        val currentDewPoint = hourly?.dewPoint2m?.getOrNull(startIndex)
            ?: ((current?.temperature2m ?: 20.0) - 4.0)

        val hourlyItems = (startIndex until (startIndex + 24).coerceAtMost(hourlyTimes.size)).mapIndexed { idx, i ->
            val rawTime = hourlyTimes[i]
            val hourLabel = if (idx == 0) {
                "现在"
            } else {
                extractHourLabel(rawTime)
            }
            HourlyForecastItem(
                timeLabel = hourLabel,
                temperatureC = hourly?.temperature2m?.getOrNull(i) ?: current?.temperature2m ?: 20.0,
                weatherCode = hourly?.weatherCode?.getOrNull(i) ?: current?.weatherCode ?: 0,
                isDay = (hourly?.isDay?.getOrNull(i) ?: 1) == 1,
                precipProbability = hourly?.precipitationProbability?.getOrNull(i) ?: 0
            )
        }

        val dailyTimes = daily?.time.orEmpty()
        val dailyItems = dailyTimes.mapIndexed { index, dateStr ->
            DailyForecastItem(
                dateIso = dateStr,
                dayLabel = if (index == 0) "今天" else formatChineseWeekday(dateStr, tzId),
                weatherCode = daily?.weatherCode?.getOrNull(index) ?: 0,
                minTempC = daily?.temperatureMin?.getOrNull(index) ?: 14.0,
                maxTempC = daily?.temperatureMax?.getOrNull(index) ?: 24.0,
                precipProbabilityMax = daily?.precipProbabilityMax?.getOrNull(index) ?: 0
            )
        }

        val todayMin = dailyItems.firstOrNull()?.minTempC ?: ((current?.temperature2m ?: 20.0) - 4.0)
        val todayMax = dailyItems.firstOrNull()?.maxTempC ?: ((current?.temperature2m ?: 20.0) + 4.0)

        return WeatherSnapshot(
            temperatureC = current?.temperature2m ?: 20.0,
            feelsLikeC = current?.apparentTemperature ?: current?.temperature2m ?: 20.0,
            minTempC = todayMin,
            maxTempC = todayMax,
            weatherCode = current?.weatherCode ?: 0,
            isDay = (current?.isDay ?: 1) == 1,
            humidityPercent = current?.relativeHumidity2m ?: 50,
            dewPointC = currentDewPoint,
            windSpeedKmh = current?.windSpeed10m ?: 8.0,
            windDirectionDeg = current?.windDirection10m ?: 0,
            pressureHpa = current?.surfacePressure ?: 1013.0,
            visibilityKm = currentVisibilityKm,
            uvIndex = currentUv,
            sunriseTime = extractClockTime(daily?.sunrise?.firstOrNull()),
            sunsetTime = extractClockTime(daily?.sunset?.firstOrNull()),
            timezoneId = tzId,
            hourly = hourlyItems,
            daily = dailyItems,
            fetchedAtEpochMs = System.currentTimeMillis()
        )
    }

    fun mapGeocodingResults(dto: OpenMeteoGeocodingResponse): List<CitySearchResult> {
        return dto.results.orEmpty()
            .sortedByDescending { it.population ?: 0L }
            .mapNotNull { item ->
                val name = item.name?.trim().takeIf { !it.isNullOrEmpty() } ?: return@mapNotNull null
                val lat = item.latitude ?: return@mapNotNull null
                val lon = item.longitude ?: return@mapNotNull null
                val id = "city_${"%.3f".format(Locale.US, lat)}_${"%.3f".format(Locale.US, lon)}"
                CitySearchResult(
                    id = id,
                    name = name,
                    adminArea = item.admin1.orEmpty(),
                    subAdminArea = item.admin2.orEmpty(),
                    country = item.country.orEmpty(),
                    countryCode = item.countryCode.orEmpty(),
                    latitude = lat,
                    longitude = lon,
                    timezone = item.timezone.orEmpty()
                )
            }.distinctBy { it.id }
    }

    private fun extractHourLabel(isoDateTime: String): String {
        val tIndex = isoDateTime.indexOf('T')
        if (tIndex != -1 && isoDateTime.length >= tIndex + 3) {
            val hh = isoDateTime.substring(tIndex + 1, tIndex + 3)
            val hourInt = hh.toIntOrNull()
            if (hourInt != null) return "${hourInt}时"
        }
        return isoDateTime.takeLast(5)
    }

    private fun extractClockTime(isoDateTime: String?): String {
        if (isoDateTime.isNullOrBlank()) return "--:--"
        val tIndex = isoDateTime.indexOf('T')
        return if (tIndex != -1 && isoDateTime.length >= tIndex + 6) {
            isoDateTime.substring(tIndex + 1, tIndex + 6)
        } else {
            isoDateTime
        }
    }

    private fun formatChineseWeekday(dateIso: String, tzId: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone(tzId)
            }
            val parsed = parser.parse(dateIso) ?: return dateIso.takeLast(5)
            val cal = Calendar.getInstance(TimeZone.getTimeZone(tzId)).apply { time = parsed }
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "周日"
                Calendar.MONDAY -> "周一"
                Calendar.TUESDAY -> "周二"
                Calendar.WEDNESDAY -> "周三"
                Calendar.THURSDAY -> "周四"
                Calendar.FRIDAY -> "周五"
                Calendar.SATURDAY -> "周六"
                else -> dateIso.takeLast(5)
            }
        } catch (_: Exception) {
            dateIso.takeLast(5)
        }
    }
}
