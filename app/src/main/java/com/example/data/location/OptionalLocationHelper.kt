package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class ResolvedDeviceLocation(
    val cityName: String,
    val adminSubtitle: String,
    val latitude: Double,
    val longitude: Double
)

object OptionalLocationHelper {

    fun hasCoarseLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentCoarseLocation(context: Context): ResolvedDeviceLocation? =
        withContext(Dispatchers.IO) {
            if (!hasCoarseLocationPermission(context)) return@withContext null

            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            val freshLocation = withTimeoutOrNull(7000L) {
                suspendCancellableCoroutine { cont ->
                    client.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cts.token
                    ).addOnSuccessListener { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }.addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
                    cont.invokeOnCancellation { cts.cancel() }
                }
            }

            val finalLocation = freshLocation ?: withTimeoutOrNull(3000L) {
                suspendCancellableCoroutine { cont ->
                    client.lastLocation
                        .addOnSuccessListener { loc ->
                            if (cont.isActive) cont.resume(loc)
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume(null)
                        }
                }
            } ?: return@withContext null

            val lat = finalLocation.latitude
            val lon = finalLocation.longitude
            val (city, subtitle) = reverseGeocodeName(context, lat, lon)
            ResolvedDeviceLocation(
                cityName = city,
                adminSubtitle = subtitle,
                latitude = lat,
                longitude = lon
            )
        }

    private suspend fun reverseGeocodeName(
        context: Context,
        latitude: Double,
        longitude: Double
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val fallbackName = "当前位置"
        val fallbackSub = "${"%.2f".format(Locale.US, latitude)}°, ${"%.2f".format(Locale.US, longitude)}°"
        if (!Geocoder.isPresent()) return@withContext fallbackName to fallbackSub

        try {
            val geocoder = Geocoder(context, Locale.CHINA)
            val address: Address? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withTimeoutOrNull(3500L) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(latitude, longitude, 1) { list ->
                            if (cont.isActive) cont.resume(list.firstOrNull())
                        }
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }

            if (address != null) {
                val city = address.locality
                    ?: address.subAdminArea
                    ?: address.adminArea
                    ?: fallbackName
                val region = listOfNotNull(address.adminArea, address.countryName)
                    .filter { it != city }
                    .distinct()
                    .joinToString(" · ")
                    .ifEmpty { fallbackSub }
                city to region
            } else {
                fallbackName to fallbackSub
            }
        } catch (_: Exception) {
            fallbackName to fallbackSub
        }
    }
}
