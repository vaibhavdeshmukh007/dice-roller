package developer.android.vd.diceroller

import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color as ComposeColor

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(applicationContext)
    }

    private var rewardedAd: RewardedAd? = null
    private var isAdDelayed = false
    private var reviewInProgress = false
    private lateinit var billingManager: BillingManager

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var shakeListener: SensorEventListener? = null
    private var lastShakeTime = 0L

    // Reactively synced states with Composable UI
    private val isProActive = mutableStateOf(false)
    private val isLifetimePro = mutableStateOf(false)
    private val backgroundColorState = mutableStateOf(Color.WHITE)
    private val remainingMillis = mutableStateOf(0L)
    private val isTotalHidden = mutableStateOf(false)

    private val countdownHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val countdownRunnable = object : Runnable {
        override fun run() {
            updateProStatusBanner()
            countdownHandler.postDelayed(this, 1000L)
        }
    }

    // Settings Launcher to reload preferences when returning from SettingsActivity
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        applySavedSettings()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.restoreHistory()

        setupBilling()
        setupAds()
        applySavedSettings()
        loadRewardedAd()
        setupShakeToRoll()

        setContent {
            val state by viewModel.uiState.observeAsState(DiceUiState())
            val history by viewModel.rollHistory.observeAsState(emptyList())

            DiceRollerTheme {
                MainScreen(
                    state = state,
                    history = history,
                    isProActive = isProActive.value,
                    isLifetimePro = isLifetimePro.value,
                    backgroundColor = backgroundColorState.value,
                    remainingMillis = remainingMillis.value,
                    isTotalHidden = isTotalHidden.value,
                    isAdDelayed = isAdDelayed,
                    onPlusClick = { viewModel.increaseDice() },
                    onMinusClick = { viewModel.decreaseDice() },
                    onDiceClick = { idx -> viewModel.toggleDiceLock(idx) },
                    onRollClick = { triggerRoll() },
                    onSettingsClick = {
                        val intent = Intent(this, SettingsActivity::class.java)
                        settingsLauncher.launch(intent)
                    },
                    onDiceTypeChange = { type ->
                        PrefsHelper.setDiceType(this, type)
                        viewModel.setDiceType(type)
                    },
                    onHistoryClick = {
                        if (PrefsHelper.isProActive(this)) {
                            startActivity(Intent(this, HistoryActivity::class.java))
                        }
                    },
                    onWatchAdClick = { watchAdToUnlockPro() },
                    onBuyLifetimeClick = { buyProLifetime() },
                    onRollAnimationFinished = { viewModel.onRollAnimationFinished() }
                )
            }
        }
    }

    private fun setupBilling() {
        billingManager = BillingManager(this)
        billingManager.setListener(object : BillingManager.BillingListener {
            override fun onPurchaseSuccess() {
                runOnUiThread {
                    updateProStatusBanner()
                    applySavedSettings()
                }
            }

            override fun onPurchaseFailure(error: String) {}
            override fun onBillingClientReady() {}
            override fun onPurchasePending() {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Payment processing... Pro will unlock automatically when confirmed by Google.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })
        billingManager.startConnection()
    }

    private fun applySavedSettings() {
        backgroundColorState.value = PrefsHelper.getBackgroundColor(this)
        isTotalHidden.value = PrefsHelper.isTotalHidden(this)
        updateProStatusBanner()
    }

    private fun setupAds() {
        if (PrefsHelper.isProActive(this)) return

        val rollCount = PrefsHelper.getRollCount(this)
        if (rollCount < 5) {
            isAdDelayed = true
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (!isDestroyed && !PrefsHelper.isProActive(this@MainActivity)) {
                    isAdDelayed = false
                }
            }, 3000)
        }
    }

    fun watchAdToUnlockPro() {
        showRewardedAd()
    }

    fun buyProLifetime() {
        billingManager.launchPurchaseFlow(this)
    }

    fun updateProStatusBanner() {
        isProActive.value = PrefsHelper.isProActive(this)
        isLifetimePro.value = PrefsHelper.isLifetimePro(this)
        remainingMillis.value = PrefsHelper.getProRemainingMillis(this)
    }

    fun maybeAskForReviewAfterValue() {
        if (reviewInProgress) return
        val rollCount = PrefsHelper.getRollCount(this)

        if (rollCount < 15 || rollCount % PrefsHelper.REVIEW_INTERVAL != 0) return

        reviewInProgress = true
        val reviewManager = ReviewManagerFactory.create(this)
        reviewManager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                reviewManager.launchReviewFlow(this, task.result)
            }
        }
    }

    private fun setupShakeToRoll() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        shakeListener = object : SensorEventListener {
            private var acceleration = 0f
            private var currentAcceleration = SensorManager.GRAVITY_EARTH
            private var lastAcceleration = SensorManager.GRAVITY_EARTH

            override fun onSensorChanged(event: SensorEvent) {
                if (!PrefsHelper.isShakeToRollEnabled(this@MainActivity)) return
                val state = viewModel.uiState.value ?: return
                if (state.isRolling) return

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                lastAcceleration = currentAcceleration
                currentAcceleration = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val delta = Math.abs(currentAcceleration - lastAcceleration)
                acceleration = acceleration * 0.9f + delta

                if (acceleration > 10f) {
                    val now = System.currentTimeMillis()
                    if (now - lastShakeTime > 2000L) {
                        lastShakeTime = now
                        runOnUiThread {
                            triggerRoll()
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    private fun registerShakeListener() {
        if (PrefsHelper.isShakeToRollEnabled(this)) {
            sensorManager?.registerListener(
                shakeListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    private fun unregisterShakeListener() {
        sensorManager?.unregisterListener(shakeListener)
    }

    private fun triggerRoll() {
        val state = viewModel.uiState.value ?: return
        if (state.isRolling) return

        if (PrefsHelper.isVibrationEnabled(this)) {
            // Use window content root view for tap vibration since layout views are gone
            window.decorView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }

        viewModel.rollDice(state.diceType)
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            this,
            getString(R.string.rewarded_ad_unit_id),
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                }
            })
    }

    private fun showRewardedAd() {
        rewardedAd?.show(this) {
            PrefsHelper.activateProTrial(this)
            updateProStatusBanner()
            viewModel.restoreHistory()
            Toast.makeText(
                this, "🎉 Pro unlocked for 6 hours", Toast.LENGTH_LONG
            ).show()
            loadRewardedAd()
        } ?: run {
            AlertDialog.Builder(this)
                .setTitle("Ad Not Ready 🚫")
                .setMessage("There are no video ads available right now. Please check your connection and try again later, or upgrade to Pro Lifetime to unlock everything forever!")
                .setPositiveButton("Buy Lifetime 💎") { _, _ ->
                    billingManager.launchPurchaseFlow(this)
                }
                .setNegativeButton("Cancel", null)
                .show()
            loadRewardedAd()
        }
    }

    override fun onPause() {
        unregisterShakeListener()
        countdownHandler.removeCallbacks(countdownRunnable)
        super.onPause()
    }

    override fun onDestroy() {
        billingManager.destroy()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        applySavedSettings()
        registerShakeListener()

        if (!PrefsHelper.isLifetimePro(this)) {
            countdownHandler.post(countdownRunnable)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: DiceUiState,
    history: List<RollEntry>,
    isProActive: Boolean,
    isLifetimePro: Boolean,
    backgroundColor: Int,
    remainingMillis: Long,
    isTotalHidden: Boolean,
    isAdDelayed: Boolean,
    onPlusClick: () -> Unit,
    onMinusClick: () -> Unit,
    onDiceClick: (Int) -> Unit,
    onRollClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDiceTypeChange: (DiceType) -> Unit,
    onHistoryClick: () -> Unit,
    onWatchAdClick: () -> Unit,
    onBuyLifetimeClick: () -> Unit,
    onRollAnimationFinished: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Dice animation state vectors
    val rotations = remember { List(9) { Animatable(0f) } }
    val scales = remember { List(9) { Animatable(1f) } }
    val blurs = remember { List(9) { Animatable(0f) } }

    val haptic = LocalHapticFeedback.current

    // Dialog & BottomSheet state controllers
    var showTypeChooser by remember { mutableStateOf(false) }
    var showAdUpsell by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }

    val isDark = remember(backgroundColor) {
        val darkness = 1 - (0.299 * Color.red(backgroundColor) + 0.587 * Color.green(backgroundColor) + 0.114 * Color.blue(backgroundColor)) / 255
        darkness >= 0.5
    }

    val textColor = if (isDark) ComposeColor.White else TextPrimary
    val textSecondaryColor = if (isDark) DarkTextSecondary else TextSecondary
    val iconColor = if (isDark) ComposeColor.White else TextPrimary

    // Animation trigger
    LaunchedEffect(state.isRolling) {
        if (state.isRolling) {
            val count = state.diceCount
            val visibleIndices = getVisibleIndicesForCount(count)
            val durationScale = 1f + ((count - 1) / 20f)
            val rollDuration = (160L * durationScale).toInt()
            val staggerDelay = (20L * durationScale).toLong()

            coroutineScope {
                visibleIndices.forEachIndexed { index, pos ->
                    val isLocked = state.lockedIndices.contains(index)
                    if (!isLocked) {
                        launch {
                            delay(index * staggerDelay)

                            if (PrefsHelper.isSoundEffectsEnabled(context)) {
                                SoundSynthesizer.playDiceClack(context)
                            }
                            if (PrefsHelper.isVibrationEnabled(context)) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }

                            launch {
                                rotations[pos - 1].animateTo(
                                    targetValue = 360f,
                                    animationSpec = tween(rollDuration, easing = FastOutSlowInEasing)
                                )
                                rotations[pos - 1].snapTo(0f)
                            }
                            launch {
                                scales[pos - 1].animateTo(1.1f, tween(rollDuration / 2, easing = LinearEasing))
                                scales[pos - 1].animateTo(1.0f, tween(rollDuration / 2, easing = LinearEasing))
                            }
                        }
                    }
                }
            }

            val totalDelay = rollDuration + (visibleIndices.size * staggerDelay)
            delay(totalDelay)

            coroutineScope {
                visibleIndices.forEachIndexed { index, pos ->
                    val isLocked = state.lockedIndices.contains(index)
                    if (!isLocked) {
                        launch {
                            blurs[pos - 1].animateTo(10f, tween(20))
                            blurs[pos - 1].animateTo(0f, tween(80))
                        }
                    }
                }
            }

            delay(50)
            onRollAnimationFinished()
            (context as? MainActivity)?.maybeAskForReviewAfterValue()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor(backgroundColor))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 70.dp) // Leave space for banner ad
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Dice Roller",
                    color = textColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (isProActive) {
                                showAdUpsell = true
                            } else {
                                showAdUpsell = true
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_gift),
                            contentDescription = "Gift Pro",
                            tint = iconColor
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings),
                            contentDescription = "Settings",
                            tint = iconColor
                        )
                    }
                }
            }

            // Dice Count Pill Selector
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
                    .background(
                        color = if (isDark) ComposeColor(0xFF1E293B) else ComposeColor(0xFFF3F4F6),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = onMinusClick,
                        enabled = state.diceCount > 1,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_minus),
                            contentDescription = "Minus",
                            tint = if (state.diceCount > 1) iconColor else iconColor.copy(alpha = 0.3f)
                        )
                    }
                    Text(
                        text = state.diceCount.toString(),
                        color = textColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    IconButton(
                        onClick = onPlusClick,
                        enabled = state.diceCount < 9,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_plus),
                            contentDescription = "Plus",
                            tint = if (state.diceCount < 9) iconColor else iconColor.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Interactive Dice Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) ComposeColor(0xFF1E293B) else ComposeColor.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.dp, if (isDark) ComposeColor.DarkGray else ComposeColor.LightGray.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Dice Grid
                    DiceGrid(
                        count = state.diceCount,
                        results = state.results,
                        diceType = state.diceType,
                        lockedIndices = state.lockedIndices,
                        rotations = rotations,
                        scales = scales,
                        blurs = blurs,
                        isRolling = state.isRolling,
                        onDiceClick = onDiceClick
                    )

                    // Total Text
                    if (!isTotalHidden && !state.results.all { it == 0 } && !state.isRolling) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Total: ${state.total}",
                            color = textColor,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Selectors & Options Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Dice Type chip
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isDark) ComposeColor(0xFF334155) else ComposeColor(0xFFE5E7EB),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { showTypeChooser = true }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${state.diceType.name} ▼",
                                color = textColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        // View History button
                        Text(
                            text = "View History",
                            color = if (isDark) ComposeColor(0xFF818CF8) else ColorPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    if (isProActive) {
                                        onHistoryClick()
                                    } else {
                                        showHistorySheet = true
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    // Pro hint label
                    if (!isLifetimePro) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isProActive) "Try Lifetime Pro for exclusive themes ✨" else "✨ Unlock all dice & themes with Pro",
                            color = ColorAccent,
                            fontSize = 13.sp,
                            fontFamily = AladinFontFamily
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Roll Button
            Button(
                onClick = onRollClick,
                enabled = !state.isRolling,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ComposeColor.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ColorAccent, ColorPrimary)
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "R O L L",
                        color = ComposeColor.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dynamic Pro Status Banner
            ProStatusBanner(
                isLifetime = isLifetimePro,
                isActive = isProActive,
                remainingMillis = remainingMillis,
                onWatchAd = onWatchAdClick,
                onBuyLifetime = onBuyLifetimeClick
            )
        }

        // Banner Ad aligned at the bottom
        if (!isProActive && !isAdDelayed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(ComposeColor.White)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx ->
                        AdView(ctx).apply {
                            setAdSize(com.google.android.gms.ads.AdSize.BANNER)
                            adUnitId = ctx.getString(R.string.banner_ad_unit_id)
                            loadAd(AdRequest.Builder().build())
                        }
                    }
                )
            }
        }
    }

    // Modal Bottom Sheet: History
    if (showHistorySheet) {
        val visibleHistory = remember(history, isProActive) {
            if (isProActive) history else history.take(10)
        }

        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Roll History",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isProActive) "Unlimited roll history" else "Last 10 rolls shown · Pro unlocks full history",
                    fontSize = 12.sp,
                    color = ComposeColor.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.height(260.dp)) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (visibleHistory.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No rolls yet", fontWeight = FontWeight.Bold, color = ComposeColor.Gray)
                                }
                            }
                        } else {
                            items(visibleHistory) { entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .background(ColorPrimary, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(entry.diceType.name, color = ComposeColor.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("${entry.diceCount} dice", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            entry.results.forEach { v ->
                                                Box(
                                                    modifier = Modifier.size(22.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Image(
                                                        painter = painterResource(id = getDiceShapeRes(entry.diceType)),
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    Text(v.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ComposeColor.Black)
                                                }
                                            }
                                        }
                                    }
                                    Text(entry.total.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorPrimary)
                                }
                            }
                        }
                    }
                }

                if (!isProActive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.linearGradient(colors = listOf(ColorPrimary, ColorAccent)))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Unlock Unlimited History 📜", color = ComposeColor.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "Watch an ad to unlock full history, all dice & no ads for 6h, or get Lifetime Pro!",
                                color = ComposeColor.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        showHistorySheet = false
                                        onWatchAdClick()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = ComposeColor.White),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Watch Ad 🎬", color = ComposeColor(0xFF2E7D32), fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        showHistorySheet = false
                                        onBuyLifetimeClick()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorAccent),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Go Lifetime 💎", color = ComposeColor.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Choose Dice Type
    if (showTypeChooser) {
        AlertDialog(
            onDismissRequest = { showTypeChooser = false },
            title = { Text("Choose Dice", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DiceType.entries.forEach { type ->
                        val isLocked = type.proOnly && !isProActive
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isLocked) {
                                        showTypeChooser = false
                                        showAdUpsell = true
                                    } else {
                                        onDiceTypeChange(type)
                                        showTypeChooser = false
                                        Toast.makeText(context, "${type.name} selected", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = type.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isLocked) {
                                Text(text = "🔒", fontSize = 14.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTypeChooser = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Pro Upsell (D10/D12/D20 selection or general)
    if (showAdUpsell) {
        AlertDialog(
            onDismissRequest = { showAdUpsell = false },
            title = { Text(if (isProActive) "Lifetime Pro Active ✅" else "Try Pro for Free! 🎁", fontWeight = FontWeight.Bold) },
            text = {
                if (isLifetimePro) {
                    Text("You have permanent access to all Pro features, advanced dice, and custom themes forever!\n\nThank you for your support.")
                } else if (isProActive) {
                    val timeLeftStr = PrefsHelper.formatRemainingTime(context)
                    Text("Pro features are unlocked for your 6-hour trial.\n\nAdvanced dice and unlimited history available for $timeLeftStr.")
                } else {
                    Text(
                        "Get full access to premium features:\n\n" +
                        "• Advanced dice (d10, d12, d20)\n" +
                        "• Unlimited roll history\n" +
                        "• No banner ads"
                    )
                }
            },
            confirmButton = {
                if (!isLifetimePro) {
                    Button(
                        onClick = {
                            showAdUpsell = false
                            onBuyLifetimeClick()
                        }
                    ) {
                        Text(if (isProActive) "Upgrade to Lifetime 💎" else "Buy Lifetime Pro 💎")
                    }
                } else {
                    Button(onClick = { showAdUpsell = false }) {
                        Text("Awesome")
                    }
                }
            },
            dismissButton = {
                Row {
                    if (!isProActive) {
                        TextButton(
                            onClick = {
                                showAdUpsell = false
                                onWatchAdClick()
                            }
                        ) {
                            Text("Watch Ad (6h) 🎬")
                        }
                    }
                    TextButton(onClick = { showAdUpsell = false }) {
                        Text(if (isProActive) "Got it" else "Maybe Later")
                    }
                }
            }
        )
    }
}

@Composable
fun DiceGrid(
    count: Int,
    results: List<Int>,
    diceType: DiceType,
    lockedIndices: Set<Int>,
    rotations: List<Animatable<Float, *>>,
    scales: List<Animatable<Float, *>>,
    blurs: List<Animatable<Float, *>>,
    isRolling: Boolean,
    onDiceClick: (Int) -> Unit
) {
    val visibleIndices = getVisibleIndicesForCount(count)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in 0..2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (col in 0..2) {
                    val pos = row * 3 + col + 1
                    val isVisible = pos in visibleIndices

                    if (isVisible) {
                        val indexInList = visibleIndices.indexOf(pos)
                        if (indexInList != -1) {
                            val value = if (results.size > indexInList) results[indexInList] else 0
                            val isLocked = lockedIndices.contains(indexInList)
                            val rotation = rotations[pos - 1].value
                            val scale = scales[pos - 1].value
                            val blur = blurs[pos - 1].value

                            DiceView(
                                value = value,
                                diceType = diceType,
                                isLocked = isLocked,
                                rotation = rotation,
                                scale = scale,
                                blur = blur,
                                isRolling = isRolling,
                                onClick = { onDiceClick(indexInList) }
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(72.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DiceView(
    value: Int,
    diceType: DiceType,
    isLocked: Boolean,
    rotation: Float,
    scale: Float,
    blur: Float,
    isRolling: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isPro = remember { PrefsHelper.isProActive(context) }
    val isLifetime = remember { PrefsHelper.isLifetimePro(context) }

    val tintColor = remember(isLifetime, isLocked) {
        if (isLifetime && !isLocked) {
            val color = PrefsHelper.getDiceColor(context)
            if (color != Color.TRANSPARENT) ComposeColor(color) else null
        } else null
    }

    val textColor = remember(tintColor) {
        if (tintColor != null) {
            val luminance = (0.299 * tintColor.red + 0.587 * tintColor.green + 0.114 * tintColor.blue)
            if (luminance < 0.5f) ComposeColor.White else ComposeColor.Black
        } else {
            ComposeColor.Black
        }
    }

    val shapeAlpha = if (isPro) 1.0f else 0.92f
    val textAlpha = if (isPro) 1.0f else 0.92f

    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer(
                rotationZ = rotation,
                scaleX = scale,
                scaleY = scale
            )
            .then(
                if (blur > 0f) Modifier.blur(blur.dp) else Modifier
            )
            .background(
                color = if (isLocked) ComposeColor(0xFFFFF0F6) else ComposeColor(0xFFFAFAFA),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isLocked) 2.5.dp else 1.5.dp,
                color = if (isLocked) ColorAccent else ComposeColor(0xFFE5E7EB),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !isRolling && value > 0) { onClick() },
        contentAlignment = Alignment.Center
    ) {

        Image(
            painter = painterResource(id = getDiceShapeRes(diceType)),
            contentDescription = "Dice Image",
            colorFilter = if (tintColor != null) ColorFilter.tint(tintColor) else null,
            alpha = shapeAlpha,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        )

        if (value > 0) {
            Text(
                text = value.toString(),
                color = textColor.copy(alpha = textAlpha),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = BebasFontFamily,
                textAlign = TextAlign.Center
            )
        }

        if (isLocked) {
            Image(
                painter = painterResource(id = R.drawable.ic_lock),
                contentDescription = "Locked",
                colorFilter = ColorFilter.tint(ColorAccent),
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            )
        }
    }
}

@Composable
fun ProStatusBanner(
    isLifetime: Boolean,
    isActive: Boolean,
    remainingMillis: Long,
    onWatchAd: () -> Unit,
    onBuyLifetime: () -> Unit
) {
    if (isLifetime) return

    val gradientBrush = remember {
        Brush.linearGradient(
            colors = listOf(ColorPrimary, ColorAccent)
        )
    }

    val trialBrush = remember {
        Brush.linearGradient(
            colors = listOf(GreenSoft, ComposeColor.White)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isActive) trialBrush else gradientBrush)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isActive) {
                Text(
                    text = "Trial Active ⚡",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(remainingMillis)
                val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60
                val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60
                val timeStr = String.format("Expires in: %02dh %02dm %02ds. Upgrade to Lifetime Pro to unlock custom themes & permanent ad removal!", hours, minutes, seconds)

                Text(
                    text = timeStr,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onBuyLifetime,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
                ) {
                    Text(
                        text = "Upgrade to Lifetime Pro 💎",
                        color = ComposeColor.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "Unlock Pro Features 💎",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = ComposeColor.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Watch a quick video to unlock all dice & no ads for 6 hours, or get Lifetime Pro!",
                    fontSize = 13.sp,
                    color = ComposeColor.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onWatchAd,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ComposeColor.White)
                    ) {
                        Text("Watch Ad 🎬", color = ComposeColor(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onBuyLifetime,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorAccent)
                    ) {
                        Text("Go Lifetime 💎", color = ComposeColor.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun getVisibleIndicesForCount(count: Int): List<Int> = when (count) {
    1 -> listOf(5)
    2 -> listOf(4, 6)
    3 -> listOf(1, 5, 9)
    4 -> listOf(1, 3, 7, 9)
    5 -> listOf(1, 3, 5, 7, 9)
    6 -> listOf(1, 3, 4, 6, 7, 9)
    7 -> listOf(1, 3, 4, 5, 6, 7, 9)
    8 -> listOf(1, 2, 3, 4, 6, 7, 8, 9)
    9 -> (1..9).toList()
    else -> listOf(5)
}
