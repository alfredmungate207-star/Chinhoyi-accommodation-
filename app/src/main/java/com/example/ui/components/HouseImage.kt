package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoSoft

@Composable
fun HouseImage(
    photoRef: String,
    photoCache: Map<String, String>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val resolved = photoCache[photoRef] ?: photoRef
    when {
        resolved.startsWith("drawable:") -> {
            val resId = when (resolved.removePrefix("drawable:")) {
                "img_login_house_bg" -> R.drawable.img_login_house_bg_1794959705044
                "img_boarding_room" -> R.drawable.img_boarding_room_1794959705790
                "img_profile_wave_banner" -> R.drawable.img_profile_wave_banner_1794959703911
                else -> R.drawable.img_boarding_exterior_1794959704694
            }
            Image(
                painter = painterResource(id = resId),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        resolved.startsWith("data:image") -> {
            val bitmap = remember(resolved) {
                try {
                    val base64Part = resolved.substringAfter("base64,", "")
                    val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                } catch (_: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = contentDescription,
                    modifier = modifier,
                    contentScale = contentScale
                )
            } else {
                FallbackHouseImage(contentDescription = contentDescription, modifier = modifier)
            }
        }
        resolved.startsWith("http") -> {
            AsyncImage(
                model = resolved,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        else -> {
            FallbackHouseImage(contentDescription = contentDescription, modifier = modifier)
        }
    }
}

@Composable
private fun FallbackHouseImage(contentDescription: String, modifier: Modifier) {
    Box(
        modifier = modifier.background(IndigoSoft),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.HomeWork,
            contentDescription = contentDescription,
            tint = IndigoPrimary,
            modifier = Modifier.fillMaxSize(0.45f)
        )
    }
}
