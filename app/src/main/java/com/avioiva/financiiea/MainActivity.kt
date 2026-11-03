package com.avioiva.financiiea

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.avioiva.financiiea.ui.AppRoot
import com.avioiva.financiiea.ui.theme.FinanciieaTheme

/**
 * FINANCIIEA opens directly on the Home dashboard — no login required.
 * (If the user enabled App Lock, a PIN screen appears first.)
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // The app always sits on the dark navy brand background → light status icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            FinanciieaTheme {
                AppRoot()
            }
        }
    }
}
