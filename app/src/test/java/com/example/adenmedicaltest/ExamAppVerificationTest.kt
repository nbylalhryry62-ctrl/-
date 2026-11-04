package com.example.adenmedicaltest

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.adenmedicaltest.data.model.ExamAnswer
import com.example.adenmedicaltest.data.model.ExamResult
import com.example.adenmedicaltest.data.model.Option
import com.example.adenmedicaltest.data.repository.AuthRepository
import com.example.adenmedicaltest.data.repository.ExamRepository
import com.example.adenmedicaltest.data.repository.SavedExamDraft
import com.example.adenmedicaltest.data.repository.SyllabusRepository
import com.example.adenmedicaltest.ui.auth.DEBUG_SECRET_KEY
import com.example.adenmedicaltest.ui.auth.DEBUG_STORE_PREFIX
import com.example.adenmedicaltest.ui.auth.DEBUG_TOKEN_EXTRA
import com.example.adenmedicaltest.ui.auth.TOKEN_CACHE_STORE_PREFIX
import com.example.adenmedicaltest.ui.auth.configureAppCheck
import com.example.adenmedicaltest.ui.auth.sanitizeLogMessage
import com.example.adenmedicaltest.ui.navigation.Screen
import com.example.adenmedicaltest.ui.viewmodel.AiTutorViewModel
import com.example.adenmedicaltest.ui.viewmodel.MainViewModel
import com.example.adenmedicaltest.ui.viewmodel.QuizGenUiState
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExamAppVerificationTest {

    private lateinit var context: Context
    private lateinit var firestore: FirebaseFirestore
    private lateinit var examRepository: ExamRepository
    private lateinit var syllabusRepository: SyllabusRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("1:528461724036:android:c726c00ff86eda6239c5d1")
                    .setProjectId("gen-lang-client-0986854562")
                    .setApiKey("fake-api-key-for-local-test")
                    .build()
            )
        }
        firestore = FirebaseFirestore.getInstance()
        examRepository = ExamRepository(firestore, context)
        syllabusRepository = SyllabusRepository(context)
    }

    @Test
    fun questionBank_loadsAllSubjectsWithFourOptionsAndScientificExplanations() = runBlocking {
        val biology = examRepository.getQuestionsForSubject("biology")
        val chemistry = examRepository.getQuestionsForSubject("chemistry")
        val english = examRepository.getQuestionsForSubject("english")

        assertEquals(179, biology.size)
        assertEquals(525, chemistry.size)
        assertEquals(438, english.size)

        val allQuestions = biology + chemistry + english
        assertEquals(1142, allQuestions.size)

        allQuestions.forEach { q ->
            assertTrue(q.question.isNotBlank(), "Question text must not be blank for ${q.id}")
            assertEquals(4, q.options.size, "Question ${q.id} must have 4 options")
            assertTrue(q.explanation.isNotBlank(), "Question ${q.id} must have a scientific explanation")
        }
    }

    @Test
    fun localPersistence_savesAndRestoresResultsAndActiveExamDraftOffline() {
        val userId = ExamRepository.LOCAL_OFFLINE_USER_ID
        val sampleResult = ExamResult(
            userId = userId,
            subjectId = "biology",
            subjectName = "الأحياء",
            level = "1",
            score = 20,
            total = 25,
            percentage = 80,
            passed = true,
            timeTakenSeconds = 240,
            answers = listOf(
                ExamAnswer(
                    questionId = "bio-1",
                    question = "سؤال تجريبي",
                    userAnswer = "A",
                    correctAnswer = "A",
                    isCorrect = true,
                    options = listOf(Option("A", "خيار 1"), Option("B", "خيار 2")),
                    explanation = "تفسير علمي مختصر"
                )
            )
        )

        val saved = examRepository.saveLocalResult(userId, sampleResult)
        assertTrue(saved.id.isNotBlank())

        val loadedList = examRepository.getLocalResults(userId)
        assertTrue(loadedList.any { it.id == saved.id && it.percentage == 80 && it.answers.size == 1 })

        // Verify Active Exam Draft save & restore (prevents data loss on app close)
        val draft = SavedExamDraft(
            subjectId = "chemistry",
            level = "2",
            questionIds = listOf("chem-1", "chem-2", "chem-3"),
            currentIndex = 1,
            answers = mapOf("chem-1" to "B"),
            totalDurationSeconds = 600,
            secondsRemaining = 480
        )
        examRepository.saveActiveExamDraft(userId, draft)
        val restoredDraft = examRepository.loadActiveExamDraft(userId)
        assertNotNull(restoredDraft)
        assertEquals("chemistry", restoredDraft.subjectId)
        assertEquals(1, restoredDraft.currentIndex)
        assertEquals("B", restoredDraft.answers["chem-1"])

        examRepository.saveActiveExamDraft(userId, null)
        assertNull(examRepository.loadActiveExamDraft(userId))
    }

    @Test
    fun examFlow_locksAnswerAfterSelectionAndCalculatesScoreAutomatically() = runBlocking {
        val viewModel = MainViewModel(
            examRepository = examRepository,
            syllabusRepository = syllabusRepository,
            authRepository = AuthRepository(firestore),
            currentUserId = ExamRepository.LOCAL_OFFLINE_USER_ID,
            currentUserEmail = "",
            currentUserDisplayName = "طالب اختبار"
        )

        viewModel.startExam("biology", "1")
        var attempts = 0
        while (viewModel.activeExam.value == null && attempts < 100) {
            Thread.sleep(20)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            attempts++
        }

        val active = assertNotNull(viewModel.activeExam.value)
        assertEquals(25, active.questions.size)
        val firstQuestion = active.questions.first()
        val chosenKey = firstQuestion.correctKey
        val wrongKey = firstQuestion.options.first { it.key != chosenKey }.key

        // 1. Select the correct option
        viewModel.answerQuestion(firstQuestion.id, chosenKey)
        assertEquals(chosenKey, viewModel.activeExam.value?.answers?.get(firstQuestion.id))

        // 2. Attempt to change the answer after confirmation -> must be locked!
        viewModel.answerQuestion(firstQuestion.id, wrongKey)
        assertEquals(
            chosenKey,
            viewModel.activeExam.value?.answers?.get(firstQuestion.id),
            "Answer must remain locked after initial confirmation"
        )

        // 3. Submit exam and verify automatic score & percentage calculation
        viewModel.submitExam()
        val result = assertNotNull(viewModel.lastResult.value)
        assertEquals(1, result.score)
        assertEquals(25, result.total)
        assertEquals(4, result.percentage)
        assertTrue(viewModel.currentScreen.value is Screen.Result)
    }

    @Test
    fun appCheck_debugAndReleaseProvidersConfigureProperlyAndClearStaleTokenCache() {
        val persistenceKey = FirebaseApp.getInstance().persistenceKey
        val tokenCachePrefs = context.getSharedPreferences(
            "$TOKEN_CACHE_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE
        )
        // Seed a stale invalid cached AppCheckToken
        tokenCachePrefs.edit().putString("com.google.firebase.appcheck.TOKEN_TYPE", "stale_token").commit()
        assertTrue(tokenCachePrefs.all.isNotEmpty())

        val sampleDebugSecret = "123e4567-e89b-12d3-a456-426614174000"
        val debugIntent = Intent().apply {
            putExtra(DEBUG_TOKEN_EXTRA, sampleDebugSecret)
        }

        // 1. Verify Debug configuration writes secret synchronously & clears stale token cache
        val debugStatus = configureAppCheck(context, debugIntent, isDebugBuild = true)
        assertTrue(debugStatus.isConfigured)
        assertTrue(debugStatus.isDebugMode)
        assertEquals("DebugAppCheckProviderFactory", debugStatus.providerName)
        assertTrue(debugStatus.hasConfiguredDebugSecret)
        assertTrue(debugStatus.clearedStaleTokenCache)
        assertTrue(tokenCachePrefs.all.isEmpty(), "Stale App Check token cache must be cleared")

        val debugPrefs = context.getSharedPreferences(
            "$DEBUG_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE
        )
        assertEquals(sampleDebugSecret, debugPrefs.getString(DEBUG_SECRET_KEY, null))

        // 2. Verify Release configuration uses PlayIntegrityAppCheckProviderFactory and never Debug provider
        val releaseStatus = configureAppCheck(context, null, isDebugBuild = false)
        assertTrue(releaseStatus.isConfigured)
        assertFalse(releaseStatus.isDebugMode)
        assertEquals("PlayIntegrityAppCheckProviderFactory", releaseStatus.providerName)
        assertFalse(releaseStatus.providerName.contains("Debug"))
    }

    @Test
    fun aiTutorViewModel_generatePracticeQuiz_recoversFromInvalidAppCheckTokenAndGeneratesSummaryAnd3Questions() = runBlocking {
        var callCount = 0
        val refreshCalls = mutableListOf<Boolean>()

        val viewModel = AiTutorViewModel(
            structuredQuizGenerator = { _ ->
                callCount++
                if (callCount == 1) {
                    throw RuntimeException("Firebase App Check token is invalid.")
                }
                """
                {
                  "topicTitle": "السيال العصبي والتشابك العصبي",
                  "studySummary": "ينتقل السيال العصبي على هيئة جهد فعل كهربائي على طول المحور العصبي، ثم يتحول في التشابك الكيميائي إلى نواقل عصبية مثل الأسيتيل كولين لعبور الشق التشابكي.",
                  "questions": [
                    {
                      "question": "أي الأيونات التالية يسبب دخوله السريع إلى داخل الليف العصبي مرحلة إزالة الاستقطاب؟",
                      "options": ["أيونات الصوديوم Na+", "أيونات البوتاسيوم K+", "أيونات الكلور Cl-", "أيونات المغنيسيوم Mg2+"],
                      "correctOptionIndex": 0,
                      "explanation": "تفتح قنوات الصوديوم المبوبة بالجهد فيتدفق Na+ إلى الداخل مسبباً إزالة الاستقطاب."
                    },
                    {
                      "question": "ما الأيون المسؤول عن تحفيز اندماج حويصلات النواقل العصبية بالغشاء قبل التشابكي؟",
                      "options": ["الكالسيوم Ca2+", "الصوديوم Na+", "البوتاسيوم K+", "الحديد Fe2+"],
                      "correctOptionIndex": 0,
                      "explanation": "وصول السيال العصبي لنهاية المحور يفتح قنوات الكالسيوم Ca2+ مما يحفز تحرر الناقل العصبي."
                    },
                    {
                      "question": "ما الإنزيم الذي يحطم الأسيتيل كولين في الشق التشابكي لإنهاء التنبيه العصبي؟",
                      "options": ["كولين إستراز", "الأميليز", "الببسين", "الكاتاليز"],
                      "correctOptionIndex": 0,
                      "explanation": "يعمل إنزيم كولين إستراز على تفكيك الأسيتيل كولين إلى كولين وحمض خليك لإعادة الغشاء لحالة الراحة."
                    }
                  ]
                }
                """.trimIndent()
            },
            tokenRefresher = { forceRefresh ->
                refreshCalls.add(forceRefresh)
                true
            }
        )

        viewModel.generatePracticeQuiz("السيال العصبي والتشابك العصبي", "الأحياء")

        var attempts = 0
        while (viewModel.quizGenState.value !is QuizGenUiState.Success && attempts < 100) {
            Thread.sleep(20)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            attempts++
        }

        val state = viewModel.quizGenState.value
        assertTrue(state is QuizGenUiState.Success, "Expected QuizGenUiState.Success after App Check token refresh retry")
        assertEquals(2, callCount, "Should retry once after refreshing invalid App Check token")
        assertEquals(listOf(false, true), refreshCalls, "Should pre-check token (false) then force-refresh token (true)")
        assertEquals("السيال العصبي والتشابك العصبي", state.topicTitle)
        assertTrue(state.studySummary.contains("السيال العصبي"))
        assertEquals(3, state.questions.size)
        state.questions.forEach { q ->
            assertEquals(4, q.options.size)
            assertTrue(q.explanation.isNotBlank())
        }
    }

    @Test
    fun sanitizeLogMessage_redactsSecretTokensAndPreservesTechnicalContext() {
        val raw = "Firebase App Check failed for token 123e4567-e89b-12d3-a456-426614174000 and jwt eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMifQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        val sanitized = sanitizeLogMessage(raw)
        assertFalse(sanitized.contains("123e4567-e89b-12d3-a456-426614174000"))
        assertFalse(sanitized.contains("eyJhbGciOiJIUzI1NiJ9"))
        assertTrue(sanitized.contains("[REDACTED_DEBUG_TOKEN]"))
        assertTrue(sanitized.contains("[REDACTED_JWT]"))
    }
}
