package com.example.adenmedicaltest.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adenmedicaltest.data.model.ExamAnswer
import com.example.adenmedicaltest.data.model.ExamResult
import com.example.adenmedicaltest.ui.theme.EmeraldSuccess
import com.example.adenmedicaltest.ui.theme.IndigoPrimary
import com.example.adenmedicaltest.ui.theme.RoseError
import com.example.adenmedicaltest.ui.viewmodel.AiUiState

@Composable
fun ResultScreen(
    result: ExamResult,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onHistory: () -> Unit,
    explanationMap: Map<String, AiUiState> = emptyMap(),
    onExplainAnswer: (ExamAnswer) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val passed = result.passed

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("result_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Result Banner
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
                            brush = if (passed) {
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF047857), EmeraldSuccess)
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFBE123C), RoseError)
                                )
                            }
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (passed) Icons.Default.EmojiEvents else Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (passed) "مبروك! اجتزت الاختبار بنجاح" else "انتهى الاختبار",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${result.subjectName} · النتيجة: ${result.percentage}%",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Summary Counters (3 columns)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCard(
                    title = "إجابات صحيحة",
                    value = result.score.toString(),
                    color = EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )

                SummaryCard(
                    title = "إجابات خاطئة",
                    value = maxOf(0, result.total - result.score).toString(),
                    color = RoseError,
                    modifier = Modifier.weight(1f)
                )

                SummaryCard(
                    title = "إجمالي الأسئلة",
                    value = result.total.toString(),
                    color = IndigoPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Action Buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("btn_result_back")
                ) {
                    Text("المواد", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IndigoPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("btn_result_retry")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إعادة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onHistory,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEEF2FF),
                        contentColor = IndigoPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("btn_result_history")
                ) {
                    Text("السجل", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Answers Detailed Review
        if (result.answers.isNotEmpty()) {
            item {
                Text(
                    text = "مراجعة الإجابات (${result.answers.size} سؤالاً)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            itemsIndexed(result.answers) { index, answer ->
                val answerKey = answer.questionId.ifBlank { answer.question }
                val aiState = explanationMap[answerKey]

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (answer.isCorrect) Color(0xFFF0FDF4) else Color(0xFFFFF1F2)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (answer.isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (answer.isCorrect) EmeraldSuccess else RoseError,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(top = 2.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "س${index + 1}: ${answer.question}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "إجابتك: ${answer.userAnswer ?: "بلا إجابة"}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (answer.isCorrect) EmeraldSuccess else RoseError
                                    )

                                    if (!answer.isCorrect) {
                                        Text(
                                            text = "الصحيحة: ${answer.correctAnswer}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldSuccess
                                        )
                                    }
                                }

                                if (answer.explanation.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "سبب الإجابة: ${answer.explanation}",
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                TextButton(
                                    onClick = { onExplainAnswer(answer) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("btn_ai_explain_$index")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = IndigoPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "اشرح الإجابة بالذكاء الاصطناعي (Gemini)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IndigoPrimary
                                    )
                                }
                            }
                        }

                        if (aiState != null && aiState !is AiUiState.Idle) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_explanation_box_$index")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    when (aiState) {
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
                                                    text = "جاري تحليل السؤال علمياً...",
                                                    fontSize = 12.sp,
                                                    color = IndigoPrimary
                                                )
                                            }
                                        }

                                        is AiUiState.Streaming -> {
                                            Text(
                                                text = aiState.text,
                                                fontSize = 12.sp,
                                                lineHeight = 19.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        is AiUiState.Success -> {
                                            Text(
                                                text = aiState.text,
                                                fontSize = 12.sp,
                                                lineHeight = 19.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        is AiUiState.Error -> {
                                            Text(
                                                text = aiState.message,
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
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
