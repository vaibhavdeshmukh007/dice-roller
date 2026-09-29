package developer.android.vd.diceroller

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.play.core.review.ReviewManagerFactory

class SettingsActivity : ComponentActivity() {

    private lateinit var billingManager: BillingManager
    private var rewardedAd: RewardedAd? = null

    // Reactive states synced with Composable UI
    private val proActiveState = mutableStateOf(false)
    private val lifetimeProState = mutableStateOf(false)
    private val remainingMillisState = mutableStateOf(0L)
    private val backgroundColorState = mutableStateOf(Color.WHITE)

    private val countdownHandler = Handler(Looper.getMainLooper())
    private val countdownRunnable = object : Runnable {
        override fun run() {
            updateProStatus()
            countdownHandler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        backgroundColorState.value = PrefsHelper.getBackgroundColor(this)
        updateProStatus()

        setupBilling()
        loadRewardedAd()

        onBackPressedDispatcher.addCallback(this) {
            closeWithAnimation()
        }

        setContent {
            DiceRollerTheme {
                SettingsScreen(
                    billingManager = billingManager,
                    proActive = proActiveState.value,
                    lifetimePro = lifetimeProState.value,
                    remainingMillis = remainingMillisState.value,
                    initialBackgroundColor = backgroundColorState.value,
                    onBackgroundColorChanged = { newColor ->
                        backgroundColorState.value = newColor
                    },
                    onWatchAdClick = {
                        showRewardedAd()
                    },
                    onBackClick = { closeWithAnimation() },
                    onProStatusChanged = {
                        updateProStatus()
                    }
                )
            }
        }
    }

    private fun setupBilling() {
        billingManager = BillingManager(this)
        billingManager.setListener(object : BillingManager.BillingListener {
            override fun onPurchaseSuccess() {
                runOnUiThread {
                    updateProStatus()
                }
            }

            override fun onPurchaseFailure(error: String) {
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "Purchase failed: $error", Toast.LENGTH_LONG).show()
                }
            }

            override fun onBillingClientReady() {}

            override fun onPurchasePending() {
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "Purchase processing... Pro will unlock automatically when confirmed.", Toast.LENGTH_LONG).show()
                }
            }
        })
        billingManager.startConnection()
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
            }
        )
    }

    private fun showRewardedAd() {
        rewardedAd?.show(this) {
            PrefsHelper.activateProTrial(this)
            updateProStatus()
            Toast.makeText(this, "🎉 Pro unlocked for 6 hours!", Toast.LENGTH_LONG).show()
            loadRewardedAd()
        } ?: run {
            AlertDialog.Builder(this)
                .setTitle("Ad Not Ready 🚫")
                .setMessage("There are no video ads available right now. Please check your internet connection and try again later, or upgrade to Pro Lifetime to unlock everything forever!")
                .setPositiveButton("Buy Lifetime 💎") { _, _ ->
                    billingManager.launchPurchaseFlow(this)
                }
                .setNegativeButton("Cancel", null)
                .show()
            loadRewardedAd()
        }
    }

    private fun updateProStatus() {
        proActiveState.value = PrefsHelper.isProActive(this)
        lifetimeProState.value = PrefsHelper.isLifetimePro(this)
        remainingMillisState.value = PrefsHelper.getProRemainingMillis(this)
    }

    private fun closeWithAnimation() {
        setResult(RESULT_OK)
        finishAfterTransition()
    }

    fun launchReviewFlow(onComplete: () -> Unit = {}) {
        val manager = ReviewManagerFactory.create(this)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val startTime = System.currentTimeMillis()
                val flow = manager.launchReviewFlow(this, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    val duration = System.currentTimeMillis() - startTime
                    if (duration < 400) {
                        redirectToPlayStore()
                    }
                    PrefsHelper.markReviewAsked(this)
                    onComplete()
                }
            } else {
                redirectToPlayStore()
                PrefsHelper.markReviewAsked(this)
                onComplete()
            }
        }
    }

    private fun redirectToPlayStore() {
        val packageName = packageName
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    override fun onResume() {
        super.onResume()
        updateProStatus()
        if (!PrefsHelper.isLifetimePro(this)) {
            countdownHandler.post(countdownRunnable)
        }
    }

    override fun onPause() {
        countdownHandler.removeCallbacks(countdownRunnable)
        super.onPause()
    }

    override fun onDestroy() {
        countdownHandler.removeCallbacks(countdownRunnable)
        billingManager.destroy()
        super.onDestroy()
    }
}

private data class BackgroundPalette(
    val name: String,
    val emoji: String,
    val colorValue: Int
)

private data class DiceFaceTheme(
    val name: String,
    val shortName: String,
    val colorValue: Int,
    val isProOnly: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    billingManager: BillingManager,
    proActive: Boolean,
    lifetimePro: Boolean,
    remainingMillis: Long,
    initialBackgroundColor: Int,
    onBackgroundColorChanged: (Int) -> Unit,
    onWatchAdClick: () -> Unit,
    onBackClick: () -> Unit,
    onProStatusChanged: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    // Preferences states
    var selectedColor by remember { mutableIntStateOf(initialBackgroundColor) }
    var selectedDiceColor by remember { mutableIntStateOf(PrefsHelper.getDiceColor(context)) }
    var isTotalVisible by remember { mutableStateOf(!PrefsHelper.isTotalHidden(context)) }
    var isVibrationEnabled by remember { mutableStateOf(PrefsHelper.isVibrationEnabled(context)) }
    var isShakeEnabled by remember { mutableStateOf(PrefsHelper.isShakeToRollEnabled(context)) }
    var isSoundEnabled by remember { mutableStateOf(PrefsHelper.isSoundEffectsEnabled(context)) }

    // Dialog state
    var showUpsellDialog by remember { mutableStateOf(false) }

    // Rating value
    var rating by remember { mutableFloatStateOf(0f) }

    // Palettes
    val palettes = remember {
        listOf(
            BackgroundPalette("Slate Dark", "🌑", Color.parseColor("#0F172A")),
            BackgroundPalette("Nordic Light", "❄️", Color.parseColor("#F0F4F8")),
            BackgroundPalette("Soft Lavender", "🪻", Color.parseColor("#F5F3FF")),
            BackgroundPalette("Mint Breeze", "🍃", Color.parseColor("#ECFDF5")),
            BackgroundPalette("Peach Cream", "🍑", Color.parseColor("#FFF7ED")),
            BackgroundPalette("Classic White", "🏳️", Color.WHITE)
        )
    }

    // Dice Themes
    val diceThemes = remember {
        listOf(
            DiceFaceTheme("Classic", "Classic", Color.TRANSPARENT, false),
            DiceFaceTheme("Golden 🏆", "Gold", Color.parseColor("#FFD700"), true),
            DiceFaceTheme("Blood Red 🩸", "Red", Color.parseColor("#8B0000"), true),
            DiceFaceTheme("Neon Green 🍏", "Green", Color.parseColor("#39FF14"), true),
            DiceFaceTheme("Midnight Purple 🍇", "Purple", Color.parseColor("#4B0082"), true)
        )
    }

    // Dynamic Contrast Calculation identical to MainActivity
    val isDark = remember(selectedColor) {
        val darkness = 1 - (0.299 * Color.red(selectedColor) + 0.587 * Color.green(selectedColor) + 0.114 * Color.blue(selectedColor)) / 255
        darkness >= 0.5
    }

    val textColor = if (isDark) ComposeColor.White else TextPrimary
    val textSecondaryColor = if (isDark) DarkTextSecondary else TextSecondary
    val iconColor = if (isDark) ComposeColor.White else TextPrimary
    val cardBackground = if (isDark) ComposeColor(0xFF1E293B) else ComposeColor.White
    val cardBorder = BorderStroke(1.dp, if (isDark) ComposeColor.DarkGray else ComposeColor.LightGray.copy(alpha = 0.5f))
    val dividerColor = if (isDark) ComposeColor(0xFF334155) else ComposeColor.LightGray.copy(alpha = 0.4f)

    SystemBarsColor(
        statusBarColor = selectedColor,
        darkIcons = !isDark
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor(selectedColor))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 24.dp)
        ) {
            // Unified Top Header Bar matching MainActivity height and styling
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (isDark) ComposeColor(0xFF334155) else ComposeColor(0xFFF3F4F6),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = iconColor
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Settings",
                        color = textColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                // Status chip on header
                if (lifetimePro) {
                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.linearGradient(listOf(PremiumGoldStart, PremiumGoldEnd)),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PRO 💎",
                            color = ComposeColor.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                } else if (proActive) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = ColorPrimary,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "TRIAL ⚡",
                            color = ComposeColor.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Section 1: Pro Membership Hero Card
                ProMembershipCard(
                    proActive = proActive,
                    lifetimePro = lifetimePro,
                    remainingMillis = remainingMillis,
                    isDark = isDark,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor,
                    cardBackground = cardBackground,
                    cardBorder = cardBorder,
                    onBuyClick = { billingManager.launchPurchaseFlow(context as Activity) },
                    onRestoreClick = {
                        billingManager.restorePurchases { restored ->
                            if (restored) {
                                Toast.makeText(context, "Purchases restored successfully! ✅", Toast.LENGTH_SHORT).show()
                                onProStatusChanged()
                            } else {
                                Toast.makeText(context, "No previous Pro purchases found.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onWatchAdClick = onWatchAdClick
                )

                // Section 2: Appearance & Customization Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    border = cardBorder,
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (isDark) ComposeColor(0xFF334155) else ComposeColor(0xFFEDE9FE),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = null,
                                    tint = ColorPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Appearance & Themes",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    text = "Live theme & custom dice colors",
                                    fontSize = 12.sp,
                                    color = textSecondaryColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // App Background Palette
                        Text(
                            text = "App Background Palette",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // 3x2 Grid for Background Palettes
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (rowIndex in 0..1) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (colIndex in 0..2) {
                                        val index = rowIndex * 3 + colIndex
                                        if (index < palettes.size) {
                                            val palette = palettes[index]
                                            val isSelected = selectedColor == palette.colorValue
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(
                                                        color = if (isSelected) {
                                                            if (isDark) ComposeColor(0xFF334155) else ComposeColor(0xFFE5E7EB)
                                                        } else {
                                                            if (isDark) ComposeColor(0xFF0F172A).copy(alpha = 0.5f) else ComposeColor(0xFFF9FAFB)
                                                        }
                                                    )
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) ColorAccent else (if (isDark) ComposeColor.DarkGray else ComposeColor.LightGray.copy(alpha = 0.5f)),
                                                        shape = RoundedCornerShape(14.dp)
                                                    )
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        selectedColor = palette.colorValue
                                                        onBackgroundColorChanged(palette.colorValue)
                                                        PrefsHelper.saveBackgroundColor(context, palette.colorValue)
                                                    }
                                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(ComposeColor(palette.colorValue))
                                                            .border(
                                                                width = 1.dp,
                                                                color = ComposeColor.Gray.copy(alpha = 0.5f),
                                                                shape = CircleShape
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Check,
                                                                contentDescription = "Selected",
                                                                tint = if (palette.name == "Slate Dark") ComposeColor.White else ComposeColor.Black,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = palette.name.split(" ").first(),
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = textColor,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = dividerColor, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(18.dp))

                        // Dice Face Theme Sub-section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Dice Face Theme",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                            if (!lifetimePro) {
                                Text(
                                    text = "Lifetime VIP 💎",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorAccent
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (lifetimePro) "Choose your favorite dice color theme" else "Unlock golden, blood red, neon & purple dice with Lifetime Pro",
                            fontSize = 12.sp,
                            color = textSecondaryColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Row of Dice Face Themes with mini 3D die previews
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            diceThemes.forEach { theme ->
                                val isSelected = selectedDiceColor == theme.colorValue
                                val isLocked = theme.isProOnly && !lifetimePro

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            color = if (isSelected) {
                                                if (isDark) ComposeColor(0xFF334155) else ComposeColor(0xFFE5E7EB)
                                            } else {
                                                if (isDark) ComposeColor(0xFF0F172A).copy(alpha = 0.5f) else ComposeColor(0xFFF9FAFB)
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) ColorAccent else (if (isDark) ComposeColor.DarkGray else ComposeColor.LightGray.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            if (isLocked) {
                                                showUpsellDialog = true
                                            } else {
                                                selectedDiceColor = theme.colorValue
                                                PrefsHelper.saveDiceColor(context, theme.colorValue)
                                                if (PrefsHelper.isSoundEffectsEnabled(context)) {
                                                    SoundSynthesizer.playDiceClack(context)
                                                }
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                Toast.makeText(context, "${theme.name} applied!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(vertical = 8.dp, horizontal = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier.size(36.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.dice_d6_base),
                                                contentDescription = null,
                                                colorFilter = if (theme.colorValue != Color.TRANSPARENT) ColorFilter.tint(ComposeColor(theme.colorValue)) else null,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            val faceTextColor = remember(theme.colorValue) {
                                                if (theme.colorValue != Color.TRANSPARENT) {
                                                    val tint = ComposeColor(theme.colorValue)
                                                    val lum = 0.299f * tint.red + 0.587f * tint.green + 0.114f * tint.blue
                                                    if (lum < 0.5f) ComposeColor.White else ComposeColor.Black
                                                } else {
                                                    ComposeColor.Black
                                                }
                                            }
                                            Text(
                                                text = "6",
                                                color = faceTextColor,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = BebasFontFamily
                                            )

                                            // Lock badge overlay
                                            if (isLocked) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(ComposeColor.Black.copy(alpha = 0.45f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Image(
                                                        painter = painterResource(id = R.drawable.ic_lock),
                                                        contentDescription = "Locked",
                                                        colorFilter = ColorFilter.tint(ComposeColor.White),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = theme.shortName,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = textColor,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: Gameplay Preferences Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    border = cardBorder,
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (isDark) ComposeColor(0xFF334155) else ComposeColor(0xFFEDE9FE),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Casino,
                                    contentDescription = null,
                                    tint = ColorPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Gameplay Preferences",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    text = "Audio, vibration & display options",
                                    fontSize = 12.sp,
                                    color = textSecondaryColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Show Total Switch
                        SettingsSwitchRow(
                            icon = Icons.Rounded.Calculate,
                            title = "Show Roll Total",
                            description = "Display sum counter after each roll",
                            checked = isTotalVisible,
                            isDark = isDark,
                            textColor = textColor,
                            textSecondaryColor = textSecondaryColor,
                            onCheckedChange = {
                                isTotalVisible = it
                                PrefsHelper.setTotalHidden(context, !it)
                            }
                        )

                        HorizontalDivider(color = dividerColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                        // Sound Effects Switch
                        SettingsSwitchRow(
                            icon = Icons.AutoMirrored.Rounded.VolumeUp,
                            title = "Dice Clack Audio",
                            description = "Synthesized physics clack on dice rolls",
                            checked = isSoundEnabled,
                            isDark = isDark,
                            textColor = textColor,
                            textSecondaryColor = textSecondaryColor,
                            onCheckedChange = {
                                isSoundEnabled = it
                                PrefsHelper.setSoundEffectsEnabled(context, it)
                                if (it) {
                                    SoundSynthesizer.playDiceClack(context)
                                }
                            }
                        )

                        HorizontalDivider(color = dividerColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                        // Tactile Vibration Switch
                        SettingsSwitchRow(
                            icon = Icons.Rounded.Vibration,
                            title = "Tactile Vibration",
                            description = "Haptic feedback on clicks, locks & rolls",
                            checked = isVibrationEnabled,
                            isDark = isDark,
                            textColor = textColor,
                            textSecondaryColor = textSecondaryColor,
                            onCheckedChange = {
                                isVibrationEnabled = it
                                PrefsHelper.setVibrationEnabled(context, it)
                                if (it) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        )

                        HorizontalDivider(color = dividerColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                        // Shake to Roll Switch
                        SettingsSwitchRow(
                            icon = Icons.Rounded.ScreenRotation,
                            title = "Shake to Roll",
                            description = "Roll dice by shaking your device",
                            checked = isShakeEnabled,
                            isDark = isDark,
                            textColor = textColor,
                            textSecondaryColor = textSecondaryColor,
                            onCheckedChange = {
                                isShakeEnabled = it
                                PrefsHelper.setShakeToRollEnabled(context, it)
                            }
                        )
                    }
                }

                // Section 4: Support & Community Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    border = cardBorder,
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (isDark) ComposeColor(0xFF334155) else ComposeColor(0xFFEDE9FE),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = ColorPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Support & Community",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    text = "Rate Dice Roller or discover more tools",
                                    fontSize = 12.sp,
                                    color = textSecondaryColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Interactive Star Rating
                        Text(
                            text = "Enjoying your rolls? Rate us!",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap a star to rate on Google Play",
                            fontSize = 12.sp,
                            color = textSecondaryColor
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            (1..5).forEach { starIndex ->
                                val isSelected = rating >= starIndex
                                IconButton(
                                    onClick = {
                                        rating = starIndex.toFloat()
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (rating >= 4f) {
                                            (context as? SettingsActivity)?.launchReviewFlow()
                                        } else {
                                            Toast.makeText(context, "Thanks for your feedback! ❤️", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.Star,
                                        contentDescription = "Rate $starIndex stars",
                                        tint = if (isSelected) ComposeColor(0xFFF59E0B) else (if (isDark) ComposeColor(0xFF475569) else ComposeColor(0xFFCBD5E1)),
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = dividerColor, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(18.dp))

                        // More Apps Button with vibrant gradient
                        Button(
                            onClick = {
                                val developerId = "Vaibhav+Deshmukh"
                                val marketUri = "market://search?q=pub:$developerId".toUri()
                                val marketIntent = Intent(Intent.ACTION_VIEW, marketUri)
                                try {
                                    context.startActivity(marketIntent)
                                } catch (e: ActivityNotFoundException) {
                                    val webUrl = "https://play.google.com/store/apps/developer?id=$developerId"
                                    val defaultColors = CustomTabColorSchemeParams.Builder()
                                        .setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary))
                                        .build()
                                    val customTabsIntent = CustomTabsIntent.Builder()
                                        .setDefaultColorSchemeParams(defaultColors)
                                        .setShowTitle(true)
                                        .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                                        .setStartAnimations(context, android.R.anim.fade_in, android.R.anim.fade_out)
                                        .setExitAnimations(context, android.R.anim.fade_in, android.R.anim.fade_out)
                                        .build()
                                    customTabsIntent.launchUrl(context, webUrl.toUri())
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ComposeColor.Transparent),
                            contentPadding = PaddingValues()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(listOf(ColorPrimary, ColorAccent)),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Explore Our Other Apps",
                                        color = ComposeColor.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                        contentDescription = null,
                                        tint = ComposeColor.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // App Version & Credit
                        Text(
                            text = "Dice Roller v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Crafted with ❤️ for RPG & Tabletop Gamers",
                            fontSize = 11.sp,
                            color = textSecondaryColor
                        )
                    }
                }
            }
        }
    }

    // Dialog: Lifetime Upsell Dialog
    if (showUpsellDialog) {
        AlertDialog(
            onDismissRequest = { showUpsellDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = cardBackground,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💎", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lifetime Exclusive",
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Custom dice themes (Golden, Blood Red, Neon Green, Midnight Purple) are an exclusive reward for Lifetime Pro members.",
                        fontSize = 14.sp,
                        color = textSecondaryColor,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = "Upgrade once and keep all dice themes, polyhedral dice & permanent ad removal forever!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpsellDialog = false
                        billingManager.launchPurchaseFlow(context as Activity)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ComposeColor.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(listOf(ColorAccent, ColorPrimary)),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Go Lifetime Pro 💎",
                            color = ComposeColor.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpsellDialog = false }) {
                    Text("Maybe Later", color = textSecondaryColor)
                }
            }
        )
    }
}

@Composable
private fun ProMembershipCard(
    proActive: Boolean,
    lifetimePro: Boolean,
    remainingMillis: Long,
    isDark: Boolean,
    textColor: ComposeColor,
    textSecondaryColor: ComposeColor,
    cardBackground: ComposeColor,
    cardBorder: BorderStroke,
    onBuyClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onWatchAdClick: () -> Unit
) {
    when {
        lifetimePro -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) ComposeColor(0xFF132E27) else GreenSoft
                ),
                border = BorderStroke(1.5.dp, PremiumGoldStart),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("💎", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lifetime Pro Active",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You have permanent access to all dice types (D4–D20), unlimited roll history, custom dice face themes & an ad-free experience. Thank you for your support!",
                        fontSize = 13.sp,
                        color = textSecondaryColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }
        proActive -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.5.dp, ColorPrimary.copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (isDark) listOf(ComposeColor(0xFF1E293B), ComposeColor(0xFF0F172A))
                                else listOf(GreenSoft, ComposeColor.White)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pro Trial Active",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = textColor
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(remainingMillis)
                        val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60
                        val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60
                        val timerFormatted = String.format("Expires in: %02dh %02dm %02ds", hours, minutes, seconds)

                        Box(
                            modifier = Modifier
                                .background(
                                    color = ColorPrimary.copy(alpha = if (isDark) 0.3f else 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = timerFormatted,
                                color = if (isDark) ComposeColor(0xFF818CF8) else ColorPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Advanced polyhedral dice & unlimited history are unlocked for your trial session. Upgrade to Lifetime Pro for permanent access and exclusive dice themes!",
                            fontSize = 13.sp,
                            color = textSecondaryColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onBuyClick,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ComposeColor.Transparent),
                                contentPadding = PaddingValues()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(listOf(ColorAccent, ColorPrimary)),
                                            shape = RoundedCornerShape(14.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Go Lifetime 💎",
                                        color = ComposeColor.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = onRestoreClick,
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, if (isDark) ComposeColor.DarkGray else ComposeColor.LightGray)
                            ) {
                                Text(
                                    text = "Restore",
                                    color = textColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
        else -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ColorAccent, ColorPrimary)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Unlock Pro Features 💎",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = ComposeColor.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Remove ads forever and elevate your rolls with full RPG power!",
                            fontSize = 13.sp,
                            color = ComposeColor.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Feature highlights
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = ComposeColor.Black.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ProFeatureLine("🎲", "Advanced Polyhedral Dice (D10, D12, D20)")
                            ProFeatureLine("📜", "Unlimited Roll History (10,000+ entries)")
                            ProFeatureLine("🚫", "100% Ad-Free Tabletop Experience")
                            ProFeatureLine("🎨", "Exclusive Dice Themes (Gold, Blood Red, etc.)")
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onWatchAdClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ComposeColor.White)
                            ) {
                                Text(
                                    text = "Watch Ad (6h) 🎬",
                                    color = ComposeColor(0xFF1B5E20),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = onBuyClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ComposeColor.White.copy(alpha = 0.25f)),
                                border = BorderStroke(1.dp, ComposeColor.White.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "Buy Lifetime 💎",
                                    color = ComposeColor.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(onClick = onRestoreClick) {
                            Text(
                                text = "Restore Purchases",
                                color = ComposeColor.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProFeatureLine(icon: String, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = ComposeColor.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    isDark: Boolean,
    textColor: ComposeColor,
    textSecondaryColor: ComposeColor,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (isDark) ComposeColor(0xFF334155) else ComposeColor(0xFFF3F4F6),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) ColorPrimary else (if (isDark) DarkTextSecondary else TextSecondary),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = textColor
            )
            Text(
                text = description,
                color = textSecondaryColor,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ComposeColor.White,
                checkedTrackColor = ColorPrimary,
                uncheckedTrackColor = if (isDark) ComposeColor(0xFF334155) else ComposeColor(0xFFE5E7EB)
            )
        )
    }
}
