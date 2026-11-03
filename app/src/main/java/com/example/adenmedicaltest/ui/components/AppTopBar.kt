package com.example.adenmedicaltest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adenmedicaltest.data.model.UserProfile
import com.example.adenmedicaltest.ui.navigation.Screen
import com.example.adenmedicaltest.ui.theme.IndigoPrimary

@Composable
fun AppTopBar(
    currentScreen: Screen,
    resultsCount: Int,
    currentUser: UserProfile?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Logo & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("brand_header")
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(IndigoPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎓", fontSize = 20.sp)
                }
                Column {
                    Text(
                        text = "اختبار القبول الطبي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "جامعة عدن",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Navigation Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onNavigate(Screen.Dashboard) },
                    modifier = Modifier.testTag("nav_dashboard")
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = "المواد",
                        tint = if (currentScreen is Screen.Dashboard) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { onNavigate(Screen.Syllabus) },
                    modifier = Modifier.testTag("nav_syllabus")
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "المنهج",
                        tint = if (currentScreen is Screen.Syllabus) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { onNavigate(Screen.AiTutor) },
                    modifier = Modifier.testTag("nav_ai_tutor")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "المعلم الذكي",
                        tint = if (currentScreen is Screen.AiTutor) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { onNavigate(Screen.ResultsHistory) },
                    modifier = Modifier.testTag("nav_history")
                ) {
                    BadgedBox(
                        badge = {
                            if (resultsCount > 0) {
                                Badge(
                                    containerColor = IndigoPrimary,
                                    contentColor = Color.White
                                ) {
                                    Text(resultsCount.toString(), fontSize = 10.sp)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "النتائج",
                            tint = if (currentScreen is Screen.ResultsHistory) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (currentUser?.role == "admin") {
                    IconButton(
                        onClick = { onNavigate(Screen.Admin) },
                        modifier = Modifier.testTag("nav_admin")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "الإدارة",
                            tint = if (currentScreen is Screen.Admin) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { onNavigate(Screen.Profile) },
                    modifier = Modifier.testTag("nav_profile")
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "الملف الشخصي",
                        tint = if (currentScreen is Screen.Profile) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
