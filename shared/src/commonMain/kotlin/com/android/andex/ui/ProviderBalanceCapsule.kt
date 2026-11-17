package com.android.andex.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.andex.data.ProviderModelOption
import com.android.andex.data.SharedProviderBalanceClient
import com.android.andex.ui.theme.AndexOnSurface
import kotlinx.coroutines.delay

@Composable
fun ProviderBalanceCapsule(option: ProviderModelOption?) {
    val client = remember { SharedProviderBalanceClient() }
    DisposableEffect(client) { onDispose { client.close() } }
    var balance by remember(option) { mutableStateOf<String?>(null) }
    LaunchedEffect(option) {
        val selected = option ?: return@LaunchedEffect
        while (true) {
            balance = client.fetch(selected)
            delay(60_000)
        }
    }
    balance?.let { value ->
        ConversationCapsule(horizontalPadding = 0.dp) {
            Text(value, Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
                color = AndexOnSurface, maxLines = 1)
        }
    }
}
