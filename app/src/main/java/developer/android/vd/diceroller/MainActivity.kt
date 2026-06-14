package developer.android.vd.diceroller

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Arrays
import java.util.Random
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {
    private var mainRl: RelativeLayout? = null
    private var dice1: ImageView? = null
    private var dice2: ImageView? = null
    private var dice3: ImageView? = null
    private var dice4: ImageView? = null
    private var dice5: ImageView? = null
    private var dice6: ImageView? = null
    private var dice7: ImageView? = null
    private var dice8: ImageView? = null
    private var dice9: ImageView? = null
    private var total: TextView? = null
    private var number: TextView? = null
    private var adView: AdView? = null

    private var validDices: ArrayList<ImageView>? = null
    private var allDiceList: ArrayList<ImageView>? = null
    private val handler = Handler(Looper.getMainLooper())
    private var timer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById<View?>(R.id.rl_main_container),
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                val systemBars = insets!!.getInsets(WindowInsetsCompat.Type.systemBars())
                v!!.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            })

        initializeViews()
        initializeAd()

        val rollButton = findViewById<Button>(R.id.btn_roll)
        rollButton.setOnClickListener(View.OnClickListener { view: View? -> this.roll(view) })

        allDiceList = ArrayList<ImageView>(
            Arrays.asList<ImageView?>(
                dice1,
                dice2,
                dice3,
                dice4,
                dice5,
                dice6,
                dice7,
                dice8,
                dice9
            )
        )
        validDices = ArrayList<ImageView>()
        validDices!!.add(dice5!!) //when app starts
    }

    private fun initializeViews() {
        mainRl = findViewById<RelativeLayout>(R.id.rl_main_container)
        number = findViewById<TextView>(R.id.tv_no_dice)
        dice1 = findViewById<ImageView>(R.id.iv_dice1)
        dice2 = findViewById<ImageView>(R.id.iv_dice2)
        dice3 = findViewById<ImageView>(R.id.iv_dice3)
        dice4 = findViewById<ImageView>(R.id.iv_dice4)
        dice5 = findViewById<ImageView>(R.id.iv_dice5)
        dice6 = findViewById<ImageView>(R.id.iv_dice6)
        dice7 = findViewById<ImageView>(R.id.iv_dice7)
        dice8 = findViewById<ImageView>(R.id.iv_dice8)
        dice9 = findViewById<ImageView>(R.id.iv_dice9)
        total = findViewById<TextView>(R.id.tv_total)

        findViewById<View?>(R.id.fb_settings).setOnClickListener(View.OnClickListener { view: View? ->
            startActivity(
                Intent(getApplicationContext(), SettingsActivity::class.java)
            )
        })
        findViewById<View?>(R.id.ib_minus).setOnClickListener(View.OnClickListener { view: View? ->
            this.minusOne(
                view
            )
        })
        findViewById<View?>(R.id.ib_plus).setOnClickListener(View.OnClickListener { view: View? ->
            this.plusOne(
                view
            )
        })

        val fab = findViewById<FloatingActionButton>(R.id.fb_themes)

        // Create scale animation
        val scale = ScaleAnimation(
            1.0f, 1.1f,  // from X to X
            1.0f, 1.1f,  // from Y to Y
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        scale.setDuration(800)

        // Create alpha animation
        val alpha = AlphaAnimation(1.0f, 0.8f)
        alpha.setDuration(800)
        alpha.setRepeatCount(Animation.INFINITE)
        alpha.setRepeatMode(Animation.REVERSE)

        // Combine them
        val set = AnimationSet(true)
        set.addAnimation(scale)
        set.addAnimation(alpha)
        set.setInterpolator(AccelerateDecelerateInterpolator())

        fab.setOnClickListener(View.OnClickListener { view: View? ->
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.rewarded_ad_dialog)
            dialog.getWindow()!!
                .setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.getWindow()!!
                .setBackgroundDrawable(getDrawable(R.drawable.background_transparent))
            val no = dialog.findViewById<Button>(R.id.bt_no_thanks)
            val yes = dialog.findViewById<Button?>(R.id.bt_watch_unlock)
            no.setOnClickListener(View.OnClickListener { view1: View? -> dialog.dismiss() })
            //TODO
            // yes.setOnClickListener(view1 -> //TODO);
            dialog.show()
        })
        // Start
        fab.startAnimation(set)
    }

    private fun initializeAd() {
        MobileAds.initialize(
            this,
            OnInitializationCompleteListener { initializationStatus: InitializationStatus? -> })
        val adRequest = AdRequest.Builder().build()
        adView = findViewById<AdView>(R.id.adView)
        adView!!.loadAd(adRequest)
    }

    private fun setDiceVisibility(visibleList: MutableList<ImageView>) {
        for (iView in allDiceList!!) {
            iView.setVisibility(View.INVISIBLE)
        }
        for (iView in visibleList) {
            validDices!!.add(iView)
            iView.setVisibility(View.VISIBLE)
        }
    }

    private fun resetDices(diceList: ArrayList<ImageView>) {
        for (dice in diceList) {
            dice.setImageDrawable(
                ResourcesCompat.getDrawable(
                    getResources(),
                    R.drawable.dice_none_solid,
                    null
                )
            )
        }
    }

    fun roll(view: View?) {
        for (dice in validDices!!) {
            animateDice(dice)
        }

        handler.postDelayed(Runnable {
            for (dice in validDices!!) {
                dice.clearAnimation() // Stop the animation
                val randomInt = Random().nextInt(6) + 1
                setViewForDice(dice, randomInt)
            }
            var sum = 0
            for (dice in validDices!!) {
                val randomInt = Random().nextInt(6) + 1
                sum += randomInt
                setViewForDice(dice, randomInt)
            }
            val totalText = "TOTAL : " + sum
            total!!.setText(totalText)
        }, 700) // Delay for the duration of the animation
    }

    private fun animateDice(dice: ImageView?) {
        val randomMultiplierX = Random().nextInt(2) * 2 - 1 //-1 or 1
        val randomMultiplierY = Random().nextInt(2) * 2 - 1 //-1 or 1
        val randomMultiplierZ = Random().nextInt(2) * 2 - 1 //-1 or 1
        val rotateX = ObjectAnimator.ofFloat(dice, "rotationX", 0f, randomMultiplierX * 1800f)
        val rotateY = ObjectAnimator.ofFloat(dice, "rotationY", 0f, randomMultiplierY * 1800f)
        val rotateZ = ObjectAnimator.ofFloat(dice, "rotation", 0f, randomMultiplierZ * 1800f)
        rotateX.setDuration(800)
        rotateY.setDuration(800)
        rotateZ.setDuration(800)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(rotateX, rotateY, rotateZ)
        animatorSet.start()

        // Play animation
        /*Animation anim = AnimationUtils.loadAnimation(this, R.anim.roll_animation);
        dice.startAnimation(anim);*/
    }


    private fun setViewForDice(diceView: ImageView, number: Int) {
        when (number) {
            1 -> diceView.setImageDrawable(
                ResourcesCompat.getDrawable(
                    getResources(),
                    R.drawable.dice_one_solid,
                    null
                )
            )

            2 -> diceView.setImageDrawable(
                ResourcesCompat.getDrawable(
                    getResources(),
                    R.drawable.dice_two_solid,
                    null
                )
            )

            3 -> diceView.setImageDrawable(
                ResourcesCompat.getDrawable(
                    getResources(),
                    R.drawable.dice_three_solid,
                    null
                )
            )

            4 -> diceView.setImageDrawable(
                ResourcesCompat.getDrawable(
                    getResources(),
                    R.drawable.dice_four_solid,
                    null
                )
            )

            5 -> diceView.setImageDrawable(
                ResourcesCompat.getDrawable(
                    getResources(),
                    R.drawable.dice_five_solid,
                    null
                )
            )

            6 -> diceView.setImageDrawable(
                ResourcesCompat.getDrawable(
                    getResources(),
                    R.drawable.dice_six_solid,
                    null
                )
            )
        }
    }

    fun plusOne(view: View?) {
        var count = number!!.getText().toString().toInt()
        if (count < 9) {
            count++
            rearrangeDice(count)
            number!!.setText(count.toString())
        }
    }

    fun minusOne(view: View?) {
        var count = number!!.getText().toString().toInt()
        if (count > 1) {
            count--
            rearrangeDice(count)
            number!!.setText(count.toString())
        }
    }

    private fun rearrangeDice(number: Int) {
        validDices!!.clear()
        when (number) {
            2 -> setDiceVisibility(Arrays.asList<ImageView?>(dice4, dice6))
            3 -> setDiceVisibility(Arrays.asList<ImageView?>(dice1, dice5, dice9))
            4 -> setDiceVisibility(Arrays.asList<ImageView?>(dice1, dice3, dice7, dice9))
            5 -> setDiceVisibility(Arrays.asList<ImageView?>(dice1, dice3, dice5, dice7, dice9))
            6 -> setDiceVisibility(
                Arrays.asList<ImageView?>(
                    dice1,
                    dice3,
                    dice4,
                    dice6,
                    dice7,
                    dice9
                )
            )

            7 -> setDiceVisibility(
                Arrays.asList<ImageView?>(
                    dice1,
                    dice3,
                    dice4,
                    dice5,
                    dice6,
                    dice7,
                    dice9
                )
            )

            8 -> setDiceVisibility(
                Arrays.asList<ImageView?>(
                    dice1,
                    dice2,
                    dice3,
                    dice4,
                    dice6,
                    dice7,
                    dice8,
                    dice9
                )
            )

            9 -> setDiceVisibility(
                Arrays.asList<ImageView?>(
                    dice1,
                    dice2,
                    dice3,
                    dice4,
                    dice5,
                    dice6,
                    dice7,
                    dice8,
                    dice9
                )
            )

            else -> setDiceVisibility(mutableListOf<ImageView?>(dice5))
        }
        resetDices(validDices!!)
        total!!.setText(getResources().getString(R.string.total))
    }


    override fun onStart() {
        super.onStart()
        startTimerForAds()

        val sharedPreferences = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)

        val totalVisibility =
            sharedPreferences.getBoolean(getString(R.string.total_visibility), true)
        total!!.setVisibility(if (totalVisibility) View.VISIBLE else View.INVISIBLE)

        val selectedBackgroundColor = sharedPreferences.getInt(
            getString(R.string.selected_background),
            Color.parseColor("#33B5E5")
        )
        mainRl!!.setBackgroundColor(selectedBackgroundColor)
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
        if (timerCutOff > System.currentTimeMillis()) {
            adView!!.setVisibility(View.GONE)
        } else {
            adView!!.setVisibility(View.VISIBLE)
        }
    }

    private fun resetTimer() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }
}