package com.android.andex

import android.app.Application
import android.content.Context
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.android.andex.data.AgentExtensionsRepository
import com.android.andex.data.AlpineChromeController
import com.android.andex.data.AgentModeController
import com.android.andex.data.AgentSkillManager
import com.android.andex.data.AndexAppExtensionManager
import com.android.andex.data.AndexModKernel
import com.android.andex.data.AndexDiagnosticLogger
import com.android.andex.data.AndexToolExecutor
import com.android.andex.data.ChatRepository
import com.android.andex.data.PiExtensionManager
import com.android.andex.data.PiExtensionStateRepository
import com.android.andex.data.RootSetupController
import com.android.andex.data.RuntimeWorkspaceFileBridge
import com.android.andex.data.ChatStateStore
import com.android.andex.data.ScheduledTask
import com.android.andex.data.ScheduledTaskManager
import com.android.andex.data.ScheduledTaskRepository
import com.android.andex.data.ScheduledTaskScheduler
import com.android.andex.data.SessionExecutionManager
import com.android.andex.data.SettingsRepository
import com.android.andex.data.WorkspaceFileBridge
import com.android.andex.data.pi.PiCompletionClient
import com.android.andex.data.pi.PiAgentRunner
import com.android.andex.data.pi.PiKernelBridge
import com.android.andex.mod.AndexNativeModManager
import com.android.andex.runtime.AlpineRuntime
import com.android.andex.runtime.RuntimeRouter
import com.android.andex.runtime.TermuxRuntime
import com.android.andex.termux.TermuxBashTool
import com.android.andex.termux.TermuxRuntimeOperations
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AndexApplication : Application() {
    val runtime: AndexAppRuntime by lazy(LazyThreadSafetyMode.NONE) {
        AndexAppRuntime(this)
    }
    @Volatile
    private var isPostHogInitialized = false

    override fun onCreate() {
        super.onCreate()
        runtime.initialize()
    }

    override fun onTerminate() {
        runtime.nativeModManager.shutdown()
        super.onTerminate()
    }

    fun initializePostHog() {
        if (isPostHogInitialized || BuildConfig.POSTHOG_API_KEY.isBlank()) return
        synchronized(this) {
            if (isPostHogInitialized || BuildConfig.POSTHOG_API_KEY.isBlank()) return
            val config = PostHogAndroidConfig(
                apiKey = BuildConfig.POSTHOG_API_KEY,
                host = BuildConfig.POSTHOG_HOST,
            ).apply {
                captureApplicationLifecycleEvents = true
                captureDeepLinks = true
                captureScreenViews = true
                debug = BuildConfig.DEBUG
                releaseIdentifier = "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
                errorTrackingConfig.autoCapture = true
                errorTrackingConfig.inAppIncludes.addAll(
                    listOf(
                        "com.android.andex",
                        "com.android.andex",
                    ),
                )
            }
            PostHogAndroid.setup(this, config)
            isPostHogInitialized = true
        }
    }
}

class AndexAppRuntime(
    private val application: AndexApplication,
) {
    val diagnosticLogger = AndexDiagnosticLogger(application)
    private val appScope = CoroutineScope(
        SupervisorJob() +
            Dispatchers.Default +
            CoroutineExceptionHandler { _, throwable ->
                diagnosticLogger.exception(
                    category = "coroutine",
                    event = "uncaught_exception",
                    throwable = throwable,
                )
            }
    )

    val settingsRepository = SettingsRepository(application)
    val piExtensionStateRepository = PiExtensionStateRepository(application)
    val modKernel = AndexModKernel()
    val chatRepository = ChatRepository(application)
    val extensionsRepository = AgentExtensionsRepository(application)
    val scheduledTaskRepository = ScheduledTaskRepository(application)
    val bashTool = TermuxBashTool(
        context = application,
        diagnosticLogger = diagnosticLogger,
    )
    val termuxRuntime = TermuxRuntime(bashTool)
    val phoneControlController = com.android.andex.phonecontrol.PhoneControlController(application, bashTool)
    private val _embeddedRuntimesReady = MutableStateFlow(false)
    val embeddedRuntimesReady: StateFlow<Boolean> = _embeddedRuntimesReady.asStateFlow()
    val alpineRuntime = AlpineRuntime(
        context = application,
        diagnosticLogger = diagnosticLogger,
    )
    val piKernelBridge = PiKernelBridge(
        alpineRuntime = alpineRuntime,
        diagnosticLogger = diagnosticLogger,
    )
    val nativeModManager = AndexNativeModManager(
        context = application,
        application = application,
        alpineRuntime = alpineRuntime,
        piKernelBridge = piKernelBridge,
        kernel = modKernel,
        piExtensionStateRepository = piExtensionStateRepository,
        diagnosticLogger = diagnosticLogger,
    )
    val piCompletionClient = PiCompletionClient(
        bridge = piKernelBridge,
        settingsRepository = settingsRepository,
    )
    val runtimeRouter = RuntimeRouter(
        termuxRuntime = termuxRuntime,
        alpineRuntime = alpineRuntime,
    )
    val rootSetupController = RootSetupController(
        context = application,
        bashTool = bashTool,
        diagnosticLogger = diagnosticLogger,
    )
    val workspaceFileBridge = WorkspaceFileBridge(
        context = application,
        bashTool = bashTool,
    )
    val runtimeWorkspaceFileBridge = RuntimeWorkspaceFileBridge(
        context = application,
        runtimeRouter = runtimeRouter,
        alpineRuntime = alpineRuntime,
        termuxFileBridge = workspaceFileBridge,
    )
    val agentModeController = AgentModeController(
        context = application,
        bashTool = bashTool,
        runtimeWorkspaceFileBridge = runtimeWorkspaceFileBridge,
        diagnosticLogger = diagnosticLogger,
    )
    val alpineChromeController = AlpineChromeController(
        context = application,
        alpineRuntime = alpineRuntime,
        diagnosticLogger = diagnosticLogger,
    )
    val skillManager = AgentSkillManager(
        context = application,
        extensionsRepository = extensionsRepository,
    )
    val piExtensionManager = PiExtensionManager(
        context = application,
        alpineRuntime = alpineRuntime,
        piKernelBridge = piKernelBridge,
        skillManager = skillManager,
        stateRepository = piExtensionStateRepository,
    )
    val andexAppExtensionManager = AndexAppExtensionManager(
        bridge = piKernelBridge,
        scope = appScope,
        diagnosticLogger = diagnosticLogger,
        modKernel = modKernel,
        loadOptionsProvider = piExtensionStateRepository::loadOptions,
    )
    val piAgentRunner = PiAgentRunner(
        bridge = piKernelBridge,
        settingsRepository = settingsRepository,
        piExtensionStateRepository = piExtensionStateRepository,
        appExtensionManager = andexAppExtensionManager,
        alpineChromeController = alpineChromeController,
        termuxRuntimeOperations = TermuxRuntimeOperations(bashTool),
        diagnosticLogger = diagnosticLogger,
        toolExecutor = AndexToolExecutor(
            runtimeRouter = runtimeRouter,
            agentModeController = agentModeController,
            phoneControlController = phoneControlController,
            webResearchTool = com.android.andex.data.WebResearchTool(application),
            archiveTool = com.android.andex.data.ArchiveTool(application),
        ),
    )
    val appForegroundTracker = AppForegroundTracker()
    val notificationController = AndexNotificationController(application)
    val scheduledTaskScheduler = ScheduledTaskScheduler(
        context = application,
        diagnosticLogger = diagnosticLogger,
    )
    val scheduledTaskManager = ScheduledTaskManager(
        repository = scheduledTaskRepository,
        scheduler = scheduledTaskScheduler,
    )
    val chatStateStore = ChatStateStore(
        scope = appScope,
        chatRepository = chatRepository,
    )
    val sessionExecutionManager = SessionExecutionManager(
        application = application,
        scope = appScope,
        settingsRepository = settingsRepository,
        extensionsRepository = extensionsRepository,
        chatStateStore = chatStateStore,
        chatRepository = chatRepository,
        bashTool = bashTool,
        runtimeRouter = runtimeRouter,
        workspaceFileBridge = workspaceFileBridge,
        rootSetupController = rootSetupController,
        agentModeController = agentModeController,
        skillManager = skillManager,
        piExtensionManager = piExtensionManager,
        scheduledTaskManager = scheduledTaskManager,
        notificationController = notificationController,
        appForegroundTracker = appForegroundTracker,
        diagnosticLogger = diagnosticLogger,
        piCompletionClient = piCompletionClient,
        piKernelBridge = piKernelBridge,
        piAgentRunner = piAgentRunner,
    )

    val taskStatusPublisher = com.android.andex.overlay.TaskStatusPublisher(
        sessionExecutionManager, chatStateStore, appForegroundTracker,
        appScope,
        andexAppExtensionManager::waitingUiSessionIds,
    )
    val pillOverlayController = com.android.andex.overlay.PillOverlayController(application)

    fun initialize() {
        if (BuildConfig.SHOWCASE_MODE) {
            appScope.launch {
                val current = settingsRepository.settings.first()
                settingsRepository.replaceImportedSettings(
                    settings = current.copy(
                        onboardingSeenVersion = com.android.andex.data.CurrentOnboardingVersion,
                        onboardingCompletedVersion = com.android.andex.data.CurrentOnboardingVersion,
                        defaultChatModelKey = "showcase-openai::gpt-6-astra",
                        modelId = "gpt-6-astra", reasoningEffort = "low",
                        keepTasksRunningInBackground = false, notifyOnTaskCompletion = false,
                    ),
                    providerConfigs = com.android.andex.data.ShowcaseCatalog.providers(),
                )
            }
            return
        }
        diagnosticLogger.installUncaughtExceptionHandler()
        diagnosticLogger.event(
            category = "app",
            event = "startup",
            details = mapOf(
                "version_name" to BuildConfig.VERSION_NAME,
                "version_code" to BuildConfig.VERSION_CODE,
                "debug" to BuildConfig.DEBUG,
            ),
        )
        notificationController.ensureChannels()
        appScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { com.android.andex.runtime.BundledNativeTools(application).initialize() }
                .onFailure { diagnosticLogger.event(
                    category = "native_tools", event = "setup_failed", level = "warn",
                    details = mapOf("message" to it.message.orEmpty()),
                ) }
        }
        appScope.launch {
            if (phoneControlController.preferences.embeddedTermux) {
                val setup = bashTool.embeddedRuntime.initialize()
                if (setup.isReady) {
                    if (phoneControlController.preferences.embeddedTermux) settingsRepository.markEmbeddedTermuxReady()
                } else diagnosticLogger.event(
                    category = "termux", event = "embedded_setup_failed", level = "warn",
                    details = mapOf("message" to setup.detail),
                )
                if (setup.isReady && phoneControlController.preferences.embeddedTermux) {
                    // Pi's kernel stays in Alpine; the selected command/filesystem runtime is Termux.
                    val kernelSetup = alpineRuntime.initialize()
                    _embeddedRuntimesReady.value = kernelSetup.isReady
                    if (!kernelSetup.isReady) diagnosticLogger.event(
                        category = "termux", event = "embedded_kernel_setup_failed", level = "warn",
                        details = mapOf("message" to kernelSetup.detail),
                    )
                }
            }
        }
        application.registerActivityLifecycleCallbacks(appForegroundTracker)
        pillOverlayController.start(appScope)
        appScope.launch {
            nativeModManager.initialize()
        }
        appScope.launch {
            alpineRuntime.refreshApkRepositoriesForCurrentNetwork()
        }
        appScope.launch {
            settingsRepository.migrateLegacyProvidersToPi()
        }
        appScope.launch {
            piExtensionStateRepository.enableBundledExtensionsOnce()
            settingsRepository.repairBuiltInSkillSelectionDefaults()
            skillManager.syncBuiltInSkills().onFailure { throwable ->
                diagnosticLogger.exception(
                    category = "skills",
                    event = "builtin_sync_failed",
                    throwable = throwable,
                )
            }
        }
        appScope.launch {
            if (settingsRepository.settings.first().privacyPolicyAccepted) {
                initializePostHog()
            }
        }
        appScope.launch {
            scheduledTaskManager.rescheduleAll()
        }
    }

    fun initializePostHog() {
        application.initializePostHog()
    }

    fun handleScheduledTaskAlarm(
        taskId: String,
        pendingResult: android.content.BroadcastReceiver.PendingResult,
    ) {
        diagnosticLogger.event(
            category = "scheduled_task",
            event = "alarm_received",
            details = mapOf("task_id" to taskId),
        )
        appScope.launch {
            try {
                val task = scheduledTaskManager.markTriggeredAndScheduleNext(taskId)
                if (task == null) {
                    diagnosticLogger.event(
                        category = "scheduled_task",
                        event = "trigger_missing_task",
                        level = "warn",
                        details = mapOf("task_id" to taskId),
                    )
                    return@launch
                }
                val started = startScheduledTaskFromAlarm(task)
                diagnosticLogger.event(
                    category = "scheduled_task",
                    event = if (started) "trigger_started" else "trigger_skipped",
                    level = if (started) "info" else "warn",
                    details = mapOf("task_id" to taskId),
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun startScheduledTaskFromAlarm(task: ScheduledTask): Boolean {
        return try {
            if (settingsRepository.settings.first().keepTasksRunningInBackground) {
                runCatching {
                    AndexForegroundService.ensureRunning(application)
                }.onFailure { throwable ->
                    diagnosticLogger.exception(
                        category = "scheduled_task",
                        event = "foreground_service_start_failed",
                        throwable = throwable,
                        details = mapOf("task_id" to task.id),
                    )
                }
            }
            sessionExecutionManager.startScheduledTask(task)
        } catch (throwable: Throwable) {
            diagnosticLogger.exception(
                category = "scheduled_task",
                event = "trigger_start_failed",
                throwable = throwable,
                details = mapOf("task_id" to task.id),
            )
            false
        }
    }

    fun rescheduleScheduledTasks(
        pendingResult: android.content.BroadcastReceiver.PendingResult,
    ) {
        appScope.launch {
            try {
                scheduledTaskManager.rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class AppForegroundTracker : Application.ActivityLifecycleCallbacks {
    private val _isForeground = MutableStateFlow(false)
    private var startedActivities = 0

    val isForeground: StateFlow<Boolean> = _isForeground.asStateFlow()

    override fun onActivityStarted(activity: android.app.Activity) {
        startedActivities++
        _isForeground.value = true
    }

    override fun onActivityStopped(activity: android.app.Activity) {
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
        _isForeground.value = startedActivities > 0
    }
    override fun onActivityCreated(activity: android.app.Activity, state: android.os.Bundle?) = Unit
    override fun onActivityResumed(activity: android.app.Activity) = Unit
    override fun onActivityPaused(activity: android.app.Activity) = Unit
    override fun onActivitySaveInstanceState(activity: android.app.Activity, state: android.os.Bundle) = Unit
    override fun onActivityDestroyed(activity: android.app.Activity) = Unit
}

val Context.andexRuntime: AndexAppRuntime
    get() = (applicationContext as AndexApplication).runtime
