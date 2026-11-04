package com.example.adenmedicaltest

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class AdenMedicalApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d(TAG, "FirebaseApp initialized in AdenMedicalApplication.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FirebaseApp in Application.onCreate", e)
        }
    }

    companion object {
        private const val TAG = "AdenMedicalApp"
    }
}
