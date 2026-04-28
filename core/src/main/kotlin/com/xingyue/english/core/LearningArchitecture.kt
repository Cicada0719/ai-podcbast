package com.xingyue.english.core

enum class LearningNeed {
    MATERIAL_INPUT,
    COMPREHENSIBLE_LISTENING,
    LEXICAL_RETRIEVAL,
    SPACED_REVIEW,
    OUTPUT_PRACTICE,
    FEEDBACK_AND_SUPPORT,
    REFLECTION
}

enum class CapabilityPriority {
    REQUIRED,
    SUPPORTING,
    OPTIONAL
}

enum class LearningSurface {
    HOME,
    IMPORT,
    PLAYER,
    STUDY_PATH,
    PRACTICE_LAB,
    WORDS,
    PROFILE,
    ANALYTICS
}

data class LearningCapability(
    val id: String,
    val name: String,
    val need: LearningNeed,
    val priority: CapabilityPriority,
    val primarySurface: LearningSurface,
    val userOutcome: String,
    val evidenceSignal: String
)

object EnglishLearningArchitecture {
    val capabilities: List<LearningCapability> = listOf(
        LearningCapability(
            id = "material-import",
            name = "学习素材导入",
            need = LearningNeed.MATERIAL_INPUT,
            priority = CapabilityPriority.REQUIRED,
            primarySurface = LearningSurface.IMPORT,
            userOutcome = "用户能把视频、音频、字幕、文档变成可学习材料。",
            evidenceSignal = "内容状态、字幕条数、失败原因"
        ),
        LearningCapability(
            id = "bilingual-player",
            name = "双语字幕精听",
            need = LearningNeed.COMPREHENSIBLE_LISTENING,
            priority = CapabilityPriority.REQUIRED,
            primarySurface = LearningSurface.PLAYER,
            userOutcome = "用户能按原片时间线逐句听懂材料。",
            evidenceSignal = "播放位置、当前字幕、重播次数、精听完成"
        ),
        LearningCapability(
            id = "manual-word-capture",
            name = "手动收词",
            need = LearningNeed.LEXICAL_RETRIEVAL,
            priority = CapabilityPriority.REQUIRED,
            primarySurface = LearningSurface.WORDS,
            userOutcome = "用户只保存自己确认需要复习的词。",
            evidenceSignal = "保存来源、上下文、高亮状态"
        ),
        LearningCapability(
            id = "fsrs-review",
            name = "间隔复习",
            need = LearningNeed.SPACED_REVIEW,
            priority = CapabilityPriority.REQUIRED,
            primarySurface = LearningSurface.STUDY_PATH,
            userOutcome = "用户优先清理到期复习和错词，降低遗忘。",
            evidenceSignal = "到期数、四档反馈、下一次复习时间"
        ),
        LearningCapability(
            id = "retrieval-practice",
            name = "主动回忆练习",
            need = LearningNeed.OUTPUT_PRACTICE,
            priority = CapabilityPriority.REQUIRED,
            primarySurface = LearningSurface.PRACTICE_LAB,
            userOutcome = "用户通过打字、听写、逐句默写把输入转成可回忆内容。",
            evidenceSignal = "练习会话、正确率、完成题数"
        ),
        LearningCapability(
            id = "tts-dictionary",
            name = "发音和词典支持",
            need = LearningNeed.FEEDBACK_AND_SUPPORT,
            priority = CapabilityPriority.SUPPORTING,
            primarySurface = LearningSurface.PROFILE,
            userOutcome = "用户能听单词/句子发音，并获得可理解释义。",
            evidenceSignal = "发音开关、缓存命中、查词记录"
        ),
        LearningCapability(
            id = "study-analytics",
            name = "学习反馈",
            need = LearningNeed.REFLECTION,
            priority = CapabilityPriority.SUPPORTING,
            primarySurface = LearningSurface.ANALYTICS,
            userOutcome = "用户知道今天完成了什么，下一步该做什么。",
            evidenceSignal = "今日完成、听力分钟、正确率、复习压力"
        ),
        LearningCapability(
            id = "light-games",
            name = "轻量词汇游戏",
            need = LearningNeed.LEXICAL_RETRIEVAL,
            priority = CapabilityPriority.OPTIONAL,
            primarySurface = LearningSurface.PRACTICE_LAB,
            userOutcome = "用户在低压力场景里巩固词形和释义。",
            evidenceSignal = "游戏局记录、胜负、尝试次数"
        )
    )

    fun requiredCapabilities(): List<LearningCapability> =
        capabilities.filter { it.priority == CapabilityPriority.REQUIRED }

    fun surfacePlan(): Map<LearningSurface, List<LearningCapability>> =
        capabilities.groupBy { it.primarySurface }

    fun nextRequiredSurface(completedCapabilityIds: Set<String>): LearningSurface? =
        requiredCapabilities().firstOrNull { it.id !in completedCapabilityIds }?.primarySurface

    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        val requiredNeeds = setOf(
            LearningNeed.MATERIAL_INPUT,
            LearningNeed.COMPREHENSIBLE_LISTENING,
            LearningNeed.LEXICAL_RETRIEVAL,
            LearningNeed.SPACED_REVIEW,
            LearningNeed.OUTPUT_PRACTICE
        )
        val coveredNeeds = requiredCapabilities().map { it.need }.toSet()
        val missingNeeds = requiredNeeds - coveredNeeds
        if (missingNeeds.isNotEmpty()) errors += "缺少必需学习需求: ${missingNeeds.joinToString()}"
        if (requiredCapabilities().any { it.primarySurface == LearningSurface.ANALYTICS }) {
            errors += "统计不能成为必需主路径，应作为反馈层。"
        }
        if (capabilities.any { it.id == "light-games" && it.priority != CapabilityPriority.OPTIONAL }) {
            errors += "游戏只能作为可选巩固，不能阻塞今日路径。"
        }
        return errors
    }
}
