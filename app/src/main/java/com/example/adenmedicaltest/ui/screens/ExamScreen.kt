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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.adenmedicaltest.ui.theme.EmeraldSuccess
import com.example.adenmedicaltest.ui.theme.IndigoPrimary
import com.example.adenmedicaltest.ui.theme.RoseError
import com.example.adenmedicaltest.ui.theme.Slate200
import com.example.adenmedicaltest.ui.theme.Slate600
import com.example.adenmedicaltest.ui.viewmodel.ActiveExamState

@Composable
fun ExamScreen(
    examState: ActiveExamState?,
    onAnswer: (questionId: String, selectedKey: String) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
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

    val isUrgentTime = examState.secondsRemaining < 60
    val timerBgColor by animateColorAsState(
        targetValue = if (isUrgentTime) Color(0xFFFFE4E6) else MaterialTheme.colorScheme.surfaceVariant,
        label = "timerBg"
    )
    val timerTextColor by animateColorAsState(
        targetValue = if (isUrgentTime) RoseError else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "timerText"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("exam_screen")
    ) {
        // Sticky Header with Timer & Progress
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
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

                    // Timer Chip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = timerBgColor,
                        modifier = Modifier.testTag("exam_timer")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "الوقت",
                                tint = timerTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = examState.formattedTime,
                                color = timerTextColor,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { examState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = IndigoPrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
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
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Options
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            currentQuestion.options.forEach { option ->
                                val isSelected = selectedOption == option.key

                                val borderColor = if (isSelected) IndigoPrimary else Slate200
                                val bgColor = if (isSelected) Color(0xFFEEF2FF) else MaterialTheme.colorScheme.surface

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(2.dp, borderColor, RoundedCornerShape(14.dp))
                                        .background(bgColor)
                                        .clickable {
                                            onAnswer(currentQuestion.id, option.key)
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
                                                .background(if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = option.key,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Text(
                                            text = option.text,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = IndigoPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
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
