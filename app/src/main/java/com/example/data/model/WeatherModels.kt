package com.example.data.model

import androidx.annotation.DrawableRes
import com.example.R
import com.squareup.moshi.JsonClass
import java.util.Locale
import kotlin.math.roundToInt

enum class WidgetStyle(val key: String, val label: String) {
    DIAGONAL_PILL("DIAGONAL_PILL", "斜向胶囊 (2×2)"),
    ROUNDED_SQUARE("ROUNDED_SQUARE", "圆角方卡 (2×2)"),
    FORECAST_CARD("FORECAST_CARD", "多时预报 (4×2)");

    companion object {
        fun fromKey(key: String?): WidgetStyle =
            entries.firstOrNull { it.key == key } ?: DIAGONAL_PILL
    }
}

enum class WidgetPalette(
    val key: String,
    val label: String,
    @DrawableRes val diagonalPillDrawable: Int
) {
    DYNAMIC_SKY("DYNAMIC_SKY", "莫奈晴空", R.drawable.bg_pill_diagonal_dynamic),
    DAYLIGHT_BLUE("DAYLIGHT_BLUE", "晨曦浅蓝", R.drawable.bg_pill_diagonal_daylight),
    MIDNIGHT_INDIGO("MIDNIGHT_INDIGO", "深邃夜幕", R.drawable.bg_pill_diagonal_midnight),
    WARM_SUN("WARM_SUN", "暖阳奶咖", R.drawable.bg_pill_diagonal_warm);

    companion object {
        fun fromKey(key: String?): WidgetPalette =
            entries.firstOrNull { it.key == key } ?: DYNAMIC_SKY
    }
}

@JsonClass(generateAdapter = true)
data class HourlyForecastItem(
    val timeLabel: String,
    val temperatureC: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val precipProbability: Int
)

@JsonClass(generateAdapter = true)
data class DailyForecastItem(
    val dateIso: String,
    val dayLabel: String,
    val weatherCode: Int,
    val minTempC: Double,
    val maxTempC: Double,
    val precipProbabilityMax: Int
)

@JsonClass(generateAdapter = true)
data class WeatherSnapshot(
    val temperatureC: Double,
    val feelsLikeC: Double,
    val minTempC: Double,
    val maxTempC: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val humidityPercent: Int,
    val dewPointC: Double,
    val windSpeedKmh: Double,
    val windDirectionDeg: Int,
    val pressureHpa: Double,
    val visibilityKm: Double,
    val uvIndex: Double,
    val sunriseTime: String,
    val sunsetTime: String,
    val timezoneId: String,
    val hourly: List<HourlyForecastItem>,
    val daily: List<DailyForecastItem>,
    val fetchedAtEpochMs: Long
)

data class CitySearchResult(
    val id: String,
    val name: String,
    val adminArea: String,
    val subAdminArea: String = "",
    val country: String,
    val countryCode: String = "",
    val latitude: Double,
    val longitude: Double,
    val timezone: String = ""
) {
    val displaySubtitle: String
        get() = listOf(subAdminArea, adminArea, country)
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != name }
            .distinct()
            .joinToString(" · ")
            .ifEmpty { coordinatesLabel }

    val coordinatesLabel: String
        get() = "${"%.2f".format(Locale.US, latitude)}°, ${"%.2f".format(Locale.US, longitude)}°"
}

data class SavedCityUiModel(
    val id: String,
    val name: String,
    val adminArea: String,
    val latitude: Double,
    val longitude: Double,
    val isCurrentLocation: Boolean,
    val isSelectedPrimary: Boolean,
    val isPinnedFavorite: Boolean = false,
    val lastAccessedEpochMs: Long = 0L,
    val weather: WeatherSnapshot?
)

data class ActiveWidgetInstanceUi(
    val appWidgetId: Int,
    val isForecastProvider: Boolean,
    val cityId: String,
    val resolvedCityName: String,
    val isGpsCity: Boolean,
    val style: WidgetStyle,
    val palette: WidgetPalette,
    val useFahrenheit: Boolean
)

object WeatherFormatter {
    fun formatTemp(tempC: Double, useFahrenheit: Boolean, includeDegree: Boolean = true): String {
        val value = if (useFahrenheit) {
            (tempC * 9.0 / 5.0 + 32.0).roundToInt()
        } else {
            tempC.roundToInt()
        }
        return if (includeDegree) "$value°" else "$value"
    }

    fun windDirectionLabel(deg: Int): String {
        val normalized = ((deg % 360) + 360) % 360
        return when (normalized) {
            in 0..22, in 338..360 -> "北风"
            in 23..67 -> "东北风"
            in 68..112 -> "东风"
            in 113..157 -> "东南风"
            in 158..202 -> "南风"
            in 203..247 -> "西南风"
            in 248..292 -> "西风"
            else -> "西北风"
        }
    }

    fun uvLevelLabel(uv: Double): String = when {
        uv < 3.0 -> "偏弱"
        uv < 6.0 -> "中等"
        uv < 8.0 -> "较强"
        uv < 11.0 -> "很强"
        else -> "极强"
    }

    fun conditionLabel(code: Int, isDay: Boolean = true): String = when (code) {
        0 -> if (isDay) "晴朗" else "晴夜"
        1 -> if (isDay) "大部晴朗" else "夜间少云"
        2 -> "晴间多云"
        3 -> "阴天"
        45, 48 -> "有雾"
        51, 53, 55 -> "毛毛雨"
        56, 57 -> "冻毛毛雨"
        61 -> "小雨"
        63 -> "中雨"
        65 -> "大雨"
        66, 67 -> "冻雨"
        71 -> "小雪"
        73 -> "中雪"
        75 -> "大雪"
        77 -> "雪粒"
        80, 81 -> "阵雨"
        82 -> "强阵雨"
        85, 86 -> "阵雪"
        95 -> "雷阵雨"
        96, 99 -> "雷暴伴冰雹"
        else -> "多云"
    }

    @DrawableRes
    fun conditionIconRes(code: Int, isDay: Boolean = true): Int = when (code) {
        0 -> if (isDay) R.drawable.ic_weather_clear_day else R.drawable.ic_weather_clear_night
        1, 2 -> if (isDay) R.drawable.ic_weather_partly_cloudy_day else R.drawable.ic_weather_partly_cloudy_night
        3 -> R.drawable.ic_weather_cloudy
        45, 48 -> R.drawable.ic_weather_fog
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> R.drawable.ic_weather_rain
        71, 73, 75, 77, 85, 86 -> R.drawable.ic_weather_snow
        95, 96, 99 -> R.drawable.ic_weather_thunderstorm
        else -> if (isDay) R.drawable.ic_weather_partly_cloudy_day else R.drawable.ic_weather_partly_cloudy_night
    }

    fun buildSmartInsight(snapshot: WeatherSnapshot, useFahrenheit: Boolean): String {
        val spreadC = (snapshot.maxTempC - snapshot.minTempC).roundToInt()
        val maxFormatted = formatTemp(snapshot.maxTempC, useFahrenheit)
        return when {
            snapshot.weatherCode in listOf(95, 96, 99) ->
                "今日有雷暴活动，外出请携带雨具并留意防雷安全。"
            snapshot.weatherCode in listOf(61, 63, 65, 80, 81, 82) ->
                "预计出现降雨，最高气温 $maxFormatted，建议随身备伞。"
            snapshot.weatherCode in listOf(71, 73, 75, 85, 86) ->
                "今日有降雪天气，路面可能湿滑，出行请注意保暖防滑。"
            snapshot.uvIndex >= 7.0 && snapshot.isDay ->
                "午后紫外线较强（指数 ${snapshot.uvIndex.roundToInt()}），户外活动建议做好防晒。"
            spreadC >= 10 ->
                "今日昼夜温差达 ${spreadC}°C，早晚偏凉，建议采用洋葱式穿搭。"
            snapshot.temperatureC >= 30.0 ->
                "今日气温较高，最高 $maxFormatted，请注意及时补充水分。"
            snapshot.temperatureC <= 5.0 ->
                "当前体感偏冷（${formatTemp(snapshot.feelsLikeC, useFahrenheit)}），外出请添衣保暖。"
            else ->
                "当前${conditionLabel(snapshot.weatherCode, snapshot.isDay)}，风速 ${snapshot.windSpeedKmh.roundToInt()} km/h，体感舒适宜出行。"
        }
    }
}
