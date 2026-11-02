package com.example.adenmedicaltest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adenmedicaltest.data.model.ExamResult
import com.example.adenmedicaltest.data.model.Subject
import com.example.adenmedicaltest.data.model.UserProfile
import com.example.adenmedicaltest.ui.theme.EmeraldSuccess
import com.example.adenmedicaltest.ui.theme.IndigoDark
import com.example.adenmedicaltest.ui.theme.IndigoLight
import com.example.adenmedicaltest.ui.theme.IndigoPrimary
import com.example.adenmedicaltest.ui.theme.Slate200
import com.example.adenmedicaltest.ui.theme.Slate600
import com.example.adenmedicaltest.ui.theme.Slate900

@Composable
fun DashboardScreen(
    subjects: List<Subject>,
    results: List<ExamResult>,
    currentUser: UserProfile?,
    onOpenLevels: (String) -> Unit,
    onStartRandom: (String) -> Unit,
    onOpenSyllabus: () -> Unit,
    onOpenResults: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalQuestions = subjects.sumOf { it.totalQuestionCount }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Banner
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_banner")
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
                        Text(
                            text = "كلية الطب والعلوم الصحية — جامعة عدن",
                            color = Color(0xFFC7D2FE),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "مرحبًا، ${currentUser?.username ?: "طالبنا"}",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "تدرّب على $totalQuestions سؤالاً شاملاً موزعة على المواد ونماذج السنوات السابقة.",
                            color = Color(0xFFE0E7FF),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onOpenSyllabus,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.2f),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("btn_hero_syllabus")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("المنهج", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onOpenResults,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = IndigoPrimary
                                ),
                                modifier = Modifier.testTag("btn_hero_history")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "سجل النتائج (${results.size})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section Title & Success Rate Badge
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "المواد والمستويات",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "اختر مستوى محددًا أو ابدأ اختبارًا عشوائيًا",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = Color(0xFFEEF2FF),
                    shape = RoundedCornerShape(20.dp),
                    border = null
                ) {
                    Text(
                        text = "النجاح من 50%",
                        color = IndigoPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Subjects Cards List
        items(subjects) { subject ->
            val attempts = results.filter { it.subjectId == subject.id }
            val passedLevelsCount = attempts.filter { it.passed }.map { it.level }.distinct().size
            val latestResult = attempts.firstOrNull()

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subject_card_${subject.id}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Icon & Level Count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = subject.icon,
                            fontSize = 32.sp
                        )
                        val levelCount = (subject.totalQuestionCount + 24) / 25
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "$levelCount مستوى",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = subject.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = subject.description,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Meta: Question count and Time
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Slate600
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${subject.totalQuestionCount} سؤالاً",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Slate600
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${subject.durationMinutes} دقائق / مستوى",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Attempts & Stats
                    val statsText = buildString {
                        append("المحاولات: ${attempts.size} · المستويات المجتازة: $passedLevelsCount")
                        if (latestResult != null) {
                            append(" · آخر نتيجة ${latestResult.percentage}%")
                        }
                    }
                    Text(
                        text = statsText,
                        fontSize = 11.sp,
                        color = if (latestResult?.passed == true) EmeraldSuccess else Slate600
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons
                    Button(
                        onClick = { onOpenLevels(subject.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IndigoPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_open_levels_${subject.id}")
                    ) {
                        Text("عرض المستويات", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onStartRandom(subject.id) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEEF2FF),
                                contentColor = IndigoPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("btn_random_${subject.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("عشوائي (25)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onOpenSyllabus,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("btn_syllabus_${subject.id}")
                        ) {
                            Text("المنهج", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
