package com.example.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class SuburbAnchor(
    val name: String,
    val lat: Double,
    val lng: Double,
    val distanceToCutKm: Double
)

object ChinhoyiGeo {
    // Chinhoyi University of Technology (CUT) Main Campus coordinates
    const val CUT_LAT = -17.3502
    const val CUT_LNG = 30.2035

    val suburbs = listOf(
        SuburbAnchor("Cold Stream", -17.3548, 30.1972, 0.8),
        SuburbAnchor("Orange Grove", -17.3455, 30.2118, 1.0),
        SuburbAnchor("Town Centre", -17.3622, 30.1978, 1.5),
        SuburbAnchor("Gadzema", -17.3665, 30.1890, 2.3),
        SuburbAnchor("Ruvimbo", -17.3390, 30.1935, 1.6),
        SuburbAnchor("Chikonohono", -17.3720, 30.2065, 2.4),
        SuburbAnchor("Mzari", -17.3418, 30.2215, 2.1),
        SuburbAnchor("Mapako", -17.3585, 30.2145, 1.4)
    )

    val availableAmenities = listOf(
        "Wi-Fi",
        "Solar Backup",
        "Borehole Water",
        "Security Wall",
        "24/7 Guard",
        "Study Desk",
        "Fitted Kitchen",
        "Laundry Area",
        "Hot Shower",
        "Meals Optional"
    )

    fun anchorForSuburb(suburb: String): SuburbAnchor {
        return suburbs.firstOrNull { it.name.equals(suburb, ignoreCase = true) } ?: suburbs.first()
    }

    fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return ((r * c) * 10.0).toInt() / 10.0
    }
}
