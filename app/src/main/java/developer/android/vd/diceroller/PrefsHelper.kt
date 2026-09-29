package developer.android.vd.diceroller

import android.content.Context
import android.graphics.Color
import androidx.core.content.edit
import java.util.concurrent.TimeUnit

object PrefsHelper {
    private const val PREFS_NAME = "DiceRollerPrefs"
    private const val KEY_TOTAL_HIDDEN = "total_hidden"
    private const val KEY_BACKGROUND_COLOR = "background_color"
    private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
    private const val KEY_DICE_TYPE = "dice_type"
    private const val KEY_DICE_COLOR = "dice_color"
    private const val KEY_PRO_PURCHASED = "pro_purchased"
    private const val KEY_PRO_TRIAL_EXPIRES_AT = "pro_trial_expires_at"
    private const val PRO_TRIAL_DURATION_HOURS = 6
    private const val KEY_PRO_USED = "pro_used"
    private const val KEY_REVIEW_ASKED = "review_asked"
    private const val KEY_REVIEW_REMIND_LATER_TIME = "review_remind_later_time"
    private const val KEY_REVIEW_NEXT_ROLL_COUNT = "review_next_roll_count"
    private const val KEY_ROLL_COUNT = "roll_count"
    private const val KEY_SHAKE_TO_ROLL = "shake_to_roll"
    private const val KEY_SOUND_EFFECTS = "sound_effects"
    const val REVIEW_INTERVAL = 50

    fun markProUsed(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_PRO_USED, true) }
    }

    fun incrementRollCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_ROLL_COUNT, 0) + 1
        prefs.edit { putInt(KEY_ROLL_COUNT, count) }
        return count
    }

    fun getRollCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_ROLL_COUNT, 0)
    }

    fun shouldAskForReview(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_REVIEW_ASKED, false)) {
            return false
        }
        val remindTime = prefs.getLong(KEY_REVIEW_REMIND_LATER_TIME, 0L)
        if (System.currentTimeMillis() < remindTime) {
            return false
        }
        val rollCount = getRollCount(context)
        val nextRollCount = prefs.getInt(KEY_REVIEW_NEXT_ROLL_COUNT, 15)
        return rollCount >= nextRollCount
    }

    fun markReviewAsked(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_REVIEW_ASKED, true) }
    }

    fun setReviewRemindLater(context: Context, days: Int = 3) {
        val remindTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days.toLong())
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putLong(KEY_REVIEW_REMIND_LATER_TIME, remindTime) }
    }

    fun postponeReviewRollCount(context: Context, n: Int = REVIEW_INTERVAL) {
        val currentRoll = getRollCount(context)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putInt(KEY_REVIEW_NEXT_ROLL_COUNT, currentRoll + n) }
    }


    fun getBackgroundColor(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_BACKGROUND_COLOR, Color.WHITE)
    }

    fun saveBackgroundColor(context: Context, color: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putInt(KEY_BACKGROUND_COLOR, color) }
    }

    fun isTotalHidden(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_TOTAL_HIDDEN, false)
    }

    fun setTotalHidden(context: Context, hidden: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_TOTAL_HIDDEN, hidden) }
    }

    fun isVibrationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_VIBRATION_ENABLED, true) // default ON
    }

    fun setVibrationEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_VIBRATION_ENABLED, enabled) }
    }

    fun isShakeToRollEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SHAKE_TO_ROLL, true)
    }

    fun setShakeToRollEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_SHAKE_TO_ROLL, enabled) }
    }

    fun isSoundEffectsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SOUND_EFFECTS, true)
    }

    fun setSoundEffectsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_SOUND_EFFECTS, enabled) }
    }


    fun setDiceType(context: Context, diceType: DiceType) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_DICE_TYPE, diceType.name) }
    }

    fun isProActive(context: Context): Boolean {
        if (isLifetimePro(context)) return true

        // Temporary Pro (rewarded ad)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val expiresAt = prefs.getLong(KEY_PRO_TRIAL_EXPIRES_AT, 0L)
        return System.currentTimeMillis() < expiresAt
    }

    fun isLifetimePro(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PRO_PURCHASED, false)
    }

    fun getDiceColor(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_DICE_COLOR, Color.TRANSPARENT) // Default to no tint
    }

    fun saveDiceColor(context: Context, color: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putInt(KEY_DICE_COLOR, color) }
    }

    fun getProRemainingMillis(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val expiresAt = prefs.getLong(KEY_PRO_TRIAL_EXPIRES_AT, 0L)
        return (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun formatRemainingTime(context: Context): String {
        val millis = getProRemainingMillis(context)
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis)

        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }

    fun activateProTrial(context: Context) {
        val expiresAt = System.currentTimeMillis() + PRO_TRIAL_DURATION_HOURS * 60 * 60 * 1000
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putLong(KEY_PRO_TRIAL_EXPIRES_AT, expiresAt) }
    }

    fun setProPurchased(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_PRO_PURCHASED, true)
            remove(KEY_PRO_TRIAL_EXPIRES_AT) // cleanup
        }
    }
}
