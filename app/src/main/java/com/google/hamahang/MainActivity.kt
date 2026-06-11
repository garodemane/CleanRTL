package com.google.hamahang

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.hamahang.features.editor.EditorScreen
import com.google.hamahang.theme.CleanRTLTheme
import com.google.hamahang.core.bidi.AppLanguage
import com.google.hamahang.core.bidi.AppThemeMode

class MainActivity : ComponentActivity() {
    
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Handle incoming text shares from other apps
        val sharedText = handleIncomingShareIntent(intent)

        setContent {
            // Instantiate SharedPreferences securely inside Compose
            val prefs = remember { getSharedPreferences("cleanrtl_settings_pref", Context.MODE_PRIVATE) }

            // Restore states locally from SharedPreferences
            var themeMode by remember { 
                mutableStateOf(
                    AppThemeMode.valueOf(prefs.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name)
                ) 
            }
            var currentLanguage by remember { 
                mutableStateOf(
                    AppLanguage.valueOf(prefs.getString("app_language", AppLanguage.FA.name) ?: AppLanguage.FA.name)
                ) 
            }
            var enableNormalization by remember {
                mutableStateOf(prefs.getBoolean("enable_normalization", true))
            }
            var uiFontScale by remember {
                mutableStateOf(prefs.getFloat("ui_font_scale", 1.0f))
            }

            // Compute actual dark theme based on persisted setting
            val darkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            CleanRTLTheme(darkTheme = darkTheme) {
                val windowSizeClass = calculateWindowSizeClass(this)
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EditorScreen(
                        windowWidthSizeClass = windowSizeClass.widthSizeClass,
                        sharedText = sharedText,
                        
                        // Pass theme states & persistence lambdas
                        themeMode = themeMode,
                        onThemeChange = { mode ->
                            themeMode = mode
                            prefs.edit().putString("theme_mode", mode.name).apply()
                        },
                        
                        // Pass language states & persistence lambdas
                        currentLanguage = currentLanguage,
                        onLanguageChange = { lang ->
                            currentLanguage = lang
                            prefs.edit().putString("app_language", lang.name).apply()
                        },
                        
                        // Pass normalization states & persistence lambdas
                        enableNormalization = enableNormalization,
                        onNormalizationChange = { norm ->
                            enableNormalization = norm
                            prefs.edit().putBoolean("enable_normalization", norm).apply()
                        },
                        
                        // Pass UI Font Scale states & persistence lambdas
                        uiFontScale = uiFontScale,
                        onUiFontScaleChange = { scale ->
                            uiFontScale = scale
                            prefs.edit().putFloat("ui_font_scale", scale).apply()
                        }
                    )
                }
            }
        }
    }

    private fun handleIncomingShareIntent(intent: Intent?): String? {
        if (intent == null) return null
        val action = intent.action
        val type = intent.type
        
        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                return intent.getStringExtra(Intent.EXTRA_TEXT)
            }
        }
        return null
    }
}
