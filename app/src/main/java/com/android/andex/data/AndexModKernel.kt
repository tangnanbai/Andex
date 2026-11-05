package com.android.andex.data

import com.android.andex.mod.AndexNativeComponentRegistry
import java.util.concurrent.atomic.AtomicLong
import org.json.JSONArray
import org.json.JSONObject

data class AndexModServiceMethod(
    val name: String,
    val description: String = "",
    val mutatesState: Boolean = false,
)

data class AndexModServiceDescriptor(
    val id: String,
    val description: String = "",
    val owner: String,
    val priority: Int,
    val methods: List<AndexModServiceMethod>,
)

fun interface AndexModServiceHandler {
    suspend fun invoke(
        method: String,
        args: JSONObject,
    ): JSONObject
}

class AndexModServiceRegistry {
    private data class Registration(
        val sequence: Long,
        val descriptor: AndexModServiceDescriptor,
        val handler: AndexModServiceHandler,
    )

    private val lock = Any()
    private val sequence = AtomicLong()
    private val registrations = linkedMapOf<String, MutableList<Registration>>()

    fun register(
        id: String,
        owner: String,
        description: String = "",
        priority: Int = 0,
        methods: List<AndexModServiceMethod>,
        handler: AndexModServiceHandler,
    ): () -> Unit {
        val normalizedId = id.trim()
        require(normalizedId.isNotBlank()) { "Andex mod services require an id." }
        val registration = Registration(
            sequence = sequence.incrementAndGet(),
            descriptor = AndexModServiceDescriptor(
                id = normalizedId,
                description = description,
                owner = owner.trim().ifBlank { "unknown" },
                priority = priority,
                methods = methods.distinctBy(AndexModServiceMethod::name),
            ),
            handler = handler,
        )
        synchronized(lock) {
            registrations.getOrPut(normalizedId, ::mutableListOf).add(registration)
        }
        return {
            synchronized(lock) {
                registrations[normalizedId]?.remove(registration)
                if (registrations[normalizedId].isNullOrEmpty()) {
                    registrations.remove(normalizedId)
                }
            }
        }
    }

    fun unregisterOwner(owner: String) {
        synchronized(lock) {
            registrations.values.forEach { entries ->
                entries.removeAll { it.descriptor.owner == owner }
            }
            registrations.entries.removeAll { it.value.isEmpty() }
        }
    }

    fun list(): List<AndexModServiceDescriptor> = synchronized(lock) {
        registrations.values.mapNotNull(::activeRegistration).map(Registration::descriptor)
    }

    fun describe(id: String): AndexModServiceDescriptor? = synchronized(lock) {
        activeRegistration(registrations[id.trim()].orEmpty())?.descriptor
    }

    suspend fun invoke(
        id: String,
        method: String,
        args: JSONObject,
    ): JSONObject {
        val registration = synchronized(lock) {
            activeRegistration(registrations[id.trim()].orEmpty())
        } ?: error("Unknown Andex mod service: $id")
        require(
            registration.descriptor.methods.any { it.name == method }
        ) {
            "Unknown method $method on Andex mod service ${registration.descriptor.id}."
        }
        return registration.handler.invoke(method, args)
    }

    fun listJson(): JSONObject = JSONObject().put(
        "services",
        JSONArray().apply {
            list().sortedBy(AndexModServiceDescriptor::id).forEach { descriptor ->
                put(descriptor.toJson())
            }
        },
    )

    fun describeJson(id: String): JSONObject =
        describe(id)?.toJson() ?: error("Unknown Andex mod service: $id")

    private fun activeRegistration(
        entries: List<Registration>,
    ): Registration? = entries.maxWithOrNull(
        compareBy<Registration> { it.descriptor.priority }.thenBy { it.sequence }
    )
}

data class AndexModOperationDecision(
    val payload: JSONObject,
    val cancelled: Boolean = false,
    val reason: String = "",
)

fun interface AndexModOperationInterceptor {
    suspend fun intercept(
        payload: JSONObject,
        context: JSONObject,
    ): AndexModOperationDecision
}

class AndexModOperationRegistry {
    private data class Registration(
        val sequence: Long,
        val operation: String,
        val owner: String,
        val priority: Int,
        val interceptor: AndexModOperationInterceptor,
    )

    private val lock = Any()
    private val sequence = AtomicLong()
    private val registrations = mutableListOf<Registration>()

    fun register(
        operation: String,
        owner: String,
        priority: Int = 0,
        interceptor: AndexModOperationInterceptor,
    ): () -> Unit {
        val normalizedOperation = operation.trim()
        require(normalizedOperation.isNotBlank()) {
            "Andex operation interceptors require an operation name."
        }
        val registration = Registration(
            sequence = sequence.incrementAndGet(),
            operation = normalizedOperation,
            owner = owner.trim().ifBlank { "unknown" },
            priority = priority,
            interceptor = interceptor,
        )
        synchronized(lock) {
            registrations += registration
        }
        return {
            synchronized(lock) {
                registrations.remove(registration)
            }
        }
    }

    fun hasInterceptors(operation: String): Boolean = synchronized(lock) {
        registrations.any { it.operation == operation || it.operation == "*" }
    }

    fun unregisterOwner(owner: String) {
        synchronized(lock) {
            registrations.removeAll { it.owner == owner }
        }
    }

    suspend fun intercept(
        operation: String,
        payload: JSONObject,
        context: JSONObject,
    ): AndexModOperationDecision {
        val interceptors = synchronized(lock) {
            registrations
                .filter { it.operation == operation || it.operation == "*" }
                .sortedWith(
                    compareBy<Registration> { it.priority }
                        .thenBy { it.sequence }
                )
        }
        var currentPayload = JSONObject(payload.toString())
        for (registration in interceptors) {
            val decision = registration.interceptor.intercept(
                JSONObject(currentPayload.toString()),
                context,
            )
            currentPayload = decision.payload
            if (decision.cancelled) {
                return decision.copy(payload = currentPayload)
            }
        }
        return AndexModOperationDecision(payload = currentPayload)
    }
}

class AndexModKernel {
    val services = AndexModServiceRegistry()
    val operations = AndexModOperationRegistry()
    val components = AndexNativeComponentRegistry()
    val toolTitles = com.android.andex.mod.AndexNativeToolTitleRegistry()
}

private fun AndexModServiceDescriptor.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("description", description)
    put("owner", owner)
    put("priority", priority)
    put(
        "methods",
        JSONArray().apply {
            methods.forEach { method ->
                put(
                    JSONObject().apply {
                        put("name", method.name)
                        put("description", method.description)
                        put("mutates_state", method.mutatesState)
                    }
                )
            }
        },
    )
}
