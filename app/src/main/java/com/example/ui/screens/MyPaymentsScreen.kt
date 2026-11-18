package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.TealBrand
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Dedicated 'My Payments' screen for customers to track EcoCash verification status
 * (PENDING / UNLOCKED / REJECTED) and browse their full transaction history.
 */
@Composable
fun MyPaymentsScreen(
    payments: List<PaymentRequest>,
    houses: List<BoardingHouse>,
    config: PlatformConfig,
    onOpenHouseMap: (houseId: String) -> Unit,
    onResubmitPayment: (BoardingHouse) -> Unit,
    onBrowseHouses: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<PaymentStatus?>(null) }

    val houseById = remember(houses) { houses.associateBy { it.id } }

    val pendingCount = remember(payments) { payments.count { it.status == PaymentStatus.PENDING.name } }
    val unlockedCount = remember(payments) { payments.count { it.status == PaymentStatus.UNLOCKED.name } }
    val rejectedCount = remember(payments) { payments.count { it.status == PaymentStatus.REJECTED.name } }

    val filteredPayments = remember(payments, searchQuery, selectedStatus) {
        payments.filter { pay ->
            val matchesStatus = selectedStatus == null || pay.status == selectedStatus?.name
            val matchesQuery = searchQuery.isBlank() ||
                pay.houseName.contains(searchQuery, ignoreCase = true) ||
                pay.referenceCode.contains(searchQuery, ignoreCase = true) ||
                pay.payerName.contains(searchQuery, ignoreCase = true) ||
                pay.payerPhone.contains(searchQuery, ignoreCase = true)
            matchesStatus && matchesQuery
        }.sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Gradient Header matching Home & Landlord Studio
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(IndigoDeep, IndigoPrimary)))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "My Payments",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Track EcoCash unlock verification status & receipts",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.88f)
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
                            Text("Standard Fee", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
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
                    placeholder = { Text("Search house, EcoCash ref, or sender...") },
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
                    modifier = Modifier.fillMaxWidth().testTag("my_payments_search_input")
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary KPI Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PaymentSummaryKpi(
                        title = "Pending",
                        value = pendingCount.toString(),
                        icon = Icons.Filled.HourglassTop,
                        bg = AmberSoft,
                        fg = Color(0xFF92400E),
                        onClick = { selectedStatus = if (selectedStatus == PaymentStatus.PENDING) null else PaymentStatus.PENDING },
                        modifier = Modifier.weight(1f)
                    )
                    PaymentSummaryKpi(
                        title = "Unlocked",
                        value = unlockedCount.toString(),
                        icon = Icons.Filled.Verified,
                        bg = EmeraldSoft,
                        fg = Color(0xFF065F46),
                        onClick = { selectedStatus = if (selectedStatus == PaymentStatus.UNLOCKED) null else PaymentStatus.UNLOCKED },
                        modifier = Modifier.weight(1f)
                    )
                    PaymentSummaryKpi(
                        title = "Rejected",
                        value = rejectedCount.toString(),
                        icon = Icons.Filled.ErrorOutline,
                        bg = CoralSoft,
                        fg = Color(0xFF991B1B),
                        onClick = { selectedStatus = if (selectedStatus == PaymentStatus.REJECTED) null else PaymentStatus.REJECTED },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Status Filter Chips
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = { selectedStatus = null },
                        label = { Text("All (${payments.size})", fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("payments_filter_all")
                    )
                    PaymentStatus.entries.forEach { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = st },
                            label = { Text(st.label, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("payments_filter_${st.name.lowercase()}")
                        )
                    }
                }
            }

            if (filteredPayments.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, SlateBorder),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(64.dp).clip(CircleShape).background(IndigoSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(32.dp))
                            }
                            Text(
                                text = if (payments.isEmpty()) "No EcoCash Transactions Yet" else "No Matching Transactions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (payments.isEmpty()) {
                                    "When you submit an EcoCash unlock payment for a boarding house, you can monitor Admin approval and view every receipt here."
                                } else {
                                    "Try clearing your search or status filter."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            if (payments.isEmpty()) {
                                Button(
                                    onClick = onBrowseHouses,
                                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("payments_browse_houses_button")
                                ) {
                                    Text("Browse Boarding Houses", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            items(filteredPayments, key = { it.id }) { pay ->
                val status = PaymentStatus.fromRaw(pay.status)
                val (badgeBg, badgeFg, statusIcon) = when (status) {
                    PaymentStatus.UNLOCKED -> Triple(EmeraldSoft, Color(0xFF065F46), Icons.Filled.CheckCircle)
                    PaymentStatus.PENDING -> Triple(AmberSoft, Color(0xFF92400E), Icons.Filled.HourglassTop)
                    PaymentStatus.REJECTED -> Triple(CoralSoft, Color(0xFF991B1B), Icons.Filled.ErrorOutline)
                }
                val formattedDate = pay.createdAt?.toDate()?.let { dateFormatter.format(it) } ?: "Just submitted"
                val linkedHouse = houseById[pay.houseId]

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(
                        width = if (status == PaymentStatus.PENDING) 1.5.dp else 1.dp,
                        color = if (status == PaymentStatus.PENDING) Color(0xFFF59E0B) else SlateBorder
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().testTag("my_payment_card_${pay.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = pay.houseName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                            Surface(color = badgeBg, shape = RoundedCornerShape(10.dp)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(statusIcon, contentDescription = null, tint = badgeFg, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = status.label,
                                        color = badgeFg,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Sender Name", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(pay.payerName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Amount", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(
                                    text = if (pay.currencyLabel == "ZiG") "ZiG ${pay.amountUsd.toInt()}" else "$${String.format("%.2f", pay.amountUsd)} USD",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TealBrand
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("EcoCash Phone", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(pay.payerPhone, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Reference Code", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(pay.referenceCode, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = IndigoDeep)
                            }
                        }

                        if (pay.adminNote.isNotBlank()) {
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Admin Note: ${pay.adminNote}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        when (status) {
                            PaymentStatus.PENDING -> {
                                Surface(
                                    color = AmberSoft.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Awaiting Admin confirmation of your EcoCash transfer. Exact GPS location & Landlord chat unlock automatically once verified.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF78350F),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                            PaymentStatus.UNLOCKED -> {
                                Button(
                                    onClick = { onOpenHouseMap(pay.houseId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                ) {
                                    Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("View Unlocked GPS Location", fontWeight = FontWeight.Bold)
                                }
                            }
                            PaymentStatus.REJECTED -> {
                                if (linkedHouse != null) {
                                    OutlinedButton(
                                        onClick = { onResubmitPayment(linkedHouse) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(44.dp)
                                    ) {
                                        Icon(Icons.Filled.Refresh, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Re-submit EcoCash Reference", color = IndigoPrimary, fontWeight = FontWeight.Bold)
                                    }
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
private fun PaymentSummaryKpi(
    title: String,
    value: String,
    icon: ImageVector,
    bg: Color,
    fg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = bg,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = fg, fontWeight = FontWeight.ExtraBold)
            Text(title, style = MaterialTheme.typography.labelSmall, color = fg, fontWeight = FontWeight.SemiBold)
        }
    }
}
