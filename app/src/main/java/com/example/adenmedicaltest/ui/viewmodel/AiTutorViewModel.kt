package com.example.adenmedicaltest.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adenmedicaltest.data.model.ExamAnswer
import com.example.adenmedicaltest.data.model.Question
import com.example.adenmedicaltest.ui.auth.ensureAppCheckTokenReady
import com.example.adenmedicaltest.ui.auth.sanitizeLogMessage
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiTutorViewModel"
private const val GEMINI_MODEL_NAME = "gemini-3.8-flash"

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val text: String) : AiUiState
    data class Success(val text: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

data class AiGeneratedQuestion(
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String
)

sealed interface QuizGenUiState {
    data object Idle : QuizGenUiState
    data object Loading : QuizGenUiState
    data class Success(
        val topicTitle: String,
        val studySummary: String,
        val questions: List<AiGeneratedQuestion>
    ) : QuizGenUiState
    data class Error(val message: String) : QuizGenUiState
}

/**
 * Determines whether an exception thrown by Firebase AI Logic is caused by an invalid or expired
 * Firebase App Check token so that a forced App Check token refresh and retry can be performed.
 */
fun isAppCheckInvalidException(error: Throwable): Boolean {
    val combinedMessage = buildString {
        append(error.message.orEmpty())
        error.cause?.message?.let {
            append(" | ")
            append(it)
        }
    }.lowercase()

    return combinedMessage.contains("app check") ||
        combinedMessage.contains("appcheck") ||
        combinedMessage.contains("token is invalid") ||
        combinedMessage.contains("unauthenticated") ||
        combinedMessage.contains("permission_denied") ||
        combinedMessage.contains("403")
}

/**
 * Maps technical exceptions to clear, user-friendly Arabic error messages without leaking secrets.
 */
fun formatUserFacingAiError(error: Throwable, fallbackMessage: String): String {
    return when {
        error is TimeoutCancellationException || error is SocketTimeoutException ->
            "انتهت مهلة الاتصال بخدمة الذكاء الاصطناعي (60 ثانية). يرجى التحقق من سرعة الإنترنت والمحاولة مجدداً."

        isAppCheckInvalidException(error) ->
            "تعذر التحقق من رمز حماية التطبيق (Firebase App Check). تم تحديث جلسة الحماية تلقائياً؛ يرجى الضغط على «إعادة المحاولة»، أو التأكد من تسجيل رمز التطوير (Debug Token) في إعدادات Firebase App Check."

        error is UnknownHostException || error is IOException ||
            error.message?.contains("network", ignoreCase = true) == true ||
            error.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
            "تعذر الاتصال بخدمة الذكاء الاصطناعي (Gemini). يرجى التحقق من اتصال الإنترنت ثم إعادة المحاولة."

        else -> fallbackMessage
    }
}

class AiTutorViewModel(
    private val structuredQuizGenerator: (suspend (String) -> String?)? = null,
    private val tutorStreamGenerator: ((String) -> Flow<String>)? = null,
    private val tokenRefresher: suspend (Boolean) -> Boolean = { force ->
        ensureAppCheckTokenReady(forceRefresh = force)
    }
) : ViewModel() {

    private fun createTutorModel(): GenerativeModel {
        return Firebase.ai.generativeModel(GEMINI_MODEL_NAME)
    }

    // Structured JSON schema using Schema.obj (not Schema.object)
    private val practiceQuizSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "topicTitle" to Schema.string(description = "عنوان الموضوع العلمي باللغة العربية"),
                "studySummary" to Schema.string(description = "ملخص علمي مركز للنقاط الأساسية في الموضوع لطلاب القبول بكلية الطب"),
                "questions" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "question" to Schema.string(description = "نص السؤال العلمي أو الطبي"),
                            "options" to Schema.array(
                                items = Schema.string(),
                                description = "أربعة خيارات للإجابة"
                            ),
                            "correctOptionIndex" to Schema.integer(description = "رقم الخيار الصحيح من 0 إلى 3"),
                            "explanation" to Schema.string(description = "التعليل العلمي الدقيق للإجابة الصحيحة")
                        )
                    ),
                    description = "قائمة من 3 أسئلة تدريبية لاختبار القبول"
                )
            )
        )
    }

    private fun createStructuredQuizModel(): GenerativeModel {
        return Firebase.ai.generativeModel(
            modelName = GEMINI_MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = practiceQuizSchema
            }
        )
    }

    private suspend fun invokeStructuredQuizModel(prompt: String): String? {
        val customGen = structuredQuizGenerator
        if (customGen != null) {
            return customGen(prompt)
        }
        return createStructuredQuizModel().generateContent(prompt).text
    }

    private fun invokeTutorStreamModel(prompt: String): Flow<String> {
        val customStream = tutorStreamGenerator
        if (customStream != null) {
            return customStream(prompt)
        }
        return createTutorModel().generateContentStream(prompt).map { it.text.orEmpty() }
    }

    private fun logSanitizedError(operation: String, error: Throwable) {
        val safeMsg = sanitizeLogMessage(error.message)
        val safeCause = error.cause?.let { "${it.javaClass.simpleName}: ${sanitizeLogMessage(it.message)}" } ?: "none"
        Log.e(
            TAG,
            "AI operation [$operation] failed | type=${error.javaClass.simpleName} | message=$safeMsg | cause=$safeCause"
        )
    }

    // 1. Interactive AI Tutor Streaming State
    private val _tutorState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val tutorState: StateFlow<AiUiState> = _tutorState.asStateFlow()

    // 2. Structured Practice Quiz State
    private val _quizGenState = MutableStateFlow<QuizGenUiState>(QuizGenUiState.Idle)
    val quizGenState: StateFlow<QuizGenUiState> = _quizGenState.asStateFlow()

    // 3. Per-Question Explanation State (used on ResultScreen)
    private val _explanationMap = MutableStateFlow<Map<String, AiUiState>>(emptyMap())
    val explanationMap: StateFlow<Map<String, AiUiState>> = _explanationMap.asStateFlow()

    // 4. Exam Hint State (used on ExamScreen)
    private val _hintMap = MutableStateFlow<Map<String, AiUiState>>(emptyMap())
    val hintMap: StateFlow<Map<String, AiUiState>> = _hintMap.asStateFlow()

    private suspend fun collectStreamWithAppCheckRetry(
        operationName: String,
        prompt: String,
        onChunk: (String) -> Unit
    ) {
        tokenRefresher(false)
        try {
            withTimeout(60_000L) {
                invokeTutorStreamModel(prompt).collect { chunkText ->
                    onChunk(chunkText)
                }
            }
        } catch (firstError: Exception) {
            if (firstError is CancellationException && firstError !is TimeoutCancellationException) {
                throw firstError
            }
            if (isAppCheckInvalidException(firstError)) {
                Log.w(
                    TAG,
                    "App Check token rejected during [$operationName] (${firstError.javaClass.simpleName}). " +
                        "Refreshing App Check token and retrying once..."
                )
                tokenRefresher(true)
                withTimeout(60_000L) {
                    invokeTutorStreamModel(prompt).collect { chunkText ->
                        onChunk(chunkText)
                    }
                }
            } else {
                throw firstError
            }
        }
    }

    fun askMedicalTutorStream(userQuestion: String, selectedSubject: String = "عام") {
        if (userQuestion.isBlank()) return
        viewModelScope.launch {
            _tutorState.value = AiUiState.Loading
            val accumulatedText = StringBuilder()
            val prompt = """
                أنت المعلم الطبي الذكي لطلاب اختبار القبول والمفاضلة في كلية الطب والعلوم الصحية بجامعة عدن.
                المادة: $selectedSubject
                سؤال الطالب: $userQuestion
                
                قدم إجابة علمية دقيقة ومبسطة باللغة العربية (مع المصطلحات الطبية الإنجليزية عند الحاجة)،
                واربط الشرح بنوعية أسئلة اختبارات القبول الطبية في الأحياء والكيمياء واللغة الإنجليزية، دون ذكر أي أسماء أشخاص في الأمثلة أو الأسئلة.
            """.trimIndent()

            try {
                collectStreamWithAppCheckRetry(
                    operationName = "askMedicalTutorStream",
                    prompt = prompt
                ) { chunkText ->
                    accumulatedText.append(chunkText)
                    _tutorState.value = AiUiState.Streaming(accumulatedText.toString())
                }
                if (accumulatedText.isNotBlank()) {
                    _tutorState.value = AiUiState.Success(accumulatedText.toString())
                } else {
                    _tutorState.value = AiUiState.Error("لم يُرجع النموذج أي إجابة. يرجى المحاولة مرة أخرى.")
                }
            } catch (e: TimeoutCancellationException) {
                logSanitizedError("askMedicalTutorStream", e)
                _tutorState.value = AiUiState.Error(formatUserFacingAiError(e, "انتهت مهلة الاتصال بعد 60 ثانية."))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logSanitizedError("askMedicalTutorStream", e)
                _tutorState.value = AiUiState.Error(
                    formatUserFacingAiError(e, "تعذر توليد الإجابة من المساعد الذكي. يرجى المحاولة مرة أخرى.")
                )
            }
        }
    }

    fun generatePracticeQuiz(topic: String, subjectName: String) {
        if (topic.isBlank()) return
        viewModelScope.launch {
            _quizGenState.value = QuizGenUiState.Loading
            val prompt = """
                أنشئ ملخصاً علمياً مركزاً و 3 أسئلة اختيار من متعدد (مع 4 خيارات لكل سؤال والتعليل العلمي)
                لطلاب اختبار القبول في كلية الطب جامعة عدن في مادة ($subjectName) حول موضوع: "$topic".
                تأكد من صياغة الأسئلة بشكل علمي بحت دون ذكر أي أسماء أشخاص.
            """.trimIndent()

            try {
                // Ensure App Check provider and token are ready before calling Gemini
                tokenRefresher(false)

                val jsonString = try {
                    withTimeout(60_000L) {
                        invokeStructuredQuizModel(prompt)
                    }
                } catch (firstError: Exception) {
                    if (firstError is CancellationException && firstError !is TimeoutCancellationException) {
                        throw firstError
                    }
                    if (isAppCheckInvalidException(firstError)) {
                        Log.w(
                            TAG,
                            "App Check token rejected during [generatePracticeQuiz] (${firstError.javaClass.simpleName}). " +
                                "Forcing App Check token refresh and retrying once..."
                        )
                        // Force-refresh App Check token and clear stale cached token
                        tokenRefresher(true)
                        withTimeout(60_000L) {
                            invokeStructuredQuizModel(prompt)
                        }
                    } else {
                        throw firstError
                    }
                } ?: throw IllegalStateException("Model returned empty JSON text.")

                // Parse directly without regex or markdown code-fence stripping
                val json = JSONObject(jsonString)
                val topicTitle = json.getString("topicTitle")
                val studySummary = json.getString("studySummary")
                val questionsArray = json.getJSONArray("questions")
                val parsedQuestions = (0 until questionsArray.length()).map { i ->
                    val qObj = questionsArray.getJSONObject(i)
                    val optsArray = qObj.getJSONArray("options")
                    val opts = (0 until optsArray.length()).map { j -> optsArray.getString(j) }
                    AiGeneratedQuestion(
                        question = qObj.getString("question"),
                        options = opts,
                        correctOptionIndex = qObj.optInt("correctOptionIndex", 0)
                            .coerceIn(0, (opts.size - 1).coerceAtLeast(0)),
                        explanation = qObj.getString("explanation")
                    )
                }

                _quizGenState.value = QuizGenUiState.Success(
                    topicTitle = topicTitle,
                    studySummary = studySummary,
                    questions = parsedQuestions
                )
            } catch (e: TimeoutCancellationException) {
                logSanitizedError("generatePracticeQuiz", e)
                _quizGenState.value = QuizGenUiState.Error(
                    formatUserFacingAiError(e, "انتهت مهلة توليد الاختبار التدريبي بعد 60 ثانية.")
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logSanitizedError("generatePracticeQuiz", e)
                _quizGenState.value = QuizGenUiState.Error(
                    formatUserFacingAiError(e, "تعذر توليد الملخص والأسئلة التدريبية حالياً. يرجى المحاولة مرة أخرى.")
                )
            }
        }
    }

    fun explainQuestionSelectionStream(subjectName: String, question: Question, selectedKey: String) {
        val answer = ExamAnswer(
            questionId = question.id,
            question = question.question,
            userAnswer = selectedKey,
            correctAnswer = question.correctKey,
            isCorrect = selectedKey == question.correctKey,
            options = question.options,
            explanation = question.explanation
        )
        explainAnswerStream(subjectName, answer)
    }

    fun explainAnswerStream(subjectName: String, answer: ExamAnswer) {
        val key = answer.questionId.ifBlank { answer.question }
        viewModelScope.launch {
            _explanationMap.value = _explanationMap.value + (key to AiUiState.Loading)
            val accumulated = StringBuilder()
            val optionsFormatted = answer.options.joinToString("\n") { "${it.key}: ${it.text}" }
            val userOptText = answer.options.find { it.key == answer.userAnswer }?.text.orEmpty()
            val correctOptText = answer.options.find { it.key == answer.correctAnswer }?.text.orEmpty()
            val statusLabel = if (answer.isCorrect) "إجابة الطالب صحيحة" else "إجابة الطالب خاطئة"
            val prompt = """
                اشرح باختصار ووضوح لطالب طب في مادة ($subjectName) التصحيح العلمي لهذا السؤال:
                السؤال: ${answer.question}
                الخيارات:
                $optionsFormatted
                اختيار الطالب: ${answer.userAnswer ?: "لم يجب"} ($userOptText) — [$statusLabel]
                الخيار الصحيح المعتمد: ${answer.correctAnswer} ($correctOptText)
                
                اشرح في 3 أسطر واضحة ومباشرة:
                1) لماذا الخيار الصحيح (${answer.correctAnswer}: $correctOptText) هو الإجابة الصحيحة علمياً.
                2) إذا كانت إجابة الطالب خاطئة، وضح باختصار سبب خطأ اختياره (${answer.userAnswer})، دون ذكر أي أسماء أشخاص.
            """.trimIndent()

            try {
                collectStreamWithAppCheckRetry(
                    operationName = "explainAnswerStream",
                    prompt = prompt
                ) { chunkText ->
                    accumulated.append(chunkText)
                    _explanationMap.value = _explanationMap.value + (key to AiUiState.Streaming(accumulated.toString()))
                }
                if (accumulated.isNotBlank()) {
                    _explanationMap.value = _explanationMap.value + (key to AiUiState.Success(accumulated.toString()))
                } else {
                    _explanationMap.value = _explanationMap.value + (key to AiUiState.Error("لم يتم استلام شرح إضافي من النموذج."))
                }
            } catch (e: TimeoutCancellationException) {
                logSanitizedError("explainAnswerStream", e)
                _explanationMap.value = _explanationMap.value + (key to AiUiState.Error(
                    formatUserFacingAiError(e, "انتهت مهلة طلب الشرح الإضافي.")
                ))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logSanitizedError("explainAnswerStream", e)
                _explanationMap.value = _explanationMap.value + (key to AiUiState.Error(
                    formatUserFacingAiError(e, "تعذر جلب الشرح الذكي الإضافي.")
                ))
            }
        }
    }

    fun requestQuestionHint(subjectName: String, question: Question) {
        val key = question.id
        viewModelScope.launch {
            _hintMap.value = _hintMap.value + (key to AiUiState.Loading)
            val accumulated = StringBuilder()
            val optionsFormatted = question.options.joinToString(" | ") { "${it.key}: ${it.text}" }
            val prompt = """
                أعطِ الطالب تلميحاً علمياً ذكياً ومختصراً (في سطرين فقط) يساعده على التفكير واستنتاج الإجابة الصحيحة بنفسه دون ذكر حرف الإجابة النهائية صراحةً ودون ذكر أي أسماء أشخاص:
                المادة: $subjectName
                السؤال: ${question.question}
                الخيارات: $optionsFormatted
            """.trimIndent()

            try {
                collectStreamWithAppCheckRetry(
                    operationName = "requestQuestionHint",
                    prompt = prompt
                ) { chunkText ->
                    accumulated.append(chunkText)
                    _hintMap.value = _hintMap.value + (key to AiUiState.Streaming(accumulated.toString()))
                }
                if (accumulated.isNotBlank()) {
                    _hintMap.value = _hintMap.value + (key to AiUiState.Success(accumulated.toString()))
                } else {
                    _hintMap.value = _hintMap.value + (key to AiUiState.Error("لم يُرجع النموذج تلميحاً."))
                }
            } catch (e: TimeoutCancellationException) {
                logSanitizedError("requestQuestionHint", e)
                _hintMap.value = _hintMap.value + (key to AiUiState.Error(
                    formatUserFacingAiError(e, "انتهت مهلة التلميح بعد 60 ثانية.")
                ))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logSanitizedError("requestQuestionHint", e)
                _hintMap.value = _hintMap.value + (key to AiUiState.Error(
                    formatUserFacingAiError(e, "تعذر جلب التلميح الذكي.")
                ))
            }
        }
    }
}
