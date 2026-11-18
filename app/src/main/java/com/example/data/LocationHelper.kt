package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object LocationHelper {
    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentCoordinates(context: Context): Pair<Double, Double>? {
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            val location = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token).await()
                ?: client.lastLocation.await()
            if (location != null) {
                location.latitude to location.longitude
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}

object PhotoCompressionHelper {
    suspend fun uriToCompressedDataUri(context: Context, uri: Uri, maxDim: Int = 640, quality: Int = 68): String? =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val original = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                if (original == null) return@withContext null

                val ratio = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
                val targetW = (original.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (original.height * ratio).toInt().coerceAtLeast(1)
                val scaled = if (ratio < 1f) Bitmap.createScaledBitmap(original, targetW, targetH, true) else original

                val baos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                if (scaled !== original) scaled.recycle()
                original.recycle()

                val bytes = baos.toByteArray()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                "data:image/jpeg;base64,$base64"
            } catch (_: Exception) {
                null
            }
        }
}
