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
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import java.util.Date
import java.util.UUID
import kotlin.math.ceil

data class SavedExamDraft(
    val subjectId: String,
    val level: String,
    val questionIds: List<String>,
    val currentIndex: Int,
    val answers: Map<String, String>,
    val totalDurationSeconds: Int,
    val secondsRemaining: Int
)

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
    private val localResultsTrigger = MutableStateFlow(0L)

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

    fun sanitizeQuestionText(rawText: String): String {
        val cleaned = rawText
            .replace(Regex("^\\s*\\d+\\)\\s*"), "")
            .replace(Regex("^\\s*\\[[^\\]]+\\]\\s*"), "")
            .replace(Regex("\\s*\\((?:حسب النموذج|وفق النموذج|حسب المنهج|ملخص[^)]*|د\\.[^)]*|الدكتور[^)]*|أ\\.[^)]*|طب أسنان جامعة عدن[^)]*|ضمن Group [AB])\\)"), "")
            .replace(Regex("(?:ملخص|إعداد|جمع)\\s+[\\p{InArabic}\\s]{3,25}(?=[\\s،.,:\\-]|$)"), "")
        return cleaned.replace(Regex("\\s{2,}"), " ").trim()
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun prefs() = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveLocalResult(userId: String, result: ExamResult): ExamResult {
        val p = prefs() ?: return result
        val effectiveId = result.id.ifBlank { "local_${UUID.randomUUID()}" }
        val effectiveCreatedAt = result.createdAt ?: Timestamp.now()
        val savedItem = result.copy(
            id = effectiveId,
            userId = userId,
            createdAt = effectiveCreatedAt
        )

        val currentList = getLocalResults(userId).toMutableList()
        currentList.removeAll { it.id == effectiveId }
        currentList.add(0, savedItem)
        val trimmed = currentList.take(MAX_LOCAL_RESULTS)

        val arr = JSONArray()
        for (item in trimmed) {
            arr.put(item.toJson())
        }
        p.edit().putString("results_$userId", arr.toString()).apply()
        localResultsTrigger.value = System.currentTimeMillis()
        return savedItem
    }

    fun getLocalResults(userId: String): List<ExamResult> {
        val p = prefs() ?: return emptyList()
        val raw = p.getString("results_$userId", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<ExamResult>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                list.add(obj.toExamResultModel())
            }
            list.sortedByDescending { it.createdAt?.seconds ?: 0L }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteLocalResult(userId: String, resultId: String) {
        val p = prefs() ?: return
        val updated = getLocalResults(userId).filterNot { it.id == resultId }
        val arr = JSONArray()
        for (item in updated) {
            arr.put(item.toJson())
        }
        p.edit().putString("results_$userId", arr.toString()).apply()
        localResultsTrigger.value = System.currentTimeMillis()
    }

    fun saveActiveExamDraft(userId: String, draft: SavedExamDraft?) {
        val p = prefs() ?: return
        val key = "active_draft_$userId"
        if (draft == null) {
            p.edit().remove(key).apply()
            return
        }
        val obj = JSONObject().apply {
            put("subjectId", draft.subjectId)
            put("level", draft.level)
            put("questionIds", JSONArray(draft.questionIds))
            put("currentIndex", draft.currentIndex)
            val ansObj = JSONObject()
            draft.answers.forEach { (k, v) -> ansObj.put(k, v) }
            put("answers", ansObj)
            put("totalDurationSeconds", draft.totalDurationSeconds)
            put("secondsRemaining", draft.secondsRemaining)
        }
        p.edit().putString(key, obj.toString()).apply()
    }

    fun loadActiveExamDraft(userId: String): SavedExamDraft? {
        val p = prefs() ?: return null
        val raw = p.getString("active_draft_$userId", null) ?: return null
        return try {
            val obj = JSONObject(raw)
            val qIdsArr = obj.optJSONArray("questionIds") ?: JSONArray()
            val qIds = (0 until qIdsArr.length()).map { qIdsArr.getString(it) }
            val ansObj = obj.optJSONObject("answers") ?: JSONObject()
            val ansMap = mutableMapOf<String, String>()
            val keys = ansObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                ansMap[k] = ansObj.optString(k, "")
            }
            val remaining = obj.optInt("secondsRemaining", 0)
            if (qIds.isEmpty() || remaining <= 0) return null
            SavedExamDraft(
                subjectId = obj.optString("subjectId", "biology"),
                level = obj.optString("level", "1"),
                questionIds = qIds,
                currentIndex = obj.optInt("currentIndex", 0).coerceIn(0, (qIds.size - 1).coerceAtLeast(0)),
                answers = ansMap,
                totalDurationSeconds = obj.optInt("totalDurationSeconds", 600),
                secondsRemaining = remaining
            )
        } catch (e: Exception) {
            null
        }
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
                        val explanationText = sanitizeQuestionText(obj.optString("explanation", ""))

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
        if (targetUid == LOCAL_OFFLINE_USER_ID) {
            emitAll(
                localResultsTrigger.map { getLocalResults(LOCAL_OFFLINE_USER_ID) }
            )
            return@flow
        }
        val path = "users/$targetUid/results"
        emitAll(
            db.collection("users").document(targetUid).collection("results")
                .snapshots()
                .map { snapshot ->
                    val cloudList = snapshot.documents.mapNotNull { doc ->
                        doc.toExamResult()
                    }
                    val localList = getLocalResults(targetUid)
                    mergeCloudAndLocalResults(cloudList, localList)
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    private fun mergeCloudAndLocalResults(
        cloudList: List<ExamResult>,
        localList: List<ExamResult>
    ): List<ExamResult> {
        if (localList.isEmpty()) return cloudList.sortedByDescending { it.createdAt?.seconds ?: 0L }
        val localById = localList.associateBy { it.id }
        val merged = cloudList.map { cloudItem ->
            val matchingLocal = localById[cloudItem.id]
            if (matchingLocal != null && cloudItem.answers.isEmpty()) {
                cloudItem.copy(answers = matchingLocal.answers)
            } else {
                cloudItem
            }
        }.toMutableList()
        val cloudIds = cloudList.map { it.id }.toSet()
        for (localItem in localList) {
            if (localItem.id !in cloudIds && localItem.id.startsWith("local_")) {
                merged.add(localItem)
            }
        }
        return merged.sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

    suspend fun getUserResults(userId: String? = null): Result<List<ExamResult>> {
        if (userId == LOCAL_OFFLINE_USER_ID) {
            return Result.success(getLocalResults(LOCAL_OFFLINE_USER_ID))
        }
        val targetUid = userId ?: requireUserId()
        val path = "users/$targetUid/results"
        return try {
            val snapshot = db.collection("users").document(targetUid).collection("results")
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { it.toExamResult() }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            Result.success(mergeCloudAndLocalResults(list, getLocalResults(targetUid)))
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
        if (result.userId == LOCAL_OFFLINE_USER_ID && auth.currentUser == null) {
            val savedLocal = saveLocalResult(LOCAL_OFFLINE_USER_ID, result)
            return Result.success(savedLocal.id)
        }
        val uid = requireUserId()
        // Save locally immediately so student result is never lost if offline or app closes
        val localSaved = saveLocalResult(uid, result)
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
            // Replace temporary local ID with Firestore document ID
            deleteLocalResult(uid, localSaved.id)
            saveLocalResult(uid, result.copy(id = docRef.id, userId = uid))
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, resultsRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteResult(resultId: String, userIdOverride: String? = null): Result<Unit> {
        if (resultId.isBlank()) return Result.failure(IllegalArgumentException("Result ID is blank"))
        if (userIdOverride == LOCAL_OFFLINE_USER_ID || resultId.startsWith("local_")) {
            val targetUser = userIdOverride ?: auth.currentUser?.uid ?: LOCAL_OFFLINE_USER_ID
            deleteLocalResult(targetUser, resultId)
            return Result.success(Unit)
        }
        val uid = requireUserId()
        deleteLocalResult(uid, resultId)
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

    private fun ExamResult.toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("userId", userId)
        obj.put("subjectId", subjectId)
        obj.put("subjectName", subjectName)
        obj.put("level", level)
        obj.put("score", score)
        obj.put("total", total)
        obj.put("percentage", percentage)
        obj.put("passed", passed)
        obj.put("timeTakenSeconds", timeTakenSeconds)
        obj.put("createdAtSeconds", createdAt?.seconds ?: (System.currentTimeMillis() / 1000))
        val answersArr = JSONArray()
        for (ans in answers) {
            val ansObj = JSONObject()
            ansObj.put("questionId", ans.questionId)
            ansObj.put("question", ans.question)
            ansObj.put("userAnswer", ans.userAnswer ?: JSONObject.NULL)
            ansObj.put("correctAnswer", ans.correctAnswer)
            ansObj.put("isCorrect", ans.isCorrect)
            ansObj.put("explanation", ans.explanation)
            val optsArr = JSONArray()
            for (opt in ans.options) {
                val optObj = JSONObject()
                optObj.put("key", opt.key)
                optObj.put("text", opt.text)
                optsArr.put(optObj)
            }
            ansObj.put("options", optsArr)
            answersArr.put(ansObj)
        }
        obj.put("answers", answersArr)
        return obj
    }

    private fun JSONObject.toExamResultModel(): ExamResult {
        val answersArr = optJSONArray("answers") ?: JSONArray()
        val parsedAnswers = mutableListOf<ExamAnswer>()
        for (i in 0 until answersArr.length()) {
            val aObj = answersArr.optJSONObject(i) ?: continue
            val optsArr = aObj.optJSONArray("options") ?: JSONArray()
            val parsedOpts = mutableListOf<Option>()
            for (j in 0 until optsArr.length()) {
                val oObj = optsArr.optJSONObject(j) ?: continue
                parsedOpts.add(
                    Option(
                        key = oObj.optString("key", ""),
                        text = oObj.optString("text", "")
                    )
                )
            }
            val rawUserAns = if (aObj.isNull("userAnswer")) null else aObj.optString("userAnswer", "").ifBlank { null }
            parsedAnswers.add(
                ExamAnswer(
                    questionId = aObj.optString("questionId", ""),
                    question = aObj.optString("question", ""),
                    userAnswer = rawUserAns,
                    correctAnswer = aObj.optString("correctAnswer", ""),
                    isCorrect = aObj.optBoolean("isCorrect", false),
                    options = parsedOpts,
                    explanation = aObj.optString("explanation", "")
                )
            )
        }
        val seconds = optLong("createdAtSeconds", System.currentTimeMillis() / 1000)
        return ExamResult(
            id = optString("id", ""),
            userId = optString("userId", ""),
            subjectId = optString("subjectId", "biology"),
            subjectName = optString("subjectName", "الأحياء"),
            level = optString("level", "1"),
            score = optInt("score", 0),
            total = optInt("total", 0),
            percentage = optInt("percentage", 0),
            passed = optBoolean("passed", false),
            timeTakenSeconds = optInt("timeTakenSeconds", 0),
            answers = parsedAnswers,
            createdAt = Timestamp(Date(seconds * 1000))
        )
    }

    companion object {
        const val LOCAL_OFFLINE_USER_ID = "offline_student"
        private const val PREFS_NAME = "aden_medical_local_store"
        private const val MAX_LOCAL_RESULTS = 100
    }
}
