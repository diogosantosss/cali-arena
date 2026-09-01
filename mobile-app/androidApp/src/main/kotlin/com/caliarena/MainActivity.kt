package com.caliarena

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.caliarena.auth.initDataStoreContext
import com.caliarena.di.initKoin
import com.caliarena.di.initKoinAndroid

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        initDataStoreContext(applicationContext)
        initKoinAndroid(applicationContext)
        initKoin()

        setContent {
            CaliArenaApp()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    CaliArenaApp()
}
