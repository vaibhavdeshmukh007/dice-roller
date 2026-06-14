package developer.android.vd.diceroller

import android.content.Context

class ProStatusProvider(private val context: Context) {

    fun isProActive(): Boolean =
        PrefsHelper.isProActive(context)

    fun markProUsed() {
        PrefsHelper.markProUsed(context)
    }

    fun incrementRollCount() {
        PrefsHelper.incrementRollCount(context)
    }
}

