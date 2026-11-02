package com.example.adenmedicaltest.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adenmedicaltest.data.model.Subject
import com.example.adenmedicaltest.data.model.SyllabusData
import com.example.adenmedicaltest.ui.theme.IndigoPrimary

@Composable
fun SyllabusScreen(
    subjects: List<Subject>,
    syllabusMap: Map<String, SyllabusData>,
    onBack: () -> Unit,
    onOpenLevels: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var selectedSubjectId by remember { mutableStateOf("english") }
    val currentSyllabus = syllabusMap[selectedSubjectId]

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("syllabus_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مفردات المنهج المقررة",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "الموضوعات المعتمدة لاختبارات القبول بالكلية",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("العودة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Subject Tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subjects.forEach { subject ->
                    val isSelected = selectedSubjectId == subject.id
                    val bgColor = if (isSelected) Color(0xFFEEF2FF) else MaterialTheme.colorScheme.surface
                    val textColor = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    val borderColor = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.outline

                    Card(
                        onClick = { selectedSubjectId = subject.id },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(borderColor)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_syllabus_${subject.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = subject.icon, fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = subject.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }

        // Syllabus Content Header & Action
        item {
            if (currentSyllabus != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentSyllabus.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = { onOpenLevels(selectedSubjectId) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            modifier = Modifier.testTag("btn_syllabus_go_levels")
                        ) {
                            Text("المستويات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Sections
        if (currentSyllabus != null) {
            items(currentSyllabus.sections) { section ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = section.heading,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            section.items.forEach { item ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "• $item",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
