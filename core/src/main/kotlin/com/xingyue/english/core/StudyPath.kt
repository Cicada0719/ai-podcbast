package com.xingyue.english.core

enum class LearningGoalMode {
    GENERAL,
    IELTS,
    TOEFL
}

enum class VocabularyDeckStage {
    CORE_3500,
    CORE_PHRASES_1200,
    ADVANCED_2000,
    AWL_570,
    IELTS_TOPICS,
    TOEFL_ACADEMIC,
    MY_SUBTITLE_WORDS
}

enum class PhraseUseCase {
    DAILY,
    IELTS_SPEAKING,
    IELTS_WRITING,
    TOEFL_LECTURE,
    TOEFL_CAMPUS
}

enum class StudyPathStage {
    VOCAB_WARMUP,
    CORE_VOCAB,
    PHRASE_INPUT,
    MATERIAL_INTENSIVE_LISTENING,
    SHADOWING_OUTPUT,
    MANUAL_WORD_CAPTURE,
    EXTENSIVE_LISTENING
}

data class VocabularyDeck(
    val id: String,
    val name: String,
    val stage: VocabularyDeckStage,
    val goalModes: Set<LearningGoalMode>,
    val description: String,
    val licenseSource: String,
    val itemTarget: Int,
    val order: Int
)

data class LexicalItem(
    val id: String,
    val word: String,
    val normalized: String = TextTools.normalizeWord(word),
    val phonetic: String = "",
    val definition: String,
    val cefr: String = "",
    val deckId: String,
    val stage: VocabularyDeckStage,
    val tags: Set<String> = emptySet(),
    val phrases: List<String> = emptyList(),
    val example: String = "",
    val licenseSource: String = "示范数据",
    val difficulty: Int = 1
)

data class PhraseChunk(
    val id: String,
    val english: String,
    val chinese: String,
    val useCase: PhraseUseCase,
    val deckId: String,
    val keywords: List<String>,
    val licenseSource: String = "示范数据",
    val ttsCacheKey: String = ""
)

data class ExamTopicPack(
    val id: String,
    val name: String,
    val goalMode: LearningGoalMode,
    val stage: VocabularyDeckStage,
    val topics: List<String>
)

data class VocabularyDiagnosticResult(
    val goalMode: LearningGoalMode,
    val recommendedStage: VocabularyDeckStage,
    val knownWordsEstimate: Int,
    val weakTags: List<String> = emptyList()
)

data class StudyPathStep(
    val id: String,
    val stage: StudyPathStage,
    val title: String,
    val subtitle: String,
    val actionLabel: String,
    val categoryTitle: String,
    val goalMode: LearningGoalMode,
    val deckStage: VocabularyDeckStage? = null,
    val taskType: StudyTaskType? = null,
    val contentId: String = "",
    val wordNormalized: String = "",
    val progressValue: Int = 0,
    val goalValue: Int = 1,
    val estimatedMinutes: Int = 5,
    val completed: Boolean = false,
    val blocked: Boolean = false,
    val blockerMessage: String = "",
    val highlights: List<String> = emptyList()
) {
    val progressFraction: Float
        get() = if (goalValue <= 0) 1f else (progressValue.toFloat() / goalValue).coerceIn(0f, 1f)
}

data class DailyStudyPath(
    val goalMode: LearningGoalMode,
    val generatedAt: Long,
    val headline: String,
    val subheadline: String,
    val deckStages: List<VocabularyDeckStage>,
    val steps: List<StudyPathStep>,
    val diagnostic: VocabularyDiagnosticResult
) {
    val completedSteps: Int
        get() = steps.count { it.completed }

    val totalSteps: Int
        get() = steps.size

    val progressFraction: Float
        get() = if (steps.isEmpty()) 1f else completedSteps.toFloat() / steps.size

    val nextStep: StudyPathStep?
        get() = steps.firstOrNull { !it.completed && !it.blocked } ?: steps.firstOrNull { !it.completed }
}

object BuiltInStudyLexicon {
    private const val SAMPLE_LICENSE = "示范数据；完整词库请通过 GPL 兼容 CSV/JSON 或用户私有导入补充"

    val decks: List<VocabularyDeck> = listOf(
        VocabularyDeck(
            id = "core-3500",
            name = "Core 3500",
            stage = VocabularyDeckStage.CORE_3500,
            goalModes = setOf(LearningGoalMode.GENERAL, LearningGoalMode.IELTS, LearningGoalMode.TOEFL),
            description = "先建立 A1-B2 高频词骨架，保证字幕听读能跟上。",
            licenseSource = SAMPLE_LICENSE,
            itemTarget = 3500,
            order = 1
        ),
        VocabularyDeck(
            id = "core-phrases-1200",
            name = "Core Phrases 1200",
            stage = VocabularyDeckStage.CORE_PHRASES_1200,
            goalModes = setOf(LearningGoalMode.GENERAL, LearningGoalMode.IELTS, LearningGoalMode.TOEFL),
            description = "高频短句、动词短语和搭配，解决听懂单词但串不成句的问题。",
            licenseSource = SAMPLE_LICENSE,
            itemTarget = 1200,
            order = 2
        ),
        VocabularyDeck(
            id = "advanced-2000",
            name = "Advanced 2000",
            stage = VocabularyDeckStage.ADVANCED_2000,
            goalModes = setOf(LearningGoalMode.GENERAL, LearningGoalMode.IELTS, LearningGoalMode.TOEFL),
            description = "B2-C1 扩展词，提升阅读密度和表达准确度。",
            licenseSource = SAMPLE_LICENSE,
            itemTarget = 2000,
            order = 3
        ),
        VocabularyDeck(
            id = "awl-570",
            name = "AWL 570",
            stage = VocabularyDeckStage.AWL_570,
            goalModes = setOf(LearningGoalMode.IELTS, LearningGoalMode.TOEFL),
            description = "学术词族桥梁，优先覆盖阅读、写作和讲座高频抽象词。",
            licenseSource = SAMPLE_LICENSE,
            itemTarget = 570,
            order = 4
        ),
        VocabularyDeck(
            id = "ielts-topic-packs",
            name = "IELTS Topic Packs",
            stage = VocabularyDeckStage.IELTS_TOPICS,
            goalModes = setOf(LearningGoalMode.IELTS),
            description = "教育、科技、环境、健康等 12 组雅思话题词和表达块。",
            licenseSource = SAMPLE_LICENSE,
            itemTarget = 1200,
            order = 5
        ),
        VocabularyDeck(
            id = "toefl-academic-packs",
            name = "TOEFL Academic Packs",
            stage = VocabularyDeckStage.TOEFL_ACADEMIC,
            goalModes = setOf(LearningGoalMode.TOEFL),
            description = "讲座、校园、课堂讨论和学科主题表达。",
            licenseSource = SAMPLE_LICENSE,
            itemTarget = 1000,
            order = 6
        ),
        VocabularyDeck(
            id = "my-subtitle-words",
            name = "My Subtitle Words",
            stage = VocabularyDeckStage.MY_SUBTITLE_WORDS,
            goalModes = setOf(LearningGoalMode.GENERAL, LearningGoalMode.IELTS, LearningGoalMode.TOEFL),
            description = "只收用户从字幕中主动保存的个人生词。",
            licenseSource = "用户个人数据",
            itemTarget = 0,
            order = 7
        )
    )

    val lexicalItems: List<LexicalItem> = listOf(
        LexicalItem("core-benefit", "benefit", definition = "n./v. 好处；使受益", cefr = "B1", deckId = "core-3500", stage = VocabularyDeckStage.CORE_3500, tags = setOf("core", "daily"), phrases = listOf("benefit from", "public benefit"), example = "Learners benefit from repeated exposure."),
        LexicalItem("core-method", "method", definition = "n. 方法；体系", cefr = "B1", deckId = "core-3500", stage = VocabularyDeckStage.CORE_3500, tags = setOf("core", "study"), phrases = listOf("a practical method", "teaching method"), example = "A clear method makes daily practice easier."),
        LexicalItem("core-evidence", "evidence", definition = "n. 证据；依据", cefr = "B2", deckId = "core-3500", stage = VocabularyDeckStage.CORE_3500, tags = setOf("core", "academic"), phrases = listOf("strong evidence", "based on evidence"), example = "The evidence supports the main idea."),
        LexicalItem("advanced-sustainable", "sustainable", definition = "adj. 可持续的", cefr = "C1", deckId = "advanced-2000", stage = VocabularyDeckStage.ADVANCED_2000, tags = setOf("advanced", "environment"), phrases = listOf("sustainable growth", "sustainable policy"), example = "Cities need sustainable transport systems."),
        LexicalItem("advanced-assumption", "assumption", definition = "n. 假设；前提", cefr = "C1", deckId = "advanced-2000", stage = VocabularyDeckStage.ADVANCED_2000, tags = setOf("advanced", "argument"), phrases = listOf("basic assumption", "challenge an assumption"), example = "The argument depends on a hidden assumption."),
        LexicalItem("awl-analyze", "analyze", definition = "v. 分析", cefr = "B2", deckId = "awl-570", stage = VocabularyDeckStage.AWL_570, tags = setOf("awl", "academic"), phrases = listOf("analyze data", "analyze the causes"), example = "The lecture analyzes changes in climate patterns."),
        LexicalItem("awl-significant", "significant", definition = "adj. 重要的；显著的", cefr = "B2", deckId = "awl-570", stage = VocabularyDeckStage.AWL_570, tags = setOf("awl", "academic"), phrases = listOf("a significant increase", "statistically significant"), example = "There was a significant shift in public opinion."),
        LexicalItem("ielts-urbanization", "urbanization", definition = "n. 城市化", cefr = "C1", deckId = "ielts-topic-packs", stage = VocabularyDeckStage.IELTS_TOPICS, tags = setOf("ielts", "city"), phrases = listOf("rapid urbanization", "urbanization pressure"), example = "Rapid urbanization can put pressure on housing."),
        LexicalItem("ielts-curriculum", "curriculum", definition = "n. 课程体系", cefr = "C1", deckId = "ielts-topic-packs", stage = VocabularyDeckStage.IELTS_TOPICS, tags = setOf("ielts", "education"), phrases = listOf("school curriculum", "broaden the curriculum"), example = "A balanced curriculum should include creative subjects."),
        LexicalItem("toefl-hypothesis", "hypothesis", definition = "n. 假说", cefr = "C1", deckId = "toefl-academic-packs", stage = VocabularyDeckStage.TOEFL_ACADEMIC, tags = setOf("toefl", "science"), phrases = listOf("test a hypothesis", "support a hypothesis"), example = "The professor explains why the hypothesis was rejected."),
        LexicalItem("toefl-artifact", "artifact", definition = "n. 人工制品；文物", cefr = "C1", deckId = "toefl-academic-packs", stage = VocabularyDeckStage.TOEFL_ACADEMIC, tags = setOf("toefl", "history"), phrases = listOf("ancient artifact", "cultural artifact"), example = "The artifact reveals how people traded goods."),
        LexicalItem("toefl-enroll", "enroll", definition = "v. 注册；入学", cefr = "B2", deckId = "toefl-academic-packs", stage = VocabularyDeckStage.TOEFL_ACADEMIC, tags = setOf("toefl", "campus"), phrases = listOf("enroll in a course", "enrollment deadline"), example = "She wants to enroll in the biology seminar.")
    )

    val phraseChunks: List<PhraseChunk> = listOf(
        PhraseChunk("daily-point-is", "The point is that...", "重点是……", PhraseUseCase.DAILY, "core-phrases-1200", listOf("point", "that")),
        PhraseChunk("daily-used-to", "I used to think that...", "我以前以为……", PhraseUseCase.DAILY, "core-phrases-1200", listOf("used to", "think")),
        PhraseChunk("daily-it-turns-out", "It turns out that...", "结果是……", PhraseUseCase.DAILY, "core-phrases-1200", listOf("turns out")),
        PhraseChunk("daily-instead-of", "Instead of doing that, we can...", "不要那样做，我们可以……", PhraseUseCase.DAILY, "core-phrases-1200", listOf("instead of")),
        PhraseChunk("daily-as-long-as", "As long as you keep practicing, ...", "只要你坚持练习，……", PhraseUseCase.DAILY, "core-phrases-1200", listOf("as long as")),
        PhraseChunk("daily-depends-on", "It depends on the situation.", "这取决于具体情况。", PhraseUseCase.DAILY, "core-phrases-1200", listOf("depends on")),
        PhraseChunk("daily-pay-attention", "Pay attention to the main idea.", "注意主旨。", PhraseUseCase.DAILY, "core-phrases-1200", listOf("pay attention")),
        PhraseChunk("daily-make-sense", "That makes sense to me.", "这对我来说说得通。", PhraseUseCase.DAILY, "core-phrases-1200", listOf("make sense")),
        PhraseChunk("daily-figure-out", "I need to figure out the reason.", "我需要弄清原因。", PhraseUseCase.DAILY, "core-phrases-1200", listOf("figure out")),
        PhraseChunk("daily-come-up-with", "Can you come up with an example?", "你能想出一个例子吗？", PhraseUseCase.DAILY, "core-phrases-1200", listOf("come up with")),
        PhraseChunk("ielts-speaking-perspective", "From my perspective, the main reason is...", "从我的角度看，主要原因是……", PhraseUseCase.IELTS_SPEAKING, "ielts-topic-packs", listOf("perspective", "reason")),
        PhraseChunk("ielts-writing-extent", "To a large extent, this depends on...", "在很大程度上，这取决于……", PhraseUseCase.IELTS_WRITING, "ielts-topic-packs", listOf("extent", "depends")),
        PhraseChunk("ielts-writing-evidence", "This is supported by evidence from...", "这一点可以由……的证据支持。", PhraseUseCase.IELTS_WRITING, "ielts-topic-packs", listOf("supported", "evidence")),
        PhraseChunk("ielts-speaking-tend-to", "People tend to prefer... because...", "人们往往更喜欢……因为……", PhraseUseCase.IELTS_SPEAKING, "ielts-topic-packs", listOf("tend to")),
        PhraseChunk("ielts-speaking-in-my-area", "In my area, it is common to...", "在我所在地区，……很常见。", PhraseUseCase.IELTS_SPEAKING, "ielts-topic-packs", listOf("common")),
        PhraseChunk("ielts-writing-on-the-one-hand", "On the one hand, this can improve...", "一方面，这可以改善……", PhraseUseCase.IELTS_WRITING, "ielts-topic-packs", listOf("one hand")),
        PhraseChunk("ielts-writing-however", "However, this may also lead to...", "然而，这也可能导致……", PhraseUseCase.IELTS_WRITING, "ielts-topic-packs", listOf("however")),
        PhraseChunk("ielts-writing-policy", "A practical policy would be to...", "一个实际可行的政策是……", PhraseUseCase.IELTS_WRITING, "ielts-topic-packs", listOf("policy")),
        PhraseChunk("toefl-lecture-topic", "The professor mainly discusses...", "教授主要讨论……", PhraseUseCase.TOEFL_LECTURE, "toefl-academic-packs", listOf("professor", "discusses")),
        PhraseChunk("toefl-lecture-example", "The example illustrates how...", "这个例子说明了……如何发生。", PhraseUseCase.TOEFL_LECTURE, "toefl-academic-packs", listOf("example", "illustrates")),
        PhraseChunk("toefl-campus-problem", "The student is concerned about...", "学生担心的是……", PhraseUseCase.TOEFL_CAMPUS, "toefl-academic-packs", listOf("student", "concerned")),
        PhraseChunk("toefl-lecture-hypothesis", "This hypothesis explains why...", "这个假说解释了为什么……", PhraseUseCase.TOEFL_LECTURE, "toefl-academic-packs", listOf("hypothesis")),
        PhraseChunk("toefl-lecture-contrast", "In contrast, the second theory suggests...", "相比之下，第二个理论认为……", PhraseUseCase.TOEFL_LECTURE, "toefl-academic-packs", listOf("contrast")),
        PhraseChunk("toefl-campus-solution", "The advisor suggests that the student should...", "顾问建议学生应该……", PhraseUseCase.TOEFL_CAMPUS, "toefl-academic-packs", listOf("advisor", "suggests")),
        PhraseChunk("toefl-campus-schedule", "The problem is related to the student's schedule.", "这个问题和学生的日程有关。", PhraseUseCase.TOEFL_CAMPUS, "toefl-academic-packs", listOf("schedule")),
        PhraseChunk("toefl-campus-option", "The first option is more practical because...", "第一个选项更实际，因为……", PhraseUseCase.TOEFL_CAMPUS, "toefl-academic-packs", listOf("option"))
    )

    val examTopicPacks: List<ExamTopicPack> = listOf(
        ExamTopicPack("ielts-topics", "IELTS 12 主题", LearningGoalMode.IELTS, VocabularyDeckStage.IELTS_TOPICS, listOf("教育", "科技", "环境", "健康", "工作", "文化", "社会", "政府", "媒体", "城市", "全球化", "犯罪")),
        ExamTopicPack("toefl-topics", "TOEFL 10 学术场景", LearningGoalMode.TOEFL, VocabularyDeckStage.TOEFL_ACADEMIC, listOf("生物", "地质", "心理", "历史", "艺术", "经济", "天文", "环境", "校园", "课堂讨论"))
    )
}

object StudyPathEngine {
    fun goalDeckStages(goalMode: LearningGoalMode): List<VocabularyDeckStage> =
        when (goalMode) {
            LearningGoalMode.GENERAL -> listOf(
                VocabularyDeckStage.CORE_3500,
                VocabularyDeckStage.CORE_PHRASES_1200,
                VocabularyDeckStage.ADVANCED_2000,
                VocabularyDeckStage.MY_SUBTITLE_WORDS
            )
            LearningGoalMode.IELTS -> listOf(
                VocabularyDeckStage.CORE_3500,
                VocabularyDeckStage.CORE_PHRASES_1200,
                VocabularyDeckStage.ADVANCED_2000,
                VocabularyDeckStage.AWL_570,
                VocabularyDeckStage.IELTS_TOPICS,
                VocabularyDeckStage.MY_SUBTITLE_WORDS
            )
            LearningGoalMode.TOEFL -> listOf(
                VocabularyDeckStage.CORE_3500,
                VocabularyDeckStage.CORE_PHRASES_1200,
                VocabularyDeckStage.ADVANCED_2000,
                VocabularyDeckStage.AWL_570,
                VocabularyDeckStage.TOEFL_ACADEMIC,
                VocabularyDeckStage.MY_SUBTITLE_WORDS
            )
        }

    fun buildTodayPath(
        goalMode: LearningGoalMode,
        plan: DailyLearningPlan,
        contents: List<ImportedContent>,
        words: List<LearningWord>,
        attempts: List<StudyAttempt>,
        lexicalItems: List<LexicalItem> = BuiltInStudyLexicon.lexicalItems,
        phraseChunks: List<PhraseChunk> = BuiltInStudyLexicon.phraseChunks,
        now: Long = System.currentTimeMillis()
    ): DailyStudyPath {
        val completedPathStages = attempts.completedPathStagesToday(now)
        val completedTasks = attempts.completedTaskTypesToday(now)
        val savedToday = words.count { isSameDay(it.createdAt, now) }
        val listenable = contents
            .filter { (it.kind == SourceType.VIDEO || it.kind == SourceType.AUDIO) && it.status == ImportProcessingStatus.READY_TO_LEARN }
            .sortedWith(compareByDescending<ImportedContent> { it.favorite }.thenByDescending { it.createdAt })
        val readyTexts = contents
            .filter { it.status == ImportProcessingStatus.READY_TO_LEARN }
            .sortedByDescending { it.createdAt }
        val preferredContent = listenable.firstOrNull() ?: readyTexts.firstOrNull()
        val duePressure = plan.dueReviewTasks.size + plan.mistakeTasks.size
        val targetNewWords = when (goalMode) {
            LearningGoalMode.GENERAL -> 12
            LearningGoalMode.IELTS -> 18
            LearningGoalMode.TOEFL -> 18
        }
        val phraseTarget = when (goalMode) {
            LearningGoalMode.GENERAL -> 3
            LearningGoalMode.IELTS,
            LearningGoalMode.TOEFL -> 5
        }
        val goalStages = goalDeckStages(goalMode)
        val goalPhrases = StudyQueuePlanner.phraseChunksFor(goalMode, phraseChunks)
        val goalWords = lexicalItems.filter { it.stage in goalStages && it.stage != VocabularyDeckStage.MY_SUBTITLE_WORDS }
        val completedNewWords = attempts.countCompletedToday(StudyTaskType.NEW_WORDS, now)
        val completedListening = attempts.countCompletedToday(StudyTaskType.LISTENING_REPEAT, now)
        val completedExtensive = attempts.countCompletedToday(StudyTaskType.HUNDRED_LS, now)
        val diagnostic = VocabularyDiagnosticResult(
            goalMode = goalMode,
            recommendedStage = recommendedStage(goalMode, words.size),
            knownWordsEstimate = words.count { it.status != LearningWordStatus.NEW_WORD },
            weakTags = goalWords.flatMap { it.tags }.groupingBy { it }.eachCount().entries
                .sortedByDescending { it.value }
                .map { it.key }
                .take(4)
        )
        val day = dayIndex(now)

        val steps = listOf(
            StudyPathStep(
                id = "path-$day-${goalMode.name.lowercase()}-warmup",
                stage = StudyPathStage.VOCAB_WARMUP,
                title = "词汇热身",
                subtitle = if (duePressure > 0) "先清 $duePressure 个到期复习和错词，降低遗忘负债。" else "今天没有到期负债，可以直接进入新输入。",
                actionLabel = if (duePressure > 0) "开始热身" else "已清空",
                categoryTitle = "今天要复习",
                goalMode = goalMode,
                taskType = if (plan.dueReviewTasks.isNotEmpty()) StudyTaskType.DUE_REVIEW else StudyTaskType.MISTAKES,
                progressValue = attempts.countCompletedToday(StudyTaskType.DUE_REVIEW, now) + attempts.countCompletedToday(StudyTaskType.MISTAKES, now),
                goalValue = duePressure.coerceAtLeast(1),
                estimatedMinutes = 5,
                completed = duePressure == 0 || StudyPathStage.VOCAB_WARMUP in completedPathStages,
                highlights = listOf("FSRS 到期", "错词回流")
            ),
            StudyPathStep(
                id = "path-$day-${goalMode.name.lowercase()}-core",
                stage = StudyPathStage.CORE_VOCAB,
                title = coreStepTitle(goalMode),
                subtitle = coreStepSubtitle(goalMode, diagnostic.recommendedStage),
                actionLabel = "完成一组",
                categoryTitle = "今天先背",
                goalMode = goalMode,
                deckStage = diagnostic.recommendedStage,
                taskType = StudyTaskType.NEW_WORDS,
                progressValue = completedNewWords,
                goalValue = targetNewWords,
                estimatedMinutes = 8,
                completed = StudyPathStage.CORE_VOCAB in completedPathStages ||
                    completedNewWords >= targetNewWords ||
                    StudyQueuePlanner.nextLexicalQueue(goalMode, diagnostic.recommendedStage, lexicalItems, attempts, sessionSize = 10).isEmpty(),
                highlights = goalWords.take(3).map { it.word }
            ),
            StudyPathStep(
                id = "path-$day-${goalMode.name.lowercase()}-phrases",
                stage = StudyPathStage.PHRASE_INPUT,
                title = "短句输入",
                subtitle = phraseStepSubtitle(goalMode),
                actionLabel = "学短句",
                categoryTitle = "今天先背",
                goalMode = goalMode,
                deckStage = VocabularyDeckStage.CORE_PHRASES_1200,
                taskType = StudyTaskType.SPELLING,
                progressValue = if (StudyPathStage.PHRASE_INPUT in completedPathStages) phraseTarget else 0,
                goalValue = phraseTarget,
                estimatedMinutes = 6,
                completed = StudyPathStage.PHRASE_INPUT in completedPathStages ||
                    StudyQueuePlanner.nextPhraseQueue(goalMode, phraseChunks, attempts, sessionSize = phraseTarget).isEmpty(),
                highlights = goalPhrases.take(3).map { it.english }
            ),
            StudyPathStep(
                id = "path-$day-${goalMode.name.lowercase()}-intensive",
                stage = StudyPathStage.MATERIAL_INTENSIVE_LISTENING,
                title = "素材精听",
                subtitle = preferredContent?.let { "打开《${it.title}》，逐句对齐双语字幕。" } ?: "先导入有字幕的音视频素材。",
                actionLabel = preferredContent?.let { "去精听" } ?: "导入素材",
                categoryTitle = "今天要听",
                goalMode = goalMode,
                taskType = StudyTaskType.LISTENING_REPEAT,
                contentId = preferredContent?.id.orEmpty(),
                progressValue = completedListening,
                goalValue = 1,
                estimatedMinutes = 10,
                completed = StudyPathStage.MATERIAL_INTENSIVE_LISTENING in completedPathStages || StudyTaskType.LISTENING_REPEAT in completedTasks,
                blocked = preferredContent == null,
                blockerMessage = "需要先导入字幕、文档或音视频素材。",
                highlights = preferredContent?.let { listOf(it.kind.name, "${it.wordCount} 词") } ?: listOf("SRT", "TXT", "JSON", "WAV/MP4")
            ),
            StudyPathStep(
                id = "path-$day-${goalMode.name.lowercase()}-shadowing",
                stage = StudyPathStage.SHADOWING_OUTPUT,
                title = "跟读输出",
                subtitle = preferredContent?.let { "挑 3-5 句，用原声或发音跟读。" } ?: "跟读来自字幕句或短句。",
                actionLabel = preferredContent?.let { "进入跟读" } ?: "等待素材",
                categoryTitle = "今天要听",
                goalMode = goalMode,
                taskType = StudyTaskType.LISTENING_REPEAT,
                contentId = preferredContent?.id.orEmpty(),
                progressValue = if (StudyPathStage.SHADOWING_OUTPUT in completedPathStages) 3 else 0,
                goalValue = 3,
                estimatedMinutes = 6,
                completed = StudyPathStage.SHADOWING_OUTPUT in completedPathStages,
                blocked = preferredContent == null,
                blockerMessage = "导入素材后才会生成跟读句。",
                highlights = listOf("原声", "发音", "复述")
            ),
            StudyPathStep(
                id = "path-$day-${goalMode.name.lowercase()}-manual-capture",
                stage = StudyPathStage.MANUAL_WORD_CAPTURE,
                title = "手动收词",
                subtitle = "点选字幕词并确认后进入个人词库。",
                actionLabel = if (preferredContent != null) "去收词" else "看生词本",
                categoryTitle = "学习记录",
                goalMode = goalMode,
                deckStage = VocabularyDeckStage.MY_SUBTITLE_WORDS,
                contentId = preferredContent?.id.orEmpty(),
                progressValue = savedToday,
                goalValue = 1,
                estimatedMinutes = 3,
                completed = StudyPathStage.MANUAL_WORD_CAPTURE in completedPathStages || savedToday > 0,
                highlights = listOf("用户确认", "字幕来源", "持久高亮")
            ),
            StudyPathStep(
                id = "path-$day-${goalMode.name.lowercase()}-extensive",
                stage = StudyPathStage.EXTENSIVE_LISTENING,
                title = "泛听巩固",
                subtitle = if (listenable.isNotEmpty()) "对同一素材记录 5/15/30 分钟泛听，培养声音熟悉度。" else "泛听只推荐音视频素材，不推荐纯文档。",
                actionLabel = if (listenable.isNotEmpty()) "记录泛听" else "导入音视频",
                categoryTitle = "学习记录",
                goalMode = goalMode,
                taskType = StudyTaskType.HUNDRED_LS,
                contentId = listenable.firstOrNull()?.id.orEmpty(),
                progressValue = completedExtensive,
                goalValue = 1,
                estimatedMinutes = 15,
                completed = StudyPathStage.EXTENSIVE_LISTENING in completedPathStages || StudyTaskType.HUNDRED_LS in completedTasks,
                blocked = listenable.isEmpty(),
                blockerMessage = "需要视频或音频素材。",
                highlights = listOf("5 分钟", "15 分钟", "30 分钟")
            )
        )

        return DailyStudyPath(
            goalMode = goalMode,
            generatedAt = now,
            headline = headline(goalMode),
            subheadline = subheadline(goalMode),
            deckStages = goalStages,
            steps = steps,
            diagnostic = diagnostic
        )
    }

    private fun recommendedStage(goalMode: LearningGoalMode, savedWordCount: Int): VocabularyDeckStage =
        when {
            goalMode == LearningGoalMode.IELTS && savedWordCount >= 1200 -> VocabularyDeckStage.IELTS_TOPICS
            goalMode == LearningGoalMode.TOEFL && savedWordCount >= 1200 -> VocabularyDeckStage.TOEFL_ACADEMIC
            savedWordCount >= 800 -> VocabularyDeckStage.ADVANCED_2000
            else -> VocabularyDeckStage.CORE_3500
        }

    private fun headline(goalMode: LearningGoalMode): String =
        when (goalMode) {
            LearningGoalMode.GENERAL -> "把素材听懂，再把词留住"
            LearningGoalMode.IELTS -> "用话题词和表达块支撑雅思输出"
            LearningGoalMode.TOEFL -> "用学术词族和讲座句型打通托福输入"
        }

    private fun subheadline(goalMode: LearningGoalMode): String =
        when (goalMode) {
            LearningGoalMode.GENERAL -> "核心词、短句、双语字幕和 FSRS 复习串成每天一条路。"
            LearningGoalMode.IELTS -> "先清复习，再背话题词，最后用精听和跟读把词变成表达。"
            LearningGoalMode.TOEFL -> "先补学术词族，再听讲座素材，把信号词和校园表达反复听熟。"
        }

    private fun coreStepTitle(goalMode: LearningGoalMode): String =
        when (goalMode) {
            LearningGoalMode.GENERAL -> "核心背词"
            LearningGoalMode.IELTS -> "IELTS 主题词"
            LearningGoalMode.TOEFL -> "TOEFL 学术词"
        }

    private fun coreStepSubtitle(goalMode: LearningGoalMode, stage: VocabularyDeckStage): String =
        when (goalMode) {
            LearningGoalMode.GENERAL -> "当前阶段 ${stage.nameForCopy()}，每天 10-15 个词，不追求一次背完。"
            LearningGoalMode.IELTS -> "围绕话题、搭配和 Lexical Resource，今天先背 15-20 个可输出词。"
            LearningGoalMode.TOEFL -> "围绕 lecture/campus 场景，今天先背学术词族和信号词。"
        }

    private fun phraseStepSubtitle(goalMode: LearningGoalMode): String =
        when (goalMode) {
            LearningGoalMode.GENERAL -> "把单词放回 3 个高频句块，解决听懂单词但反应慢。"
            LearningGoalMode.IELTS -> "背 5 个口语/写作表达块，重点是可直接复述和改写。"
            LearningGoalMode.TOEFL -> "背 5 个讲座/校园句框，听力和口语综合题优先。"
        }

    private fun List<StudyAttempt>.completedPathStagesToday(now: Long): Set<StudyPathStage> =
        asSequence()
            .filter { it.countsAsCompleted && isSameDay(it.completedOrClosedAt, now) }
            .mapNotNull { attempt ->
                attempt.result.removePrefix("path:").takeIf { attempt.result.startsWith("path:") }
                    ?.let { runCatching { StudyPathStage.valueOf(it) }.getOrNull() }
            }
            .toSet()

    private fun List<StudyAttempt>.completedTaskTypesToday(now: Long): Set<StudyTaskType> =
        asSequence()
            .filter { it.countsAsCompleted && isSameDay(it.completedOrClosedAt, now) }
            .map { it.taskType }
            .toSet()

    private fun List<StudyAttempt>.countCompletedToday(taskType: StudyTaskType, now: Long): Int =
        count { it.taskType == taskType && it.countsAsCompleted && isSameDay(it.completedOrClosedAt, now) }

    private fun isSameDay(left: Long, right: Long): Boolean = dayIndex(left) == dayIndex(right)

    private fun dayIndex(time: Long): Long = time / 86_400_000L

    private fun VocabularyDeckStage.nameForCopy(): String =
        when (this) {
            VocabularyDeckStage.CORE_3500 -> "Core 3500"
            VocabularyDeckStage.CORE_PHRASES_1200 -> "Core Phrases"
            VocabularyDeckStage.ADVANCED_2000 -> "Advanced 2000"
            VocabularyDeckStage.AWL_570 -> "AWL 570"
            VocabularyDeckStage.IELTS_TOPICS -> "IELTS Topics"
            VocabularyDeckStage.TOEFL_ACADEMIC -> "TOEFL Academic"
            VocabularyDeckStage.MY_SUBTITLE_WORDS -> "个人字幕词"
        }
}
