package com.example.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.staticCompositionLocalOf

val LocalHapticsEnabled = staticCompositionLocalOf { true }

object AppHaptics {
    /**
     * Performs a subtle tap/click haptic feedback if [enabled] is true.
     * When [enabled] is false, suppresses app-triggered vibration/haptics completely.
     */
    fun performTap(view: View, enabled: Boolean) {
        if (!enabled) return
        try {
            val performed = view.performHapticFeedback(
                HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
            if (!performed) {
                vibrateLight(view.context, 12L)
            }
        } catch (_: Exception) {
            vibrateLight(view.context, 12L)
        }
    }

    /**
     * Performs a toggle/switch haptic feedback if [enabled] is true.
     */
    fun performToggle(view: View, enabled: Boolean) {
        if (!enabled) return
        try {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }
            val performed = view.performHapticFeedback(
                constant,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
            if (!performed) {
                vibrateLight(view.context, 16L)
            }
        } catch (_: Exception) {
            vibrateLight(view.context, 16L)
        }
    }

    /**
     * Performs a wheel/selection tick haptic feedback if [enabled] is true.
     */
    fun performTick(view: View, enabled: Boolean) {
        if (!enabled) return
        try {
            val performed = view.performHapticFeedback(
                HapticFeedbackConstants.CLOCK_TICK,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
            if (!performed) {
                vibrateLight(view.context, 8L)
            }
        } catch (_: Exception) {
            vibrateLight(view.context, 8L)
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrateLight(context: Context, durationMs: Long) {
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {
        }
    }
}
