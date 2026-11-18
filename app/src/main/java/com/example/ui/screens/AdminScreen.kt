package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BoardingHouse
import com.example.data.GenderPolicy
import com.example.data.HousePrivateLocation
import com.example.data.HouseStatus
import com.example.data.PaymentRequest
import com.example.data.PaymentStatus
import com.example.data.PlatformConfig
import com.example.data.RoomType
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
import com.example.ui.theme.TealSoft

private enum class AdmissionFilter(val label: String) {
    ALL("All"),
    PENDING("Pending Review"),
    APPROVED("Approved"),
    SUSPENDED("Suspended")
}

/**
 * Admin Dashboard inspired by MatDash & Clean Settings Table templates:
 * - Adaptive MatDash Sidebar Rail on tablets/foldables + Breadcrumb & KPI Header
 * - Admissions Review Queue: Search, Filter by Pending/Approved/Suspended, inspect full
 *   house photos, description, amenities & private GPS location before Accepting/Featuring
 * - EcoCash Unlocks: Verify Payer Full Name, Phone & Reference Code -> Unlock Location & Chat
 * - Users & Landlords Table: Search, Status Pills (Active/Pending/Disabled), Access Toggle & Menu
 * - Upgradable Features: Tune EcoCash fees, receiver number, max photos & feature flags live
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    houses: List<BoardingHouse>,
    payments: List<PaymentRequest>,
    users: List<UserProfile>,
    config: PlatformConfig,
    photoCache: Map<String, String>,
    privateLocations: Map<String, HousePrivateLocation>,
    adminProfile: UserProfile?,
    isBusy: Boolean,
    onInspectHouse: (BoardingHouse) -> Unit,
    onApproveHouse: (BoardingHouse, HouseStatus, Boolean) -> Unit,
    onUnlockPayment: (PaymentRequest) -> Unit,
    onRejectPayment: (PaymentRequest) -> Unit,
    onUpdateUser: (UserProfile, UserRole, Boolean, Boolean) -> Unit,
    onSaveConfig: (PlatformConfig) -> Unit,
    onSeedStarterHouses: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) }
    var admissionFilter by remember { mutableStateOf(AdmissionFilter.ALL) }
    var reviewingHouse by remember { mutableStateOf<BoardingHouse?>(null) }

    val tabs = listOf("Admissions", "EcoCash Unlocks", "Users & Landlords", "Upgrade Features")

    val pendingHousesCount = remember(houses) { houses.count { it.status == HouseStatus.PENDING.name } }
    val approvedHousesCount = remember(houses) { houses.count { it.status == HouseStatus.APPROVED.name } }
    val pendingPaymentsCount = remember(payments) { payments.count { it.status == PaymentStatus.PENDING.name } }
    val activeUsersCount = remember(users) { users.count { it.accountActive } }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FB))
    ) {
        val isWideLayout = maxWidth >= 720.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // MatDash / Settings Left Sidebar on Medium & Expanded screens
            if (isWideLayout) {
                AdminSidebarRail(
                    activeTab = activeTab,
                    pendingHousesCount = pendingHousesCount,
                    pendingPaymentsCount = pendingPaymentsCount,
                    adminProfile = adminProfile,
                    isBusy = isBusy,
                    onSelectTab = { activeTab = it },
                    onSeedStarterHouses = onSeedStarterHouses
                )
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // MatDash Top Breadcrumb & KPI Header
                AdminDashboardHeader(
                    activeTab = activeTab,
                    tabs = tabs,
                    isWideLayout = isWideLayout,
                    adminProfile = adminProfile,
                    pendingHousesCount = pendingHousesCount,
                    approvedHousesCount = approvedHousesCount,
                    pendingPaymentsCount = pendingPaymentsCount,
                    activeUsersCount = activeUsersCount,
                    isBusy = isBusy,
                    onSelectTab = { activeTab = it },
                    onJumpToPendingAdmissions = {
                        activeTab = 0
                        admissionFilter = AdmissionFilter.PENDING
                    },
                    onSeedStarterHouses = onSeedStarterHouses
                )

                // Active Dashboard Pane
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (activeTab) {
                        0 -> AdminAdmissionsTab(
                            houses = houses,
                            filter = admissionFilter,
                            onFilterChange = { admissionFilter = it },
                            photoCache = photoCache,
                            isBusy = isBusy,
                            onReviewHouse = { house ->
                                onInspectHouse(house)
                                reviewingHouse = house
                            },
                            onApproveHouse = onApproveHouse
                        )
                        1 -> AdminEcoCashTab(
                            payments = payments,
                            isBusy = isBusy,
                            onUnlockPayment = onUnlockPayment,
                            onRejectPayment = onRejectPayment
                        )
                        2 -> AdminUsersTableTab(
                            users = users,
                            onUpdateUser = onUpdateUser
                        )
                        else -> AdminUpgradeFeaturesTab(
                            config = config,
                            isBusy = isBusy,
                            onSaveConfig = onSaveConfig
                        )
                    }
                }
            }
        }
    }

    val targetReviewHouse = reviewingHouse?.let { current ->
        houses.firstOrNull { it.id == current.id } ?: current
    }
    if (targetReviewHouse != null) {
        LaunchedEffect(targetReviewHouse.id) {
            onInspectHouse(targetReviewHouse)
        }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { reviewingHouse = null },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            BoardingHouseReviewSheet(
                house = targetReviewHouse,
                photoCache = photoCache,
                privateLocation = privateLocations[targetReviewHouse.id],
                isBusy = isBusy,
                onAcceptAndAdvertise = { featured ->
                    onApproveHouse(targetReviewHouse, HouseStatus.APPROVED, featured)
                    reviewingHouse = null
                },
                onSuspend = {
                    onApproveHouse(targetReviewHouse, HouseStatus.SUSPENDED, false)
                    reviewingHouse = null
                },
                onReturnToPending = {
                    onApproveHouse(targetReviewHouse, HouseStatus.PENDING, false)
                    reviewingHouse = null
                },
                onClose = { reviewingHouse = null }
            )
        }
    }
}

@Composable
private fun AdminSidebarRail(
    activeTab: Int,
    pendingHousesCount: Int,
    pendingPaymentsCount: Int,
    adminProfile: UserProfile?,
    isBusy: Boolean,
    onSelectTab: (Int) -> Unit,
    onSeedStarterHouses: () -> Unit
) {
    Surface(
        color = Color.White,
        border = BorderStroke(1.dp, SlateBorder),
        modifier = Modifier.width(232.dp).fillMaxHeight()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(IndigoPrimary, IndigoDeep))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text("Chinhoyi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = IndigoDeep)
                        Text("Admin Console", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }

                Text("OPERATIONS", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)

                SidebarNavItem(
                    label = "Admissions",
                    icon = Icons.Filled.Apartment,
                    badgeCount = pendingHousesCount,
                    selected = activeTab == 0,
                    onClick = { onSelectTab(0) }
                )
                SidebarNavItem(
                    label = "EcoCash Unlocks",
                    icon = Icons.Filled.Payments,
                    badgeCount = pendingPaymentsCount,
                    selected = activeTab == 1,
                    onClick = { onSelectTab(1) }
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text("SYSTEM", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)

                SidebarNavItem(
                    label = "Users & Landlords",
                    icon = Icons.Filled.People,
                    badgeCount = 0,
                    selected = activeTab == 2,
                    onClick = { onSelectTab(2) }
                )
                SidebarNavItem(
                    label = "Upgrade Features",
                    icon = Icons.Filled.Tune,
                    badgeCount = 0,
                    selected = activeTab == 3,
                    onClick = { onSelectTab(3) }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onSeedStarterHouses,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Seed Starter Stays", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(IndigoSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (adminProfile?.displayName?.firstOrNull() ?: 'A').uppercase(),
                                color = IndigoDeep,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = adminProfile?.displayName ?: "Admin",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text("Super Admin", style = MaterialTheme.typography.labelSmall, color = TealBrand)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarNavItem(
    label: String,
    icon: ImageVector,
    badgeCount: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) IndigoPrimary else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else Color(0xFF475569),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) Color.White else Color(0xFF334155)
                )
            }
            if (badgeCount > 0) {
                Surface(
                    color = if (selected) Color.White.copy(alpha = 0.22f) else AmberSoft,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = badgeCount.toString(),
                        color = if (selected) Color.White else Color(0xFF92400E),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminDashboardHeader(
    activeTab: Int,
    tabs: List<String>,
    isWideLayout: Boolean,
    adminProfile: UserProfile?,
    pendingHousesCount: Int,
    approvedHousesCount: Int,
    pendingPaymentsCount: Int,
    activeUsersCount: Int,
    isBusy: Boolean,
    onSelectTab: (Int) -> Unit,
    onJumpToPendingAdmissions: () -> Unit,
    onSeedStarterHouses: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // MatDash Breadcrumb + Top Identity Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(IndigoSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = IndigoDeep)
                    }
                    Column {
                        Text(
                            text = "Admin Dashboard",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Home, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(13.dp))
                            Text("/", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Surface(color = IndigoSoft, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = tabs.getOrElse(activeTab) { "Admissions" },
                                    color = IndigoDeep,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isWideLayout) {
                        OutlinedButton(
                            onClick = onSeedStarterHouses,
                            enabled = !isBusy,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Seed Stays", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, SlateBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(24.dp).clip(CircleShape).background(IndigoPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (adminProfile?.displayName?.firstOrNull() ?: 'A').uppercase(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = adminProfile?.displayName?.substringBefore(" ") ?: "Admin",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // MatDash KPI Summary Cards Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminKpiChip(
                    title = "Pending Requests",
                    value = pendingHousesCount.toString(),
                    icon = Icons.Filled.HourglassTop,
                    bg = AmberSoft,
                    fg = Color(0xFF92400E),
                    onClick = onJumpToPendingAdmissions
                )
                AdminKpiChip(
                    title = "Live Stays",
                    value = approvedHousesCount.toString(),
                    icon = Icons.Filled.Verified,
                    bg = EmeraldSoft,
                    fg = Color(0xFF065F46),
                    onClick = { onSelectTab(0) }
                )
                AdminKpiChip(
                    title = "EcoCash Queue",
                    value = pendingPaymentsCount.toString(),
                    icon = Icons.Filled.Payments,
                    bg = IndigoSoft,
                    fg = IndigoDeep,
                    onClick = { onSelectTab(1) }
                )
                AdminKpiChip(
                    title = "Active Users",
                    value = activeUsersCount.toString(),
                    icon = Icons.Filled.People,
                    bg = TealSoft,
                    fg = Color(0xFF0F766E),
                    onClick = { onSelectTab(2) }
                )
            }

            if (!isWideLayout) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { idx, label ->
                        val badge = when (idx) {
                            0 -> if (pendingHousesCount > 0) " ($pendingHousesCount)" else ""
                            1 -> if (pendingPaymentsCount > 0) " ($pendingPaymentsCount)" else ""
                            else -> ""
                        }
                        FilterChip(
                            selected = activeTab == idx,
                            onClick = { onSelectTab(idx) },
                            label = { Text("$label$badge", fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                val ic = when (idx) {
                                    0 -> Icons.Filled.Apartment
                                    1 -> Icons.Filled.Payments
                                    2 -> Icons.Filled.People
                                    else -> Icons.Filled.Tune
                                }
                                Icon(ic, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoPrimary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            ),
                            modifier = Modifier.testTag("admin_tab_$idx")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminKpiChip(
    title: String,
    value: String,
    icon: ImageVector,
    bg: Color,
    fg: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, SlateBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            }
            Column {
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun AdminAdmissionsTab(
    houses: List<BoardingHouse>,
    filter: AdmissionFilter,
    onFilterChange: (AdmissionFilter) -> Unit,
    photoCache: Map<String, String>,
    isBusy: Boolean,
    onReviewHouse: (BoardingHouse) -> Unit,
    onApproveHouse: (BoardingHouse, HouseStatus, Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val pendingCount = remember(houses) { houses.count { it.status == HouseStatus.PENDING.name } }
    val sortedAndFiltered = remember(houses, filter, searchQuery) {
        houses
            .filter { house ->
                val statusMatches = when (filter) {
                    AdmissionFilter.ALL -> true
                    AdmissionFilter.PENDING -> house.status == HouseStatus.PENDING.name
                    AdmissionFilter.APPROVED -> house.status == HouseStatus.APPROVED.name
                    AdmissionFilter.SUSPENDED -> house.status == HouseStatus.SUSPENDED.name
                }
                val queryMatches = searchQuery.isBlank() ||
                    house.name.contains(searchQuery, ignoreCase = true) ||
                    house.suburb.contains(searchQuery, ignoreCase = true) ||
                    house.landlordName.contains(searchQuery, ignoreCase = true)
                statusMatches && queryMatches
            }
            .sortedWith(
                compareBy<BoardingHouse> {
                    when (HouseStatus.fromRaw(it.status)) {
                        HouseStatus.PENDING -> 0
                        HouseStatus.APPROVED -> 1
                        HouseStatus.SUSPENDED -> 2
                    }
                }.thenByDescending { it.createdAt?.seconds ?: 0L }
            )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search + Filter Chips Card
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search boarding house, suburb, or landlord...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("admin_admissions_search")
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdmissionFilter.entries.forEach { entry ->
                        val labelText = if (entry == AdmissionFilter.PENDING && pendingCount > 0) {
                            "${entry.label} ($pendingCount)"
                        } else {
                            entry.label
                        }
                        FilterChip(
                            selected = filter == entry,
                            onClick = { onFilterChange(entry) },
                            label = { Text(labelText, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoDeep,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("admin_admission_filter_${entry.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (pendingCount > 0 && filter != AdmissionFilter.APPROVED && filter != AdmissionFilter.SUSPENDED) {
            item {
                Surface(
                    color = AmberSoft,
                    border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.HourglassTop, contentDescription = null, tint = Color(0xFF92400E))
                        Text(
                            text = "$pendingCount new boarding house registration request(s) awaiting your review and acceptance.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF78350F)
                        )
                    }
                }
            }
        }

        if (sortedAndFiltered.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SlateBorder),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.Apartment, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(36.dp))
                        Text("No Boarding House Requests Match", fontWeight = FontWeight.Bold)
                        Text(
                            "New landlord registrations appear here automatically for your review before going live.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        items(sortedAndFiltered, key = { it.id }) { house ->
            val status = HouseStatus.fromRaw(house.status)
            val (bg, fg) = when (status) {
                HouseStatus.APPROVED -> EmeraldSoft to Color(0xFF065F46)
                HouseStatus.PENDING -> AmberSoft to Color(0xFF92400E)
                HouseStatus.SUSPENDED -> CoralSoft to Color(0xFF991B1B)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(
                    width = if (status == HouseStatus.PENDING) 1.5.dp else 1.dp,
                    color = if (status == HouseStatus.PENDING) Color(0xFFF59E0B) else SlateBorder
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReviewHouse(house) }
                    .testTag("admin_house_card_${house.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        HouseImage(
                            photoRef = house.coverPhotoRef,
                            photoCache = photoCache,
                            contentDescription = house.name,
                            modifier = Modifier.size(74.dp).clip(RoundedCornerShape(14.dp))
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = house.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                                )
                                Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = if (status == HouseStatus.PENDING) "Pending Review" else status.label,
                                        color = fg,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Landlord: ${house.landlordName} • ${house.suburb}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "$${house.monthlyRentUsd.toInt()}/mo • ${house.vacantRooms}/${house.totalRooms} vacant",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = IndigoPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (house.featured) {
                                    Surface(color = AmberSoft, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            text = "Featured",
                                            color = Color(0xFF92400E),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            if (house.description.isNotBlank()) {
                                Text(
                                    text = house.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onReviewHouse(house) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("admin_review_house_${house.id}")
                        ) {
                            Icon(Icons.Filled.RateReview, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Review", maxLines = 1)
                        }
                        if (status != HouseStatus.APPROVED) {
                            Button(
                                onClick = { onApproveHouse(house, HouseStatus.APPROVED, house.featured) },
                                enabled = !isBusy,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.25f).testTag("admin_accept_house_${house.id}")
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Accept & Publish", maxLines = 1)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onApproveHouse(house, HouseStatus.APPROVED, !house.featured) },
                                enabled = !isBusy,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = if (house.featured) AmberWarning else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (house.featured) "Featured" else "Boost", maxLines = 1)
                            }
                        }
                        OutlinedButton(
                            onClick = { onApproveHouse(house, HouseStatus.SUSPENDED, false) },
                            enabled = !isBusy && status != HouseStatus.SUSPENDED,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Suspend", color = CoralDanger)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoardingHouseReviewSheet(
    house: BoardingHouse,
    photoCache: Map<String, String>,
    privateLocation: HousePrivateLocation?,
    isBusy: Boolean,
    onAcceptAndAdvertise: (featured: Boolean) -> Unit,
    onSuspend: () -> Unit,
    onReturnToPending: () -> Unit,
    onClose: () -> Unit
) {
    val status = HouseStatus.fromRaw(house.status)
    val roomTypeLabel = RoomType.fromRaw(house.roomType).label
    val genderLabel = GenderPolicy.fromRaw(house.genderPolicy).label
    val photos = house.photoRefs.ifEmpty { listOf(house.coverPhotoRef) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Registration Review", style = MaterialTheme.typography.labelMedium, color = IndigoPrimary, fontWeight = FontWeight.Bold)
                Text(house.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("${house.suburb} • Submitted by ${house.landlordName}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close review")
            }
        }

        // Photo Gallery Verification Strip
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                Text("Uploaded Photos (${photos.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(photos) { ref ->
                    HouseImage(
                        photoRef = ref,
                        photoCache = photoCache,
                        contentDescription = house.name,
                        modifier = Modifier
                            .width(210.dp)
                            .height(145.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
            }
        }

        // Pricing, Rooms & Policy Summary
        Surface(
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Monthly Rent", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("$${house.monthlyRentUsd.toInt()} USD", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = IndigoDeep)
                    }
                    Column {
                        Text("Deposit", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("$${house.depositUsd.toInt()} USD", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Vacancy", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("${house.vacantRooms} / ${house.totalRooms} rooms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TealBrand)
                    }
                }
                HorizontalDivider()
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AssistChip(onClick = {}, label = { Text(roomTypeLabel) })
                    AssistChip(onClick = {}, label = { Text(genderLabel) })
                }
            }
        }

        if (house.description.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Property Description", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(house.description, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF334155))
            }
        }

        if (house.amenities.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Amenities & Utilities", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    house.amenities.forEach { amenity ->
                        Surface(color = IndigoSoft, shape = RoundedCornerShape(10.dp)) {
                            Text(
                                text = amenity,
                                color = IndigoDeep,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // Exact Private GPS & Landlord Verification Block
        Surface(
            color = EmeraldSoft.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFF065F46), modifier = Modifier.size(18.dp))
                    Text("Submitted Exact Location & Contact", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                }
                if (privateLocation == null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Loading private GPS & street address...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                } else {
                    Text("Address: ${privateLocation.streetAddress.ifBlank { "Not specified" }}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    if (privateLocation.landmarks.isNotBlank()) {
                        Text("Landmarks: ${privateLocation.landmarks}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF334155))
                    }
                    Text(
                        text = "GPS: ${"%.5f".format(privateLocation.exactLat)}, ${"%.5f".format(privateLocation.exactLng)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = IndigoDeep
                    )
                    if (privateLocation.contactPhone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = TealBrand)
                            Text("Landlord Phone: ${privateLocation.contactPhone}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Decision Action Buttons
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onAcceptAndAdvertise(false) },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1.2f).height(50.dp).testTag("review_accept_button")
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Accept & Publish", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onAcceptAndAdvertise(true) },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(50.dp).testTag("review_accept_feature_button")
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Accept + Boost", fontWeight = FontWeight.Bold)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (status != HouseStatus.PENDING) {
                    OutlinedButton(
                        onClick = onReturnToPending,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Move to Pending")
                    }
                }
                if (status != HouseStatus.SUSPENDED) {
                    OutlinedButton(
                        onClick = onSuspend,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).testTag("review_suspend_button")
                    ) {
                        Icon(Icons.Filled.Block, contentDescription = null, tint = CoralDanger, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reject / Suspend", color = CoralDanger)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun AdminEcoCashTab(
    payments: List<PaymentRequest>,
    isBusy: Boolean,
    onUnlockPayment: (PaymentRequest) -> Unit,
    onRejectPayment: (PaymentRequest) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<PaymentStatus?>(null) }

    val pendingCount = remember(payments) { payments.count { it.status == PaymentStatus.PENDING.name } }
    val filteredPayments = remember(payments, search, statusFilter) {
        payments
            .filter { pay ->
                val matchesStatus = statusFilter == null || pay.status == statusFilter?.name
                val matchesQuery = search.isBlank() ||
                    pay.payerName.contains(search, ignoreCase = true) ||
                    pay.payerPhone.contains(search, ignoreCase = true) ||
                    pay.referenceCode.contains(search, ignoreCase = true) ||
                    pay.houseName.contains(search, ignoreCase = true)
                matchesStatus && matchesQuery
            }
            .sortedWith(
                compareBy<PaymentRequest> {
                    when (PaymentStatus.fromRaw(it.status)) {
                        PaymentStatus.PENDING -> 0
                        PaymentStatus.UNLOCKED -> 1
                        PaymentStatus.REJECTED -> 2
                    }
                }.thenByDescending { it.createdAt?.seconds ?: 0L }
            )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search sender name, EcoCash phone, ref or house...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("admin_ecocash_search")
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = statusFilter == null,
                        onClick = { statusFilter = null },
                        label = { Text("All (${payments.size})", fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoDeep,
                            selectedLabelColor = Color.White
                        )
                    )
                    PaymentStatus.entries.forEach { st ->
                        val label = if (st == PaymentStatus.PENDING && pendingCount > 0) {
                            "${st.label} ($pendingCount)"
                        } else {
                            st.label
                        }
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = st },
                            label = { Text(label, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoDeep,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        if (filteredPayments.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SlateBorder),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Payments, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No EcoCash Payments Match", fontWeight = FontWeight.Bold)
                        Text(
                            "When customers submit EcoCash transfers, their Sender Name, Phone, and Reference Code appear here with PENDING status so you can verify and unlock their map pin & landlord chat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
        items(filteredPayments, key = { it.id }) { pay ->
            val st = PaymentStatus.fromRaw(pay.status)
            val (bg, fg) = when (st) {
                PaymentStatus.UNLOCKED -> EmeraldSoft to Color(0xFF065F46)
                PaymentStatus.PENDING -> AmberSoft to Color(0xFF92400E)
                PaymentStatus.REJECTED -> CoralSoft to Color(0xFF991B1B)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(
                    width = if (st == PaymentStatus.PENDING) 1.5.dp else 1.dp,
                    color = if (st == PaymentStatus.PENDING) Color(0xFFF59E0B) else SlateBorder
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().testTag("admin_payment_card_${pay.id}")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sender Name: ${pay.payerName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = IndigoDeep)
                            Text("Phone: ${pay.payerPhone} • Ref: ${pay.referenceCode}", style = MaterialTheme.typography.bodySmall)
                            if (pay.customerEmail.isNotBlank()) {
                                Text("Account: ${pay.customerEmail}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                        Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = st.label,
                                color = fg,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "House: ${pay.houseName} • Amount: ${pay.amountUsd} ${pay.currencyLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TealBrand
                    )

                    if (st == PaymentStatus.PENDING) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onUnlockPayment(pay) },
                                enabled = !isBusy,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.4f).testTag("admin_unlock_pay_${pay.id}")
                            ) {
                                Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verify & Unlock")
                            }
                            OutlinedButton(
                                onClick = { onRejectPayment(pay) },
                                enabled = !isBusy,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).testTag("admin_reject_pay_${pay.id}")
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = null, tint = CoralDanger, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reject", color = CoralDanger)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminUsersTableTab(
    users: List<UserProfile>,
    onUpdateUser: (UserProfile, UserRole, Boolean, Boolean) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(users, search) {
        users.filter {
            search.isBlank() ||
                it.displayName.contains(search, ignoreCase = true) ||
                it.email.contains(search, ignoreCase = true)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search users & landlords...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Template #6 Clean Settings Table Header
        item {
            Surface(color = Color(0xFFF8FAFC), border = BorderStroke(1.dp, SlateBorder), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("User Details (${filtered.size})", style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.weight(1.6f))
                    Text("Status", style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.weight(0.8f))
                    Text("Access", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                }
            }
        }

        items(filtered, key = { it.userId }) { user ->
            var menuExpanded by remember { mutableStateOf(false) }
            val role = UserRole.fromRaw(user.role)
            val statusLabel = when {
                !user.accountActive -> "Disabled"
                role == UserRole.LANDLORD && !user.landlordApproved -> "Pending"
                else -> "Active"
            }
            val (stBg, stFg) = when (statusLabel) {
                "Active" -> EmeraldSoft to Color(0xFF065F46)
                "Pending" -> AmberSoft to Color(0xFF92400E)
                else -> CoralSoft to Color(0xFF991B1B)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(IndigoSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (user.displayName.firstOrNull() ?: 'U').uppercase(),
                            color = IndigoDeep,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.weight(1.4f)) {
                        Text(user.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(user.email, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
                        Text(role.label, style = MaterialTheme.typography.labelSmall, color = IndigoPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    Surface(color = stBg, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = statusLabel,
                            color = stFg,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Switch(
                        checked = user.accountActive,
                        onCheckedChange = { active ->
                            onUpdateUser(user, role, user.landlordApproved, active)
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = EmeraldAccent)
                    )

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "User actions")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Approve as Landlord") },
                                onClick = {
                                    menuExpanded = false
                                    onUpdateUser(user, UserRole.LANDLORD, true, true)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Set as Customer") },
                                onClick = {
                                    menuExpanded = false
                                    onUpdateUser(user, UserRole.CUSTOMER, false, true)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (user.accountActive) "Suspend Access" else "Restore Access") },
                                onClick = {
                                    menuExpanded = false
                                    onUpdateUser(user, role, user.landlordApproved, !user.accountActive)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminUpgradeFeaturesTab(
    config: PlatformConfig,
    isBusy: Boolean,
    onSaveConfig: (PlatformConfig) -> Unit
) {
    var feeUsd by remember(config) { mutableStateOf(config.unlockFeeUsd.toString()) }
    var feeZig by remember(config) { mutableStateOf(config.unlockFeeZig.toString()) }
    var ecocashNum by remember(config) { mutableStateOf(config.ecocashNumber) }
    var ecocashName by remember(config) { mutableStateOf(config.ecocashName) }
    var maxPhotos by remember(config) { mutableStateOf(config.maxPhotosPerHouse.toString()) }
    var bannerText by remember(config) { mutableStateOf(config.welcomeBannerText) }

    var requireApproval by remember(config) { mutableStateOf(config.requireHouseApproval) }
    var requireLocationFee by remember(config) { mutableStateOf(config.requireFeeForLocation) }
    var requireChatFee by remember(config) { mutableStateOf(config.requireFeeForChat) }
    var enableVacancy by remember(config) { mutableStateOf(config.enableLiveVacancy) }
    var enableFeatured by remember(config) { mutableStateOf(config.enableFeaturedBoost) }
    var maintenanceMode by remember(config) { mutableStateOf(config.maintenanceMode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("EcoCash Fee & Merchant Upgrade", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = feeUsd,
                        onValueChange = { feeUsd = it },
                        label = { Text("Unlock Fee (USD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("admin_fee_usd_input")
                    )
                    OutlinedTextField(
                        value = feeZig,
                        onValueChange = { feeZig = it },
                        label = { Text("Unlock Fee (ZiG)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = ecocashNum,
                    onValueChange = { ecocashNum = it },
                    label = { Text("Admin EcoCash Receiver Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ecocashName,
                    onValueChange = { ecocashName = it },
                    label = { Text("EcoCash Account Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = maxPhotos,
                    onValueChange = { maxPhotos = it },
                    label = { Text("Max Photos Allowed Per House") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bannerText,
                    onValueChange = { bannerText = it },
                    label = { Text("Customer Home Announcement Banner") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Upgradable Platform Feature Toggles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FeatureToggleRow("Require Admin Admission Approval", "Landlord listings wait for your approval before advertising", requireApproval) { requireApproval = it }
                HorizontalDivider()
                FeatureToggleRow("Paywall Exact GPS Location", "Require EcoCash verification before showing street address & pin", requireLocationFee) { requireLocationFee = it }
                HorizontalDivider()
                FeatureToggleRow("Paywall Direct Landlord Chat", "Require EcoCash verification before customer can chat with landlord", requireChatFee) { requireChatFee = it }
                HorizontalDivider()
                FeatureToggleRow("Live Vacancy Ticker", "Display real-time vacant room counts on all cards", enableVacancy) { enableVacancy = it }
                HorizontalDivider()
                FeatureToggleRow("Featured Listing Boost", "Show gold Featured badge on boosted houses", enableFeatured) { enableFeatured = it }
                HorizontalDivider()
                FeatureToggleRow("Maintenance Notice Mode", "Show maintenance notice banner across the platform", maintenanceMode) { maintenanceMode = it }
            }
        }

        Button(
            onClick = {
                onSaveConfig(
                    config.copy(
                        unlockFeeUsd = feeUsd.toDoubleOrNull() ?: config.unlockFeeUsd,
                        unlockFeeZig = feeZig.toDoubleOrNull() ?: config.unlockFeeZig,
                        ecocashNumber = ecocashNum,
                        ecocashName = ecocashName,
                        maxPhotosPerHouse = maxPhotos.toLongOrNull() ?: config.maxPhotosPerHouse,
                        requireHouseApproval = requireApproval,
                        requireFeeForLocation = requireLocationFee,
                        requireFeeForChat = requireChatFee,
                        enableLiveVacancy = enableVacancy,
                        enableFeaturedBoost = enableFeatured,
                        maintenanceMode = maintenanceMode,
                        welcomeBannerText = bannerText
                    )
                )
            },
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = IndigoDeep),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("admin_save_config_button")
        ) {
            Text("Save & Upgrade All Platform Features", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FeatureToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = IndigoPrimary)
        )
    }
}
