package com.example.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * On-device Receipt OCR processor powered by Google ML Kit Text Recognition.
 *
 * Key capabilities:
 * - Processes full-resolution receipt URIs (handling EXIF rotation via InputImage.fromFilePath)
 *   as well as Bitmaps with explicit rotation degrees.
 * - Spatially reconstructs horizontal receipt rows using bounding-box Y coordinates so left-aligned
 *   labels ("GRAND TOTAL") and right-aligned amounts ("315.00") on the same physical row are paired
 *   accurately even when ML Kit separates columns into different TextBlocks.
 */
object ReceiptOcrManager {

    data class PositionedLine(
        val text: String,
        val left: Int,
        val top: Int,
        val bottom: Int
    ) {
        val centerY: Float get() = (top + bottom) / 2f
        val height: Int get() = max(1, bottom - top)
    }

    suspend fun processReceiptUri(
        context: Context,
        imageUri: Uri,
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>
    ): Result<ParsedReceiptData> {
        return try {
            val image = InputImage.fromFilePath(context, imageUri)
            recognizeAndParse(image, accounts, categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun processReceiptBitmap(
        bitmap: Bitmap,
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        rotationDegrees: Int = 0
    ): Result<ParsedReceiptData> {
        return try {
            val preparedBitmap = preprocessBitmapForOcr(bitmap)
            val image = InputImage.fromBitmap(preparedBitmap, rotationDegrees)
            recognizeAndParse(image, accounts, categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360f == 0f) return source
        return try {
            val matrix = Matrix().apply { postRotate(degrees) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } catch (_: Exception) {
            source
        }
    }

    /**
     * Ensures small camera preview thumbnails are upscaled enough for ML Kit's character detector,
     * while capping oversized images to avoid OOM.
     */
    private fun preprocessBitmapForOcr(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return bitmap

        val minDimension = minOf(width, height)
        val maxDimension = maxOf(width, height)

        return when {
            minDimension < 480 -> {
                val scale = (720f / minDimension).coerceAtMost(3.0f)
                val targetW = (width * scale).toInt().coerceAtLeast(1)
                val targetH = (height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            }
            maxDimension > 2560 -> {
                val scale = 2560f / maxDimension
                val targetW = (width * scale).toInt().coerceAtLeast(1)
                val targetH = (height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            }
            else -> bitmap
        }
    }

    private suspend fun recognizeAndParse(
        image: InputImage,
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>
    ): Result<ParsedReceiptData> = suspendCancellableCoroutine { continuation ->
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val spatiallyAlignedText = reconstructSpatiallyAlignedText(visionText)
                    val parsed = ReceiptParser.parse(spatiallyAlignedText, accounts, categories)
                    if (continuation.isActive) {
                        continuation.resume(Result.success(parsed))
                    }
                    recognizer.close()
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(exception))
                    }
                    recognizer.close()
                }
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    /**
     * Reconstructs receipt text row-by-row using ML Kit bounding boxes so left-column labels
     * and right-column amounts on the same horizontal line are combined onto one logical line.
     */
    fun reconstructSpatiallyAlignedText(visionText: Text): String {
        val allLines = visionText.textBlocks.flatMap { it.lines }
        if (allLines.isEmpty()) return visionText.text

        val positioned = mutableListOf<PositionedLine>()
        val unpositioned = mutableListOf<String>()

        for (line in allLines) {
            val text = line.text.trim()
            if (text.isEmpty()) continue
            val box = line.boundingBox
            if (box != null) {
                positioned.add(
                    PositionedLine(
                        text = text,
                        left = box.left,
                        top = box.top,
                        bottom = box.bottom
                    )
                )
            } else {
                unpositioned.add(text)
            }
        }

        if (positioned.isEmpty()) return visionText.text

        val reconstructed = reconstructLinesFromBoxes(positioned)
        return (reconstructed + unpositioned).joinToString("\n")
    }

    fun reconstructLinesFromBoxes(lines: List<PositionedLine>): List<String> {
        if (lines.isEmpty()) return emptyList()

        val sortedByY = lines.sortedWith(compareBy<PositionedLine> { it.centerY }.thenBy { it.left })
        val rows = mutableListOf<MutableList<PositionedLine>>()

        for (item in sortedByY) {
            val lastRow = rows.lastOrNull()
            if (lastRow == null) {
                rows.add(mutableListOf(item))
            } else {
                val rowAvgCenterY = lastRow.map { it.centerY }.average().toFloat()
                val rowAvgHeight = lastRow.map { it.height }.average().toFloat()
                val threshold = max(rowAvgHeight, item.height.toFloat()) * 0.58f
                if (abs(item.centerY - rowAvgCenterY) <= threshold) {
                    lastRow.add(item)
                } else {
                    rows.add(mutableListOf(item))
                }
            }
        }

        return rows.map { rowItems ->
            rowItems.sortedBy { it.left }.joinToString("   ") { it.text }
        }
    }
}
