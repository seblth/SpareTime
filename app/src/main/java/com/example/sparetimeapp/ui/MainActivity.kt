package com.example.sparetimeapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.sparetimeapp.ui.nav.NavGraph
import com.example.sparetimeapp.ui.theme.SpareTimeAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpareTimeAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(Modifier.padding(innerPadding)) {
                        NavGraph()
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    androidx.compose.material3.Text("Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SpareTimeAppTheme {
        Greeting("Android")
    }
}