package com.xingyue.english.android.ui

import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xingyue.english.android.data.AppConfig
import com.xingyue.english.android.data.BailianTtsVoices
import com.xingyue.english.android.data.ImportUiSeverity
import com.xingyue.english.android.data.PlatformLinkResolver
import com.xingyue.english.android.data.SpeechPlaybackState
import com.xingyue.english.android.data.TtsPlaybackManager
import com.xingyue.english.android.data.XingYueRepository
import com.xingyue.english.core.AbLoopPhase
import com.xingyue.english.core.AbLoopState
import com.xingyue.english.core.CaptionCue
import com.xingyue.english.core.CaptionToken
import com.xingyue.english.core.ArticleDictationEngine
import com.xingyue.english.core.AchievementEngine
import com.xingyue.english.core.BuiltInStudyLexicon
import com.xingyue.english.core.DailyLearningPlan
import com.xingyue.english.core.DailyStudyPath
import com.xingyue.english.core.DictionaryEntry
import com.xingyue.english.core.GuessDescriptionQuestion
import com.xingyue.english.core.ImportProcessingStatus
import com.xingyue.english.core.ImportedContent
import com.xingyue.english.core.LearningGoalMode
import com.xingyue.english.core.LearningPlanEngine
import com.xingyue.english.core.LearningSourceType
import com.xingyue.english.core.LearningWord
import com.xingyue.english.core.LearningWordStatus
import com.xingyue.english.core.LexicalItem
import com.xingyue.english.core.PhraseChunk
import com.xingyue.english.core.PhraseUseCase
import com.xingyue.english.core.PracticeMode
import com.xingyue.english.core.PracticePrompt
import com.xingyue.english.core.PracticeSessionEngine
import com.xingyue.english.core.PlaybackLoopEngine
import com.xingyue.english.core.ReviewRating
import com.xingyue.english.core.ScientificStudyTaskOrder
import com.xingyue.english.core.SourceType
import com.xingyue.english.core.StudyAnalyticsEngine
import com.xingyue.english.core.StudyAttemptStatus
import com.xingyue.english.core.StudyPathEngine
import com.xingyue.english.core.StudyQueuePlanner
import com.xingyue.english.core.StudyPathStage
import com.xingyue.english.core.StudyPathStep
import com.xingyue.english.core.StudyRecord
import com.xingyue.english.core.StudyTaskItem
import com.xingyue.english.core.StudyTaskType
import com.xingyue.english.core.TextTools
import com.xingyue.english.core.TodayVocabularyDashboardUiState
import com.xingyue.english.core.TypeWordsPracticeFlowEngine
import com.xingyue.english.core.TypingPracticeEngine
import com.xingyue.english.core.TypingPracticeResult
import com.xingyue.english.core.VocabularyCoursePlanner
import com.xingyue.english.core.VocabularyDeckStage
import com.xingyue.english.core.VocabularySessionUiState
import com.xingyue.english.core.WordGameEngine
import com.xingyue.english.core.WordLearningPhase
import com.xingyue.english.core.WordleTileState
import com.xingyue.english.core.WordSelectionContext
import com.xingyue.english.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Night = Color(0xFFFAFBFF)
private val Panel = Color(0xF8FFFFFF)
private val PanelBright = Color(0xFFF7F3FF)
private val HoloStroke = Color(0xFFD6DAF0)
private val StrokePurple = Color(0xFFD8C8F6)
private val NeonPurple = Color(0xFF775AF2)
private val NeonBlue = Color(0xFF238BB8)
private val NeonGreen = Color(0xFF23987B)
private val NeonAmber = Color(0xFFC4913E)
private val NeonRed = Color(0xFFD6536A)
private val TextMain = Color(0xFF182135)
private val TextSoft = Color(0xFF657083)
private val HoloGold = Color(0xFFC79A4A)
private val SakuraPink = Color(0xFFE879A0)
private val PorcelainBlue = Color(0xFFE9F4FF)
private val SilkLavender = Color(0xFFF0E8FF)
private val HoloTitleFont = FontFamily(Font(R.font.exo2_variable))
private val HoloMonoFont = FontFamily(Font(R.font.fira_code_variable))
private val HoloBodyFont = FontFamily.SansSerif
private val HoloEase = CubicBezierEasing(0.25f, 0.46f, 0.45f, 0.94f)
private val ExpoEase = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

private enum class MainTab(val label: String) {
    HOME("学习"),
    LIBRARY("素材"),
    STUDY("练习"),
    WORDS("词库"),
    PROFILE("我的")
}

private data class PlaybackSegment(
    val startMs: Long,
    val endMs: Long,
    val enabled: Boolean,
    val label: String,
    val repeat: Boolean = true
)

private data class HoloParticle(
    val x: Float,
    val y: Float,
    val drift: Float,
    val size: Float,
    val color: Color,
    val phase: Float
)

private enum class AnimeSceneKind {
    MoonLibrary,
    TrainWindow,
    StarClassroom,
    VoiceStudio,
    MaterialsDock,
    ProfileBay
}

private fun AnimeSceneKind.drawableRes(): Int =
    when (this) {
        AnimeSceneKind.MoonLibrary -> R.drawable.xy_scene_home
        AnimeSceneKind.TrainWindow -> R.drawable.xy_scene_player
        AnimeSceneKind.StarClassroom -> R.drawable.xy_scene_learn
        AnimeSceneKind.VoiceStudio -> R.drawable.xy_scene_practice
        AnimeSceneKind.MaterialsDock -> R.drawable.xy_scene_materials
        AnimeSceneKind.ProfileBay -> R.drawable.xy_scene_profile
    }

private val DefaultHoloMotionLevel = HoloMotionLevel.Standard

@Composable
fun XingYueApp(
    repository: XingYueRepository,
    onImportUri: (Uri) -> Unit
) {
    val contents by repository.observeContents().collectAsStateWithLifecycle(initialValue = emptyList())
    val words by repository.observeWords().collectAsStateWithLifecycle(initialValue = emptyList())
    val records by repository.observeStudyRecords().collectAsStateWithLifecycle(initialValue = emptyList())
    val config by repository.observeConfig().collectAsStateWithLifecycle(initialValue = AppConfig())
    val lexicalItems by repository.observeLexicalItems().collectAsStateWithLifecycle(initialValue = BuiltInStudyLexicon.lexicalItems)
    val phraseChunks by repository.observePhraseChunks().collectAsStateWithLifecycle(initialValue = BuiltInStudyLexicon.phraseChunks)
    val navController = rememberNavController()
    val ttsPlaybackManager = remember { TtsPlaybackManager() }
    DisposableEffect(Unit) { onDispose { ttsPlaybackManager.close() } }

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = NeonPurple,
            secondary = NeonBlue,
            surface = Panel,
            onSurface = TextMain,
            onPrimary = Color.White,
            background = Night
        ),
        typography = MaterialTheme.typography.copy(
            displayLarge = MaterialTheme.typography.displayLarge.copy(fontFamily = HoloTitleFont, letterSpacing = 0.5.sp),
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontFamily = HoloTitleFont, letterSpacing = 0.3.sp),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = HoloTitleFont),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = HoloBodyFont),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = HoloBodyFont),
            labelLarge = MaterialTheme.typography.labelLarge.copy(fontFamily = HoloTitleFont, letterSpacing = 0.2.sp)
        )
    ) {
        NavHost(navController = navController, startDestination = "main") {
            composable(
                route = "main?tab={tab}",
                arguments = listOf(navArgument("tab") { type = NavType.StringType; defaultValue = MainTab.HOME.name })
            ) { entry ->
                val initialTab = runCatching {
                    enumValueOf<MainTab>(entry.arguments?.getString("tab").orEmpty())
                }.getOrDefault(MainTab.HOME)
                MainTabs(contents, words, records, config, lexicalItems, phraseChunks, repository, navController, onImportUri, initialTab)
            }
            composable(
                route = "content/{id}?startMs={startMs}&endMs={endMs}&autoplay={autoplay}&loop={loop}",
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("startMs") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("endMs") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("autoplay") { type = NavType.BoolType; defaultValue = false },
                    navArgument("loop") { type = NavType.BoolType; defaultValue = false }
                )
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                val startMs = entry.arguments?.getLong("startMs") ?: 0L
                val endMs = entry.arguments?.getLong("endMs") ?: 0L
                val autoplay = entry.arguments?.getBoolean("autoplay") ?: false
                val loop = entry.arguments?.getBoolean("loop") ?: false
                contents.firstOrNull { it.id == id }?.let {
                    ContentDetailScreen(it, startMs, endMs, autoplay, loop, words, repository, navController, ttsPlaybackManager)
                }
            }
            composable(
                route = "word/{normalized}",
                arguments = listOf(navArgument("normalized") { type = NavType.StringType })
            ) { entry ->
                val normalized = entry.arguments?.getString("normalized").orEmpty()
                WordDetailScreen(words.firstOrNull { it.normalized == normalized }, repository, navController, ttsPlaybackManager)
            }
            composable(
                route = "study/{task}",
                arguments = listOf(navArgument("task") { type = NavType.StringType })
            ) { entry ->
                val task = runCatching {
                    enumValueOf<StudyTaskType>(entry.arguments?.getString("task").orEmpty())
                }.getOrDefault(StudyTaskType.NEW_WORDS)
                StudyTaskScreen(task, words, contents, records, repository, navController)
            }
            composable(
                route = "study-path/{stage}/{goal}",
                arguments = listOf(
                    navArgument("stage") { type = NavType.StringType },
                    navArgument("goal") { type = NavType.StringType }
                )
            ) { entry ->
                val stage = runCatching {
                    enumValueOf<StudyPathStage>(entry.arguments?.getString("stage").orEmpty())
                }.getOrDefault(StudyPathStage.CORE_VOCAB)
                val goal = runCatching {
                    enumValueOf<LearningGoalMode>(entry.arguments?.getString("goal").orEmpty())
                }.getOrDefault(config.learningGoalMode)
                StudyPathDetailScreen(stage, goal, words, contents, records, lexicalItems, phraseChunks, repository, navController, ttsPlaybackManager)
            }
            composable(
                route = "practice/{mode}/{goal}",
                arguments = listOf(
                    navArgument("mode") { type = NavType.StringType },
                    navArgument("goal") { type = NavType.StringType }
                )
            ) { entry ->
                val mode = runCatching {
                    enumValueOf<PracticeMode>(entry.arguments?.getString("mode").orEmpty())
                }.getOrDefault(PracticeMode.COPY_TYPING)
                val goal = runCatching {
                    enumValueOf<LearningGoalMode>(entry.arguments?.getString("goal").orEmpty())
                }.getOrDefault(config.learningGoalMode)
                PracticeScreen(mode, goal, words, contents, records, lexicalItems, phraseChunks, repository, navController, ttsPlaybackManager)
            }
            composable(
                route = "word-game/{type}",
                arguments = listOf(navArgument("type") { type = NavType.StringType })
            ) { entry ->
                WordGameScreen(entry.arguments?.getString("type").orEmpty(), words, lexicalItems, repository, navController)
            }
            composable("analytics") {
                AnalyticsScreen(words, records, navController)
            }
            composable("achievements") {
                AchievementsScreen(contents, words, records, navController)
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MainTabs(
    contents: List<ImportedContent>,
    words: List<LearningWord>,
    records: List<StudyRecord>,
    config: AppConfig,
    lexicalItems: List<LexicalItem>,
    phraseChunks: List<PhraseChunk>,
    repository: XingYueRepository,
    navController: NavController,
    onImportUri: (Uri) -> Unit,
    initialTab: MainTab
) {
    var tab by rememberSaveable(initialTab) { mutableStateOf(initialTab) }
    var showLinkDialog by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(repository) {
        repository.observeImportEvents().collect { event ->
            val prefix = when (event.severity) {
                ImportUiSeverity.SUCCESS -> "完成"
                ImportUiSeverity.WARNING -> "注意"
                ImportUiSeverity.ERROR -> "失败"
                ImportUiSeverity.INFO -> "处理中"
            }
            snackbarHostState.showSnackbar(
                message = "$prefix：${event.message}",
                actionLabel = event.actionLabel,
                withDismissAction = true,
                duration = if (event.severity == ImportUiSeverity.ERROR) SnackbarDuration.Long else SnackbarDuration.Short
            )
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImportUri(uri)
    }
    val launchImport = {
        importer.launch(arrayOf("video/*", "audio/*", "text/*", "application/pdf", "application/json", "application/octet-stream", "*/*"))
    }
    val plan = remember(words, contents, records) { LearningPlanEngine.buildTodayPlan(words, contents, records) }
    val streak = remember(records) { LearningPlanEngine.streakDays(records) }

    NebulaScaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xF2FFFFFF),
                tonalElevation = 0.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(1.dp, HoloStroke.copy(alpha = 0.86f), RoundedCornerShape(28.dp))
            ) {
                MainTab.values().forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon(), contentDescription = item.label) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonPurple,
                            selectedTextColor = NeonPurple,
                            indicatorColor = SakuraPink.copy(alpha = 0.16f),
                            unselectedIconColor = TextSoft,
                            unselectedTextColor = TextSoft
                        )
                    )
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInHorizontally { it / 10 }) togetherWith
                    (fadeOut(tween(160)) + slideOutHorizontally { -it / 12 })
            },
            label = "main-tab",
            modifier = Modifier.padding(padding)
        ) { current ->
            when (current) {
                MainTab.HOME -> HomeScreen(contents, words, records, plan, config.learningGoalMode, lexicalItems, streak, launchImport, { showLinkDialog = true }, { tab = MainTab.STUDY }, navController)
                MainTab.LIBRARY -> LibraryScreen(contents, words, records, launchImport, { showLinkDialog = true }, repository, navController)
                MainTab.STUDY -> StudyScreen(plan, records, contents, words, config, lexicalItems, phraseChunks, repository, navController)
                MainTab.WORDS -> WordsScreen(words, navController)
                MainTab.PROFILE -> ProfileScreen(config, repository)
            }
        }
    }

    if (showLinkDialog) {
        DirectLinkDialog(
            onDismiss = { showLinkDialog = false },
            onSubmit = { url ->
                showLinkDialog = false
                scope.launch { runCatching { repository.importDirectUrl(url) } }
            }
        )
    }
}

@Composable
private fun HomeScreen(
    contents: List<ImportedContent>,
    words: List<LearningWord>,
    records: List<StudyRecord>,
    plan: DailyLearningPlan,
    goalMode: LearningGoalMode,
    lexicalItems: List<LexicalItem>,
    streak: Int,
    launchImport: () -> Unit,
    launchLink: () -> Unit,
    openStudy: () -> Unit,
    navController: NavController
) {
    val dashboard = remember(goalMode, plan, words, records) {
        VocabularyCoursePlanner.buildDashboard(goalMode, plan, words, records)
    }
    val session = remember(goalMode, words, lexicalItems) {
        VocabularyCoursePlanner.buildSession(goalMode, words, preferredSessionSize = dashboard.newWordTarget, sourceItems = lexicalItems)
    }
    val lastStudyByContent = remember(records) {
        records.filter { it.contentId.isNotBlank() }
            .groupBy { it.contentId }
            .mapValues { row -> row.value.maxOf { it.createdAt } }
    }
    val recent = contents.sortedByDescending { lastStudyByContent[it.id] ?: it.createdAt }.take(4)
    LazyColumn(contentPadding = PaddingValues(20.dp, 22.dp, 20.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            MoonHeroHeader(
                eyebrow = "今日学习",
                title = "星月陪你学英语",
                subtitle = "复习、新词、短句和素材听读一起推进。",
                metric = "到期 ${dashboard.dueReviewCount} · 错词 ${dashboard.mistakeCount} · 连续 ${streak} 天",
                icon = Icons.Filled.MenuBook,
                scene = AnimeSceneKind.MoonLibrary,
                sceneLabel = "学习"
            )
        }
        item {
            TodayVocabCommandCard(
                dashboard = dashboard,
                session = session,
                onPrimary = {
                    when (dashboard.nextQueueType) {
                        StudyTaskType.DUE_REVIEW,
                        StudyTaskType.MISTAKES,
                        StudyTaskType.SPELLING -> navController.navigate("study/${dashboard.nextQueueType.name}")
                        else -> navController.navigate("practice/${PracticeMode.COPY_TYPING.name}/${goalMode.name}")
                    }
                },
                onSpell = { navController.navigate("practice/${PracticeMode.CN_TO_EN.name}/${goalMode.name}") },
                onBook = openStudy
            )
        }
        item {
            AnimeStoryboardStrip()
        }
        item {
            val current = contents
                .filter { it.status == ImportProcessingStatus.READY_TO_LEARN }
                .maxByOrNull { lastStudyByContent[it.id] ?: it.createdAt }
                ?: contents.firstOrNull()
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("继续学习", "上次位置")
                if (current == null) {
                    HomeEmptyLearningCard()
                } else {
                    ContinueCard(current, words.count { word -> word.contexts.any { it.sourceItemId == current.id } }, navController)
                }
            }
        }
        item {
            SectionTitle("素材收词", "字幕和例句")
            HoloImportCommandDeck(
                onLink = launchLink,
                onVideo = launchImport,
                onAudio = launchImport,
                onSubtitle = launchImport,
                onDocument = launchImport
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                QuickImportCard("视频", Icons.Filled.VideoFile, launchImport, Modifier.weight(1f))
                QuickImportCard("音频", Icons.Filled.Headphones, launchImport, Modifier.weight(1f))
                QuickImportCard("字幕", Icons.Filled.Subtitles, launchImport, Modifier.weight(1f))
                QuickImportCard("文档", Icons.Filled.UploadFile, launchImport, Modifier.weight(1f))
            }
        }
        item {
            SectionTitle("今日学习", "学习记录")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("新词", dashboard.newWordTarget.toString(), Icons.Filled.School, NeonPurple, Modifier.weight(1f))
                StatCard("复习", dashboard.dueReviewCount.toString(), Icons.Filled.Replay, NeonGreen, Modifier.weight(1f))
                StatCard("错词", dashboard.mistakeCount.toString(), Icons.Filled.Refresh, NeonAmber, Modifier.weight(1f))
            }
        }
        item { SectionTitle("素材例句", "最近") }
        items(recent, key = { it.id }) { content ->
            CompactContentRow(content, navController)
        }
    }
}

@Composable
private fun TodayVocabCommandCard(
    dashboard: TodayVocabularyDashboardUiState,
    session: VocabularySessionUiState,
    onPrimary: () -> Unit,
    onSpell: () -> Unit,
    onBook: () -> Unit
) {
    GlowCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(dashboard.goalMode.label(), color = NeonBlue, fontFamily = HoloMonoFont, fontWeight = FontWeight.Bold)
                Text("今天先背 ${dashboard.newWordTarget} 个词", color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = HoloTitleFont)
                Text(dashboard.message, color = TextSoft)
                Text("听音 · 例句 · 拼写 · 反馈", color = TextSoft, fontSize = 13.sp)
            }
            HoloTag("${session.cards.size} 词待学", NeonPurple)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            NeonButton(dashboard.primaryActionLabel, Icons.Filled.PlayArrow, onPrimary, Modifier.weight(1f))
            OutlineNeonButton("拼写小测", Icons.Filled.CheckCircle, onSpell, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("到期", dashboard.dueReviewCount.toString(), Icons.Filled.Replay, NeonGreen, Modifier.weight(1f))
            StatCard("错词", dashboard.mistakeCount.toString(), Icons.Filled.Refresh, NeonRed, Modifier.weight(1f))
            StatCard("素材词", dashboard.subtitleWordCount.toString(), Icons.Filled.Subtitles, NeonBlue, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        OutlineNeonButton("打开词书计划", Icons.Filled.Book, onBook, Modifier.fillMaxWidth())
    }
}

@Composable
private fun LibraryScreen(
    contents: List<ImportedContent>,
    words: List<LearningWord>,
    records: List<StudyRecord>,
    launchImport: () -> Unit,
    launchLink: () -> Unit,
    repository: XingYueRepository,
    navController: NavController
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("全部") }
    var deleteTarget by remember { mutableStateOf<ImportedContent?>(null) }
    val scope = rememberCoroutineScope()
    val filtered = contents
        .filter { filter == "全部" || it.kind.label() == filter || (filter == "收藏" && it.favorite) }
        .filter { query.isBlank() || it.title.contains(query, true) || it.statusMessage.contains(query, true) }
    val lastStudyByContent = remember(records) {
        records.filter { it.contentId.isNotBlank() }
            .groupBy { it.contentId }
            .mapValues { row -> row.value.maxOf { it.createdAt } }
    }

    LazyColumn(contentPadding = PaddingValues(20.dp, 24.dp, 20.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            MoonHeroHeader(
                eyebrow = "素材导入",
                title = "素材库",
                subtitle = "视频、字幕和文档都可变成例句与练习。",
                metric = "已导入 ${contents.size} · 可学习 ${contents.count { it.status == ImportProcessingStatus.READY_TO_LEARN }}",
                icon = Icons.Filled.UploadFile,
                scene = AnimeSceneKind.MaterialsDock,
                sceneLabel = "素材"
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                NeonButton("上传内容", Icons.Filled.UploadFile, launchImport, Modifier.weight(1f))
                OutlineNeonButton("粘贴链接", Icons.Filled.Link, launchLink, Modifier.weight(1f))
            }
        }
        item {
            FilterRow(listOf("全部", "视频", "音频", "字幕", "文档", "收藏"), filter) { filter = it }
        }
        item {
            SearchField(query, "搜索标题、关键词或来源") { query = it }
        }
        item {
            ProcessingPanel(contents.firstOrNull { it.status !in setOf(ImportProcessingStatus.READY_TO_LEARN, ImportProcessingStatus.FAILED) })
        }
        items(filtered, key = { it.id }) { content ->
            ContentMediaCard(
                content = content,
                wordCount = words.count { word -> word.contexts.any { it.sourceItemId == content.id } },
                lastStudyAt = lastStudyByContent[content.id],
                onOpen = { navController.navigate("content/${content.id}") },
                onRetry = { scope.launch { repository.retryProcessing(content.id) } },
                onFavorite = { scope.launch { repository.toggleFavorite(content.id) } },
                onDelete = { deleteTarget = content }
            )
        }
    }

    deleteTarget?.let { target ->
        DeleteDialog(
            title = target.title,
            onDismiss = { deleteTarget = null },
            onKeepWords = {
                deleteTarget = null
                scope.launch { repository.deleteContent(target.id, deleteSourceWords = false) }
            },
            onDeleteWords = {
                deleteTarget = null
                scope.launch { repository.deleteContent(target.id, deleteSourceWords = true) }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudyScreen(
    plan: DailyLearningPlan,
    records: List<StudyRecord>,
    contents: List<ImportedContent>,
    words: List<LearningWord>,
    config: AppConfig,
    lexicalItems: List<LexicalItem>,
    phraseChunks: List<PhraseChunk>,
    repository: XingYueRepository,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    var goalMode by remember(config.learningGoalMode) { mutableStateOf(config.learningGoalMode) }
    var studySection by rememberSaveable { mutableStateOf("今日") }
    val path = remember(goalMode, plan, records, contents, words, lexicalItems, phraseChunks) {
        StudyPathEngine.buildTodayPath(
            goalMode = goalMode,
            plan = plan,
            contents = contents,
            words = words,
            attempts = records,
            lexicalItems = lexicalItems,
            phraseChunks = phraseChunks
        )
    }
    val dashboard = remember(goalMode, plan, words, records) {
        VocabularyCoursePlanner.buildDashboard(goalMode, plan, words, records)
    }
    val session = remember(goalMode, words, lexicalItems) {
        VocabularyCoursePlanner.buildSession(goalMode, words, dashboard.newWordTarget, sourceItems = lexicalItems)
    }
    val reviewCount = path.steps.firstOrNull { it.stage == StudyPathStage.VOCAB_WARMUP }?.goalValue ?: 0
    val listeningMinutes = records
        .filter { it.taskType == StudyTaskType.HUNDRED_LS || it.taskType == StudyTaskType.LISTENING_REPEAT }
        .sumOf { (it.durationMs / 60_000L).toInt().coerceAtLeast(0) }

    fun openStep(step: StudyPathStep) {
        when {
            step.stage == StudyPathStage.MATERIAL_INTENSIVE_LISTENING && step.contentId.isNotBlank() ->
                navController.navigate("content/${step.contentId}?autoplay=true")
            step.stage == StudyPathStage.MANUAL_WORD_CAPTURE && step.contentId.isNotBlank() ->
                navController.navigate("content/${step.contentId}")
            else -> navController.navigate("study-path/${step.stage.name}/${goalMode.name}")
        }
    }

    fun completeStep(step: StudyPathStep) {
        scope.launch { repository.recordStudyPathStep(step) }
    }

    LazyColumn(contentPadding = PaddingValues(20.dp, 24.dp, 20.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            MoonHeroHeader(
                eyebrow = "词库计划",
                title = "练习计划",
                subtitle = "复习、背词、短句和听读在同一条线里。",
                metric = "今日 ${dashboard.newWordTarget} 新词 · ${dashboard.dueReviewCount} 到期",
                icon = Icons.Filled.School,
                scene = AnimeSceneKind.StarClassroom,
                sceneLabel = "练习"
            )
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LearningGoalMode.values().forEach { mode ->
                    GoalModeChip(
                        mode = mode,
                        selected = goalMode == mode,
                        onClick = {
                            goalMode = mode
                            scope.launch { repository.saveLearningGoalMode(mode) }
                        }
                    )
                }
            }
        }
        item { StudyPathHero(path, dashboard, session) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("今日新词", dashboard.newWordTarget.toString(), Icons.Filled.School, NeonPurple, Modifier.weight(1f))
                StatCard("到期复习", reviewCount.toString(), Icons.Filled.Replay, NeonGreen, Modifier.weight(1f))
                StatCard("听力分钟", listeningMinutes.toString(), Icons.Filled.Headphones, NeonAmber, Modifier.weight(1f))
            }
        }
        item { FilterRow(listOf("今日", "词库", "练习", "统计"), studySection) { studySection = it } }
        when (studySection) {
            "今日" -> {
                item {
                    Text("今日顺序", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("先复习，再学新词。", color = TextSoft)
                }
                item {
                    TodayVocabCommandCard(
                        dashboard = dashboard,
                        session = session,
                        onPrimary = {
                            when (dashboard.nextQueueType) {
                                StudyTaskType.DUE_REVIEW,
                                StudyTaskType.MISTAKES,
                                StudyTaskType.SPELLING -> navController.navigate("study/${dashboard.nextQueueType.name}")
                                else -> navController.navigate("practice/${PracticeMode.COPY_TYPING.name}/${goalMode.name}")
                            }
                        },
                        onSpell = { navController.navigate("practice/${PracticeMode.CN_TO_EN.name}/${goalMode.name}") },
                        onBook = { studySection = "词库" }
                    )
                }
                items(path.steps, key = { it.id }) { step ->
                    StudyPathStepCard(
                        step = step,
                        onPrimary = { openStep(step) },
                        onComplete = { completeStep(step) }
                    )
                }
            }
            "练习" -> {
                item {
                    MoonPracticeHub(goalMode = goalMode, navController = navController)
                }
            }
            "词库" -> {
                item {
                    Text("词库阶梯", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("核心词、短语、进阶词和个人素材词。", color = TextSoft)
                }
                item {
                    VocabularyStageRail(path = path, savedWords = words.size, lexicalItems = lexicalItems)
                }
            }
            else -> {
                item {
                    GlowCard {
                        Text("学习统计", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("背词、复习、错词和听力进度。", color = TextSoft)
                        Spacer(Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("复习优先", "新词小组", "错词回流", "素材例句").forEachIndexed { index, label ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(label) },
                                    leadingIcon = { Icon(listOf(Icons.Filled.Book, Icons.Filled.Headphones, Icons.Filled.Replay, Icons.Filled.Star)[index], null) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalModeChip(mode: LearningGoalMode, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Column {
                Text(mode.label(), fontWeight = FontWeight.Bold)
                Text(mode.shortCopy(), fontSize = 11.sp, color = if (selected) TextMain else TextSoft, maxLines = 1)
            }
        },
        leadingIcon = {
            Icon(
                when (mode) {
                    LearningGoalMode.GENERAL -> Icons.Filled.Book
                    LearningGoalMode.IELTS -> Icons.Filled.Star
                    LearningGoalMode.TOEFL -> Icons.Filled.School
                },
                null
            )
        }
    )
}

@Composable
private fun StudyPathHero(path: DailyStudyPath, dashboard: TodayVocabularyDashboardUiState, session: VocabularySessionUiState) {
    GlowCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(path.goalMode.label(), color = NeonPurple, fontFamily = HoloMonoFont, fontWeight = FontWeight.Bold)
                Text("复习优先 · ${dashboard.newWordTarget} 词一组", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = HoloTitleFont)
                Text("本组 ${session.cards.size} 词，按听音、例句、拼写和反馈推进。", color = TextSoft)
                path.nextStep?.let {
                    Text("下一步：${it.title} · ${it.estimatedMinutes} 分钟", color = NeonGreen, fontWeight = FontWeight.Bold)
                }
            }
            AnimatedRing(progress = path.progressFraction, color = NeonBlue)
        }
    }
}

private data class PracticeHubEntry(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val route: String,
    val essential: Boolean
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoonPracticeHub(goalMode: LearningGoalMode, navController: NavController) {
    val entries = remember(goalMode) {
        listOf(
            PracticeHubEntry("打字背词", "看词形主动输入", Icons.Filled.MenuBook, NeonPurple, "practice/${PracticeMode.COPY_TYPING.name}/${goalMode.name}", true),
            PracticeHubEntry("听写", "听音到拼写连接", Icons.Filled.VolumeUp, NeonBlue, "practice/${PracticeMode.DICTATION.name}/${goalMode.name}", true),
            PracticeHubEntry("逐句默写", "字幕/短句整句回忆", Icons.Filled.FilterList, HoloGold, "practice/${PracticeMode.SPELLING_MEMORY.name}/${goalMode.name}", true),
            PracticeHubEntry("学习模式", "跟写、听写、默写、复习连续走完", Icons.Filled.School, NeonGreen, "practice/${PracticeMode.TYPEWORDS_SYSTEM.name}/${goalMode.name}", true),
            PracticeHubEntry("错词回流", "错误优先清理", Icons.Filled.Refresh, NeonRed, "study/${StudyTaskType.MISTAKES.name}", true),
            PracticeHubEntry("自由练习", "只跟写当前新词", Icons.Filled.PlayArrow, NeonPurple, "practice/${PracticeMode.TYPEWORDS_FREE.name}/${goalMode.name}", false),
            PracticeHubEntry("随机复习", "打散旧词主动回忆", Icons.Filled.FilterList, NeonAmber, "practice/${PracticeMode.TYPEWORDS_SHUFFLE.name}/${goalMode.name}", false),
            PracticeHubEntry("统计", "正确率/听力分钟", Icons.Filled.CheckCircle, NeonBlue, "analytics", false),
            PracticeHubEntry("成就", "学习里程碑", Icons.Filled.School, HoloGold, "achievements", false),
            PracticeHubEntry("Wordle", "词形轻量巩固", Icons.Filled.Star, NeonGreen, "word-game/WORDLE", false),
            PracticeHubEntry("猜描述", "释义识别巩固", Icons.Filled.Search, NeonAmber, "word-game/GUESS_DESCRIPTION", false)
        )
    }
    GlowCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("练习与反馈", color = TextMain, fontSize = 23.sp, fontWeight = FontWeight.Black, fontFamily = HoloTitleFont)
                Text("听写、默写、错词和词形巩固。", color = TextSoft)
            }
            HoloTag(goalMode.shortCopy(), NeonBlue)
        }
        Spacer(Modifier.height(14.dp))
        Text("必做练习", color = TextMain, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            entries.filter { it.essential }.forEach { entry ->
                PracticeHubTile(entry = entry, onClick = { navController.navigate(entry.route) })
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("辅助巩固", color = TextMain, fontWeight = FontWeight.Bold)
        Text("完成主线后再练。", color = TextSoft, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            entries.filterNot { it.essential }.forEach { entry ->
                PracticeHubTile(entry = entry, onClick = { navController.navigate(entry.route) })
            }
        }
    }
}

@Composable
private fun PracticeHubTile(entry: PracticeHubEntry, onClick: () -> Unit) {
    HoloPressSurface(
        modifier = Modifier.width(148.dp).height(96.dp),
        color = Color.White.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, entry.color.copy(alpha = 0.34f)),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(entry.icon, null, tint = entry.color)
                Canvas(Modifier.size(22.dp)) {
                    drawCircle(entry.color.copy(alpha = 0.12f), radius = size.minDimension / 2f)
                    drawArc(entry.color.copy(alpha = 0.34f), -90f, 240f, false, style = Stroke(1.dp.toPx(), cap = StrokeCap.Round))
                }
            }
            Column {
                Text(entry.title, color = TextMain, fontWeight = FontWeight.Black, fontFamily = HoloTitleFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(entry.subtitle, color = TextSoft, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudyPathStepCard(
    step: StudyPathStep,
    onPrimary: () -> Unit,
    onComplete: () -> Unit
) {
    val accent = step.stage.accent()
    GlowCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                Surface(
                    color = accent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.42f)),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(step.stage.icon(), null, tint = accent)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(step.categoryTitle, color = accent, fontFamily = HoloMonoFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(step.title, color = TextMain, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(step.subtitle, color = TextSoft, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
            if (step.completed) {
                Icon(Icons.Filled.CheckCircle, null, tint = NeonGreen, modifier = Modifier.padding(start = 8.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { step.progressFraction },
            color = accent,
            trackColor = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(99.dp))
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            step.highlights.take(4).forEach { label ->
                AssistChip(onClick = {}, label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) })
            }
        }
        if (step.blocked) {
            Text(step.blockerMessage, color = NeonAmber, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlineNeonButton(step.actionLabel, step.stage.icon(), onPrimary, Modifier.weight(1f))
            NeonButton(if (step.completed) "已完成" else "记为完成", Icons.Filled.CheckCircle, onComplete, Modifier.weight(1f))
        }
    }
}

@Composable
private fun VocabularyStageRail(path: DailyStudyPath, savedWords: Int, lexicalItems: List<LexicalItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        path.deckStages.forEachIndexed { index, stage ->
            val sampleCount = lexicalItems.ifEmpty { BuiltInStudyLexicon.lexicalItems }.count { it.stage == stage }
            val target = BuiltInStudyLexicon.decks.firstOrNull { it.stage == stage }?.itemTarget ?: 0
            VocabularyStageCard(
                index = index + 1,
                stage = stage,
                active = stage == path.diagnostic.recommendedStage,
                sampleCount = if (stage == VocabularyDeckStage.MY_SUBTITLE_WORDS) savedWords else sampleCount,
                target = target
            )
        }
    }
}

@Composable
private fun VocabularyStageCard(
    index: Int,
    stage: VocabularyDeckStage,
    active: Boolean,
    sampleCount: Int,
    target: Int
) {
    Surface(
        color = if (active) NeonPurple.copy(alpha = 0.14f) else PanelBright.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, if (active) NeonPurple.copy(alpha = 0.65f) else HoloStroke),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                Text(index.toString().padStart(2, '0'), color = if (active) NeonPurple else TextSoft, fontFamily = HoloMonoFont, fontWeight = FontWeight.Bold)
                Column {
                    Text(stage.label(), color = TextMain, fontWeight = FontWeight.Bold)
                    Text(stage.copyLine(), color = TextSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (target > 0) "$sampleCount/$target" else sampleCount.toString(), color = if (active) NeonGreen else TextSoft, fontFamily = HoloMonoFont)
                Text(if (active) "当前" else "阶段", color = TextSoft, fontSize = 12.sp)
            }
        }
    }
}

private fun LearningGoalMode.label(): String =
    when (this) {
        LearningGoalMode.GENERAL -> "通用英语"
        LearningGoalMode.IELTS -> "IELTS"
        LearningGoalMode.TOEFL -> "TOEFL"
    }

private fun LearningGoalMode.shortCopy(): String =
    when (this) {
        LearningGoalMode.GENERAL -> "字幕听读"
        LearningGoalMode.IELTS -> "话题输出"
        LearningGoalMode.TOEFL -> "学术讲座"
    }

private fun StudyPathStage.icon(): androidx.compose.ui.graphics.vector.ImageVector =
    when (this) {
        StudyPathStage.VOCAB_WARMUP -> Icons.Filled.Replay
        StudyPathStage.CORE_VOCAB -> Icons.Filled.Book
        StudyPathStage.PHRASE_INPUT -> Icons.Filled.MenuBook
        StudyPathStage.MATERIAL_INTENSIVE_LISTENING -> Icons.Filled.Headphones
        StudyPathStage.SHADOWING_OUTPUT -> Icons.Filled.VolumeUp
        StudyPathStage.MANUAL_WORD_CAPTURE -> Icons.Filled.Star
        StudyPathStage.EXTENSIVE_LISTENING -> Icons.Filled.Audiotrack
    }

private fun StudyPathStage.accent(): Color =
    when (this) {
        StudyPathStage.VOCAB_WARMUP -> NeonGreen
        StudyPathStage.CORE_VOCAB -> NeonPurple
        StudyPathStage.PHRASE_INPUT -> HoloGold
        StudyPathStage.MATERIAL_INTENSIVE_LISTENING -> NeonBlue
        StudyPathStage.SHADOWING_OUTPUT -> NeonAmber
        StudyPathStage.MANUAL_WORD_CAPTURE -> NeonPurple
        StudyPathStage.EXTENSIVE_LISTENING -> NeonGreen
    }

private fun VocabularyDeckStage.label(): String =
    when (this) {
        VocabularyDeckStage.CORE_3500 -> "Core 3500"
        VocabularyDeckStage.CORE_PHRASES_1200 -> "Core Phrases 1200"
        VocabularyDeckStage.ADVANCED_2000 -> "Advanced 2000"
        VocabularyDeckStage.AWL_570 -> "AWL 570"
        VocabularyDeckStage.IELTS_TOPICS -> "IELTS Topic Packs"
        VocabularyDeckStage.TOEFL_ACADEMIC -> "TOEFL Academic Packs"
        VocabularyDeckStage.MY_SUBTITLE_WORDS -> "My Subtitle Words"
    }

private fun VocabularyDeckStage.copyLine(): String =
    when (this) {
        VocabularyDeckStage.CORE_3500 -> "先背基础高频词，支撑 A1-B2 听读。"
        VocabularyDeckStage.CORE_PHRASES_1200 -> "高频短句、动词短语、搭配和听力块。"
        VocabularyDeckStage.ADVANCED_2000 -> "B2-C1 扩展词，提高阅读密度和表达准确性。"
        VocabularyDeckStage.AWL_570 -> "学术词族桥梁，服务阅读、写作和讲座听力。"
        VocabularyDeckStage.IELTS_TOPICS -> "教育、科技、环境等雅思话题词和表达块。"
        VocabularyDeckStage.TOEFL_ACADEMIC -> "讲座信号词、校园表达和学科主题词。"
        VocabularyDeckStage.MY_SUBTITLE_WORDS -> "字幕里保存的个人词。"
    }

@Composable
private fun StudyPathDetailScreen(
    stage: StudyPathStage,
    goalMode: LearningGoalMode,
    words: List<LearningWord>,
    contents: List<ImportedContent>,
    records: List<StudyRecord>,
    lexicalItems: List<LexicalItem>,
    phraseChunks: List<PhraseChunk>,
    repository: XingYueRepository,
    navController: NavController,
    ttsPlaybackManager: TtsPlaybackManager
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val systemTts = remember { TextToSpeech(context) { } }
    DisposableEffect(Unit) { onDispose { systemTts.shutdown() } }
    val plan = remember(words, contents, records) { LearningPlanEngine.buildTodayPlan(words, contents, records) }
    val path = remember(stage, goalMode, plan, records, contents, words, lexicalItems, phraseChunks) {
        StudyPathEngine.buildTodayPath(
            goalMode = goalMode,
            plan = plan,
            contents = contents,
            words = words,
            attempts = records,
            lexicalItems = lexicalItems,
            phraseChunks = phraseChunks
        )
    }
    val step = path.steps.firstOrNull { it.stage == stage } ?: path.nextStep ?: path.steps.first()
    val caption by if (step.contentId.isNotBlank()) {
        repository.observeCaption(step.contentId).collectAsStateWithLifecycle(initialValue = null)
    } else {
        remember { mutableStateOf(null) }
    }
    val content = contents.firstOrNull { it.id == step.contentId }
    var speechState by remember { mutableStateOf<SpeechPlaybackState>(SpeechPlaybackState.Idle) }
    var message by remember(step.id) { mutableStateOf("") }

    fun complete(completedValue: Int = step.goalValue, durationMs: Long = step.estimatedMinutes * 60_000L) {
        scope.launch {
            repository.recordStudyPathStep(step, completedValue = completedValue, durationMs = durationMs)
            message = "已记录：${step.title}"
        }
    }

    val lexicalQueue = remember(step.id, goalMode, lexicalItems, records) {
        StudyQueuePlanner.nextLexicalQueue(goalMode, step.deckStage, lexicalItems, records, sessionSize = 8)
    }
    val phraseQueue = remember(goalMode, phraseChunks, records) {
        StudyQueuePlanner.nextPhraseQueue(goalMode, phraseChunks, records, sessionSize = 8)
    }
    val shadowingQueue = remember(caption, goalMode) { caption?.cues?.take(5).orEmpty() }

    NebulaScaffold(topBar = { DetailTopBar(step.title, navController) }) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(padding)
        ) {
            item {
                GlowCard {
                    Text(step.categoryTitle, color = step.stage.accent(), fontFamily = HoloMonoFont, fontWeight = FontWeight.Bold)
                    Text(step.title, color = TextMain, fontSize = 30.sp, fontFamily = HoloTitleFont, fontWeight = FontWeight.Black)
                    Text(step.subtitle, color = TextSoft)
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { step.progressFraction },
                        color = step.stage.accent(),
                        trackColor = Color.White.copy(alpha = 0.10f),
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                    )
                    if (message.isNotBlank()) Text(message, color = NeonGreen, modifier = Modifier.padding(top = 8.dp))
                    if (speechState !is SpeechPlaybackState.Idle) Text(speechState.message, color = NeonAmber, modifier = Modifier.padding(top = 8.dp))
                }
            }
            when (stage) {
                StudyPathStage.VOCAB_WARMUP -> {
                    item {
                        PathWordQueueCard(
                            title = "到期复习",
                            words = plan.dueReviews.take(6),
                            emptyText = "今天没有到期复习。",
                            onOpen = { navController.navigate("study/${StudyTaskType.DUE_REVIEW.name}") }
                        )
                    }
                    item {
                        PathWordQueueCard(
                            title = "错词回顾",
                            words = plan.mistakes.take(6),
                            emptyText = "今天没有错词回流。",
                            onOpen = { navController.navigate("study/${StudyTaskType.MISTAKES.name}") }
                        )
                    }
                    item { NeonButton("完成热身", Icons.Filled.CheckCircle, { complete() }, Modifier.fillMaxWidth()) }
                }
                StudyPathStage.CORE_VOCAB -> {
                    if (lexicalQueue.isEmpty()) {
                        item {
                            GlowCard {
                                Text("当前词组已学完", color = TextMain, fontSize = 23.sp, fontWeight = FontWeight.Black)
                                Text("继续短句、复习到期词，或导入新的词书和素材。", color = TextSoft)
                            }
                        }
                    } else {
                        items(lexicalQueue, key = { it.id }) { item ->
                            LexicalPracticeCard(item = item, onSpeak = {
                                scope.launch { speak(repository, item.word, systemTts, ttsPlaybackManager) { speechState = it } }
                            })
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlineNeonButton("打字背词", Icons.Filled.MenuBook, { navController.navigate("practice/${PracticeMode.COPY_TYPING.name}/${goalMode.name}") }, Modifier.weight(1f))
                            OutlineNeonButton("听写", Icons.Filled.VolumeUp, { navController.navigate("practice/${PracticeMode.DICTATION.name}/${goalMode.name}") }, Modifier.weight(1f))
                        }
                    }
                    item { NeonButton("完成这一组词", Icons.Filled.CheckCircle, { complete(completedValue = lexicalQueue.size.coerceAtLeast(1)) }, Modifier.fillMaxWidth()) }
                }
                StudyPathStage.PHRASE_INPUT -> {
                    if (phraseQueue.isEmpty()) {
                        item {
                            GlowCard {
                                Text("当前短句已学完", color = TextMain, fontSize = 23.sp, fontWeight = FontWeight.Black)
                                Text("进入逐句默写，或从词书短语和文章句子继续扩展。", color = TextSoft)
                            }
                        }
                    } else {
                        items(phraseQueue, key = { it.id }) { chunk ->
                            PhrasePracticeCard(chunk = chunk, onSpeak = {
                                scope.launch { speak(repository, chunk.english, systemTts, ttsPlaybackManager) { speechState = it } }
                            })
                        }
                    }
                    item { OutlineNeonButton("进入逐句默写", Icons.Filled.FilterList, { navController.navigate("practice/${PracticeMode.SPELLING_MEMORY.name}/${goalMode.name}") }, Modifier.fillMaxWidth()) }
                    item { NeonButton("完成短句输入", Icons.Filled.CheckCircle, { complete(completedValue = phraseQueue.size.coerceAtLeast(1)) }, Modifier.fillMaxWidth()) }
                }
                StudyPathStage.MATERIAL_INTENSIVE_LISTENING -> {
                    item { ContentActionCard(content, "逐句精听", "打开素材，按双语时间线逐句听。", navController) }
                    item { NeonButton("记录精听完成", Icons.Filled.CheckCircle, { complete(durationMs = 10L * 60L * 1000L) }, Modifier.fillMaxWidth()) }
                }
                StudyPathStage.SHADOWING_OUTPUT -> {
                    if (shadowingQueue.isEmpty()) {
                        items(phraseQueue.take(5), key = { it.id }) { chunk ->
                            PhrasePracticeCard(chunk = chunk, onSpeak = {
                                scope.launch { speak(repository, chunk.english, systemTts, ttsPlaybackManager) { speechState = it } }
                            })
                        }
                    } else {
                        items(shadowingQueue, key = { it.id }) { cue ->
                            ShadowingCueCard(cue = cue, onSpeak = {
                                scope.launch { speak(repository, cue.english, systemTts, ttsPlaybackManager) { speechState = it } }
                            })
                        }
                    }
                    item { NeonButton("完成 3 句跟读", Icons.Filled.CheckCircle, { complete(completedValue = 3, durationMs = 6L * 60L * 1000L) }, Modifier.fillMaxWidth()) }
                }
                StudyPathStage.MANUAL_WORD_CAPTURE -> {
                    item {
                        GlowCard {
                            Text("字幕收词", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("点选字幕词并确认后，才会进入个人词库。", color = TextSoft)
                            Text("今天已手动保存 ${words.count { isToday(it.createdAt) }} 个词", color = NeonPurple, fontWeight = FontWeight.Bold)
                        }
                    }
                    item { ContentActionCard(content, "去字幕里收词", "打开素材后点击需要掌握的词。", navController) }
                }
                StudyPathStage.EXTENSIVE_LISTENING -> {
                    item { ContentActionCard(content, "泛听素材", "记录 5/15/30 分钟听力。", navController) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(5, 15, 30).forEach { minutes ->
                                OutlineNeonButton("${minutes}分钟", Icons.Filled.Headphones, {
                                    complete(completedValue = minutes, durationMs = minutes * 60_000L)
                                }, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PathWordQueueCard(title: String, words: List<LearningWord>, emptyText: String, onOpen: () -> Unit) {
    GlowCard {
        Text(title, color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (words.isEmpty()) {
            Text(emptyText, color = TextSoft)
        } else {
            words.forEach { word ->
                Text("${word.word} · ${word.chineseDefinition.ifBlank { word.status.label() }}", color = TextSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlineNeonButton("打开队列", Icons.Filled.PlayArrow, onOpen, Modifier.fillMaxWidth())
    }
}

@Composable
private fun LexicalPracticeCard(item: LexicalItem, onSpeak: () -> Unit) {
    GlowCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.word, color = TextMain, fontSize = 28.sp, fontFamily = HoloTitleFont, fontWeight = FontWeight.Black)
                Text("${item.cefr.ifBlank { "CEFR" }} · ${item.stage.label()}", color = NeonPurple, fontFamily = HoloMonoFont, fontSize = 12.sp)
            }
            IconButton(onClick = onSpeak) { Icon(Icons.Filled.VolumeUp, null, tint = NeonGreen) }
        }
        Text(item.definition, color = TextSoft)
        if (item.phrases.isNotEmpty()) Text(item.phrases.joinToString(" / "), color = HoloGold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (item.example.isNotBlank()) Text(item.example, color = TextMain.copy(alpha = 0.82f), maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PhrasePracticeCard(chunk: PhraseChunk, onSpeak: () -> Unit) {
    GlowCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(chunk.useCase.label(), color = NeonBlue, fontFamily = HoloMonoFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(chunk.english, color = TextMain, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onSpeak) { Icon(Icons.Filled.VolumeUp, null, tint = NeonGreen) }
        }
        Text(chunk.chinese, color = TextSoft)
        Text(chunk.keywords.joinToString(" / "), color = HoloGold, fontSize = 12.sp)
    }
}

@Composable
private fun ShadowingCueCard(cue: CaptionCue, onSpeak: () -> Unit) {
    GlowCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(formatMs(cue.startMs), color = NeonPurple, fontFamily = HoloMonoFont, fontWeight = FontWeight.Bold)
            IconButton(onClick = onSpeak) { Icon(Icons.Filled.VolumeUp, null, tint = NeonGreen) }
        }
        Text(cue.english, color = TextMain, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        if (cue.chinese.isNotBlank()) Text(cue.chinese, color = TextSoft)
    }
}

@Composable
private fun ContentActionCard(content: ImportedContent?, title: String, body: String, navController: NavController) {
    GlowCard {
        Text(title, color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(content?.title ?: body, color = TextSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(10.dp))
        OutlineNeonButton(if (content != null) "打开素材" else "暂无可用素材", Icons.Filled.PlayArrow, {
            if (content != null) navController.navigate("content/${content.id}?autoplay=true")
        }, Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
private fun PracticeScreen(
    mode: PracticeMode,
    goalMode: LearningGoalMode,
    words: List<LearningWord>,
    contents: List<ImportedContent>,
    records: List<StudyRecord>,
    lexicalItems: List<LexicalItem>,
    phraseChunks: List<PhraseChunk>,
    repository: XingYueRepository,
    navController: NavController,
    ttsPlaybackManager: TtsPlaybackManager
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val systemTts = remember { TextToSpeech(context) { } }
    DisposableEffect(Unit) { onDispose { systemTts.shutdown() } }
    val prompts = remember(mode, goalMode, words, contents, records, lexicalItems, phraseChunks) {
        practicePrompts(mode, goalMode, words, contents, records, lexicalItems, phraseChunks).take(10)
    }
    val courseSession = remember(goalMode, words, lexicalItems) {
        VocabularyCoursePlanner.buildSession(goalMode, words, preferredSessionSize = 10, sourceItems = lexicalItems)
    }
    var index by rememberSaveable(mode.name, goalMode.name) { mutableStateOf(0) }
    var answer by rememberSaveable(mode.name, goalMode.name) { mutableStateOf("") }
    var startedAt by remember(mode.name, goalMode.name, index) { mutableLongStateOf(System.currentTimeMillis()) }
    var sessionStartedAt by remember(mode.name, goalMode.name) { mutableLongStateOf(System.currentTimeMillis()) }
    var results by remember(mode.name, goalMode.name, prompts) { mutableStateOf<List<TypingPracticeResult>>(emptyList()) }
    var currentResult by remember(mode.name, goalMode.name, index) { mutableStateOf<TypingPracticeResult?>(null) }
    var sessionSaved by remember(mode.name, goalMode.name, prompts) { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    var speechState by remember { mutableStateOf<SpeechPlaybackState>(SpeechPlaybackState.Idle) }
    val safeIndex = index.coerceIn(0, (prompts.size - 1).coerceAtLeast(0))
    val prompt = prompts.getOrNull(safeIndex)
    val summary = PracticeSessionEngine.summarize(prompts, results)

    fun completionStep(): StudyPathStep =
        StudyPathStep(
            id = "practice-${mode.name.lowercase()}-${goalMode.name.lowercase()}",
            stage = when (mode) {
                PracticeMode.SPELLING_MEMORY -> StudyPathStage.PHRASE_INPUT
                PracticeMode.DICTATION -> StudyPathStage.SHADOWING_OUTPUT
                PracticeMode.COPY_TYPING,
                PracticeMode.SELF_TEST,
                PracticeMode.CN_TO_EN,
                PracticeMode.TYPEWORDS_SYSTEM,
                PracticeMode.TYPEWORDS_FREE,
                PracticeMode.TYPEWORDS_REVIEW,
                PracticeMode.TYPEWORDS_SHUFFLE -> StudyPathStage.CORE_VOCAB
            },
            title = mode.label(),
            subtitle = mode.description(),
            actionLabel = "继续练习",
            categoryTitle = "今天先背",
            goalMode = goalMode,
            taskType = when (mode) {
                PracticeMode.DICTATION,
                PracticeMode.CN_TO_EN,
                PracticeMode.SPELLING_MEMORY,
                PracticeMode.TYPEWORDS_SYSTEM,
                PracticeMode.TYPEWORDS_FREE,
                PracticeMode.TYPEWORDS_REVIEW,
                PracticeMode.TYPEWORDS_SHUFFLE -> StudyTaskType.SPELLING
                PracticeMode.COPY_TYPING,
                PracticeMode.SELF_TEST -> StudyTaskType.NEW_WORDS
            },
            contentId = prompts.firstOrNull()?.sourceId.orEmpty(),
            wordNormalized = prompts.firstOrNull()?.normalized.orEmpty(),
            progressValue = summary.completedCount,
            goalValue = prompts.size.coerceAtLeast(1),
            estimatedMinutes = 8
        )

    fun resetSession() {
        index = 0
        answer = ""
        currentResult = null
        results = emptyList()
        resultText = ""
        sessionSaved = false
        startedAt = System.currentTimeMillis()
        sessionStartedAt = System.currentTimeMillis()
    }

    fun saveCompletedSession(updatedResults: List<TypingPracticeResult>) {
        if (sessionSaved || prompts.isEmpty()) return
        sessionSaved = true
        val duration = System.currentTimeMillis() - sessionStartedAt
        scope.launch {
            repository.recordTypingPracticeSession(
                mode = mode,
                results = updatedResults,
                sourceId = prompts.firstOrNull()?.sourceId.orEmpty(),
                sourceType = prompts.firstOrNull()?.sourceType.orEmpty(),
                targetCount = prompts.size,
                durationMs = duration
            )
            repository.recordStudyPathStep(
                step = completionStep(),
                completedValue = updatedResults.size,
                durationMs = duration
            )
            resultText = "本组完成 · 正确 ${updatedResults.count { it.correct }}/${prompts.size}"
        }
    }

    fun submit() {
        val current = prompt ?: return
        if (answer.isBlank()) {
            resultText = "先输入答案，再判定。"
            return
        }
        val duration = System.currentTimeMillis() - startedAt
        val result = TypingPracticeEngine.evaluate(current, answer, duration)
        keyboardController?.hide()
        currentResult = result
        val updated = (results.filterNot { it.promptId == result.promptId } + result)
            .sortedBy { item -> prompts.indexOfFirst { it.id == item.promptId }.let { if (it < 0) Int.MAX_VALUE else it } }
        results = updated
        resultText = if (result.correct) {
            if (PracticeSessionEngine.summarize(prompts, updated).completed) {
                "最后一题正确，正在保存..."
            } else {
                "正确 · ${result.accuracy}% · ${result.wpm} WPM，点下一题继续"
            }
        } else {
            if (PracticeSessionEngine.summarize(prompts, updated).completed) {
                "已记录本题 · ${result.accuracy}% · 正在保存..."
            } else {
                "已记录本题 · ${result.accuracy}% · ${result.typoCount} 处差异，点下一题继续"
            }
        }
        if (PracticeSessionEngine.summarize(prompts, updated).completed) saveCompletedSession(updated)
    }

    fun goNext() {
        val current = prompt
        if (current != null && results.none { it.promptId == current.id }) {
            resultText = "先判定本题，再进入下一题。"
            return
        }
        val next = PracticeSessionEngine.nextIndex(index, prompts, results)
        if (next == null) {
            saveCompletedSession(results)
            return
        }
        index = next
        answer = ""
        currentResult = null
        resultText = ""
        startedAt = System.currentTimeMillis()
    }

    NebulaScaffold(topBar = { DetailTopBar(mode.label(), navController) }) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(padding)
        ) {
            item {
                GlowCard {
                    Text("月白练习舱", color = NeonPurple, fontFamily = HoloMonoFont, fontWeight = FontWeight.Bold)
                    Text(mode.label(), color = TextMain, fontSize = 32.sp, fontFamily = HoloTitleFont, fontWeight = FontWeight.Black)
                    Text(mode.description(), color = TextSoft)
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        courseSession.phases.forEach { phase ->
                            HoloTag(phase.label(), if (phase == WordLearningPhase.SPELLING_RECALL) NeonAmber else NeonBlue)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { summary.progressFraction },
                        color = NeonBlue,
                        trackColor = HoloStroke.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "本组 ${summary.completedCount}/${summary.targetCount} · 正确 ${summary.correctCount} · 平均 ${summary.averageAccuracy}%",
                        color = TextSoft,
                        fontFamily = HoloMonoFont,
                        fontSize = 13.sp
                    )
                }
            }
            if (prompt == null) {
                item {
                    GlowCard {
                        Text("暂无练习材料", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("先导入素材，或选择通用、IELTS、TOEFL 词库。", color = TextSoft)
                    }
                }
            } else {
                if (summary.completed) {
                    item {
                        GlowCard {
                            Text("本组练习已完成", color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Black)
                            Text("正确 ${summary.correctCount}/${summary.targetCount}，平均准确率 ${summary.averageAccuracy}%。下一组已准备好。", color = TextSoft)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlineNeonButton("再练一组", Icons.Filled.Refresh, { resetSession() }, Modifier.weight(1f))
                                NeonButton("回到学习路径", Icons.Filled.CheckCircle, { navController.popBackStack() }, Modifier.weight(1f))
                            }
                        }
                    }
                }
                if (!summary.completed) {
                    item {
                        GlowCard {
                            Text("${safeIndex + 1}/${prompts.size}", color = NeonBlue, fontFamily = HoloMonoFont, fontWeight = FontWeight.Bold)
                            Text(prompt.stageHint(mode), color = TextSoft)
                            if (!prompt.hideExpected(mode)) {
                                Text(prompt.expected, color = TextMain, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            }
                            if (prompt.chinese.isNotBlank()) Text(prompt.chinese, color = TextSoft)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlineNeonButton("发音", Icons.Filled.VolumeUp, {
                                    scope.launch { speak(repository, prompt.expected, systemTts, ttsPlaybackManager) { speechState = it } }
                                }, Modifier.weight(1f))
                                OutlineNeonButton("提示", Icons.Filled.Visibility, {
                                    resultText = prompt.expected
                                }, Modifier.weight(1f))
                            }
                        }
                    }
                    item {
                        GlowCard {
                            OutlinedTextField(
                                value = answer,
                                onValueChange = { answer = it },
                                label = { Text("输入答案") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { submit() }),
                                singleLine = mode != PracticeMode.SPELLING_MEMORY,
                                minLines = if (mode == PracticeMode.SPELLING_MEMORY) 3 else 1,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                NeonButton("判定", Icons.Filled.CheckCircle, { submit() }, Modifier.weight(1f))
                                OutlineNeonButton("下一题", Icons.Filled.PlayArrow, { goNext() }, Modifier.weight(1f))
                            }
                            val result = currentResult
                            if (result != null) {
                                Text(
                                    "答案：${result.expected}",
                                    color = TextMain,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 10.dp)
                                )
                            }
                            if (resultText.isNotBlank()) Text(
                                resultText,
                                color = if (resultText.startsWith("正确") || resultText.startsWith("最后")) NeonGreen else NeonAmber,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                            SpeechStatusText(speechState)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordGameScreen(
    type: String,
    words: List<LearningWord>,
    lexicalItems: List<LexicalItem>,
    repository: XingYueRepository,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    val title = if (type.equals("GUESS_DESCRIPTION", ignoreCase = true)) "看描述猜词" else "Wordle 词阵"
    val lexicon = remember(words, lexicalItems) {
        val saved = words.map { LexicalItem("saved-${it.normalized}", it.word, definition = it.chineseDefinition, deckId = "my-subtitle-words", stage = VocabularyDeckStage.MY_SUBTITLE_WORDS) }
        (saved + lexicalItems.ifEmpty { BuiltInStudyLexicon.lexicalItems }).distinctBy { it.normalized }
    }
    NebulaScaffold(topBar = { DetailTopBar(title, navController) }) { padding ->
        if (type.equals("GUESS_DESCRIPTION", ignoreCase = true)) {
            GuessDescriptionGame(lexicon, repository, scope, Modifier.padding(padding))
        } else {
            WordleGame(lexicon, repository, scope, Modifier.padding(padding))
        }
    }
}

@Composable
private fun WordleGame(
    lexicon: List<LexicalItem>,
    repository: XingYueRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    modifier: Modifier = Modifier
) {
    val secret = remember(lexicon) { lexicon.firstOrNull { it.word.length in 5..8 }?.word ?: "method" }
    var guess by rememberSaveable { mutableStateOf("") }
    var attempts by remember { mutableStateOf(listOf<com.xingyue.english.core.WordleGuessResult>()) }
    var finished by rememberSaveable { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = modifier) {
        item {
            GlowCard {
                Text("词阵破译", color = TextMain, fontSize = 32.sp, fontFamily = HoloTitleFont, fontWeight = FontWeight.Black)
                Text("6 次机会猜出目标词。", color = TextSoft)
            }
        }
        items(attempts) { attempt ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                attempt.tiles.forEach { tile ->
                    WordleTileBox(tile.char.uppercaseChar().toString(), tile.state, Modifier.weight(1f))
                }
            }
        }
        item {
            GlowCard {
                OutlinedTextField(value = guess, onValueChange = { guess = it.take(secret.length) }, label = { Text("${secret.length} 个字母") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                NeonButton(if (finished) "已完成" else "提交猜测", Icons.Filled.CheckCircle, {
                    if (!finished && guess.isNotBlank()) {
                        val result = WordGameEngine.evaluateWordle(secret, guess, attempts.size + 1)
                        attempts = attempts + result
                        guess = ""
                        if (result.won || result.remainingAttempts == 0) {
                            finished = true
                            scope.launch { repository.recordWordGame("WORDLE", secret, result.won, attempts.size + 1, if (result.won) 100 else 40) }
                        }
                    }
                }, Modifier.fillMaxWidth())
                if (finished) Text("目标词：$secret", color = NeonPurple, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

@Composable
private fun GuessDescriptionGame(
    lexicon: List<LexicalItem>,
    repository: XingYueRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    modifier: Modifier = Modifier
) {
    val questions = remember(lexicon) { WordGameEngine.offlineGuessQuestions(lexicon, 6) }
    var index by rememberSaveable { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    val question = questions.getOrNull(index)
    LazyColumn(contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = modifier) {
        item {
            GlowCard {
                Text("描述猜词", color = TextMain, fontSize = 32.sp, fontFamily = HoloTitleFont, fontWeight = FontWeight.Black)
                Text("根据释义选择单词。", color = TextSoft)
            }
        }
        if (question == null) {
            item { GlowCard { Text("暂无题目", color = TextMain); Text("请先导入词库或保存生词。", color = TextSoft) } }
        } else {
            item { GuessQuestionCard(question, message) { choice ->
                val correct = TextTools.normalizeWord(choice) == TextTools.normalizeWord(question.word)
                message = if (correct) "正确：${question.word}" else "答案是：${question.word}"
                scope.launch { repository.recordWordGame("GUESS_DESCRIPTION", question.word, correct, 1, if (correct) 100 else 50) }
            } }
            item {
                OutlineNeonButton("下一题", Icons.Filled.PlayArrow, {
                    index = (index + 1).coerceAtMost((questions.size - 1).coerceAtLeast(0))
                    message = ""
                }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun GuessQuestionCard(question: GuessDescriptionQuestion, message: String, onChoice: (String) -> Unit) {
    GlowCard {
        Text(question.description, color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            question.choices.forEach { choice ->
                OutlineNeonButton(choice, Icons.Filled.Search, { onChoice(choice) }, Modifier.fillMaxWidth())
            }
        }
        if (message.isNotBlank()) Text(message, color = if (message.startsWith("正确")) NeonGreen else NeonAmber, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun WordleTileBox(text: String, state: WordleTileState, modifier: Modifier = Modifier) {
    val color = when (state) {
        WordleTileState.CORRECT -> NeonGreen
        WordleTileState.PRESENT -> NeonAmber
        WordleTileState.ABSENT -> TextSoft
    }
    Surface(color = color.copy(alpha = 0.16f), border = BorderStroke(1.dp, color.copy(alpha = 0.56f)), shape = RoundedCornerShape(12.dp), modifier = modifier.aspectRatio(1f)) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = TextMain, fontSize = 22.sp, fontFamily = HoloMonoFont, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun AnalyticsScreen(words: List<LearningWord>, records: List<StudyRecord>, navController: NavController) {
    val analytics = remember(words, records) { StudyAnalyticsEngine.build(records, words) }
    NebulaScaffold(topBar = { DetailTopBar("学习统计", navController) }) { padding ->
        LazyColumn(contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(padding)) {
            item {
                GlowCard {
                    Text("月白数据舱", color = TextMain, fontSize = 32.sp, fontFamily = HoloTitleFont, fontWeight = FontWeight.Black)
                    Text("今日练习、正确率、错词和听力分钟。", color = TextSoft)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("今日练习", analytics.todayAttempts.toString(), Icons.Filled.CheckCircle, NeonPurple, Modifier.weight(1f))
                    StatCard("正确率", "${analytics.accuracy}%", Icons.Filled.Star, NeonGreen, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("复习压力", analytics.dueCount.toString(), Icons.Filled.Replay, NeonAmber, Modifier.weight(1f))
                    StatCard("错词", analytics.mistakeCount.toString(), Icons.Filled.Refresh, NeonRed, Modifier.weight(1f))
                }
            }
            item {
                GlowCard {
                    Text("听力累计 ${analytics.listeningMinutes} 分钟", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("个人字幕词 ${analytics.savedWords} 个。", color = TextSoft)
                }
            }
        }
    }
}

@Composable
private fun AchievementsScreen(
    contents: List<ImportedContent>,
    words: List<LearningWord>,
    records: List<StudyRecord>,
    navController: NavController
) {
    val unlocked = remember(contents, words, records) {
        AchievementEngine.evaluate(contents, words, records).map { it.definition.id }.toSet()
    }
    NebulaScaffold(topBar = { DetailTopBar("成就", navController) }) { padding ->
        LazyColumn(contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(padding)) {
            item {
                GlowCard {
                    Text("月白魔法阵成就", color = TextMain, fontSize = 32.sp, fontFamily = HoloTitleFont, fontWeight = FontWeight.Black)
                    Text("完成学习、收词和听力目标后点亮。", color = TextSoft)
                }
            }
            items(AchievementEngine.defaultDefinitions, key = { it.id }) { achievement ->
                AchievementCard(achievement.title, achievement.description, achievement.id in unlocked)
            }
        }
    }
}

@Composable
private fun AchievementCard(title: String, description: String, unlocked: Boolean) {
    GlowCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Canvas(Modifier.size(48.dp)) {
                val color = if (unlocked) HoloGold else TextSoft
                drawCircle(color.copy(alpha = 0.12f), radius = size.minDimension / 2f)
                repeat(3) { ring ->
                    drawArc(color.copy(alpha = 0.42f - ring * 0.09f), ring * 36f, 160f, false, style = Stroke(1.dp.toPx(), cap = StrokeCap.Round))
                }
                drawCircle(color.copy(alpha = if (unlocked) 0.78f else 0.24f), radius = 4.dp.toPx(), center = center)
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = TextMain, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text(description, color = TextSoft)
            }
            HoloTag(if (unlocked) "UNLOCK" else "LOCK", if (unlocked) HoloGold else TextSoft)
        }
    }
}

private fun practicePrompts(
    mode: PracticeMode,
    goalMode: LearningGoalMode,
    words: List<LearningWord>,
    contents: List<ImportedContent>,
    records: List<StudyRecord>,
    lexicalItems: List<LexicalItem>,
    phraseChunks: List<PhraseChunk>
): List<PracticePrompt> {
    val savedPrompts = words.take(12).map { word ->
        PracticePrompt(
            id = "saved-${word.normalized}",
            expected = word.word,
            hint = word.chineseDefinition.ifBlank { word.contexts.firstOrNull()?.chineseSentence.orEmpty() },
            chinese = word.chineseDefinition,
            sourceId = "my-subtitle-words",
            sourceType = VocabularyDeckStage.MY_SUBTITLE_WORDS.name,
            normalized = word.normalized
        )
    }
    val lexical = TypingPracticeEngine.promptsFromLexicalItems(
        StudyQueuePlanner.nextLexicalQueue(
            goalMode,
            deckStage = null,
            sourceItems = lexicalItems,
            attempts = records,
            sessionSize = 16
        ),
        mode,
        limit = 16
    )
    if (mode.isTypeWordsFlow()) {
        val newWords = StudyQueuePlanner.nextLexicalQueue(
            goalMode,
            deckStage = null,
            sourceItems = lexicalItems,
            attempts = records,
            sessionSize = 20
        ).map(TypeWordsPracticeFlowEngine::toTypeWordsWord)
        val reviewWords = words
            .filter { it.status != LearningWordStatus.MASTERED || it.dueAt <= System.currentTimeMillis() }
            .take(40)
            .map(TypeWordsPracticeFlowEngine::toTypeWordsWord)
        return TypeWordsPracticeFlowEngine.prompts(
            appMode = mode,
            newWords = newWords,
            reviewWords = reviewWords,
            sessionLimit = 40
        ).map { it.prompt }
    }
    return when (mode) {
        PracticeMode.SPELLING_MEMORY -> {
            val phrasePrompts = ArticleDictationEngine.fromPhraseChunks(
                StudyQueuePlanner.nextPhraseQueue(goalMode, phraseChunks, records, sessionSize = 10),
                limit = 10
            )
            val documentPrompts = contents
                .filter { it.originalText.isNotBlank() }
                .flatMap { ArticleDictationEngine.fromPlainText(it.id, it.title, it.originalText, limit = 4) }
            (documentPrompts + phrasePrompts).ifEmpty { phrasePrompts + lexical }
        }
        else -> (savedPrompts + lexical).distinctBy { it.normalized.ifBlank { it.expected } }
    }
}

private fun PracticeMode.label(): String =
    when (this) {
        PracticeMode.COPY_TYPING -> "打字背词"
        PracticeMode.DICTATION -> "听写"
        PracticeMode.SELF_TEST -> "自测"
        PracticeMode.CN_TO_EN -> "看中文拼写英文"
        PracticeMode.SPELLING_MEMORY -> "逐句默写"
        PracticeMode.TYPEWORDS_SYSTEM -> "学习模式"
        PracticeMode.TYPEWORDS_FREE -> "自由练习"
        PracticeMode.TYPEWORDS_REVIEW -> "复习模式"
        PracticeMode.TYPEWORDS_SHUFFLE -> "随机复习"
    }

private fun PracticeMode.description(): String =
    when (this) {
        PracticeMode.COPY_TYPING -> "看到英文后跟打，建立词形肌肉记忆。"
        PracticeMode.DICTATION -> "先听发音，再输入单词。"
        PracticeMode.SELF_TEST -> "用释义和例句自测是否真的认识。"
        PracticeMode.CN_TO_EN -> "根据中文主动回忆英文。"
        PracticeMode.SPELLING_MEMORY -> "把短句或字幕整句默写出来，训练表达块。"
        PracticeMode.TYPEWORDS_SYSTEM -> "按跟写、听写、默写、复习的原流程连续学习。"
        PracticeMode.TYPEWORDS_FREE -> "只练当前新词，适合快速加深词形记忆。"
        PracticeMode.TYPEWORDS_REVIEW -> "优先复习旧词和到期词。"
        PracticeMode.TYPEWORDS_SHUFFLE -> "打散词序做随机回忆。"
    }

private fun PracticeMode.isTypeWordsFlow(): Boolean =
    this == PracticeMode.TYPEWORDS_SYSTEM ||
        this == PracticeMode.TYPEWORDS_FREE ||
        this == PracticeMode.TYPEWORDS_REVIEW ||
        this == PracticeMode.TYPEWORDS_SHUFFLE

private fun PracticePrompt.stageHint(mode: PracticeMode): String {
    if (!mode.isTypeWordsFlow()) return if (mode == PracticeMode.DICTATION) "听音后输入" else hint.ifBlank { "输入目标内容" }
    val stage = sourceType
    val prefix = when {
        stage.startsWith("FOLLOW_WRITE") -> "跟写"
        stage.startsWith("LISTEN") -> "听写"
        stage.startsWith("DICTATION") -> "默写"
        stage.startsWith("IDENTIFY") -> "自测"
        stage == "SHUFFLE" -> "随机复习"
        else -> "练习"
    }
    return listOf(prefix, hint.ifBlank { "输入目标内容" }).joinToString(" · ")
}

private fun PracticePrompt.hideExpected(mode: PracticeMode): Boolean {
    if (!mode.isTypeWordsFlow()) {
        return mode == PracticeMode.DICTATION || mode == PracticeMode.CN_TO_EN || mode == PracticeMode.SELF_TEST
    }
    return sourceType.startsWith("LISTEN") ||
        sourceType.startsWith("DICTATION") ||
        sourceType.startsWith("IDENTIFY") ||
        sourceType == "SHUFFLE"
}

private fun PhraseUseCase.label(): String =
    when (this) {
        PhraseUseCase.DAILY -> "日常"
        PhraseUseCase.IELTS_SPEAKING -> "雅思口语"
        PhraseUseCase.IELTS_WRITING -> "雅思写作"
        PhraseUseCase.TOEFL_LECTURE -> "托福讲座"
        PhraseUseCase.TOEFL_CAMPUS -> "托福校园"
    }

@Composable
private fun WordsScreen(words: List<LearningWord>, navController: NavController) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("全部") }
    val filtered = words
        .filter {
            filter == "全部" ||
                (filter == "今天新增" && isToday(it.createdAt)) ||
                (filter == "待复习" && it.dueAt <= System.currentTimeMillis()) ||
                it.status.label() == filter ||
                it.contexts.any { context -> context.sourceType.label() == filter }
        }
        .filter { query.isBlank() || it.word.contains(query, true) || it.chineseDefinition.contains(query, true) }
        .sortedWith(compareBy<LearningWord> { it.dueAt }.thenByDescending { it.occurrenceCount })

    LazyColumn(contentPadding = PaddingValues(20.dp, 24.dp, 20.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("生词本", color = TextMain, fontSize = 40.sp, fontWeight = FontWeight.Black)
                    Text("${words.size} 个单词", color = NeonPurple, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                GlowPill("学习设置", Icons.Filled.Settings)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("掌握中", words.count { it.status == LearningWordStatus.LEARNING }.toString(), Icons.Filled.Replay, NeonPurple, Modifier.weight(1f))
                StatCard("待复习", words.count { it.dueAt <= System.currentTimeMillis() }.toString(), Icons.Filled.Audiotrack, NeonAmber, Modifier.weight(1f))
                StatCard("已熟悉", words.count { it.status == LearningWordStatus.FAMILIAR || it.status == LearningWordStatus.MASTERED }.toString(), Icons.Filled.CheckCircle, NeonGreen, Modifier.weight(1f))
            }
        }
        item { FilterRow(listOf("全部", "今天新增", "待复习", "掌握中", "已熟悉", "视频", "音频", "字幕", "文档"), filter) { filter = it } }
        item { SearchField(query, "搜索单词或释义") { query = it } }
        itemsIndexed(filtered, key = { _, word -> word.normalized }) { index, word ->
            WordCard(word, index) { navController.navigate("word/${word.normalized}") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileScreen(config: AppConfig, repository: XingYueRepository) {
    val scope = rememberCoroutineScope()
    var key by remember(config.bailianKey) { mutableStateOf(config.bailianKey) }
    var enabled by remember(config.ttsEnabled) { mutableStateOf(config.ttsEnabled) }
    var selectedVoice by remember(config.ttsVoiceId) { mutableStateOf(config.ttsVoiceId) }
    var speed by remember(config.ttsSpeed) { mutableStateOf(config.ttsSpeed) }
    var showKey by rememberSaveable { mutableStateOf(false) }
    var testingCloud by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf("") }
    var ttsResult by remember { mutableStateOf("") }
    var dictionaryResult by remember { mutableStateOf("") }
    var backupResult by remember { mutableStateOf("") }
    val dictionaryImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val count = repository.importDictionary(uri)
                dictionaryResult = if (count > 0) "已导入 $count 个词条" else "未读取到有效词条"
            }
        }
    }
    val backupExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                val result = repository.exportBackup(uri)
                backupResult = "已导出 ${result.tableCount} 张表、${result.rowCount} 条记录"
            }
        }
    }
    val backupImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val result = repository.importBackup(uri)
                backupResult = "已恢复 ${result.tableCount} 张表、${result.rowCount} 条记录"
            }
        }
    }

    LazyColumn(contentPadding = PaddingValues(20.dp, 24.dp, 20.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            MoonHeroHeader(
                eyebrow = "个人数据",
                title = "我的",
                subtitle = "目标、发音和本地数据。",
                metric = "本地优先 · 可导出恢复",
                icon = Icons.Filled.Settings,
                scene = AnimeSceneKind.ProfileBay,
                sceneLabel = "我的"
            )
        }
        item {
            GlowCard {
                Text("云端转写", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("未配置时仍可导入本地字幕。", color = TextSoft)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("访问密钥") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null, tint = TextSoft)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlineNeonButton(if (testingCloud) "验证中" else "只验证", Icons.Filled.Refresh, {
                        scope.launch {
                            testingCloud = true
                            val result = repository.testBailianConnection(key)
                            testResult = "${result.message} ${result.checkedCapabilities.joinToString(" / ").ifBlank { "" }}".trim()
                            testingCloud = false
                        }
                    }, Modifier.weight(1f))
                    NeonButton("保存并测试", Icons.Filled.Save, {
                        scope.launch {
                            testingCloud = true
                            repository.saveBailianKey(key)
                            val result = repository.testBailianConnection()
                            testResult = result.message
                            testingCloud = false
                        }
                    }, Modifier.weight(1f))
                }
                if (testResult.isNotBlank()) Text(testResult, color = if (testResult.contains("正常")) NeonGreen else NeonAmber)
            }
        }
        item {
            GlowCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("云端发音", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("开启后优先使用云端音色。", color = TextSoft)
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { value ->
                            enabled = value
                            scope.launch {
                                repository.saveTtsConfig(value, selectedVoice, speed, config.ttsVolume)
                                ttsResult = if (value) {
                                    "云端发音已开启。未配置时使用本机发音。"
                                } else {
                                    "云端发音已关闭。"
                                }
                            }
                        }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = if (enabled) NeonGreen.copy(alpha = 0.12f) else NeonAmber.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, if (enabled) NeonGreen.copy(alpha = 0.38f) else NeonAmber.copy(alpha = 0.38f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (enabled) "当前状态：云端发音已开启" else "当前状态：云端发音已关闭", color = TextMain, fontWeight = FontWeight.Bold)
                        Text(
                            if (enabled) "句子、单词和双语播放会优先使用本地缓存。"
                            else "发音按钮会使用本机发音。",
                            color = TextSoft,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("音色", color = TextMain, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BailianTtsVoices.voices.forEach { voice ->
                        FilterChip(
                            selected = selectedVoice == voice.id,
                            onClick = { selectedVoice = voice.id },
                            label = { Text(voice.name) }
                        )
                    }
                }
                Text(BailianTtsVoices.find(selectedVoice).description, color = TextSoft, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Text("语速 ${"%.1f".format(speed)}x", color = TextSoft)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlineNeonButton("慢一点", Icons.Filled.Replay, { speed = (speed - 0.1f).coerceAtLeast(0.5f) }, Modifier.weight(1f))
                    OutlineNeonButton("快一点", Icons.Filled.PlayArrow, { speed = (speed + 0.1f).coerceAtMost(2.0f) }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                NeonButton(if (enabled) "保存音色和语速" else "保存设置并保持关闭", Icons.Filled.Save, {
                    scope.launch {
                        repository.saveTtsConfig(enabled, selectedVoice, speed, config.ttsVolume)
                        ttsResult = if (enabled) "已保存：云端发音开启，音色 ${BailianTtsVoices.find(selectedVoice).name}，语速 ${"%.1f".format(speed)}x" else "已保存：云端发音关闭"
                    }
                }, Modifier.fillMaxWidth())
                if (ttsResult.isNotBlank()) Text(ttsResult, color = if (enabled) NeonGreen else NeonAmber, modifier = Modifier.padding(top = 8.dp))
            }
        }
        item {
            GlowCard {
                Text("离线词典", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("已内置完整英汉词典，可导入本地词典扩展。", color = TextSoft)
                Spacer(Modifier.height(12.dp))
                OutlineNeonButton("导入本地词典", Icons.Filled.UploadFile, {
                    dictionaryImporter.launch(arrayOf("text/*", "application/json", "text/csv", "*/*"))
                }, Modifier.fillMaxWidth())
                if (dictionaryResult.isNotBlank()) Text(dictionaryResult, color = NeonGreen, modifier = Modifier.padding(top = 8.dp))
            }
        }
        item {
            GlowCard {
                Text("数据备份", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("导出或恢复词库、素材、字幕、复习记录和练习记录。", color = TextSoft)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlineNeonButton("导出", Icons.Filled.Save, {
                        backupExporter.launch("xingyue-learning-backup.json")
                    }, Modifier.weight(1f))
                    OutlineNeonButton("恢复", Icons.Filled.UploadFile, {
                        backupImporter.launch(arrayOf("application/json", "text/*", "*/*"))
                    }, Modifier.weight(1f))
                }
                if (backupResult.isNotBlank()) Text(backupResult, color = NeonGreen, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun ContentDetailScreen(
    content: ImportedContent,
    initialStartMs: Long,
    initialEndMs: Long,
    autoplay: Boolean,
    loop: Boolean,
    words: List<LearningWord>,
    repository: XingYueRepository,
    navController: NavController,
    ttsPlaybackManager: TtsPlaybackManager
) {
    val caption by repository.observeCaption(content.id).collectAsStateWithLifecycle(initialValue = null)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<WordSelectionContext?>(null) }
    var position by remember { mutableLongStateOf(initialStartMs) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var segment by remember { mutableStateOf<PlaybackSegment?>(null) }
    var abState by remember { mutableStateOf(AbLoopState()) }
    var playerNotice by remember { mutableStateOf("") }
    var speechState by remember { mutableStateOf<SpeechPlaybackState>(SpeechPlaybackState.Idle) }
    var autoScrollPausedUntil by remember { mutableLongStateOf(0L) }
    val isMedia = content.kind == SourceType.VIDEO || content.kind == SourceType.AUDIO
    val saved = words.map { it.normalized }.toSet()
    val cues = caption?.cues.orEmpty()
    val currentCue = cues.firstOrNull { position >= it.startMs && position < it.endMs }
        ?: cues.lastOrNull { position >= it.startMs }
        ?: cues.firstOrNull()
    val currentIndex = cues.indexOfFirst { it.id == currentCue?.id }.coerceAtLeast(0)
    val timelineState = rememberLazyListState()
    val systemTts = remember { TextToSpeech(context) { } }

    val player = remember(content.id) {
        if (isMedia && content.sourcePath.isNotBlank()) {
            ExoPlayer.Builder(context).build().apply {
                val uri = if (content.sourcePath.startsWith("http")) Uri.parse(content.sourcePath) else Uri.fromFile(File(content.sourcePath))
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                if (initialStartMs > 0) seekTo(initialStartMs)
            }
        } else null
    }
    DisposableEffect(player) { onDispose { player?.release(); systemTts.shutdown() } }
    LaunchedEffect(player, playbackSpeed) { player?.setPlaybackSpeed(playbackSpeed) }
    LaunchedEffect(player, initialStartMs, initialEndMs, autoplay, loop) {
        if (initialStartMs > 0) player?.seekTo(initialStartMs)
        if (initialEndMs > initialStartMs && loop) {
            segment = PlaybackSegment(initialStartMs, initialEndMs, true, "原片段循环")
        }
        if (autoplay) player?.play()
    }
    LaunchedEffect(player, segment, abState) {
        while (true) {
            player?.let {
                position = it.currentPosition
                val abRange = PlaybackLoopEngine.activeRange(abState)
                val active = segment?.takeIf { loop -> loop.enabled } ?: abRange?.let { range ->
                    PlaybackSegment(range.startMs, range.endMs, true, "A-B 循环", repeat = range.repeat)
                }
                if (active != null && it.currentPosition >= active.endMs) {
                    if (active.repeat) {
                        it.seekTo(active.startMs)
                    } else {
                        it.pause()
                        segment = null
                    }
                }
            }
            delay(350)
        }
    }
    LaunchedEffect(currentCue?.id) {
        currentCue?.let {
            repository.saveReadingProgress(content.id, caption?.id.orEmpty(), it.id, cues.indexOf(it), it.startMs)
        }
    }
    LaunchedEffect(timelineState.isScrollInProgress) {
        if (timelineState.isScrollInProgress) autoScrollPausedUntil = System.currentTimeMillis() + 1600L
    }
    LaunchedEffect(currentIndex, cues.size) {
        if (cues.isNotEmpty() && System.currentTimeMillis() >= autoScrollPausedUntil) {
            timelineState.animateScrollToItem((currentIndex - 1).coerceAtLeast(0))
        }
    }

    NebulaScaffold(topBar = { DetailTopBar(content.title, navController) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 16.dp)) {
                if (isMedia && player != null) {
                    MediaPanel(player, content, position, currentCue, abState)
                } else {
                    ReaderHero(content, cues.indexOf(currentCue).coerceAtLeast(0), cues.size)
                }
                if (isMedia && player != null) {
                    PlayerControls(
                        isPlaying = player.isPlaying,
                        speed = playbackSpeed,
                        abState = abState,
                        replayEnabled = currentCue != null,
                        onPlayPause = { if (player.isPlaying) player.pause() else player.play() },
                        onSpeed = { playbackSpeed = if (playbackSpeed >= 1.25f) 0.75f else playbackSpeed + 0.25f },
                        onReplayCue = {
                            currentCue?.let { cue ->
                                player.seekTo(cue.startMs)
                                position = cue.startMs
                                player.play()
                                segment = PlaybackSegment(cue.startMs, cue.endMs, enabled = true, label = "单句重播", repeat = false)
                                playerNotice = "正在播放当前句。"
                            }
                        },
                        onAb = {
                            val update = PlaybackLoopEngine.onAbMarkerTap(abState, player.currentPosition)
                            abState = update.state
                            playerNotice = update.message
                            segment = null
                            if (update.seekToStart) player.seekTo(update.state.startMs)
                        }
                    )
                    PlayerLoopStatus(abState, segment, playerNotice)
                }
                if (!isMedia && playerNotice.isNotBlank()) {
                    Text(playerNotice, color = NeonBlue, fontSize = 13.sp)
                }
                if (cues.isNotEmpty()) {
                    CurrentCaptionHud(
                        cue = currentCue,
                        currentIndex = currentIndex,
                        total = cues.size,
                        content = content
                    )
                }
                SpeechStatusText(speechState)
            }
            LazyColumn(
                state = timelineState,
                contentPadding = PaddingValues(top = 14.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (cues.isEmpty()) {
                    item {
                        SubtitleUnavailablePanel(
                            content = content,
                            onRetry = { scope.launch { repository.retryProcessing(content.id) } },
                            onConfigureKey = { navController.navigate("main?tab=${MainTab.PROFILE.name}") },
                            onReimport = { navController.navigate("main?tab=${MainTab.HOME.name}") }
                        )
                    }
                } else {
                    items(cues, key = { it.id }) { cue ->
                        CaptionLine(
                            cue = cue.copy(tokens = TextTools.tokenize(cue.english, saved)),
                            active = cue.id == currentCue?.id,
                            sourceType = content.kind.toLearningSource(),
                            content = content,
                            onSeek = { player?.seekTo(cue.startMs); position = cue.startMs },
                            onReplay = {
                                player?.seekTo(cue.startMs)
                                position = cue.startMs
                                player?.play()
                                segment = PlaybackSegment(cue.startMs, cue.endMs, enabled = true, label = "单句重播", repeat = false)
                            },
                            onSpeakEnglish = { scope.launch { speak(repository, cue.english, systemTts, ttsPlaybackManager) { speechState = it } } },
                            onSpeakChinese = { scope.launch { speak(repository, cue.chinese.ifBlank { cue.english }, systemTts, ttsPlaybackManager) { speechState = it } } },
                            onSpeakBilingual = {
                                val text = listOf(cue.english, cue.chinese).filter { it.isNotBlank() }.joinToString("。")
                                scope.launch { speak(repository, text, systemTts, ttsPlaybackManager) { speechState = it } }
                            },
                            onWord = { word ->
                                selected = selection(content, cue, word, caption?.id.orEmpty())
                            },
                            onLongWord = { word ->
                                scope.launch { repository.addFromSelection(selection(content, cue, word, caption?.id.orEmpty())) }
                            }
                        )
                    }
                }
            }
        }
    }

    selected?.let { selection ->
        LookupSheet(
            context = selection,
            alreadySaved = selection.normalized in saved,
            repository = repository,
            ttsPlaybackManager = ttsPlaybackManager,
            onDismiss = { selected = null },
            onOpenDetail = { navController.navigate("word/${selection.normalized}") },
            onPlaySource = {
                player?.seekTo(selection.captionStartMs)
                player?.play()
                segment = PlaybackSegment(selection.captionStartMs, selection.captionEndMs, true, "原声片段")
            }
        )
    }
}

@Composable
private fun WordDetailScreen(
    word: LearningWord?,
    repository: XingYueRepository,
    navController: NavController,
    ttsPlaybackManager: TtsPlaybackManager
) {
    val scope = rememberCoroutineScope()
    var notes by remember(word?.normalized) { mutableStateOf(word?.notes.orEmpty()) }
    var entry by remember(word?.normalized) { mutableStateOf<DictionaryEntry?>(null) }
    var speechState by remember { mutableStateOf<SpeechPlaybackState>(SpeechPlaybackState.Idle) }
    val context = LocalContext.current
    val systemTts = remember {
        TextToSpeech(context) { }
    }
    DisposableEffect(Unit) { onDispose { systemTts.shutdown() } }
    LaunchedEffect(word?.normalized) {
        if (word != null) entry = repository.lookupWord(word.word)
    }

    NebulaScaffold(topBar = { DetailTopBar("单词详情", navController) }) { padding ->
        if (word == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("单词不存在", color = TextSoft)
            }
            return@NebulaScaffold
        }
        LazyColumn(contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(padding)) {
            item {
                GlowCard {
                    Text(word.word, color = TextMain, fontSize = 46.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(word.phonetic.ifBlank { entry?.phonetic.orEmpty() }, color = TextSoft, fontSize = 18.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeonButton("播放发音", Icons.Filled.VolumeUp, {
                            scope.launch { speak(repository, word.word, systemTts, ttsPlaybackManager) { speechState = it } }
                        }, Modifier.weight(1f))
                        OutlineNeonButton("加入复习", Icons.Filled.Replay, {
                            scope.launch { repository.reviewWord(word.normalized, ReviewRating.HARD) }
                        }, Modifier.weight(1f))
                        OutlineNeonButton("标记掌握", Icons.Filled.Star, {
                            scope.launch { repository.markWord(word.normalized, LearningWordStatus.MASTERED) }
                        }, Modifier.weight(1f))
                    }
                    SpeechStatusText(speechState)
                }
            }
            item { InfoBlock("中文释义", word.chineseDefinition.ifBlank { entry?.definition ?: "离线词典暂无结果。" }) }
            word.contexts.firstOrNull()?.let { source ->
                item {
                    GlowCard {
                        SectionLabel("原字幕语境")
                        Text(source.englishSentence, color = TextMain, fontSize = 17.sp)
                        if (source.chineseSentence.isNotBlank()) Text(source.chineseSentence, color = TextSoft)
                        Text("— ${source.sourceTitle}", color = NeonPurple)
                        Spacer(Modifier.height(12.dp))
                        OutlineNeonButton("回到原片段", Icons.Filled.PlayArrow, {
                            navController.navigate(
                                "content/${source.sourceItemId}?startMs=${source.captionStartMs}&endMs=${source.captionEndMs}&autoplay=true&loop=true"
                            )
                        }, Modifier.fillMaxWidth())
                    }
                }
            }
            item { InfoBlock("搭配短语", entry?.phrases.orEmpty().ifEmpty { listOf("be ${word.normalized} about sth. 结合语境学习") }.joinToString("\n")) }
            item { InfoBlock("更多例句", entry?.examples.orEmpty().ifEmpty { listOf(word.contexts.firstOrNull()?.englishSentence.orEmpty()) }.filter { it.isNotBlank() }.joinToString("\n")) }
            item { InfoBlock("词根词缀提示", entry?.let { rootHint(it.word) } ?: rootHint(word.word)) }
            item {
                GlowCard {
                    SectionLabel("记忆笔记")
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    Spacer(Modifier.height(10.dp))
                    NeonButton("保存笔记", Icons.Filled.Save, { scope.launch { repository.updateNotes(word.normalized, notes) } }, Modifier.fillMaxWidth())
                }
            }
            item {
                ReviewPanel { rating ->
                    scope.launch { repository.reviewWord(word.normalized, rating) }
                }
            }
        }
    }
}

@Composable
private fun StudyTaskScreen(
    task: StudyTaskType,
    words: List<LearningWord>,
    contents: List<ImportedContent>,
    records: List<StudyRecord>,
    repository: XingYueRepository,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    val plan = remember(words, contents, records) { LearningPlanEngine.buildTodayPlan(words, contents, records) }
    val queue = plan.queue(task)
    val wordsByNormalized = remember(words) { words.associateBy { it.normalized } }
    val contentsById = remember(contents) { contents.associateBy { it.id } }
    val completedToday = remember(records, task) {
        records.count { it.taskType == task && it.countsAsCompleted && isToday(it.completedOrClosedAt) }
    }

    NebulaScaffold(topBar = { DetailTopBar(task.label(), navController) }) { padding ->
        LazyColumn(contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(padding)) {
            item {
                GlowCard {
                    Text(task.label(), color = TextMain, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text(task.description(), color = TextSoft)
                    Text("今日完成 $completedToday 项 · 当前队列 ${queue.size} 项", color = NeonPurple)
                }
            }
            if (queue.isEmpty()) {
                item {
                    GlowCard {
                        Text("今天这类任务已清空", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("新增素材或完成任务后会更新队列。", color = TextSoft)
                    }
                }
            }
            items(queue, key = { it.id }) { item ->
                val word = wordsByNormalized[item.wordNormalized]
                val content = contentsById[item.contentId]
                val itemStartedAt = remember(item.id) { System.currentTimeMillis() }
                val durationMs = { (System.currentTimeMillis() - itemStartedAt).coerceAtLeast(1L) }
                when (task) {
                    StudyTaskType.SPELLING -> SpellingTaskCard(item, word) { answer, correct ->
                        scope.launch {
                            repository.recordStudyTask(
                                taskType = task,
                                taskItemId = item.id,
                                contentId = item.contentId,
                                wordNormalized = item.wordNormalized,
                                result = if (correct) "correct" else "incorrect:$answer",
                                score = if (correct) 100 else 0,
                                status = if (correct) StudyAttemptStatus.COMPLETED else StudyAttemptStatus.FAILED,
                                durationMs = durationMs()
                            )
                            if (correct && item.wordNormalized.isNotBlank()) {
                                repository.reviewWord(item.wordNormalized, ReviewRating.GOOD)
                            }
                        }
                    }
                    StudyTaskType.LISTENING_REPEAT,
                    StudyTaskType.HUNDRED_LS -> ListeningTaskCard(task, item, content, navController) { durationMs ->
                        scope.launch {
                            repository.recordStudyTask(
                                taskType = task,
                                taskItemId = item.id,
                                contentId = item.contentId,
                                wordNormalized = item.wordNormalized,
                                result = if (task == StudyTaskType.HUNDRED_LS) "listened" else "repeat",
                                score = 100,
                                status = StudyAttemptStatus.COMPLETED,
                                durationMs = durationMs
                            )
                        }
                    }
                    else -> WordTaskCard(task, item, word) { rating ->
                        scope.launch {
                            if (item.wordNormalized.isNotBlank()) {
                                repository.reviewWord(item.wordNormalized, rating)
                            }
                            repository.recordStudyTask(
                                taskType = task,
                                taskItemId = item.id,
                                contentId = item.contentId,
                                wordNormalized = item.wordNormalized,
                                result = rating.name,
                                score = rating.score(),
                                status = if (rating == ReviewRating.AGAIN) StudyAttemptStatus.FAILED else StudyAttemptStatus.COMPLETED,
                                durationMs = durationMs()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordTaskCard(
    task: StudyTaskType,
    item: StudyTaskItem,
    word: LearningWord?,
    onRating: (ReviewRating) -> Unit
) {
    GlowCard {
        Text(word?.word ?: item.title, color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(word?.phonetic.orEmpty(), color = TextSoft)
        Text(word?.chineseDefinition?.ifBlank { item.subtitle } ?: item.subtitle, color = TextSoft)
        word?.contexts?.firstOrNull()?.let { context ->
            Spacer(Modifier.height(8.dp))
            Text(context.englishSentence, color = TextMain.copy(alpha = 0.86f), maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (context.chineseSentence.isNotBlank()) Text(context.chineseSentence, color = TextSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(12.dp))
        Text(if (task == StudyTaskType.MISTAKES) "按真实记忆反馈处理错词" else "选择本次记忆状态", color = TextSoft, fontSize = 13.sp)
        ReviewButtons(onRating)
    }
}

@Composable
private fun SpellingTaskCard(
    item: StudyTaskItem,
    word: LearningWord?,
    onSubmit: (String, Boolean) -> Unit
) {
    var answer by remember(item.id) { mutableStateOf("") }
    var checked by remember(item.id) { mutableStateOf<Boolean?>(null) }
    val target = item.wordNormalized
    GlowCard {
        Text("拼写测试", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(word?.chineseDefinition?.ifBlank { item.subtitle } ?: item.subtitle, color = TextSoft)
        word?.contexts?.firstOrNull()?.englishSentence?.let {
            Text(it.replace(word.word, "____", ignoreCase = true), color = TextMain.copy(alpha = 0.86f))
        }
        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it },
            label = { Text("输入英文拼写") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            NeonButton("检查", Icons.Filled.CheckCircle, {
                val ok = TextTools.normalizeWord(answer) == target
                checked = ok
                onSubmit(answer, ok)
            }, Modifier.weight(1f))
            Text(
                when (checked) {
                    true -> "正确"
                    false -> "答案：${word?.word ?: item.title}"
                    null -> ""
                },
                color = if (checked == true) NeonGreen else NeonAmber,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ListeningTaskCard(
    task: StudyTaskType,
    item: StudyTaskItem,
    content: ImportedContent?,
    navController: NavController,
    onComplete: (Long) -> Unit
) {
    val durationMs = if (task == StudyTaskType.HUNDRED_LS) 15L * 60L * 1000L else 60L * 1000L
    GlowCard {
        Text(content?.title ?: item.title, color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(item.subtitle.ifBlank { "打开素材后进行单句循环或泛听记录" }, color = TextSoft)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            NeonButton("打开素材", Icons.Filled.PlayArrow, {
                if (item.contentId.isNotBlank()) navController.navigate("content/${item.contentId}?autoplay=true")
            }, Modifier.weight(1f))
            OutlineNeonButton(
                if (task == StudyTaskType.HUNDRED_LS) "记录15分钟" else "完成跟读",
                Icons.Filled.CheckCircle,
                { onComplete(durationMs) },
                Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LookupSheet(
    context: WordSelectionContext,
    alreadySaved: Boolean,
    repository: XingYueRepository,
    ttsPlaybackManager: TtsPlaybackManager,
    onDismiss: () -> Unit,
    onOpenDetail: () -> Unit,
    onPlaySource: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val androidContext = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var entry by remember(context.normalized) { mutableStateOf<DictionaryEntry?>(null) }
    var saveStatus by remember(context.normalized) { mutableStateOf("") }
    var speechState by remember { mutableStateOf<SpeechPlaybackState>(SpeechPlaybackState.Idle) }
    val systemTts = remember { TextToSpeech(androidContext) { } }
    val density = LocalDensity.current
    var revealed by remember(context.normalized) { mutableStateOf(false) }
    val reveal by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(520, easing = ExpoEase),
        label = "lookup-book-reveal"
    )
    DisposableEffect(Unit) { onDispose { systemTts.shutdown() } }
    LaunchedEffect(context.normalized) { entry = repository.lookupWord(context.word) }
    LaunchedEffect(context.normalized) { revealed = true }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Panel,
        contentColor = TextMain,
        dragHandle = { Box(Modifier.padding(8.dp).size(64.dp, 4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f))) }
    ) {
        Box(
            Modifier
                .padding(22.dp)
                .graphicsLayer {
                    alpha = reveal
                    scaleX = 0.94f + reveal * 0.06f
                    scaleY = 0.96f + reveal * 0.04f
                    rotationY = -14f * (1f - reveal)
                    cameraDistance = 14f * density.density
                }
        ) {
            HoloMagicSheetOverlay(reveal, Modifier.matchParentSize())
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(context.word, color = TextMain, fontSize = 34.sp, fontWeight = FontWeight.Black, fontFamily = HoloTitleFont)
                        Text(entry?.phonetic.orEmpty(), color = TextSoft, fontFamily = HoloMonoFont)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, null, tint = TextSoft) }
                }
                Text(entry?.definition ?: context.chineseDefinition.ifBlank { "正在查词..." }, color = TextMain, fontSize = 18.sp)
                GlowCard {
                    SectionLabel("例句来源")
                    Text(context.englishSentence, color = TextMain)
                    if (context.chineseSentence.isNotBlank()) Text(context.chineseSentence, color = TextSoft)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    NeonButton("加入生词本", Icons.Filled.MenuBook, {
                        scope.launch {
                            if (alreadySaved) {
                                saveStatus = "已存在于生词本"
                                return@launch
                            }
                            runCatching {
                                repository.addFromSelection(
                                    context.copy(
                                        phonetic = entry?.phonetic.orEmpty(),
                                        chineseDefinition = entry?.definition ?: context.chineseDefinition
                                    )
                                )
                            }.onSuccess {
                                saveStatus = "已保存到生词本"
                            }.onFailure {
                                saveStatus = "保存失败：${it.message ?: "请重试"}"
                            }
                        }
                    }, Modifier.weight(1f))
                    OutlineNeonButton("标记掌握", Icons.Filled.Star, {
                        scope.launch { repository.markWord(context.normalized, LearningWordStatus.MASTERED) }
                    }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlineNeonButton("发音", Icons.Filled.VolumeUp, {
                        scope.launch { speak(repository, context.word, systemTts, ttsPlaybackManager) { speechState = it } }
                    }, Modifier.weight(1f))
                    OutlineNeonButton("原声", Icons.Filled.PlayArrow, onPlaySource, Modifier.weight(1f))
                    OutlineNeonButton("详情", Icons.Filled.Search, onOpenDetail, Modifier.weight(1f))
                }
                if (saveStatus.isNotBlank()) Text(saveStatus, color = if (saveStatus.startsWith("已")) NeonGreen else NeonAmber)
                SpeechStatusText(speechState)
            }
        }
    }
}

@Composable
private fun HoloMagicSheetOverlay(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (progress <= 0f) return@Canvas
        val alpha = progress.coerceIn(0f, 1f)
        val grid = 28.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = NeonBlue.copy(alpha = 0.035f * alpha),
                start = Offset(0f, y),
                end = Offset(size.width, y + size.width * 0.12f),
                strokeWidth = 0.8.dp.toPx()
            )
            y += grid
        }
        val corner = 24.dp.toPx()
        val inset = 3.dp.toPx()
        val stroke = 1.2.dp.toPx()
        val cornerColor = HoloGold.copy(alpha = 0.42f * alpha)
        listOf(
            Offset(inset, inset),
            Offset(size.width - inset, inset),
            Offset(inset, size.height - inset),
            Offset(size.width - inset, size.height - inset)
        ).forEachIndexed { index, p ->
            val sx = if (index % 2 == 0) 1f else -1f
            val sy = if (index < 2) 1f else -1f
            drawLine(cornerColor, p, Offset(p.x + corner * sx, p.y), stroke)
            drawLine(cornerColor, p, Offset(p.x, p.y + corner * sy), stroke)
        }
        val scanY = size.height * (0.1f + 0.72f * progress)
        drawLine(
            color = Color.White.copy(alpha = 0.12f * (1f - kotlin.math.abs(progress - 0.7f).coerceIn(0f, 1f))),
            start = Offset(0f, scanY),
            end = Offset(size.width, scanY + 18.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
private fun ContentMediaCard(
    content: ImportedContent,
    wordCount: Int,
    lastStudyAt: Long?,
    onOpen: () -> Unit,
    onRetry: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    GlowCard(onClick = onOpen) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            MediaPoster(content)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(content.title, color = TextMain, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(
                        content.kind.label(),
                        content.extension.uppercase(Locale.ROOT).ifBlank { "未知格式" },
                        content.status.label(),
                        "$wordCount 生词",
                        lastStudyAt?.let { "最近 ${formatAgo(it)}" }
                    ).filterNotNull().joinToString(" · "),
                    color = TextSoft,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                LinearProgressIndicator(
                    progress = { content.progress.coerceIn(0, 100) / 100f },
                    color = if (content.status == ImportProcessingStatus.FAILED) NeonRed else NeonPurple,
                    trackColor = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape)
                )
                Text(content.statusMessage, color = statusColor(content.status), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlineNeonButton("继续学习", Icons.Filled.PlayArrow, onOpen, Modifier.weight(1f))
                    IconButton(onClick = onFavorite) { Icon(if (content.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null, tint = NeonPurple) }
                    IconButton(onClick = onRetry) { Icon(Icons.Filled.Refresh, null, tint = TextSoft) }
                    IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, null, tint = NeonRed) }
                }
            }
        }
    }
}

@Composable
private fun CompactContentRow(content: ImportedContent, navController: NavController) {
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { navController.navigate("content/${content.id}") }
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MediaPoster(content, small = true)
            Column(Modifier.weight(1f)) {
                Text(content.title, color = TextMain, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${content.kind.label()} · ${content.status.label()}", color = TextSoft)
            }
            LinearProgressIndicator(progress = { content.progress / 100f }, modifier = Modifier.width(68.dp).height(4.dp).clip(CircleShape), color = NeonPurple, trackColor = Color.White.copy(alpha = 0.12f))
            Icon(Icons.Filled.MoreVert, null, tint = TextSoft)
        }
    }
}

@Composable
private fun MoonHeroHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
    metric: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    scene: AnimeSceneKind = AnimeSceneKind.MoonLibrary,
    sceneLabel: String = "学习"
) {
    Surface(
        color = Color.White.copy(alpha = 0.82f),
        shape = RoundedCornerShape(30.dp),
        border = BorderStroke(1.dp, HoloStroke.copy(alpha = 0.95f)),
        shadowElevation = 11.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        BoxWithConstraints {
            val compact = maxWidth < 390.dp
            Canvas(Modifier.matchParentSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        listOf(SakuraPink.copy(alpha = 0.18f), NeonPurple.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width * 0.82f, size.height * 0.24f),
                        radius = size.minDimension * 0.90f
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        listOf(NeonGreen.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width * 0.10f, size.height * 0.92f),
                        radius = size.minDimension * 0.58f
                    )
                )
                repeat(4) { index ->
                    drawCircle(
                        color = if (index % 2 == 0) SakuraPink.copy(alpha = 0.12f) else NeonBlue.copy(alpha = 0.10f),
                        center = Offset(size.width * 0.84f, size.height * 0.34f),
                        radius = (34.dp + 16.dp * index.toFloat()).toPx(),
                        style = Stroke(1.dp.toPx())
                    )
                }
                drawCircle(HoloGold.copy(alpha = 0.16f), center = Offset(size.width * 0.13f, size.height * 0.18f), radius = 14.dp.toPx())
                drawCircle(Color.White.copy(alpha = 0.74f), center = Offset(size.width * 0.145f, size.height * 0.15f), radius = 13.dp.toPx())
                drawLine(NeonBlue.copy(alpha = 0.18f), Offset(size.width * 0.08f, size.height * 0.78f), Offset(size.width * 0.92f, size.height * 0.26f), 1.dp.toPx())
                drawDiamond(Offset(size.width * 0.91f, size.height * 0.76f), 3.dp.toPx(), HoloGold.copy(alpha = 0.36f))
            }
            if (compact) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AnimeSceneFrame(
                        scene = scene,
                        modifier = Modifier.fillMaxWidth().height(210.dp),
                        label = sceneLabel
                    )
                    HeroCopy(eyebrow, title, subtitle, metric, icon)
                }
            } else {
                Row(
                    Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        HeroCopy(eyebrow, title, subtitle, metric, icon)
                    }
                    AnimeSceneFrame(
                        scene = scene,
                        modifier = Modifier.width(136.dp).height(176.dp),
                        label = sceneLabel
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCopy(
    eyebrow: String,
    title: String,
    subtitle: String,
    metric: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Text(eyebrow, color = NeonBlue, fontFamily = HoloMonoFont, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    Text(
        title,
        color = TextMain,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Black,
        fontFamily = HoloTitleFont,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
    Text(subtitle, color = TextSoft, fontSize = 15.sp, lineHeight = 22.sp)
    GlowPill(metric, icon)
}

@Composable
private fun AnimeStoryboardStrip() {
    GlowCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("星月陪练台", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = HoloTitleFont)
                Text("听力、默写和复习保持同一进度。", color = TextSoft, fontSize = 13.sp)
            }
            GlowPill("专注模式", Icons.Filled.Star)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AnimeSceneFrame(AnimeSceneKind.MoonLibrary, Modifier.weight(1f).height(126.dp), "词库")
            AnimeSceneFrame(AnimeSceneKind.TrainWindow, Modifier.weight(1f).height(126.dp), "听读")
            AnimeSceneFrame(AnimeSceneKind.VoiceStudio, Modifier.weight(1f).height(126.dp), "练习")
        }
    }
}

@Composable
private fun AnimeSceneFrame(
    scene: AnimeSceneKind,
    modifier: Modifier = Modifier,
    label: String = ""
) {
    Surface(
        color = Color.White.copy(alpha = 0.54f),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, HoloStroke.copy(alpha = 0.90f)),
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Box(Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(scene.drawableRes()),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Canvas(Modifier.matchParentSize()) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.02f),
                            Color.White.copy(alpha = 0.46f)
                        )
                    ),
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )
                drawLine(NeonBlue.copy(alpha = 0.22f), Offset(0f, size.height * 0.68f), Offset(size.width, size.height * 0.38f), 1.dp.toPx())
                drawDiamond(Offset(size.width * 0.86f, size.height * 0.18f), 2.5.dp.toPx(), HoloGold.copy(alpha = 0.55f))
                if (label.isNotBlank()) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.66f),
                        topLeft = Offset(size.width * 0.10f, size.height * 0.76f),
                        size = Size(size.width * 0.58f, 18.dp.toPx()),
                        cornerRadius = CornerRadius(9.dp.toPx(), 9.dp.toPx())
                    )
                }
            }
            if (label.isNotBlank()) {
                Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.BottomStart) {
                    Text(label, color = TextMain.copy(alpha = 0.68f), fontSize = 9.sp, fontFamily = HoloMonoFont, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HoloImportCommandDeck(
    onLink: () -> Unit,
    onVideo: () -> Unit,
    onAudio: () -> Unit,
    onSubtitle: () -> Unit,
    onDocument: () -> Unit
) {
    Surface(
        color = Panel.copy(alpha = 0.95f),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, HoloStroke.copy(alpha = 0.95f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        listOf(NeonPurple.copy(alpha = 0.16f), NeonBlue.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.86f, size.height * 0.18f),
                        radius = size.minDimension * 0.76f
                    )
                )
                drawLine(NeonBlue.copy(alpha = 0.22f), Offset(size.width * 0.04f, size.height * 0.18f), Offset(size.width * 0.86f, size.height * 0.18f), 1.dp.toPx())
                drawLine(NeonPurple.copy(alpha = 0.16f), Offset(size.width * 0.18f, 0f), Offset(size.width * 0.92f, size.height), 1.dp.toPx())
                repeat(3) { ring ->
                    drawCircle(
                        NeonBlue.copy(alpha = 0.08f - ring * 0.015f),
                        center = Offset(size.width * 0.86f, size.height * 0.24f),
                        radius = size.minDimension * (0.28f + ring * 0.11f),
                        style = Stroke(1.dp.toPx())
                    )
                }
                drawDiamond(Offset(size.width * 0.92f, size.height * 0.16f), 3.dp.toPx(), HoloGold.copy(alpha = 0.36f))
                drawDiamond(Offset(size.width * 0.08f, size.height * 0.82f), 2.5.dp.toPx(), NeonBlue.copy(alpha = 0.28f))
            }
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("素材入口", color = NeonBlue, fontFamily = HoloMonoFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("导入链接", color = TextMain, fontFamily = HoloTitleFont, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        Text("视频平台链接从这里进入", color = TextSoft, fontSize = 13.sp)
                    }
                    Icon(Icons.Filled.Link, null, tint = NeonPurple, modifier = Modifier.size(34.dp))
                }
                HoloPressSurface(
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    color = NeonPurple.copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                    shape = RoundedCornerShape(20.dp),
                    onClick = onLink
                ) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Filled.Link, null, tint = Color.White)
                        Spacer(Modifier.width(10.dp))
                        Text("粘贴平台链接 / 媒体直链", color = Color.White, fontFamily = HoloTitleFont, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MiniImportChip("视频", Icons.Filled.VideoFile, onVideo, Modifier.weight(1f))
                    MiniImportChip("音频", Icons.Filled.Headphones, onAudio, Modifier.weight(1f))
                    MiniImportChip("字幕", Icons.Filled.Subtitles, onSubtitle, Modifier.weight(1f))
                    MiniImportChip("文档", Icons.Filled.UploadFile, onDocument, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HomeEmptyLearningCard() {
    GlowCard {
        Text("等待第一条双语时间线", color = TextMain, fontSize = 23.sp, fontWeight = FontWeight.Black, fontFamily = HoloTitleFont)
        Text("导入链接、字幕、文档或英文音频后开始学习。", color = TextSoft)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HoloTag("无素材", TextSoft)
            HoloTag("字幕 0", NeonAmber)
            HoloTag("等待导入", NeonBlue)
        }
    }
}

@Composable
private fun ContinueCard(content: ImportedContent, wordCount: Int, navController: NavController) {
    val openContent = { navController.navigate("content/${content.id}") }
    GlowCard(onClick = openContent) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        HoloTag(content.status.label(), statusColor(content.status))
                        HoloTag(sourceSignal(content), NeonBlue)
                    }
                    Text(content.title, color = TextMain, fontSize = 25.sp, fontWeight = FontWeight.Black, fontFamily = HoloTitleFont, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(content.statusMessage, color = statusColor(content.status), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                MediaPoster(content)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HoloMetric("字幕", if (content.captionId.isNotBlank()) "已绑定" else "未生成", if (content.captionId.isNotBlank()) NeonGreen else NeonAmber, Modifier.weight(1f))
                HoloMetric("生词", "$wordCount", NeonPurple, Modifier.weight(1f))
                HoloMetric("时长", if (content.durationMs > 0L) formatMs(content.durationMs) else "--:--", TextSoft, Modifier.weight(1f))
            }
            LinearProgressIndicator(
                progress = { content.progress.coerceIn(0, 100) / 100f },
                color = statusColor(content.status),
                trackColor = Color.White.copy(alpha = 0.10f),
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape)
            )
            NeonButton("继续播放", Icons.Filled.PlayArrow, openContent, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ProcessingPanel(content: ImportedContent?) {
    if (content == null) return
    GlowCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("处理中", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("${content.progress}%", color = NeonPurple, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(progress = { content.progress / 100f }, color = NeonPurple, trackColor = Color.White.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            StepDot("识别字幕", content.progress >= 30)
            StepDot("生成双语", content.progress >= 70)
            StepDot("可点选词", content.progress >= 90)
        }
        Text(content.statusMessage, color = TextSoft, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun CurrentCaptionHud(cue: CaptionCue?, currentIndex: Int, total: Int, content: ImportedContent) {
    Surface(
        color = Panel.copy(alpha = 0.78f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        HoloTag("NOW", NeonBlue)
                        HoloTag(sourceSignal(content), TextSoft)
                    }
                    CyberNumberText("${currentIndex + 1}/${total.coerceAtLeast(1)}", active = true)
                }
                Text(cue?.english ?: "等待字幕定位", color = TextMain, fontFamily = HoloTitleFont, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!cue?.chinese.isNullOrBlank()) {
                    Text(cue?.chinese.orEmpty(), color = TextSoft, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Canvas(Modifier.matchParentSize()) {
                drawLine(NeonBlue.copy(alpha = 0.18f), Offset(0f, size.height - 1.dp.toPx()), Offset(size.width * ((currentIndex + 1f) / total.coerceAtLeast(1)).coerceIn(0f, 1f), size.height - 1.dp.toPx()), 1.dp.toPx())
                drawCircle(NeonPurple.copy(alpha = 0.05f), center = Offset(size.width * 0.92f, size.height * 0.26f), radius = size.minDimension * 0.55f, style = Stroke(1.dp.toPx()))
            }
        }
    }
}

@Composable
private fun SubtitleUnavailablePanel(
    content: ImportedContent,
    onRetry: () -> Unit,
    onConfigureKey: () -> Unit,
    onReimport: () -> Unit
) {
    GlowCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("暂无字幕时间线", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = HoloTitleFont)
                    Text("当前素材还没有可显示的双语字幕。", color = TextSoft)
                }
                HoloTag(content.status.label(), statusColor(content.status))
            }
            Surface(
                color = Color.White.copy(alpha = 0.58f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, HoloStroke.copy(alpha = 0.82f)),
                modifier = Modifier.fillMaxWidth().height(150.dp)
            ) {
                Box {
                    Image(
                        painter = painterResource(R.drawable.xy_mascot_empty),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Text(
                        "SUBTITLE GATE",
                        color = TextMain.copy(alpha = 0.68f),
                        fontFamily = HoloMonoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HoloMetric("来源", sourceSignal(content), NeonBlue, Modifier.weight(1f))
                HoloMetric("格式", content.extension.uppercase(Locale.ROOT).ifBlank { content.kind.label() }, TextSoft, Modifier.weight(1f))
                HoloMetric("字幕", if (content.captionId.isBlank()) "0" else "待加载", NeonAmber, Modifier.weight(1f))
            }
            Text(
                subtitleEmptySuggestion(content),
                color = TextMain.copy(alpha = 0.88f),
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
            Text(content.statusMessage.ifBlank { "还没有字幕处理结果。" }, color = statusColor(content.status), maxLines = 3, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlineNeonButton("重试处理", Icons.Filled.Refresh, onRetry, Modifier.weight(1f))
                OutlineNeonButton("配置转写", Icons.Filled.Settings, onConfigureKey, Modifier.weight(1f))
            }
            NeonButton("重新导入本地文件", Icons.Filled.UploadFile, onReimport, Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun CaptionLine(
    cue: CaptionCue,
    active: Boolean,
    sourceType: LearningSourceType,
    content: ImportedContent,
    onSeek: () -> Unit,
    onReplay: () -> Unit,
    onSpeakEnglish: () -> Unit,
    onSpeakChinese: () -> Unit,
    onSpeakBilingual: () -> Unit,
    onWord: (String) -> Unit,
    onLongWord: (String) -> Unit
) {
    val activeProgress by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(400, easing = HoloEase),
        label = "caption-active"
    )
    val background = if (active) {
        Brush.horizontalGradient(listOf(NeonPurple.copy(alpha = 0.16f + activeProgress * 0.08f), NeonBlue.copy(alpha = 0.08f + activeProgress * 0.08f)))
    } else {
        Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.04f), Color.White.copy(alpha = 0.02f)))
    }
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, if (active) StrokePurple.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth().background(background, RoundedCornerShape(22.dp)).clickable { onSeek() }
    ) {
        Box {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CyberNumberText(formatMs(cue.startMs), active = active)
                    Text(formatMs(cue.endMs), color = TextSoft.copy(alpha = 0.72f), fontFamily = HoloMonoFont, fontSize = 11.sp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        cue.tokens.forEach { token ->
                            HoloWordToken(
                                token = token,
                                active = active,
                                onWord = onWord,
                                onLongWord = onLongWord
                            )
                        }
                    }
                    if (cue.chinese.isNotBlank()) {
                        HoloChineseCaption(cue.chinese, activeProgress)
                    }
                    if (sourceType == LearningSourceType.DOCUMENT) Text(content.title, color = TextSoft, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                CaptionActionRail(
                    active = active,
                    onReplay = onReplay,
                    onSpeakEnglish = onSpeakEnglish,
                    onSpeakChinese = onSpeakChinese,
                    onSpeakBilingual = onSpeakBilingual
                )
            }
            HoloCaptionScan(activeProgress, Modifier.matchParentSize())
        }
    }
}

@Composable
private fun MediaPanel(
    player: ExoPlayer,
    content: ImportedContent,
    position: Long,
    currentCue: CaptionCue?,
    abState: AbLoopState
) {
    var started by remember(content.id) { mutableStateOf(false) }
    val intro by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(800, easing = ExpoEase),
        label = "media-intro"
    )
    LaunchedEffect(content.id) { started = true }
    val durationMs = when {
        content.durationMs > 0L -> content.durationMs
        player.duration > 0L -> player.duration
        else -> 0L
    }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, NeonBlue.copy(alpha = 0.34f), RoundedCornerShape(24.dp))
    ) {
        if (content.kind == SourceType.AUDIO) {
            Image(
                painter = painterResource(R.drawable.xy_scene_player),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.30f)))
        } else {
            AndroidView(factory = { PlayerView(it).apply { this.player = player; useController = false } }, modifier = Modifier.fillMaxSize())
        }
        if (intro < 0.98f) HoloMediaLoadOverlay(intro)
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.76f))
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            currentCue?.let { cue ->
                Text(cue.english, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (cue.chinese.isNotBlank()) Text(cue.chinese, color = Color.White.copy(alpha = 0.82f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            PlaybackProgressBar(position = position, durationMs = durationMs, abState = abState)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                CyberNumberText(formatMs(position), active = true)
                Text(content.title, color = Color.White.copy(alpha = 0.92f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                CyberNumberText(formatMs(durationMs), active = false)
            }
        }
    }
}

@Composable
private fun ReaderHero(content: ImportedContent, current: Int, total: Int) {
    GlowCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Filled.Book, null, tint = NeonPurple, modifier = Modifier.size(54.dp))
            Column(Modifier.weight(1f)) {
                Text(content.title, color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("阅读位置 ${current + 1} / ${total.coerceAtLeast(1)}", color = TextSoft)
                LinearProgressIndicator(progress = { if (total == 0) 0f else (current + 1).toFloat() / total }, color = NeonPurple, trackColor = Color.White.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape))
            }
        }
    }
}

@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    speed: Float,
    abState: AbLoopState,
    replayEnabled: Boolean,
    onPlayPause: () -> Unit,
    onSpeed: () -> Unit,
    onReplayCue: () -> Unit,
    onAb: () -> Unit
) {
    Surface(
        color = Panel.copy(alpha = 0.78f),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, HoloStroke.copy(alpha = 0.92f)),
        modifier = Modifier.fillMaxWidth().height(84.dp)
    ) {
        Box {
            Canvas(Modifier.fillMaxSize()) {
                drawLine(NeonBlue.copy(alpha = 0.16f), Offset(size.width * 0.08f, size.height * 0.18f), Offset(size.width * 0.92f, size.height * 0.18f), 1.dp.toPx())
                drawLine(NeonPurple.copy(alpha = 0.10f), Offset(size.width * 0.12f, size.height), Offset(size.width * 0.58f, 0f), 1.dp.toPx())
            }
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
            ) {
                ControlButton("${"%.2f".format(speed)}x", "SPD", Icons.Filled.Audiotrack, onSpeed)
                ControlButton("单句", if (replayEnabled) "REPLAY" else "等待字幕", Icons.Filled.Replay, onReplayCue)
                HoloPressSurface(color = NeonPurple, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)), shape = CircleShape, modifier = Modifier.size(56.dp), onClick = onPlayPause) {
                    Box(contentAlignment = Alignment.Center) { Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                }
                ControlButton(abButtonText(abState), abSubtitle(abState), if (abState.phase == AbLoopPhase.ACTIVE) Icons.Filled.Close else Icons.Filled.Subtitles, onAb)
            }
        }
    }
}

@Composable
private fun PlaybackProgressBar(position: Long, durationMs: Long, abState: AbLoopState) {
    val progress = if (durationMs <= 0L) 0f else (position.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    val a = if (durationMs > 0L && abState.startMs >= 0L) (abState.startMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else null
    val b = if (durationMs > 0L && abState.endMs > abState.startMs) (abState.endMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else null
    Canvas(Modifier.fillMaxWidth().height(16.dp)) {
        val barHeight = 5.dp.toPx()
        val top = (size.height - barHeight) / 2f
        drawRoundRect(Color.White.copy(alpha = 0.18f), topLeft = Offset(0f, top), size = Size(size.width, barHeight), cornerRadius = CornerRadius(99f, 99f))
        drawRoundRect(NeonBlue.copy(alpha = 0.92f), topLeft = Offset(0f, top), size = Size(size.width * progress, barHeight), cornerRadius = CornerRadius(99f, 99f))
        a?.let { marker ->
            drawCircle(NeonGreen, radius = 5.dp.toPx(), center = Offset(size.width * marker, size.height / 2f))
        }
        b?.let { marker ->
            drawCircle(NeonRed, radius = 5.dp.toPx(), center = Offset(size.width * marker, size.height / 2f))
        }
    }
}

@Composable
private fun PlayerLoopStatus(abState: AbLoopState, segment: PlaybackSegment?, notice: String) {
    if (abState.phase == AbLoopPhase.EMPTY && segment == null && notice.isBlank()) return
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        segment?.let { HoloTag(it.label, if (it.repeat) NeonGreen else NeonBlue) }
        when (abState.phase) {
            AbLoopPhase.EMPTY -> Unit
            AbLoopPhase.WAITING_FOR_B -> HoloTag("A 点 ${formatMs(abState.startMs)}", NeonAmber)
            AbLoopPhase.ACTIVE -> {
                HoloTag("A ${formatMs(abState.startMs)}", NeonGreen)
                HoloTag("B ${formatMs(abState.endMs)}", NeonRed)
            }
        }
        if (notice.isNotBlank()) {
            Text(notice, color = TextSoft, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
    }
}

private fun abButtonText(state: AbLoopState): String =
    when (state.phase) {
        AbLoopPhase.EMPTY -> "设 A"
        AbLoopPhase.WAITING_FOR_B -> "设 B"
        AbLoopPhase.ACTIVE -> "清除"
    }

private fun abSubtitle(state: AbLoopState): String =
    when (state.phase) {
        AbLoopPhase.EMPTY -> "AB 循环"
        AbLoopPhase.WAITING_FOR_B -> formatMs(state.startMs)
        AbLoopPhase.ACTIVE -> "循环中"
    }

@Composable
private fun WordCard(word: LearningWord, index: Int, onOpen: () -> Unit) {
    var visible by remember(word.normalized) { mutableStateOf(false) }
    LaunchedEffect(word.normalized) {
        delay((index.coerceAtMost(8) * 30L))
        visible = true
    }
    val enter by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(280, easing = HoloEase),
        label = "word-card-enter"
    )
    GlowCard(
        modifier = Modifier.graphicsLayer {
            alpha = enter
            translationY = (1f - enter) * 8.dp.toPx()
            rotationX = (1f - enter) * 3f
        },
        onClick = onOpen
    ) {
        Box {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(statusColor(word.status).copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MenuBook, null, tint = statusColor(word.status))
                }
                Column(Modifier.weight(1f)) {
                    Text(word.word, color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = HoloTitleFont, letterSpacing = 0.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(word.phonetic, color = TextSoft)
                    Text(word.chineseDefinition, color = TextMain.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("来源：${word.contexts.firstOrNull()?.sourceTitle ?: "离线词典"}", color = NeonPurple, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(word.status.label(), color = statusColor(word.status), fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(5) { index ->
                            Box(Modifier.size(10.dp).clip(CircleShape).background(if (index < word.masteryDots()) statusColor(word.status) else Color.White.copy(alpha = 0.12f)))
                        }
                    }
                }
            }
            HoloVocabularyCardSheen(enter, statusColor(word.status), Modifier.matchParentSize())
        }
    }
}

@Composable
private fun HoloVocabularyCardSheen(progress: Float, color: Color, modifier: Modifier = Modifier) {
    val sweep = if (progress > 0.98f && DefaultHoloMotionLevel == HoloMotionLevel.Standard) {
        val transition = rememberInfiniteTransition(label = "vocab-card-sheen")
        val phase by transition.animateFloat(
            initialValue = -0.35f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
            label = "vocab-card-sheen-phase"
        )
        phase
    } else {
        -1f
    }
    Canvas(modifier) {
        if (sweep >= -0.3f) {
            val x = size.width * sweep
            drawLine(
                color = NeonBlue.copy(alpha = 0.16f),
                start = Offset(x - 22.dp.toPx(), 0f),
                end = Offset(x + 18.dp.toPx(), size.height),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        drawLine(
            color = color.copy(alpha = 0.16f * progress),
            start = Offset(0f, size.height - 1.dp.toPx()),
            end = Offset(size.width * 0.42f, size.height - 1.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
private fun ReviewPanel(onRating: (ReviewRating) -> Unit) {
    GlowCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("智能复习", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("选择越准确，记忆安排越合理", color = TextSoft)
            }
            Text("下次复习自动计算", color = TextSoft)
        }
        Spacer(Modifier.height(14.dp))
        ReviewButtons(onRating)
    }
}

@Composable
private fun ReviewButtons(onRating: (ReviewRating) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        ReviewButton("不会", "今日回流", NeonRed, ReviewRating.AGAIN, onRating, Modifier.weight(1f))
        ReviewButton("模糊", "短间隔", NeonAmber, ReviewRating.HARD, onRating, Modifier.weight(1f))
        ReviewButton("认识", "正常复习", NeonGreen, ReviewRating.GOOD, onRating, Modifier.weight(1f))
        ReviewButton("太简单", "延后复习", NeonPurple, ReviewRating.EASY, onRating, Modifier.weight(1f))
    }
}

@Composable
private fun ReviewButton(label: String, detail: String, color: Color, rating: ReviewRating, onRating: (ReviewRating) -> Unit, modifier: Modifier = Modifier) {
    Surface(color = color.copy(alpha = 0.12f), border = BorderStroke(1.dp, color.copy(alpha = 0.75f)), shape = RoundedCornerShape(14.dp), modifier = modifier.clickable { onRating(rating) }) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = color, fontWeight = FontWeight.Black)
            Text(detail, color = TextSoft, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun NebulaScaffold(
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = Night,
        topBar = { topBar?.invoke() },
        bottomBar = bottomBar,
        snackbarHost = snackbarHost
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White, Color(0xFFFFF8FB), PorcelainBlue, SilkLavender, Night)
                    )
                )
        ) {
            NebulaBackground()
            content(padding)
        }
    }
}

@Composable
private fun NebulaBackground() {
    val transition = rememberInfiniteTransition(label = "nebula")
    val pulse by transition.animateFloat(0.04f, 0.12f, infiniteRepeatable(tween(4200), RepeatMode.Reverse), label = "pulse")
    val drift by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "drift")
    val reduced = DefaultHoloMotionLevel == HoloMotionLevel.Reduced
    val particles = remember {
        List(if (reduced) 10 else 24) { index ->
            val seed = index + 1
            HoloParticle(
                x = ((seed * 37) % 100) / 100f,
                y = ((seed * 61) % 100) / 100f,
                drift = 0.12f + ((seed * 17) % 24) / 100f,
                size = 1.4f + ((seed * 11) % 18) / 10f,
                color = when (index % 7) {
                    0 -> HoloGold
                    1 -> SakuraPink
                    2 -> NeonBlue
                    3 -> NeonGreen
                    else -> NeonPurple
                },
                phase = ((seed * 13) % 100) / 100f
            )
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                listOf(SakuraPink.copy(alpha = 0.14f), NeonPurple.copy(alpha = 0.07f), Color.Transparent),
                center = Offset(size.width * 0.82f, size.height * 0.06f),
                radius = size.minDimension * 0.72f
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                listOf(HoloGold.copy(alpha = 0.10f), NeonGreen.copy(alpha = 0.05f), Color.Transparent),
                center = Offset(size.width * 0.08f, size.height * 0.94f),
                radius = size.minDimension * 0.62f
            )
        )
        val step = 72.dp.toPx()
        var x = -size.height
        while (x < size.width) {
            drawLine(
                color = NeonPurple.copy(alpha = pulse * 0.30f),
                start = Offset(x, 0f),
                end = Offset(x + size.height, size.height),
                strokeWidth = 1.dp.toPx()
            )
            x += step
        }
        repeat(4) { ring ->
            drawCircle(
                color = NeonBlue.copy(alpha = 0.045f - ring * 0.006f),
                center = Offset(size.width * 0.84f, size.height * 0.12f),
                radius = (64.dp + 26.dp * ring.toFloat()).toPx(),
                style = Stroke(1.dp.toPx())
            )
        }
        particles.forEachIndexed { index, particle ->
            val p = (drift + particle.phase) % 1f
            val px = (particle.x * size.width + sin((p * PI * 2).toDouble()).toFloat() * particle.drift * 36.dp.toPx()).floorMod(size.width)
            val py = (particle.y * size.height - p * particle.drift * 48.dp.toPx()).floorMod(size.height)
            val alpha = 0.08f + 0.13f * ((sin((p * PI * 2).toDouble()).toFloat() + 1f) / 2f)
            if (index % 5 == 0) {
                drawHexagon(Offset(px, py), particle.size.dp.toPx(), particle.color.copy(alpha = alpha))
            } else {
                drawDiamond(Offset(px, py), particle.size.dp.toPx(), particle.color.copy(alpha = alpha))
            }
        }
        if (!reduced && drift > 0.42f && drift < 0.50f) {
            val group = particles.take(4).map {
                Offset(it.x * size.width, it.y * size.height)
            }
            group.zipWithNext().forEach { (a, b) ->
                drawLine(NeonBlue.copy(alpha = 0.13f), a, b, 1.dp.toPx())
            }
        }
    }
}

@Composable
private fun GlowCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "card-press"
    )
    val clickable = if (onClick != null) {
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interaction, indication = null) { onClick() }
    } else {
        modifier
    }
    Surface(
        color = Panel.copy(alpha = 0.94f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, HoloStroke.copy(alpha = 0.86f)),
        shadowElevation = 7.dp,
        modifier = clickable.fillMaxWidth()
    ) {
        Box {
            Column(
                Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.82f), SilkLavender.copy(alpha = 0.36f), PorcelainBlue.copy(alpha = 0.28f), Color.White.copy(alpha = 0.46f))
                        )
                    )
                    .padding(18.dp),
                content = content
            )
            HoloSurfaceOverlay(pressed = pressed, modifier = Modifier.matchParentSize())
        }
    }
}

@Composable
private fun NeonButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    HoloPressSurface(
        modifier = modifier.height(52.dp),
        color = NeonPurple,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        onClick = onClick
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            Icon(icon, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(text, color = Color.White, fontFamily = HoloTitleFont, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun OutlineNeonButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    HoloPressSurface(
        modifier = modifier.height(52.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.45f)),
        onClick = onClick
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            Icon(icon, null, tint = TextMain)
            Spacer(Modifier.width(8.dp))
            Text(text, color = TextMain, fontFamily = HoloTitleFont, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HoloPressSurface(
    modifier: Modifier = Modifier,
    color: Color,
    border: BorderStroke,
    shape: Shape = RoundedCornerShape(18.dp),
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 650f),
        label = "holo-press-scale"
    )
    val wave by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(if (pressed) 80 else 200, easing = ExpoEase),
        label = "holo-press-wave"
    )
    Surface(
        color = color,
        border = border,
        shape = shape,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            content()
            Canvas(Modifier.fillMaxSize()) {
                if (wave > 0f) {
                    val radius = size.maxDimension * wave
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonBlue.copy(alpha = 0.24f * (1f - wave)), NeonPurple.copy(alpha = 0.08f * (1f - wave)), Color.Transparent),
                            center = center,
                            radius = radius.coerceAtLeast(1f)
                        ),
                        radius = radius,
                        center = center
                    )
                }
                if (!pressed && wave == 0f) {
                    drawCircle(NeonBlue.copy(alpha = 0.08f), radius = 1.5.dp.toPx(), center = Offset(size.width * 0.75f, size.height * 0.25f))
                }
            }
        }
    }
}

@Composable
private fun HoloSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(360, easing = ExpoEase),
        label = "holo-switch"
    )
    HoloPressSurface(
        modifier = Modifier.size(66.dp, 36.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, if (checked) NeonPurple.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.16f)),
        shape = CircleShape,
        onClick = { onCheckedChange(!checked) }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val pad = 6.dp.toPx()
            val radius = 12.dp.toPx()
            val cx = pad + radius + (size.width - (pad + radius) * 2f) * progress
            val cy = size.height / 2f
            drawLine(
                color = NeonPurple.copy(alpha = 0.18f + 0.28f * progress),
                start = Offset(pad + radius, cy),
                end = Offset(cx, cy),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            repeat(6) { index ->
                val t = index / 5f
                if (t < progress) {
                    drawTriangle(
                        center = Offset(
                            pad + radius + (cx - pad - radius) * t,
                            cy + sin(((progress + t) * PI).toDouble()).toFloat() * 8.dp.toPx()
                        ),
                        radius = 1.8.dp.toPx(),
                        color = NeonBlue.copy(alpha = 0.18f * (1f - t))
                    )
                }
            }
            rotateCrystal(cx, cy, progress)
        }
    }
}

private fun DrawScope.drawTriangle(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius * 0.86f, center.y + radius * 0.5f)
        lineTo(center.x - radius * 0.86f, center.y + radius * 0.5f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.rotateCrystal(cx: Float, cy: Float, progress: Float) {
    val radius = 12.dp.toPx()
    val phase = progress * PI.toFloat()
    val skew = cos(phase.toDouble()).toFloat() * 3.dp.toPx()
    val crystal = Path().apply {
        moveTo(cx, cy - radius)
        lineTo(cx + radius * 0.9f + skew, cy)
        lineTo(cx, cy + radius)
        lineTo(cx - radius * 0.9f + skew, cy)
        close()
    }
    drawPath(crystal, NeonPurple.copy(alpha = 0.78f))
    drawPath(crystal, Color.White.copy(alpha = 0.14f), style = Stroke(width = 1.dp.toPx()))
    drawCircle(NeonBlue.copy(alpha = 0.7f), radius = 3.dp.toPx(), center = Offset(cx, cy))
}

@Composable
private fun HoloSurfaceOverlay(pressed: Boolean, modifier: Modifier = Modifier) {
    val glow by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(if (pressed) 80 else 220, easing = ExpoEase),
        label = "surface-energy"
    )
    Canvas(modifier) {
        val corner = 18.dp.toPx()
        val inset = 9.dp.toPx()
        val cornerColor = NeonPurple.copy(alpha = if (pressed) 0.26f else 0.13f)
        listOf(
            Offset(inset, inset),
            Offset(size.width - inset, inset),
            Offset(inset, size.height - inset),
            Offset(size.width - inset, size.height - inset)
        ).forEachIndexed { index, point ->
            val sx = if (index % 2 == 0) 1f else -1f
            val sy = if (index < 2) 1f else -1f
            drawLine(cornerColor, point, Offset(point.x + corner * sx, point.y), 1.dp.toPx())
            drawLine(cornerColor, point, Offset(point.x, point.y + corner * sy), 1.dp.toPx())
        }
        if (glow > 0f) {
            drawRect(
                brush = Brush.radialGradient(
                    listOf(NeonBlue.copy(alpha = 0.18f * glow), NeonPurple.copy(alpha = 0.08f * glow), Color.Transparent),
                    center = center,
                    radius = size.maxDimension * 0.8f
                )
            )
            drawLine(
                color = HoloGold.copy(alpha = 0.22f * glow),
                start = Offset(size.width * 0.12f, size.height * 0.08f),
                end = Offset(size.width * 0.88f, size.height * 0.92f),
                strokeWidth = 1.dp.toPx()
            )
        }
        drawLine(
            color = NeonBlue.copy(alpha = 0.07f),
            start = Offset(size.width * 0.08f, 0f),
            end = Offset(size.width * 0.72f, size.height),
            strokeWidth = 1.dp.toPx()
        )
        drawDiamond(Offset(size.width - 18.dp.toPx(), 18.dp.toPx()), 2.dp.toPx(), HoloGold.copy(alpha = 0.26f))
    }
}

@Composable
private fun CyberNumberText(text: String, active: Boolean, modifier: Modifier = Modifier) {
    val sparkPhase = if (active && DefaultHoloMotionLevel == HoloMotionLevel.Standard) {
        val transition = rememberInfiniteTransition(label = "cyber-number-sparks")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1260, easing = LinearEasing), RepeatMode.Restart),
            label = "cyber-number-spark-phase"
        )
        phase
    } else {
        0f
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                (fadeIn(tween(180, easing = ExpoEase)) + slideInVertically { it / 2 }) togetherWith
                    (fadeOut(tween(150, easing = HoloEase)) + slideOutVertically { -it / 2 })
            },
            label = "cyber-number"
        ) { value ->
            Text(
                value,
                color = if (active) NeonPurple else TextSoft,
                fontFamily = HoloMonoFont,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                style = TextStyle(
                    shadow = if (active) Shadow(NeonPurple.copy(alpha = 0.45f), Offset.Zero, 5f) else Shadow(Color.Transparent, Offset.Zero, 0f)
                )
            )
        }
        Canvas(Modifier.matchParentSize()) {
            if (active) {
                drawLine(
                    color = NeonBlue.copy(alpha = 0.34f),
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 1.dp.toPx()
                )
                val split = size.height / 2f
                drawLine(
                    color = HoloGold.copy(alpha = 0.16f),
                    start = Offset(size.width * 0.18f, split - 3.dp.toPx()),
                    end = Offset(size.width * 0.82f, split + 3.dp.toPx()),
                    strokeWidth = 0.8.dp.toPx()
                )
                repeat(6) { index ->
                    val local = ((sparkPhase + index * 0.17f) % 1f)
                    val direction = if (index % 2 == 0) -1f else 1f
                    drawDiamond(
                        center = Offset(
                            size.width * (0.18f + local * 0.64f),
                            split + direction * local * 8.dp.toPx()
                        ),
                        radius = 1.0.dp.toPx(),
                        color = NeonBlue.copy(alpha = 0.18f * (1f - local))
                    )
                }
            }
        }
    }
}

@Composable
private fun HoloCaptionScan(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (progress <= 0f) return@Canvas
        val x = size.width * progress
        drawLine(
            color = Color.White.copy(alpha = 0.48f * (1f - kotlin.math.abs(progress - 0.55f).coerceIn(0f, 1f))),
            start = Offset((x - 32.dp.toPx()).coerceAtLeast(0f), 0f),
            end = Offset(x.coerceAtMost(size.width), size.height),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = NeonBlue.copy(alpha = 0.26f),
            start = Offset(0f, size.height - 1.dp.toPx()),
            end = Offset(size.width * progress.coerceIn(0f, 0.7f), size.height - 1.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HoloWordToken(
    token: CaptionToken,
    active: Boolean,
    onWord: (String) -> Unit,
    onLongWord: (String) -> Unit
) {
    val activeProgress by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(400, easing = HoloEase),
        label = "word-token-active"
    )
    val shimmer = if (active) {
        val transition = rememberInfiniteTransition(label = "word-token-scan")
        val phase by transition.animateFloat(
            initialValue = -0.25f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(tween(820, easing = LinearEasing), RepeatMode.Restart),
            label = "word-token-scan-phase"
        )
        phase
    } else {
        -1f
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .combinedClickable(
                onClick = { onWord(token.text) },
                onLongClick = { onLongWord(token.text) }
            )
            .padding(horizontal = 1.dp, vertical = 1.dp)
    ) {
        Text(
            token.text,
            color = if (token.saved) NeonPurple else if (active) TextMain else TextSoft,
            fontSize = if (active) 22.sp else 18.sp,
            fontWeight = if (token.saved || active) FontWeight.Bold else FontWeight.Normal,
            fontFamily = HoloTitleFont,
            letterSpacing = (activeProgress * 0.5f).sp,
            style = TextStyle(
                shadow = if (active) {
                    Shadow(NeonPurple.copy(alpha = 0.34f + activeProgress * 0.18f), Offset(0f, 0f), 8f + activeProgress * 3f)
                } else {
                    Shadow(Color.Transparent, Offset.Zero, 0f)
                }
            )
        )
        if (active) {
            Canvas(Modifier.matchParentSize()) {
                val x = size.width * shimmer
                drawLine(
                    color = Color.White.copy(alpha = 0.40f * activeProgress),
                    start = Offset(x, 0f),
                    end = Offset(x + 8.dp.toPx(), size.height),
                    strokeWidth = 0.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = NeonBlue.copy(alpha = 0.18f * activeProgress),
                    start = Offset(0f, size.height - 0.8.dp.toPx()),
                    end = Offset(size.width * activeProgress, size.height - 0.8.dp.toPx()),
                    strokeWidth = 0.8.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun HoloChineseCaption(text: String, activeProgress: Float) {
    Box(Modifier.fillMaxWidth().graphicsLayer { translationY = (1f - activeProgress) * 4.dp.toPx(); alpha = 0.72f + activeProgress * 0.28f }) {
        Text(
            text,
            color = if (activeProgress > 0.5f) TextMain.copy(alpha = 0.88f) else TextSoft,
            fontFamily = HoloBodyFont,
            fontWeight = if (activeProgress > 0.5f) FontWeight.Medium else FontWeight.Normal,
            letterSpacing = (activeProgress * 1.5f).sp,
            lineHeight = 24.sp
        )
        Canvas(Modifier.matchParentSize()) {
            if (activeProgress > 0f) {
                val lineY = size.height - 1.dp.toPx()
                drawLine(
                    color = NeonBlue.copy(alpha = 0.24f * activeProgress),
                    start = Offset(0f, lineY),
                    end = Offset(size.width * (0.2f + activeProgress * 0.5f), lineY),
                    strokeWidth = 1.dp.toPx()
                )
                repeat(8) { index ->
                    val t = index / 7f
                    val y = size.height - activeProgress * (8.dp.toPx() + 6.dp.toPx() * t)
                    drawDiamond(Offset(size.width * (0.08f + t * 0.68f), y), 1.4.dp.toPx(), NeonGreen.copy(alpha = 0.22f * activeProgress * (1f - t * 0.45f)))
                }
            }
        }
    }
}

@Composable
private fun HoloMediaLoadOverlay(progress: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val base = 18.dp.toPx() + size.minDimension * 0.62f * progress
        drawRect(Color.Black.copy(alpha = 0.24f * (1f - progress)))
        drawCircle(NeonBlue.copy(alpha = 0.08f * (1f - progress)), radius = base, center = c, style = Stroke(1.dp.toPx()))
        repeat(3) { ring ->
            val r = 14.dp.toPx() + ring * 7.dp.toPx() + progress * size.minDimension * 0.2f
            drawArc(
                color = listOf(NeonBlue, NeonPurple, HoloGold)[ring].copy(alpha = 0.42f * (1f - progress * 0.65f)),
                startAngle = progress * 240f + ring * 80f,
                sweepAngle = 72f,
                useCenter = false,
                topLeft = Offset(c.x - r, c.y - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(1.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        val sweepY = size.height * progress
        drawLine(
            color = Color.White.copy(alpha = 0.38f * (1f - progress)),
            start = Offset(0f, sweepY),
            end = Offset(size.width, sweepY + 24.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }
}

private fun DrawScope.drawAnimeScene(scene: AnimeSceneKind) {
    val w = size.width
    val h = size.height
    val s = size.minDimension / 150f
    val palette = when (scene) {
        AnimeSceneKind.MoonLibrary -> listOf(Color(0xFFF7FBFF), Color(0xFFDCEAFF), Color(0xFFFFF2D2))
        AnimeSceneKind.TrainWindow -> listOf(Color(0xFFFFF6E1), Color(0xFFE3EEFF), Color(0xFFF9D6C8))
        AnimeSceneKind.StarClassroom -> listOf(Color(0xFFF8FBFF), Color(0xFFE7F3FF), Color(0xFFEDE8FF))
        AnimeSceneKind.VoiceStudio -> listOf(Color(0xFFF7FBFF), Color(0xFFE4F7F4), Color(0xFFE9EEFF))
        AnimeSceneKind.MaterialsDock -> listOf(Color(0xFFF7FCFF), Color(0xFFDFF5FF), Color(0xFFE9F0FF))
        AnimeSceneKind.ProfileBay -> listOf(Color(0xFFFBFCFF), Color(0xFFEAF4FF), Color(0xFFF5E9FF))
    }
    drawRoundRect(
        brush = Brush.verticalGradient(palette),
        cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx())
    )
    drawCircle(Color.White.copy(alpha = 0.76f), radius = 30f * s, center = Offset(w * 0.75f, h * 0.22f))
    drawCircle(HoloGold.copy(alpha = 0.16f), radius = 36f * s, center = Offset(w * 0.74f, h * 0.20f), style = Stroke(1.1.dp.toPx()))
    drawLine(NeonBlue.copy(alpha = 0.16f), Offset(w * 0.12f, h * 0.20f), Offset(w * 0.88f, h * 0.10f), 1.dp.toPx())
    drawLine(NeonPurple.copy(alpha = 0.11f), Offset(w * 0.10f, h * 0.54f), Offset(w * 0.92f, h * 0.30f), 1.dp.toPx())

    when (scene) {
        AnimeSceneKind.MoonLibrary -> {
            drawRect(Color.White.copy(alpha = 0.46f), Offset(w * 0.08f, h * 0.18f), Size(w * 0.54f, h * 0.44f))
            repeat(3) { index ->
                val x = w * (0.15f + index * 0.14f)
                drawRoundRect(Color(0xFF8BA0BE).copy(alpha = 0.22f), Offset(x, h * 0.25f), Size(w * 0.055f, h * 0.33f), CornerRadius(4f * s, 4f * s))
            }
            drawRoundRect(NeonPurple.copy(alpha = 0.22f), Offset(w * 0.12f, h * 0.64f), Size(w * 0.42f, h * 0.13f), CornerRadius(8f * s, 8f * s))
            drawAnimeCharacter(Offset(w * 0.69f, h * 0.64f), s, NeonPurple)
        }
        AnimeSceneKind.TrainWindow -> {
            drawRoundRect(Color.White.copy(alpha = 0.54f), Offset(w * 0.08f, h * 0.18f), Size(w * 0.76f, h * 0.44f), CornerRadius(14f * s, 14f * s))
            drawLine(Color(0xFF50627A).copy(alpha = 0.26f), Offset(w * 0.12f, h * 0.42f), Offset(w * 0.80f, h * 0.42f), 1.dp.toPx())
            drawPath(
                Path().apply {
                    moveTo(w * 0.08f, h * 0.72f)
                    cubicTo(w * 0.28f, h * 0.58f, w * 0.52f, h * 0.72f, w * 0.84f, h * 0.58f)
                    lineTo(w * 0.84f, h)
                    lineTo(w * 0.08f, h)
                    close()
                },
                NeonBlue.copy(alpha = 0.18f)
            )
            drawAnimeCharacter(Offset(w * 0.64f, h * 0.66f), s, HoloGold)
        }
        AnimeSceneKind.StarClassroom -> {
            drawRoundRect(Color.White.copy(alpha = 0.48f), Offset(w * 0.10f, h * 0.16f), Size(w * 0.66f, h * 0.40f), CornerRadius(10f * s, 10f * s))
            repeat(2) { index ->
                drawLine(NeonBlue.copy(alpha = 0.20f), Offset(w * (0.24f + index * 0.20f), h * 0.17f), Offset(w * (0.24f + index * 0.20f), h * 0.55f), 1.dp.toPx())
            }
            repeat(5) { index ->
                drawDiamond(Offset(w * (0.18f + index * 0.15f), h * (0.18f + (index % 2) * 0.14f)), 2.5f * s, HoloGold.copy(alpha = 0.56f))
            }
            drawRoundRect(Color(0xFF2B3D5C).copy(alpha = 0.18f), Offset(w * 0.12f, h * 0.70f), Size(w * 0.62f, h * 0.12f), CornerRadius(8f * s, 8f * s))
            drawAnimeCharacter(Offset(w * 0.70f, h * 0.64f), s, NeonBlue)
        }
        AnimeSceneKind.VoiceStudio -> {
            drawRoundRect(Color.White.copy(alpha = 0.54f), Offset(w * 0.12f, h * 0.16f), Size(w * 0.70f, h * 0.46f), CornerRadius(16f * s, 16f * s))
            drawCircle(NeonGreen.copy(alpha = 0.18f), radius = 24f * s, center = Offset(w * 0.33f, h * 0.44f), style = Stroke(3.dp.toPx()))
            drawLine(NeonGreen.copy(alpha = 0.42f), Offset(w * 0.33f, h * 0.42f), Offset(w * 0.33f, h * 0.66f), 3.dp.toPx(), cap = StrokeCap.Round)
            drawAnimeCharacter(Offset(w * 0.68f, h * 0.66f), s, NeonGreen)
        }
        AnimeSceneKind.MaterialsDock -> {
            drawRoundRect(Color.White.copy(alpha = 0.50f), Offset(w * 0.10f, h * 0.18f), Size(w * 0.72f, h * 0.40f), CornerRadius(14f * s, 14f * s))
            drawLine(NeonBlue.copy(alpha = 0.28f), Offset(w * 0.18f, h * 0.30f), Offset(w * 0.72f, h * 0.30f), 1.dp.toPx())
            drawLine(NeonPurple.copy(alpha = 0.20f), Offset(w * 0.18f, h * 0.42f), Offset(w * 0.64f, h * 0.42f), 1.dp.toPx())
            drawAnimeCharacter(Offset(w * 0.66f, h * 0.66f), s, NeonBlue)
        }
        AnimeSceneKind.ProfileBay -> {
            drawRoundRect(Color.White.copy(alpha = 0.48f), Offset(w * 0.16f, h * 0.18f), Size(w * 0.62f, h * 0.42f), CornerRadius(18f * s, 18f * s))
            drawCircle(NeonPurple.copy(alpha = 0.18f), radius = 26f * s, center = Offset(w * 0.42f, h * 0.38f), style = Stroke(2.dp.toPx()))
            drawDiamond(Offset(w * 0.42f, h * 0.38f), 7f * s, HoloGold.copy(alpha = 0.42f))
            drawAnimeCharacter(Offset(w * 0.68f, h * 0.66f), s, NeonPurple)
        }
    }
}

private fun DrawScope.drawAnimeCharacter(anchor: Offset, scale: Float, accent: Color) {
    val skin = Color(0xFFFFDFC8)
    val ink = Color(0xFF243047)
    val r = 13f * scale
    val head = Offset(anchor.x, anchor.y - 28f * scale)
    drawPath(
        Path().apply {
            moveTo(head.x - 19f * scale, head.y - 2f * scale)
            cubicTo(head.x - 18f * scale, head.y - 24f * scale, head.x + 18f * scale, head.y - 25f * scale, head.x + 21f * scale, head.y - 1f * scale)
            cubicTo(head.x + 14f * scale, head.y + 8f * scale, head.x - 12f * scale, head.y + 12f * scale, head.x - 19f * scale, head.y - 2f * scale)
            close()
        },
        ink.copy(alpha = 0.90f)
    )
    drawCircle(skin, radius = r, center = head)
    drawCircle(ink.copy(alpha = 0.82f), radius = 1.2f * scale, center = Offset(head.x - 5f * scale, head.y - 1f * scale))
    drawCircle(ink.copy(alpha = 0.82f), radius = 1.2f * scale, center = Offset(head.x + 5f * scale, head.y - 1f * scale))
    drawPath(
        Path().apply {
            moveTo(anchor.x - 23f * scale, anchor.y + 28f * scale)
            lineTo(anchor.x - 11f * scale, anchor.y - 10f * scale)
            lineTo(anchor.x + 12f * scale, anchor.y - 10f * scale)
            lineTo(anchor.x + 27f * scale, anchor.y + 28f * scale)
            close()
        },
        accent.copy(alpha = 0.70f)
    )
    drawPath(
        Path().apply {
            moveTo(anchor.x - 9f * scale, anchor.y - 7f * scale)
            lineTo(anchor.x, anchor.y + 4f * scale)
            lineTo(anchor.x + 9f * scale, anchor.y - 7f * scale)
        },
        Color.White.copy(alpha = 0.78f),
        style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round)
    )
    drawCircle(Color.White.copy(alpha = 0.40f), radius = 3f * scale, center = Offset(anchor.x + 18f * scale, anchor.y - 42f * scale))
}

private fun DrawScope.drawDiamond(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius, center.y)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius, center.y)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawHexagon(center: Offset, radius: Float, color: Color) {
    val path = Path()
    repeat(6) { index ->
        val angle = PI / 3.0 * index + PI / 6.0
        val point = Offset(
            center.x + cos(angle).toFloat() * radius,
            center.y + sin(angle).toFloat() * radius
        )
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    drawPath(path, color)
}

private fun Float.floorMod(mod: Float): Float {
    if (mod <= 0f) return this
    val value = this % mod
    return if (value < 0f) value + mod else value
}

@Composable
private fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = Color.White.copy(alpha = 0.80f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.36f)),
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Box {
            Canvas(Modifier.matchParentSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        listOf(color.copy(alpha = 0.13f), SakuraPink.copy(alpha = 0.035f), Color.Transparent),
                        center = Offset(size.width * 0.84f, size.height * 0.18f),
                        radius = size.minDimension * 0.72f
                    )
                )
                drawLine(color.copy(alpha = 0.18f), Offset(size.width * 0.12f, size.height), Offset(size.width * 0.92f, size.height * 0.20f), 1.dp.toPx())
            }
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, null, tint = color)
                Text(value, color = TextMain, fontSize = 25.sp, fontFamily = HoloMonoFont, fontWeight = FontWeight.Black)
                Text(title, color = TextSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun MediaPoster(content: ImportedContent, small: Boolean = false) {
    val size = if (small) 66.dp else 112.dp
    val colors = when (content.kind) {
        SourceType.VIDEO -> listOf(Color(0xFFEAF1FF), Color(0xFFD9E6FF))
        SourceType.AUDIO -> listOf(Color(0xFFE8FBF6), Color(0xFFD7F4EE))
        SourceType.SUBTITLE -> listOf(Color(0xFFFFF7E7), Color(0xFFF7E8C9))
        SourceType.DOCUMENT -> listOf(Color(0xFFFFFFFF), Color(0xFFEFF4FF))
    }
    Box(Modifier.size(size).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(colors)).border(1.dp, HoloStroke.copy(alpha = 0.85f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
        Icon(content.kind.icon(), null, tint = NeonPurple, modifier = Modifier.size(if (small) 28.dp else 42.dp))
    }
}

@Composable
private fun QuickImportCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    HoloPressSurface(
        color = Color.White.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, HoloStroke.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.height(86.dp),
        onClick = onClick
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = NeonBlue)
            Text(title, color = TextMain, fontFamily = HoloTitleFont, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MiniImportChip(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    HoloPressSurface(
        modifier = modifier.height(48.dp),
        color = Color.White.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, HoloStroke.copy(alpha = 0.62f)),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = NeonBlue, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(5.dp))
            Text(title, color = TextMain, fontFamily = HoloTitleFont, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable
private fun HoloTag(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.11f), border = BorderStroke(1.dp, color.copy(alpha = 0.42f)), shape = CircleShape) {
        Text(text, color = color, fontFamily = HoloMonoFont, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), maxLines = 1)
    }
}

@Composable
private fun HoloMetric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(color = Color.White.copy(alpha = 0.045f), border = BorderStroke(1.dp, color.copy(alpha = 0.25f)), shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = TextSoft, fontSize = 11.sp, maxLines = 1)
            Text(value, color = color, fontFamily = HoloMonoFont, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SearchField(value: String, placeholder: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextSoft) },
        placeholder = { Text(placeholder, color = TextSoft) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterRow(options: List<String>, selected: String, onSelected: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(selected = selected == option, onClick = { onSelected(option) }, label = { Text(option) })
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String = "") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (action.isNotBlank()) Text(action, color = TextSoft)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = NeonPurple, fontWeight = FontWeight.Bold)
}

@Composable
private fun InfoBlock(title: String, body: String) {
    GlowCard {
        SectionLabel(title)
        Text(body.ifBlank { "待补全" }, color = TextMain, fontSize = 17.sp)
    }
}

@Composable
private fun SpeechStatusText(state: SpeechPlaybackState) {
    if (state == SpeechPlaybackState.Idle) return
    val color = when (state) {
        is SpeechPlaybackState.Cloud, is SpeechPlaybackState.Cached -> NeonGreen
        SpeechPlaybackState.Loading -> TextSoft
        is SpeechPlaybackState.SystemFallback -> NeonAmber
        is SpeechPlaybackState.Failed -> NeonRed
        SpeechPlaybackState.Idle -> TextSoft
    }
    Text(state.message, color = color, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun StepDot(label: String, done: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(if (done) NeonPurple else Color.White.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
            Icon(if (done) Icons.Filled.CheckCircle else Icons.Filled.Search, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Text(label, color = if (done) TextMain else TextSoft, fontSize = 12.sp)
    }
}

@Composable
private fun GlowPill(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(color = Color.White.copy(alpha = 0.70f), border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.18f)), shape = CircleShape) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = NeonPurple, modifier = Modifier.size(20.dp))
            Text(text, color = TextMain, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ControlButton(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    HoloPressSurface(
        modifier = Modifier.size(72.dp, 58.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.Transparent),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
            Icon(icon, null, tint = TextMain)
            Text(title, color = TextMain, fontFamily = HoloMonoFont, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, color = TextSoft, fontSize = 11.sp)
        }
    }
}

@Composable
private fun TinySpeechChip(label: String, onClick: () -> Unit) {
    HoloPressSurface(
        color = Color.White.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shape = CircleShape,
        modifier = Modifier.size(28.dp),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = TextSoft, fontFamily = HoloTitleFont, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CaptionActionRail(
    active: Boolean,
    onReplay: () -> Unit,
    onSpeakEnglish: () -> Unit,
    onSpeakChinese: () -> Unit,
    onSpeakBilingual: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HoloPressSurface(
            color = if (active) NeonGreen.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f),
            border = BorderStroke(1.dp, if (active) NeonGreen.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.10f)),
            shape = CircleShape,
            modifier = Modifier.size(32.dp),
            onClick = onReplay
        ) {
            Icon(Icons.Filled.Replay, "重播本句", tint = if (active) NeonGreen else TextSoft, modifier = Modifier.size(17.dp))
        }
        HoloPressSurface(
            color = if (active) NeonPurple.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f),
            border = BorderStroke(1.dp, if (active) NeonPurple.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.10f)),
            shape = CircleShape,
            modifier = Modifier.size(32.dp),
            onClick = onSpeakEnglish
        ) {
            Icon(Icons.Filled.VolumeUp, "播放英文", tint = if (active) NeonPurple else TextSoft, modifier = Modifier.size(17.dp))
        }
        TinySpeechChip("中", onSpeakChinese)
        TinySpeechChip("双", onSpeakBilingual)
    }
}

@Composable
private fun AnimatedRing(progress: Float, color: Color) {
    Canvas(Modifier.size(58.dp)) {
        drawCircle(Color.White.copy(alpha = 0.08f), style = Stroke(6.dp.toPx()))
        drawArc(color, -90f, progress.coerceIn(0f, 1f) * 360f, false, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun StudyTaskCard(
    step: Int,
    type: StudyTaskType,
    value: String,
    onClick: () -> Unit
) {
    GlowCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(54.dp).clip(CircleShape).background(NeonPurple.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(type.icon(), null, tint = NeonPurple)
            }
            Column(Modifier.weight(1f)) {
                Text("第 $step 步 · ${type.label()}", color = TextMain, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(type.description(), color = TextSoft)
            }
            Text(value, color = NeonPurple, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            AssistChip(onClick = {}, label = { Text(type.helpLabel()) })
            NeonButton(type.actionLabel(), Icons.Filled.PlayArrow, onClick, Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(title: String, navController: NavController) {
    TopAppBar(
        title = { Text(title, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.Close, null, tint = TextMain)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(alpha = 0.92f))
    )
}

@Composable
private fun DirectLinkDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var url by rememberSaveable { mutableStateOf("") }
    val normalized = url.trim()
    val extractedUrl = PlatformLinkResolver.extractFirstHttpUrl(normalized).orEmpty()
    val localError = normalized.isNotBlank() && extractedUrl.isBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(enabled = normalized.isNotBlank() && !localError, onClick = { onSubmit(normalized) }) { Text("导入") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("粘贴平台链接") },
        text = {
            Column {
                Text("粘贴分享文案或网页链接即可导入。")
                OutlinedTextField(value = url, onValueChange = { url = it }, placeholder = { Text("复制的整段分享文案或 https://...") }, singleLine = false, minLines = 2)
                if (localError) Text("粘贴内容里需要包含 http:// 或 https:// 链接", color = NeonAmber, fontSize = 13.sp)
                if (extractedUrl.isNotBlank()) Text("将解析：$extractedUrl", color = NeonBlue, fontFamily = HoloMonoFont, fontSize = 12.sp)
            }
        }
    )
}

@Composable
private fun DeleteDialog(title: String, onDismiss: () -> Unit, onKeepWords: () -> Unit, onDeleteWords: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除内容") },
        text = { Text("删除「$title」后，可选择是否同时删除只来自该材料的生词。") },
        confirmButton = { TextButton(onClick = onKeepWords) { Text("仅删除内容") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDeleteWords) { Text("内容和来源生词") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}

private fun selection(content: ImportedContent, cue: CaptionCue, word: String, captionId: String): WordSelectionContext =
    WordSelectionContext(
        word = word,
        sourceItemId = content.id,
        captionStartMs = cue.startMs,
        captionEndMs = cue.endMs,
        englishSentence = cue.english,
        chineseSentence = cue.chinese,
        sourceType = content.kind.toLearningSource(),
        captionId = captionId,
        sourceTitle = content.title,
        sourceUrl = content.sourceUrl
    )

private suspend fun speak(
    repository: XingYueRepository,
    text: String,
    systemTts: TextToSpeech,
    ttsPlaybackManager: TtsPlaybackManager,
    onState: (SpeechPlaybackState) -> Unit
) {
    onState(SpeechPlaybackState.Loading)
    val cloudResult = runCatching { repository.synthesizeSpeech(text) }
    val cloud = cloudResult.getOrNull()
    if (cloud?.audioPath != null) {
        runCatching {
            ttsPlaybackManager.play(cloud.audioPath)
        }.onSuccess {
            onState(if (cloud.cached) SpeechPlaybackState.Cached(cloud.voiceId) else SpeechPlaybackState.Cloud(cloud.voiceId))
        }.onFailure {
            systemTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, TextTools.normalizeWord(text))
            onState(SpeechPlaybackState.SystemFallback("云端音频播放失败"))
        }
    } else {
        val reason = cloudResult.exceptionOrNull()?.message ?: "未启用云端发音"
        systemTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, TextTools.normalizeWord(text))
        onState(SpeechPlaybackState.SystemFallback(reason))
    }
}

private fun MainTab.icon() = when (this) {
    MainTab.HOME -> Icons.Filled.Home
    MainTab.LIBRARY -> Icons.Filled.VideoFile
    MainTab.STUDY -> Icons.Filled.Book
    MainTab.WORDS -> Icons.Filled.MenuBook
    MainTab.PROFILE -> Icons.Filled.Person
}

private fun SourceType.icon() = when (this) {
    SourceType.VIDEO -> Icons.Filled.VideoFile
    SourceType.AUDIO -> Icons.Filled.Headphones
    SourceType.SUBTITLE -> Icons.Filled.Subtitles
    SourceType.DOCUMENT -> Icons.Filled.Book
}

private fun SourceType.label() = when (this) {
    SourceType.VIDEO -> "视频"
    SourceType.AUDIO -> "音频"
    SourceType.SUBTITLE -> "字幕"
    SourceType.DOCUMENT -> "文档"
}

private fun SourceType.toLearningSource() = when (this) {
    SourceType.VIDEO -> LearningSourceType.VIDEO
    SourceType.AUDIO -> LearningSourceType.AUDIO
    SourceType.SUBTITLE -> LearningSourceType.SUBTITLE
    SourceType.DOCUMENT -> LearningSourceType.DOCUMENT
}

private fun LearningSourceType.label() = when (this) {
    LearningSourceType.VIDEO -> "视频"
    LearningSourceType.AUDIO -> "音频"
    LearningSourceType.SUBTITLE -> "字幕"
    LearningSourceType.DOCUMENT -> "文档"
}

private fun sourceSignal(content: ImportedContent): String {
    val host = runCatching { Uri.parse(content.sourceUrl.ifBlank { content.sourcePath }).host.orEmpty().lowercase(Locale.ROOT) }
        .getOrDefault("")
    return when {
        host.contains("youtube") || host.contains("youtu.be") -> "YouTube"
        host.contains("bilibili") || host.contains("b23.tv") -> "Bilibili"
        host.contains("douyin") || host.contains("iesdouyin") -> "Douyin"
        host.isNotBlank() -> host.removePrefix("www.").take(18)
        content.sourcePath.startsWith("http", ignoreCase = true) -> "网页链接"
        else -> content.kind.label()
    }
}

private fun subtitleEmptySuggestion(content: ImportedContent): String = when (content.status) {
    ImportProcessingStatus.NEEDS_CLOUD_KEY -> "当前音视频没有可显示字幕。配置云端转写后可生成双语字幕。"
    ImportProcessingStatus.FAILED -> "处理失败。可重试，或改用本地字幕文件。"
    ImportProcessingStatus.NEEDS_MEDIA_ENGINE -> "当前素材需要字幕文件或转写能力。"
    ImportProcessingStatus.READY_TO_LEARN -> "没有读取到字幕行。可导入同名字幕文件。"
    else -> "字幕仍在生成，请稍后查看。"
}

private fun ImportProcessingStatus.label() = when (this) {
    ImportProcessingStatus.IMPORTED -> "已导入"
    ImportProcessingStatus.EXTRACTING_SUBTITLE -> "识别字幕"
    ImportProcessingStatus.TRANSCRIBING -> "语音识别"
    ImportProcessingStatus.TRANSLATING -> "生成双语"
    ImportProcessingStatus.EXTRACTING_WORDS -> "提取生词"
    ImportProcessingStatus.READY_TO_LEARN -> "可学习"
    ImportProcessingStatus.NEEDS_CLOUD_KEY -> "需要云端转写"
    ImportProcessingStatus.NEEDS_MEDIA_ENGINE -> "需要字幕或转写"
    ImportProcessingStatus.FAILED -> "处理失败"
}

private fun LearningWordStatus.label() = when (this) {
    LearningWordStatus.NEW_WORD -> "新词"
    LearningWordStatus.LEARNING -> "掌握中"
    LearningWordStatus.DUE -> "待复习"
    LearningWordStatus.FAMILIAR -> "已熟悉"
    LearningWordStatus.MASTERED -> "已掌握"
    LearningWordStatus.IGNORED -> "已忽略"
}

private fun StudyTaskType.label() = when (this) {
    StudyTaskType.NEW_WORDS -> "今日新词"
    StudyTaskType.DUE_REVIEW -> "到期复习"
    StudyTaskType.SPELLING -> "拼写测试"
    StudyTaskType.LISTENING_REPEAT -> "听力跟读"
    StudyTaskType.HUNDRED_LS -> "100LS"
    StudyTaskType.MISTAKES -> "错词回顾"
}

private fun StudyTaskType.description() = when (this) {
    StudyTaskType.NEW_WORDS -> "新词入组。"
    StudyTaskType.DUE_REVIEW -> "到期词优先。"
    StudyTaskType.SPELLING -> "拼写回忆。"
    StudyTaskType.LISTENING_REPEAT -> "单句跟读。"
    StudyTaskType.HUNDRED_LS -> "泛听计时。"
    StudyTaskType.MISTAKES -> "错词回流。"
}

private fun StudyTaskType.helpLabel() = when (this) {
    StudyTaskType.DUE_REVIEW -> "先清到期"
    StudyTaskType.MISTAKES -> "先纠错"
    StudyTaskType.SPELLING -> "主动回忆"
    StudyTaskType.NEW_WORDS -> "再学新词"
    StudyTaskType.LISTENING_REPEAT -> "跟读输出"
    StudyTaskType.HUNDRED_LS -> "泛听积累"
}

private fun StudyTaskType.actionLabel() = when (this) {
    StudyTaskType.DUE_REVIEW -> "开始复习"
    StudyTaskType.MISTAKES -> "处理错词"
    StudyTaskType.SPELLING -> "开始拼写"
    StudyTaskType.NEW_WORDS -> "学习新词"
    StudyTaskType.LISTENING_REPEAT -> "开始跟读"
    StudyTaskType.HUNDRED_LS -> "记录100LS"
}

private fun StudyTaskType.valueLabel(size: Int) = when (this) {
    StudyTaskType.LISTENING_REPEAT,
    StudyTaskType.HUNDRED_LS -> "$size 条"
    else -> "$size 个"
}

private fun WordLearningPhase.label() = when (this) {
    WordLearningPhase.RECOGNIZE -> "看词认识"
    WordLearningPhase.LISTEN -> "听发音"
    WordLearningPhase.EXAMPLE -> "看例句"
    WordLearningPhase.SPELLING_RECALL -> "拼写回忆"
    WordLearningPhase.FEEDBACK -> "熟悉度反馈"
}

private fun StudyTaskType.icon() = when (this) {
    StudyTaskType.DUE_REVIEW -> Icons.Filled.Replay
    StudyTaskType.MISTAKES -> Icons.Filled.Refresh
    StudyTaskType.SPELLING -> Icons.Filled.FilterList
    StudyTaskType.NEW_WORDS -> Icons.Filled.MenuBook
    StudyTaskType.LISTENING_REPEAT -> Icons.Filled.Audiotrack
    StudyTaskType.HUNDRED_LS -> Icons.Filled.Headphones
}

private fun ReviewRating.score() = when (this) {
    ReviewRating.AGAIN -> 0
    ReviewRating.HARD -> 55
    ReviewRating.GOOD -> 80
    ReviewRating.EASY -> 100
}

private fun statusColor(status: ImportProcessingStatus) = when (status) {
    ImportProcessingStatus.READY_TO_LEARN -> NeonGreen
    ImportProcessingStatus.FAILED, ImportProcessingStatus.NEEDS_CLOUD_KEY, ImportProcessingStatus.NEEDS_MEDIA_ENGINE -> NeonAmber
    else -> NeonPurple
}

private fun statusColor(status: LearningWordStatus) = when (status) {
    LearningWordStatus.NEW_WORD -> NeonPurple
    LearningWordStatus.LEARNING -> NeonPurple
    LearningWordStatus.DUE -> NeonAmber
    LearningWordStatus.FAMILIAR -> NeonGreen
    LearningWordStatus.MASTERED -> NeonGreen
    LearningWordStatus.IGNORED -> TextSoft
}

private fun LearningWord.masteryDots() = when (status) {
    LearningWordStatus.NEW_WORD -> 1
    LearningWordStatus.LEARNING -> 3
    LearningWordStatus.DUE -> 2
    LearningWordStatus.FAMILIAR -> 4
    LearningWordStatus.MASTERED -> 5
    LearningWordStatus.IGNORED -> 0
}

private fun rootHint(word: String): String {
    val normalized = TextTools.normalizeWord(word)
    return when {
        normalized.endsWith("ing") -> "${normalized.removeSuffix("ing")} + -ing -> 正在进行或状态延续"
        normalized.endsWith("tion") -> "${normalized.removeSuffix("tion")} + -tion -> 名词化动作或结果"
        normalized.endsWith("able") -> "${normalized.removeSuffix("able")} + -able -> 可...的"
        else -> "结合原句记忆：先记语境，再记释义。"
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val total = ms / 1000
    val minutes = total / 60
    val seconds = total % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun formatAgo(timestamp: Long): String {
    val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    val minute = 60_000L
    val hour = 60L * minute
    val day = 24L * hour
    return when {
        diff < minute -> "刚刚"
        diff < hour -> "${diff / minute} 分钟前"
        diff < day -> "${diff / hour} 小时前"
        else -> "${diff / day} 天前"
    }
}

private fun isToday(timestamp: Long): Boolean {
    val day = 24L * 60L * 60L * 1000L
    return timestamp / day == System.currentTimeMillis() / day
}
