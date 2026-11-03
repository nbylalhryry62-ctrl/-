package com.example.adenmedicaltest.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adenmedicaltest.data.model.Question
import com.example.adenmedicaltest.ui.theme.EmeraldSuccess
import com.example.adenmedicaltest.ui.theme.IndigoPrimary
import com.example.adenmedicaltest.ui.theme.RoseError
import com.example.adenmedicaltest.ui.theme.Slate200
import com.example.adenmedicaltest.ui.theme.Slate600
import com.example.adenmedicaltest.ui.viewmodel.ActiveExamState
import com.example.adenmedicaltest.ui.viewmodel.AiUiState

@Composable
fun ExamScreen(
    examState: ActiveExamState?,
    onAnswer: (questionId: String, selectedKey: String) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    hintMap: Map<String, AiUiState> = emptyMap(),
    onRequestHint: (Question) -> Unit = {},
    explanationMap: Map<String, AiUiState> = emptyMap(),
    onExplainSelection: (Question, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    BackHandler {
        showCancelDialog = true
    }

    if (examState == null || examState.questions.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("لا توجد أسئلة متاحة في هذا الاختبار")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onCancel) {
                    Text("العودة للمواد")
                }
            }
        }
        return
    }

    val currentQuestion = examState.currentQuestion
    val selectedOption = currentQuestion?.let { examState.answers[it.id] }
    val isLastQuestion = examState.currentIndex == examState.questions.size - 1

    val isUrgentTime = examState.secondsRemaining in 1..59
    val isWarningTime = examState.secondsRemaining in 60..119
    val timerBgColor by animateColorAsState(
        targetValue = when {
            isUrgentTime -> Color(0xFFFFE4E6)
            isWarningTime -> Color(0xFFFEF3C7)
            else -> Color(0xFFEEF2FF)
        },
        label = "timerBg"
    )
    val timerTextColor by animateColorAsState(
        targetValue = when {
            isUrgentTime -> RoseError
            isWarningTime -> Color(0xFFD97706)
            else -> IndigoPrimary
        },
        label = "timerText"
    )
    val timerBarColor by animateColorAsState(
        targetValue = when {
            isUrgentTime -> RoseError
            isWarningTime -> Color(0xFFD97706)
            else -> EmeraldSuccess
        },
        label = "timerBar"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("exam_screen")
    ) {
        // Sticky Header with Full-Quiz Countdown Timer & Question Progress
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${examState.subject.icon} ${examState.subject.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "السؤال ${examState.currentIndex + 1} من ${examState.questions.size} · أجبت ${examState.answeredCount}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Full-Quiz Countdown Timer Chip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = timerBgColor,
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = timerTextColor.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .testTag("exam_timer")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "الوقت المتبقي للاختبار",
                                tint = timerTextColor,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = examState.formattedTime,
                                    color = timerTextColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "من ${examState.formattedTotalTime}",
                                    color = timerTextColor.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Countdown Timer Progress Bar for Entire Quiz
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الوقت المتبقي للاختبار الكامل (تسليم تلقائي عند انتهاء الوقت)",
                        fontSize = 11.sp,
                        color = if (isUrgentTime) RoseError else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isUrgentTime) FontWeight.Bold else FontWeight.Medium
                    )
                    Text(
                        text = "${(examState.timeProgress * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = timerTextColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { examState.timeProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .testTag("exam_timer_progress"),
                    color = timerBarColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Question Progress Bar
                LinearProgressIndicator(
                    progress = { examState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = IndigoPrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                if (isUrgentTime) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFF1F2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, RoseError.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .testTag("exam_low_time_warning")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = RoseError,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "تنبيه: تبقى أقل من دقيقة واحدة! سيتم تسليم الاختبار تلقائياً عند انتهاء الوقت.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoseError
                            )
                        }
                    }
                }
            }
        }

        // Scrollable Question Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (currentQuestion != null) {
                val currentHintState = hintMap[currentQuestion.id]
                val currentExplanationState = explanationMap[currentQuestion.id]
                val hasAnsweredCurrent = selectedOption != null
                val isCurrentAnswerCorrect = selectedOption == currentQuestion.correctKey

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = currentQuestion.question,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 26.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("exam_question_text")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Options with Immediate Answer Evaluation
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            currentQuestion.options.forEach { option ->
                                val isSelected = selectedOption == option.key
                                val isCorrectOption = option.key == currentQuestion.correctKey

                                val borderColor = when {
                                    hasAnsweredCurrent && isCorrectOption -> EmeraldSuccess
                                    hasAnsweredCurrent && isSelected && !isCorrectOption -> RoseError
                                    isSelected -> IndigoPrimary
                                    else -> Slate200
                                }
                                val bgColor = when {
                                    hasAnsweredCurrent && isCorrectOption -> Color(0xFFECFDF5)
                                    hasAnsweredCurrent && isSelected && !isCorrectOption -> Color(0xFFFFF1F2)
                                    isSelected -> Color(0xFFEEF2FF)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                                val keyBadgeBg = when {
                                    hasAnsweredCurrent && isCorrectOption -> EmeraldSuccess
                                    hasAnsweredCurrent && isSelected && !isCorrectOption -> RoseError
                                    isSelected -> IndigoPrimary
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                                val keyBadgeText = if (hasAnsweredCurrent && (isCorrectOption || isSelected) || isSelected) {
                                    Color.White
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(2.dp, borderColor, RoundedCornerShape(14.dp))
                                        .background(bgColor)
                                        .clickable(enabled = !hasAnsweredCurrent) {
                                            onAnswer(currentQuestion.id, option.key)
                                            onExplainSelection(currentQuestion, option.key)
                                        }
                                        .padding(14.dp)
                                        .testTag("option_${option.key}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Key Badge
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(keyBadgeBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = option.key,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = keyBadgeText
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = option.text,
                                                fontSize = 14.sp,
                                                fontWeight = if (hasAnsweredCurrent && (isCorrectOption || isSelected)) {
                                                    FontWeight.Bold
                                                } else {
                                                    FontWeight.Normal
                                                },
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            if (hasAnsweredCurrent) {
                                                if (isCorrectOption && isSelected) {
                                                    Text(
                                                        text = "إجابتك صحيحة ✓",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = EmeraldSuccess
                                                    )
                                                } else if (isCorrectOption) {
                                                    Text(
                                                        text = "الخيار الصحيح ✓",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = EmeraldSuccess
                                                    )
                                                } else if (isSelected) {
                                                    Text(
                                                        text = "اختيارك (إجابة خاطئة ✗)",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = RoseError
                                                    )
                                                }
                                            }
                                        }

                                        if (hasAnsweredCurrent && isCorrectOption) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "إجابة صحيحة",
                                                tint = EmeraldSuccess,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        } else if (hasAnsweredCurrent && isSelected && !isCorrectOption) {
                                            Icon(
                                                imageVector = Icons.Default.Cancel,
                                                contentDescription = "إجابة خاطئة",
                                                tint = RoseError,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Immediate Correction & Explanation Card (Appears immediately upon selecting an option)
                        if (hasAnsweredCurrent && selectedOption != null) {
                            val selectedOptText = currentQuestion.options.find { it.key == selectedOption }?.text.orEmpty()
                            val correctOptText = currentQuestion.options.find { it.key == currentQuestion.correctKey }?.text.orEmpty()

                            Spacer(modifier = Modifier.height(16.dp))

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrentAnswerCorrect) Color(0xFFF0FDF4) else Color(0xFFFFF1F2)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isCurrentAnswerCorrect) EmeraldSuccess else RoseError,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .testTag("instant_feedback_card")
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Status Header: Correct or Wrong
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentAnswerCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                            contentDescription = null,
                                            tint = if (isCurrentAnswerCorrect) EmeraldSuccess else RoseError,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Column {
                                            Text(
                                                text = if (isCurrentAnswerCorrect) {
                                                    "إجابة صحيحة! أحسنت الاختيار"
                                                } else {
                                                    "إجابة خاطئة — تعرّف على التصحيح الصحيح"
                                                },
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isCurrentAnswerCorrect) Color(0xFF065F46) else Color(0xFF9F1239)
                                            )
                                            Text(
                                                text = "تم اعتماد إجابتك لهذا السؤال ولا يمكن تغييرها",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Slate600,
                                                modifier = Modifier.testTag("answer_locked_notice")
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Selected vs Correct Option Breakdown
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "إجابتك:",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate600
                                                )
                                                Text(
                                                    text = "($selectedOption) $selectedOptText — ${if (isCurrentAnswerCorrect) "صحيحة ✓" else "خاطئة ✗"}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCurrentAnswerCorrect) EmeraldSuccess else RoseError
                                                )
                                            }

                                            if (!isCurrentAnswerCorrect) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    verticalAlignment = Alignment.Top,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "الخيار الصحيح:",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Slate600
                                                    )
                                                    Text(
                                                        text = "(${currentQuestion.correctKey}) $correctOptText ✓",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = EmeraldSuccess
                                                    )
                                                }
                                            }

                                            HorizontalDivider(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                color = Slate200
                                            )

                                            // Reason / Explanation
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = IndigoPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "التصحيح وسبب اختيار الإجابة:",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = IndigoPrimary
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            val baseExplanation = if (currentQuestion.explanation.isNotBlank()) {
                                                if (isCurrentAnswerCorrect) {
                                                    "إجابتك ($selectedOption: $selectedOptText) صحيحة. ${currentQuestion.explanation}"
                                                } else {
                                                    "اختيارك ($selectedOption: $selectedOptText) غير صحيح لهذا السؤال. ${currentQuestion.explanation}"
                                                }
                                            } else {
                                                if (isCurrentAnswerCorrect) {
                                                    "إجابتك ($selectedOption: $selectedOptText) هي الإجابة العلمية الصحيحة المعتمدة لهذا السؤال."
                                                } else {
                                                    "اختيارك ($selectedOption: $selectedOptText) خاطئ، والإجابة العلمية الصحيحة المعتمدة هي (${currentQuestion.correctKey}: $correctOptText)."
                                                }
                                            }

                                            Text(
                                                text = baseExplanation,
                                                fontSize = 13.sp,
                                                lineHeight = 20.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.testTag("instant_explanation_text")
                                            )

                                            // Live AI Detailed Explanation if available or loading
                                            if (currentExplanationState != null && currentExplanationState !is AiUiState.Idle) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Color(0xFFEEF2FF),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.AutoAwesome,
                                                                contentDescription = null,
                                                                tint = IndigoPrimary,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Text(
                                                                text = "التفصيل العلمي الإضافي:",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = IndigoPrimary
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        when (currentExplanationState) {
                                                            is AiUiState.Loading -> {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                                ) {
                                                                    CircularProgressIndicator(
                                                                        modifier = Modifier.size(14.dp),
                                                                        color = IndigoPrimary,
                                                                        strokeWidth = 2.dp
                                                                    )
                                                                    Text(
                                                                        text = "جاري إعداد التعليل التفصيلي...",
                                                                        fontSize = 11.sp,
                                                                        color = IndigoPrimary
                                                                    )
                                                                }
                                                            }
                                                            is AiUiState.Streaming -> {
                                                                Text(
                                                                    text = currentExplanationState.text,
                                                                    fontSize = 12.sp,
                                                                    lineHeight = 18.sp,
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                            }
                                                            is AiUiState.Success -> {
                                                                Text(
                                                                    text = currentExplanationState.text,
                                                                    fontSize = 12.sp,
                                                                    lineHeight = 18.sp,
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                            }
                                                            is AiUiState.Error -> {
                                                                // Keep built-in explanation clean if offline/error
                                                            }
                                                            else -> {}
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Gemini AI Hint Button & Box (helpful before answering)
                        if (!hasAnsweredCurrent) {
                            TextButton(
                                onClick = { onRequestHint(currentQuestion) },
                                modifier = Modifier.testTag("btn_ai_hint")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تلميح علمي ذكي قبل الإجابة",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoPrimary
                                )
                            }

                            if (currentHintState != null && currentHintState !is AiUiState.Idle) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFEEF2FF),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ai_hint_box")
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        when (currentHintState) {
                                            is AiUiState.Loading -> {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp),
                                                        color = IndigoPrimary,
                                                        strokeWidth = 2.dp
                                                    )
                                                    Text(
                                                        text = "جاري تحضير التلميح الذكي...",
                                                        fontSize = 12.sp,
                                                        color = IndigoPrimary
                                                    )
                                                }
                                            }

                                            is AiUiState.Streaming -> {
                                                Text(
                                                    text = currentHintState.text,
                                                    fontSize = 12.sp,
                                                    lineHeight = 19.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            is AiUiState.Success -> {
                                                Text(
                                                    text = currentHintState.text,
                                                    fontSize = 12.sp,
                                                    lineHeight = 19.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            is AiUiState.Error -> {
                                                Text(
                                                    text = currentHintState.message,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = RoseError
                                                )
                                            }

                                            else -> {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation and Submission buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPrev,
                    enabled = examState.currentIndex > 0,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_prev_question")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "السابق",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("السابق", fontWeight = FontWeight.Bold)
                }

                if (isLastQuestion) {
                    Button(
                        onClick = { showConfirmDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSuccess,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("btn_submit_exam")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسليم الاختبار", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onNext,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IndigoPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("btn_next_question")
                    ) {
                        Text("التالي", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "التالي",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    // Submission Confirmation Dialog
    if (showConfirmDialog) {
        val unanswered = examState.questions.size - examState.answeredCount
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (unanswered > 0) RoseError else EmeraldSuccess
                )
            },
            title = {
                Text("تأكيد تسليم الاختبار", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "أجبت على ${examState.answeredCount} من ${examState.questions.size} سؤالاً."
                    )
                    if (unanswered > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "تنبيه: هناك $unanswered أسئلة بلا إجابة!",
                            color = RoseError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onSubmit()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                ) {
                    Text("تسليم نهائي")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) {
                    Text("متابعة الإجابة")
                }
            }
        )
    }

    // Cancel / Exit Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("مغادرة الاختبار؟", fontWeight = FontWeight.Bold) },
            text = { Text("إذا خرجت الآن، فلن تُحسب نتيجتك وسيتم إلغاء المحاولة.") },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onCancel()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("خروج")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelDialog = false }) {
                    Text("البقاء")
                }
            }
        )
    }
}
