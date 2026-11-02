package com.example.data.model

import androidx.annotation.DrawableRes
import com.example.R
import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

enum class WidgetStyle(val key: String, val label: String, val shortTag: String) {
    PIXEL_AT_A_GLANCE("PIXEL_AT_A_GLANCE", "At a Glance 胶囊 (4×1)", "胶囊 4×1"),
    FORECAST_CARD("FORECAST_CARD", "At a Glance 预报 (4×2)", "预报 4×2"),
    PIXEL_MINIMAL("PIXEL_MINIMAL", "无框极简一览 (4×1)", "极简 4×1"),
    ROUNDED_SQUARE("ROUNDED_SQUARE", "紧凑方卡 (2×2)", "方卡 2×2");

    companion object {
        fun fromKey(key: String?): WidgetStyle = when (key) {
            "DIAGONAL_PILL" -> PIXEL_AT_A_GLANCE
            else -> entries.firstOrNull { it.key == key } ?: PIXEL_AT_A_GLANCE
        }
    }
}

enum class WidgetPalette(
    val key: String,
    val label: String,
    val subtitle: String
) {
    LIGHT_PILL("LIGHT_PILL", "浅色冰晶", "深色字 · 经典蓝标"),
    DARK_PILL("DARK_PILL", "深色夜幕", "纯白字 · 适配深壁纸"),
    DYNAMIC_SKY("DYNAMIC_SKY", "莫奈天青", "天青底 · 经典蓝标"),
    WARM_SUN("WARM_SUN", "暖阳奶咖", "暖棕字 · 琥珀角标");

    companion object {
        fun fromKey(key: String?): WidgetPalette = when (key) {
            "DAYLIGHT_BLUE" -> LIGHT_PILL
            "MIDNIGHT_INDIGO", "PIXEL_FROST" -> DARK_PILL
            else -> entries.firstOrNull { it.key == key } ?: LIGHT_PILL
        }
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
    val backgroundAlpha: Float,
    val showBorder: Boolean,
    val showDate: Boolean,
    val useEnglishDate: Boolean,
    val showCityOnWidget: Boolean,
    val useFahrenheit: Boolean
)

object WeatherFormatter {

    fun formatAtAGlanceDate(
        timezoneId: String? = null,
        useEnglish: Boolean = false,
        shortForm: Boolean = false,
        nowEpochMs: Long = System.currentTimeMillis()
    ): String {
        val tz = resolveSafeTimeZone(timezoneId)
        val date = Date(nowEpochMs)
        if (useEnglish) {
            val pattern = if (shortForm) "EEE, MMM d" else "EEEE, MMM d"
            return SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                timeZone = tz
            }.format(date)
        }
        val cal = Calendar.getInstance(tz).apply { time = date }
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val weekday = if (shortForm) {
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "周日"
                Calendar.MONDAY -> "周一"
                Calendar.TUESDAY -> "周二"
                Calendar.WEDNESDAY -> "周三"
                Calendar.THURSDAY -> "周四"
                Calendar.FRIDAY -> "周五"
                Calendar.SATURDAY -> "周六"
                else -> ""
            }
        } else {
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "星期日"
                Calendar.MONDAY -> "星期一"
                Calendar.TUESDAY -> "星期二"
                Calendar.WEDNESDAY -> "星期三"
                Calendar.THURSDAY -> "星期四"
                Calendar.FRIDAY -> "星期五"
                Calendar.SATURDAY -> "星期六"
                else -> ""
            }
        }
        return "${month}月${day}日 $weekday".trim()
    }

    fun formatOpacityModeLabel(alpha: Float, showBorder: Boolean): String {
        val pct = (alpha.coerceIn(0f, 1f) * 100f).roundToInt()
        return when {
            pct == 0 && showBorder -> "透明描边 · Transparent (0%)"
            pct == 0 -> "全透无框 · Transparent (0%)"
            pct == 100 -> "纯色实心 · Solid (100%)"
            pct in 64..72 -> "半透明 · Semi-transparent ($pct%)"
            else -> "自定义半透 ($pct%)"
        }
    }

    fun formatOpacityShortTag(alpha: Float, showBorder: Boolean): String {
        val pct = (alpha.coerceIn(0f, 1f) * 100f).roundToInt()
        return when {
            pct == 0 && showBorder -> "透明描边 0%"
            pct == 0 -> "全透明 0%"
            pct == 100 -> "实心 100%"
            else -> "不透明度 $pct%"
        }
    }

    private fun resolveSafeTimeZone(timezoneId: String?): TimeZone {
        if (timezoneId.isNullOrBlank()) return TimeZone.getDefault()
        val parsed = TimeZone.getTimeZone(timezoneId)
        return if (
            parsed.id == "GMT" &&
            !timezoneId.equals("GMT", ignoreCase = true) &&
            !timezoneId.equals("UTC", ignoreCase = true)
        ) {
            TimeZone.getDefault()
        } else {
            parsed
        }
    }

    fun formatTemp(tempC: Double, useFahrenheit: Boolean, includeDegree: Boolean = true): String {
        val value = if (useFahrenheit) {
            (tempC * 9.0 / 5.0 + 32.0).roundToInt()
        } else {
            tempC.roundToInt()
        }
        return if (includeDegree) "$value°" else "$value"
    }

    fun formatTempWithUnit(tempC: Double, useFahrenheit: Boolean): String {
        val value = if (useFahrenheit) {
            (tempC * 9.0 / 5.0 + 32.0).roundToInt()
        } else {
            tempC.roundToInt()
        }
        return if (useFahrenheit) "$value°F" else "$value°C"
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
