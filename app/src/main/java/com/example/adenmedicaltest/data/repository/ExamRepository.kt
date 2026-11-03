package com.example.adenmedicaltest.data.repository

import android.content.Context
import com.example.adenmedicaltest.R
import com.example.adenmedicaltest.data.model.ExamAnswer
import com.example.adenmedicaltest.data.model.ExamResult
import com.example.adenmedicaltest.data.model.Option
import com.example.adenmedicaltest.data.model.Question
import com.example.adenmedicaltest.data.model.Subject
import com.example.adenmedicaltest.data.model.SubjectLevel
import com.example.adenmedicaltest.data.remote.OperationType
import com.example.adenmedicaltest.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.ceil

class ExamRepository(
    private val db: FirebaseFirestore,
    private val appContext: Context? = null
) {
    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        context.applicationContext
    )

    private val auth
        get() = Firebase.auth

    private var cachedQuestions: Map<String, List<Question>>? = null

    val subjects = listOf(
        Subject(
            id = "biology",
            name = "الأحياء",
            icon = "🧬",
            description = "الإحساس والتنظيم العصبي، التغذية والهضم، والوراثة ووظائف الأعضاء — إجمالي 179 سؤالاً",
            durationMinutes = 10,
            totalQuestionCount = 179
        ),
        Subject(
            id = "chemistry",
            name = "الكيمياء",
            icon = "⚗️",
            description = "الكيمياء العضوية، الاتزان الكيميائي، الكهربائية، والمسائل الحسابية والعلاقات — إجمالي 525 سؤالاً شاملاً",
            durationMinutes = 10,
            totalQuestionCount = 525
        ),
        Subject(
            id = "english",
            name = "اللغة الإنجليزية",
            icon = "📖",
            description = "القواعد النحوية، الأزمنة، والتراكيب والمصطلحات الطبية المعتمدة — إجمالي 438 سؤالاً شاملاً",
            durationMinutes = 10,
            totalQuestionCount = 438
        )
    )

    private fun sanitizeQuestionText(rawText: String): String {
        var cleaned = rawText
            .replace(Regex("^\\s*\\d+\\)\\s*"), "")
            .replace(Regex("^\\s*\\[[^\\]]+\\]\\s*"), "")
            .replace(Regex("\\s*\\((?:حسب النموذج|وفق النموذج|حسب المنهج|طب أسنان جامعة عدن[^)]*|ضمن Group [AB])\\)"), "")
        val namesToStrip = listOf(
            "محمد اليوسفي",
            "أحمد الكوري",
            "احمد الكوري",
            "الدكتور جمال الجعدني",
            "جمال الجعدني",
            "د. جمال الجعدني",
            "ملخص محمد اليوسفي",
            "ملخص أحمد الكوري",
            "ملخص احمد الكوري"
        )
        for (name in namesToStrip) {
            cleaned = cleaned.replace(name, "")
        }
        return cleaned.replace(Regex("\\s{2,}"), " ").trim()
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun getQuestionsForSubject(subjectId: String): List<Question> = withContext(Dispatchers.IO) {
        if (cachedQuestions == null) {
            loadAllQuestions()
        }
        cachedQuestions?.get(subjectId) ?: emptyList()
    }

    private fun loadAllQuestions() {
        val context = appContext ?: return
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
                        val questionText = sanitizeQuestionText(obj.optString("question", ""))
                        val correctKey = obj.optString("correctKey", "A")
                        val explanationText = obj.optString("explanation", "")

                        val optionsList = mutableListOf<Option>()
                        val optsArr = obj.optJSONArray("options") ?: JSONArray()
                        for (j in 0 until optsArr.length()) {
                            val optObj = optsArr.getJSONObject(j)
                            optionsList.add(
                                Option(
                                    key = optObj.optString("key", ""),
                                    text = sanitizeQuestionText(optObj.optString("text", ""))
                                )
                            )
                        }

                        list.add(
                            Question(
                                id = id,
                                question = questionText,
                                options = optionsList,
                                correctKey = correctKey,
                                explanation = explanationText
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

    fun observeResults(userId: String? = null): Flow<List<ExamResult>> = flow {
        val targetUid = userId ?: auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$targetUid/results"
        emitAll(
            db.collection("users").document(targetUid).collection("results")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toExamResult()
                    }.sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getUserResults(userId: String? = null): Result<List<ExamResult>> {
        val targetUid = userId ?: requireUserId()
        val path = "users/$targetUid/results"
        return try {
            val snapshot = db.collection("users").document(targetUid).collection("results")
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { it.toExamResult() }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getResultById(ownerUserId: String, resultId: String): Result<ExamResult?> {
        val docRef = db.collection("users").document(ownerUserId).collection("results").document(resultId)
        return try {
            val snapshot = docRef.get().await()
            Result.success(if (snapshot.exists()) snapshot.toExamResult() else null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveResult(result: ExamResult): Result<String> {
        val uid = requireUserId()
        val resultsRef = db.collection("users").document(uid).collection("results")
        val payload = mapOf(
            "userId" to uid,
            "subjectId" to result.subjectId,
            "subjectName" to result.subjectName,
            "level" to result.level,
            "score" to result.score,
            "total" to result.total,
            "percentage" to result.percentage,
            "passed" to result.passed,
            "timeTakenSeconds" to result.timeTakenSeconds,
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            val docRef = resultsRef.add(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, resultsRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteResult(resultId: String): Result<Unit> {
        val uid = requireUserId()
        if (resultId.isBlank()) return Result.failure(IllegalArgumentException("Result ID is blank"))
        val docRef = db.collection("users").document(uid).collection("results").document(resultId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    private fun DocumentSnapshot.toExamResult(): ExamResult? {
        if (!exists()) return null
        val scoreInt = (getLong("score") ?: 0L).toInt()
        val totalInt = (getLong("total") ?: 0L).toInt()
        val percentageInt = (getLong("percentage") ?: 0L).toInt()
        val timeTakenInt = (getLong("timeTakenSeconds") ?: 0L).toInt()
        val ts = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        return ExamResult(
            id = id,
            userId = getString("userId") ?: "",
            subjectId = getString("subjectId") ?: "biology",
            subjectName = getString("subjectName") ?: "الأحياء",
            level = getString("level") ?: "1",
            score = scoreInt,
            total = totalInt,
            percentage = percentageInt,
            passed = getBoolean("passed") ?: (percentageInt >= 50),
            timeTakenSeconds = timeTakenInt,
            answers = emptyList(),
            createdAt = ts
        )
    }
}
