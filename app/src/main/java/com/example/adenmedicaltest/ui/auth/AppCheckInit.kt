package com.example.adenmedicaltest.ui.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.adenmedicaltest.BuildConfig
import com.example.adenmedicaltest.R
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

private const val TAG = "AppCheckInit"

// App Check contract keys defined by the Firebase SDK and runtime harness
const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
const val TOKEN_CACHE_STORE_PREFIX = "com.google.firebase.appcheck.store."

private val UUID_REGEX = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
private val JWT_REGEX = Regex("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")
private val API_KEY_REGEX = Regex("AIza[0-9A-Za-z_-]{35}")

data class AppCheckConfigurationStatus(
    val isConfigured: Boolean,
    val providerName: String,
    val isDebugMode: Boolean,
    val hasConfiguredDebugSecret: Boolean,
    val clearedStaleTokenCache: Boolean
)

@Volatile
private var lastConfiguredStatus: AppCheckConfigurationStatus? = null

fun getLastAppCheckStatus(): AppCheckConfigurationStatus? = lastConfiguredStatus

/**
 * Sanitizes technical log messages to guarantee no secret tokens, JWTs, or API keys
 * are ever exposed in Logcat while preserving diagnostic error codes and types.
 */
fun sanitizeLogMessage(raw: String?): String {
    if (raw.isNullOrBlank()) return "Unknown error (empty message)"
    return raw
        .replace(JWT_REGEX, "[REDACTED_JWT]")
        .replace(UUID_REGEX, "[REDACTED_DEBUG_TOKEN]")
        .replace(API_KEY_REGEX, "[REDACTED_API_KEY]")
}

/**
 * Configures Firebase App Check before any Firebase AI Logic (Gemini) or Firestore calls:
 * - In Debug builds (BuildConfig.DEBUG): Uses DebugAppCheckProviderFactory, persists the
 *   harness/console debug secret synchronously with commit(), and clears any stale invalid
 *   cached AppCheckToken when the secret is injected or updated.
 * - In Release builds (!BuildConfig.DEBUG): Uses PlayIntegrityAppCheckProviderFactory
 *   (with RecaptchaAppCheckProviderFactory fallback if needed) and NEVER DebugAppCheckProviderFactory.
 */
fun configureAppCheck(
    context: Context,
    intent: Intent?,
    isDebugBuild: Boolean = BuildConfig.DEBUG
): AppCheckConfigurationStatus {
    return try {
        val appContext = context.applicationContext ?: context

        // 1. Ensure FirebaseApp is initialized before accessing persistenceKey or FirebaseAppCheck
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            FirebaseApp.initializeApp(appContext)
        }

        val firebaseApp = FirebaseApp.getInstance()
        val persistenceKey = firebaseApp.persistenceKey
        val firebaseAppCheck = FirebaseAppCheck.getInstance(firebaseApp)
        firebaseAppCheck.setTokenAutoRefreshEnabled(true)

        if (isDebugBuild) {
            // 2. Development / Debug Environment: Use DebugAppCheckProviderFactory ONLY in debug builds
            val debugPrefs = appContext.getSharedPreferences(
                "$DEBUG_STORE_PREFIX$persistenceKey",
                Context.MODE_PRIVATE
            )
            val tokenCachePrefs = appContext.getSharedPreferences(
                "$TOKEN_CACHE_STORE_PREFIX$persistenceKey",
                Context.MODE_PRIVATE
            )

            val incomingDebugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)?.trim()
            val existingSecret = debugPrefs.getString(DEBUG_SECRET_KEY, null)?.trim()
            var clearedCache = false

            if (!incomingDebugToken.isNullOrEmpty()) {
                val secretChanged = existingSecret != incomingDebugToken
                // CRITICAL: Use commit() synchronously so DebugAppCheckProviderFactory reads
                // the exact secret immediately on installation.
                debugPrefs.edit()
                    .putString(DEBUG_SECRET_KEY, incomingDebugToken)
                    .commit()

                // CRITICAL: Clear cached AppCheckToken in com.google.firebase.appcheck.store.<persistenceKey>
                // so any previously cached invalid/unverified token is discarded before calling Gemini.
                if (secretChanged || tokenCachePrefs.all.isNotEmpty()) {
                    tokenCachePrefs.edit().clear().commit()
                    clearedCache = true
                }
                Log.d(TAG, "App Check debug secret persisted synchronously from Intent (secret redacted).")
            } else if (!existingSecret.isNullOrEmpty()) {
                Log.d(TAG, "Using existing persisted App Check debug secret from SharedPreferences (secret redacted).")
            } else {
                // No Intent secret and no persisted secret yet; clear any corrupt cached token
                if (tokenCachePrefs.all.isNotEmpty()) {
                    tokenCachePrefs.edit().clear().commit()
                    clearedCache = true
                }
                Log.w(
                    TAG,
                    "No FIREBASE_APPCHECK_DEBUG_TOKEN found in Intent extra. " +
                        "DebugAppCheckProviderFactory will generate a local debug token; ensure it is registered in Firebase Console > App Check."
                )
            }

            val hasSecret = !incomingDebugToken.isNullOrEmpty() || !existingSecret.isNullOrEmpty()
            val debugFactory: AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(debugFactory, true)
            Log.d(TAG, "DebugAppCheckProviderFactory installed for development build.")

            AppCheckConfigurationStatus(
                isConfigured = true,
                providerName = "DebugAppCheckProviderFactory",
                isDebugMode = true,
                hasConfiguredDebugSecret = hasSecret,
                clearedStaleTokenCache = clearedCache
            ).also { lastConfiguredStatus = it }
        } else {
            // 3. Production / Release Environment: Use PlayIntegrityAppCheckProviderFactory (NEVER Debug provider)
            val providerName = try {
                val playIntegrityFactory = PlayIntegrityAppCheckProviderFactory.getInstance()
                firebaseAppCheck.installAppCheckProviderFactory(playIntegrityFactory, true)
                Log.d(TAG, "PlayIntegrityAppCheckProviderFactory installed for release build.")
                "PlayIntegrityAppCheckProviderFactory"
            } catch (playIntegrityError: Exception) {
                Log.w(
                    TAG,
                    "PlayIntegrityAppCheckProviderFactory setup fallback triggered: ${sanitizeLogMessage(playIntegrityError.message)}"
                )
                val siteKey = runCatching { appContext.getString(R.string.recaptcha_site_key).trim() }.getOrDefault("")
                if (siteKey.isNotEmpty()) {
                    firebaseAppCheck.installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey),
                        true
                    )
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed as release fallback.")
                    "RecaptchaAppCheckProviderFactory"
                } else {
                    throw playIntegrityError
                }
            }

            AppCheckConfigurationStatus(
                isConfigured = true,
                providerName = providerName,
                isDebugMode = false,
                hasConfiguredDebugSecret = false,
                clearedStaleTokenCache = false
            ).also { lastConfiguredStatus = it }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check: ${sanitizeLogMessage(e.message)}", e)
        AppCheckConfigurationStatus(
            isConfigured = false,
            providerName = "Uninitialized",
            isDebugMode = isDebugBuild,
            hasConfiguredDebugSecret = false,
            clearedStaleTokenCache = false
        ).also { lastConfiguredStatus = it }
    }
}

/**
 * Ensures an App Check token is fetched or refreshed prior to or during a Firebase AI Logic request.
 * Never logs the raw token string.
 */
suspend fun ensureAppCheckTokenReady(
    context: Context? = null,
    forceRefresh: Boolean = false
): Boolean {
    return try {
        if (context != null && lastConfiguredStatus == null) {
            configureAppCheck(context, null)
        }
        val firebaseApp = FirebaseApp.getInstance()
        if (forceRefresh) {
            // Also clear local token cache store when forcing refresh after a 403 / invalid token error
            val appCtx = firebaseApp.applicationContext
            val persistenceKey = firebaseApp.persistenceKey
            appCtx.getSharedPreferences("$TOKEN_CACHE_STORE_PREFIX$persistenceKey", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
        }
        val tokenResult = withTimeout(10_000L) {
            FirebaseAppCheck.getInstance(firebaseApp).getAppCheckToken(forceRefresh).await()
        }
        val isTokenPresent = tokenResult.token.isNotBlank()
        Log.d(
            TAG,
            "App Check token ready (forceRefresh=$forceRefresh, valid=${isTokenPresent}, expireTimeMillis=${tokenResult.expireTimeMillis})"
        )
        isTokenPresent
    } catch (e: Exception) {
        Log.w(
            TAG,
            "App Check token pre-fetch warning (forceRefresh=$forceRefresh): ${e.javaClass.simpleName} - ${sanitizeLogMessage(e.message)}"
        )
        false
    }
}
