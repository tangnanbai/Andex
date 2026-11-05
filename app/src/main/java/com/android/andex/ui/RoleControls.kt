package com.android.andex.ui
import com.android.andex.ui.AndexGlassAlertDialog as AlertDialog

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.android.andex.data.AgentRole
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun RoleAvatar(avatar: String, modifier: Modifier = Modifier) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, avatar) {
        value = withContext(Dispatchers.IO) {
            if (avatar.startsWith("data:image/")) runCatching {
                val bytes = android.util.Base64.decode(avatar.substringAfter(","), android.util.Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull() else if (avatar.startsWith("/")) runCatching {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(avatar, options)
                options.inSampleSize = (maxOf(options.outWidth, options.outHeight) / 128).coerceAtLeast(1)
                options.inJustDecodeBounds = false
                BitmapFactory.decodeFile(avatar, options)
            }.getOrNull() else null
        }
    }
    if (bitmap != null) Image(bitmap!!.asImageBitmap(), "角色头像",
        modifier.size(32.dp).clip(CircleShape), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
    else Box(modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text(avatar.takeIf { !it.startsWith("/") && !it.startsWith("data:image/") }.orEmpty().ifBlank { "🤖" })
    }
}

@Composable
internal fun RolePicker(roles: List<AgentRole>, selectedId: String, defaultId: String,
                        onSelected: (String) -> Unit) {
    RoleCapsule(roles, selectedId, defaultId,
        avatar = { value, modifier -> RoleAvatar(value, modifier) }, onSelected = onSelected)

}

@Composable
internal fun RoleEditor(roles: List<AgentRole>, defaultId: String, selectedId: String,
                       onChange: (List<AgentRole>, String, String) -> Unit) {
    var editing by remember { mutableStateOf<AgentRole?>(null) }
    var name by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf("🤖") }
    var error by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, options) }
                    options.inSampleSize = (maxOf(options.outWidth, options.outHeight) / 512).coerceAtLeast(1)
                    options.inJustDecodeBounds = false
                    context.contentResolver.openInputStream(uri)!!.use { input ->
                        val image = BitmapFactory.decodeStream(input, null, options) ?: kotlin.error("无法读取图片")
                        val scale = minOf(1f, 256f / maxOf(image.width, image.height))
                        val resized = android.graphics.Bitmap.createScaledBitmap(image,
                            (image.width * scale).toInt().coerceAtLeast(1), (image.height * scale).toInt().coerceAtLeast(1), true)
                        val output = java.io.ByteArrayOutputStream()
                        resized.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
                        "data:image/png;base64," + android.util.Base64.encodeToString(output.toByteArray(), android.util.Base64.NO_WRAP)
                    }
                }
            }.onSuccess { avatar = it; error = "" }.onFailure { error = "头像读取失败" }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("选择角色后，每次回答都会载入它的设定。新对话使用默认角色。",
            style = MaterialTheme.typography.bodySmall)
        RolePicker(roles, selectedId, defaultId) { onChange(roles, defaultId, it) }
        roles.forEach { role ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row {
                        RoleAvatar(role.avatar)
                        Text(role.name, Modifier.padding(start = 8.dp).weight(1f))
                        TextButton(onClick = { editing = role; name = role.name; prompt = role.prompt; avatar = role.avatar }) { Text("编辑") }
                    }
                    Text(role.prompt, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Row {
                        TextButton(onClick = { onChange(roles, role.id, selectedId) }) { Text(if (defaultId == role.id) "默认角色 ✓" else "设为默认") }
                        TextButton(onClick = {
                            val remaining = roles.filterNot { it.id == role.id }
                            val nextDefault = defaultId.takeUnless { it == role.id } ?: remaining.firstOrNull()?.id.orEmpty()
                            onChange(remaining, nextDefault, selectedId.takeUnless { it == role.id } ?: nextDefault)
                        }) { Text("删除") }
                    }
                }
            }
        }
        OutlinedButton(onClick = {
            editing = AgentRole(UUID.randomUUID().toString(), "")
            name = ""; prompt = ""; avatar = "🤖"; error = ""
        }) { Text("＋ 添加角色") }
    }
    if (editing != null) AlertDialog(
        onDismissRequest = { editing = null },
        title = { Text("角色定义") },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("角色名称") }, singleLine = true)
                Row { RoleAvatar(avatar); TextButton(onClick = { picker.launch("image/*") }) { Text("选择头像") } }
                OutlinedTextField(prompt, { prompt = it }, label = { Text("角色提示词") }, minLines = 4, maxLines = 7)
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                val item = editing!!.copy(name = name.trim(), prompt = prompt, avatar = avatar.ifBlank { "🤖" })
                val list = roles.filterNot { it.id == item.id } + item
                onChange(list, defaultId.ifBlank { item.id }, selectedId.ifBlank { item.id })
                editing = null
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = { editing = null }) { Text("取消") } },
    )
}
