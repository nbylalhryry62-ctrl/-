package com.example.adenmedicaltest.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adenmedicaltest.data.model.ExamAnswer
import com.example.adenmedicaltest.data.model.ExamResult
import com.example.adenmedicaltest.data.model.Question
import com.example.adenmedicaltest.data.model.Subject
import com.example.adenmedicaltest.data.model.SubjectLevel
import com.example.adenmedicaltest.data.model.SyllabusData
import com.example.adenmedicaltest.data.model.UserProfile
import com.example.adenmedicaltest.data.repository.AuthRepository
import com.example.adenmedicaltest.data.repository.ExamRepository
import com.example.adenmedicaltest.data.repository.SavedExamDraft
import com.example.adenmedicaltest.data.repository.SyllabusRepository
import com.example.adenmedicaltest.ui.navigation.Screen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class ActiveExamState(
    val subject: Subject,
    val level: String,
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val answers: Map<String, String> = emptyMap(),
    val totalDurationSeconds: Int = 600,
    val secondsRemaining: Int = 600,
    val isTimerRunning: Boolean = false,
    val isSubmitted: Boolean = false
) {
    val currentQuestion: Question?
        get() = questions.getOrNull(currentIndex)

    val answeredCount: Int
        get() = answers.size

    val progress: Float
        get() = if (questions.isNotEmpty()) (currentIndex + 1).toFloat() / questions.size else 0f

    val timeProgress: Float
        get() = if (totalDurationSeconds > 0) {
            (secondsRemaining.toFloat() / totalDurationSeconds.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val formattedTime: String
        get() {
            val mins = secondsRemaining / 60
            val secs = secondsRemaining % 60
            return "%02d:%02d".format(mins, secs)
        }

    val formattedTotalTime: String
        get() {
            val mins = totalDurationSeconds / 60
            val secs = totalDurationSeconds % 60
            return "%02d:%02d".format(mins, secs)
        }
}

class MainViewModel(
    private val examRepository: ExamRepository,
    private val syllabusRepository: SyllabusRepository,
    private val authRepository: AuthRepository,
    private val currentUserId: String,
    private val currentUserEmail: String,
    private val currentUserDisplayName: String?
) : ViewModel() {

    val subjects: List<Subject> = examRepository.subjects

    private val defaultProfile = UserProfile(
        id = currentUserId,
        userId = currentUserId,
        email = currentUserEmail,
        username = currentUserEmail.substringBefore("@").ifBlank { "طالب الطب" },
        fullName = currentUserDisplayName ?: "طالب كلية الطب",
        role = "student"
    )

    val currentUser: StateFlow<UserProfile?> = (
        if (currentUserId == ExamRepository.LOCAL_OFFLINE_USER_ID) {
            flowOf(defaultProfile)
        } else {
            authRepository.observeUserProfile(currentUserId)
                .catch { error ->
                    Log.w(TAG, "Error observing user profile", error)
                    emit(defaultProfile)
                }
        }
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = defaultProfile
    )

    // Two-tier Flow error handling for Firestore snapshot stream with local fallback
    val resultsState: StateFlow<UiState<List<ExamResult>>> = examRepository.observeResults(currentUserId)
        .map<List<ExamResult>, UiState<List<ExamResult>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing exam results, falling back to local storage", error)
            val localFallback = examRepository.getLocalResults(currentUserId)
            emit(UiState.Success(localFallback))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(examRepository.getLocalResults(currentUserId))
        )

    val results: StateFlow<List<ExamResult>> = resultsState
        .map { state ->
            when (state) {
                is UiState.Success -> state.data
                else -> examRepository.getLocalResults(currentUserId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = examRepository.getLocalResults(currentUserId)
        )

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _activeExam = MutableStateFlow<ActiveExamState?>(null)
    val activeExam: StateFlow<ActiveExamState?> = _activeExam.asStateFlow()

    private val _savedDraft = MutableStateFlow<SavedExamDraft?>(null)
    val savedDraft: StateFlow<SavedExamDraft?> = _savedDraft.asStateFlow()

    private val _lastResult = MutableStateFlow<ExamResult?>(null)
    val lastResult: StateFlow<ExamResult?> = _lastResult.asStateFlow()

    private val _wasAutoSubmittedOnTimeout = MutableStateFlow(false)
    val wasAutoSubmittedOnTimeout: StateFlow<Boolean> = _wasAutoSubmittedOnTimeout.asStateFlow()

    private val _subjectLevels = MutableStateFlow<List<SubjectLevel>>(emptyList())
    val subjectLevels: StateFlow<List<SubjectLevel>> = _subjectLevels.asStateFlow()

    private val _selectedLevelsSubject = MutableStateFlow<Subject?>(null)
    val selectedLevelsSubject: StateFlow<Subject?> = _selectedLevelsSubject.asStateFlow()

    private val _syllabusMap = MutableStateFlow<Map<String, SyllabusData>>(emptyMap())
    val syllabusMap: StateFlow<Map<String, SyllabusData>> = _syllabusMap.asStateFlow()

    private val _errorBanner = MutableStateFlow<String?>(null)
    val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

    private var timerJob: Job? = null
    private var examStartTime: Long = 0L

    init {
        loadSyllabus()
        ensureProfileInitialized()
        _savedDraft.value = examRepository.loadActiveExamDraft(currentUserId)
    }

    private fun ensureProfileInitialized() {
        if (currentUserId == ExamRepository.LOCAL_OFFLINE_USER_ID) return
        viewModelScope.launch {
            authRepository.ensureUserProfile(
                userId = currentUserId,
                email = currentUserEmail,
                displayName = currentUserDisplayName
            )
        }
    }

    private fun loadSyllabus() {
        viewModelScope.launch {
            _syllabusMap.value = syllabusRepository.getAllSyllabus()
        }
    }

    fun clearErrorBanner() {
        _errorBanner.value = null
    }

    fun navigateTo(screen: Screen) {
        if (screen !is Screen.Exam) {
            timerJob?.cancel()
        }
        _currentScreen.value = screen
    }

    fun openResultReview(result: ExamResult) {
        _wasAutoSubmittedOnTimeout.value = false
        _lastResult.value = result
        _currentScreen.value = Screen.Result(result)
    }

    fun cancelExam() {
        timerJob?.cancel()
        _activeExam.value = null
        examRepository.saveActiveExamDraft(currentUserId, null)
        _savedDraft.value = null
        _currentScreen.value = Screen.Dashboard
    }

    fun discardSavedDraft() {
        examRepository.saveActiveExamDraft(currentUserId, null)
        _savedDraft.value = null
    }

    fun resumeSavedDraft() {
        val draft = _savedDraft.value ?: return
        val subject = subjects.find { it.id == draft.subjectId } ?: subjects.first()
        viewModelScope.launch {
            val allQuestions = examRepository.getQuestionsForSubject(draft.subjectId)
            val byId = allQuestions.associateBy { it.id }
            val restoredQuestions = draft.questionIds.mapNotNull { byId[it] }
            if (restoredQuestions.isEmpty()) {
                discardSavedDraft()
                return@launch
            }
            _wasAutoSubmittedOnTimeout.value = false
            val state = ActiveExamState(
                subject = subject,
                level = draft.level,
                questions = restoredQuestions,
                currentIndex = draft.currentIndex.coerceIn(0, restoredQuestions.size - 1),
                answers = draft.answers,
                totalDurationSeconds = draft.totalDurationSeconds,
                secondsRemaining = draft.secondsRemaining,
                isTimerRunning = true,
                isSubmitted = false
            )
            _activeExam.value = state
            val alreadyElapsed = (draft.totalDurationSeconds - draft.secondsRemaining).coerceAtLeast(0)
            examStartTime = System.currentTimeMillis() - (alreadyElapsed * 1000L)
            _currentScreen.value = Screen.Exam(subject.id, draft.level)
            startTimer()
        }
    }

    private fun persistDraft(state: ActiveExamState?) {
        if (state == null || state.isSubmitted || state.questions.isEmpty()) {
            examRepository.saveActiveExamDraft(currentUserId, null)
            _savedDraft.value = null
            return
        }
        val draft = SavedExamDraft(
            subjectId = state.subject.id,
            level = state.level,
            questionIds = state.questions.map { it.id },
            currentIndex = state.currentIndex,
            answers = state.answers,
            totalDurationSeconds = state.totalDurationSeconds,
            secondsRemaining = state.secondsRemaining
        )
        examRepository.saveActiveExamDraft(currentUserId, draft)
        _savedDraft.value = draft
    }

    fun openSubjectLevels(subjectId: String) {
        val subject = subjects.find { it.id == subjectId } ?: subjects.first()
        _selectedLevelsSubject.value = subject
        viewModelScope.launch {
            _subjectLevels.value = examRepository.getSubjectLevels(subjectId)
            _currentScreen.value = Screen.SubjectLevels(subjectId)
        }
    }

    fun startExam(subjectId: String, level: String = "1") {
        val subject = subjects.find { it.id == subjectId } ?: subjects.first()
        viewModelScope.launch {
            val questions = when (level) {
                "random" -> examRepository.getRandomQuestions(subjectId, 25)
                "all" -> examRepository.getQuestionsForSubject(subjectId)
                else -> {
                    val levels = examRepository.getSubjectLevels(subjectId)
                    levels.find { it.id == level }?.questionsPool
                        ?: examRepository.getRandomQuestions(subjectId, 25)
                }
            }

            val shuffledQuestions = questions.shuffled().map { q ->
                q.copy(options = q.options.shuffled())
            }

            val totalDurationSeconds = if (level == "all") {
                maxOf(subject.durationMinutes * 60, shuffledQuestions.size * 24)
            } else {
                (subject.durationMinutes * 60).coerceAtLeast(60)
            }

            _wasAutoSubmittedOnTimeout.value = false
            val newState = ActiveExamState(
                subject = subject,
                level = level,
                questions = shuffledQuestions,
                currentIndex = 0,
                answers = emptyMap(),
                totalDurationSeconds = totalDurationSeconds,
                secondsRemaining = totalDurationSeconds,
                isTimerRunning = true,
                isSubmitted = false
            )
            _activeExam.value = newState
            persistDraft(newState)
            examStartTime = System.currentTimeMillis()
            _currentScreen.value = Screen.Exam(subjectId, level)
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _activeExam.value ?: break
                if (current.isSubmitted || !current.isTimerRunning) break
                val elapsedSeconds = ((System.currentTimeMillis() - examStartTime) / 1000).toInt()
                val remaining = (current.totalDurationSeconds - elapsedSeconds).coerceAtMost(current.secondsRemaining - 1)
                if (remaining <= 0) {
                    _activeExam.value = current.copy(
                        secondsRemaining = 0,
                        isTimerRunning = false
                    )
                    submitExam(autoSubmittedByTimer = true)
                    break
                } else {
                    val updated = current.copy(secondsRemaining = remaining)
                    _activeExam.value = updated
                    if (remaining % 5 == 0) {
                        persistDraft(updated)
                    }
                }
            }
        }
    }

    fun answerQuestion(questionId: String, selectedKey: String) {
        val current = _activeExam.value ?: return
        if (current.isSubmitted) return
        // Lock answer once selected: prevent changing the answer after immediate correction is shown
        if (current.answers.containsKey(questionId)) return
        val updatedAnswers = current.answers.toMutableMap()
        updatedAnswers[questionId] = selectedKey
        val nextState = current.copy(answers = updatedAnswers)
        _activeExam.value = nextState
        persistDraft(nextState)
    }

    fun nextQuestion() {
        val current = _activeExam.value ?: return
        if (current.currentIndex < current.questions.size - 1) {
            val nextState = current.copy(currentIndex = current.currentIndex + 1)
            _activeExam.value = nextState
            persistDraft(nextState)
        }
    }

    fun prevQuestion() {
        val current = _activeExam.value ?: return
        if (current.currentIndex > 0) {
            val prevState = current.copy(currentIndex = current.currentIndex - 1)
            _activeExam.value = prevState
            persistDraft(prevState)
        }
    }

    fun submitExam(autoSubmittedByTimer: Boolean = false) {
        val current = _activeExam.value ?: return
        if (current.isSubmitted) return
        timerJob?.cancel()

        _wasAutoSubmittedOnTimeout.value = autoSubmittedByTimer

        val timeTakenSeconds = if (autoSubmittedByTimer) {
            current.totalDurationSeconds
        } else {
            ((System.currentTimeMillis() - examStartTime) / 1000).toInt()
                .coerceIn(1, current.totalDurationSeconds)
        }

        val answersDetails = current.questions.map { q ->
            val userAns = current.answers[q.id]
            ExamAnswer(
                questionId = q.id,
                question = q.question,
                userAnswer = userAns,
                correctAnswer = q.correctKey,
                isCorrect = userAns == q.correctKey,
                options = q.options,
                explanation = q.explanation
            )
        }

        val correctCount = answersDetails.count { it.isCorrect }
        val totalCount = current.questions.size
        val percentage = if (totalCount > 0) (correctCount * 100) / totalCount else 0
        val passed = percentage >= 50

        val result = ExamResult(
            userId = currentUserId,
            subjectId = current.subject.id,
            subjectName = current.subject.name,
            level = current.level,
            score = correctCount,
            total = totalCount,
            percentage = percentage,
            passed = passed,
            timeTakenSeconds = timeTakenSeconds,
            answers = answersDetails
        )

        val submittedState = current.copy(
            secondsRemaining = if (autoSubmittedByTimer) 0 else current.secondsRemaining,
            isTimerRunning = false,
            isSubmitted = true
        )
        _activeExam.value = submittedState
        persistDraft(null)
        _lastResult.value = result
        _currentScreen.value = Screen.Result(result)

        viewModelScope.launch {
            val saved = examRepository.saveResult(result)
            saved.onSuccess { docId ->
                val updated = result.copy(id = docId)
                _lastResult.value = updated
            }.onFailure {
                // Result is already persisted locally on device by ExamRepository.saveResult()
                _errorBanner.value = "تم حفظ النتيجة محلياً على جهازك (تعذرت المزامنة السحابية حالياً)"
            }
        }
    }

    fun retryCurrentExam() {
        val result = _lastResult.value ?: return
        startExam(result.subjectId, result.level)
    }

    fun deleteResult(id: String) {
        viewModelScope.launch {
            examRepository.deleteResult(id, currentUserId).onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "تعذر حذف النتيجة"
            }
        }
    }

    fun updateProfileFullName(fullName: String) {
        if (currentUserId == ExamRepository.LOCAL_OFFLINE_USER_ID) return
        viewModelScope.launch {
            authRepository.updateFullName(fullName).onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "تعذر تحديث الاسم الكامل"
            }
        }
    }

    private companion object {
        const val TAG = "MainViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
