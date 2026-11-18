package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.HouseUnlock
import com.example.data.PaymentRequest
import com.example.data.PaymentStatus
import com.example.data.UserProfile
import com.example.data.UserRole
import com.example.ui.theme.AmberSoft
import com.example.ui.theme.CoralSoft
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.IndigoDeep
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoSoft
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.TealBrand

/**
 * Profile Info Screen copied & beautified from Template #2 (MatDash User Profile):
 * - Breadcrumb bar ("User Profile" / Home > User Profile)
 * - Silky 3D Wave Header Banner with centered overlapping circular avatar
 * - 3 Stats columns (Unlocked, Chats, Status) + Edit / Sign Out buttons
 * - Sub-tabs (Profile, Unlocked Stays, EcoCash Payments)
 * - Introduction Card (Institution, Email, Phone/EcoCash, Chinhoyi Zimbabwe) + Editable Bio/Status card
 */
@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    unlockedCount: Int,
    chatCount: Int,
    unlocks: List<HouseUnlock>,
    payments: List<PaymentRequest>,
    isAdmin: Boolean,
    isBusy: Boolean,
    onSaveProfile: (displayName: String, phone: String, ecocashNumber: String, institution: String, bio: String, role: UserRole) -> Unit,
    onOpenMyPayments: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var isEditing by remember { mutableStateOf(false) }

    var nameField by remember(userProfile) { mutableStateOf(userProfile?.displayName ?: "") }
    var phoneField by remember(userProfile) { mutableStateOf(userProfile?.phone ?: "") }
    var ecocashField by remember(userProfile) { mutableStateOf(userProfile?.ecocashNumber ?: "") }
    var institutionField by remember(userProfile) { mutableStateOf(userProfile?.institution ?: "Chinhoyi University of Technology") }
    var bioField by remember(userProfile) { mutableStateOf(userProfile?.bio ?: "") }
    var roleField by remember(userProfile) { mutableStateOf(UserRole.fromRaw(userProfile?.role)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Template #2 Breadcrumb Header Bar
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, SlateBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("User Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Home, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Text("/", color = Color.Gray)
                    Surface(color = IndigoSoft, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "User Profile",
                            style = MaterialTheme.typography.labelMedium,
                            color = IndigoPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Template #2 Main Profile Hero Card with Wave Banner & Overlapping Avatar
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, SlateBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.fillMaxWidth().height(155.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_profile_wave_banner_1794959703911),
                        contentDescription = "Profile Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(128.dp)
                    )

                    // Overlapping circular avatar
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .align(Alignment.BottomCenter)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(IndigoSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        val avatarUrl = userProfile?.avatarRef.orEmpty()
                        if (avatarUrl.startsWith("http")) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "User Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = (userProfile?.displayName?.firstOrNull() ?: 'C').uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = IndigoDeep,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = userProfile?.displayName?.ifBlank { "Chinhoyi Resident" } ?: "Chinhoyi Resident",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isAdmin) "Platform Admin" else UserRole.fromRaw(userProfile?.role).label,
                    style = MaterialTheme.typography.bodySmall,
                    color = IndigoPrimary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3 Stats Columns (Template #2)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStatItem(unlockedCount.toString(), "Unlocked")
                    ProfileStatItem(chatCount.toString(), "Chats")
                    ProfileStatItem(payments.size.toString(), "Payments")
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { isEditing = !isEditing },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isEditing) "Cancel Edit" else "Edit Profile")
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Template #2 lavender sub-tab strip
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = IndigoSoft.copy(alpha = 0.65f),
                    contentColor = IndigoDeep
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Profile") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Unlocked") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Payments") })
                }
            }
        }

        when (selectedTab) {
            0 -> {
                // Introduction Card (Template #2)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Introduction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = userProfile?.bio?.ifBlank { "Student & resident in Chinhoyi." } ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        InfoLineRow(Icons.Filled.School, userProfile?.institution ?: "Chinhoyi University of Technology")
                        InfoLineRow(Icons.Filled.Email, userProfile?.email ?: "")
                        InfoLineRow(Icons.Filled.Phone, userProfile?.phone?.ifBlank { "Add phone number" } ?: "Add phone number")
                        InfoLineRow(Icons.Filled.Payments, "EcoCash: ${userProfile?.ecocashNumber?.ifBlank { "Not set" } ?: "Not set"}")
                        InfoLineRow(Icons.Filled.LocationOn, userProfile?.locationCity ?: "Chinhoyi, Zimbabwe")
                    }
                }

                if (isEditing) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, SlateBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Update Your Details & Role", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = nameField, onValueChange = { nameField = it }, label = { Text("Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = phoneField, onValueChange = { phoneField = it }, label = { Text("Phone Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = ecocashField, onValueChange = { ecocashField = it }, label = { Text("EcoCash Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = institutionField, onValueChange = { institutionField = it }, label = { Text("University / Workplace") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = bioField, onValueChange = { bioField = it }, label = { Text("Bio / About") }, minLines = 2, modifier = Modifier.fillMaxWidth())

                            if (!isAdmin) {
                                Text("Account Mode", style = MaterialTheme.typography.labelLarge)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = roleField == UserRole.CUSTOMER,
                                        onClick = { roleField = UserRole.CUSTOMER },
                                        label = { Text("Student / Customer") }
                                    )
                                    FilterChip(
                                        selected = roleField == UserRole.LANDLORD,
                                        onClick = { roleField = UserRole.LANDLORD },
                                        label = { Text("Landlord") }
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    onSaveProfile(nameField, phoneField, ecocashField, institutionField, bioField, roleField)
                                    isEditing = false
                                },
                                enabled = !isBusy,
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Save Profile", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            1 -> {
                if (unlocks.isEmpty()) {
                    EmptyTabCard(Icons.Filled.LockOpen, "No Unlocked Houses Yet", "Pay the EcoCash unlock fee on any house to reveal its exact GPS pin & landlord chat.")
                } else {
                    unlocks.forEach { u ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SlateBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Filled.LockOpen, contentDescription = null, tint = TealBrand)
                                Column {
                                    Text(u.houseName, fontWeight = FontWeight.Bold)
                                    Text("Verified payer: ${u.payerName}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                Button(
                    onClick = onOpenMyPayments,
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp).testTag("profile_open_my_payments_button")
                ) {
                    Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Full My Payments Page", fontWeight = FontWeight.Bold)
                }
                if (payments.isEmpty()) {
                    EmptyTabCard(Icons.Filled.Payments, "No EcoCash Submissions", "Your EcoCash fee submissions and approval status appear here.")
                } else {
                    payments.forEach { p ->
                        val st = PaymentStatus.fromRaw(p.status)
                        val (bg, fg) = when (st) {
                            PaymentStatus.UNLOCKED -> EmeraldSoft to Color(0xFF065F46)
                            PaymentStatus.PENDING -> AmberSoft to Color(0xFF92400E)
                            PaymentStatus.REJECTED -> CoralSoft to Color(0xFF991B1B)
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SlateBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p.houseName, fontWeight = FontWeight.Bold)
                                    Text("${p.payerName} • Ref: ${p.referenceCode}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
                                    Text(st.label, color = fg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
private fun InfoLineRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyTabCard(icon: ImageVector, title: String, desc: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(32.dp))
            Text(title, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}
