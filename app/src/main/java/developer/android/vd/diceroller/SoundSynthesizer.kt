package developer.android.vd.diceroller

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

object SoundSynthesizer {
    private const val SAMPLE_RATE = 22050

    fun playDiceClack(context: Context) {
        if (!PrefsHelper.isSoundEffectsEnabled(context)) return

        Thread {
            try {
                val durationMs = 80L
                val numSamples = (durationMs * SAMPLE_RATE / 1000).toInt()
                val samples = ShortArray(numSamples)
                
                val frequency = 950.0 // Hz
                val random = java.util.Random()

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val envelope = Math.exp(-t * 45.0) // Decay envelope
                    val sine = Math.sin(2.0 * Math.PI * frequency * t)
                    val noise = random.nextFloat() * 2.0 - 1.0
                    
                    val signal = (sine * 0.85 + noise * 0.15) * envelope
                    samples[i] = (signal * 28000.0).toInt().coerceIn(-32768, 32767).toShort()
                }

                @Suppress("DEPRECATION")
                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    samples.size * 2,
                    AudioTrack.MODE_STATIC
                )

                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()

                Thread.sleep(durationMs + 20)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
