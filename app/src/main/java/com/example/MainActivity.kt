package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.AccommodationRepository
import com.example.data.AuthManager
import com.example.data.BoardingHouse
import com.example.data.PaymentStatus
import com.example.data.RegistrationDraft
import com.example.data.UiState
import com.example.data.UserRole
import com.example.ui.components.EcoCashUnlockSheet
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LandlordScreen
import com.example.ui.screens.MapScreen
import com.example.ui.screens.MyPaymentsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.AmberSoft
import com.example.ui.theme.ChinhoyiStaysTheme
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoSoft
import com.example.viewmodel.AppViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class AppTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    MAP("Map", Icons.Filled.Map, Icons.Outlined.Map),
    CHAT("Chat", Icons.Filled.Chat, Icons.Outlined.Chat),
    PAYMENTS("Payments", Icons.Filled.Payments, Icons.Outlined.Payments),
    LANDLORD("Landlord", Icons.Filled.Apartment, Icons.Outlined.Apartment),
    ADMIN("Admin", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChinhoyiStaysTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val auth = Firebase.auth
    val currentUser by AuthManager.authStateFlow(auth)
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var preferredLoginRole by remember { mutableStateOf(UserRole.CUSTOMER) }
    var pendingRegistrationDraft by remember { mutableStateOf<RegistrationDraft?>(null) }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            selectedRole = preferredLoginRole,
            onRoleSelected = { preferredLoginRole = it },
            onRegistrationDraftReady = { pendingRegistrationDraft = it },
            onAuthSuccess = { }
        )
    } else {
        key(user.uid) {
            AuthenticatedAppShell(
                currentUserId = user.uid,
                initialPreferredRole = preferredLoginRole,
                registrationDraft = pendingRegistrationDraft
            )
        }
    }
}

@Composable
private fun AuthenticatedAppShell(
    currentUserId: String,
    initialPreferredRole: UserRole,
    registrationDraft: RegistrationDraft?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: AppViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                AppViewModel(
                    repository = AccommodationRepository(db),
                    currentUserId = currentUserId,
                    initialPreferredRole = initialPreferredRole,
                    registrationDraft = registrationDraft
                )
            }
        }
    )

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val config by viewModel.platformConfig.collectAsStateWithLifecycle()
    val approvedHousesState by viewModel.approvedHousesState.collectAsStateWithLifecycle()
    val landlordHousesState by viewModel.landlordHousesState.collectAsStateWithLifecycle()
    val customerUnlocks by viewModel.customerUnlocks.collectAsStateWithLifecycle()
    val landlordUnlocks by viewModel.landlordUnlocks.collectAsStateWithLifecycle()
    val customerPayments by viewModel.customerPayments.collectAsStateWithLifecycle()
    val chatThreadsState by viewModel.chatThreadsState.collectAsStateWithLifecycle()
    val activeThread by viewModel.activeThread.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeThreadMessages.collectAsStateWithLifecycle()
    val activeReviews by viewModel.activeHouseReviews.collectAsStateWithLifecycle()
    val adminHouses by viewModel.adminHouses.collectAsStateWithLifecycle()
    val adminPayments by viewModel.adminPayments.collectAsStateWithLifecycle()
    val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
    val photoCache by viewModel.photoCache.collectAsStateWithLifecycle()
    val privateLocations by viewModel.privateLocationCache.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val isAdmin = viewModel.isAdminUser
    val userRole = UserRole.fromRaw(userProfile?.role)
    val showLandlordTab = isAdmin || userRole == UserRole.LANDLORD

    var currentTab by remember(currentUserId) {
        mutableStateOf(
            when {
                isAdmin && initialPreferredRole == UserRole.ADMIN -> AppTab.ADMIN
                (isAdmin || initialPreferredRole == UserRole.LANDLORD) && initialPreferredRole == UserRole.LANDLORD -> AppTab.LANDLORD
                else -> AppTab.HOME
            }
        )
    }
    var mapTargetHouseId by remember { mutableStateOf<String?>(null) }
    var houseToUnlock by remember { mutableStateOf<BoardingHouse?>(null) }

    val visibleTabs = remember(isAdmin, showLandlordTab) {
        buildList {
            add(AppTab.HOME)
            add(AppTab.MAP)
            add(AppTab.CHAT)
            if (!showLandlordTab && !isAdmin) add(AppTab.PAYMENTS)
            if (showLandlordTab) add(AppTab.LANDLORD)
            if (isAdmin) add(AppTab.ADMIN)
            add(AppTab.PROFILE)
        }
    }

    val unlockedHouseIds = remember(customerUnlocks) {
        customerUnlocks.map { it.houseId }.toSet()
    }

    val pendingPaymentsCount = remember(customerPayments) {
        customerPayments.count { it.status == PaymentStatus.PENDING.name }
    }

    val unreadCount = remember(chatThreadsState, currentUserId) {
        val list = (chatThreadsState as? UiState.Success)?.data ?: emptyList()
        list.sumOf {
            if (it.customerUid == currentUserId) it.unreadByCustomer else it.unreadByLandlord
        }.toInt()
    }

    val handleSignOut: () -> Unit = {
        AuthManager.signOut(credentialManager, scope)
    }

    BackHandler(enabled = currentTab != AppTab.HOME && activeThread == null) {
        currentTab = AppTab.HOME
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                visibleTabs.forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = {
                            val badgeCount = when (tab) {
                                AppTab.CHAT -> unreadCount
                                AppTab.PAYMENTS -> pendingPaymentsCount
                                else -> 0
                            }
                            if (badgeCount > 0) {
                                BadgedBox(badge = { Badge { Text(badgeCount.toString()) } }) {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (config.maintenanceMode) {
                Surface(color = AmberSoft, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Platform Notice: Admin maintenance update in progress.",
                        color = Color(0xFF92400E),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (currentTab) {
                    AppTab.HOME -> HomeScreen(
                        housesState = approvedHousesState,
                        config = config,
                        unlockedHouseIds = unlockedHouseIds,
                        customerPayments = customerPayments,
                        photoCache = photoCache,
                        privateLocations = privateLocations,
                        activeHouseReviews = activeReviews,
                        isAdmin = isAdmin,
                        onInspectHouse = { viewModel.loadHouseAssetsAndDetails(it) },
                        onOpenMapForHouse = { house ->
                            mapTargetHouseId = house.id
                            currentTab = AppTab.MAP
                        },
                        onUnlockHouseClick = { houseToUnlock = it },
                        onChatWithLandlord = { house ->
                            viewModel.openChatWithLandlord(house) {
                                currentTab = AppTab.CHAT
                            }
                        },
                        onAddReview = { id, rating, comment -> viewModel.addReview(id, rating, comment) },
                        onSeedStarterHouses = { viewModel.adminSeedStarterHouses() }
                    )

                    AppTab.MAP -> MapScreen(
                        housesState = approvedHousesState,
                        userProfile = userProfile,
                        config = config,
                        unlockedHouseIds = unlockedHouseIds,
                        privateLocations = privateLocations,
                        photoCache = photoCache,
                        activeHouseReviews = activeReviews,
                        initialSelectedHouseId = mapTargetHouseId,
                        isAdmin = isAdmin,
                        onInspectHouse = { viewModel.loadHouseAssetsAndDetails(it) },
                        onUnlockHouseClick = { houseToUnlock = it },
                        onChatWithLandlord = { house ->
                            viewModel.openChatWithLandlord(house) {
                                currentTab = AppTab.CHAT
                            }
                        },
                        onAddReview = { id, rating, comment -> viewModel.addReview(id, rating, comment) },
                        onNavigateToLandlord = { currentTab = AppTab.LANDLORD },
                        onNavigateToAdmin = { currentTab = AppTab.ADMIN },
                        onNavigateToPayments = { currentTab = AppTab.PAYMENTS },
                        onNavigateToProfile = { currentTab = AppTab.PROFILE },
                        onSignOut = handleSignOut
                    )

                    AppTab.CHAT -> ChatScreen(
                        currentUserId = currentUserId,
                        threadsState = chatThreadsState,
                        activeThread = activeThread,
                        messages = activeMessages,
                        onSelectThread = { viewModel.selectChatThread(it) },
                        onSendMessage = { text, img -> viewModel.sendChatMessage(text, img) },
                        onGoToHome = { currentTab = AppTab.HOME },
                        onOpenHouseMap = { houseId ->
                            mapTargetHouseId = houseId
                            currentTab = AppTab.MAP
                        }
                    )

                    AppTab.PAYMENTS -> {
                        val allApproved = (approvedHousesState as? UiState.Success)?.data ?: emptyList()
                        MyPaymentsScreen(
                            payments = customerPayments,
                            houses = allApproved,
                            config = config,
                            onOpenHouseMap = { houseId ->
                                mapTargetHouseId = houseId
                                currentTab = AppTab.MAP
                            },
                            onResubmitPayment = { house -> houseToUnlock = house },
                            onBrowseHouses = { currentTab = AppTab.HOME }
                        )
                    }

                    AppTab.LANDLORD -> LandlordScreen(
                        userProfile = userProfile,
                        isAdmin = isAdmin,
                        config = config,
                        landlordHousesState = landlordHousesState,
                        landlordUnlocks = landlordUnlocks,
                        photoCache = photoCache,
                        privateLocations = privateLocations,
                        isBusy = isBusy,
                        onInspectHouse = { house -> viewModel.loadHouseAssetsAndDetails(house) },
                        onRequestLandlordUpgrade = {
                            viewModel.updateProfile(
                                displayName = userProfile?.displayName ?: "Landlord",
                                phone = userProfile?.phone ?: "",
                                ecocashNumber = userProfile?.ecocashNumber ?: "",
                                institution = userProfile?.institution ?: "Chinhoyi Accommodations",
                                bio = userProfile?.bio ?: "",
                                role = UserRole.LANDLORD
                            )
                        },
                        onSubmitHouse = { name, suburb, desc, rent, dep, total, vacant, gp, rt, amen, lat, lng, addr, land, phone, photos, preset ->
                            viewModel.submitHouseListing(
                                name, suburb, desc, rent, dep, total, vacant, gp, rt, amen, lat, lng, addr, land, phone, photos, preset
                            )
                        },
                        onUpdateVacancy = { house, delta -> viewModel.updateVacancy(house, delta) },
                        onUpdateLocation = { id, lat, lng, addr, land, phone ->
                            viewModel.updateLandlordHouseLocation(id, lat, lng, addr, land, phone)
                        },
                        onMessageUnlockedStudent = { unlock ->
                            val houses = (landlordHousesState as? UiState.Success)?.data ?: emptyList()
                            val h = houses.firstOrNull { it.id == unlock.houseId }
                                ?: BoardingHouse(id = unlock.houseId, name = unlock.houseName, landlordUid = unlock.landlordUid, landlordName = userProfile?.displayName ?: "Landlord")
                            viewModel.openChatWithUnlockedStudent(unlock, h) {
                                currentTab = AppTab.CHAT
                            }
                        }
                    )

                    AppTab.ADMIN -> AdminScreen(
                        houses = adminHouses,
                        payments = adminPayments,
                        users = adminUsers,
                        config = config,
                        photoCache = photoCache,
                        privateLocations = privateLocations,
                        adminProfile = userProfile,
                        isBusy = isBusy,
                        onInspectHouse = { house -> viewModel.loadHouseAssetsAndDetails(house) },
                        onApproveHouse = { house, status, featured -> viewModel.adminApproveHouse(house, status, featured) },
                        onUnlockPayment = { pay -> viewModel.adminUnlockPayment(pay) },
                        onRejectPayment = { pay -> viewModel.adminRejectPayment(pay) },
                        onUpdateUser = { user, role, approved, active -> viewModel.adminUpdateUser(user, role, approved, active) },
                        onSaveConfig = { cfg -> viewModel.adminSaveConfig(cfg) },
                        onSeedStarterHouses = { viewModel.adminSeedStarterHouses() }
                    )

                    AppTab.PROFILE -> {
                        val threads = (chatThreadsState as? UiState.Success)?.data ?: emptyList()
                        ProfileScreen(
                            userProfile = userProfile,
                            unlockedCount = unlockedHouseIds.size,
                            chatCount = threads.size,
                            unlocks = customerUnlocks,
                            payments = customerPayments,
                            isAdmin = isAdmin,
                            isBusy = isBusy,
                            onSaveProfile = { name, phone, eco, inst, bio, role ->
                                viewModel.updateProfile(name, phone, eco, inst, bio, role)
                            },
                            onOpenMyPayments = { currentTab = AppTab.PAYMENTS },
                            onSignOut = handleSignOut
                        )
                    }
                }
            }
        }
    }

    val unlockTarget = houseToUnlock
    if (unlockTarget != null) {
        val latestPayment = customerPayments.firstOrNull { it.houseId == unlockTarget.id }
        EcoCashUnlockSheet(
            house = unlockTarget,
            config = config,
            defaultPayerName = userProfile?.displayName ?: "",
            defaultPayerPhone = userProfile?.ecocashNumber?.ifBlank { userProfile?.phone ?: "" } ?: "",
            existingPayment = latestPayment,
            isBusy = isBusy,
            onDismiss = { houseToUnlock = null },
            onSubmitPayment = { payerName, payerPhone, ref, currency ->
                viewModel.submitEcoCashPayment(unlockTarget, payerName, payerPhone, ref, currency)
                houseToUnlock = null
            }
        )
    }
}
