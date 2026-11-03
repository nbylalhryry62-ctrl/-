package com.example.adenmedicaltest.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object ResultsHistory : Screen("results_history")
    data object Syllabus : Screen("syllabus")
    data object AiTutor : Screen("ai_tutor")
    data object Profile : Screen("profile")
    data object Admin : Screen("admin")

    data class SubjectLevels(val subjectId: String) : Screen("levels/$subjectId")
    data class Exam(val subjectId: String, val level: String) : Screen("exam/$subjectId/$level")
    data class Result(val result: com.example.adenmedicaltest.data.model.ExamResult) : Screen("result")
}
