package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Desk
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.BoardingHouse
import com.example.data.ChinhoyiGeo
import com.example.data.GenderPolicy
import com.example.data.HousePrivateLocation
import com.example.data.HouseReview
import com.example.data.LocationHelper
import com.example.data.PlatformConfig
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.ui.components.ChinhoyiMapCanvas
import com.example.ui.components.HouseImage
import com.example.ui.components.HouseReviewsSection
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralSoft
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoSoft
import com.example.ui.theme.MapDrawerBlue
import com.example.ui.theme.TealBrand
import kotlinx.coroutines.launch

/**
 * Map & Live Location Page copied & beautified from Template #4:
 * - Royal Blue left Navigation Drawer (#0D5BFF) with avatar, name, email, menu items & Sign Out
 * - Pale interactive Chinhoyi Map Canvas with search bar, GPS recenter & category filter pills
 * - Bottom House Location Detail Card with GPS coordinates, distance, photos, blue amenities & reviews
 */
@Composable
fun MapScreen(
    housesState: UiState<List<BoardingHouse>>,
    userProfile: UserProfile?,
    config: PlatformConfig,
    unlockedHouseIds: Set<String>,
    privateLocations: Map<String, HousePrivateLocation>,
    photoCache: Map<String, String>,
    activeHouseReviews: List<HouseReview>,
    initialSelectedHouseId: String?,
    isAdmin: Boolean,
    onInspectHouse: (BoardingHouse) -> Unit,
    onUnlockHouseClick: (BoardingHouse) -> Unit,
    onChatWithLandlord: (BoardingHouse) -> Unit,
    onAddReview: (houseId: String, rating: Long, comment: String) -> Unit,
    onNavigateToLandlord: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedHouse by remember { mutableStateOf<BoardingHouse?>(null) }
    var userGps by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scope.launch {
                userGps = LocationHelper.fetchCurrentCoordinates(context)
            }
        }
    }

    val houses = (housesState as? UiState.Success)?.data ?: emptyList()

    LaunchedEffect(initialSelectedHouseId, houses) {
        if (!initialSelectedHouseId.isNullOrBlank()) {
            val found = houses.firstOrNull { it.id == initialSelectedHouseId }
            if (found != null) {
                selectedHouse = found
                onInspectHouse(found)
            }
        } else if (selectedHouse == null && houses.isNotEmpty()) {
            selectedHouse = houses.first()
            onInspectHouse(houses.first())
        }
    }

    val filteredHouses = remember(houses, searchQuery, selectedFilter, unlockedHouseIds) {
        houses.filter { h ->
            val matchesQ = searchQuery.isBlank() ||
                h.name.contains(searchQuery, ignoreCase = true) ||
                h.suburb.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Near CUT" -> h.distanceToCutKm <= 1.2
                "Girls Only" -> h.genderPolicy == GenderPolicy.FEMALE_ONLY.name
                "Boys Only" -> h.genderPolicy == GenderPolicy.MALE_ONLY.name
                "Unlocked" -> unlockedHouseIds.contains(h.id) || !config.requireFeeForLocation
                else -> true
            }
            matchesQ && matchesFilter
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MapDrawerBlue,
                drawerContentColor = Color.White,
                modifier = Modifier.width(280.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                        // User Avatar + Name + Email (Template #4 Blue Drawer)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (userProfile?.displayName?.firstOrNull() ?: 'C').uppercase(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    text = userProfile?.displayName?.ifBlank { "Chinhoyi User" } ?: "Chinhoyi User",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = userProfile?.email ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                        DrawerMenuItem(Icons.Filled.MyLocation, "All Chinhoyi Houses") {
                            selectedFilter = "All"
                            scope.launch { drawerState.close() }
                        }
                        DrawerMenuItem(Icons.Filled.LockOpen, "My Unlocked Pins") {
                            selectedFilter = "Unlocked"
                            scope.launch { drawerState.close() }
                        }
                        DrawerMenuItem(Icons.Filled.Apartment, "Landlord Portal") {
                            scope.launch { drawerState.close() }
                            onNavigateToLandlord()
                        }
                        if (isAdmin) {
                            DrawerMenuItem(Icons.Filled.AdminPanelSettings, "Admin Console") {
                                scope.launch { drawerState.close() }
                                onNavigateToAdmin()
                            }
                        }
                        DrawerMenuItem(Icons.Filled.Payments, "My Payments") {
                            scope.launch { drawerState.close() }
                            onNavigateToPayments()
                        }
                        DrawerMenuItem(Icons.Filled.Person, "Profile & Settings") {
                            scope.launch { drawerState.close() }
                            onNavigateToProfile()
                        }
                    }

                    DrawerMenuItem(Icons.AutoMirrored.Filled.ExitToApp, "Signout") {
                        scope.launch { drawerState.close() }
                        onSignOut()
                    }
                }
            }
        }
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            // Live Pale Map Canvas
            ChinhoyiMapCanvas(
                houses = filteredHouses,
                selectedHouseId = selectedHouse?.id,
                unlockedHouseIds = if (!config.requireFeeForLocation) houses.map { it.id }.toSet() else unlockedHouseIds,
                privateLocations = privateLocations,
                userLocation = userGps,
                onSelectHouse = { house ->
                    selectedHouse = house
                    onInspectHouse(house)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Floating Top Bar (Menu button + Search Location pill + GPS button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open Map Drawer", tint = Color(0xFF1F2937))
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Location in Chinhoyi...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(26.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .shadow(6.dp, RoundedCornerShape(26.dp))
                )

                FloatingActionButton(
                    onClick = {
                        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (fine || coarse) {
                            scope.launch { userGps = LocationHelper.fetchCurrentCoordinates(context) }
                        } else {
                            locationLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                            )
                        }
                    },
                    containerColor = Color.White,
                    contentColor = MapDrawerBlue,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Use My GPS Location")
                }
            }

            // Category Filter Pills floating above bottom sheet
            val currentHouse = selectedHouse
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Near CUT", "Girls Only", "Boys Only", "Unlocked").forEach { filter ->
                        val active = selectedFilter == filter
                        Surface(
                            color = if (active) MapDrawerBlue else Color.White,
                            shape = RoundedCornerShape(14.dp),
                            shadowElevation = 4.dp,
                            modifier = Modifier.clickable { selectedFilter = filter }
                        ) {
                            Text(
                                text = filter,
                                color = if (active) Color.White else Color(0xFF334155),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                            )
                        }
                    }
                }

                // Bottom House Location Detail Card (Template #4 Right Screen)
                if (currentHouse != null) {
                    val isUnlocked = unlockedHouseIds.contains(currentHouse.id) || !config.requireFeeForLocation || isAdmin
                    val privateLoc = privateLocations[currentHouse.id]
                    val distFromUser = userGps?.let { (uLat, uLng) ->
                        val tLat = if (isUnlocked && privateLoc != null) privateLoc.exactLat else currentHouse.approxLat
                        val tLng = if (isUnlocked && privateLoc != null) privateLoc.exactLng else currentHouse.approxLng
                        ChinhoyiGeo.distanceKm(uLat, uLng, tLat, tLng)
                    } ?: currentHouse.distanceToCutKm

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        shadowElevation = 16.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Drag handle + Close
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.width(32.dp))
                                Box(
                                    modifier = Modifier
                                        .width(42.dp)
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD1D5DB))
                                )
                                IconButton(
                                    onClick = { selectedHouse = null },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Minimize sheet", modifier = Modifier.size(18.dp))
                                }
                            }

                            // Title + Vibrant Blue Turn-Right Directions / Unlock Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentHouse.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${currentHouse.suburb} • $${currentHouse.monthlyRentUsd.toInt()}/mo • ${currentHouse.vacantRooms} vacant",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = if (isUnlocked) MapDrawerBlue else IndigoPrimary,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable {
                                            if (isUnlocked && privateLoc != null) {
                                                val uri = Uri.parse(
                                                    "geo:${privateLoc.exactLat},${privateLoc.exactLng}?q=${privateLoc.exactLat},${privateLoc.exactLng}(${Uri.encode(currentHouse.name)})"
                                                )
                                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                                runCatching { context.startActivity(intent) }
                                            } else {
                                                onUnlockHouseClick(currentHouse)
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isUnlocked) Icons.Filled.Directions else Icons.Filled.LockOpen,
                                            contentDescription = if (isUnlocked) "Open Directions" else "Unlock Location",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }

                            // GPS Location & Distance row (Template #4)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(if (isUnlocked) EmeraldSoft else CoralSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isUnlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                                            contentDescription = null,
                                            tint = if (isUnlocked) EmeraldAccent else Color(0xFFDC2626),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text("GPS Location", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text(
                                            text = if (isUnlocked && privateLoc != null) {
                                                "${String.format("%.4f", privateLoc.exactLat)}, ${String.format("%.4f", privateLoc.exactLng)}"
                                            } else {
                                                "Locked ($${config.unlockFeeUsd.toInt()} EcoCash)"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(IndigoSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Directions, contentDescription = null, tint = MapDrawerBlue, modifier = Modifier.size(16.dp))
                                    }
                                    Column {
                                        Text("Distance", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text(
                                            text = "$distFromUser km",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            if (isUnlocked && privateLoc != null) {
                                Text(
                                    text = "Address: ${privateLoc.streetAddress} (${privateLoc.landmarks})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF065F46),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Photo thumbnails strip (FREE)
                            val refs = currentHouse.photoRefs.ifEmpty { listOf(currentHouse.coverPhotoRef) }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(refs) { ref ->
                                    HouseImage(
                                        photoRef = ref,
                                        photoCache = photoCache,
                                        contentDescription = currentHouse.name,
                                        modifier = Modifier
                                            .size(74.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                    )
                                }
                            }

                            // Amenities blue icon strip (Template #4)
                            Text("Amenities", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                AmenityIconBadge(Icons.Filled.Wifi, "Wi-Fi", currentHouse.amenities.any { it.contains("Wi-Fi", true) })
                                AmenityIconBadge(Icons.Filled.Bolt, "Solar", currentHouse.amenities.any { it.contains("Solar", true) })
                                AmenityIconBadge(Icons.Filled.WaterDrop, "Borehole", currentHouse.amenities.any { it.contains("Borehole", true) })
                                AmenityIconBadge(Icons.Filled.Security, "Security", currentHouse.amenities.any { it.contains("Security", true) })
                                AmenityIconBadge(Icons.Filled.Desk, "Study Desk", currentHouse.amenities.any { it.contains("Desk", true) })
                            }

                            // Ratings & Verified Reviews
                            Text("Ratings & Student Reviews", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            HouseReviewsSection(
                                house = currentHouse,
                                reviews = activeHouseReviews,
                                canReview = isAdmin || unlockedHouseIds.contains(currentHouse.id),
                                onSubmitReview = { rating, comment -> onAddReview(currentHouse.id, rating, comment) },
                                maxVisibleReviews = 3
                            )

                            Button(
                                onClick = {
                                    if (isUnlocked) onChatWithLandlord(currentHouse)
                                    else onUnlockHouseClick(currentHouse)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isUnlocked) TealBrand else MapDrawerBlue),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (isUnlocked) Icons.Filled.Chat else Icons.Filled.LockOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isUnlocked) "Chat with Landlord" else "Pay EcoCash ($${String.format("%.2f", config.unlockFeeUsd)}) to Unlock Location & Chat",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AmenityIconBadge(icon: ImageVector, label: String, available: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (available) IndigoSoft else Color(0xFFF3F4F6)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (available) MapDrawerBlue else Color.LightGray,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (available) Color(0xFF334155) else Color.Gray
        )
    }
}
