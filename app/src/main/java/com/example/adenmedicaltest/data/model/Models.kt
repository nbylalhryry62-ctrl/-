package com.example.adenmedicaltest.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.serialization.Serializable

@Serializable
data class Subject(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val description: String = "",
    val durationMinutes: Int = 10,
    val totalQuestionCount: Int = 0
)

@Serializable
data class Option(
    val key: String = "",
    val text: String = ""
)

@Serializable
data class Question(
    val id: String = "",
    val question: String = "",
    val options: List<Option> = emptyList(),
    val correctKey: String = ""
)

data class SubjectLevel(
    val levelNumber: Int = 1,
    val id: String = "1",
    val title: String = "",
    val difficultyLabel: String = "",
    val questionCount: Int = 0,
    val rangeLabel: String = "",
    val questionsPool: List<Question> = emptyList()
)

data class ExamAnswer(
    val questionId: String = "",
    val question: String = "",
    val userAnswer: String? = null,
    val correctAnswer: String = "",
    val isCorrect: Boolean = false,
    val options: List<Option> = emptyList()
)

@IgnoreExtraProperties
data class ExamResult(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val level: String = "1",
    val score: Int = 0,
    val total: Int = 0,
    val percentage: Int = 0,
    val passed: Boolean = false,
    val timeTakenSeconds: Int = 0,
    val answers: List<ExamAnswer> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Serializable
data class SyllabusSection(
    val heading: String = "",
    val items: List<String> = emptyList()
)

@Serializable
data class SyllabusData(
    val title: String = "",
    val sections: List<SyllabusSection> = emptyList()
)

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val username: String = "",
    val fullName: String = "",
    val role: String = "student",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
