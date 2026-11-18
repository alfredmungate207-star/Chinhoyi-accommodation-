package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AccommodationRepository
import com.example.data.AuthManager
import com.example.data.BoardingHouse
import com.example.data.ChatMessage
import com.example.data.ChatThread
import com.example.data.GenderPolicy
import com.example.data.HousePrivateLocation
import com.example.data.HouseReview
import com.example.data.HouseStatus
import com.example.data.HouseUnlock
import com.example.data.PaymentRequest
import com.example.data.PlatformConfig
import com.example.data.RegistrationDraft
import com.example.data.RoomType
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.UserRole
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    private val repository: AccommodationRepository,
    val currentUserId: String,
    initialPreferredRole: UserRole,
    registrationDraft: RegistrationDraft? = null
) : ViewModel() {

    val isAdminUser: Boolean = AuthManager.isAdminUser(Firebase.auth.currentUser)

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    // Track photo data URIs fetched from /houses/{id}/photos
    private val _photoCache = MutableStateFlow<Map<String, String>>(emptyMap())
    val photoCache: StateFlow<Map<String, String>> = _photoCache.asStateFlow()

    // Track unlocked exact private locations per houseId
    private val _privateLocationCache = MutableStateFlow<Map<String, HousePrivateLocation>>(emptyMap())
    val privateLocationCache: StateFlow<Map<String, HousePrivateLocation>> = _privateLocationCache.asStateFlow()

    private val _activeThread = MutableStateFlow<ChatThread?>(null)
    val activeThread: StateFlow<ChatThread?> = _activeThread.asStateFlow()

    private val _selectedHouseForReviews = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            val user = Firebase.auth.currentUser
            if (user != null) {
                repository.ensureUserProfile(user, initialPreferredRole, registrationDraft)
                if (initialPreferredRole == UserRole.ADMIN && !isAdminUser) {
                    _statusMessage.value = "Admin console requires ${AuthManager.BOOTSTRAPPED_ADMIN_EMAIL}. Signed in as standard user."
                } else if (registrationDraft?.isSignUpFlow == true) {
                    _statusMessage.value = "Account created! Welcome to Chinhoyi Accommodations."
                }
            }
        }
    }

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            unwrapAndLog("userProfile", error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val platformConfig: StateFlow<PlatformConfig> = repository.observePlatformConfig()
        .catch { error ->
            unwrapAndLog("platformConfig", error)
            emit(PlatformConfig())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PlatformConfig())

    val approvedHousesState: StateFlow<UiState<List<BoardingHouse>>> =
        repository.observeApprovedHouses().toUiState("approvedHouses")

    val landlordHousesState: StateFlow<UiState<List<BoardingHouse>>> =
        repository.observeLandlordHouses(currentUserId).toUiState("landlordHouses")

    val customerUnlocks: StateFlow<List<HouseUnlock>> = repository.observeCustomerUnlocks(currentUserId)
        .catch { error ->
            unwrapAndLog("customerUnlocks", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val landlordUnlocks: StateFlow<List<HouseUnlock>> = repository.observeLandlordUnlocks(currentUserId)
        .catch { error ->
            unwrapAndLog("landlordUnlocks", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val customerPayments: StateFlow<List<PaymentRequest>> = repository.observeCustomerPayments(currentUserId)
        .catch { error ->
            unwrapAndLog("customerPayments", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val chatThreadsState: StateFlow<UiState<List<ChatThread>>> =
        repository.observeChatThreads(currentUserId).toUiState("chatThreads")

    val activeThreadMessages: StateFlow<List<ChatMessage>> = _activeThread
        .flatMapLatest { thread ->
            if (thread == null) flowOf(emptyList())
            else repository.observeThreadMessages(thread.id, currentUserId)
                .catch { error ->
                    unwrapAndLog("threadMessages", error)
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val activeHouseReviews: StateFlow<List<HouseReview>> = _selectedHouseForReviews
        .flatMapLatest { houseId ->
            if (houseId.isNullOrBlank()) flowOf(emptyList())
            else repository.observeHouseReviews(houseId)
                .catch { error ->
                    unwrapAndLog("houseReviews", error)
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    // Admin-only streams: NEVER attached unless isAdminUser == true
    val adminHouses: StateFlow<List<BoardingHouse>> =
        (if (isAdminUser) repository.observeAllHousesForAdmin() else flowOf(emptyList()))
            .catch { error ->
                unwrapAndLog("adminHouses", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val adminPayments: StateFlow<List<PaymentRequest>> =
        (if (isAdminUser) repository.observeAllPaymentsForAdmin() else flowOf(emptyList()))
            .catch { error ->
                unwrapAndLog("adminPayments", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val adminUsers: StateFlow<List<UserProfile>> =
        (if (isAdminUser) repository.observeAllUsersForAdmin() else flowOf(emptyList()))
            .catch { error ->
                unwrapAndLog("adminUsers", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    fun clearMessage() {
        _statusMessage.value = null
    }

    fun isHouseUnlockedForCustomer(house: BoardingHouse): Boolean {
        if (isAdminUser || house.landlordUid == currentUserId) return true
        if (!platformConfig.value.requireFeeForLocation) return true
        return customerUnlocks.value.any { it.houseId == house.id }
    }

    fun isChatUnlockedForCustomer(house: BoardingHouse): Boolean {
        if (isAdminUser || house.landlordUid == currentUserId) return true
        if (!platformConfig.value.requireFeeForChat) return true
        return customerUnlocks.value.any { it.houseId == house.id }
    }

    fun loadHouseAssetsAndDetails(house: BoardingHouse) {
        _selectedHouseForReviews.value = house.id
        viewModelScope.launch {
            val subRefs = house.photoRefs.filter { it.startsWith("sub:") }
            if (subRefs.isNotEmpty()) {
                repository.getHousePhotos(house.id, subRefs).onSuccess { map ->
                    _photoCache.value = _photoCache.value + map
                }
            }
            if (isHouseUnlockedForCustomer(house)) {
                repository.getHousePrivateLocation(house.id).onSuccess { loc ->
                    if (loc != null) {
                        _privateLocationCache.value = _privateLocationCache.value + (house.id to loc)
                    }
                }
            }
        }
    }

    fun updateProfile(
        displayName: String,
        phone: String,
        ecocashNumber: String,
        institution: String,
        bio: String,
        role: UserRole
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.updateMyProfile(displayName, phone, ecocashNumber, institution, bio, role)
                .onSuccess { _statusMessage.value = "Profile updated" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to update profile" }
            _isBusy.value = false
        }
    }

    fun submitEcoCashPayment(
        house: BoardingHouse,
        payerName: String,
        payerPhone: String,
        referenceCode: String,
        currencyLabel: String
    ) {
        if (payerName.trim().length < 2) {
            _statusMessage.value = "Please enter the EcoCash payer's full name"
            return
        }
        if (payerPhone.trim().length < 5) {
            _statusMessage.value = "Please enter the EcoCash phone number"
            return
        }
        if (referenceCode.trim().length < 2) {
            _statusMessage.value = "Please enter your EcoCash transaction reference"
            return
        }
        val cfg = platformConfig.value
        val amount = if (currencyLabel == "ZiG") cfg.unlockFeeZig else cfg.unlockFeeUsd
        viewModelScope.launch {
            _isBusy.value = true
            repository.submitEcoCashPayment(house, payerName, payerPhone, referenceCode, amount, currencyLabel)
                .onSuccess {
                    _statusMessage.value = "EcoCash payment submitted for $payerName! Admin will verify and unlock shortly."
                }
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "Payment submission failed"
                }
            _isBusy.value = false
        }
    }

    fun openChatWithLandlord(house: BoardingHouse, onThreadReady: (ChatThread) -> Unit) {
        val profileName = userProfile.value?.displayName ?: Firebase.auth.currentUser?.displayName ?: "Student"
        viewModelScope.launch {
            _isBusy.value = true
            repository.openOrCreateChatThread(currentUserId, profileName, house)
                .onSuccess { thread ->
                    _activeThread.value = thread
                    repository.markThreadRead(thread)
                    onThreadReady(thread)
                }
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "Unable to open chat. Please unlock house first."
                }
            _isBusy.value = false
        }
    }

    fun openChatWithUnlockedStudent(unlock: HouseUnlock, house: BoardingHouse, onThreadReady: (ChatThread) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.openOrCreateChatThread(unlock.customerUid, unlock.payerName, house)
                .onSuccess { thread ->
                    _activeThread.value = thread
                    repository.markThreadRead(thread)
                    onThreadReady(thread)
                }
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "Unable to open student chat."
                }
            _isBusy.value = false
        }
    }

    fun selectChatThread(thread: ChatThread?) {
        _activeThread.value = thread
        if (thread != null) {
            viewModelScope.launch {
                repository.markThreadRead(thread)
            }
        }
    }

    fun sendChatMessage(text: String, imageDataUri: String? = null) {
        val thread = _activeThread.value ?: return
        val senderName = userProfile.value?.displayName ?: Firebase.auth.currentUser?.displayName ?: "User"
        viewModelScope.launch {
            repository.sendChatMessage(thread, senderName, text, imageDataUri)
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "Failed to send message"
                }
        }
    }

    fun submitHouseListing(
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
    ) {
        if (name.trim().length < 2) {
            _statusMessage.value = "Boarding house name must be at least 2 characters"
            return
        }
        val landlordName = userProfile.value?.displayName ?: Firebase.auth.currentUser?.displayName ?: "Landlord"
        viewModelScope.launch {
            _isBusy.value = true
            val autoApprove = isAdminUser || !platformConfig.value.requireHouseApproval
            repository.submitBoardingHouse(
                houseId = null,
                landlordName = landlordName,
                name = name,
                suburb = suburb,
                description = description,
                monthlyRentUsd = monthlyRentUsd,
                depositUsd = depositUsd,
                totalRooms = totalRooms,
                vacantRooms = vacantRooms,
                genderPolicy = genderPolicy,
                roomType = roomType,
                amenities = amenities,
                exactLat = exactLat,
                exactLng = exactLng,
                streetAddress = streetAddress,
                landmarks = landmarks,
                contactPhone = contactPhone,
                uploadedPhotoDataUris = uploadedPhotoDataUris,
                presetCoverRef = presetCoverRef,
                autoApproveAsAdmin = autoApprove
            ).onSuccess {
                _statusMessage.value = if (autoApprove) {
                    "House published live!"
                } else {
                    "House submitted for Admin admission approval!"
                }
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to submit house"
            }
            _isBusy.value = false
        }
    }

    fun updateVacancy(house: BoardingHouse, delta: Long) {
        val nextVacant = (house.vacantRooms + delta).coerceIn(0L, house.totalRooms)
        if (nextVacant == house.vacantRooms) return
        viewModelScope.launch {
            repository.updateHouseVacancy(house.id, house.totalRooms, nextVacant)
                .onSuccess { _statusMessage.value = "${house.name}: $nextVacant vacant rooms updated" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to update vacancy" }
        }
    }

    fun updateLandlordHouseLocation(
        houseId: String,
        exactLat: Double,
        exactLng: Double,
        streetAddress: String,
        landmarks: String,
        contactPhone: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.updateHouseLocation(houseId, exactLat, exactLng, streetAddress, landmarks, contactPhone)
                .onSuccess { _statusMessage.value = "Live location updated" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to update location" }
            _isBusy.value = false
        }
    }

    fun addReview(houseId: String, rating: Long, comment: String) {
        if (comment.trim().length < 2) {
            _statusMessage.value = "Please enter a short review comment"
            return
        }
        val authorName = userProfile.value?.displayName ?: "Student"
        viewModelScope.launch {
            repository.addHouseReview(houseId, authorName, rating, comment)
                .onSuccess { _statusMessage.value = "Review posted" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Unlock house first to review" }
        }
    }

    // --- ADMIN ACTIONS ---

    fun adminApproveHouse(house: BoardingHouse, status: HouseStatus, featured: Boolean) {
        if (!isAdminUser) return
        viewModelScope.launch {
            repository.adminUpdateHouseAdmission(house.id, status, featured)
                .onSuccess { _statusMessage.value = "${house.name} updated to ${status.label}" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Admin update failed" }
        }
    }

    fun adminUnlockPayment(payment: PaymentRequest) {
        if (!isAdminUser) return
        viewModelScope.launch {
            _isBusy.value = true
            repository.adminVerifyAndUnlockPayment(payment)
                .onSuccess { _statusMessage.value = "Unlocked ${payment.houseName} for ${payment.payerName}" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Unlock failed" }
            _isBusy.value = false
        }
    }

    fun adminRejectPayment(payment: PaymentRequest) {
        if (!isAdminUser) return
        viewModelScope.launch {
            repository.adminRejectPayment(payment.id, "EcoCash reference could not be confirmed.")
                .onSuccess { _statusMessage.value = "Payment rejected for ${payment.payerName}" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Action failed" }
        }
    }

    fun adminUpdateUser(user: UserProfile, role: UserRole, landlordApproved: Boolean, accountActive: Boolean) {
        if (!isAdminUser) return
        viewModelScope.launch {
            repository.adminUpdateUserAccess(user.userId, role, landlordApproved, accountActive)
                .onSuccess { _statusMessage.value = "Updated ${user.displayName}" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to update user" }
        }
    }

    fun adminSaveConfig(config: PlatformConfig) {
        if (!isAdminUser) return
        viewModelScope.launch {
            _isBusy.value = true
            repository.savePlatformConfig(config)
                .onSuccess { _statusMessage.value = "Platform features & fees upgraded!" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save config" }
            _isBusy.value = false
        }
    }

    fun adminSeedStarterHouses() {
        if (!isAdminUser) return
        val name = userProfile.value?.displayName ?: "Chinhoyi Stays Admin"
        viewModelScope.launch {
            _isBusy.value = true
            repository.seedStarterChinhoyiHousesAsAdmin(name)
                .onSuccess { _statusMessage.value = "4 Chinhoyi boarding houses published!" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to seed houses" }
            _isBusy.value = false
        }
    }

    private fun <T> Flow<T>.toUiState(name: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val msg = unwrapAndLog(name, error)
                emit(UiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private fun unwrapAndLog(streamName: String, error: Throwable): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, "Stream error in $streamName", root)
        return root.message ?: "Unable to load $streamName"
    }

    private companion object {
        const val TAG = "AppViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
