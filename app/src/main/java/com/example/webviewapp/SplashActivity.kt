package com.example.webviewapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

/**
 * Splash screen: kuch der logo dikhata hai, phir MainActivity (WebView) khol deta hai.
 */
class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY_MS = 1200L

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_WebViewApp_Splash)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, SPLASH_DELAY_MS)
    }
}
