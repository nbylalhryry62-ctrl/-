package com.example.adenmedicaltest.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adenmedicaltest.data.model.ExamAnswer
import com.example.adenmedicaltest.data.model.Question
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiTutorViewModel"

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

class AiTutorViewModel : ViewModel() {

    // Lazy initialization of general-use Gemini 3.8 Flash model
    private val tutorModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
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

    private val structuredQuizModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = practiceQuizSchema
            }
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
                withTimeout(60_000L) {
                    tutorModel.generateContentStream(prompt).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        accumulatedText.append(chunkText)
                        _tutorState.value = AiUiState.Streaming(accumulatedText.toString())
                    }
                }
                if (accumulatedText.isNotBlank()) {
                    _tutorState.value = AiUiState.Success(accumulatedText.toString())
                } else {
                    _tutorState.value = AiUiState.Error("لم يُرجع النموذج أي إجابة. يرجى المحاولة مرة أخرى.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Tutor streaming timed out", e)
                _tutorState.value = AiUiState.Error("انتهت مهلة الاتصال بعد 60 ثانية. يرجى المحاولة مجدداً.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Tutor streaming generation failed", e)
                _tutorState.value = AiUiState.Error(e.localizedMessage ?: "فشل توليد الإجابة من المساعد الذكي.")
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
                val response = withTimeout(60_000L) {
                    structuredQuizModel.generateContent(prompt)
                }
                val jsonString = response.text ?: throw IllegalStateException("Model returned empty JSON text.")

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
                        correctOptionIndex = qObj.optInt("correctOptionIndex", 0).coerceIn(0, (opts.size - 1).coerceAtLeast(0)),
                        explanation = qObj.getString("explanation")
                    )
                }

                _quizGenState.value = QuizGenUiState.Success(
                    topicTitle = topicTitle,
                    studySummary = studySummary,
                    questions = parsedQuestions
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Structured quiz generation timed out", e)
                _quizGenState.value = QuizGenUiState.Error("انتهت مهلة توليد الاختبار التدريبي بعد 60 ثانية.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate or parse structured quiz JSON", e)
                _quizGenState.value = QuizGenUiState.Error(e.localizedMessage ?: "تعذر توليد الأسئلة التدريبية.")
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
                withTimeout(60_000L) {
                    tutorModel.generateContentStream(prompt).collect { chunk ->
                        accumulated.append(chunk.text.orEmpty())
                        _explanationMap.value = _explanationMap.value + (key to AiUiState.Streaming(accumulated.toString()))
                    }
                }
                if (accumulated.isNotBlank()) {
                    _explanationMap.value = _explanationMap.value + (key to AiUiState.Success(accumulated.toString()))
                } else {
                    _explanationMap.value = _explanationMap.value + (key to AiUiState.Error("لم يتم استلام شرح إضافي من النموذج."))
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Explanation timed out", e)
                _explanationMap.value = _explanationMap.value + (key to AiUiState.Error("انتهت مهلة طلب الشرح الإضافي."))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Explanation generation failed", e)
                _explanationMap.value = _explanationMap.value + (key to AiUiState.Error(e.localizedMessage ?: "تعذر جلب الشرح الذكي الإضافي."))
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
                withTimeout(60_000L) {
                    tutorModel.generateContentStream(prompt).collect { chunk ->
                        accumulated.append(chunk.text.orEmpty())
                        _hintMap.value = _hintMap.value + (key to AiUiState.Streaming(accumulated.toString()))
                    }
                }
                if (accumulated.isNotBlank()) {
                    _hintMap.value = _hintMap.value + (key to AiUiState.Success(accumulated.toString()))
                } else {
                    _hintMap.value = _hintMap.value + (key to AiUiState.Error("لم يُرجع النموذج تلميحاً."))
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Hint generation timed out", e)
                _hintMap.value = _hintMap.value + (key to AiUiState.Error("انتهت مهلة التلميح بعد 60 ثانية."))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Hint generation failed", e)
                _hintMap.value = _hintMap.value + (key to AiUiState.Error(e.localizedMessage ?: "تعذر جلب التلميح الذكي."))
            }
        }
    }
}
