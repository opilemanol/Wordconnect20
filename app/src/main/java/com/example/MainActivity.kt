package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.os.Bundle
import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.BorderStroke
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
        
        // Safely initialize AdMob Mobile Ads SDK on the Main thread with exception handling
        try {
            MobileAds.initialize(this) {
                try {
                    loadInterstitialAd()
                    loadRewardedAd()
                } catch (e: Throwable) {
                    // Ignore ads load exception
                }
            }
        } catch (e: Throwable) {
            // Ignore any Play Services or other runtime exceptions to guarantee smooth offline play
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
        try {
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
        } catch (e: Throwable) {
            onAdClosed()
        }
    }

    // Displays rewarded video ad to earn hints/coins
    fun showRewarded(onAwardReward: (Int) -> Unit, onAdClosed: () -> Unit = {}) {
        try {
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
                    try {
                        onAwardReward(rewardItem.amount)
                    } catch (e: Throwable) {}
                }
            } else {
                loadRewardedAd()
                try {
                    Toast.makeText(this, "Ad is still loading... Please try again!", Toast.LENGTH_SHORT).show()
                } catch (e: Throwable) {}
                onAdClosed()
            }
        } catch (e: Throwable) {
            onAdClosed()
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
enum class AppTab(val title: String) {
    GAME("Play"),
    HOW_TO_PLAY("Rules"),
    FREE_COINS("Free Coins"),
    SETTINGS("Settings");

    val icon: androidx.compose.ui.graphics.vector.ImageVector
        get() = when (this) {
            GAME -> Icons.Default.PlayArrow
            HOW_TO_PLAY -> Icons.Default.Info
            FREE_COINS -> Icons.Default.Star
            SETTINGS -> Icons.Default.Settings
        }
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
    
    // --- DAILY STREAK STATE & LOGIC ---
    var streakCount by rememberSaveable(activeProfileId) { 
        mutableStateOf(sharedPrefs.getInt("profile_${activeProfileId}_streak_count", 1)) 
    }
    var lastStreakDate by rememberSaveable(activeProfileId) { 
        mutableStateOf(sharedPrefs.getString("profile_${activeProfileId}_last_streak_date", "") ?: "") 
    }
    var dailyClaimedDate by rememberSaveable(activeProfileId) { 
        mutableStateOf(sharedPrefs.getString("profile_${activeProfileId}_daily_claimed_date", "") ?: "") 
    }
    var showDailyStreakDialog by rememberSaveable { mutableStateOf(false) }

    val currentDateString = remember {
        try {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        } catch (e: Exception) {
            ""
        }
    }

    val yesterdayDateString = remember {
        try {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(System.currentTimeMillis() - 86400000L))
        } catch (e: Exception) {
            ""
        }
    }

    LaunchedEffect(activeProfileId, currentDateString) {
        if (lastStreakDate.isNotEmpty() && lastStreakDate != currentDateString && lastStreakDate != yesterdayDateString) {
            streakCount = 1
            sharedPrefs.edit().putInt("profile_${activeProfileId}_streak_count", 1).apply()
        }
    }

    val isDailyRewardClaimedToday = (dailyClaimedDate == currentDateString)

    val claimDailyReward = {
        val rewardDay = ((streakCount - 1) % 7) + 1
        val rewardAmount = when (rewardDay) {
            1 -> 100
            2 -> 150
            3 -> 200
            4 -> 250
            5 -> 300
            6 -> 400
            7 -> 1000
            else -> 100
        }
        
        val newStreak = if (lastStreakDate == yesterdayDateString || lastStreakDate.isEmpty()) streakCount + 1 else if (lastStreakDate == currentDateString) streakCount else 1
        streakCount = newStreak
        lastStreakDate = currentDateString
        dailyClaimedDate = currentDateString
        coins += rewardAmount
        
        sharedPrefs.edit().apply {
            putInt("profile_${activeProfileId}_streak_count", newStreak)
            putString("profile_${activeProfileId}_last_streak_date", currentDateString)
            putString("profile_${activeProfileId}_daily_claimed_date", currentDateString)
            putInt("profile_${activeProfileId}_coins", coins)
            apply()
        }
        
        showToast("🎁 Day $rewardDay Daily Reward Claimed! +$rewardAmount Coins!")
    }
    
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lastRewardedTime) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val cooldownMillis = 0L // No cooldown limitation on rewarded ads
    val elapsed = currentTimeMillis - lastRewardedTime
    val remainingSeconds = (((cooldownMillis - elapsed) / 1000L).coerceAtLeast(0L)).toInt()
    
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
        val combinedHash = (currentLevelIndex.toLong() + activeProfileId.hashCode().toLong())
        val poolIndex = (Math.abs(combinedHash) % puzzlePool.size).toInt()
        val nameIndex = (Math.abs(combinedHash) % levelNames.size).toInt()
        
        val chosenPuzzle = puzzlePool[poolIndex]
        val chosenName = "${levelNames[nameIndex]} ${currentLevelIndex + 1}"
        
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
        // App background image with dark overlay for optimal legibility
        Image(
            painter = painterResource(id = R.drawable.wcbg),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Subtle dark overlay to ensure readability of text and buttons
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
        )

        // Custom Confetti Particle overlay celebrating level clearance!
        ConfettiOverlay(visible = showLevelComplete)

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Column {
                    // --- ADMOB ADAPTIVE BANNER AREA (SAFE BOTTOM POSITIONING WITH CLEAR PADDING) ---
                    var bannerAdViewRef by remember { mutableStateOf<AdView?>(null) }
                    DisposableEffect(Unit) {
                        onDispose {
                            try {
                                bannerAdViewRef?.destroy()
                            } catch (e: Throwable) {}
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0D1714))
                            .border(1.dp, Color(0xFF264038))
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("admob_banner"),
                            factory = { ctx ->
                                try {
                                    AdView(ctx).apply {
                                        setAdSize(AdSize.BANNER)
                                        adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test Banner ID
                                        loadAd(AdRequest.Builder().build())
                                        bannerAdViewRef = this
                                    }
                                } catch (e: Throwable) {
                                    // Provide safe fallback View if AdView fails in emulator or offline environment
                                    android.view.View(ctx)
                                }
                            }
                        )
                    }

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

            // Daily Streak Chip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            if (!isDailyRewardClaimedToday) Color(0xFFFFE082) else Color(0xFFFFF3E0).copy(alpha = 0.85f),
                            RoundedCornerShape(16.dp)
                        )
                        .border(
                            1.2.dp,
                            if (!isDailyRewardClaimedToday) Color(0xFFFF9800) else Color(0xFFFFCC80),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { showDailyStreakDialog = true }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🔥 Daily Streak: $streakCount Days",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFD84315)
                        )
                    )
                    if (!isDailyRewardClaimedToday) {
                        Text(
                            text = "🎁 CLAIM BONUS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFBF360C)
                            ),
                            modifier = Modifier
                                .background(Color(0xFFFFB300), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    } else {
                        Text(
                            text = "✅",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
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
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "FIND ALL WORDS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF5D4037), // Neat brown header
                                letterSpacing = 1.5.sp
                            )
                        )

                        // Target grid displaying word spaces flexibly using FlowRow
                        val wordGridScrollState = rememberScrollState()
                        val isScrollable = wordGridScrollState.maxValue > 0
                        val canScrollDown = wordGridScrollState.canScrollForward

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    @OptIn(ExperimentalLayoutApi::class)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .verticalScroll(wordGridScrollState)
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        currentLevel.targetWords.forEach { word ->
                                            val isSolved = solvedWords.contains(word)
                                            val hints = revealedIndicesByWord[word] ?: emptySet()

                                            val cellWidth = when {
                                                word.length >= 8 -> 21.dp
                                                word.length >= 7 -> 23.dp
                                                word.length >= 6 -> 25.dp
                                                word.length >= 5 -> 27.dp
                                                else -> 29.dp
                                            }
                                            val cellHeight = when {
                                                word.length >= 8 -> 25.dp
                                                word.length >= 7 -> 27.dp
                                                word.length >= 6 -> 29.dp
                                                word.length >= 5 -> 31.dp
                                                else -> 33.dp
                                            }
                                            val fontSize = when {
                                                word.length >= 8 -> 11.sp
                                                word.length >= 7 -> 12.sp
                                                word.length >= 6 -> 13.sp
                                                word.length >= 5 -> 14.sp
                                                else -> 15.sp
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

                                    // Custom Visible Vertical Scrollbar
                                    if (isScrollable) {
                                        Box(
                                            modifier = Modifier
                                                .padding(start = 4.dp)
                                                .width(6.dp)
                                                .height(90.dp)
                                                .background(Color(0xFFD7CCC8), RoundedCornerShape(3.dp))
                                        ) {
                                            val scrollPercent = if (wordGridScrollState.maxValue > 0) {
                                                wordGridScrollState.value.toFloat() / wordGridScrollState.maxValue.toFloat()
                                            } else 0f
                                            val thumbOffset = (58.dp * scrollPercent)

                                            Box(
                                                modifier = Modifier
                                                    .offset(y = thumbOffset)
                                                    .fillMaxWidth()
                                                    .height(32.dp)
                                                    .background(Color(0xFF8D6E63), RoundedCornerShape(3.dp))
                                            )
                                        }
                                    }
                                }
                            }

                            // Visible Scroll Indicator Banner for hidden words
                            if (isScrollable) {
                                Row(
                                    modifier = Modifier
                                        .background(Color(0xFFFFF3E0), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (canScrollDown) "📜 Scroll down for more words ↓" else "📜 Scroll up ↑",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100),
                                            fontSize = 11.sp
                                        )
                                    )
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
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
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
                        .widthIn(min = 180.dp, max = 242.dp)
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
                    val circleRadius = widthPx * 0.32f
                    val touchThreshold = widthPx * 0.16f
                    val letterSize = with(density) { (maxWidth * 0.22f).coerceIn(34.dp, 48.dp) }
                    val halfSizePx = with(density) { (letterSize / 2).toPx() }

                    // Calculate coordinate targets for letters safely on all screen sizes & OS versions
                    val positions = remember(shuffledLetters, widthPx, heightPx) {
                        if (widthPx > 0f && heightPx > 0f) {
                            shuffledLetters.indices.map { i ->
                                val angle = (360f / shuffledLetters.size * i - 90f) * (Math.PI / 180f)
                                Offset(
                                    x = center.x + circleRadius * cos(angle).toFloat(),
                                    y = center.y + circleRadius * sin(angle).toFloat()
                                )
                            }
                        } else {
                            emptyList()
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
                                    .offset { offsetDp }
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

                // Hint Button right (deducts 500 coins and finds an uncompleted word)
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
                                    val foundWord = uncompleted.random()
                                    coins -= 500
                                    solvedWords.add(foundWord)
                                    solvedWordsString = solvedWords.joinToString(";")
                                    
                                    saveGameState(currentLevelIndex, coins, solvedWords)
                                    playSound(true)
                                    triggerHaptic()
                                    showToast("💡 Word found: $foundWord!")

                                    if (solvedWords.size == currentLevel.targetWords.size) {
                                        coins += 50 // Level clear bonus
                                        saveGameState(currentLevelIndex, coins, solvedWords)
                                        showLevelComplete = true
                                    }
                                } else {
                                    // Let user know they can earn free coins under the Free Coins tab
                                    showToast("Not enough coins! Directing to Free Coins...")
                                    selectedTab = AppTab.FREE_COINS
                                }
                            } else {
                                showToast("All words in this level are already found!")
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
                            contentDescription = "Buy Word Hint",
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
                "3. Word Hints" to "Spending 500 coins on hints instantly grants and reveals a full target word in the puzzle!",
                "4. Recharging Coin Supply" to "Need coins? Visit the 'Free Coins' tab and watch rewarded video ads anytime with no daily limits for +500 free coins each!"
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

                    val isCooldownActive = remainingSeconds > 0

                    Text(
                        text = "✨ Unlimited Rewarded Ads! Watch anytime for +500 Coins",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Button(
                        onClick = {
                            activity.showRewarded(
                                onAwardReward = { amt ->
                                    coins += 500
                                    onAdWatchedSuccessfully(amt)
                                    saveGameState(currentLevelIndex, coins, solvedWords)
                                    showToast("🎁 Congratulations! +500 Coins added to your journey!")
                                    selectedTab = AppTab.GAME
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800)
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
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Watch Icon",
                                tint = Color.White
                            )
                            Text(
                                text = "WATCH VIDEO (+500 COINS)",
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
            val shareContext = LocalContext.current
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val activity = (shareContext as? Activity)
                                    val rootView = activity?.window?.decorView?.rootView
                                    if (rootView != null && rootView.width > 0 && rootView.height > 0) {
                                        val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
                                        val canvas = Canvas(bitmap)
                                        rootView.draw(canvas)

                                        val imagesDir = File(shareContext.cacheDir, "images")
                                        if (!imagesDir.exists()) {
                                            imagesDir.mkdirs()
                                        }
                                        val imageFile = File(imagesDir, "level_complete_share.png")
                                        val outputStream = FileOutputStream(imageFile)
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                                        outputStream.flush()
                                        outputStream.close()

                                        val contentUri: Uri = FileProvider.getUriForFile(
                                            shareContext,
                                            "${shareContext.packageName}.fileprovider",
                                            imageFile
                                        )

                                        val shareText = "🎮 I just cleared Level ${currentLevelIndex + 1} (${currentLevel.name}) in Word Connect Stories!\n" +
                                                "🏆 Rank: ${getPlayerRankBadge(currentLevelIndex + 1).title}\n" +
                                                "🔥 Daily Streak: $streakCount Days!\n" +
                                                "Can you beat my vocabulary score? Download & play now! 🔤✨"

                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/png"
                                            putExtra(Intent.EXTRA_SUBJECT, "Word Connect Stories - Level Complete!")
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            putExtra(Intent.EXTRA_STREAM, contentUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        shareContext.startActivity(Intent.createChooser(shareIntent, "Share victory screenshot via"))
                                    } else {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "Word Connect Stories - Level Complete!")
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "🎮 I just cleared Level ${currentLevelIndex + 1} (${currentLevel.name}) in Word Connect Stories!\n" +
                                                "🏆 Rank: ${getPlayerRankBadge(currentLevelIndex + 1).title}\n" +
                                                "🔥 Daily Streak: $streakCount Days!\n" +
                                                "Can you beat my vocabulary score? Download & play now! 🔤✨"
                                            )
                                        }
                                        shareContext.startActivity(Intent.createChooser(shareIntent, "Share victory via"))
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Word Connect Stories - Level Complete!")
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "🎮 I just cleared Level ${currentLevelIndex + 1} (${currentLevel.name}) in Word Connect Stories!\n" +
                                            "🏆 Rank: ${getPlayerRankBadge(currentLevelIndex + 1).title}\n" +
                                            "🔥 Daily Streak: $streakCount Days!\n" +
                                            "Can you beat my vocabulary score? Download & play now! 🔤✨"
                                        )
                                    }
                                    shareContext.startActivity(Intent.createChooser(shareIntent, "Share victory via"))
                                }
                            },
                            border = BorderStroke(1.5.dp, Color(0xFF2E7D32)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Progress",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontWeight = FontWeight.Bold)
                        }

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
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("btn_complete_dialog_next")
                        ) {
                            Text("Next Level ➔", color = Color.White, fontWeight = FontWeight.Bold)
                        }
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

        // --- DAILY STREAK CALENDAR MODAL DIALOG ---
        if (showDailyStreakDialog) {
            AlertDialog(
                onDismissRequest = { showDailyStreakDialog = false },
                confirmButton = {
                    Button(
                        onClick = {
                            if (!isDailyRewardClaimedToday) {
                                claimDailyReward()
                            }
                            showDailyStreakDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDailyRewardClaimedToday) Color(0xFF757575) else Color(0xFFFF9800)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("btn_claim_daily_streak")
                    ) {
                        Text(
                            text = if (isDailyRewardClaimedToday) "Claimed Today ✅" else "🎁 Claim Today's Bonus!",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDailyStreakDialog = false }) {
                        Text("Close", color = Color(0xFF795548))
                    }
                },
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔥 DAILY STREAK CALENDAR", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = Color(0xFFD84315)))
                        Text("Log in daily to earn bigger gold coin rewards!", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF5D4037)))
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Streak Banner Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                            border = BorderStroke(1.5.dp, Color(0xFFFFB300)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("🔥", style = MaterialTheme.typography.headlineMedium)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "$streakCount Day Streak!",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = Color(0xFFE65100))
                                    )
                                    Text(
                                        if (isDailyRewardClaimedToday) "Come back tomorrow for Day ${((streakCount) % 7) + 1}!" else "Claim your daily bonus now!",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6D4C41))
                                    )
                                }
                            }
                        }

                        // 7-Day Rewards Grid
                        Text(
                            "7-DAY LOGIN REWARDS",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF8D6E63)),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )

                        val currentCycleDay = ((streakCount - 1) % 7) + 1
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            (1..7).forEach { dayNum ->
                                val dayRewards = listOf(100, 150, 200, 250, 300, 400, 1000)
                                val rewardVal = dayRewards[dayNum - 1]
                                val isCurrentDay = (dayNum == currentCycleDay)
                                val isPastDay = (dayNum < currentCycleDay) || (isCurrentDay && isDailyRewardClaimedToday)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                isCurrentDay && !isDailyRewardClaimedToday -> Color(0xFFFFF176)
                                                isPastDay -> Color(0xFFC8E6C9)
                                                else -> Color(0xFFF5F5F5)
                                            }
                                        )
                                        .border(
                                            width = if (isCurrentDay) 1.5.dp else 1.dp,
                                            color = if (isCurrentDay) Color(0xFFFFB300) else Color(0xFFE0E0E0),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .padding(vertical = 8.dp, horizontal = 2.dp)
                                ) {
                                    Text(
                                        "D$dayNum",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrentDay) Color(0xFFE65100) else Color(0xFF616161)
                                        )
                                    )
                                    Text(
                                        if (dayNum == 7) "🎁" else "🪙",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "+$rewardVal",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isPastDay) Color(0xFF2E7D32) else Color(0xFF333333)
                                        )
                                    )
                                    if (isPastDay) {
                                        Text("✓", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }
                },
                containerColor = Color(0xFFFFFDF9),
                shape = RoundedCornerShape(24.dp)
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
                        Text("4. Need help? Tap the ⭐ Star to spend 500 coins and instantly find a word!", color = Color(0xFF5D4037), fontWeight = FontWeight.Medium)
                        Text("5. Short on coins? Watch rewarded videos anytime under the 'Free Coins' tab with no daily limits for +500 free coins!", color = Color(0xFF5D4037), fontWeight = FontWeight.Medium)
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
