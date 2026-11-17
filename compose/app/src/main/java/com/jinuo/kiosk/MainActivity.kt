package com.jinuo.kiosk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.jinuo.kiosk.ui.CustomScreen
import com.jinuo.kiosk.ui.theme.KioskTheme

/**
 * Jinuo coffee kiosk — Jetpack Compose prototype of
 * pages/05-custom.html?debug=1 (the product configuration screen).
 *
 * This is a real Android application, NOT a WebView and NOT HTML.
 * All layout is expressed in 1080 x 1920 artboard coordinates (1 px = 1 dp)
 * and scaled to the device by the Artboard composable.
 *
 * Build:  ./gradlew :app:assembleDebug
 * Run:    install app/build/outputs/apk/debug/app-debug.apk on a device/emulator
 * Resync: python3 tools/sync_from_web.py   (re-copies artwork + catalogue)
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The design carries its own status row; hide the system bars.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView)
            .hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            KioskTheme {
                CustomScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
