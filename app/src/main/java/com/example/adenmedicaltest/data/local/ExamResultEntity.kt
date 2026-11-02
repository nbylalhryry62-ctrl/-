package com.example.adenmedicaltest.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exam_results")
data class ExamResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: String,
    val subjectName: String,
    val level: String,
    val score: Int,
    val total: Int,
    val percentage: Int,
    val passed: Boolean,
    val timeTakenSeconds: Int,
    val answersJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
