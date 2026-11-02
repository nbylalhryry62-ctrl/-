package com.example.adenmedicaltest.data.repository

import com.example.adenmedicaltest.base.FirestoreEmulatorTestBase
import com.example.adenmedicaltest.data.model.ExamResult
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExamRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveResult_validPayload_createsDocumentAndReturnsId() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = ExamRepository(firestore)

        val examResult = ExamResult(
            userId = uid,
            subjectId = "biology",
            subjectName = "الأحياء",
            level = "1",
            score = 20,
            total = 25,
            percentage = 80,
            passed = true,
            timeTakenSeconds = 320
        )

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveResult(examResult) }
        assertTrue(saveResult.isSuccess)
        val resultId = saveResult.getOrThrow()
        assertTrue(resultId.isNotBlank())
    }

    @Test
    fun getUserResults_authenticatedOwner_returnsMatchingResults() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = ExamRepository(firestore)

        val examResult = ExamResult(
            userId = uid,
            subjectId = "chemistry",
            subjectName = "الكيمياء",
            level = "2",
            score = 18,
            total = 25,
            percentage = 72,
            passed = true,
            timeTakenSeconds = 400
        )

        val resultId = withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveResult(examResult).getOrThrow() }
        val resultsList = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getUserResults(uid).getOrThrow() }
        assertTrue(resultsList.any { it.id == resultId })
    }

    @Test
    fun observeResults_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = ExamRepository(firestore)

        val examResult = ExamResult(
            userId = uid,
            subjectId = "english",
            subjectName = "اللغة الإنجليزية",
            level = "1",
            score = 22,
            total = 25,
            percentage = 88,
            passed = true,
            timeTakenSeconds = 290
        )

        val resultId = withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveResult(examResult).getOrThrow() }

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeResults(uid).first { list -> list.any { it.id == resultId } }
        }
        assertTrue(emitted.any { it.id == resultId })
    }

    @Test
    fun getResultById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = ExamRepository(firestore)

        val examResult = ExamResult(
            userId = aliceUid,
            subjectId = "biology",
            subjectName = "الأحياء",
            level = "1",
            score = 19,
            total = 25,
            percentage = 76,
            passed = true,
            timeTakenSeconds = 310
        )
        val resultId = withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.saveResult(examResult).getOrThrow() }

        signInTestUser(BOB_EMAIL)
        val bobRepo = ExamRepository(firestore)
        val exception = assertFailsWith<FirebaseFirestoreException> {
            withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getResultById(aliceUid, resultId).getOrThrow() }
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception.code)
    }

    @Test
    fun observeResults_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = ExamRepository(firestore)

        val exception = assertFailsWith<Exception> {
            withTimeout(FLOW_TIMEOUT_MS) { repository.observeResults("unauthenticated_user").first() }
        }
        val firestoreException = assertNotNull(exception.unwrapFirestoreException())
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@aden.edu.ye"
        const val BOB_EMAIL = "bob@aden.edu.ye"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
