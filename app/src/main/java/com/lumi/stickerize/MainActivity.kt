package com.lumi.stickerize

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.lumi.stickerize.ui.LumiApp
import com.lumi.stickerize.ui.MainViewModel
import com.lumi.stickerize.ui.theme.LumiTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Header aplikasi berwarna biru, jadi ikon status bar dibuat putih
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            LumiTheme {
                LumiApp(vm)
            }
        }
    }
}
