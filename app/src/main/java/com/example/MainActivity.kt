package com.example

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.MediaRepository
import com.example.model.AppLanguage
import com.example.model.CameraSettings
import com.example.ui.CameraScreen
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.MyApplicationTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var mediaRepository: MediaRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mediaRepository = MediaRepository(applicationContext)

        setContent {
            var settings by remember { mutableStateOf(CameraSettings()) }

            // Apply selected language to activity resources if explicitly chosen
            val currentLanguage = settings.language
            val currentLocale = when (currentLanguage) {
                AppLanguage.ARABIC -> Locale("ar")
                AppLanguage.ENGLISH -> Locale("en")
                AppLanguage.SYSTEM -> resources.configuration.locales[0]
            }

            if (resources.configuration.locales[0].language != currentLocale.language) {
                val config = Configuration(resources.configuration)
                config.setLocale(currentLocale)
                @Suppress("DEPRECATION")
                resources.updateConfiguration(config, resources.displayMetrics)
            }

            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkObsidian
                ) {
                    CameraScreen(
                        mediaRepository = mediaRepository,
                        settings = settings,
                        onSettingsChange = { newSettings ->
                            settings = newSettings
                        }
                    )
                }
            }
        }
    }
}
