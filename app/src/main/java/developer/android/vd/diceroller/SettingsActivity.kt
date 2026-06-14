package developer.android.vd.diceroller

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

class SettingsActivity : ComponentActivity() {

    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup Billing
        billingManager = BillingManager(this)

        onBackPressedDispatcher.addCallback(this) {
            closeWithAnimation()
        }

        setContent {
            var proActive by remember { mutableStateOf(PrefsHelper.isProActive(this)) }
            var lifetimePro by remember { mutableStateOf(PrefsHelper.isLifetimePro(this)) }
            var remainingTime by remember { mutableStateOf(PrefsHelper.formatRemainingTime(this)) }

            // Establish billing connections and callbacks
            LaunchedEffect(Unit) {
                billingManager.setListener(object : BillingManager.BillingListener {
                    override fun onPurchaseSuccess() {
                        proActive = PrefsHelper.isProActive(this@SettingsActivity)
                        lifetimePro = PrefsHelper.isLifetimePro(this@SettingsActivity)
                        remainingTime = PrefsHelper.formatRemainingTime(this@SettingsActivity)
                    }

                    override fun onPurchaseFailure(error: String) {
                        Toast.makeText(this@SettingsActivity, "Purchase failed: $error", Toast.LENGTH_LONG).show()
                    }

                    override fun onBillingClientReady() {}

                    override fun onPurchasePending() {
                        Toast.makeText(this@SettingsActivity, "Purchase pending...", Toast.LENGTH_LONG).show()
                    }
                })
                billingManager.startConnection()
            }

            DiceRollerTheme {
                SettingsScreen(
                    billingManager = billingManager,
                    proActive = proActive,
                    lifetimePro = lifetimePro,
                    remainingTime = remainingTime,
                    onBackClick = { closeWithAnimation() },
                    onProStatusChanged = {
                        proActive = PrefsHelper.isProActive(this)
                        lifetimePro = PrefsHelper.isLifetimePro(this)
                        remainingTime = PrefsHelper.formatRemainingTime(this)
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        billingManager.destroy()
        super.onDestroy()
    }

    private fun closeWithAnimation() {
        setResult(RESULT_OK)
        finishAfterTransition()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    billingManager: BillingManager,
    proActive: Boolean,
    lifetimePro: Boolean,
    remainingTime: String,
    onBackClick: () -> Unit,
    onProStatusChanged: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Preferences states
    var selectedColor by remember { mutableStateOf(PrefsHelper.getBackgroundColor(context)) }
    var isTotalVisible by remember { mutableStateOf(!PrefsHelper.isTotalHidden(context)) }
    var isVibrationEnabled by remember { mutableStateOf(PrefsHelper.isVibrationEnabled(context)) }
    var isShakeEnabled by remember { mutableStateOf(PrefsHelper.isShakeToRollEnabled(context)) }
    var isSoundEnabled by remember { mutableStateOf(PrefsHelper.isSoundEffectsEnabled(context)) }

    // Dialog flags
    var showColorDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showUpsellDialog by remember { mutableStateOf(false) }

    // Rating value
    var rating by remember { mutableFloatStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // GoPro Billing Container Card
            ProStatusCard(
                proActive = proActive,
                lifetimePro = lifetimePro,
                remainingTime = remainingTime,
                onBuyClick = { billingManager.launchPurchaseFlow(context as SettingsActivity) },
                onRestoreClick = {
                    billingManager.restorePurchases { restored ->
                        if (restored) {
                            Toast.makeText(context, "Purchases restored successfully! ✅", Toast.LENGTH_SHORT).show()
                            onProStatusChanged()
                        } else {
                            Toast.makeText(context, "No previous Pro purchases found.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            Divider(color = ComposeColor.LightGray.copy(alpha = 0.5f), thickness = 1.dp)

            // Background Color Picker Button
            Card(
                onClick = { showColorDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ComposeColor.LightGray.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Background Color 🎨",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(ComposeColor(selectedColor))
                            .border(1.dp, ComposeColor.Gray, CircleShape)
                    )
                }
            }

            // Dice Theme Button
            Card(
                onClick = {
                    if (lifetimePro) {
                        showThemeDialog = true
                    } else {
                        showUpsellDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ComposeColor.LightGray.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Choose Dice Theme 🎲",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    val currentDiceColor = PrefsHelper.getDiceColor(context)
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (currentDiceColor == Color.TRANSPARENT) ComposeColor.White else ComposeColor(currentDiceColor))
                            .border(1.dp, ComposeColor.Gray, RoundedCornerShape(4.dp))
                    )
                }
            }

            Divider(color = ComposeColor.LightGray.copy(alpha = 0.5f), thickness = 1.dp)

            // Switch Toggles
            SettingsSwitchRow(
                title = "Show Total",
                description = "Show the sum of all rolled dice",
                checked = isTotalVisible,
                onCheckedChange = {
                    isTotalVisible = it
                    PrefsHelper.setTotalHidden(context, !it)
                }
            )

            SettingsSwitchRow(
                title = "Enable Vibration",
                description = "Haptic feedback on clicks and rolls",
                checked = isVibrationEnabled,
                onCheckedChange = {
                    isVibrationEnabled = it
                    PrefsHelper.setVibrationEnabled(context, it)
                }
            )

            SettingsSwitchRow(
                title = "Shake to Roll",
                description = "Roll the dice by shaking the device",
                checked = isShakeEnabled,
                onCheckedChange = {
                    isShakeEnabled = it
                    PrefsHelper.setShakeToRollEnabled(context, it)
                }
            )

            SettingsSwitchRow(
                title = "Sound Effects",
                description = "Play synthesized clack sounds when rolling",
                checked = isSoundEnabled,
                onCheckedChange = {
                    isSoundEnabled = it
                    PrefsHelper.setSoundEffectsEnabled(context, it)
                }
            )

            Divider(color = ComposeColor.LightGray.copy(alpha = 0.5f), thickness = 1.dp)

            // Star Rating component
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enjoying the app? Rate us!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { starIndex ->
                        val isSelected = rating >= starIndex
                        IconButton(
                            onClick = {
                                rating = starIndex.toFloat()
                                if (rating >= 4f) {
                                    val marketUri = "market://details?id=${context.packageName}".toUri()
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, marketUri))
                                    } catch (e: ActivityNotFoundException) {
                                        val webUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
                                        context.startActivity(Intent(Intent.ACTION_VIEW, webUrl.toUri()))
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = null,
                                tint = if (isSelected) ColorPrimary else ComposeColor.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            // Other Apps link button
            Button(
                onClick = {
                    val developerId = "Vaibhav+Deshmukh"
                    val marketUri = "market://search?q=pub:$developerId".toUri()
                    val marketIntent = Intent(Intent.ACTION_VIEW, marketUri)
                    try {
                        context.startActivity(marketIntent)
                    } catch (e: ActivityNotFoundException) {
                        val webUrl = "https://play.google.com/store/apps/developer?id=$developerId"
                        val customTabsIntent = CustomTabsIntent.Builder()
                            .setShowTitle(true)
                            .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                            .setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary))
                            .setStartAnimations(context, android.R.anim.fade_in, android.R.anim.fade_out)
                            .setExitAnimations(context, android.R.anim.fade_in, android.R.anim.fade_out)
                            .build()
                        customTabsIntent.launchUrl(context, webUrl.toUri())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "Our Other Apps 📱",
                    color = ComposeColor.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }

    // Dialog: Background Color Picker
    if (showColorDialog) {
        val colors = listOf("Slate Dark 🌑", "Nordic Light ❄️", "Soft Lavender 🪻", "Mint Breeze 🍃", "Peach Cream 🍑", "Classic White 🏳️")
        val colorValues = listOf(
            Color.parseColor("#0F172A"),
            Color.parseColor("#F0F4F8"),
            Color.parseColor("#F5F3FF"),
            Color.parseColor("#ECFDF5"),
            Color.parseColor("#FFF7ED"),
            Color.WHITE
        )

        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = { Text("Choose Background Color", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEachIndexed { idx, label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val valColor = colorValues[idx]
                                    selectedColor = valColor
                                    PrefsHelper.saveBackgroundColor(context, valColor)
                                    showColorDialog = false
                                    (context as? SettingsActivity)?.setResult(android.app.Activity.RESULT_OK)
                                    (context as? SettingsActivity)?.finishAfterTransition()
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(ComposeColor(colorValues[idx]))
                                    .border(1.dp, ComposeColor.Gray, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showColorDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Dice Themes
    if (showThemeDialog) {
        val themes = listOf("Classic (White)", "Golden 🏆", "Blood Red 🩸", "Neon Green 🍏", "Midnight Purple 🍇")
        val colors = listOf(
            Color.TRANSPARENT,
            Color.parseColor("#FFD700"),
            Color.parseColor("#8B0000"),
            Color.parseColor("#39FF14"),
            Color.parseColor("#4B0082")
        )

        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Dice Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    themes.forEachIndexed { idx, label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    PrefsHelper.saveDiceColor(context, colors[idx])
                                    Toast.makeText(context, "$label applied!", Toast.LENGTH_SHORT).show()
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (colors[idx] == Color.TRANSPARENT) ComposeColor.White else ComposeColor(colors[idx]))
                                    .border(1.dp, ComposeColor.Gray, RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Upsell Custom Premium Theme dialog
    if (showUpsellDialog) {
        AlertDialog(
            onDismissRequest = { showUpsellDialog = false },
            title = { Text("Premium Feature 💎", fontWeight = FontWeight.Bold) },
            text = {
                Text("Dice Customization is an exclusive feature for Lifetime Pro members. It's not available in the rewarded trial.\n\nReady to get your permanent gold dice?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpsellDialog = false
                        billingManager.launchPurchaseFlow(context as SettingsActivity)
                    }
                ) {
                    Text("Go Pro")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpsellDialog = false }) {
                    Text("Maybe Later")
                }
            }
        )
    }
}

@Composable
fun ProStatusCard(
    proActive: Boolean,
    lifetimePro: Boolean,
    remainingTime: String,
    onBuyClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    val gradientBrush = remember {
        Brush.linearGradient(
            colors = listOf(PremiumPurpleStart, PremiumPurpleEnd)
        )
    }

    val trialBrush = remember {
        Brush.linearGradient(
            colors = listOf(GreenSoft, ComposeColor.White)
        )
    }

    when {
        lifetimePro -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GreenSoft)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Lifetime Pro Active ✅",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Thank you for your support! You have permanent access to all features and themes.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        proActive -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(trialBrush)
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Trial Active ⏳",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Expires in: $remainingTime. Upgrade to Lifetime for permanent access and exclusive themes!",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onBuyClick,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
                            ) {
                                Text("Upgrade to Lifetime 💎", fontSize = 12.sp, color = ComposeColor.White)
                            }
                            OutlinedButton(
                                onClick = onRestoreClick,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Restore", fontSize = 12.sp, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }
        else -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gradientBrush)
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Upgrade to Pro 💎",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = ComposeColor.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Remove ads forever and unlock all advanced dice and themes.",
                            fontSize = 13.sp,
                            color = ComposeColor.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onBuyClick,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ComposeColor.White)
                            ) {
                                Text("Buy Pro", fontSize = 12.sp, color = ColorPrimary)
                            }
                            Button(
                                onClick = onRestoreClick,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ComposeColor.White.copy(alpha = 0.2f))
                            ) {
                                Text("Restore", fontSize = 12.sp, color = ComposeColor.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(text = description, color = TextSecondary, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = ColorPrimary)
        )
    }
}
