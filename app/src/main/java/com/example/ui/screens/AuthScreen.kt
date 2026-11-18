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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.AuthManager
import com.example.data.RegistrationDraft
import com.example.data.UserRole
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.IndigoDeep
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoSoft
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.TealBrand

private enum class AuthPageMode {
    SIGN_IN,
    CREATE_ACCOUNT
}

@Composable
fun AuthScreen(
    selectedRole: UserRole,
    onRoleSelected: (UserRole) -> Unit,
    onRegistrationDraftReady: (RegistrationDraft?) -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var pageMode by remember { mutableStateOf(AuthPageMode.SIGN_IN) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Create Account onboarding fields
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("Chinhoyi University of Technology") }
    var createAccountRole by remember { mutableStateOf(UserRole.CUSTOMER) }

    // Silent auto sign-in on cold start if already authorized
    LaunchedEffect(Unit) {
        AuthManager.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Real Chinhoyi Accommodation House Background
        Image(
            painter = painterResource(id = R.drawable.img_login_house_bg_1794959705044),
            contentDescription = "Chinhoyi Boarding House Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Rich atmospheric gradient overlay for legibility while preserving house architecture
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xB81E2B7A),
                            Color(0x88364FE0),
                            Color(0xD9F6F8FC)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Circular "Chinhoyi Accommodations" Emblem
            Box(
                modifier = Modifier
                    .size(148.dp)
                    .shadow(16.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White, Color(0xFFEEF2FF))
                        )
                    )
                    .border(4.dp, Color.White.copy(alpha = 0.9f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(IndigoPrimary, TealBrand))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.HomeWork,
                            contentDescription = "Chinhoyi Accommodations Logo",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "CHINHOYI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = IndigoDeep,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "ACCOMMODATIONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TealBrand,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Rounded Card inspired by Template #1 (Welcome) & Template #4 (Welcome Back / Create Account)
            Card(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Template #4 Top Sign In / Create Account Switch Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = if (pageMode == AuthPageMode.SIGN_IN) {
                                        listOf(IndigoDeep, IndigoPrimary)
                                    } else {
                                        listOf(TealBrand, EmeraldAccent)
                                    }
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ModePillButton(
                                    label = "Sign In",
                                    selected = pageMode == AuthPageMode.SIGN_IN,
                                    onClick = {
                                        errorMessage = null
                                        pageMode = AuthPageMode.SIGN_IN
                                    },
                                    modifier = Modifier.weight(1f).testTag("auth_mode_sign_in")
                                )
                                ModePillButton(
                                    label = "Create Account",
                                    selected = pageMode == AuthPageMode.CREATE_ACCOUNT,
                                    onClick = {
                                        errorMessage = null
                                        pageMode = AuthPageMode.CREATE_ACCOUNT
                                    },
                                    modifier = Modifier.weight(1f).testTag("auth_mode_create_account")
                                )
                            }

                            Text(
                                text = if (pageMode == AuthPageMode.SIGN_IN) "Welcome Back!" else "Create Account",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = if (pageMode == AuthPageMode.SIGN_IN) {
                                    "Choose Student, Landlord, or Admin login below."
                                } else {
                                    "Set up your Chinhoyi Stays profile before linking Google."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.92f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (pageMode == AuthPageMode.SIGN_IN) {
                            Text(
                                text = "Select Login Portal",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = selectedRole == UserRole.CUSTOMER,
                                    onClick = { onRoleSelected(UserRole.CUSTOMER) },
                                    label = { Text("Student", maxLines = 1) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(15.dp))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IndigoSoft,
                                        selectedLabelColor = IndigoDeep
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("role_customer_chip")
                                )
                                FilterChip(
                                    selected = selectedRole == UserRole.LANDLORD,
                                    onClick = { onRoleSelected(UserRole.LANDLORD) },
                                    label = { Text("Landlord", maxLines = 1) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Apartment, contentDescription = null, modifier = Modifier.size(15.dp))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldSoft,
                                        selectedLabelColor = Color(0xFF065F46)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("role_landlord_chip")
                                )
                                FilterChip(
                                    selected = selectedRole == UserRole.ADMIN,
                                    onClick = { onRoleSelected(UserRole.ADMIN) },
                                    label = { Text("Admin", maxLines = 1) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(15.dp))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IndigoDeep,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("role_admin_chip")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Contextual Portal Info Banner
                            Surface(
                                color = if (selectedRole == UserRole.ADMIN) Color(0xFFEEF2FF) else IndigoSoft.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedRole == UserRole.ADMIN) IndigoPrimary else IndigoPrimary.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (selectedRole == UserRole.ADMIN) Icons.Filled.AdminPanelSettings else Icons.Filled.HomeWork,
                                        contentDescription = null,
                                        tint = IndigoDeep,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = when (selectedRole) {
                                            UserRole.ADMIN -> "Admin Login: Sign in with ${AuthManager.BOOTSTRAPPED_ADMIN_EMAIL} to accept house admissions, verify EcoCash payer names & upgrade features."
                                            UserRole.LANDLORD -> "Landlord Portal: Post house photos, set live GPS pins & toggle room vacancies in real time."
                                            UserRole.CUSTOMER -> "Student / Stay: View boarding house photos free & unlock exact GPS + Landlord chat via EcoCash."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = IndigoDeep
                                    )
                                }
                            }
                        } else {
                            // Dedicated Create Account Form (Template #4 Inspired)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it },
                                    label = { Text("Full Name") },
                                    placeholder = { Text("e.g. Tatenda Moyo") },
                                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("create_account_name_input")
                                )

                                OutlinedTextField(
                                    value = phoneNumber,
                                    onValueChange = { phoneNumber = it },
                                    label = { Text("Phone / EcoCash Number") },
                                    placeholder = { Text("e.g. 0772 123 456") },
                                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("create_account_phone_input")
                                )

                                OutlinedTextField(
                                    value = institution,
                                    onValueChange = { institution = it },
                                    label = { Text("University / Institution") },
                                    leadingIcon = { Icon(Icons.Filled.School, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("create_account_institution_input")
                                )

                                Text(
                                    text = "Register As",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = createAccountRole == UserRole.CUSTOMER,
                                        onClick = { createAccountRole = UserRole.CUSTOMER },
                                        label = { Text("Student / Stay", maxLines = 1) },
                                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = IndigoSoft,
                                            selectedLabelColor = IndigoDeep
                                        ),
                                        modifier = Modifier.weight(1f).testTag("create_role_customer")
                                    )
                                    FilterChip(
                                        selected = createAccountRole == UserRole.LANDLORD,
                                        onClick = { createAccountRole = UserRole.LANDLORD },
                                        label = { Text("Landlord", maxLines = 1) },
                                        leadingIcon = { Icon(Icons.Filled.Apartment, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldSoft,
                                            selectedLabelColor = Color(0xFF065F46)
                                        ),
                                        modifier = Modifier.weight(1f).testTag("create_role_landlord")
                                    )
                                }
                            }
                        }

                        if (!errorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val isCreateMode = pageMode == AuthPageMode.CREATE_ACCOUNT
                        Button(
                            onClick = {
                                errorMessage = null
                                if (isCreateMode) {
                                    if (fullName.trim().length < 2) {
                                        errorMessage = "Please enter your full name to create your account."
                                        return@Button
                                    }
                                    if (phoneNumber.trim().length < 6) {
                                        errorMessage = "Please enter your phone / EcoCash number."
                                        return@Button
                                    }
                                    onRoleSelected(createAccountRole)
                                    onRegistrationDraftReady(
                                        RegistrationDraft(
                                            fullName = fullName.trim(),
                                            phone = phoneNumber.trim(),
                                            institution = institution.trim().ifBlank { "Chinhoyi University of Technology" },
                                            role = createAccountRole,
                                            isSignUpFlow = true
                                        )
                                    )
                                } else {
                                    onRegistrationDraftReady(null)
                                }

                                isLoading = true
                                AuthManager.onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isLoading = false
                                    }
                                )
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCreateMode) TealBrand else IndigoDeep
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Icon(
                                    imageVector = when {
                                        isCreateMode -> Icons.Filled.PersonAdd
                                        selectedRole == UserRole.ADMIN -> Icons.Filled.AdminPanelSettings
                                        else -> Icons.Filled.Lock
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = when {
                                        isCreateMode -> "Create Account with Google"
                                        selectedRole == UserRole.ADMIN -> "Admin Sign In with Google"
                                        selectedRole == UserRole.LANDLORD -> "Landlord Sign In with Google"
                                        else -> "Sign in with Google"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        TextButton(
                            onClick = {
                                errorMessage = null
                                pageMode = if (isCreateMode) AuthPageMode.SIGN_IN else AuthPageMode.CREATE_ACCOUNT
                            },
                            modifier = Modifier.testTag("toggle_auth_page_mode")
                        ) {
                            Text(
                                text = if (isCreateMode) {
                                    "Already have an account? Sign In"
                                } else {
                                    "New to Chinhoyi Stays? Create Account"
                                },
                                color = IndigoPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FeatureMiniBadge("Free Photos")
                            FeatureMiniBadge("Live Vacancy")
                            FeatureMiniBadge("EcoCash Verified")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModePillButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (selected) Color.White else Color.Transparent,
        shape = RoundedCornerShape(11.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) IndigoDeep else Color.White
            )
        }
    }
}

@Composable
private fun FeatureMiniBadge(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = EmeraldAccent,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
