package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BoardingHouse
import com.example.data.ChinhoyiGeo
import com.example.data.HousePrivateLocation
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MapCanvasBg
import com.example.ui.theme.MapDrawerBlue
import com.example.ui.theme.MapParkColor
import com.example.ui.theme.MapRoadColor
import com.example.ui.theme.MapWaterColor
import kotlin.math.roundToInt

/**
 * Interactive pale-slate Chinhoyi Map Canvas styled after Template #4 (Map + Location Pins + Bottom Sheet).
 * Projects real Chinhoyi GPS coordinates (Cold Stream, Orange Grove, CUT, Town Centre, Gadzema, Ruvimbo)
 * and shows pulsing user GPS dot + interactive teardrop house pins.
 */
@Composable
fun ChinhoyiMapCanvas(
    houses: List<BoardingHouse>,
    selectedHouseId: String?,
    unlockedHouseIds: Set<String>,
    privateLocations: Map<String, HousePrivateLocation>,
    userLocation: Pair<Double, Double>?,
    onSelectHouse: (BoardingHouse) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val pulseTransition = rememberInfiniteTransition(label = "gps_pulse")
    val pulseRadius by pulseTransition.animateFloat(
        initialValue = 10f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "pulse"
    )

    // Chinhoyi bounding box centered around CUT (-17.3502, 30.2035)
    val minLat = -17.380
    val maxLat = -17.330
    val minLng = 30.180
    val maxLng = 30.230

    BoxWithConstraints(
        modifier = modifier
            .background(MapCanvasBg)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(0.85f, 2.8f)
                    scale = newScale
                    panOffset = Offset(
                        x = (panOffset.x + pan.x).coerceIn(-450f, 450f),
                        y = (panOffset.y + pan.y).coerceIn(-550f, 550f)
                    )
                }
            }
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        fun project(lat: Double, lng: Double): Offset {
            val normX = ((lng - minLng) / (maxLng - minLng)).toFloat().coerceIn(0.05f, 0.95f)
            val normY = (1f - ((lat - minLat) / (maxLat - minLat)).toFloat()).coerceIn(0.06f, 0.94f)
            val cx = widthPx / 2f
            val cy = heightPx / 2f
            return Offset(
                x = cx + (normX * widthPx - cx) * scale + panOffset.x,
                y = cy + (normY * heightPx - cy) * scale + panOffset.y
            )
        }

        Canvas(modifier = Modifier.matchParentSize()) {
            // Urban green zones (Chinhoyi Golf / Parks)
            val parkPos = project(-17.347, 30.200)
            drawCircle(color = MapParkColor, radius = 95f * scale, center = parkPos)
            val park2 = project(-17.361, 30.214)
            drawCircle(color = MapParkColor, radius = 70f * scale, center = park2)

            // Manyame River curve
            val riverPath = Path().apply {
                val r1 = project(-17.335, 30.222)
                val r2 = project(-17.355, 30.220)
                val r3 = project(-17.375, 30.215)
                moveTo(r1.x, r1.y)
                quadraticTo(r2.x, r2.y, r3.x, r3.y)
            }
            drawPath(riverPath, color = MapWaterColor, style = Stroke(width = 22f * scale, cap = StrokeCap.Round))

            // Major roads & Harare-Chirundu A1 Highway
            val roadGrid = listOf(
                project(-17.334, 30.185) to project(-17.376, 30.218), // A1 Chirundu-Harare Hwy
                project(-17.350, 30.182) to project(-17.350, 30.226), // CUT Campus Ave
                project(-17.362, 30.182) to project(-17.362, 30.226), // Magamba Way / CBD
                project(-17.340, 30.194) to project(-17.374, 30.194), // Cold Stream Rd
                project(-17.338, 30.210) to project(-17.370, 30.210), // Orange Grove Dr
                project(-17.345, 30.185) to project(-17.345, 30.222),
                project(-17.356, 30.186) to project(-17.356, 30.216)
            )
            roadGrid.forEachIndexed { idx, (start, end) ->
                drawLine(
                    color = if (idx == 0) Color(0xFFFFFFFF) else MapRoadColor,
                    start = start,
                    end = end,
                    strokeWidth = (if (idx <= 2) 14f else 7f) * scale,
                    cap = StrokeCap.Round
                )
            }

            // Secondary neighborhood grid lines
            for (step in 1..7) {
                val frac = step / 8f
                val lat = minLat + (maxLat - minLat) * frac
                drawLine(
                    color = MapRoadColor.copy(alpha = 0.55f),
                    start = project(lat, minLng),
                    end = project(lat, maxLng),
                    strokeWidth = 3f * scale
                )
            }

            // Pulsing user GPS dot
            val userGeo = userLocation ?: (ChinhoyiGeo.CUT_LAT - 0.004 to ChinhoyiGeo.CUT_LNG + 0.001)
            val userPos = project(userGeo.first, userGeo.second)
            drawCircle(color = MapDrawerBlue.copy(alpha = 0.18f), radius = pulseRadius * scale, center = userPos)
            drawCircle(color = Color.White, radius = 9.5f * scale, center = userPos)
            drawCircle(color = MapDrawerBlue, radius = 6.5f * scale, center = userPos)
        }

        // Suburb Labels
        ChinhoyiGeo.suburbs.forEach { anchor ->
            val p = project(anchor.lat, anchor.lng)
            Text(
                text = anchor.name.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B),
                letterSpacing = 0.8.sp,
                modifier = Modifier.offset {
                    IntOffset((p.x - 45).roundToInt(), (p.y + 18).roundToInt())
                }
            )
        }

        // CUT Main Campus landmark badge
        val cutPos = project(ChinhoyiGeo.CUT_LAT, ChinhoyiGeo.CUT_LNG)
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 4.dp,
            modifier = Modifier.offset {
                IntOffset((cutPos.x - 42).roundToInt(), (cutPos.y - 16).roundToInt())
            }
        ) {
            Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.School, contentDescription = "CUT Campus", tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                    Text("CUT Campus", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)
                }
            }
        }

        // Boarding House Pins (Exact GPS if unlocked, approximate suburb pin if locked)
        houses.forEach { house ->
            val isUnlocked = unlockedHouseIds.contains(house.id)
            val privateLoc = privateLocations[house.id]
            val pinLat = if (isUnlocked && privateLoc != null) privateLoc.exactLat else house.approxLat
            val pinLng = if (isUnlocked && privateLoc != null) privateLoc.exactLng else house.approxLng
            val pos = project(pinLat, pinLng)
            val isSelected = house.id == selectedHouseId

            val pinColor = when {
                isSelected -> MapDrawerBlue
                isUnlocked -> EmeraldAccent
                house.vacantRooms <= 0L -> CoralDanger
                house.featured -> AmberWarning
                else -> IndigoPrimary
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset {
                        IntOffset((pos.x - 24.dp.toPx()).roundToInt(), (pos.y - 44.dp.toPx()).roundToInt())
                    }
                    .clickable { onSelectHouse(house) }
            ) {
                Surface(
                    color = pinColor,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = if (isSelected) 8.dp else 4.dp
                ) {
                    Box(modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)) {
                        Text(
                            text = "$${house.monthlyRentUsd.toInt()}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 34.dp else 28.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(pinColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUnlocked) Icons.Filled.LockOpen else if (isSelected) Icons.Filled.Home else Icons.Filled.Lock,
                        contentDescription = house.name,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
