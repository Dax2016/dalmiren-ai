package com.darchums.dalmiren.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.darchums.dalmiren.domain.model.AgeGroup
import com.darchums.dalmiren.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dalmiren_user_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DEFAULT_AGE_GROUP = stringPreferencesKey("default_age_group")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val ageStr = prefs[Keys.DEFAULT_AGE_GROUP] ?: AgeGroup.EARLY_CHILDHOOD.name
        val ageGroup = runCatching { AgeGroup.valueOf(ageStr) }.getOrDefault(AgeGroup.EARLY_CHILDHOOD)
        UserPreferences(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            defaultAgeGroup = ageGroup,
            darkTheme = prefs[Keys.DARK_THEME]
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setDefaultAgeGroup(ageGroup: AgeGroup) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_AGE_GROUP] = ageGroup.name
        }
    }

    suspend fun setDarkTheme(enabled: Boolean?) {
        context.dataStore.edit { prefs ->
            if (enabled == null) {
                prefs.remove(Keys.DARK_THEME)
            } else {
                prefs[Keys.DARK_THEME] = enabled
            }
        }
    }

    suspend fun clearAllPreferences() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
