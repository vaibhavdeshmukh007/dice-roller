package developer.android.vd.diceroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class HistoryActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.restoreHistory()

        setContent {
            DiceRollerTheme {
                HistoryScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val history by viewModel.rollHistory.observeAsState(emptyList())
    val context = androidx.compose.ui.platform.LocalContext.current
    val isPro = remember { PrefsHelper.isProActive(context) }
    val uiItems = remember(history, isPro) {
        buildUiItems(history, isPro, showProHeader = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Roll History", fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(uiItems) { item ->
                when (item) {
                    is HistoryUiItem.ProHeader -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    "Pro History",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                                Text(
                                    "Unlimited roll history",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                    is HistoryUiItem.DateHeader -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = item.label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    is HistoryUiItem.Roll -> {
                        RollHistoryItem(entry = item.entry)
                    }
                    is HistoryUiItem.Empty -> {
                        Column(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("🎲", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No rolls yet", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Roll some dice to see history here",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RollHistoryItem(entry: RollEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = entry.diceType.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${entry.diceCount} dice",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                entry.results.forEach { valStr ->
                    Box(
                        modifier = Modifier.size(26.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = getDiceShapeRes(entry.diceType)),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = valStr.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
        Text(
            text = entry.total.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

fun getDiceShapeRes(type: DiceType): Int = when (type) {
    DiceType.D4 -> R.drawable.dice_d4_base
    DiceType.D6 -> R.drawable.dice_d6_base
    DiceType.D8 -> R.drawable.dice_d8_base
    DiceType.D10 -> R.drawable.dice_d10_base
    DiceType.D12 -> R.drawable.dice_d12_base
    DiceType.D20 -> R.drawable.dice_d20_base
}

fun buildUiItems(history: List<RollEntry>, isPro: Boolean, showProHeader: Boolean): List<HistoryUiItem> {
    if (history.isEmpty()) {
        return listOf(HistoryUiItem.Empty)
    }

    val items = mutableListOf<HistoryUiItem>()

    if (isPro && showProHeader) {
        items += HistoryUiItem.ProHeader
    }

    var lastLabel: String? = null
    history.forEach { entry ->
        val label = entry.timestamp.toDateLabel()
        if (label != lastLabel) {
            items += HistoryUiItem.DateHeader(label)
            lastLabel = label
        }
        items += HistoryUiItem.Roll(entry)
    }

    return items
}
