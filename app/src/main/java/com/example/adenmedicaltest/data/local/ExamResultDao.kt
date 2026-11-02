package com.example.adenmedicaltest.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamResultDao {
    @Query("SELECT * FROM exam_results ORDER BY createdAt DESC")
    fun getAllResults(): Flow<List<ExamResultEntity>>

    @Query("SELECT * FROM exam_results WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun getResultsBySubject(subjectId: String): Flow<List<ExamResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: ExamResultEntity): Long

    @Query("DELETE FROM exam_results WHERE id = :id")
    suspend fun deleteResult(id: Long)

    @Query("DELETE FROM exam_results")
    suspend fun clearAll()
}
