package com.plp.attendance.services

import android.content.Context
import android.content.res.Configuration
import com.plp.attendance.data.preferences.AppPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Singleton

@Singleton
class LocalizationManager(
    private val context: Context,
    private val appPreferences: AppPreferences
) {
    
    companion object {
        const val KHMER_LANGUAGE_CODE = "km"
        const val ENGLISH_LANGUAGE_CODE = "en"
        const val DEFAULT_LANGUAGE = KHMER_LANGUAGE_CODE // Default to Khmer
    }
    
    /**
     * Initialize app locale on startup
     */
    fun initializeLocale() {
        val savedLanguage = runBlocking { 
            appPreferences.language.first() 
        }
        setLanguage(savedLanguage)
    }
    
    /**
     * Set the app language
     */
    fun setLanguage(languageCode: String) {
        val locale = when (languageCode) {
            KHMER_LANGUAGE_CODE -> Locale("km", "KH")
            ENGLISH_LANGUAGE_CODE -> Locale.ENGLISH
            else -> Locale(DEFAULT_LANGUAGE, "KH") // Default to Khmer
        }
        
        updateLocale(locale)
    }
    
    /**
     * Update the app locale
     */
    private fun updateLocale(locale: Locale) {
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
    
    /**
     * Get current language code
     */
    fun getCurrentLanguage(): String {
        return runBlocking { 
            appPreferences.language.first() 
        }
    }
    
    /**
     * Save language preference
     */
    suspend fun saveLanguagePreference(languageCode: String) {
        appPreferences.setLanguage(languageCode)
        setLanguage(languageCode)
    }
    
    /**
     * Create a context with specific locale
     */
    fun createLocalizedContext(baseContext: Context, languageCode: String = getCurrentLanguage()): Context {
        val locale = when (languageCode) {
            KHMER_LANGUAGE_CODE -> Locale("km", "KH")
            ENGLISH_LANGUAGE_CODE -> Locale.ENGLISH
            else -> Locale(DEFAULT_LANGUAGE, "KH")
        }
        
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        
        return baseContext.createConfigurationContext(config)
    }
}