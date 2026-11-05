package com.android.andex.phonecontrol

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.andex.AndexApplication
import com.android.andex.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun EmbeddedTermuxSettings(onRefresh: () -> Unit, onOpenTerminal: () -> Unit) {
    val context = LocalContext.current
    val runtime = (context.applicationContext as AndexApplication).runtime
    val preferences = runtime.phoneControlController.preferences
    val scope = rememberCoroutineScope()
    var embedded by remember { mutableStateOf(preferences.embeddedTermux) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    fun prepare(makeDefault: Boolean) {
        busy = true
        scope.launch {
            try {
                val setup = runtime.bashTool.embeddedRuntime.initialize()
                error = setup.detail.takeUnless { setup.isReady }.orEmpty()
                if (setup.isReady) runtime.settingsRepository.markEmbeddedTermuxReady(makeDefault)
                onRefresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message.orEmpty()
            } finally {
                busy = false
            }
        }
    }

    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.phone_control_embedded), Modifier.weight(1f))
            Switch(checked = embedded, enabled = !busy, onCheckedChange = { value ->
                preferences.embeddedTermux = value
                embedded = value
                if (value) prepare(false) else onRefresh()
            })
        }
        Text(
            stringResource(R.string.termux_automatic_storage),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !busy && embedded, onClick = onOpenTerminal) {
                Text(stringResource(R.string.phone_control_terminal))
            }
            OutlinedButton(enabled = !busy && embedded, onClick = { prepare(true) }) {
                Text(stringResource(R.string.phone_control_default))
            }
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
    }
}
