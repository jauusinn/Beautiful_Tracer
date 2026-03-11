package com.beautifultracer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beautifultracer.app.ui.screen.TracerouteScreen
import com.beautifultracer.app.ui.theme.BeautifulTracerTheme
import com.beautifultracer.app.viewmodel.TracerouteViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BeautifulTracerTheme {
                val viewModel: TracerouteViewModel = viewModel()
                TracerouteScreen(viewModel = viewModel)
            }
        }
    }
}
