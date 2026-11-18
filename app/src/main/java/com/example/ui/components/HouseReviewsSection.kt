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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BoardingHouse
import com.example.data.HouseReview
import com.example.ui.theme.AmberSoft
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.IndigoDeep
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoSoft
import com.example.ui.theme.SlateBorder
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Reusable Star-Rating & Verified Customer Review component for Boarding Houses.
 * Displays live average star ratings, individual verified feedback cards, and an
 * interactive 1-5 star review composer gated to verified customers who unlocked the house.
 */
@Composable
fun HouseReviewsSection(
    house: BoardingHouse,
    reviews: List<HouseReview>,
    canReview: Boolean,
    onSubmitReview: (rating: Long, comment: String) -> Unit,
    maxVisibleReviews: Int = 5,
    modifier: Modifier = Modifier
) {
    var selectedStars by remember(house.id) { mutableIntStateOf(5) }
    var commentText by remember(house.id) { mutableStateOf("") }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val displayAverage = remember(reviews, house.rating) {
        if (reviews.isNotEmpty()) {
            reviews.map { it.rating.toDouble() }.average()
        } else {
            house.rating
        }
    }
    val displayCount = remember(reviews, house.reviewCount) {
        if (reviews.isNotEmpty()) reviews.size.toLong() else house.reviewCount
    }

    val ratingDescription = when (selectedStars) {
        1 -> "Poor"
        2 -> "Fair"
        3 -> "Good"
        4 -> "Very Good"
        else -> "Excellent"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Rating Summary Card
        Surface(
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(color = AmberSoft, shape = RoundedCornerShape(12.dp)) {
                        Text(
                            text = String.format("%.1f", displayAverage),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            for (star in 1..5) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = if (star <= displayAverage.toInt()) AmberWarning else Color(0xFFD1D5DB),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "$displayCount verified review(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                Surface(color = EmeraldSoft, shape = RoundedCornerShape(10.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = Color(0xFF065F46), modifier = Modifier.size(13.dp))
                        Text(
                            text = "Verified Stays",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46)
                        )
                    }
                }
            }
        }

        // Verified Customer Interactive Star Rating & Review Composer
        if (canReview) {
            Surface(
                color = IndigoSoft.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.RateReview, contentDescription = null, tint = IndigoDeep, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Rate Your Stay & Landlord",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = IndigoDeep
                            )
                        }
                        Surface(color = Color.White, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = "$selectedStars/5 • $ratingDescription",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = IndigoPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (star in 1..5) {
                            IconButton(
                                onClick = { selectedStars = star },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("review_star_${house.id}_$star")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "Rate $star stars",
                                    tint = if (star <= selectedStars) AmberWarning else Color(0xFFD1D5DB),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("How was the landlord, rooms, Wi-Fi, water & security?") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .testTag("review_comment_input_${house.id}")
                    )

                    Button(
                        onClick = {
                            onSubmitReview(selectedStars.toLong(), commentText.trim())
                            commentText = ""
                        },
                        enabled = commentText.trim().length >= 2,
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("submit_review_button_${house.id}")
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Post $selectedStars-Star Verified Review", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Surface(
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Only verified customers who have unlocked this boarding house and interacted with the landlord can post a star rating and review.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Individual Reviews List
        if (reviews.isEmpty()) {
            Text(
                text = "No written reviews yet. Be the first verified resident to share feedback!",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        } else {
            reviews.take(maxVisibleReviews).forEach { rev ->
                val dateLabel = rev.createdAt?.toDate()?.let { dateFormatter.format(it) } ?: "Verified stay"
                Surface(
                    color = Color.White,
                    border = BorderStroke(1.dp, SlateBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.size(30.dp).clip(CircleShape).background(IndigoSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (rev.authorName.firstOrNull() ?: 'S').uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = IndigoDeep
                                    )
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(rev.authorName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Filled.Verified, contentDescription = "Verified customer", tint = Color(0xFF0D9488), modifier = Modifier.size(13.dp))
                                    }
                                    Text(dateLabel, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                for (s in 1..5) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = if (s <= rev.rating) AmberWarning else Color(0xFFE5E7EB),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = rev.comment,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF334155)
                        )
                    }
                }
            }
        }
    }
}
