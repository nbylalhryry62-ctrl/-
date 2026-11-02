package com.example.adenmedicaltest.data.repository

import android.content.Context
import com.example.adenmedicaltest.data.local.AppDatabase
import com.example.adenmedicaltest.data.local.ExamResultEntity
import com.example.adenmedicaltest.data.model.ExamAnswer
import com.example.adenmedicaltest.data.model.ExamResult
import com.example.adenmedicaltest.data.model.Option
import com.example.adenmedicaltest.data.model.Question
import com.example.adenmedicaltest.data.model.Subject
import com.example.adenmedicaltest.data.model.SubjectLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.ceil

class ExamRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.examResultDao()

    private var cachedQuestions: Map<String, List<Question>>? = null

    val subjects = listOf(
        Subject(
            id = "biology",
            name = "الأحياء",
            icon = "🧬",
            description = "الإحساس والتنظيم العصبي (1-85) + التغذية والهضم (87-126) + نماذج طب بشري وأسنان وصيدلة ومختبرات وتمريض جامعة عدن — إجمالي 179 سؤالاً",
            durationMinutes = 10,
            totalQuestionCount = 179
        ),
        Subject(
            id = "chemistry",
            name = "الكيمياء",
            icon = "⚗️",
            description = "اختيار من متعدد (1-43) + مسائل حسابية (44-75) + صح وخطأ وعلاقات كيميائية ونماذج طب أسنان — إجمالي 525 سؤالاً شاملاً",
            durationMinutes = 10,
            totalQuestionCount = 525
        ),
        Subject(
            id = "english",
            name = "اللغة الإنجليزية",
            icon = "📖",
            description = "المفردات والقواعد والأزمنة (1-88) + القواعد والمفردات الطبية (150 قاعدة + 200 مفردة طبية: 89-438) — إجمالي 438 سؤالاً شاملاً",
            durationMinutes = 10,
            totalQuestionCount = 438
        )
    )

    suspend fun getQuestionsForSubject(subjectId: String): List<Question> = withContext(Dispatchers.IO) {
        if (cachedQuestions == null) {
            loadAllQuestions()
        }
        cachedQuestions?.get(subjectId) ?: emptyList()
    }

    private fun loadAllQuestions() {
        try {
            val inputStream = context.assets.open("question_bank.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonStr = reader.use { it.readText() }
            val root = JSONObject(jsonStr)

            val map = mutableMapOf<String, List<Question>>()
            for (key in listOf("biology", "chemistry", "english")) {
                if (root.has(key)) {
                    val arr = root.getJSONArray(key)
                    val list = mutableListOf<Question>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("id", "$key-$i")
                        val questionText = obj.optString("question", "")
                        val correctKey = obj.optString("correctKey", "A")

                        val optionsList = mutableListOf<Option>()
                        val optsArr = obj.optJSONArray("options") ?: JSONArray()
                        for (j in 0 until optsArr.length()) {
                            val optObj = optsArr.getJSONObject(j)
                            optionsList.add(
                                Option(
                                    key = optObj.optString("key", ""),
                                    text = optObj.optString("text", "")
                                )
                            )
                        }

                        list.add(
                            Question(
                                id = id,
                                question = questionText,
                                options = optionsList,
                                correctKey = correctKey
                            )
                        )
                    }
                    map[key] = list
                }
            }
            cachedQuestions = map
        } catch (e: Exception) {
            e.printStackTrace()
            cachedQuestions = emptyMap()
        }
    }

    suspend fun getSubjectLevels(subjectId: String): List<SubjectLevel> = withContext(Dispatchers.IO) {
        val questions = getQuestionsForSubject(subjectId)
        val perLevel = 25
        val count = maxOf(1, ceil(questions.size.toDouble() / perLevel).toInt())

        (0 until count).map { index ->
            val start = index * perLevel
            val end = minOf(start + perLevel, questions.size)
            val pool = if (start < questions.size) questions.subList(start, end) else emptyList()
            val number = index + 1
            val difficultyLabel = when {
                number > ceil(count * 0.66).toInt() -> "مستوى متقدم"
                number > ceil(count * 0.33).toInt() -> "مستوى متوسط"
                else -> "مستوى تأسيسي"
            }
            SubjectLevel(
                levelNumber = number,
                id = number.toString(),
                title = "المستوى $number",
                difficultyLabel = difficultyLabel,
                questionCount = pool.size,
                rangeLabel = "الأسئلة ${start + 1} – $end",
                questionsPool = pool
            )
        }
    }

    suspend fun getRandomQuestions(subjectId: String, count: Int = 25): List<Question> = withContext(Dispatchers.IO) {
        val pool = getQuestionsForSubject(subjectId)
        pool.shuffled().take(count)
    }

    fun getAllResults(): Flow<List<ExamResult>> {
        return dao.getAllResults().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    suspend fun saveResult(result: ExamResult): Long = withContext(Dispatchers.IO) {
        dao.insertResult(result.toEntity())
    }

    suspend fun deleteResult(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteResult(id)
    }

    private fun ExamResult.toEntity(): ExamResultEntity {
        val answersArray = JSONArray()
        for (a in answers) {
            val obj = JSONObject()
            obj.put("questionId", a.questionId)
            obj.put("question", a.question)
            obj.put("userAnswer", a.userAnswer ?: "")
            obj.put("correctAnswer", a.correctAnswer)
            obj.put("isCorrect", a.isCorrect)
            answersArray.put(obj)
        }
        return ExamResultEntity(
            id = id,
            subjectId = subjectId,
            subjectName = subjectName,
            level = level,
            score = score,
            total = total,
            percentage = percentage,
            passed = passed,
            timeTakenSeconds = timeTakenSeconds,
            answersJson = answersArray.toString(),
            createdAt = createdAt
        )
    }

    private fun ExamResultEntity.toDomainModel(): ExamResult {
        val answersList = mutableListOf<ExamAnswer>()
        try {
            val arr = JSONArray(answersJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                answersList.add(
                    ExamAnswer(
                        questionId = obj.optString("questionId", ""),
                        question = obj.optString("question", ""),
                        userAnswer = obj.optString("userAnswer", "").ifEmpty { null },
                        correctAnswer = obj.optString("correctAnswer", ""),
                        isCorrect = obj.optBoolean("isCorrect", false),
                        options = emptyList()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ExamResult(
            id = id,
            subjectId = subjectId,
            subjectName = subjectName,
            level = level,
            score = score,
            total = total,
            percentage = percentage,
            passed = passed,
            timeTakenSeconds = timeTakenSeconds,
            answers = answersList,
            createdAt = createdAt
        )
    }
}
