package com.shuowen.point24

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.onSizeChanged
import com.shuowen.point24.game.Point24
import com.shuowen.point24.ui.theme.ButterCream
import com.shuowen.point24.ui.theme.ButterYellow
import com.shuowen.point24.ui.theme.CreamCard
import com.shuowen.point24.ui.theme.DeepTealGlow
import com.shuowen.point24.ui.theme.DeepTeal
import com.shuowen.point24.ui.theme.OliveGold
import com.shuowen.point24.ui.theme.Pine
import com.shuowen.point24.ui.theme.Point24Theme
import com.shuowen.point24.ui.theme.RoseDust
import com.shuowen.point24.ui.theme.ThemeMode
import com.shuowen.point24.ui.theme.WarmGold
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var themeMode by remember { mutableStateOf(loadThemeModePreference()) }
            val systemDensity = LocalDensity.current
            val cappedDensity = remember(systemDensity) {
                Density(
                    density = systemDensity.density,
                    fontScale = systemDensity.fontScale.coerceAtMost(MAX_FONT_SCALE)
                )
            }

            CompositionLocalProvider(LocalDensity provides cappedDensity) {
                Point24Theme(themeMode = themeMode) {
                    val navigationBarColor = MaterialTheme.colorScheme.background
                    val useDarkNavigationIcons = navigationBarColor.luminance() > 0.5f

                    SideEffect {
                        @Suppress("DEPRECATION")
                        window.navigationBarColor = navigationBarColor.toArgb()
                        WindowCompat.getInsetsController(window, window.decorView)
                            .isAppearanceLightNavigationBars = useDarkNavigationIcons
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            window.isNavigationBarContrastEnforced = false
                        }
                    }

                    Surface(modifier = Modifier.fillMaxSize()) {
                        Point24App(
                            themeMode = themeMode,
                            onThemeModeChange = { nextMode ->
                                themeMode = nextMode
                                saveThemeModePreference(nextMode)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun loadThemeModePreference(): ThemeMode {
        val value = getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
            .getString(THEME_MODE_KEY, ThemeMode.System.name)
        return ThemeMode.entries.firstOrNull { it.name == value } ?: ThemeMode.System
    }

    private fun saveThemeModePreference(themeMode: ThemeMode) {
        getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(THEME_MODE_KEY, themeMode.name)
            .apply()
    }
}

private enum class ResultTab {
    Hint,
    Answer
}

private data class KeypadAction(
    val label: String,
    val kind: KeypadActionKind,
)

private enum class KeypadActionKind {
    Card,
    Delete,
    Clear,
    Query,
}

private val keypadActions = listOf(
    KeypadAction("A", KeypadActionKind.Card),
    KeypadAction("2", KeypadActionKind.Card),
    KeypadAction("3", KeypadActionKind.Card),
    KeypadAction("4", KeypadActionKind.Card),
    KeypadAction("5", KeypadActionKind.Card),
    KeypadAction("6", KeypadActionKind.Card),
    KeypadAction("7", KeypadActionKind.Card),
    KeypadAction("8", KeypadActionKind.Card),
    KeypadAction("9", KeypadActionKind.Card),
    KeypadAction("10", KeypadActionKind.Card),
    KeypadAction("J", KeypadActionKind.Card),
    KeypadAction("Q", KeypadActionKind.Card),
    KeypadAction("K", KeypadActionKind.Card),
    KeypadAction("删除", KeypadActionKind.Delete),
    KeypadAction("清空", KeypadActionKind.Clear),
    KeypadAction("查询", KeypadActionKind.Query),
)

private fun cardValueOf(label: String): Int = when (label) {
    "A" -> 1
    "J" -> 11
    "Q" -> 12
    "K" -> 13
    else -> label.toInt()
}

private fun cardLabelOf(value: Int): String = when (value) {
    1 -> "A"
    11 -> "J"
    12 -> "Q"
    13 -> "K"
    else -> value.toString()
}

private fun randomCardLabels(requireSolution: Boolean): List<String> {
    while (true) {
        val values = List(4) { Random.nextInt(1, 14) }
        val hasSolution = Point24.calc24(*values.toIntArray()).isNotEmpty()
        if (!requireSolution || hasSolution) {
            return values.map(::cardLabelOf)
        }
    }
}

private const val THEME_PREFS = "point24_theme_prefs"
private const val THEME_MODE_KEY = "theme_mode"
private const val MAX_FONT_SCALE = 1.2f

private data class PaletteOverride(
    val cardKeyContainer: Color,
    val cardKeyContent: Color,
    val cardKeyBorder: Color,
    val activeSlotAccent: Color,
    val activeSlotContainer: Color,
    val resetButtonContainer: Color,
    val resetButtonContent: Color,
    val clearButtonContainer: Color,
    val clearButtonBorder: Color,
)

@Composable
private fun rememberPaletteOverride(): PaletteOverride {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkPalette = colorScheme.background.luminance() < 0.5f

    return if (isDarkPalette) {
        PaletteOverride(
            cardKeyContainer = DeepTeal,
            cardKeyContent = CreamCard,
            cardKeyBorder = Pine,
            activeSlotAccent = WarmGold,
            activeSlotContainer = DeepTealGlow,
            resetButtonContainer = colorScheme.tertiary,
            resetButtonContent = colorScheme.onTertiary,
            clearButtonContainer = colorScheme.errorContainer,
            clearButtonBorder = colorScheme.error.copy(alpha = 0.65f)
        )
    } else {
        PaletteOverride(
            cardKeyContainer = colorScheme.secondaryContainer,
            cardKeyContent = colorScheme.onSecondaryContainer,
            cardKeyBorder = colorScheme.outline.copy(alpha = 0.55f),
            activeSlotAccent = ButterYellow,
            activeSlotContainer = ButterCream,
            resetButtonContainer = colorScheme.surface,
            resetButtonContent = colorScheme.tertiary,
            clearButtonContainer = RoseDust,
            clearButtonBorder = colorScheme.error.copy(alpha = 0.22f)
        )
    }
}

@Composable
fun Point24App(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    var cards by remember { mutableStateOf(List(4) { "" }) }
    var activeIndex by remember { mutableStateOf<Int?>(null) }
    var hasQueried by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(ResultTab.Hint) }
    var hintMessage by remember { mutableStateOf("请先输入四张牌，再点击查询。") }
    var answers by remember { mutableStateOf<List<String>>(emptyList()) }
    var bottomAreaHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val paletteOverride = rememberPaletteOverride()

    fun clearAll() {
        cards = List(4) { "" }
        activeIndex = null
        hasQueried = false
        selectedTab = ResultTab.Hint
        hintMessage = "请先输入四张牌，再点击查询。"
        answers = emptyList()
    }

    fun applyCard(card: String) {
        val manualIndex = activeIndex
        val firstEmpty = cards.indexOfFirst { it.isBlank() }
        val targetIndex = when {
            manualIndex != null -> manualIndex
            firstEmpty >= 0 -> firstEmpty
            else -> cards.lastIndex
        }

        cards = cards.toMutableList().apply {
            this[targetIndex] = card
        }
        activeIndex = cards.getOrNull(targetIndex + 1)?.let { targetIndex + 1 }
        hasQueried = false
    }

    fun deleteCard() {
        val mutableCards = cards.toMutableList()
        when {
            activeIndex != null && mutableCards[activeIndex!!].isNotBlank() -> {
                mutableCards[activeIndex!!] = ""
            }

            activeIndex != null && activeIndex!! > 0 -> {
                val previousIndex = activeIndex!! - 1
                mutableCards[previousIndex] = ""
                activeIndex = previousIndex
            }

            mutableCards.indexOfLast { it.isNotBlank() } >= 0 -> {
                val previousIndex = mutableCards.indexOfLast { it.isNotBlank() }
                mutableCards[previousIndex] = ""
                activeIndex = previousIndex
            }

            mutableCards.first().isNotBlank() -> {
                mutableCards[0] = ""
            }
        }
        cards = mutableCards
        hasQueried = false
    }

    fun runQuery() {
        if (cards.any { it.isBlank() }) {
            hasQueried = true
            selectedTab = ResultTab.Hint
            hintMessage = "请先填满四张牌。"
            answers = emptyList()
            return
        }

        val numericCards = cards.map(::cardValueOf).toIntArray()
        val resultList = Point24.calc24(*numericCards)
        answers = resultList
        hintMessage = if (resultList.isEmpty()) {
            "这组牌无解。"
        } else {
            "这组牌共有 ${resultList.size} 种答案（去重后）。"
        }
        hasQueried = true
        selectedTab = ResultTab.Hint
    }

    fun fillRandomCards(requireSolution: Boolean) {
        cards = randomCardLabels(requireSolution)
        activeIndex = null
        hasQueried = false
        selectedTab = ResultTab.Hint
        hintMessage = if (requireSolution) {
            "已生成一组可解牌面。"
        } else {
            "已生成一组随机牌面。"
        }
        answers = emptyList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    activeIndex = null
                })
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "24 POINT",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 3.sp
                )

                ThemeToggleButton(
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

//            Text(
//                text = "四张牌，直接算",
//                style = MaterialTheme.typography.headlineLarge,
//                color = MaterialTheme.colorScheme.onBackground
//            )
//
//            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (hasQueried) "切换查看提示或完整答案。" else "点击下方牌面键盘输入",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                cards.forEachIndexed { index, card ->
                    CardSlot(
                        value = card,
                        isActive = !hasQueried && index == activeIndex,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (!hasQueried) {
                                activeIndex = index
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!hasQueried) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RandomFillButton(
                        text = "来组可解",
                        modifier = Modifier.weight(1f),
                        onClick = { fillRandomCards(requireSolution = true) }
                    )
                    RandomFillButton(
                        text = "随便来组",
                        modifier = Modifier.weight(1f),
                        onClick = { fillRandomCards(requireSolution = false) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (hasQueried) {
                ResultPanel(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    hintMessage = hintMessage,
                    answers = answers,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Spacer(modifier = Modifier.height(bottomAreaHeight.dp + 12.dp))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .onSizeChanged {
                    bottomAreaHeight = with(density) { it.height.toDp().value.toInt() }
                },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!hasQueried) {
                Keypad(
                    queryEnabled = cards.none { it.isBlank() },
                    onAction = { action ->
                        when (action.kind) {
                            KeypadActionKind.Card -> applyCard(action.label)
                            KeypadActionKind.Delete -> deleteCard()
                            KeypadActionKind.Clear -> clearAll()
                            KeypadActionKind.Query -> runQuery()
                        }
                    }
                )
            } else {
                Button(
                    onClick = { clearAll() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = paletteOverride.resetButtonContainer,
                        contentColor = paletteOverride.resetButtonContent
                    )
                ) {
                    Text(
                        text = "重新输入",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun RandomFillButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun ThemeToggleButton(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                RoundedCornerShape(18.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        ) {
            val selectedIndex = ThemeMode.entries.indexOf(themeMode).coerceAtLeast(0)
            val segmentWidth = maxWidth / ThemeMode.entries.size
            val indicatorOffset by animateDpAsState(
                targetValue = segmentWidth * selectedIndex,
                animationSpec = tween(durationMillis = 260),
                label = "theme selection indicator"
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(segmentWidth)
                    .height(36.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )

            Row(modifier = Modifier.fillMaxSize()) {
                ThemeMode.entries.forEach { mode ->
                    val selected = mode == themeMode
                    val label = when (mode) {
                        ThemeMode.System -> "系统色"
                        ThemeMode.Light -> "亮色"
                        ThemeMode.Dark -> "暗色"
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onThemeModeChange(mode) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardSlot(
    value: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val paletteOverride = rememberPaletteOverride()
    val borderColor = if (isActive) {
        paletteOverride.activeSlotAccent
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    }
    val containerColor = if (isActive) {
        paletteOverride.activeSlotContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .height(92.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isActive) 2.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 10.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (value.isBlank()) "?" else value,
                style = MaterialTheme.typography.displaySmall,
                color = if (value.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                } else if (isActive) {
                    paletteOverride.activeSlotAccent
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun ResultPanel(
    selectedTab: ResultTab,
    onTabSelected: (ResultTab) -> Unit,
    hintMessage: String,
    answers: List<String>,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val resultBodyMaxHeight = (configuration.screenHeightDp * 0.34f).dp

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            ResultTabs(selectedTab = selectedTab, onTabSelected = onTabSelected)

            Spacer(modifier = Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = resultBodyMaxHeight)
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedTab) {
                    ResultTab.Hint -> {
                        Text(
                            text = "提示",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = hintMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    ResultTab.Answer -> {
                        Text(
                            text = "答案",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (answers.isEmpty()) {
                            Text(
                                text = "没有可展示的解法。",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            answers.forEachIndexed { index, expr ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = "$expr = 24",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (index != answers.lastIndex) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun ResultTabs(
    selectedTab: ResultTab,
    onTabSelected: (ResultTab) -> Unit,
) {
    val selectedFraction by animateFloatAsState(
        targetValue = if (selectedTab == ResultTab.Hint) 0f else 1f,
        animationSpec = tween(280),
        label = "result_tab_indicator"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(4.dp)
    ) {
        val tabWidth = maxWidth / 2
        val indicatorOffset = tabWidth * selectedFraction

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(tabWidth)
                    .offset(x = indicatorOffset)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .fillMaxSize()
            )

            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                listOf(
                    ResultTab.Hint to "提示",
                    ResultTab.Answer to "答案"
                ).forEach { (tab, label) ->
                    val selected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Keypad(
    queryEnabled: Boolean,
    onAction: (KeypadAction) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            userScrollEnabled = false,
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(keypadActions) { action ->
                KeypadButton(
                    action = action,
                    enabled = action.kind != KeypadActionKind.Query || queryEnabled,
                    onClick = { onAction(action) }
                )
            }
        }
    }
}

@Composable
private fun KeypadButton(
    action: KeypadAction,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val paletteOverride = rememberPaletteOverride()
    val containerColor = when (action.kind) {
        KeypadActionKind.Card -> paletteOverride.cardKeyContainer
        KeypadActionKind.Delete -> MaterialTheme.colorScheme.secondaryContainer
        KeypadActionKind.Clear -> paletteOverride.clearButtonContainer
        KeypadActionKind.Query -> MaterialTheme.colorScheme.primary
    }
    val contentColor = when (action.kind) {
        KeypadActionKind.Card -> paletteOverride.cardKeyContent
        KeypadActionKind.Delete -> MaterialTheme.colorScheme.onSecondaryContainer
        KeypadActionKind.Clear -> MaterialTheme.colorScheme.onErrorContainer
        KeypadActionKind.Query -> MaterialTheme.colorScheme.onPrimary
    }
    val borderColor = when (action.kind) {
        KeypadActionKind.Card -> paletteOverride.cardKeyBorder
        KeypadActionKind.Delete -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
        KeypadActionKind.Clear -> paletteOverride.clearButtonBorder
        KeypadActionKind.Query -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.35f))
            .border(
                BorderStroke(1.dp, if (enabled) borderColor else borderColor.copy(alpha = 0.35f)),
                RoundedCornerShape(18.dp)
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = action.label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) contentColor else contentColor.copy(alpha = 0.45f),
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun Point24AppPreview() {
    Point24Theme {
        Point24App(
            themeMode = ThemeMode.System,
            onThemeModeChange = {}
        )
    }
}
