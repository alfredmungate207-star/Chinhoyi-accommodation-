package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BoardingHouse
import com.example.data.ChinhoyiGeo
import com.example.data.GenderPolicy
import com.example.data.HousePrivateLocation
import com.example.data.HouseReview
import com.example.data.PaymentRequest
import com.example.data.PaymentStatus
import com.example.data.PlatformConfig
import com.example.data.RoomType
import com.example.data.UiState
import com.example.ui.components.HouseImage
import com.example.ui.components.HouseReviewsSection
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

@Composable
fun HomeScreen(
    housesState: UiState<List<BoardingHouse>>,
    config: PlatformConfig,
    unlockedHouseIds: Set<String>,
    customerPayments: List<PaymentRequest>,
    photoCache: Map<String, String>,
    privateLocations: Map<String, HousePrivateLocation>,
    activeHouseReviews: List<HouseReview>,
    isAdmin: Boolean,
    onInspectHouse: (BoardingHouse) -> Unit,
    onOpenMapForHouse: (BoardingHouse) -> Unit,
    onUnlockHouseClick: (BoardingHouse) -> Unit,
    onChatWithLandlord: (BoardingHouse) -> Unit,
    onAddReview: (houseId: String, rating: Long, comment: String) -> Unit,
    onSeedStarterHouses: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSuburb by remember { mutableStateOf("All") }
    var onlyVacant by remember { mutableStateOf(false) }
    var detailHouse by remember { mutableStateOf<BoardingHouse?>(null) }

    val suburbOptions = remember { listOf("All") + ChinhoyiGeo.suburbs.map { it.name } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Gradient Hero Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(IndigoDeep, IndigoPrimary))
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Chinhoyi Accommodations",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = config.welcomeBannerText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.88f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Unlock Fee", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                            Text(
                                text = "$${String.format("%.2f", config.unlockFeeUsd)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search boarding house, suburb, Wi-Fi, solar...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = IndigoPrimary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_input")
                )
            }
        }

        // Suburb & Vacancy Filter Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = onlyVacant,
                onClick = { onlyVacant = !onlyVacant },
                label = { Text("Vacant Only") },
                leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldSoft,
                    selectedLabelColor = Color(0xFF065F46)
                )
            )
            suburbOptions.forEach { suburb ->
                FilterChip(
                    selected = selectedSuburb == suburb,
                    onClick = { selectedSuburb = suburb },
                    label = { Text(suburb) }
                )
            }
        }

        when (housesState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = IndigoPrimary)
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = housesState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            is UiState.Success -> {
                val filtered = remember(housesState.data, searchQuery, selectedSuburb, onlyVacant) {
                    housesState.data.filter { house ->
                        val matchesQuery = searchQuery.isBlank() ||
                            house.name.contains(searchQuery, ignoreCase = true) ||
                            house.suburb.contains(searchQuery, ignoreCase = true) ||
                            house.amenities.any { it.contains(searchQuery, ignoreCase = true) }
                        val matchesSuburb = selectedSuburb == "All" || house.suburb.equals(selectedSuburb, ignoreCase = true)
                        val matchesVacant = !onlyVacant || house.vacantRooms > 0L
                        matchesQuery && matchesSuburb && matchesVacant
                    }
                }

                if (filtered.isEmpty()) {
                    EmptyHousesState(isAdmin = isAdmin, onSeedStarterHouses = onSeedStarterHouses)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filtered, key = { it.id }) { house ->
                            val isUnlocked = unlockedHouseIds.contains(house.id) || !config.requireFeeForLocation
                            val pendingPayment = customerPayments.firstOrNull {
                                it.houseId == house.id && it.status == PaymentStatus.PENDING.name
                            }
                            BoardingHouseCard(
                                house = house,
                                config = config,
                                isUnlocked = isUnlocked,
                                hasPendingPayment = pendingPayment != null,
                                photoCache = photoCache,
                                onCardClick = {
                                    onInspectHouse(house)
                                    detailHouse = house
                                },
                                onOpenMap = {
                                    onInspectHouse(house)
                                    onOpenMapForHouse(house)
                                },
                                onUnlockClick = { onUnlockHouseClick(house) },
                                onChatClick = { onChatWithLandlord(house) }
                            )
                        }
                    }
                }
            }
        }
    }

    val currentDetail = detailHouse
    if (currentDetail != null) {
        LaunchedEffect(currentDetail.id) {
            onInspectHouse(currentDetail)
        }
        val latestPayment = customerPayments.firstOrNull { it.houseId == currentDetail.id }
        HouseCatalogueDetailSheet(
            house = currentDetail,
            config = config,
            isUnlocked = unlockedHouseIds.contains(currentDetail.id) || !config.requireFeeForLocation,
            isChatUnlocked = unlockedHouseIds.contains(currentDetail.id) || !config.requireFeeForChat,
            canReview = isAdmin || unlockedHouseIds.contains(currentDetail.id),
            existingPayment = latestPayment,
            privateLocation = privateLocations[currentDetail.id],
            reviews = activeHouseReviews,
            photoCache = photoCache,
            onDismiss = { detailHouse = null },
            onOpenMap = {
                detailHouse = null
                onOpenMapForHouse(currentDetail)
            },
            onUnlockClick = {
                detailHouse = null
                onUnlockHouseClick(currentDetail)
            },
            onChatClick = {
                detailHouse = null
                onChatWithLandlord(currentDetail)
            },
            onAddReview = { rating, comment -> onAddReview(currentDetail.id, rating, comment) }
        )
    }
}

@Composable
private fun BoardingHouseCard(
    house: BoardingHouse,
    config: PlatformConfig,
    isUnlocked: Boolean,
    hasPendingPayment: Boolean,
    photoCache: Map<String, String>,
    onCardClick: () -> Unit,
    onOpenMap: () -> Unit,
    onUnlockClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("house_card_${house.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
                HouseImage(
                    photoRef = house.coverPhotoRef,
                    photoCache = photoCache,
                    contentDescription = house.name,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                            )
                        )
                )

                // Top badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (house.featured && config.enableFeaturedBoost) {
                            Surface(color = AmberWarning, shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    "FEATURED",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (config.enableLiveVacancy) {
                            val vacantColor = if (house.vacantRooms > 0) EmeraldAccent else CoralDanger
                            Surface(color = vacantColor, shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    text = if (house.vacantRooms > 0) "${house.vacantRooms} Vacant" else "Full",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        color = if (isUnlocked) EmeraldSoft else Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isUnlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                                contentDescription = null,
                                tint = if (isUnlocked) Color(0xFF065F46) else Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (isUnlocked) "Unlocked" else "Free Photos",
                                color = if (isUnlocked) Color(0xFF065F46) else Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Bottom price & suburb overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = house.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text(
                                text = "${house.suburb} • ${house.distanceToCutKm} km from CUT",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }
                    Surface(color = Color.White, shape = RoundedCornerShape(12.dp)) {
                        Text(
                            text = "$${house.monthlyRentUsd.toInt()}/mo",
                            color = IndigoDeep,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${GenderPolicy.fromRaw(house.genderPolicy).label} • ${RoomType.fromRaw(house.roomType).label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = IndigoPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${house.rating} (${house.reviewCount})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Amenities preview row
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    house.amenities.take(5).forEach { amenity ->
                        Surface(
                            color = IndigoSoft,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = amenity,
                                style = MaterialTheme.typography.labelSmall,
                                color = IndigoDeep,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenMap,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Map Pin", maxLines = 1)
                    }

                    if (isUnlocked) {
                        Button(
                            onClick = onChatClick,
                            colors = ButtonDefaults.buttonColors(containerColor = TealBrand),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chat Landlord", maxLines = 1)
                        }
                    } else if (hasPendingPayment) {
                        Surface(
                            onClick = onUnlockClick,
                            color = AmberSoft,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                Text(
                                    text = "Awaiting Admin Unlock",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF92400E),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onUnlockClick,
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("EcoCash $${config.unlockFeeUsd.toInt()}", maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HouseCatalogueDetailSheet(
    house: BoardingHouse,
    config: PlatformConfig,
    isUnlocked: Boolean,
    isChatUnlocked: Boolean,
    canReview: Boolean,
    existingPayment: PaymentRequest?,
    privateLocation: HousePrivateLocation?,
    reviews: List<HouseReview>,
    photoCache: Map<String, String>,
    onDismiss: () -> Unit,
    onOpenMap: () -> Unit,
    onUnlockClick: () -> Unit,
    onChatClick: () -> Unit,
    onAddReview: (rating: Long, comment: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(house.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Hosted by ${house.landlordName} • ${house.suburb}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(color = EmeraldSoft, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        text = "${house.vacantRooms}/${house.totalRooms} Vacant",
                        color = Color(0xFF065F46),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Free Photo Gallery
            Text("Boarding House Gallery (Free Preview)", style = MaterialTheme.typography.labelLarge, color = IndigoDeep)
            val refs = house.photoRefs.ifEmpty { listOf(house.coverPhotoRef) }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(refs) { ref ->
                    HouseImage(
                        photoRef = ref,
                        photoCache = photoCache,
                        contentDescription = house.name,
                        modifier = Modifier
                            .size(width = 180.dp, height = 125.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
            }

            Text(house.description, style = MaterialTheme.typography.bodyMedium)

            // Pricing & Policy summary
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(color = IndigoSoft, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Monthly Rent", style = MaterialTheme.typography.labelSmall, color = IndigoPrimary)
                        Text("$${house.monthlyRentUsd.toInt()} USD", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(color = IndigoSoft, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Deposit", style = MaterialTheme.typography.labelSmall, color = IndigoPrimary)
                        Text("$${house.depositUsd.toInt()} USD", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Exact Location Box (Locked vs Pending vs Unlocked)
            val isPendingVerification = !isUnlocked && existingPayment?.status == PaymentStatus.PENDING.name
            Surface(
                color = when {
                    isUnlocked -> EmeraldSoft
                    isPendingVerification -> AmberSoft
                    else -> CoralSoft.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isUnlocked) Icons.Filled.Verified else Icons.Filled.Lock,
                        contentDescription = null,
                        tint = when {
                            isUnlocked -> Color(0xFF065F46)
                            isPendingVerification -> Color(0xFF92400E)
                            else -> CoralDanger
                        }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        if (isUnlocked && privateLocation != null) {
                            Text(privateLocation.streetAddress, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                            Text(
                                text = "GPS: ${String.format("%.4f", privateLocation.exactLat)}, ${String.format("%.4f", privateLocation.exactLng)} • ${privateLocation.landmarks}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF065F46)
                            )
                        } else if (isUnlocked) {
                            Text("Location Unlocked", fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                            Text("Tap Map Pin below to view exact location.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF065F46))
                        } else if (isPendingVerification && existingPayment != null) {
                            Text("EcoCash Verification Pending", fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                            Text(
                                text = "Submitted by ${existingPayment.payerName} (Ref: ${existingPayment.referenceCode}). Admin will verify & unlock shortly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF78350F)
                            )
                        } else {
                            Text("Exact GPS Address Locked", fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                            Text(
                                text = "Pay $${String.format("%.2f", config.unlockFeeUsd)} via EcoCash to reveal street address, GPS directions & Landlord chat.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onOpenMap,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Map")
                }

                if (isChatUnlocked) {
                    Button(
                        onClick = onChatClick,
                        colors = ButtonDefaults.buttonColors(containerColor = TealBrand),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.3f).height(48.dp)
                    ) {
                        Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Message Landlord")
                    }
                } else {
                    Button(
                        onClick = onUnlockClick,
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.3f).height(48.dp)
                    ) {
                        Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Unlock via EcoCash")
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Ratings & Resident Reviews", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HouseReviewsSection(
                house = house,
                reviews = reviews,
                canReview = canReview,
                onSubmitReview = onAddReview
            )
        }
    }
}

@Composable
private fun EmptyHousesState(
    isAdmin: Boolean,
    onSeedStarterHouses: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(IndigoSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Apartment, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(36.dp))
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "No Approved Boarding Houses Yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Landlords can submit properties for Admin admission, or Admin can publish starter Chinhoyi listings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isAdmin) {
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onSeedStarterHouses,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("seed_houses_button")
            ) {
                Text("Publish Starter Chinhoyi Houses")
            }
        }
    }
}
