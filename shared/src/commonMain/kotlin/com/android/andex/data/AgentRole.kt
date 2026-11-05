package com.android.andex.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Serializable
data class AgentRole(val id: String, val name: String, val prompt: String = "", val avatar: String = "🤖")

private val RoleJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
fun serializeAgentRoles(roles: List<AgentRole>): String = RoleJson.encodeToString(roles)
fun parseAgentRoles(value: String): List<AgentRole> = runCatching { RoleJson.decodeFromString<List<AgentRole>>(value) }.getOrDefault(emptyList())
fun AppSettings.activeRole(): AgentRole? = roles.firstOrNull { it.id == selectedRoleId }
    ?: roles.firstOrNull { it.id == defaultRoleId }
fun AppSettings.effectiveSystemPrompt(): String = listOf(systemPrompt, activeRole()?.let {
    "Role: ${it.name}\n${it.prompt}"
}.orEmpty()).filter { it.isNotBlank() }.joinToString("\n\n")
