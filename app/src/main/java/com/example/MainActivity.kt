package com.example

import android.os.Bundle
import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.viewinterop.AndroidView
import android.view.HapticFeedbackConstants
import android.media.AudioManager
import android.media.ToneGenerator
import com.example.ui.theme.MyApplicationTheme
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope

// Definition of standard gameplay levels
data class WordLevel(
    val levelNumber: Int,
    val name: String,
    val letters: String, // String of unique characters to form the words of this level
    val targetWords: List<String>
)

class MainActivity : ComponentActivity() {
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Safely initialize AdMob Mobile Ads SDK in a background thread with exception handling
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    MobileAds.initialize(this@MainActivity) {}
                }
                loadInterstitialAd()
                loadRewardedAd()
            } catch (e: Throwable) {
                // Ignore any Play Services or other runtime exceptions to guarantee smooth offline play
            }
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WordConnectGameApp(
                        activity = this,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    fun loadInterstitialAd() {
        try {
            InterstitialAd.load(
                this,
                "ca-app-pub-3940256099942544/1033173712", // Google AdMob Test Interstitial ID
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialAd = null
                    }
                }
            )
        } catch (e: Throwable) {
            interstitialAd = null
        }
    }

    fun loadRewardedAd() {
        try {
            RewardedAd.load(
                this,
                "ca-app-pub-3940256099942544/5224354917", // Google AdMob Test Rewarded ID
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedAd = null
                    }
                }
            )
        } catch (e: Throwable) {
            rewardedAd = null
        }
    }

    // Displays interstitial ad between levels with clean callback execution
    fun showInterstitial(onAdClosed: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd() // pre-load next
                    onAdClosed()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    loadInterstitialAd()
                    onAdClosed()
                }
            }
            ad.show(this)
        } else {
            loadInterstitialAd()
            onAdClosed() // move forward if ad isn't ready
        }
    }

    // Displays rewarded video ad to earn hints/coins
    fun showRewarded(onAwardReward: (Int) -> Unit, onAdClosed: () -> Unit = {}) {
        val ad = rewardedAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewardedAd() // pre-load next
                    onAdClosed()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedAd = null
                    loadRewardedAd()
                    onAdClosed()
                }
            }
            ad.show(this) { rewardItem ->
                onAwardReward(rewardItem.amount)
            }
        } else {
            loadRewardedAd()
            Toast.makeText(this, "Ad is still loading... Please try again!", Toast.LENGTH_SHORT).show()
        }
    }
}

// Data class for Player Rank Badges
data class PlayerRankBadge(
    val title: String,
    val textColor: Color,
    val borderColor: Color,
    val containerColor: Color,
    val rangeText: String,
    val description: String,
    val minLevel: Int,
    val maxLevel: Int,
    val nextThreshold: Int?,
    val nextRankName: String?
)

fun getPlayerRankBadge(levelNumber: Int): PlayerRankBadge {
    return when {
        levelNumber <= 20 -> PlayerRankBadge(
            title = "Amateur",
            textColor = Color(0xFF6D4C41),
            borderColor = Color(0xFFBCAAA4),
            containerColor = Color(0xFFEFEBE9),
            rangeText = "Lvl 1 - 20",
            description = "Beginning an epic lexical odyssey!",
            minLevel = 1,
            maxLevel = 20,
            nextThreshold = 21,
            nextRankName = "Beginner"
        )
        levelNumber <= 50 -> PlayerRankBadge(
            title = "Beginner",
            textColor = Color(0xFF2E7D32),
            borderColor = Color(0xFFA5D6A7),
            containerColor = Color(0xFFE8F5E9),
            rangeText = "Lvl 21 - 50",
            description = "Sowing seeds of vast vocabulary!",
            minLevel = 21,
            maxLevel = 50,
            nextThreshold = 51,
            nextRankName = "Intermediate"
        )
        levelNumber <= 200 -> PlayerRankBadge(
            title = "Intermediate",
            textColor = Color(0xFF1565C0),
            borderColor = Color(0xFF90CAF9),
            containerColor = Color(0xFFE3F2FD),
            rangeText = "Lvl 51 - 200",
            description = "Navigating compound and complex words!",
            minLevel = 51,
            maxLevel = 200,
            nextThreshold = 201,
            nextRankName = "Expert"
        )
        levelNumber <= 500 -> PlayerRankBadge(
            title = "Expert",
            textColor = Color(0xFFE65100),
            borderColor = Color(0xFFFFB74D),
            containerColor = Color(0xFFFFF3E0),
            rangeText = "Lvl 201 - 500",
            description = "A seasoned wizard of vocabulary!",
            minLevel = 201,
            maxLevel = 500,
            nextThreshold = 501,
            nextRankName = "Professional"
        )
        levelNumber <= 1000 -> PlayerRankBadge(
            title = "Professional",
            textColor = Color(0xFFAD1457),
            borderColor = Color(0xFFF48FB1),
            containerColor = Color(0xFFFCE4EC),
            rangeText = "Lvl 501 - 1000",
            description = "Mastering syntax and phonetic arts!",
            minLevel = 501,
            maxLevel = 1000,
            nextThreshold = 1001,
            nextRankName = "Master"
        )
        levelNumber <= 2000 -> PlayerRankBadge(
            title = "Master",
            textColor = Color(0xFF6A1B9A),
            borderColor = Color(0xFFCE93D8),
            containerColor = Color(0xFFF3E5F5),
            rangeText = "Lvl 1001 - 2000",
            description = "Letters shape under your magical touch!",
            minLevel = 1001,
            maxLevel = 2000,
            nextThreshold = 2001,
            nextRankName = "Grandmaster"
        )
        levelNumber <= 5000 -> PlayerRankBadge(
            title = "Grandmaster",
            textColor = Color(0xFFF9A825),
            borderColor = Color(0xFFFFF59D),
            containerColor = Color(0xFFFFFDE7),
            rangeText = "Lvl 2001 - 5000",
            description = "Eminent authority of spelling magic!",
            minLevel = 2001,
            maxLevel = 5000,
            nextThreshold = 5001,
            nextRankName = "Legendary Conjurer"
        )
        levelNumber <= 10000 -> PlayerRankBadge(
            title = "Legendary Conjurer",
            textColor = Color(0xFFC62828),
            borderColor = Color(0xFFEF9A9A),
            containerColor = Color(0xFFFFEBEE),
            rangeText = "Lvl 5001 - 10000",
            description = "Scribe of legends, weaving cosmic verbs!",
            minLevel = 5001,
            maxLevel = 10000,
            nextThreshold = 10001,
            nextRankName = "Spellweaver Eternal"
        )
        levelNumber <= 25000 -> PlayerRankBadge(
            title = "Spellweaver Eternal",
            textColor = Color(0xFF00838F),
            borderColor = Color(0xFF80DEEA),
            containerColor = Color(0xFFE0F7FA),
            rangeText = "Lvl 10001 - 25000",
            description = "Weaving letters into the fabric of eternity!",
            minLevel = 10001,
            maxLevel = 25000,
            nextThreshold = 25001,
            nextRankName = "Lexicon Overlord"
        )
        levelNumber <= 50000 -> PlayerRankBadge(
            title = "Lexicon Overlord",
            textColor = Color(0xFF283593),
            borderColor = Color(0xFF9FA8DA),
            containerColor = Color(0xFFE8EAF6),
            rangeText = "Lvl 25001 - 50000",
            description = "Complete rule over all semantic languages!",
            minLevel = 25001,
            maxLevel = 50000,
            nextThreshold = 50001,
            nextRankName = "Cosmic Word Deity"
        )
        else -> PlayerRankBadge(
            title = "Cosmic Word Deity",
            textColor = Color(0xFF37474F),
            borderColor = Color(0xFFB0BEC5),
            containerColor = Color(0xFFECEFF1),
            rangeText = "Lvl 50001+",
            description = "Omniscient creator of all known alphabets!",
            minLevel = 50001,
            maxLevel = 1000000,
            nextThreshold = null,
            nextRankName = null
        )
    }
}

// Confetti Particle System for Level Completed Celebration
class ConfettiParticle(
    var x: Float,
    var y: Float,
    val color: Color,
    val size: Float,
    var speedX: Float,
    var speedY: Float,
    var rotation: Float,
    val rotSpeed: Float,
    val isStar: Boolean = false
)

@Composable
fun ConfettiOverlay(visible: Boolean) {
    if (!visible) return
    
    val infiniteTransition = rememberInfiniteTransition(label = "Confetti")
    val frameTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "frameLoop"
    )
    
    var screenWidth by remember { mutableStateOf(0f) }
    var screenHeight by remember { mutableStateOf(0f) }
    
    // We spawn colorful particles
    val particles = remember { mutableStateListOf<ConfettiParticle>() }
    
    // Re-initialize particles when visible becomes true
    LaunchedEffect(visible) {
        if (visible) {
            particles.clear()
            val colors = listOf(
                Color(0xFFFFD54F), // Gold
                Color(0xFFFF7043), // Orange/Coral
                Color(0xFF81C784), // Emerald Mint
                Color(0xFF64B5F6), // Sky Cyan
                Color(0xFFBA68C8), // Violet
                Color(0xFFFF8A80)  // Rose
            )
            val startX = if (screenWidth > 0f) screenWidth / 2f else 500f
            val startY = if (screenHeight > 0f) screenHeight / 3f else 600f
            repeat(60) {
                particles.add(
                    ConfettiParticle(
                        x = startX,
                        y = startY,
                        color = colors.random(),
                        size = (12..28).random().toFloat(),
                        speedX = (-18..18).random().toFloat() * 1.5f,
                        speedY = (-28..8).random().toFloat() * 1.5f,
                        rotation = (0..360).random().toFloat(),
                        rotSpeed = (-10..10).random().toFloat() * 1.5f,
                        isStar = it % 2 == 0
                    )
                )
            }
        }
    }
    
    // Update particle physics simulation ticks
    val tick = remember(frameTime) { System.nanoTime() }
    LaunchedEffect(tick) {
        particles.forEach { p ->
            p.x += p.speedX
            p.y += p.speedY
            p.speedY += 0.9f // Gravity acceleration
            p.speedX *= 0.97f // Air friction slowing down
            p.rotation += p.rotSpeed
        }
    }
    
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { layoutCoordinates ->
                screenWidth = layoutCoordinates.size.width.toFloat()
                screenHeight = layoutCoordinates.size.height.toFloat()
            }
    ) {
        particles.forEach { p ->
            drawContext.canvas.save()
            drawContext.canvas.translate(p.x, p.y)
            drawContext.canvas.rotate(p.rotation)
            
            if (p.isStar) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, -p.size)
                    lineTo(p.size / 2, -p.size / 2)
                    lineTo(p.size, 0f)
                    lineTo(p.size / 2, p.size / 2)
                    lineTo(0f, p.size)
                    lineTo(-p.size / 2, p.size / 2)
                    lineTo(-p.size, 0f)
                    lineTo(-p.size / 2, -p.size / 2)
                    close()
                }
                drawPath(path, p.color)
            } else {
                drawRect(
                    color = p.color,
                    topLeft = Offset(-p.size / 2f, -p.size / 2f),
                    size = androidx.compose.ui.geometry.Size(p.size, p.size)
                )
            }
            
            drawContext.canvas.restore()
        }
    }
}

// Bottom Tab Navigation Items enum
enum class AppTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    GAME("Play", Icons.Default.PlayArrow),
    HOW_TO_PLAY("Rules", Icons.Default.Info),
    FREE_COINS("Free Coins", Icons.Default.Star),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WordConnectGameApp(
    activity: MainActivity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    // --- CUSTOM TOAST WITH ICON SYSTEM ---
    var customToastMessage by remember { mutableStateOf<String?>(null) }
    var activeToastJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val showToast = { msg: String ->
        customToastMessage = msg
        activeToastJob?.cancel()
        activeToastJob = coroutineScope.launch {
            delay(2800)
            if (customToastMessage == msg) {
                customToastMessage = null
            }
        }
    }
    
    // SharedPreferences for persistent game states
    val sharedPrefs = remember { context.getSharedPreferences("word_connect_prefs", Context.MODE_PRIVATE) }
    
    // --- MULTIPLAYER PROFILE STATE MANAGEMENT ---
    var activeProfileId by rememberSaveable { mutableStateOf(sharedPrefs.getString("active_profile_id", "default") ?: "default") }
    var profileIdsString by rememberSaveable { mutableStateOf(sharedPrefs.getString("profile_ids", "default") ?: "default") }
    
    // Migrate legacy non-profile data to default profile if legacy exists
    LaunchedEffect(Unit) {
        if (!sharedPrefs.contains("profile_default_migrated")) {
            val legacyLevel = sharedPrefs.getInt("current_level", -1)
            val legacyCoins = sharedPrefs.getInt("coins", -1)
            sharedPrefs.edit().apply {
                if (legacyLevel != -1) putInt("profile_default_current_level", legacyLevel)
                if (legacyCoins != -1) putInt("profile_default_coins", legacyCoins)
                for (i in 0 until 12) {
                    val solved = sharedPrefs.getString("solved_words_$i", null)
                    if (solved != null) {
                        putString("profile_default_solved_words_$i", solved)
                    }
                }
                putBoolean("profile_default_migrated", true)
                putString("profile_name_default", "Adventurer 1")
                apply()
            }
        }
    }
    
    var currentLevelIndex by rememberSaveable(activeProfileId) { 
        mutableStateOf(sharedPrefs.getInt("profile_${activeProfileId}_current_level", 0)) 
    }
    var coins by rememberSaveable(activeProfileId) { 
        mutableStateOf(sharedPrefs.getInt("profile_${activeProfileId}_coins", 200)) 
    }
    
    var lastRewardedTime by rememberSaveable(activeProfileId) { 
        mutableStateOf(sharedPrefs.getLong("profile_${activeProfileId}_last_rewarded_time", 0L)) 
    }
    var adsWatchedCount by rememberSaveable(activeProfileId) { 
        mutableStateOf(sharedPrefs.getInt("profile_${activeProfileId}_ads_watched_count", 0)) 
    }
    var adsWatchedDate by rememberSaveable(activeProfileId) { 
        mutableStateOf(sharedPrefs.getString("profile_${activeProfileId}_ads_watched_date", "") ?: "") 
    }
    
    val currentDateString = remember {
        try {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        } catch (e: Exception) {
            ""
        }
    }
    
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lastRewardedTime) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val maxDailyAds = 8
    val cooldownMillis = 60_000L // 60-second cooldown to protect AdMob account integrity
    val elapsed = currentTimeMillis - lastRewardedTime
    val remainingSeconds = (((cooldownMillis - elapsed) / 1000L).coerceAtLeast(0L)).toInt()
    val remainingDailyAds = (maxDailyAds - if (adsWatchedDate == currentDateString) adsWatchedCount else 0).coerceAtLeast(0)
    
    val onAdWatchedSuccessfully = { rewardAmt: Int ->
        val newCount = if (adsWatchedDate == currentDateString) adsWatchedCount + 1 else 1
        adsWatchedCount = newCount
        adsWatchedDate = currentDateString
        lastRewardedTime = System.currentTimeMillis()
        
        sharedPrefs.edit().apply {
            putLong("profile_${activeProfileId}_last_rewarded_time", lastRewardedTime)
            putInt("profile_${activeProfileId}_ads_watched_count", newCount)
            putString("profile_${activeProfileId}_ads_watched_date", currentDateString)
            apply()
        }
    }
    
    // Central single ToneGenerator resource to prevent native resource exhaustion crash
    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                toneGenerator?.release()
            } catch (e: Exception) {}
        }
    }

    // Dynamic settings
    var soundEffectsEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("sound_effects_enabled", true)) }
    var hapticFeedbackEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("haptic_feedback_enabled", true)) }

    val playSound = { isSuccess: Boolean ->
        if (soundEffectsEnabled && toneGenerator != null) {
            try {
                if (isSuccess) {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 250) // high pitch success beep
                } else {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 250) // short low buzz
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val playCelebrationSound = {
        if (soundEffectsEnabled && toneGenerator != null) {
            Thread {
                try {
                    // Beautiful major-chord fanfare arpeggio of tones!
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_1, 100)
                    Thread.sleep(110)
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_5, 100)
                    Thread.sleep(110)
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_9, 100)
                    Thread.sleep(110)
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_D, 450)
                } catch (e: Exception) {}
            }.start()
        }
    }

    val triggerHaptic = {
        if (hapticFeedbackEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Safety check for Level Bounds using a randomized dynamic level generator per-profile (infinite levels)
    val currentLevel = remember(currentLevelIndex, activeProfileId) {
        val puzzlePool = PuzzleData.puzzlePool
        val levelNames = PuzzleData.levelNames
        val combinedHash = (currentLevelIndex + activeProfileId.hashCode())
        val nonNegHash = if (combinedHash < 0) -combinedHash else combinedHash
        
        val chosenPuzzle = puzzlePool[nonNegHash % puzzlePool.size]
        val chosenName = "${levelNames[nonNegHash % levelNames.size]} ${currentLevelIndex + 1}"
        
        WordLevel(
            levelNumber = currentLevelIndex + 1,
            name = chosenName,
            letters = chosenPuzzle.first,
            targetWords = chosenPuzzle.second
        )
    }
    
    // Track solved words within the current level
    var solvedWordsString by rememberSaveable(currentLevelIndex, activeProfileId) {
        mutableStateOf(sharedPrefs.getString("profile_${activeProfileId}_solved_words_${currentLevelIndex}", "") ?: "")
    }
    
    val solvedWords = remember(solvedWordsString) {
        val list = mutableStateListOf<String>()
        if (solvedWordsString.isNotEmpty()) {
            list.addAll(solvedWordsString.split(";").filter { it.isNotEmpty() })
        }
        list
    }

    // Keep track of letter hints purchased
    // Maps each target word -> list of character indices that show up as Hints
    val revealedIndicesByWord = remember(currentLevelIndex, activeProfileId) {
        mutableStateMapOf<String, Set<Int>>()
    }

    // State overlays
    var showLevelComplete by rememberSaveable { mutableStateOf(false) }
    var showHowToPlay by rememberSaveable { mutableStateOf(false) }
    var showSettingsState by rememberSaveable { mutableStateOf(false) }
    
    // Custom introductory splash overlay timer
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(2000) // Show for 2 seconds
        showSplash = false
    }
    
    // Selected Tab
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.GAME) }
    
    // Shuffle level's letter positions on the interactive circle
    val lettersList = currentLevel.letters.map { it }
    var shuffledIndices by remember(currentLevelIndex, activeProfileId) {
        mutableStateOf(lettersList.indices.shuffled())
    }
    val shuffledLetters = remember(shuffledIndices, lettersList) {
        shuffledIndices.map { lettersList[it] }
    }

    // Current letters selected in active gesture
    val selectedIndices = remember { mutableStateListOf<Int>() }
    var currentFingerPosition by remember { mutableStateOf<Offset?>(null) }
    
    val currentSpelledWord = remember(shuffledLetters) {
        derivedStateOf {
            selectedIndices.map { shuffledLetters[it] }.joinToString("")
        }
    }.value

    // Theme color palette (Rich solid green forest-jungle premium gradient background)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F2620), // Deep Forest dark green
            Color(0xFF061411)  // Enchanted rich dark jungle black-green
        )
    )

    fun saveGameState(level: Int, currentCoins: Int, solved: List<String>) {
        sharedPrefs.edit().apply {
            putInt("profile_${activeProfileId}_current_level", level)
            putInt("profile_${activeProfileId}_coins", currentCoins)
            putString("profile_${activeProfileId}_solved_words_${level}", solved.joinToString(";"))
            apply()
        }
        currentLevelIndex = level
        coins = currentCoins
    }

    val completeWordCheck = { formedWord: String ->
        if (formedWord.isNotEmpty()) {
            if (currentLevel.targetWords.contains(formedWord)) {
                if (!solvedWords.contains(formedWord)) {
                    solvedWords.add(formedWord)
                    solvedWordsString = solvedWords.joinToString(";")
                    saveGameState(currentLevelIndex, coins, solvedWords)
                    
                    // Simple toast or sound confirmation
                    showToast("✨ Correct! $formedWord!")
                    playSound(true)
                    triggerHaptic()
                    
                    // Award bonus coins per word found
                    coins += 10
                    saveGameState(currentLevelIndex, coins, solvedWords)
                    
                    // Level completion trigger
                    if (solvedWords.size == currentLevel.targetWords.size) {
                        showLevelComplete = true
                        coins += 50 // Level clear bonus
                        saveGameState(currentLevelIndex, coins, solvedWords)
                        
                        // Celebrating Sound
                        playCelebrationSound()
                    }
                } else {
                    showToast("Already solved: $formedWord")
                    playSound(false)
                }
            } else {
                showToast("Not in list: $formedWord")
                playSound(false)
            }
        }
    }

    // Multiplayer configuration states
    var showAddProfileDialog by rememberSaveable { mutableStateOf(false) }
    var newProfileNameInput by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Dark green/jungled forest background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        )

        // Custom Confetti Particle overlay celebrating level clearance!
        ConfettiOverlay(visible = showLevelComplete)

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // --- TOP FIXED ADMOB BANNER AREA ---
                var bannerAdViewRef by remember { mutableStateOf<AdView?>(null) }
                DisposableEffect(Unit) {
                    onDispose {
                        bannerAdViewRef?.destroy()
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0D1714))
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .statusBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admob_banner"),
                        factory = { ctx ->
                            AdView(ctx).apply {
                                setAdSize(AdSize.BANNER)
                                adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test Banner ID
                                loadAd(AdRequest.Builder().build())
                                bannerAdViewRef = this
                            }
                        }
                    )
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF13221C).copy(alpha = 0.96f),
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    AppTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) Color(0xFFFFD54F) else Color(0xFF88A096),
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFFFFD54F) else Color(0xFF88A096)
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF13221C),
                                selectedTextColor = Color(0xFFFFD54F),
                                indicatorColor = Color(0xFFFFB300),
                                unselectedIconColor = Color(0xFF88A096),
                                unselectedTextColor = Color(0xFF88A096)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    AppTab.GAME -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
            
            // --- TOP STATUS BAR AREA ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active profile select button (Directs to settings tab to swap profiles)
                Row(
                    modifier = Modifier
                        .background(Color(0xFFFFFAEB).copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                        .border(1.2.dp, Color(0xFFFFB300), RoundedCornerShape(12.dp))
                        .clickable { selectedTab = AppTab.SETTINGS }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("👤", style = MaterialTheme.typography.bodyMedium)
                    val currentPName = sharedPrefs.getString("profile_name_$activeProfileId", if (activeProfileId == "default") "Adventurer 1" else "Player") ?: "Player"
                    Text(
                        text = currentPName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFD84315)
                        )
                    )
                }

                // Level badge container
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LEVEL ${currentLevel.levelNumber}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD54F),
                            letterSpacing = 1.5.sp
                        )
                    )
                    
                    val currentStageLevel = currentLevelIndex + 1
                    val rankBadge = getPlayerRankBadge(currentStageLevel)
                    
                    Row(
                        modifier = Modifier
                            .padding(vertical = 3.dp)
                            .background(rankBadge.containerColor, RoundedCornerShape(10.dp))
                            .border(1.dp, rankBadge.borderColor, RoundedCornerShape(10.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✦",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = rankBadge.textColor,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(end = 3.dp)
                        )
                        Text(
                            text = rankBadge.title.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = rankBadge.textColor,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }

                    Text(
                        text = currentLevel.name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE0F2F1)
                        )
                    )
                }

                // Balance display pill (Directs to free coins tab)
                Row(
                    modifier = Modifier
                        .background(Color(0xFFFFFDE7), RoundedCornerShape(20.dp))
                        .border(1.5.dp, Color(0xFFFFB300), RoundedCornerShape(20.dp))
                        .clickable { selectedTab = AppTab.FREE_COINS }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🪙 $coins",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                    )
                }
            }

            // Word Grid Gameboard area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            2.dp,
                            Color(0xFF8D6E63).copy(alpha = 0.65f), // Warm cozy wood frame
                            RoundedCornerShape(24.dp)
                        )
                ) {
                    // White washed wood pattern background with fluffy fiber overlay
                    Image(
                        painter = painterResource(id = R.drawable.img_white_wood_wool_pattern_1779904562234),
                        contentDescription = "White Wool Wooden Pattern Background",
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "FIND ALL WORDS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF5D4037), // Neat brown header
                                letterSpacing = 1.5.sp
                            )
                        )

                        // Target grid displaying word spaces
                        val maxWordLength = currentLevel.targetWords.maxOfOrNull { it.length } ?: 4
                        val displayInGrid = currentLevel.targetWords.size > 3 && maxWordLength < 5
                        val targetWordsChunks = if (displayInGrid) {
                            currentLevel.targetWords.chunked(2)
                        } else {
                            currentLevel.targetWords.chunked(1)
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(if (displayInGrid) 6.dp else 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                        ) {
                            targetWordsChunks.forEach { chunk ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    chunk.forEach { word ->
                                        val isSolved = solvedWords.contains(word)
                                        val hints = revealedIndicesByWord[word] ?: emptySet()

                                         val cellWidth = when {
                                             word.length >= 8 -> 22.dp
                                             word.length >= 7 -> 25.dp
                                             word.length >= 6 -> 28.dp
                                             else -> if (displayInGrid) 25.dp else 30.dp
                                         }
                                         val cellHeight = when {
                                             word.length >= 8 -> 26.dp
                                             word.length >= 7 -> 28.dp
                                             word.length >= 6 -> 32.dp
                                             else -> if (displayInGrid) 28.dp else 34.dp
                                         }
                                         val fontSize = when {
                                             word.length >= 8 -> 12.sp
                                             word.length >= 7 -> 14.sp
                                             word.length >= 6 -> 15.sp
                                             else -> 16.sp
                                         }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            modifier = Modifier.padding(vertical = 1.dp)
                                        ) {
                                            word.forEachIndexed { charIndex, char ->
                                                val showChar = isSolved || hints.contains(charIndex)
                                                val cellBg = when {
                                                    isSolved -> Color(0xFFC8E6C9)       // Solved: Fresh light mint-green
                                                    hints.contains(charIndex) -> Color(0xFFFFE082) // Hint: Warm shiny sun-yellow
                                                    else -> Color(0xFFF0EBE1)            // Unsolved: Clean obvious soft sand-cream
                                                }
                                                val cellBorderColor = when {
                                                    isSolved -> Color(0xFF4CAF50)
                                                    hints.contains(charIndex) -> Color(0xFFFFB300)
                                                    else -> Color(0xFFC7BCAE)
                                                }
                                                val cellTextColor = when {
                                                    isSolved -> Color(0xFF1B5E20)
                                                    hints.contains(charIndex) -> Color(0xFFE65100)
                                                    else -> Color(0xFF5D4037)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = cellWidth, height = cellHeight)
                                                        .background(cellBg, RoundedCornerShape(6.dp))
                                                        .border(1.2.dp, cellBorderColor, RoundedCornerShape(6.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = if (showChar) char.toString() else "",
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = fontSize,
                                                            color = cellTextColor
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Word preview text bubble (displays currently spelled word in real-time swipe dragging)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (currentSpelledWord.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFFFF3E0), RoundedCornerShape(24.dp))
                            .border(2.dp, Color(0xFFFF7043), RoundedCornerShape(24.dp))
                            .padding(vertical = 8.dp, horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentSpelledWord,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFD84315),
                                letterSpacing = 4.sp
                            )
                        )
                    }
                } else {
                    Text(
                        text = "Trace letters in order below!",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFFFF3E0),
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- BOTTOM SWIPE CONTROLLER & WHEEL UTILITIES ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button left
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    FloatingActionButton(
                        onClick = {
                            shuffledIndices = shuffledIndices.shuffled()
                            triggerHaptic()
                            showToast("Letters shuffled!")
                        },
                        containerColor = Color(0xFFFFF8F1),
                        contentColor = Color(0xFFD84315),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(56.dp)
                            .border(1.5.dp, Color(0xFFD84315), CircleShape)
                            .testTag("btn_shuffle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Shuffle",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Shuffle",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFF3E0)
                        )
                    )
                }

                // Interactive Letter Pad Canvas Wheel (Gorgeous Parchment Adventurer Dial with Responsive Sizing)
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .widthIn(max = 242.dp)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .border(4.dp, Color(0xFF8D6E63), CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_word_wheel_bg_1779854305416),
                        contentDescription = "Wheel Background Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    val density = LocalDensity.current
                    val widthPx = with(density) { maxWidth.toPx() }
                    val heightPx = with(density) { maxHeight.toPx() }
                    val center = Offset(widthPx / 2f, heightPx / 2f)
                    val circleRadius = widthPx * 0.35f
                    val touchThreshold = widthPx * 0.14f
                    val letterSize = with(density) { (maxWidth * 0.20f).coerceIn(36.dp, 48.dp) }
                    val halfSizePx = with(density) { (letterSize / 2).toPx() }

                    // Calculate static coordinate targets for letters
                    val positions = remember(shuffledLetters, widthPx) {
                        shuffledLetters.indices.map { i ->
                            val angle = (360f / shuffledLetters.size * i - 90f) * (Math.PI / 180f)
                            Offset(
                                x = center.x + circleRadius * cos(angle).toFloat(),
                                y = center.y + circleRadius * sin(angle).toFloat()
                            )
                        }
                    }

                    // Handles actual swipe connection gesture mapping
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(shuffledLetters, positions) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        selectedIndices.clear()
                                        currentFingerPosition = offset
                                        positions.forEachIndexed { i, pos ->
                                            val dx = offset.x - pos.x
                                            val dy = offset.y - pos.y
                                            if (sqrt(dx * dx + dy * dy) <= touchThreshold) {
                                                selectedIndices.add(i)
                                                triggerHaptic()
                                            }
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val nextPos = change.position
                                        currentFingerPosition = nextPos

                                        positions.forEachIndexed { i, pos ->
                                            val dx = nextPos.x - pos.x
                                            val dy = nextPos.y - pos.y
                                            if (sqrt(dx * dx + dy * dy) <= touchThreshold) {
                                                if (!selectedIndices.contains(i)) {
                                                    selectedIndices.add(i)
                                                    triggerHaptic()
                                                } else if (selectedIndices.size >= 2 && selectedIndices[selectedIndices.size - 2] == i) {
                                                    // Allow going back to undo connection
                                                    selectedIndices.removeAt(selectedIndices.size - 1)
                                                    triggerHaptic()
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        // CRITICAL: Compute final spelled word dynamically to avoid stale state trap!
                                        val finalWord = selectedIndices.map { shuffledLetters[it] }.joinToString("")
                                        completeWordCheck(finalWord)
                                        selectedIndices.clear()
                                        currentFingerPosition = null
                                    },
                                    onDragCancel = {
                                        selectedIndices.clear()
                                        currentFingerPosition = null
                                    }
                                )
                            }
                    ) {
                        // Drawing path lines
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Line connects between already selected letters
                            for (j in 0 until selectedIndices.size - 1) {
                                val p1 = positions[selectedIndices[j]]
                                val p2 = positions[selectedIndices[j + 1]]
                                drawLine(
                                    color = Color(0xFFFF7043), // Vivid adventurous coral
                                    start = p1,
                                    end = p2,
                                    strokeWidth = 14f,
                                    cap = StrokeCap.Round
                                )
                            }

                            // Dynamic live line following user moving finger
                            if (selectedIndices.isNotEmpty() && currentFingerPosition != null) {
                                val lastPos = positions[selectedIndices.last()]
                                val finger = currentFingerPosition!!
                                drawLine(
                                    color = Color(0xFFFF7043).copy(alpha = 0.6f),
                                    start = lastPos,
                                    end = finger,
                                    strokeWidth = 10f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }

                        // Drawing actual Letters around pad surface
                        shuffledLetters.forEachIndexed { i: Int, letter: Char ->
                            val pos = positions[i]
                            val isSelected = selectedIndices.contains(i)
                            val offsetDp = IntOffset(
                                x = (pos.x - halfSizePx).toInt(),
                                y = (pos.y - halfSizePx).toInt()
                            )

                            Box(
                                modifier = Modifier
                                    .absoluteOffset { offsetDp }
                                    .size(letterSize)
                                    .background(
                                        if (isSelected) Color(0xFFFF7043) else Color(0xFFFFF8F1),
                                        CircleShape
                                    )
                                    .border(
                                        width = 3.dp,
                                        color = if (isSelected) Color(0xFFE65100) else Color(0xFF8D6E63).copy(alpha = 0.85f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter.toString(),
                                    style = if (letterSize < 42.dp) {
                                        MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color(0xFF3E2723)
                                        )
                                    } else {
                                        MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color(0xFF3E2723)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Hint Button right (deducts 500 coins or offers rewarded video if out of coins)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    FloatingActionButton(
                        onClick = {
                            val activeWords = currentLevel.targetWords
                            val uncompleted = activeWords.filter { !solvedWords.contains(it) }
                            
                            if (uncompleted.isNotEmpty()) {
                                if (coins >= 500) {
                                    // Gather all unfilled letters
                                    val candidates = mutableListOf<Triple<String, Int, Char>>()
                                    uncompleted.forEach { targetWord ->
                                        val revealed = revealedIndicesByWord[targetWord] ?: emptySet()
                                        targetWord.forEachIndexed { cIdx, char ->
                                            if (!revealed.contains(cIdx)) {
                                                candidates.add(Triple(targetWord, cIdx, char))
                                            }
                                        }
                                    }

                                    if (candidates.isNotEmpty()) {
                                        val luckyPick = candidates.random()
                                        val targetWord = luckyPick.first
                                        val charIndex = luckyPick.second

                                        coins -= 500
                                        val currentSet = revealedIndicesByWord[targetWord] ?: emptySet()
                                        revealedIndicesByWord[targetWord] = currentSet + charIndex
                                        
                                        saveGameState(currentLevelIndex, coins, solvedWords)
                                        triggerHaptic()
                                        showToast("Hints purchased! 🎁 Letter revealed!")
                                    }
                                } else {
                                    // Let user know they can only earn coins under the Free Coins tab
                                    showToast("Not enough coins! Buy hints for 500 coins, or get more under 'Free Coins'!")
                                }
                            }
                        },
                        containerColor = Color(0xFFFFFDE7),
                        contentColor = Color(0xFFE65100),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(56.dp)
                            .border(1.5.dp, Color(0xFFFFB300), CircleShape)
                            .testTag("btn_hint")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Buy Hint",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Hint",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFF3E0)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fallback Next Level check to guarantee player progress NEVER gets stuck if dialog dismissed
            if (solvedWords.size == currentLevel.targetWords.size) {
                Button(
                    onClick = {
                        val nextLevelAction = {
                            showLevelComplete = false
                            currentLevelIndex = currentLevelIndex + 1
                            solvedWordsString = ""
                            revealedIndicesByWord.clear()
                            saveGameState(currentLevelIndex, coins, emptyList())
                        }
                        val completedLevelNum = currentLevelIndex + 1
                        if (completedLevelNum % 3 == 0) {
                            activity.showInterstitial { nextLevelAction() }
                        } else {
                            nextLevelAction()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(bottom = 12.dp)
                ) {
                    Text("PROCEED TO NEXT LEVEL ➔", color = Color.White, fontWeight = FontWeight.Black)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    AppTab.HOW_TO_PLAY -> {
        // Rules explanation tab screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "HOW TO PLAY",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD54F)
                )
            )
            
            val instructions = listOf(
                "1. Connecting Letters" to "Drag your finger across circular letter pads to connect them, tracing out valid english words corresponding to the level targets.",
                "2. Level Completed Trigger" to "Forming all valid words in a level successfully triggers a clear bonus. Leaving the app won't trap you since level progression state remains securely stored.",
                "3. Charging Hints" to "Purchasing a random character hint in any uncompleted target word costs exactly 500 coins.",
                "4. Recharging Coin Supply" to "Got zero coins left? Navigate to the 'Free Coins' tab and watch highly rewarding video ads anytime for +500 free coins instantly!"
            )
            
            instructions.forEach { (title, content) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9).copy(alpha = 0.9f)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF8D6E63).copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5D4037)
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF5D4037))
                        )
                    }
                }
            }
        }
    }

    AppTab.FREE_COINS -> {
        // Rewarded coins claiming terminal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.95f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9).copy(alpha = 0.92f)),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFB300))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "🪙 COIN RECHARGER",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE65100)
                        )
                    )
                    
                    Text(
                        text = "Your Current Balance:",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF5D4037))
                    )
                    
                    Text(
                        text = "🪙 $coins Coins",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF7043)
                        )
                    )
                    
                    Divider(color = Color(0xFFE8E5DF), thickness = 1.dp)
                    
                    Text(
                        text = "Watch a fast promotional video clip to instantly grant 500 Gold coins to your treasury!",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF5D4037)),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    val isDailyLimitReached = remainingDailyAds <= 0
                    val isCooldownActive = remainingSeconds > 0

                    Text(
                        text = "Daily Limit: $remainingDailyAds of $maxDailyAds videos remaining today",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isDailyLimitReached) Color.Red else Color(0xFF7D5742),
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Button(
                        onClick = {
                            if (isCooldownActive) {
                                showToast("⏳ Cooldown active. Please wait ${remainingSeconds}s.")
                            } else if (isDailyLimitReached) {
                                showToast("🚨 Daily limit reached. Try again tomorrow!")
                            } else {
                                activity.showRewarded(
                                    onAwardReward = { amt ->
                                        coins += 500
                                        onAdWatchedSuccessfully(amt)
                                        saveGameState(currentLevelIndex, coins, solvedWords)
                                        showToast("🎁 Congratulations! +500 Coins added to your journey!")
                                    }
                                )
                            }
                        },
                        enabled = !isCooldownActive && !isDailyLimitReached,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCooldownActive || isDailyLimitReached) Color.Gray else Color(0xFFFF9800)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("btn_get_free_coins"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isCooldownActive) Icons.Default.Refresh else Icons.Default.PlayArrow,
                                contentDescription = "Watch Icon",
                                tint = Color.White
                            )
                            Text(
                                text = when {
                                    isDailyLimitReached -> "DAILY LIMIT REACHED"
                                    isCooldownActive -> "NEXT VIDEO IN ${remainingSeconds}S"
                                    else -> "WATCH VIDEO (+500 COINS)"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                        }
                    }
                }
            }
        }
    }

    AppTab.SETTINGS -> {
        // Advanced dynamic settings & multiplayer session managers
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SETTINGS & SESSIONS",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD54F)
                )
            )

            // Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9).copy(alpha = 0.9f)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF8D6E63).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("GAME CONFIGURATION", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, color = Color(0xFF8D6E63)))
                    
                    // sound effects switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sound Effects", color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
                        Switch(
                            checked = soundEffectsEnabled,
                            onCheckedChange = { nextVal ->
                                soundEffectsEnabled = nextVal
                                sharedPrefs.edit().putBoolean("sound_effects_enabled", nextVal).apply()
                            }
                        )
                    }

                    // haptics switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Haptic Feedback", color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
                        Switch(
                            checked = hapticFeedbackEnabled,
                            onCheckedChange = { nextVal ->
                                hapticFeedbackEnabled = nextVal
                                sharedPrefs.edit().putBoolean("haptic_feedback_enabled", nextVal).apply()
                            }
                        )
                    }
                }
            }

            // Multiplayer Profile Manager Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9).copy(alpha = 0.9f)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF8D6E63).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("DEVICE SESSION SESSIONS (MULTIPLAYER)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, color = Color(0xFF8D6E63)))
                    
                    Text(
                        text = "Allow other players on this device to record separate progress, levels and balance stash in isolation!",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF5D4037))
                    )

                    val profileIds = profileIdsString.split(";").filter { it.isNotEmpty() }
                    profileIds.forEach { id ->
                        val isActive = id == activeProfileId
                        val pName = sharedPrefs.getString("profile_name_$id", if (id == "default") "Adventurer 1" else "Player") ?: "Player"
                        val pLvl = sharedPrefs.getInt("profile_${id}_current_level", 0) + 1
                        val pCoins = sharedPrefs.getInt("profile_${id}_coins", 200)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isActive) Color(0xFFE8F5E9) else Color(0xFFF9F5EF),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isActive) Color(0xFF4CAF50) else Color(0xFFE0D8D0),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    sharedPrefs
                                        .edit()
                                        .putString("active_profile_id", id)
                                        .apply()
                                    activeProfileId = id
                                    showToast("Switched profile session to: $pName")
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = pName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) Color(0xFF2E7D32) else Color(0xFF5D4037)
                                    )
                                )
                                Text(
                                    text = "Lvl: $pLvl • Coins: 🪙 $pCoins",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF795548))
                                )
                            }
                            if (isActive) {
                                Text("✅ ACTIVE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = Color(0xFF2E7D32)))
                            } else {
                                Text("SWITCH", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF8D6E63)))
                            }
                        }
                    }

                    Button(
                        onClick = {
                            newProfileNameInput = ""
                            showAddProfileDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF836253)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ CREATE NEW PLAYER PROFILE", fontWeight = FontWeight.Black)
                    }
                }
            }

            // Rest Game Progress Button
            Button(
                onClick = {
                    val id = activeProfileId
                    sharedPrefs.edit().apply {
                        putInt("profile_${id}_current_level", 0)
                        putInt("profile_${id}_coins", 200)
                        // Clear cached randomized level mappings so they are re-randomized on reset!
                        for (i in 0..100) {
                            remove("profile_${id}_lvl_${i}_letters")
                            remove("profile_${id}_lvl_${i}_targets")
                            remove("profile_${id}_lvl_${i}_name")
                            remove("profile_${id}_solved_words_${i}")
                        }
                        apply()
                    }
                    currentLevelIndex = 0
                    coins = 200
                    solvedWordsString = ""
                    revealedIndicesByWord.clear()
                    showToast("Current profile progress reset!")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_settings_reset")
            ) {
                Text("RESET CURRENT PROFILE PROGRESS", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
        }

// --- SUB DIALOG: CREATE NEW PROFILE ---
if (showAddProfileDialog) {
    AlertDialog(
        onDismissRequest = { showAddProfileDialog = false },
        title = { Text("Create New Profile", fontWeight = FontWeight.Black, color = Color(0xFF5D4037)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter name for a blank isolated gameplay session:")
                OutlinedTextField(
                    value = newProfileNameInput,
                    onValueChange = { newProfileNameInput = it },
                    placeholder = { Text("Adventurer Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_profile_name")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rawName = newProfileNameInput.trim()
                    if (rawName.isNotEmpty()) {
                        val nextProfileUniqueId = "profile_" + System.currentTimeMillis()
                        val newIdsString = profileIdsString + ";" + nextProfileUniqueId
                        
                        sharedPrefs.edit().apply {
                            putString("profile_ids", newIdsString)
                            putString("profile_name_$nextProfileUniqueId", rawName)
                            putInt("profile_${nextProfileUniqueId}_current_level", 0)
                            putInt("profile_${nextProfileUniqueId}_coins", 500) // Welcoming free coins!
                            putString("active_profile_id", nextProfileUniqueId)
                            apply()
                        }
                        
                        profileIdsString = newIdsString
                        activeProfileId = nextProfileUniqueId
                        showAddProfileDialog = false
                        showToast("Welcome agent $rawName!")
                    } else {
                        showToast("Name cannot be empty!")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Create & Switch", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { showAddProfileDialog = false }) {
                Text("Cancel", color = Color(0xFF8D6E63))
            }
        }
    )
}

        // --- LEVEL COMPLETE CELEBRATION OVERLAY DIALOG ---
        if (showLevelComplete) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {
                    Button(
                        onClick = {
                            val nextLevelAction = {
                                showLevelComplete = false
                                currentLevelIndex = currentLevelIndex + 1
                                solvedWordsString = ""
                                revealedIndicesByWord.clear()
                                saveGameState(currentLevelIndex, coins, emptyList())
                            }

                            val completedLevel = currentLevelIndex + 1
                            if (completedLevel % 3 == 0) {
                                // Show interstitial transition ad on every 3 level intervals (e.g. Lvl 3, 6, 9...)
                                activity.showInterstitial {
                                    nextLevelAction()
                                }
                            } else {
                                // Transition directly without showing an interstitial ad
                                nextLevelAction()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), // Obvious bright friendly green
                        modifier = Modifier.testTag("btn_complete_dialog_next")
                    ) {
                        Text("Next Level ➔", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Victory Medal",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "LEVEL COMPLETE!",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF2E7D32)
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Amazing vocabulary skill!",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF5D4037)),
                            textAlign = TextAlign.Center
                        )
                        
                        // Beautiful Rank Badge progress card
                        val levelNum = currentLevelIndex + 1
                        val rankBadge = getPlayerRankBadge(levelNum)
                        val rangeLen = (rankBadge.maxLevel - rankBadge.minLevel + 1).coerceAtLeast(1)
                        val currentProg = (levelNum - rankBadge.minLevel + 1).coerceAtLeast(1)
                        val progPercent = (currentProg.toFloat() / rangeLen).coerceIn(0f, 1f)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = rankBadge.containerColor),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, rankBadge.borderColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "RANK STATUS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = rankBadge.textColor.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "✦",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = rankBadge.textColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = rankBadge.title.uppercase(),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = rankBadge.textColor,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = rankBadge.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = rankBadge.textColor.copy(alpha = 0.85f),
                                        fontStyle = FontStyle.Italic
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Progress bar to visualize progress
                                LinearProgressIndicator(
                                    progress = { progPercent },
                                    modifier = Modifier
                                        .fillMaxWidth(0.95f)
                                        .height(8.dp),
                                    color = rankBadge.textColor,
                                    trackColor = Color.White.copy(alpha = 0.6f),
                                    strokeCap = StrokeCap.Round
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val levelsLeftText = if (rankBadge.nextThreshold != null) {
                                    val levelsLeft = rankBadge.nextThreshold - levelNum
                                    "$levelsLeft levels to next rank (${rankBadge.nextRankName})"
                                } else {
                                    "Highest Rank Achieved!"
                                }
                                
                                Text(
                                    text = levelsLeftText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = rankBadge.textColor.copy(alpha = 0.9f)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .background(Color(0xFFFFF9C4), RoundedCornerShape(12.dp)) // Bright obvious light pill
                                .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "🪙 Level Clear Bonus: +50 Coins Added",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color(0xFFFFFDF9) // Pristine light elegant dialog canvas
            )
        }

        // --- SETTINGS OVERLAY ---
        if (showSettingsState) {
            AlertDialog(
                onDismissRequest = { showSettingsState = false },
                confirmButton = {
                    Button(
                        onClick = { showSettingsState = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            // Reset SharedPreferences progress
                            sharedPrefs.edit().clear().apply()
                            currentLevelIndex = 0
                            coins = 200
                            solvedWordsString = ""
                            revealedIndicesByWord.clear()
                            soundEffectsEnabled = true
                            hapticFeedbackEnabled = true
                            showSettingsState = false
                            showToast("Game reset completed!")
                        },
                        modifier = Modifier.testTag("btn_settings_reset")
                    ) {
                        Text("Reset All Progress", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                    }
                },
                title = {
                    Text("Settings", fontWeight = FontWeight.Black, color = Color(0xFF5D4037))
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Sound switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val nextVal = !soundEffectsEnabled
                                    soundEffectsEnabled = nextVal
                                    sharedPrefs.edit().putBoolean("sound_effects_enabled", nextVal).apply()
                                }
                        ) {
                            Text("Sound Effects", color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
                            Switch(
                                checked = soundEffectsEnabled,
                                onCheckedChange = { nextVal ->
                                    soundEffectsEnabled = nextVal
                                    sharedPrefs.edit().putBoolean("sound_effects_enabled", nextVal).apply()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF4CAF50),
                                    uncheckedThumbColor = Color(0xFFBCAAA4),
                                    uncheckedTrackColor = Color(0xFFECEFF1)
                                )
                            )
                        }

                        // Haptics switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val nextVal = !hapticFeedbackEnabled
                                    hapticFeedbackEnabled = nextVal
                                    sharedPrefs.edit().putBoolean("haptic_feedback_enabled", nextVal).apply()
                                }
                        ) {
                            Text("Haptic Feedback", color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
                            Switch(
                                checked = hapticFeedbackEnabled,
                                onCheckedChange = { nextVal ->
                                    hapticFeedbackEnabled = nextVal
                                    sharedPrefs.edit().putBoolean("haptic_feedback_enabled", nextVal).apply()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF4CAF50),
                                    uncheckedThumbColor = Color(0xFFBCAAA4),
                                    uncheckedTrackColor = Color(0xFFECEFF1)
                                )
                            )
                        }

                        Divider(color = Color(0xFFE8E5DF), thickness = 1.dp)

                        val settingsLevelNumber = currentLevelIndex + 1
                        val settingsBadge = getPlayerRankBadge(settingsLevelNumber)
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Current Rank:", color = Color(0xFF795548), fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier
                                    .background(settingsBadge.containerColor, RoundedCornerShape(8.dp))
                                    .border(1.dp, settingsBadge.borderColor, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = settingsBadge.title.uppercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = settingsBadge.textColor,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }

                        Text("Playable Levels: Unlimited randomized levels", color = Color(0xFF795548), fontWeight = FontWeight.Medium)
                        Text("Current High Score: Level $settingsLevelNumber", color = Color(0xFF795548), fontWeight = FontWeight.Medium)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color(0xFFFFFDF9)
            )
        }

        // --- HOW TO PLAY INFO SCREEN ---
        if (showHowToPlay) {
            AlertDialog(
                onDismissRequest = { showHowToPlay = false },
                confirmButton = {
                    Button(
                        onClick = { showHowToPlay = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Let's Play!", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                title = {
                    Text("How to Play", fontWeight = FontWeight.Black, color = Color(0xFFD84315))
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("1. Drag your finger across the circular letters to connect them and form words.", color = Color(0xFF5D4037), fontWeight = FontWeight.Medium)
                        Text("2. Release your finger to submit the spelling.", color = Color(0xFF5D4037), fontWeight = FontWeight.Medium)
                        Text("3. Correct guesses will reveal letters on the board.", color = Color(0xFF5D4037), fontWeight = FontWeight.Medium)
                        Text("4. Need help? Tap the ⭐ Star to buy a hint for 500 coins!", color = Color(0xFF5D4037), fontWeight = FontWeight.Medium)
                        Text("5. Short on coins? Watch rewarded videos anytime under the 'Free Coins' tab to get +500 free coins!", color = Color(0xFF5D4037), fontWeight = FontWeight.Medium)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color(0xFFFFFDF9)
            )
        }
        // --- CUSTOM ANIMATED TOAST OVERLAY WITH GAME ICON ---
        AnimatedVisibility(
            visible = customToastMessage != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp) // Below the top-bar banner ad!
                .padding(horizontal = 24.dp)
                .zIndex(9999f)
        ) {
            customToastMessage?.let { msg ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF132F2A), // Forest/Aesthetic dark theme card
                        contentColor = Color(0xFFFFF8F1)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFB300)), // Classic sparkling golden border
                    modifier = Modifier.widthIn(max = 350.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.wordcon),
                            contentDescription = "Game Icon Toast Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFF8F1)
                            )
                        )
                    }
                }
            }
        }

        // --- CUSTOM INTRO SPLASH SCREEN OVERLAY ---
        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(durationMillis = 800)),
            modifier = Modifier.zIndex(100000f) // Keep on topmost layer
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F2620), // Jungle Dark Green
                                Color(0xFF061411)  // Rich Deep Emerald Black
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Elevated rounded frame for our core logo
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFB300)),
                        modifier = Modifier
                            .size(200.dp)
                            .padding(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.wordcon),
                            contentDescription = "Offline Brain Game Splash Screen Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "BRAIN MASTER",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFB300),
                            letterSpacing = 5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "OFFLINE BRAIN GAME",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC8E6C9),
                            letterSpacing = 2.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    CircularProgressIndicator(
                        color = Color(0xFFFFB300),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}
}
