package com.example.adenmedicaltest.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.adenmedicaltest.ui.theme.EmeraldSuccess
import com.example.adenmedicaltest.ui.theme.IndigoDark
import com.example.adenmedicaltest.ui.theme.IndigoPrimary
import com.example.adenmedicaltest.ui.theme.RoseError
import com.example.adenmedicaltest.ui.theme.Slate200
import com.example.adenmedicaltest.ui.viewmodel.AiGeneratedQuestion
import com.example.adenmedicaltest.ui.viewmodel.AiTutorViewModel
import com.example.adenmedicaltest.ui.viewmodel.AiUiState
import com.example.adenmedicaltest.ui.viewmodel.QuizGenUiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiTutorScreen(
    aiViewModel: AiTutorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val tutorState by aiViewModel.tutorState.collectAsStateWithLifecycle()
    val quizGenState by aiViewModel.quizGenState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedSubject by remember { mutableStateOf("الأحياء") }
    var promptInput by remember { mutableStateOf("") }
    var topicInput by remember { mutableStateOf("السيال العصبي والتشابك العصبي") }

    val practiceTotalSeconds = 180
    var practiceSecondsRemaining by remember { mutableIntStateOf(practiceTotalSeconds) }
    var isPracticeSubmitted by remember { mutableStateOf(false) }
    var isPracticeTimedOut by remember { mutableStateOf(false) }
    val practiceSelectedAnswers = remember { mutableStateMapOf<Int, Int>() }

    LaunchedEffect(quizGenState) {
        if (quizGenState is QuizGenUiState.Success) {
            practiceSelectedAnswers.clear()
            practiceSecondsRemaining = practiceTotalSeconds
            isPracticeSubmitted = false
            isPracticeTimedOut = false
            while (practiceSecondsRemaining > 0 && !isPracticeSubmitted) {
                delay(1000L)
                if (!isPracticeSubmitted) {
                    practiceSecondsRemaining -= 1
                    if (practiceSecondsRemaining <= 0) {
                        practiceSecondsRemaining = 0
                        isPracticeTimedOut = true
                        isPracticeSubmitted = true
                    }
                }
            }
        }
    }

    val subjectOptions = listOf("الأحياء", "الكيمياء", "اللغة الإنجليزية")

    val quickPrompts = mapOf(
        "الأحياء" to listOf(
            "اشرح آلية انتقال السيال العصبي عبر التشابك الكيميائي",
            "ما الفرق بين الهضم في المعدة والأمعاء الدقيقة؟",
            "لخص هرمونات الغدة النخامية ووظائفها الأساسية"
        ),
        "الكيمياء" to listOf(
            "كيف أميز بين الكحولات الأولية والثانوية والثالثية؟",
            "اشرح قانون فاراداي للتحليل الكهربائي مع مثال",
            "قواعد تسمية المركبات العضوية حسب نظام IUPAC"
        ),
        "اللغة الإنجليزية" to listOf(
            "أهم البادئات واللواحق الطبية (Medical Prefixes & Suffixes)",
            "متى نستخدم Present Perfect مقابل Past Simple في الاختبار؟",
            "شرح قاعدة Conditional Sentences بأنواعها الثلاثة"
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_tutor_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Banner
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(IndigoDark, IndigoPrimary)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "المساعد الذكي",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "المعلم الطبي الذكي (Gemini AI)",
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "شرح فوري للمفاهيم العلمية وتوليد اختبارات تدريبية مخصصة",
                                    color = Color(0xFFE0E7FF),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Mode Tabs: Chat Tutor vs Structured Practice Quiz Generator
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = IndigoPrimary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("الشرح الفوري", fontWeight = FontWeight.Bold) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("tab_ai_chat")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("مولّد أسئلة ذكي", fontWeight = FontWeight.Bold) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Quiz,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("tab_ai_quiz_gen")
                    )
                }
            }
        }

        // Subject Selector Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المادة:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                subjectOptions.forEach { subj ->
                    FilterChip(
                        selected = selectedSubject == subj,
                        onClick = { selectedSubject = subj },
                        label = { Text(subj, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEEF2FF),
                            selectedLabelColor = IndigoPrimary
                        ),
                        modifier = Modifier.testTag("chip_subject_$subj")
                    )
                }
            }
        }

        if (selectedTab == 0) {
            // TAB 0: Streaming Medical Tutor
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "اسأل المعلم الذكي في $selectedSubject",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            (quickPrompts[selectedSubject] ?: emptyList()).forEachIndexed { idx, suggestion ->
                                AssistChip(
                                    onClick = {
                                        promptInput = suggestion
                                        aiViewModel.askMedicalTutorStream(suggestion, selectedSubject)
                                    },
                                    label = { Text(suggestion, fontSize = 11.sp) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.testTag("quick_prompt_$idx")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = promptInput,
                            onValueChange = { promptInput = it },
                            label = { Text("اكتب سؤالك العلمي أو الطبي هنا...") },
                            shape = RoundedCornerShape(14.dp),
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_prompt_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                aiViewModel.askMedicalTutorStream(promptInput, selectedSubject)
                            },
                            enabled = promptInput.isNotBlank() && tutorState !is AiUiState.Loading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IndigoPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_ask_ai_tutor")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "إرسال",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اشرح لي الآن", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Streaming / Success / Error Output Card
            item {
                when (val state = tutorState) {
                    is AiUiState.Idle -> {
                        // Subtle helper card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFEEF2FF),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = IndigoPrimary
                                )
                                Text(
                                    text = "اختر أحد الأسئلة المقترحة أعلاه أو اكتب أي سؤال في الأحياء أو الكيمياء أو الإنجليزية ليقوم Gemini بشرحه لك خطوة بخطوة.",
                                    fontSize = 13.sp,
                                    color = IndigoDark,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    is AiUiState.Loading -> {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_tutor_loading")
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = IndigoPrimary,
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = "جاري إعداد الشرح العلمي عبر Gemini...",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    is AiUiState.Streaming -> {
                        AiResponseCard(
                            title = "جاري كتابة الشرح...",
                            text = state.text,
                            isStreaming = true
                        )
                    }

                    is AiUiState.Success -> {
                        AiResponseCard(
                            title = "شرح المعلم الذكي ($selectedSubject)",
                            text = state.text,
                            isStreaming = false
                        )
                    }

                    is AiUiState.Error -> {
                        AiErrorCard(message = state.message)
                    }
                }
            }
        } else {
            // TAB 1: Structured JSON Practice Quiz Generator (Schema.obj)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "توليد اختبار مصغّر مهيكل (Structured JSON)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "أدخل عنوان أي درس في منهج القبول ليقوم Gemini بإنشاء ملخص مركز مع 3 أسئلة تدريبية وتعليلاتها العلمية.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = topicInput,
                            onValueChange = { topicInput = it },
                            label = { Text("موضوع الدرس (مثال: الكيمياء العضوية، الجهاز الهضمي...)") },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_quiz_topic_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                aiViewModel.generatePracticeQuiz(topicInput, selectedSubject)
                            },
                            enabled = topicInput.isNotBlank() && quizGenState !is QuizGenUiState.Loading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IndigoPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_generate_ai_quiz")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("توليد ملخص و 3 أسئلة تدريبية", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            when (val qState = quizGenState) {
                is QuizGenUiState.Idle -> {}
                is QuizGenUiState.Loading -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_quiz_loading")
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = IndigoPrimary,
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = "جاري توليد الملخص والأسئلة التدريبية المهيكلة...",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                is QuizGenUiState.Error -> {
                    item {
                        AiErrorCard(message = qState.message)
                    }
                }

                is QuizGenUiState.Success -> {
                    item {
                        val mins = practiceSecondsRemaining / 60
                        val secs = practiceSecondsRemaining % 60
                        val formattedTimer = "%02d:%02d".format(mins, secs)
                        val timeProgress = (practiceSecondsRemaining.toFloat() / practiceTotalSeconds.toFloat()).coerceIn(0f, 1f)
                        val isLowTime = practiceSecondsRemaining in 1..45
                        val correctScore = qState.questions.indices.count { idx ->
                            practiceSelectedAnswers[idx] == qState.questions[idx].correctOptionIndex
                        }

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isPracticeSubmitted && isPracticeTimedOut -> Color(0xFFFFF1F2)
                                    isPracticeSubmitted -> Color(0xFFF0FDF4)
                                    isLowTime -> Color(0xFFFFF1F2)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_practice_timer_card")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = "مؤقت الاختبار التدريبي",
                                            tint = if (isLowTime || isPracticeTimedOut) RoseError else IndigoPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = when {
                                                    isPracticeTimedOut -> "انتهى الوقت — تم تسليم الاختبار تلقائياً"
                                                    isPracticeSubmitted -> "تم تسليم الاختبار التدريبي"
                                                    else -> "مؤقت الاختبار التدريبي الكامل"
                                                },
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = when {
                                                    isPracticeTimedOut -> RoseError
                                                    isPracticeSubmitted -> EmeraldSuccess
                                                    isLowTime -> RoseError
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                            Text(
                                                text = if (isPracticeSubmitted) {
                                                    "النتيجة النهائية: $correctScore من ${qState.questions.size}"
                                                } else {
                                                    "أجبت على ${practiceSelectedAnswers.size} من ${qState.questions.size} · يُسلّم تلقائياً عند انتهاء الوقت"
                                                },
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isLowTime || isPracticeTimedOut) Color(0xFFFFE4E6) else Color(0xFFEEF2FF)
                                    ) {
                                        Text(
                                            text = formattedTimer,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = if (isLowTime || isPracticeTimedOut) RoseError else IndigoPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LinearProgressIndicator(
                                    progress = { timeProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (isLowTime || isPracticeTimedOut) RoseError else EmeraldSuccess,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                if (!isPracticeSubmitted) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { isPracticeSubmitted = true },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldSuccess,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .testTag("btn_submit_practice_quiz")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Flag,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("تسليم الاختبار التدريبي الآن", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_quiz_summary_card")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "📌 ملخص: ${qState.topicTitle}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = IndigoDark
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = qState.studySummary,
                                    fontSize = 13.sp,
                                    lineHeight = 21.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    itemsIndexed(qState.questions) { index, qItem ->
                        InteractiveAiQuestionCard(
                            index = index,
                            item = qItem,
                            selectedIdx = practiceSelectedAnswers[index],
                            isQuizSubmitted = isPracticeSubmitted,
                            onSelectOption = { optIdx ->
                                if (!isPracticeSubmitted) {
                                    practiceSelectedAnswers[index] = optIdx
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiResponseCard(
    title: String,
    text: String,
    isStreaming: Boolean
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_tutor_response_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = IndigoPrimary
                )
                if (isStreaming) {
                    Spacer(modifier = Modifier.width(6.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = IndigoPrimary,
                        strokeWidth = 2.dp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                lineHeight = 23.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AiErrorCard(message: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_error_card")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = RoseError
            )
            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = RoseError
            )
        }
    }
}

@Composable
private fun InteractiveAiQuestionCard(
    index: Int,
    item: AiGeneratedQuestion,
    selectedIdx: Int?,
    isQuizSubmitted: Boolean,
    onSelectOption: (Int) -> Unit
) {
    val showFeedback = selectedIdx != null || isQuizSubmitted

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_generated_question_$index")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "سؤال تدريبي ${index + 1}: ${item.question}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.options.forEachIndexed { optIdx, optText ->
                    val isSelected = selectedIdx == optIdx
                    val isCorrect = optIdx == item.correctOptionIndex

                    val borderColor = when {
                        showFeedback && isCorrect -> EmeraldSuccess
                        showFeedback && isSelected && !isCorrect -> RoseError
                        isSelected -> IndigoPrimary
                        else -> Slate200
                    }
                    val bgColor = when {
                        showFeedback && isCorrect -> Color(0xFFF0FDF4)
                        showFeedback && isSelected && !isCorrect -> Color(0xFFFFF1F2)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .clickable(enabled = !isQuizSubmitted) { onSelectOption(optIdx) }
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = optText,
                                fontSize = 13.sp,
                                fontWeight = if (showFeedback && isCorrect) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (showFeedback && isCorrect) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else if (showFeedback && isSelected && !isCorrect) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = RoseError,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (showFeedback) {
                val isAnswerCorrect = selectedIdx == item.correctOptionIndex
                val correctOptionText = item.options.getOrNull(item.correctOptionIndex).orEmpty()
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAnswerCorrect) Color(0xFFF0FDF4) else Color(0xFFFFF1F2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isAnswerCorrect) EmeraldSuccess else RoseError,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = when {
                                isAnswerCorrect -> "إجابة صحيحة ✓"
                                selectedIdx == null -> "لم تتم الإجابة قبل انتهاء الوقت ✗ — الخيار الصحيح: $correctOptionText"
                                else -> "إجابة خاطئة ✗ — الخيار الصحيح: $correctOptionText"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = if (isAnswerCorrect) EmeraldSuccess else RoseError
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "سبب الإجابة والتصحيح: ${item.explanation}",
                            fontSize = 12.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
