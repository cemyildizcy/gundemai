package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "gundem_user_settings")

class UserPreferencesRepository(private val context: Context) {
    private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    private val AUTH_COMPLETED = booleanPreferencesKey("auth_completed")
    private val FOLLOWED_CATEGORIES = stringSetPreferencesKey("followed_categories")
    private val FOLLOWED_TOPICS = stringSetPreferencesKey("followed_topics")
    private val DARK_THEME_ENABLED = booleanPreferencesKey("dark_theme_enabled")
    private val USER_EMAIL = androidx.datastore.preferences.core.stringPreferencesKey("user_email")
    private val USER_NAME = androidx.datastore.preferences.core.stringPreferencesKey("user_name")
    private val IS_PRO_USER = booleanPreferencesKey("is_pro_user")
    private val PRO_PLAN_PERIOD = androidx.datastore.preferences.core.stringPreferencesKey("pro_plan_period")

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETED] ?: false
    }

    val authCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTH_COMPLETED] ?: false
    }

    val followedCategories: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[FOLLOWED_CATEGORIES] ?: setOf("Sana Özel", "Son Dakika", "Yapay Zekâ", "Teknoloji", "Türkiye")
    }

    val followedTopics: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[FOLLOWED_TOPICS] ?: setOf("openai", "google", "gemini", "fenerbahce", "yapay_zeka_trendler")
    }

    val darkThemeEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DARK_THEME_ENABLED] ?: true
    }

    val userEmail: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_EMAIL]
    }

    val userName: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_NAME]
    }

    val isProUser: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_PRO_USER] ?: false
    }

    val proPlanPeriod: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PRO_PLAN_PERIOD] ?: "YEARLY"
    }

    suspend fun setUserAccount(email: String?, name: String?) {
        context.dataStore.edit { prefs ->
            if (email != null) prefs[USER_EMAIL] = email else prefs.remove(USER_EMAIL)
            if (name != null) prefs[USER_NAME] = name else prefs.remove(USER_NAME)
        }
    }

    suspend fun setProUserStatus(isPro: Boolean, planPeriod: String = "YEARLY") {
        context.dataStore.edit { prefs ->
            prefs[IS_PRO_USER] = isPro
            prefs[PRO_PLAN_PERIOD] = planPeriod
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setAuthCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AUTH_COMPLETED] = completed
        }
    }

    suspend fun updateFollowedCategories(categories: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[FOLLOWED_CATEGORIES] = categories
        }
    }

    suspend fun updateFollowedTopics(topics: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[FOLLOWED_TOPICS] = topics
        }
    }

    suspend fun setDarkThemeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DARK_THEME_ENABLED] = enabled
        }
    }

    suspend fun clearForAccountDeletion() {
        context.dataStore.edit { prefs -> prefs.clear() }
    }
}
