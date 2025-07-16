package com.plp.attendance.ui.config

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppConfigScreen(
    onConfigComplete: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "App Configuration",
                style = MaterialTheme.typography.headlineMedium
            )
            Button(onClick = { onConfigComplete(true) }) {
                Text("Demo Mode")
            }
            Button(onClick = { onConfigComplete(false) }) {
                Text("Production Mode")
            }
        }
    }
}