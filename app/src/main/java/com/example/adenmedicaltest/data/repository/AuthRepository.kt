package com.example.adenmedicaltest.data.repository

import android.content.Context
import com.example.adenmedicaltest.R
import com.example.adenmedicaltest.data.model.UserProfile
import com.example.adenmedicaltest.data.remote.OperationType
import com.example.adenmedicaltest.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val db: FirebaseFirestore
) {
    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth
        get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        userId: String,
        email: String,
        displayName: String?
    ): Result<UserProfile> {
        val docRef = db.collection("users").document(userId)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val profile = existing.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(
                    id = userId,
                    userId = userId,
                    email = email,
                    username = email.substringBefore("@").ifBlank { "student" },
                    fullName = displayName ?: "طالب كلية الطب",
                    role = "student"
                )
                Result.success(profile)
            } else {
                val cleanEmail = email.trim().take(200)
                val derivedUsername = cleanEmail.substringBefore("@").ifBlank { "student" }.take(100)
                val cleanFullName = (displayName?.trim()?.ifBlank { null } ?: "طالب كلية الطب").take(100)
                val payload = mapOf(
                    "userId" to userId,
                    "email" to cleanEmail,
                    "username" to derivedUsername,
                    "fullName" to cleanFullName,
                    "role" to "student",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                Result.success(
                    UserProfile(
                        id = userId,
                        userId = userId,
                        email = cleanEmail,
                        username = derivedUsername,
                        fullName = cleanFullName,
                        role = "student"
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateFullName(fullName: String): Result<Unit> {
        val uid = requireUserId()
        val cleanName = fullName.trim().take(100)
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال الاسم الكامل"))
        }
        val docRef = db.collection("users").document(uid)
        val currentEmail = auth.currentUser?.email ?: ""
        val username = currentEmail.substringBefore("@").ifBlank { "student" }.take(100)
        val updatePayload = mapOf(
            "username" to username,
            "fullName" to cleanName,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updatePayload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }
}
