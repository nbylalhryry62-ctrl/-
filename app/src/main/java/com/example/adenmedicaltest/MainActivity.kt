package com.example.adenmedicaltest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.adenmedicaltest.data.repository.AuthRepository
import com.example.adenmedicaltest.data.repository.ExamRepository
import com.example.adenmedicaltest.data.repository.SyllabusRepository
import com.example.adenmedicaltest.ui.auth.authStateFlow
import com.example.adenmedicaltest.ui.auth.configureAppCheck
import com.example.adenmedicaltest.ui.auth.signOut
import com.example.adenmedicaltest.ui.components.AppTopBar
import com.example.adenmedicaltest.ui.navigation.Screen
import com.example.adenmedicaltest.ui.screens.AboutPrivacyScreen
import com.example.adenmedicaltest.ui.screens.AdminDashboardScreen
import com.example.adenmedicaltest.ui.screens.AiTutorScreen
import com.example.adenmedicaltest.ui.screens.AuthScreen
import com.example.adenmedicaltest.ui.screens.DashboardScreen
import com.example.adenmedicaltest.ui.screens.ExamScreen
import com.example.adenmedicaltest.ui.screens.ProfileScreen
import com.example.adenmedicaltest.ui.screens.ResultScreen
import com.example.adenmedicaltest.ui.screens.ResultsHistoryScreen
import com.example.adenmedicaltest.ui.screens.SubjectLevelsScreen
import com.example.adenmedicaltest.ui.screens.SyllabusScreen
import com.example.adenmedicaltest.ui.theme.AdenMedicalTestTheme
import com.example.adenmedicaltest.ui.viewmodel.AiTutorViewModel
import com.example.adenmedicaltest.ui.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                AdenMedicalTestTheme {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    var isOfflineMode by rememberSaveable { mutableStateOf(false) }
    var showPreAuthPrivacy by rememberSaveable { mutableStateOf(false) }

    if (showPreAuthPrivacy && user == null && !isOfflineMode) {
        AboutPrivacyScreen(
            onBack = { showPreAuthPrivacy = false }
        )
        return
    }

    if (user == null && !isOfflineMode) {
        // Navigation-Level Auth Gate with option for Offline Local Study Mode
        AuthScreen(
            onAuthSuccess = { isOfflineMode = false },
            onEnterOfflineMode = { isOfflineMode = true },
            onOpenAboutPrivacy = { showPreAuthPrivacy = true }
        )
    } else {
        val effectiveUid = user?.uid ?: ExamRepository.LOCAL_OFFLINE_USER_ID
        val effectiveEmail = user?.email ?: ""
        val effectiveDisplayName = user?.displayName ?: "طالب كلية الطب (الوضع المحلي)"

        // Keyed by effectiveUid so switching accounts creates a fresh ViewModel instance
        val viewModel: MainViewModel = viewModel(
            key = effectiveUid,
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY]) {
                        "APPLICATION_KEY missing from CreationExtras"
                    }
                    // CRITICAL: Resolve named Firestore database ID from string resources
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    MainViewModel(
                        examRepository = ExamRepository(db, app),
                        syllabusRepository = SyllabusRepository(app),
                        authRepository = AuthRepository(db),
                        currentUserId = effectiveUid,
                        currentUserEmail = effectiveEmail,
                        currentUserDisplayName = effectiveDisplayName
                    )
                }
            }
        )
        val aiViewModel: AiTutorViewModel = viewModel(key = "ai_$effectiveUid")
        MainApp(
            viewModel = viewModel,
            aiViewModel = aiViewModel,
            onExitOfflineMode = { isOfflineMode = false }
        )
    }
}

@Composable
fun MainApp(
    viewModel: MainViewModel,
    aiViewModel: AiTutorViewModel = viewModel(),
    onExitOfflineMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val activeExam by viewModel.activeExam.collectAsStateWithLifecycle()
    val savedDraft by viewModel.savedDraft.collectAsStateWithLifecycle()
    val lastResult by viewModel.lastResult.collectAsStateWithLifecycle()
    val wasAutoSubmittedOnTimeout by viewModel.wasAutoSubmittedOnTimeout.collectAsStateWithLifecycle()
    val subjectLevels by viewModel.subjectLevels.collectAsStateWithLifecycle()
    val selectedLevelsSubject by viewModel.selectedLevelsSubject.collectAsStateWithLifecycle()
    val syllabusMap by viewModel.syllabusMap.collectAsStateWithLifecycle()

    val explanationMap by aiViewModel.explanationMap.collectAsStateWithLifecycle()
    val hintMap by aiViewModel.hintMap.collectAsStateWithLifecycle()

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
                        onOpenResults = { viewModel.navigateTo(Screen.ResultsHistory) },
                        onOpenAiTutor = { viewModel.navigateTo(Screen.AiTutor) },
                        onOpenAboutPrivacy = { viewModel.navigateTo(Screen.AboutPrivacy) },
                        savedDraft = savedDraft,
                        onResumeDraft = { viewModel.resumeSavedDraft() },
                        onDiscardDraft = { viewModel.discardSavedDraft() }
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
                        onCancel = { viewModel.cancelExam() },
                        hintMap = hintMap,
                        onRequestHint = { question ->
                            val subjName = activeExam?.subject?.name ?: "عام"
                            aiViewModel.requestQuestionHint(subjName, question)
                        },
                        explanationMap = explanationMap,
                        onExplainSelection = { question, selectedKey ->
                            val subjName = activeExam?.subject?.name ?: "عام"
                            aiViewModel.explainQuestionSelectionStream(subjName, question, selectedKey)
                        }
                    )
                }

                is Screen.Result -> {
                    val res = lastResult ?: screen.result
                    ResultScreen(
                        result = res,
                        onBack = { viewModel.navigateTo(Screen.Dashboard) },
                        onRetry = { viewModel.retryCurrentExam() },
                        onHistory = { viewModel.navigateTo(Screen.ResultsHistory) },
                        wasAutoSubmittedOnTimeout = wasAutoSubmittedOnTimeout,
                        explanationMap = explanationMap,
                        onExplainAnswer = { answer ->
                            aiViewModel.explainAnswerStream(res.subjectName, answer)
                        }
                    )
                }

                is Screen.ResultsHistory -> {
                    ResultsHistoryScreen(
                        results = results,
                        onRetry = { subjectId, level -> viewModel.startExam(subjectId, level) },
                        onDelete = { id -> viewModel.deleteResult(id) },
                        onDashboard = { viewModel.navigateTo(Screen.Dashboard) },
                        onReviewResult = { item -> viewModel.openResultReview(item) }
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

                is Screen.AiTutor -> {
                    AiTutorScreen(
                        aiViewModel = aiViewModel,
                        onBack = { viewModel.navigateTo(Screen.Dashboard) }
                    )
                }

                is Screen.Profile -> {
                    ProfileScreen(
                        user = currentUser,
                        onUpdateFullName = { fullName -> viewModel.updateProfileFullName(fullName) },
                        onSignOut = {
                            if (currentUser?.userId == ExamRepository.LOCAL_OFFLINE_USER_ID) {
                                onExitOfflineMode()
                            } else {
                                signOut(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onSignOutComplete = {},
                                    scope = coroutineScope
                                )
                            }
                        },
                        onBack = { viewModel.navigateTo(Screen.Dashboard) },
                        onOpenAboutPrivacy = { viewModel.navigateTo(Screen.AboutPrivacy) }
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

                is Screen.AboutPrivacy -> {
                    AboutPrivacyScreen(
                        onBack = { viewModel.navigateTo(Screen.Dashboard) }
                    )
                }
            }
        }
    }
}
