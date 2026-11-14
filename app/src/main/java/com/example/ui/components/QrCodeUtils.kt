package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MonoFontFamily
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrCodeUtils {

    fun generateQrBitmap(
        content: String,
        sizePx: Int = 640,
        darkArgb: Int = 0xFF1E1618.toInt(),
        lightArgb: Int = 0xFFFFFFFF.toInt()
    ): ImageBitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 2
            )
            val matrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )
            val width = matrix.width
            val height = matrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (matrix.get(x, y)) darkArgb else lightArgb
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extracts the student Numero de Control from barcodes/QRs printed on CBTis 134 credentials.
     * Supports raw numeric codes (e.g. 23320051340123), structured QR tags (NC:23320051340123),
     * or institutional URLs containing a numeric student ID.
     */
    fun extractNumeroControl(rawValue: String): String {
        val trimmed = rawValue.trim()
        val ncTagMatch = Regex("(?:NC|CONTROL|NO_CONTROL)[:=]\\s*([A-Za-z0-9\\-]{6,20})", RegexOption.IGNORE_CASE)
            .find(trimmed)
        if (ncTagMatch != null) {
            return ncTagMatch.groupValues[1].uppercase()
        }
        val dgetiDigits = Regex("\\b\\d{10,16}\\b").find(trimmed)
        if (dgetiDigits != null) {
            return dgetiDigits.value
        }
        return trimmed
            .replace(Regex("[^A-Za-z0-9\\-]"), "")
            .take(20)
            .uppercase()
    }
}

@Composable
fun QrCodeTicketBox(
    payload: String,
    caption: String,
    modifier: Modifier = Modifier,
    qrSize: Dp = 210.dp
) {
    val qrBitmap = remember(payload) {
        QrCodeUtils.generateQrBitmap(payload)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(qrSize)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap,
                    contentDescription = "Código QR: $caption",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.QrCode2,
                    contentDescription = "Código QR no disponible",
                    modifier = Modifier.size(96.dp),
                    tint = Color(0xFF1E1618)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFF1E1618),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
