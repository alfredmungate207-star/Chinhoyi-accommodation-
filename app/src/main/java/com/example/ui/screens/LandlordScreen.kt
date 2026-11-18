package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.BoardingHouse
import com.example.data.ChinhoyiGeo
import com.example.data.GenderPolicy
import com.example.data.HousePrivateLocation
import com.example.data.HouseStatus
import com.example.data.HouseUnlock
import com.example.data.LocationHelper
import com.example.data.PhotoCompressionHelper
import com.example.data.PlatformConfig
import com.example.data.RoomType
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.UserRole
import com.example.ui.components.HouseImage
import com.example.ui.theme.AmberSoft
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.CoralSoft
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.IndigoDeep
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoSoft
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.TealBrand
import kotlinx.coroutines.launch

@Composable
fun LandlordScreen(
    userProfile: UserProfile?,
    isAdmin: Boolean,
    config: PlatformConfig,
    landlordHousesState: UiState<List<BoardingHouse>>,
    landlordUnlocks: List<HouseUnlock>,
    photoCache: Map<String, String>,
    privateLocations: Map<String, HousePrivateLocation>,
    isBusy: Boolean,
    onInspectHouse: (BoardingHouse) -> Unit,
    onRequestLandlordUpgrade: () -> Unit,
    onSubmitHouse: (
        name: String,
        suburb: String,
        description: String,
        monthlyRentUsd: Double,
        depositUsd: Double,
        totalRooms: Long,
        vacantRooms: Long,
        genderPolicy: GenderPolicy,
        roomType: RoomType,
        amenities: List<String>,
        exactLat: Double,
        exactLng: Double,
        streetAddress: String,
        landmarks: String,
        contactPhone: String,
        uploadedPhotoDataUris: List<String>,
        presetCoverRef: String
    ) -> Unit,
    onUpdateVacancy: (BoardingHouse, Long) -> Unit,
    onUpdateLocation: (
        houseId: String,
        exactLat: Double,
        exactLng: Double,
        streetAddress: String,
        landmarks: String,
        contactPhone: String
    ) -> Unit,
    onMessageUnlockedStudent: (HouseUnlock) -> Unit,
    modifier: Modifier = Modifier
) {
    val role = UserRole.fromRaw(userProfile?.role)
    val canAccessLandlord = isAdmin || role == UserRole.LANDLORD

    if (!canAccessLandlord) {
        LandlordAccessGate(onRequestLandlordUpgrade = onRequestLandlordUpgrade, modifier = modifier)
        return
    }

    var showAddSheet by remember { mutableStateOf(false) }
    var locationEditHouse by remember { mutableStateOf<BoardingHouse?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // MatDash-style Gradient Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(TealBrand, IndigoPrimary)))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Landlord Studio",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        if (userProfile?.landlordApproved == true || isAdmin) {
                            Icon(Icons.Filled.Verified, contentDescription = "Verified Landlord", tint = EmeraldSoft, modifier = Modifier.size(20.dp))
                        }
                    }
                    Text(
                        text = "Post real photos, catalogue, live GPS & update room vacancies anytime",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Button(
                    onClick = { showAddSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = IndigoDeep),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("landlord_add_house_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Register House", fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Stats summary row
            item {
                val myHouses = (landlordHousesState as? UiState.Success)?.data ?: emptyList()
                val totalVacant = myHouses.sumOf { it.vacantRooms }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatMiniCard("My Houses", myHouses.size.toString(), IndigoSoft, IndigoDeep, Modifier.weight(1f))
                    StatMiniCard("Open Rooms", totalVacant.toString(), EmeraldSoft, Color(0xFF065F46), Modifier.weight(1f))
                    StatMiniCard("Unlocked Leads", landlordUnlocks.size.toString(), AmberSoft, Color(0xFF92400E), Modifier.weight(1f))
                }
            }

            // My Boarding Houses
            item {
                Text("My Registered Boarding Houses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            when (landlordHousesState) {
                is UiState.Loading -> item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> item {
                    Text(landlordHousesState.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    if (landlordHousesState.data.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, SlateBorder),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Apartment, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(36.dp))
                                    Text("No Boarding Houses Registered Yet", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Tap 'Register House' above to upload real photos, amenities, room counts, and live GPS for Admin admission.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(landlordHousesState.data, key = { it.id }) { house ->
                            LandlordHouseManageCard(
                                house = house,
                                photoCache = photoCache,
                                onToggleVacancy = { makeVacant ->
                                    val delta = if (makeVacant) {
                                        if (house.vacantRooms <= 0L) 1L else 0L
                                    } else {
                                        -house.vacantRooms
                                    }
                                    if (delta != 0L) onUpdateVacancy(house, delta)
                                },
                                onAdjustVacancy = { delta -> onUpdateVacancy(house, delta) },
                                onEditLocation = {
                                    onInspectHouse(house)
                                    locationEditHouse = house
                                }
                            )
                        }
                    }
                }
            }

            // Unlocked Student Leads
            if (landlordUnlocks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Paid & Unlocked Student Inquiries", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(landlordUnlocks, key = { it.id }) { unlock ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, SlateBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(unlock.payerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Unlocked ${unlock.houseName}", style = MaterialTheme.typography.bodySmall, color = TealBrand)
                            }
                            Button(
                                onClick = { onMessageUnlockedStudent(unlock) },
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chat")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        RegisterBoardingHouseSheet(
            config = config,
            defaultPhone = userProfile?.phone ?: "",
            isBusy = isBusy,
            onDismiss = { showAddSheet = false },
            onSubmit = { name, suburb, desc, rent, deposit, total, vacant, gender, room, amenities, lat, lng, addr, landmarks, phone, photos, preset ->
                onSubmitHouse(name, suburb, desc, rent, deposit, total, vacant, gender, room, amenities, lat, lng, addr, landmarks, phone, photos, preset)
                showAddSheet = false
            }
        )
    }

    val locHouse = locationEditHouse
    if (locHouse != null) {
        LaunchedEffect(locHouse.id) {
            onInspectHouse(locHouse)
        }
        UpdateLocationSheet(
            house = locHouse,
            existingPrivateLocation = privateLocations[locHouse.id],
            isBusy = isBusy,
            onDismiss = { locationEditHouse = null },
            onSave = { lat, lng, addr, landmarks, phone ->
                onUpdateLocation(locHouse.id, lat, lng, addr, landmarks, phone)
                locationEditHouse = null
            }
        )
    }
}

@Composable
private fun LandlordAccessGate(
    onRequestLandlordUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(CoralSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Lock, contentDescription = "Landlords Only", tint = IndigoDeep, modifier = Modifier.size(38.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Landlords Only Portal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Only verified boarding house owners can post property photos, set live GPS pins, and update room vacancies. Switch your account to Landlord mode to register your boarding house for Admin admission.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onRequestLandlordUpgrade,
            colors = ButtonDefaults.buttonColors(containerColor = TealBrand),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Switch to Landlord Account & Register Property", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LandlordHouseManageCard(
    house: BoardingHouse,
    photoCache: Map<String, String>,
    onToggleVacancy: (Boolean) -> Unit,
    onAdjustVacancy: (Long) -> Unit,
    onEditLocation: () -> Unit
) {
    val status = HouseStatus.fromRaw(house.status)
    val (badgeBg, badgeFg) = when (status) {
        HouseStatus.APPROVED -> EmeraldSoft to Color(0xFF065F46)
        HouseStatus.PENDING -> AmberSoft to Color(0xFF92400E)
        HouseStatus.SUSPENDED -> CoralSoft to Color(0xFF991B1B)
    }
    val hasVacancy = house.vacantRooms > 0L

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                HouseImage(
                    photoRef = house.coverPhotoRef,
                    photoCache = photoCache,
                    contentDescription = house.name,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(14.dp))
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = house.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        )
                        Surface(color = badgeBg, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = status.label,
                                color = badgeFg,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = "${house.suburb} • $${house.monthlyRentUsd.toInt()}/mo • ${house.photoRefs.size.coerceAtLeast(1)} photo(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Real-Time Vacancy Toggle & Room Counter
            Surface(
                color = if (hasVacancy) EmeraldSoft.copy(alpha = 0.5f) else CoralSoft.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, if (hasVacancy) EmeraldAccent.copy(alpha = 0.35f) else CoralDanger.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (hasVacancy) "Vacancy Status: VACANT" else "Vacancy Status: FULL",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (hasVacancy) Color(0xFF065F46) else Color(0xFF991B1B),
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (hasVacancy) "${house.vacantRooms} of ${house.totalRooms} rooms available now" else "Toggle ON when a room opens up",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF334155)
                            )
                        }
                        Switch(
                            checked = hasVacancy,
                            onCheckedChange = onToggleVacancy,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldAccent,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = CoralDanger.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.testTag("landlord_vacancy_toggle_${house.id}")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Exact Vacant Room Count", style = MaterialTheme.typography.labelMedium, color = IndigoDeep, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = Color.White,
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                IconButton(
                                    onClick = { onAdjustVacancy(-1L) },
                                    enabled = house.vacantRooms > 0L
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease vacancy", tint = IndigoDeep)
                                }
                            }
                            Text("${house.vacantRooms}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            Surface(
                                color = IndigoPrimary,
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                IconButton(
                                    onClick = { onAdjustVacancy(1L) },
                                    enabled = house.vacantRooms < house.totalRooms
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase vacancy", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onEditLocation,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Update Live GPS & Street Address")
            }
        }
    }
}

@Composable
private fun StatMiniCard(label: String, value: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Surface(color = bg, shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = fg, fontWeight = FontWeight.ExtraBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = fg)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RegisterBoardingHouseSheet(
    config: PlatformConfig,
    defaultPhone: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        name: String,
        suburb: String,
        description: String,
        monthlyRentUsd: Double,
        depositUsd: Double,
        totalRooms: Long,
        vacantRooms: Long,
        genderPolicy: GenderPolicy,
        roomType: RoomType,
        amenities: List<String>,
        exactLat: Double,
        exactLng: Double,
        streetAddress: String,
        landmarks: String,
        contactPhone: String,
        uploadedPhotoDataUris: List<String>,
        presetCoverRef: String
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var suburb by remember { mutableStateOf(ChinhoyiGeo.suburbs.first().name) }
    var description by remember { mutableStateOf("") }
    var rentText by remember { mutableStateOf("80") }
    var depositText by remember { mutableStateOf("80") }
    var totalRoomsText by remember { mutableStateOf("10") }
    var vacantRoomsText by remember { mutableStateOf("4") }
    var genderPolicy by remember { mutableStateOf(GenderPolicy.MIXED) }
    var roomType by remember { mutableStateOf(RoomType.SHARED_2) }
    val selectedAmenities = remember { mutableStateListOf("Wi-Fi", "Solar Backup", "Borehole Water") }
    var streetAddress by remember { mutableStateOf("") }
    var landmarks by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf(defaultPhone) }
    var exactLat by remember { mutableDoubleStateOf(ChinhoyiGeo.anchorForSuburb(suburb).lat) }
    var exactLng by remember { mutableDoubleStateOf(ChinhoyiGeo.anchorForSuburb(suburb).lng) }
    var gpsCaptured by remember { mutableStateOf(false) }
    val pickedPhotos = remember { mutableStateListOf<String>() }

    val maxPhotos = config.maxPhotosPerHouse.toInt().coerceIn(1, 10)
    val multiPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxPhotos)
    ) { uris ->
        scope.launch {
            uris.take(maxPhotos - pickedPhotos.size).forEach { uri ->
                val dataUri = PhotoCompressionHelper.uriToCompressedDataUri(context, uri)
                if (dataUri != null) pickedPhotos.add(dataUri)
            }
        }
    }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            scope.launch {
                LocationHelper.fetchCurrentCoordinates(context)?.let { (lat, lng) ->
                    exactLat = lat
                    exactLng = lng
                    gpsCaptured = true
                }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Register Boarding House", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Upload real photos, full catalogue & live GPS location. Admin reviews admissions before advertising.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Real Photo Picker Upload
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("House Photos (${pickedPhotos.size}/$maxPhotos)", style = MaterialTheme.typography.labelLarge)
                OutlinedButton(
                    onClick = {
                        multiPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pick Real Photos")
                }
            }

            if (pickedPhotos.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(pickedPhotos) { dataUri ->
                        HouseImage(
                            photoRef = dataUri,
                            photoCache = emptyMap(),
                            contentDescription = "Uploaded house photo",
                            modifier = Modifier.size(84.dp).clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Boarding House Name") },
                placeholder = { Text("e.g. Emerald Palms Residence") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("landlord_house_name_input")
            )

            Text("Chinhoyi Suburb", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChinhoyiGeo.suburbs.forEach { anchor ->
                    FilterChip(
                        selected = suburb == anchor.name,
                        onClick = {
                            suburb = anchor.name
                            if (!gpsCaptured) {
                                exactLat = anchor.lat
                                exactLng = anchor.lng
                            }
                        },
                        label = { Text(anchor.name) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rentText,
                    onValueChange = { rentText = it },
                    label = { Text("Rent ($ USD/mo)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = depositText,
                    onValueChange = { depositText = it },
                    label = { Text("Deposit ($ USD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = totalRoomsText,
                    onValueChange = { totalRoomsText = it },
                    label = { Text("Total Rooms") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = vacantRoomsText,
                    onValueChange = { vacantRoomsText = it },
                    label = { Text("Vacant Rooms Now") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Gender & Room Policy", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GenderPolicy.entries.forEach { gp ->
                    FilterChip(
                        selected = genderPolicy == gp,
                        onClick = { genderPolicy = gp },
                        label = { Text(gp.label) }
                    )
                }
            }
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoomType.entries.forEach { rt ->
                    FilterChip(
                        selected = roomType == rt,
                        onClick = { roomType = rt },
                        label = { Text(rt.label) }
                    )
                }
            }

            Text("House Catalogue Amenities", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ChinhoyiGeo.availableAmenities.forEach { amenity ->
                    val selected = selectedAmenities.contains(amenity)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) selectedAmenities.remove(amenity) else selectedAmenities.add(amenity)
                        },
                        label = { Text(amenity) }
                    )
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Full House Catalogue & Rules") },
                placeholder = { Text("Describe beds, study desks, Wi-Fi speed, curfew, cooking, etc.") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Live GPS Capture
            Surface(color = IndigoSoft, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Exact Live House Location", fontWeight = FontWeight.Bold, color = IndigoDeep)
                            Text(
                                text = "GPS: ${String.format("%.4f", exactLat)}, ${String.format("%.4f", exactLng)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        OutlinedButton(onClick = {
                            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (fine || coarse) {
                                scope.launch {
                                    LocationHelper.fetchCurrentCoordinates(context)?.let { (lat, lng) ->
                                        exactLat = lat
                                        exactLng = lng
                                        gpsCaptured = true
                                    }
                                }
                            } else {
                                gpsPermissionLauncher.launch(
                                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                                )
                            }
                        }) {
                            Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (gpsCaptured) "GPS Captured" else "Capture GPS")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = streetAddress,
                onValueChange = { streetAddress = it },
                label = { Text("Exact Street / Stand Number (Hidden until Fee Paid)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = landmarks,
                onValueChange = { landmarks = it },
                label = { Text("Nearby Landmark") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = contactPhone,
                onValueChange = { contactPhone = it },
                label = { Text("Landlord Contact Phone") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val rent = rentText.toDoubleOrNull() ?: 80.0
                    val dep = depositText.toDoubleOrNull() ?: rent
                    val total = totalRoomsText.toLongOrNull() ?: 10L
                    val vacant = vacantRoomsText.toLongOrNull() ?: 1L
                    onSubmit(
                        name,
                        suburb,
                        description.ifBlank { "Clean Chinhoyi student boarding house in $suburb." },
                        rent,
                        dep,
                        total,
                        vacant,
                        genderPolicy,
                        roomType,
                        selectedAmenities.toList(),
                        exactLat,
                        exactLng,
                        streetAddress.ifBlank { "Stand 12, $suburb, Chinhoyi" },
                        landmarks.ifBlank { "Near $suburb main road" },
                        contactPhone,
                        pickedPhotos.toList(),
                        "drawable:img_boarding_exterior"
                    )
                },
                enabled = !isBusy && name.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_house_button")
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit Boarding House for Admission", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateLocationSheet(
    house: BoardingHouse,
    existingPrivateLocation: HousePrivateLocation?,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (lat: Double, lng: Double, streetAddress: String, landmarks: String, phone: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lat by remember(existingPrivateLocation) { mutableDoubleStateOf(existingPrivateLocation?.exactLat ?: house.approxLat) }
    var lng by remember(existingPrivateLocation) { mutableDoubleStateOf(existingPrivateLocation?.exactLng ?: house.approxLng) }
    var street by remember(existingPrivateLocation) { mutableStateOf(existingPrivateLocation?.streetAddress ?: "") }
    var landmarks by remember(existingPrivateLocation) { mutableStateOf(existingPrivateLocation?.landmarks ?: "") }
    var phone by remember(existingPrivateLocation) { mutableStateOf(existingPrivateLocation?.contactPhone ?: "") }

    val gpsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            scope.launch {
                LocationHelper.fetchCurrentCoordinates(context)?.let { (la, ln) ->
                    lat = la
                    lng = ln
                }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Update Live Location: ${house.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedButton(
                onClick = {
                    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (fine || coarse) {
                        scope.launch {
                            LocationHelper.fetchCurrentCoordinates(context)?.let { (la, ln) ->
                                lat = la
                                lng = ln
                            }
                        }
                    } else {
                        gpsLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Use Live Phone GPS (${String.format("%.4f", lat)}, ${String.format("%.4f", lng)})")
            }

            OutlinedTextField(
                value = street,
                onValueChange = { street = it },
                label = { Text("Street / Stand Number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = landmarks,
                onValueChange = { landmarks = it },
                label = { Text("Landmarks") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Landlord Phone") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onSave(lat, lng, street, landmarks, phone) },
                enabled = !isBusy && street.trim().length >= 3,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Save Exact Location", fontWeight = FontWeight.Bold)
            }
        }
    }
}
