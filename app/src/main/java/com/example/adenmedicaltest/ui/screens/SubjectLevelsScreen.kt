package com.example.adenmedicaltest.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adenmedicaltest.data.model.ExamResult
import com.example.adenmedicaltest.data.model.Subject
import com.example.adenmedicaltest.data.model.SubjectLevel
import com.example.adenmedicaltest.ui.theme.EmeraldSuccess
import com.example.adenmedicaltest.ui.theme.IndigoPrimary
import com.example.adenmedicaltest.ui.theme.RoseError

@Composable
fun SubjectLevelsScreen(
    subject: Subject,
    levels: List<SubjectLevel>,
    results: List<ExamResult>,
    onBack: () -> Unit,
    onSelectLevel: (String) -> Unit,
    onStartRandom: () -> Unit,
    onStartAll: () -> Unit,
    onOpenSyllabus: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val attempts = results.filter { it.subjectId == subject.id }
    val bestScoresByLevel = attempts.groupBy { it.level }
        .mapValues { (_, list) -> list.maxOf { it.percentage } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("subject_levels_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Back & Header Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = onBack,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("btn_back_levels")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "العودة",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("العودة للمواد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(subject.icon, fontSize = 36.sp)
                        Column {
                            Text(
                                text = "مستويات ${subject.name}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${subject.totalQuestionCount} سؤالاً موزعة على ${levels.size} مستويات",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onStartRandom,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IndigoPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_levels_random")
                        ) {
                            Text("عشوائي (25)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onStartAll,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_levels_all")
                        ) {
                            Text("البنك الكامل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onOpenSyllabus,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_levels_syllabus")
                        ) {
                            Text("المنهج", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section header
        item {
            Text(
                text = "قائمة المستويات",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Levels items
        items(levels) { level ->
            val bestScore = bestScoresByLevel[level.id]

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("level_item_${level.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = level.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (level.difficultyLabel) {
                                    "مستوى متقدم" -> Color(0xFFF3E8FF)
                                    "مستوى متوسط" -> Color(0xFFE0E7FF)
                                    else -> Color(0xFFDCFCE7)
                                }
                            ) {
                                Text(
                                    text = level.difficultyLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (level.difficultyLabel) {
                                        "مستوى متقدم" -> Color(0xFF6B21A8)
                                        "مستوى متوسط" -> Color(0xFF3730A3)
                                        else -> Color(0xFF166534)
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${level.rangeLabel} · ${level.questionCount} سؤالاً",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (bestScore != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "أفضل نتيجة: $bestScore%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bestScore >= 50) EmeraldSuccess else RoseError
                            )
                        }
                    }

                    Button(
                        onClick = { onSelectLevel(level.id) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEEF2FF),
                            contentColor = IndigoPrimary
                        ),
                        modifier = Modifier.testTag("btn_start_level_${level.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ابدأ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
