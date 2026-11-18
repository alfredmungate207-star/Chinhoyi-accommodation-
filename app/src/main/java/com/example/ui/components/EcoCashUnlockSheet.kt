package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.BoardingHouse
import com.example.data.PaymentRequest
import com.example.data.PaymentStatus
import com.example.data.PlatformConfig
import com.example.ui.theme.AmberSoft
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.CoralSoft
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.IndigoDeep
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoSoft
import com.example.ui.theme.SlateBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcoCashUnlockSheet(
    house: BoardingHouse,
    config: PlatformConfig,
    defaultPayerName: String,
    defaultPayerPhone: String = "",
    existingPayment: PaymentRequest? = null,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmitPayment: (payerName: String, payerPhone: String, referenceCode: String, currency: String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var copiedMerchant by remember { mutableStateOf(false) }

    var payerName by remember(defaultPayerName) { mutableStateOf(defaultPayerName) }
    var payerPhone by remember(defaultPayerPhone) { mutableStateOf(defaultPayerPhone) }
    var referenceCode by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("USD") }

    val existingStatus = existingPayment?.let { PaymentStatus.fromRaw(it.status) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isFormValid = payerName.trim().length >= 2 &&
        payerPhone.trim().length >= 6 &&
        referenceCode.trim().length >= 3

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(IndigoSoft, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LockOpen,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "EcoCash Verification",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Unlock ${house.name} GPS pin & Landlord chat",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close verification")
                }
            }

            // Live Firestore verification state banner when a request already exists
            if (existingPayment != null && existingStatus != null) {
                val (bannerBg, bannerBorder, bannerFg, bannerTitle) = when (existingStatus) {
                    PaymentStatus.PENDING -> listOf(
                        AmberSoft,
                        Color(0xFFF59E0B),
                        Color(0xFF92400E),
                        "Pending Admin Approval"
                    )
                    PaymentStatus.REJECTED -> listOf(
                        CoralSoft,
                        CoralDanger,
                        Color(0xFF991B1B),
                        "Verification Rejected — Re-submit Below"
                    )
                    PaymentStatus.UNLOCKED -> listOf(
                        EmeraldSoft,
                        EmeraldAccent,
                        Color(0xFF065F46),
                        "Payment Verified & Unlocked"
                    )
                }
                Surface(
                    color = bannerBg as Color,
                    border = BorderStroke(1.dp, bannerBorder as Color),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("ecocash_existing_status_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (existingStatus == PaymentStatus.PENDING) Icons.Filled.HourglassTop else Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = bannerFg as Color
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(bannerTitle as String, fontWeight = FontWeight.Bold, color = bannerFg)
                            Text(
                                text = "Sender: ${existingPayment.payerName} • Phone: ${existingPayment.payerPhone} • Ref: ${existingPayment.referenceCode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = bannerFg
                            )
                            if (existingPayment.adminNote.isNotBlank()) {
                                Text(
                                    text = existingPayment.adminNote,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = bannerFg
                                )
                            }
                        }
                    }
                }
            }

            // Merchant instructions card
            Card(
                colors = CardDefaults.cardColors(containerColor = IndigoSoft),
                border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
                            Text("Send EcoCash Unlock Fee To:", style = MaterialTheme.typography.labelLarge, color = IndigoDeep, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(config.ecocashNumber))
                                copiedMerchant = true
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = if (copiedMerchant) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                contentDescription = "Copy EcoCash number",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (copiedMerchant) "Copied" else "Copy", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text(
                        text = "${config.ecocashNumber}  •  ${config.ecocashName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Fee: $${String.format("%.2f", config.unlockFeeUsd)} USD  or  ZiG ${String.format("%.0f", config.unlockFeeZig)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF065F46),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(EmeraldSoft, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedCurrency == "USD",
                    onClick = { selectedCurrency = "USD" },
                    label = { Text("USD ($${String.format("%.2f", config.unlockFeeUsd)})", fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = selectedCurrency == "ZiG",
                    onClick = { selectedCurrency = "ZiG" },
                    label = { Text("ZiG (${String.format("%.0f", config.unlockFeeZig)})", fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }

            OutlinedTextField(
                value = payerName,
                onValueChange = { payerName = it },
                label = { Text("EcoCash Sender Full Name (Required)") },
                placeholder = { Text("Exact name shown on EcoCash SMS") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ecocash_payer_name_input")
            )

            OutlinedTextField(
                value = payerPhone,
                onValueChange = { payerPhone = it },
                label = { Text("EcoCash Sender Phone Number") },
                placeholder = { Text("077X XXX XXX") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ecocash_payer_phone_input")
            )

            OutlinedTextField(
                value = referenceCode,
                onValueChange = { referenceCode = it },
                label = { Text("EcoCash Transaction Ref / Approval Code") },
                placeholder = { Text("e.g. MP261117.1542.A9832") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ecocash_ref_input")
            )

            Surface(
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Your submission enters a PENDING verification queue in Firestore. Once Admin verifies your Sender Name & Reference, exact GPS location and Landlord chat unlock immediately.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Button(
                onClick = { onSubmitPayment(payerName.trim(), payerPhone.trim(), referenceCode.trim(), selectedCurrency) },
                enabled = !isBusy && isFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_ecocash_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (existingStatus == PaymentStatus.PENDING) "Submit Another EcoCash Reference" else "Submit for Admin Verification",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
