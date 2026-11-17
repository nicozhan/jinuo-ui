package com.jinuo.kiosk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.jinuo.kiosk.ui.KioskApp
import com.jinuo.kiosk.ui.Screen
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
            // The whole UI is authored in Figma artboard pixels (1080x1920),
            // treating 1 design px == 1 dp. Pin density to 1 so this holds on
            // every device: the 1080dp board maps to exactly 1080 physical px
            // and matches both the Compose Preview and the HTML prototype.
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                KioskTheme {
                    // Optional debug deep-link: `am start --es screen <name> [--ez cart true]`
                    // Names: home | lang | help | shopping | custom | cart |
                    //        orderconfirm | cardpay | wrong | paysuccess | making | ready.
                    // help/cart/language are global overlays; deep links open them
                    // over the shopping page so the real UX can be inspected.
                    val screenExtra = intent?.getStringExtra("screen")
                    val start = when (screenExtra) {
                        "help" -> Screen.Shopping
                        "cart" -> Screen.Shopping
                        "lang" -> Screen.Lang
                        "shopping" -> Screen.Shopping
                        "custom" -> Screen.Custom(intent.getStringExtra("product") ?: "americano")
                        "orderconfirm" -> Screen.OrderConfirm
                        "cardpay" -> Screen.CardPay
                        "wrong" -> Screen.Wrong
                        "paysuccess" -> Screen.PaySuccess
                        "making" -> Screen.Making
                        "ready" -> Screen.Ready
                        else -> Screen.Home
                    }
                    KioskApp(
                        initialScreen = start,
                        initialCart = screenExtra == "cart" ||
                            intent?.getBooleanExtra("cart", false) == true,
                        initialHelp = screenExtra == "help",
                        initialLangOpen = intent?.getBooleanExtra("langPicker", false) == true,
                        initialLangAnchor = if (screenExtra == "shopping") Pair(480f, 155f)
                            else if (screenExtra == "custom") Pair(765f, 155f)
                            else Pair(66f, 1380f),
                    )
                }
            }
        }
    }
}
