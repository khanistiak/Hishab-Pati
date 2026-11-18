package com.example.ui.screens.insights

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CustomDateRangeDialog(
    initialStart: Long?,
    initialEnd: Long?,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (startMillis: Long, endMillis: Long) -> Unit
) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val now = remember { System.currentTimeMillis() }
    val thirtyDaysAgo = remember { now - 30L * 24 * 60 * 60 * 1000 }

    var startText by remember { mutableStateOf(sdf.format(Date(initialStart ?: thirtyDaysAgo))) }
    var endText by remember { mutableStateOf(sdf.format(Date(initialEnd ?: now))) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (language == AppLanguage.BANGLA) "কাস্টম তারিখ সীমা নির্বাচন" else "Custom Date Range",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (language == AppLanguage.BANGLA) "তারিখ ফরম্যাট: YYYY-MM-DD" else "Date format: YYYY-MM-DD",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = startText,
                    onValueChange = {
                        startText = it
                        errorMessage = null
                    },
                    label = { Text(if (language == AppLanguage.BANGLA) "শুরুর তারিখ" else "Start Date") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_start_date_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = endText,
                    onValueChange = {
                        endText = it
                        errorMessage = null
                    },
                    label = { Text(if (language == AppLanguage.BANGLA) "শেষ তারিখ" else "End Date") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_end_date_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (language == AppLanguage.BANGLA) "দ্রুত নির্বাচন:" else "Quick Presets:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PresetChip(
                        label = if (language == AppLanguage.BANGLA) "গত ৭ দিন" else "Last 7d",
                        onClick = {
                            val c = Calendar.getInstance()
                            val e = c.timeInMillis
                            c.add(Calendar.DAY_OF_YEAR, -7)
                            val s = c.timeInMillis
                            startText = sdf.format(Date(s))
                            endText = sdf.format(Date(e))
                        }
                    )
                    PresetChip(
                        label = if (language == AppLanguage.BANGLA) "গত ৩০ দিন" else "Last 30d",
                        onClick = {
                            val c = Calendar.getInstance()
                            val e = c.timeInMillis
                            c.add(Calendar.DAY_OF_YEAR, -30)
                            val s = c.timeInMillis
                            startText = sdf.format(Date(s))
                            endText = sdf.format(Date(e))
                        }
                    )
                    PresetChip(
                        label = if (language == AppLanguage.BANGLA) "গত ৯০ দিন" else "Last 90d",
                        onClick = {
                            val c = Calendar.getInstance()
                            val e = c.timeInMillis
                            c.add(Calendar.DAY_OF_YEAR, -90)
                            val s = c.timeInMillis
                            startText = sdf.format(Date(s))
                            endText = sdf.format(Date(e))
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val sDate = sdf.parse(startText.trim())
                        val eDate = sdf.parse(endText.trim())
                        if (sDate != null && eDate != null) {
                            val sCal = Calendar.getInstance().apply {
                                time = sDate
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                            }
                            val eCal = Calendar.getInstance().apply {
                                time = eDate
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                            }
                            if (eCal.timeInMillis >= sCal.timeInMillis) {
                                onConfirm(sCal.timeInMillis, eCal.timeInMillis)
                                onDismiss()
                            } else {
                                errorMessage = if (language == AppLanguage.BANGLA) "শেষ তারিখ শুরুর তারিখের পরে হতে হবে" else "End date must be after start date."
                            }
                        } else {
                            errorMessage = if (language == AppLanguage.BANGLA) "সঠিক তারিখ লিখুন (YYYY-MM-DD)" else "Please enter valid date (YYYY-MM-DD)."
                        }
                    } catch (_: Exception) {
                        errorMessage = if (language == AppLanguage.BANGLA) "তারিখ ফরম্যাট সঠিক নয়" else "Invalid date format."
                    }
                },
                modifier = Modifier.testTag("apply_custom_date_btn"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (language == AppLanguage.BANGLA) "প্রয়োগ করুন" else "Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (language == AppLanguage.BANGLA) "বাতিল" else "Cancel")
            }
        }
    )
}

@Composable
private fun PresetChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
