package developer.android.vd.diceroller

import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.RatingBar
import android.widget.RatingBar.OnRatingBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import yuku.ambilwarna.AmbilWarnaDialog
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener
import java.util.Objects
import java.util.Timer
import java.util.TimerTask

class SettingsActivity : AppCompatActivity() {
    private var rewardedAd: RewardedAd? = null
    private var removeAdsBtn: Button? = null
    private var showTotal: TextView? = null
    private var hideTotal: TextView? = null

    private var sharedPreferences: SharedPreferences? = null
    private var timer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById<View?>(R.id.main),
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                val systemBars = insets!!.getInsets(WindowInsetsCompat.Type.systemBars())
                v!!.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            })

        initializeVariables()
        handleToggle()
        initializeAds()
    }

    private fun initializeVariables() {
        val otherAppsBtn = findViewById<Button>(R.id.bt_other_apps)
        val colorPickerBtn = findViewById<Button>(R.id.bt_color_picker)

        showTotal = findViewById<TextView>(R.id.tv_show_total)
        hideTotal = findViewById<TextView>(R.id.tv_hide_total)
        removeAdsBtn = findViewById<Button>(R.id.bt_pause_ads)

        colorPickerBtn.setOnClickListener(View.OnClickListener { view: View? ->
            this.colorPicker(
                view
            )
        })
        removeAdsBtn!!.setOnClickListener(View.OnClickListener { view: View? -> this.pauseAds(view) })
        otherAppsBtn.setOnClickListener(View.OnClickListener { view: View? -> this.otherApps(view) })

        findViewById<View?>(R.id.ib_back).setOnClickListener(View.OnClickListener { view: View? -> finish() })

        (findViewById<View?>(R.id.rr_ratings) as RatingBar).setOnRatingBarChangeListener(
            OnRatingBarChangeListener { ratingBar: RatingBar?, rating: Float, fromUser: Boolean ->
                if (fromUser) {
                    openPlayStore()
                }
            })

        sharedPreferences = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
    }


    fun colorPicker(view: View?) {
        val selectedBackgroundColor = sharedPreferences!!.getInt(
            getString(R.string.selected_background),
            Color.parseColor("#33b5e5")
        )

        val ambilWarnaDialog =
            AmbilWarnaDialog(this, selectedBackgroundColor, object : OnAmbilWarnaListener {
                override fun onCancel(dialog: AmbilWarnaDialog?) {
                }

                override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                    val bgcEdit = sharedPreferences!!.edit()
                    bgcEdit.putInt(getString(R.string.selected_background), color)
                    bgcEdit.apply()
                    closeWithTransition()
                }
            })
        ambilWarnaDialog.show()
    }

    private fun handleToggle() {
        toggleButton()

        showTotal!!.setOnClickListener(View.OnClickListener { view: View? ->
            setTotalVisibility(
                false
            )
        })
        hideTotal!!.setOnClickListener(View.OnClickListener { view: View? -> setTotalVisibility(true) })
    }

    private fun toggleButton() {
        val toggleSwitch =
            sharedPreferences!!.getBoolean(getString(R.string.total_visibility), true)
        showTotal!!.setVisibility(if (toggleSwitch) View.VISIBLE else View.GONE)
        hideTotal!!.setVisibility(if (toggleSwitch) View.GONE else View.VISIBLE)
    }

    private fun setTotalVisibility(visibility: Boolean) {
        val toggleEdit = sharedPreferences!!.edit()
        toggleEdit.putBoolean(getString(R.string.total_visibility), visibility)
        toggleEdit.apply()
        toggleButton()
    }

    private fun openPlayStore() {
        val uri = Uri.parse("market://details?id=" + getPackageName())
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        goToMarket.addFlags(
            Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )
        try {
            startActivity(goToMarket)
        } catch (e: Exception) {
            val webUri =
                Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())
            startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    fun otherApps(view: View?) {
        val uri = Uri.parse("market://search?q=pub:Vaibhav+Deshmukh")
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        goToMarket.addFlags(
            Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )
        try {
            startActivity(goToMarket)
        } catch (e: Exception) {
            val webUri = Uri.parse("https://play.google.com/store/search?q=pub:Vaibhav+Deshmukh")
            startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    private fun closeWithTransition() {
        finish()
        overridePendingTransition(R.anim.animate_zoom_enter, R.anim.animate_zoom_exit)
    }

    private fun initializeAds() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            this, getString(R.string.rewarded_ad_unit_id),
            adRequest, object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Handle the error.
                    Log.d(ContentValues.TAG, loadAdError.toString())
                    rewardedAd = null
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(ContentValues.TAG, "Ad was loaded.")
                }
            })
    }

    fun pauseAds(view: View?) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.ads_confirmation_dialog)
        Objects.requireNonNull<Window?>(dialog.getWindow()).setBackgroundDrawable(
            ColorDrawable(
                Color.TRANSPARENT
            )
        )
        Objects.requireNonNull<Window?>(dialog.getWindow())
            .setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val ok = dialog.findViewById<Button>(R.id.bt_ok_ads_dialog)
        ok.setOnClickListener(View.OnClickListener { view1: View? ->
            dialog.cancel()
            showRewardedVideoAd()
        })

        val cancel = dialog.findViewById<Button>(R.id.bt_cancel_ads_dialog)
        cancel.setOnClickListener(View.OnClickListener { view1: View? -> dialog.cancel() })
        dialog.show()
    }

    private fun startTimerForAds() {
        resetTimer()
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                Handler(Looper.getMainLooper()).post(Runnable { updateTimeForAds() })
            }
        }, 0, 1000) // Update every second
    }

    private fun updateTimeForAds() {
        val sharedPreferences = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
        val timerCutOff = sharedPreferences.getLong(
            getString(R.string.pause_ads_cut_off_time),
            System.currentTimeMillis()
        )
        val timeRemainingHrs =
            (timerCutOff - System.currentTimeMillis()) / (60 * 60 * 1000).toFloat()
        if (timeRemainingHrs > 0) {
            removeAdsBtn!!.setBackgroundColor(Color.GRAY)
            removeAdsBtn!!.setEnabled(false)

            if (timeRemainingHrs > 1) {
                removeAdsBtn!!.setText(getString(R.string.no_ads_hrs, timeRemainingHrs))
            } else {
                val minutes = (timeRemainingHrs * 60).toInt()
                removeAdsBtn!!.setText(getString(R.string.no_ads_mins, minutes))
            }
        } else {
            removeAdsBtn!!.setBackgroundColor(Color.BLACK)
            removeAdsBtn!!.setEnabled(true)
            removeAdsBtn!!.setText(getString(R.string.remove_ads))
        }
    }

    private fun showRewardedVideoAd() {
        if (rewardedAd == null) {
            Toast.makeText(
                this,
                "Rewarded video isn't ready, please check internet connection",
                Toast.LENGTH_LONG
            ).show()
        } else {
            rewardedAd!!.setFullScreenContentCallback(object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    Log.d(ContentValues.TAG, "Ad was clicked.")
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Set the ad reference to null so you don't show the ad a second time.
                    Log.d(ContentValues.TAG, "Ad dismissed fullscreen content.")
                    rewardedAd = null
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Called when ad fails to show.
                    Log.e(ContentValues.TAG, "Ad failed to show fullscreen content.")
                    rewardedAd = null
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    Log.d(ContentValues.TAG, "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    Log.d(ContentValues.TAG, "Ad showed fullscreen content.")
                }
            })

            rewardedAd!!.show(this, OnUserEarnedRewardListener { rewardItem: RewardItem? ->
                // Handle the reward.
                Log.d(ContentValues.TAG, "The user earned the reward.")
                val editor = sharedPreferences!!.edit()
                editor.putLong(
                    getString(R.string.pause_ads_cut_off_time),
                    System.currentTimeMillis() + COOL_DOWN_TIME
                )
                editor.apply()
            })
        }
    }

    private fun resetTimer() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    override fun onStart() {
        super.onStart()
        startTimerForAds()
    }

    override fun onDestroy() {
        super.onDestroy()
        resetTimer()
    }

    companion object {
        private val COOL_DOWN_TIME = 8 * 60 * 60 * 1000
    }
}