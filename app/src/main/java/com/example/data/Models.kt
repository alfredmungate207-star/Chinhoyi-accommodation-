package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class UserRole(val label: String) {
    CUSTOMER("Student / Customer"),
    LANDLORD("Landlord"),
    ADMIN("Admin");

    companion object {
        fun fromRaw(raw: String?): UserRole =
            entries.firstOrNull { it.name == raw } ?: CUSTOMER
    }
}

enum class HouseStatus(val label: String) {
    PENDING("Pending Review"),
    APPROVED("Approved"),
    SUSPENDED("Suspended");

    companion object {
        fun fromRaw(raw: String?): HouseStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class GenderPolicy(val label: String) {
    MIXED("Mixed"),
    FEMALE_ONLY("Girls Only"),
    MALE_ONLY("Boys Only");

    companion object {
        fun fromRaw(raw: String?): GenderPolicy =
            entries.firstOrNull { it.name == raw } ?: MIXED
    }
}

enum class RoomType(val label: String) {
    SHARED_2("2-Sharing"),
    SINGLE("Single Room"),
    BEDSITTER("Bedsitter"),
    MIXED("Mixed Rooms");

    companion object {
        fun fromRaw(raw: String?): RoomType =
            entries.firstOrNull { it.name == raw } ?: SHARED_2
    }
}

enum class PaymentStatus(val label: String) {
    PENDING("Pending EcoCash Check"),
    UNLOCKED("Unlocked"),
    REJECTED("Rejected");

    companion object {
        fun fromRaw(raw: String?): PaymentStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val ecocashNumber: String = "",
    val institution: String = "Chinhoyi University of Technology",
    val bio: String = "Looking for a safe, comfortable stay in Chinhoyi.",
    val locationCity: String = "Chinhoyi, Zimbabwe",
    val avatarRef: String = "",
    val role: String = UserRole.CUSTOMER.name,
    val landlordApproved: Boolean = false,
    val accountActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PlatformConfig(
    @DocumentId val id: String = "settings",
    val unlockFeeUsd: Double = 2.0,
    val unlockFeeZig: Double = 55.0,
    val ecocashNumber: String = "0771 234 567",
    val ecocashName: String = "Chinhoyi Accommodations Admin",
    val maxPhotosPerHouse: Long = 8L,
    val requireHouseApproval: Boolean = true,
    val requireFeeForLocation: Boolean = true,
    val requireFeeForChat: Boolean = true,
    val enableLiveVacancy: Boolean = true,
    val enableFeaturedBoost: Boolean = true,
    val maintenanceMode: Boolean = false,
    val welcomeBannerText: String = "Verified Chinhoyi boarding houses • Pay small EcoCash fee to unlock exact GPS & Landlord chat",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BoardingHouse(
    @DocumentId val id: String = "",
    val landlordUid: String = "",
    val landlordName: String = "",
    val name: String = "",
    val suburb: String = "Cold Stream",
    val description: String = "",
    val monthlyRentUsd: Double = 75.0,
    val depositUsd: Double = 75.0,
    val totalRooms: Long = 1L,
    val vacantRooms: Long = 1L,
    val genderPolicy: String = GenderPolicy.MIXED.name,
    val roomType: String = RoomType.SHARED_2.name,
    val distanceToCutKm: Double = 1.0,
    val approxLat: Double = -17.352,
    val approxLng: Double = 30.205,
    val amenities: List<String> = emptyList(),
    val coverPhotoRef: String = "drawable:img_boarding_exterior",
    val photoRefs: List<String> = emptyList(),
    val status: String = HouseStatus.PENDING.name,
    val featured: Boolean = false,
    val rating: Double = 4.8,
    val reviewCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HousePrivateLocation(
    @DocumentId val id: String = "location",
    val houseId: String = "",
    val landlordUid: String = "",
    val exactLat: Double = -17.3526,
    val exactLng: Double = 30.2054,
    val streetAddress: String = "",
    val landmarks: String = "",
    val contactPhone: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HousePhoto(
    @DocumentId val id: String = "",
    val houseId: String = "",
    val landlordUid: String = "",
    val photoData: String = "",
    val caption: String = "",
    val sortOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HouseReview(
    @DocumentId val id: String = "",
    val houseId: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PaymentRequest(
    @DocumentId val id: String = "",
    val customerUid: String = "",
    val customerEmail: String = "",
    val houseId: String = "",
    val houseName: String = "",
    val landlordUid: String = "",
    val payerName: String = "",
    val payerPhone: String = "",
    val referenceCode: String = "",
    val amountUsd: Double = 2.0,
    val currencyLabel: String = "USD",
    val status: String = PaymentStatus.PENDING.name,
    val adminNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HouseUnlock(
    @DocumentId val id: String = "",
    val customerUid: String = "",
    val houseId: String = "",
    val houseName: String = "",
    val landlordUid: String = "",
    val payerName: String = "",
    val paymentRequestId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatThread(
    @DocumentId val id: String = "",
    val customerUid: String = "",
    val customerName: String = "",
    val landlordUid: String = "",
    val landlordName: String = "",
    val houseId: String = "",
    val houseName: String = "",
    val memberUids: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastSenderUid: String = "",
    val unreadByCustomer: Long = 0L,
    val unreadByLandlord: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val threadId: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val memberUids: List<String> = emptyList(),
    val text: String = "",
    val messageType: String = "TEXT",
    val imageData: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class RegistrationDraft(
    val fullName: String = "",
    val phone: String = "",
    val institution: String = "Chinhoyi University of Technology",
    val role: UserRole = UserRole.CUSTOMER,
    val isSignUpFlow: Boolean = false
)
