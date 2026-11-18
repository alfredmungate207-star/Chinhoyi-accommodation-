package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AccommodationRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun isCurrentUserAdmin(): Boolean = AuthManager.isAdminUser(auth.currentUser)

    // --- USER PROFILES ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        firebaseUser: FirebaseUser,
        preferredRole: UserRole,
        registrationDraft: RegistrationDraft? = null
    ): Result<UserProfile> {
        val uid = firebaseUser.uid
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            val isAdmin = AuthManager.isAdminUser(firebaseUser)
            val effectiveRole = when {
                isAdmin -> UserRole.ADMIN
                registrationDraft?.isSignUpFlow == true && registrationDraft.role == UserRole.LANDLORD -> UserRole.LANDLORD
                preferredRole == UserRole.LANDLORD -> UserRole.LANDLORD
                else -> UserRole.CUSTOMER
            }

            if (snap.exists()) {
                val existing = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (existing != null) {
                    if (registrationDraft?.isSignUpFlow == true) {
                        val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
                        val cleanName = registrationDraft.fullName.trim().take(80)
                        if (cleanName.isNotEmpty()) updates["displayName"] = cleanName
                        val cleanPhone = registrationDraft.phone.trim().take(30)
                        if (cleanPhone.isNotEmpty()) {
                            updates["phone"] = cleanPhone
                            updates["ecocashNumber"] = cleanPhone
                        }
                        val cleanInst = registrationDraft.institution.trim().take(120)
                        if (cleanInst.isNotEmpty()) updates["institution"] = cleanInst
                        if (!isAdmin) {
                            updates["role"] = if (effectiveRole == UserRole.LANDLORD) UserRole.LANDLORD.name else UserRole.CUSTOMER.name
                        }
                        docRef.update(updates).await()
                        val refreshed = docRef.get().await()
                            .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        if (refreshed != null) return Result.success(refreshed)
                    }
                    return Result.success(existing)
                }
            }

            val assignedRole = effectiveRole.name
            val draftName = registrationDraft?.fullName?.trim()?.takeIf { it.isNotBlank() }
            val displayName = (draftName ?: firebaseUser.displayName?.takeIf { it.isNotBlank() } ?: "Chinhoyi User").take(80)
            val email = (firebaseUser.email ?: "").take(150)
            val phone = registrationDraft?.phone?.trim()?.take(30).orEmpty()
            val institution = registrationDraft?.institution?.trim()?.takeIf { it.isNotBlank() }?.take(120)
                ?: "Chinhoyi University of Technology"

            val strictPayload = mutableMapOf<String, Any>(
                "userId" to uid,
                "displayName" to displayName,
                "email" to email,
                "institution" to institution,
                "bio" to if (assignedRole == UserRole.LANDLORD.name) "Boarding house owner in Chinhoyi." else "Student / Resident in Chinhoyi.",
                "locationCity" to "Chinhoyi, Zimbabwe",
                "role" to assignedRole,
                "landlordApproved" to isAdmin,
                "accountActive" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (phone.isNotEmpty()) {
                strictPayload["phone"] = phone
                strictPayload["ecocashNumber"] = phone
            }
            val photo = firebaseUser.photoUrl?.toString()?.take(500)
            if (!photo.isNullOrBlank()) {
                strictPayload["avatarRef"] = photo
            }
            docRef.set(strictPayload).await()
            val created = docRef.get().await()
                .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: UserProfile(
                    userId = uid,
                    displayName = displayName,
                    email = email,
                    phone = phone,
                    ecocashNumber = phone,
                    institution = institution,
                    role = assignedRole
                )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMyProfile(
        displayName: String,
        phone: String,
        ecocashNumber: String,
        institution: String,
        bio: String,
        role: UserRole
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val safeRole = if (role == UserRole.ADMIN && !isCurrentUserAdmin()) UserRole.CUSTOMER.name else role.name
            val updates = mapOf(
                "displayName" to displayName.trim().ifBlank { "Chinhoyi User" }.take(80),
                "phone" to phone.trim().take(30),
                "ecocashNumber" to ecocashNumber.trim().take(30),
                "institution" to institution.trim().take(120),
                "bio" to bio.trim().take(400),
                "role" to safeRole,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeAllUsersForAdmin(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        val path = "users"
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snap -> snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun adminUpdateUserAccess(
        targetUid: String,
        role: UserRole,
        landlordApproved: Boolean,
        accountActive: Boolean
    ): Result<Unit> {
        val path = "users/$targetUid"
        return try {
            requireUserId()
            db.collection("users").document(targetUid).update(
                mapOf(
                    "role" to role.name,
                    "landlordApproved" to landlordApproved,
                    "accountActive" to accountActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- PLATFORM CONFIG (ADMIN UPGRADABLE) ---

    fun observePlatformConfig(): Flow<PlatformConfig> = flow {
        val uid = requireUserId()
        val path = "platform_config/settings"
        emitAll(
            db.collection("platform_config").document("settings")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(PlatformConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?: PlatformConfig()
                    } else {
                        PlatformConfig()
                    }
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    suspend fun savePlatformConfig(config: PlatformConfig): Result<Unit> {
        val path = "platform_config/settings"
        return try {
            requireUserId()
            val payload = mapOf(
                "unlockFeeUsd" to config.unlockFeeUsd.coerceIn(0.0, 500.0),
                "unlockFeeZig" to config.unlockFeeZig.coerceIn(0.0, 100000.0),
                "ecocashNumber" to config.ecocashNumber.trim().ifBlank { "0771 234 567" }.take(30),
                "ecocashName" to config.ecocashName.trim().ifBlank { "Chinhoyi Admin" }.take(80),
                "maxPhotosPerHouse" to config.maxPhotosPerHouse.coerceIn(1L, 20L),
                "requireHouseApproval" to config.requireHouseApproval,
                "requireFeeForLocation" to config.requireFeeForLocation,
                "requireFeeForChat" to config.requireFeeForChat,
                "enableLiveVacancy" to config.enableLiveVacancy,
                "enableFeaturedBoost" to config.enableFeaturedBoost,
                "maintenanceMode" to config.maintenanceMode,
                "welcomeBannerText" to config.welcomeBannerText.trim().take(200),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("platform_config").document("settings").set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- BOARDING HOUSES ---

    fun observeApprovedHouses(): Flow<List<BoardingHouse>> = flow {
        val uid = requireUserId()
        val path = "houses"
        emitAll(
            db.collection("houses")
                .whereEqualTo("status", HouseStatus.APPROVED.name)
                .snapshots()
                .map { snap ->
                    snap.toObjects(BoardingHouse::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareByDescending<BoardingHouse> { it.featured }.thenBy { it.distanceToCutKm })
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeLandlordHouses(landlordUid: String): Flow<List<BoardingHouse>> = flow {
        val path = "houses"
        emitAll(
            db.collection("houses")
                .whereEqualTo("landlordUid", landlordUid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(BoardingHouse::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, landlordUid)
                    throw error
                }
        )
    }

    fun observeAllHousesForAdmin(): Flow<List<BoardingHouse>> = flow {
        val uid = requireUserId()
        val path = "houses"
        emitAll(
            db.collection("houses")
                .snapshots()
                .map { snap ->
                    snap.toObjects(BoardingHouse::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun submitBoardingHouse(
        houseId: String?,
        landlordName: String,
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
        presetCoverRef: String,
        autoApproveAsAdmin: Boolean
    ): Result<String> {
        var path = "houses"
        return try {
            val uid = requireUserId()
            val targetId = houseId ?: "house_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            path = "houses/$targetId"
            val docRef = db.collection("houses").document(targetId)
            val anchor = ChinhoyiGeo.anchorForSuburb(suburb)
            val dist = ChinhoyiGeo.distanceKm(ChinhoyiGeo.CUT_LAT, ChinhoyiGeo.CUT_LNG, exactLat, exactLng)
                .coerceIn(0.1, 99.0)

            // Upload gallery photos into subcollection first
            val photoRefs = mutableListOf<String>()
            uploadedPhotoDataUris.forEachIndexed { idx, dataUri ->
                val photoDocId = "photo_${idx}_${UUID.randomUUID().toString().take(8)}"
                db.collection("houses").document(targetId)
                    .collection("photos").document(photoDocId)
                    .set(
                        mapOf(
                            "houseId" to targetId,
                            "landlordUid" to uid,
                            "photoData" to dataUri.take(349000),
                            "caption" to "$name Photo ${idx + 1}".take(120),
                            "sortOrder" to idx.toLong(),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                photoRefs.add("sub:$photoDocId")
            }

            if (photoRefs.isEmpty()) {
                photoRefs.add(presetCoverRef)
                photoRefs.add("drawable:img_boarding_room")
            }

            val safeTotal = totalRooms.coerceIn(1L, 500L)
            val safeVacant = vacantRooms.coerceIn(0L, safeTotal)
            val coverRef = photoRefs.first()
            val statusValue = if (autoApproveAsAdmin && isCurrentUserAdmin()) HouseStatus.APPROVED.name else HouseStatus.PENDING.name

            val housePayload = mapOf(
                "landlordUid" to uid,
                "landlordName" to landlordName.trim().ifBlank { "Landlord" }.take(80),
                "name" to name.trim().take(100),
                "suburb" to suburb.trim().take(60),
                "description" to description.trim().take(1500),
                "monthlyRentUsd" to monthlyRentUsd.coerceIn(0.0, 10000.0),
                "depositUsd" to depositUsd.coerceIn(0.0, 10000.0),
                "totalRooms" to safeTotal,
                "vacantRooms" to safeVacant,
                "genderPolicy" to genderPolicy.name,
                "roomType" to roomType.name,
                "distanceToCutKm" to dist,
                "approxLat" to anchor.lat,
                "approxLng" to anchor.lng,
                "amenities" to amenities.take(15),
                "coverPhotoRef" to coverRef.take(500),
                "photoRefs" to photoRefs.take(12),
                "status" to statusValue,
                "featured" to false,
                "rating" to 4.8,
                "reviewCount" to 1L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(housePayload).await()

            // Save gated exact private location
            db.collection("houses").document(targetId)
                .collection("private").document("location")
                .set(
                    mapOf(
                        "houseId" to targetId,
                        "landlordUid" to uid,
                        "exactLat" to exactLat.coerceIn(-90.0, 90.0),
                        "exactLng" to exactLng.coerceIn(-180.0, 180.0),
                        "streetAddress" to streetAddress.trim().ifBlank { "Near $suburb, Chinhoyi" }.take(200),
                        "landmarks" to landmarks.trim().take(200),
                        "contactPhone" to contactPhone.trim().take(30),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            Result.success(targetId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateHouseVacancy(houseId: String, totalRooms: Long, newVacantRooms: Long): Result<Unit> {
        val path = "houses/$houseId"
        return try {
            requireUserId()
            val safeTotal = totalRooms.coerceIn(1L, 500L)
            val safeVacant = newVacantRooms.coerceIn(0L, safeTotal)
            db.collection("houses").document(houseId).update(
                mapOf(
                    "totalRooms" to safeTotal,
                    "vacantRooms" to safeVacant,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateHouseLocation(
        houseId: String,
        exactLat: Double,
        exactLng: Double,
        streetAddress: String,
        landmarks: String,
        contactPhone: String
    ): Result<Unit> {
        val path = "houses/$houseId/private/location"
        return try {
            requireUserId()
            db.collection("houses").document(houseId)
                .collection("private").document("location")
                .update(
                    mapOf(
                        "exactLat" to exactLat,
                        "exactLng" to exactLng,
                        "streetAddress" to streetAddress.trim().ifBlank { "Chinhoyi Residence" }.take(200),
                        "landmarks" to landmarks.trim().take(200),
                        "contactPhone" to contactPhone.trim().take(30),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            val dist = ChinhoyiGeo.distanceKm(ChinhoyiGeo.CUT_LAT, ChinhoyiGeo.CUT_LNG, exactLat, exactLng)
                .coerceIn(0.1, 99.0)
            db.collection("houses").document(houseId).update(
                mapOf(
                    "distanceToCutKm" to dist,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateHouseAdmission(
        houseId: String,
        status: HouseStatus,
        featured: Boolean
    ): Result<Unit> {
        val path = "houses/$houseId"
        return try {
            requireUserId()
            db.collection("houses").document(houseId).update(
                mapOf(
                    "status" to status.name,
                    "featured" to featured,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun getHousePrivateLocation(houseId: String): Result<HousePrivateLocation?> {
        val path = "houses/$houseId/private/location"
        return try {
            requireUserId()
            val snap = db.document(path).get().await()
            val loc = if (snap.exists()) {
                snap.toObject(HousePrivateLocation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
            Result.success(loc)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun getHousePhotos(houseId: String, photoRefs: List<String>): Result<Map<String, String>> {
        val resultMap = mutableMapOf<String, String>()
        return try {
            requireUserId()
            for (ref in photoRefs) {
                if (ref.startsWith("sub:")) {
                    val photoId = ref.removePrefix("sub:")
                    val snap = db.collection("houses").document(houseId)
                        .collection("photos").document(photoId).get().await()
                    val data = snap.getString("photoData")
                    if (!data.isNullOrBlank()) {
                        resultMap[ref] = data
                    }
                }
            }
            Result.success(resultMap)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "houses/$houseId/photos")
            Result.failure(e)
        }
    }

    suspend fun seedStarterChinhoyiHousesAsAdmin(adminName: String): Result<Unit> {
        return try {
            requireUserId()
            val starters = listOf(
                Triple("Jacaranda Haven Residence", "Cold Stream", 85.0) to Triple(4L, GenderPolicy.FEMALE_ONLY, "drawable:img_login_house_bg"),
                Triple("CUT View Executive Lodge", "Orange Grove", 110.0) to Triple(3L, GenderPolicy.MIXED, "drawable:img_boarding_exterior"),
                Triple("Manyame Quiet Study Suites", "Town Centre", 95.0) to Triple(2L, GenderPolicy.MALE_ONLY, "drawable:img_boarding_room"),
                Triple("Ruvimbo Green Palms House", "Ruvimbo", 70.0) to Triple(5L, GenderPolicy.MIXED, "drawable:img_boarding_exterior")
            )
            starters.forEachIndexed { index, pair ->
                val (info, meta) = pair
                val (title, suburb, rent) = info
                val (vacant, gender, cover) = meta
                val anchor = ChinhoyiGeo.anchorForSuburb(suburb)
                submitBoardingHouse(
                    houseId = "starter_house_${index + 1}",
                    landlordName = adminName,
                    name = title,
                    suburb = suburb,
                    description = "Clean, secure Chinhoyi boarding residence with reliable borehole water, solar inverter backup, study desks, and high-speed Wi-Fi.",
                    monthlyRentUsd = rent,
                    depositUsd = rent,
                    totalRooms = vacant + 6L,
                    vacantRooms = vacant,
                    genderPolicy = gender,
                    roomType = if (index == 1) RoomType.SINGLE else RoomType.SHARED_2,
                    amenities = listOf("Wi-Fi", "Solar Backup", "Borehole Water", "Security Wall", "Study Desk"),
                    exactLat = anchor.lat + 0.0012,
                    exactLng = anchor.lng - 0.0009,
                    streetAddress = "Stand ${140 + index * 17}, $suburb, Chinhoyi",
                    landmarks = "5 mins walk from $suburb Turnoff",
                    contactPhone = "0771 234 567",
                    uploadedPhotoDataUris = emptyList(),
                    presetCoverRef = cover,
                    autoApproveAsAdmin = true
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- REVIEWS ---

    fun observeHouseReviews(houseId: String): Flow<List<HouseReview>> = flow {
        val uid = requireUserId()
        val path = "houses/$houseId/reviews"
        emitAll(
            db.collection("houses").document(houseId)
                .collection("reviews")
                .whereEqualTo("houseId", houseId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(HouseReview::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun addHouseReview(houseId: String, authorName: String, rating: Long, comment: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val reviewId = "rev_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val path = "houses/$houseId/reviews/$reviewId"
        return try {
            db.document(path).set(
                mapOf(
                    "houseId" to houseId,
                    "authorUid" to uid,
                    "authorName" to authorName.trim().ifBlank { "Student" }.take(80),
                    "rating" to rating.coerceIn(1L, 5L),
                    "comment" to comment.trim().take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- ECOCASH PAYMENTS & UNLOCKS ---

    fun observeCustomerPayments(customerUid: String): Flow<List<PaymentRequest>> = flow {
        val path = "payment_requests"
        emitAll(
            db.collection("payment_requests")
                .whereEqualTo("customerUid", customerUid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PaymentRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, customerUid)
                    throw error
                }
        )
    }

    fun observeCustomerUnlocks(customerUid: String): Flow<List<HouseUnlock>> = flow {
        val path = "unlocks"
        emitAll(
            db.collection("unlocks")
                .whereEqualTo("customerUid", customerUid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(HouseUnlock::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, customerUid)
                    throw error
                }
        )
    }

    fun observeLandlordUnlocks(landlordUid: String): Flow<List<HouseUnlock>> = flow {
        val path = "unlocks"
        emitAll(
            db.collection("unlocks")
                .whereEqualTo("landlordUid", landlordUid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(HouseUnlock::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, landlordUid)
                    throw error
                }
        )
    }

    fun observeAllPaymentsForAdmin(): Flow<List<PaymentRequest>> = flow {
        val uid = requireUserId()
        val path = "payment_requests"
        emitAll(
            db.collection("payment_requests")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PaymentRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun submitEcoCashPayment(
        house: BoardingHouse,
        payerName: String,
        payerPhone: String,
        referenceCode: String,
        amountUsd: Double,
        currencyLabel: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val requestId = "pay_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "payment_requests/$requestId"
        return try {
            val email = auth.currentUser?.email ?: ""
            db.collection("payment_requests").document(requestId).set(
                mapOf(
                    "customerUid" to uid,
                    "customerEmail" to email.take(150),
                    "houseId" to house.id,
                    "houseName" to house.name.take(100),
                    "landlordUid" to house.landlordUid,
                    "payerName" to payerName.trim().take(100),
                    "payerPhone" to payerPhone.trim().take(30),
                    "referenceCode" to referenceCode.trim().take(80),
                    "amountUsd" to amountUsd.coerceIn(0.0, 500.0),
                    "currencyLabel" to if (currencyLabel == "ZiG") "ZiG" else "USD",
                    "status" to PaymentStatus.PENDING.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminVerifyAndUnlockPayment(payment: PaymentRequest): Result<Unit> {
        val path = "payment_requests/${payment.id}"
        return try {
            requireUserId()
            val unlockId = "${payment.customerUid}_${payment.houseId}"
            val batch = db.batch()
            val payRef = db.collection("payment_requests").document(payment.id)
            batch.update(
                payRef,
                mapOf(
                    "status" to PaymentStatus.UNLOCKED.name,
                    "adminNote" to "EcoCash verified for ${payment.payerName}".take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val unlockRef = db.collection("unlocks").document(unlockId)
            batch.set(
                unlockRef,
                mapOf(
                    "customerUid" to payment.customerUid,
                    "houseId" to payment.houseId,
                    "houseName" to payment.houseName.take(100),
                    "landlordUid" to payment.landlordUid,
                    "payerName" to payment.payerName.take(100),
                    "paymentRequestId" to payment.id,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun adminRejectPayment(paymentId: String, reason: String): Result<Unit> {
        val path = "payment_requests/$paymentId"
        return try {
            requireUserId()
            db.collection("payment_requests").document(paymentId).update(
                mapOf(
                    "status" to PaymentStatus.REJECTED.name,
                    "adminNote" to reason.trim().ifBlank { "EcoCash reference could not be matched." }.take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- CHAT THREADS & MESSAGES ---

    fun observeChatThreads(userId: String): Flow<List<ChatThread>> = flow {
        val path = "chat_threads"
        emitAll(
            db.collection("chat_threads")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeThreadMessages(threadId: String, currentUserId: String): Flow<List<ChatMessage>> = flow {
        val path = "chat_threads/$threadId/messages"
        emitAll(
            db.collection("chat_threads").document(threadId)
                .collection("messages")
                .whereArrayContains("memberUids", currentUserId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, currentUserId)
                    throw error
                }
        )
    }

    suspend fun openOrCreateChatThread(
        customerUid: String,
        customerName: String,
        house: BoardingHouse
    ): Result<ChatThread> {
        val threadId = "${customerUid}_${house.id}"
        val path = "chat_threads/$threadId"
        return try {
            requireUserId()
            val docRef = db.collection("chat_threads").document(threadId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(ChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (existing != null) return Result.success(existing)
            }
            val memberUids = listOf(customerUid, house.landlordUid)
            val payload = mapOf(
                "customerUid" to customerUid,
                "customerName" to customerName.trim().ifBlank { "Student" }.take(80),
                "landlordUid" to house.landlordUid,
                "landlordName" to house.landlordName.trim().ifBlank { "Landlord" }.take(80),
                "houseId" to house.id,
                "houseName" to house.name.trim().take(100),
                "memberUids" to memberUids,
                "lastMessage" to "Unlocked inquiry for ${house.name}".take(500),
                "lastSenderUid" to customerUid,
                "unreadByCustomer" to 0L,
                "unreadByLandlord" to 1L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            val created = docRef.get().await()
                .toObject(ChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: ChatThread(
                    id = threadId,
                    customerUid = customerUid,
                    customerName = customerName,
                    landlordUid = house.landlordUid,
                    landlordName = house.landlordName,
                    houseId = house.id,
                    houseName = house.name,
                    memberUids = memberUids
                )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(
        thread: ChatThread,
        senderName: String,
        text: String,
        imageDataUri: String? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val messageId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "chat_threads/${thread.id}/messages/$messageId"
        return try {
            val isImage = !imageDataUri.isNullOrBlank()
            val cleanText = text.trim().ifBlank { if (isImage) "Sent a photo" else "" }.take(1000)
            if (cleanText.isBlank() && !isImage) return Result.success(Unit)

            val msgPayload = mutableMapOf<String, Any>(
                "threadId" to thread.id,
                "senderUid" to uid,
                "senderName" to senderName.trim().ifBlank { "User" }.take(80),
                "memberUids" to thread.memberUids,
                "text" to cleanText,
                "messageType" to if (isImage) "IMAGE" else "TEXT",
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (isImage && imageDataUri != null) {
                msgPayload["imageData"] = imageDataUri.take(349000)
            }

            val batch = db.batch()
            val msgRef = db.collection("chat_threads").document(thread.id)
                .collection("messages").document(messageId)
            batch.set(msgRef, msgPayload)

            val threadRef = db.collection("chat_threads").document(thread.id)
            val isSenderCustomer = uid == thread.customerUid
            batch.update(
                threadRef,
                mapOf(
                    "lastMessage" to cleanText.take(500),
                    "lastSenderUid" to uid,
                    "unreadByCustomer" to if (isSenderCustomer) 0L else thread.unreadByCustomer + 1L,
                    "unreadByLandlord" to if (isSenderCustomer) thread.unreadByLandlord + 1L else 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun markThreadRead(thread: ChatThread): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val path = "chat_threads/${thread.id}"
        return try {
            val isCustomer = uid == thread.customerUid
            val updates = if (isCustomer && thread.unreadByCustomer > 0L) {
                mapOf("unreadByCustomer" to 0L, "updatedAt" to FieldValue.serverTimestamp())
            } else if (!isCustomer && thread.unreadByLandlord > 0L) {
                mapOf("unreadByLandlord" to 0L, "updatedAt" to FieldValue.serverTimestamp())
            } else {
                return Result.success(Unit)
            }
            db.collection("chat_threads").document(thread.id).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private fun logStreamError(error: Throwable, op: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }
}
