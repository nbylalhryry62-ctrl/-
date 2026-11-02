package com.example.adenmedicaltest.data.repository

import com.example.adenmedicaltest.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository {

    private val _currentUser = MutableStateFlow<UserProfile?>(
        UserProfile(
            id = "student-1",
            email = "student@aden.edu.ye",
            username = "student",
            fullName = "طالب كلية الطب",
            role = "student"
        )
    )
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    fun login(email: String, role: String = "student"): UserProfile {
        val cleanEmail = email.trim().lowercase()
        val username = cleanEmail.substringBefore("@").ifEmpty { "مستخدم" }
        val user = UserProfile(
            id = "user-${System.currentTimeMillis()}",
            email = cleanEmail,
            username = username,
            fullName = if (role == "admin") "المشرف العام" else "طالب كلية الطب",
            role = role
        )
        _currentUser.value = user
        return user
    }

    fun logout() {
        _currentUser.value = null
    }

    fun toggleRole() {
        val current = _currentUser.value ?: return
        val newRole = if (current.role == "admin") "student" else "admin"
        _currentUser.value = current.copy(role = newRole)
    }
}
