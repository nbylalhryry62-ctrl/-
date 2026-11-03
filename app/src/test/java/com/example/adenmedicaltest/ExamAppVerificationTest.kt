package com.example.adenmedicaltest

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.adenmedicaltest.data.model.ExamAnswer
import com.example.adenmedicaltest.data.model.ExamResult
import com.example.adenmedicaltest.data.model.Option
import com.example.adenmedicaltest.data.repository.AuthRepository
import com.example.adenmedicaltest.data.repository.ExamRepository
import com.example.adenmedicaltest.data.repository.SavedExamDraft
import com.example.adenmedicaltest.data.repository.SyllabusRepository
import com.example.adenmedicaltest.ui.navigation.Screen
import com.example.adenmedicaltest.ui.viewmodel.MainViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
                    .setApplicationId("com.aistudio.adenmedicaltest.vqkrzx")
                    .setProjectId("demo-aden-medical-test")
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
        // Wait for IO coroutine + Main Looper continuation in Robolectric
        var attempts = 0
        while (viewModel.activeExam.value == null && attempts < 100) {
            Thread.sleep(20)
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
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
}
