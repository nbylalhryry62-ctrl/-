package com.example.adenmedicaltest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.adenmedicaltest.ui.components.AppTopBar
import com.example.adenmedicaltest.ui.navigation.Screen
import com.example.adenmedicaltest.ui.screens.AdminDashboardScreen
import com.example.adenmedicaltest.ui.screens.DashboardScreen
import com.example.adenmedicaltest.ui.screens.ExamScreen
import com.example.adenmedicaltest.ui.screens.ProfileScreen
import com.example.adenmedicaltest.ui.screens.ResultScreen
import com.example.adenmedicaltest.ui.screens.ResultsHistoryScreen
import com.example.adenmedicaltest.ui.screens.SubjectLevelsScreen
import com.example.adenmedicaltest.ui.screens.SyllabusScreen
import com.example.adenmedicaltest.ui.theme.AdenMedicalTestTheme
import com.example.adenmedicaltest.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                AdenMedicalTestTheme {
                    MainApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val results by viewModel.results.collectAsState()
    val activeExam by viewModel.activeExam.collectAsState()
    val lastResult by viewModel.lastResult.collectAsState()
    val subjectLevels by viewModel.subjectLevels.collectAsState()
    val selectedLevelsSubject by viewModel.selectedLevelsSubject.collectAsState()
    val syllabusMap by viewModel.syllabusMap.collectAsState()

    val showTopBar = currentScreen !is Screen.Exam

    Scaffold(
        topBar = {
            if (showTopBar) {
                AppTopBar(
                    currentScreen = currentScreen,
                    resultsCount = results.size,
                    currentUser = currentUser,
                    onNavigate = { viewModel.navigateTo(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is Screen.Dashboard -> {
                    DashboardScreen(
                        subjects = viewModel.subjects,
                        results = results,
                        currentUser = currentUser,
                        onOpenLevels = { subjectId -> viewModel.openSubjectLevels(subjectId) },
                        onStartRandom = { subjectId -> viewModel.startExam(subjectId, "random") },
                        onOpenSyllabus = { viewModel.navigateTo(Screen.Syllabus) },
                        onOpenResults = { viewModel.navigateTo(Screen.ResultsHistory) }
                    )
                }

                is Screen.SubjectLevels -> {
                    val subject = selectedLevelsSubject ?: viewModel.subjects.first()
                    SubjectLevelsScreen(
                        subject = subject,
                        levels = subjectLevels,
                        results = results,
                        onBack = { viewModel.navigateTo(Screen.Dashboard) },
                        onSelectLevel = { levelId -> viewModel.startExam(subject.id, levelId) },
                        onStartRandom = { viewModel.startExam(subject.id, "random") },
                        onStartAll = { viewModel.startExam(subject.id, "all") },
                        onOpenSyllabus = { viewModel.navigateTo(Screen.Syllabus) }
                    )
                }

                is Screen.Exam -> {
                    ExamScreen(
                        examState = activeExam,
                        onAnswer = { qId, key -> viewModel.answerQuestion(qId, key) },
                        onNext = { viewModel.nextQuestion() },
                        onPrev = { viewModel.prevQuestion() },
                        onSubmit = { viewModel.submitExam() },
                        onCancel = { viewModel.navigateTo(Screen.Dashboard) }
                    )
                }

                is Screen.Result -> {
                    val res = lastResult ?: screen.result
                    ResultScreen(
                        result = res,
                        onBack = { viewModel.navigateTo(Screen.Dashboard) },
                        onRetry = { viewModel.retryCurrentExam() },
                        onHistory = { viewModel.navigateTo(Screen.ResultsHistory) }
                    )
                }

                is Screen.ResultsHistory -> {
                    ResultsHistoryScreen(
                        results = results,
                        onRetry = { subjectId, level -> viewModel.startExam(subjectId, level) },
                        onDelete = { id -> viewModel.deleteResult(id) },
                        onDashboard = { viewModel.navigateTo(Screen.Dashboard) }
                    )
                }

                is Screen.Syllabus -> {
                    SyllabusScreen(
                        subjects = viewModel.subjects,
                        syllabusMap = syllabusMap,
                        onBack = { viewModel.navigateTo(Screen.Dashboard) },
                        onOpenLevels = { subjectId -> viewModel.openSubjectLevels(subjectId) }
                    )
                }

                is Screen.Profile -> {
                    ProfileScreen(
                        user = currentUser,
                        onUpdateEmail = { email -> viewModel.login(email) },
                        onToggleRole = { viewModel.toggleAdminRole() },
                        onBack = { viewModel.navigateTo(Screen.Dashboard) }
                    )
                }

                is Screen.Admin -> {
                    AdminDashboardScreen(
                        username = currentUser?.username ?: "المشرف",
                        results = results,
                        subjects = viewModel.subjects,
                        onBack = { viewModel.navigateTo(Screen.Dashboard) }
                    )
                }
            }
        }
    }
}
