package com.example.adenmedicaltest.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Subject(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val durationMinutes: Int = 10,
    val totalQuestionCount: Int = 0
)

@Serializable
data class Option(
    val key: String,
    val text: String
)

@Serializable
data class Question(
    val id: String,
    val question: String,
    val options: List<Option>,
    val correctKey: String
)

data class SubjectLevel(
    val levelNumber: Int,
    val id: String,
    val title: String,
    val difficultyLabel: String,
    val questionCount: Int,
    val rangeLabel: String,
    val questionsPool: List<Question>
)

data class ExamAnswer(
    val questionId: String,
    val question: String,
    val userAnswer: String?,
    val correctAnswer: String,
    val isCorrect: Boolean,
    val options: List<Option>
)

data class ExamResult(
    val id: Long = 0,
    val subjectId: String,
    val subjectName: String,
    val level: String,
    val score: Int,
    val total: Int,
    val percentage: Int,
    val passed: Boolean,
    val timeTakenSeconds: Int,
    val answers: List<ExamAnswer> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class SyllabusSection(
    val heading: String,
    val items: List<String>
)

@Serializable
data class SyllabusData(
    val title: String,
    val sections: List<SyllabusSection>
)

data class UserProfile(
    val id: String = "student-1",
    val email: String = "student@aden.edu.ye",
    val username: String = "student",
    val fullName: String = "طالب كلية الطب",
    val role: String = "student"
)
