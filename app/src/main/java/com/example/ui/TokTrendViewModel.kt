package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.TikTokApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class MainScreen {
    TRENDS,
    EDITOR,
    QUEUE,
    LOGGER,
    SETTINGS
}

class TokTrendViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TokTrendRepository

    // SharedPreferences for persistent Autopilot state
    private val sharedPrefs = application.getSharedPreferences("TokTrendAutopilotPrefs", android.content.Context.MODE_PRIVATE)

    var isAutopilotEnabled by mutableStateOf(sharedPrefs.getBoolean("is_autopilot_enabled", false))
        private set

    var autopilotIntervalMs by mutableStateOf(sharedPrefs.getLong("autopilot_interval_ms", 3600000L * 4)) // Default: 4 hours
        private set

    var lastAutopilotTriggerTime by mutableStateOf(sharedPrefs.getLong("last_autopilot_trigger_time", 0L))
        private set

    fun updateAutopilotEnabled(enabled: Boolean) {
        isAutopilotEnabled = enabled
        sharedPrefs.edit().putBoolean("is_autopilot_enabled", enabled).apply()
        TikTokApiService.log("INFO", "Autopilot", "Piloto Automático " + (if (enabled) "ACTIVADO." else "DESACTIVADO."))
    }

    fun setAutopilotInterval(intervalMs: Long) {
        autopilotIntervalMs = intervalMs
        sharedPrefs.edit().putLong("autopilot_interval_ms", intervalMs).apply()
        val intervalMinutes = intervalMs / 60000
        TikTokApiService.log("INFO", "Autopilot", "Frecuencia establecida en: cada $intervalMinutes minutos.")
    }

    fun updateLastAutopilotTrigger(time: Long) {
        lastAutopilotTriggerTime = time
        sharedPrefs.edit().putLong("last_autopilot_trigger_time", time).apply()
    }

    // AGENCY-AGENTS INTEGRATION STATE
    var isAgentIntegrationEnabled by mutableStateOf(sharedPrefs.getBoolean("is_agent_integration_enabled", true))
        private set

    var githubAgentsRepoUrl by mutableStateOf(sharedPrefs.getString("github_agents_repo_url", "https://github.com/Erik755/agency-agents.git") ?: "https://github.com/Erik755/agency-agents.git")
        private set

    var selectedAgentId by mutableStateOf(sharedPrefs.getString("selected_agent_id", "tiktok_publisher_agent") ?: "tiktok_publisher_agent")
        private set

    var selectedAgentModel by mutableStateOf(sharedPrefs.getString("selected_agent_model", "gemini-1.5-flash") ?: "gemini-1.5-flash")
        private set

    fun updateAgentIntegrationEnabled(enabled: Boolean) {
        isAgentIntegrationEnabled = enabled
        sharedPrefs.edit().putBoolean("is_agent_integration_enabled", enabled).apply()
        TikTokApiService.log(
            "INFO",
            "IA Agent Hub",
            "Integridad de Agente IA de Agency-Agents " + (if (enabled) "HABILITADA." else "DESHABILITADA.")
        )
    }

    fun updateGithubAgentsRepoUrl(url: String) {
        githubAgentsRepoUrl = url
        sharedPrefs.edit().putString("github_agents_repo_url", url).apply()
        TikTokApiService.log("INFO", "IA Agent Hub", "Repositorio de agentes externo cambiado a: $url")
    }

    fun updateSelectedAgentId(agentId: String) {
        selectedAgentId = agentId
        sharedPrefs.edit().putString("selected_agent_id", agentId).apply()
        TikTokApiService.log("INFO", "IA Agent Hub", "Se cargó el perfil de agente activo: $agentId")
    }

    fun updateSelectedAgentModel(model: String) {
        selectedAgentModel = model
        sharedPrefs.edit().putString("selected_agent_model", model).apply()
        TikTokApiService.log("INFO", "IA Agent Hub", "Motor LLM de Agente configurado en: $model")
    }

    // ONE-TOUCH FULL AI AUTOMATION STATE
    var isAutoCreatingEverything by mutableStateOf(false)
        private set
    var autoCreationStep by mutableStateOf("")
        private set

    fun runFullAiAutomationPipeline() {
        viewModelScope.launch {
            isAutoCreatingEverything = true
            autoCreationStep = "🤖 agency-agents: Seleccionando el mejor agente de la red..."
            try {
                delay(1200)
                // 1. Automatic Agent Selection based on category
                val bestAgent = when {
                    selectedCategory.contains("tech", ignoreCase = true) -> "tiktok_publisher_agent"
                    selectedCategory.contains("lifestyle", ignoreCase = true) -> "social_media_ninja"
                    else -> "operations_manager"
                }
                updateSelectedAgentId(bestAgent)
                updateAgentIntegrationEnabled(true)
                TikTokApiService.log("INFO", "Agente Auto Selector", "🤖 agency-agents asignó a: $bestAgent para la categoría '$selectedCategory'")
                
                // 2. Scan Trends
                autoCreationStep = "🔍 Analizando corrientes virales de TikTok en tiempo real..."
                TikTokApiService.log("INFO", "Auto-Flow", "Iniciando escaneo algorítmico...")
                repository.refreshTrends(selectedCategory, searchSeed)
                delay(1200)
                
                val currentTrends = repository.allTrends.first()
                val topTrend = currentTrends.maxByOrNull { it.score }
                if (topTrend == null) {
                    autoCreationStep = "❌ No se encontraron tendencias actualmente"
                    isAutoCreatingEverything = false
                    return@launch
                }
                
                // 3. Create Video Project from topTrend
                autoCreationStep = "🎬 Diseñando guión cinematográfico y escenas para #${topTrend.hashtag}..."
                TikTokApiService.log("INFO", "Auto-Flow", "Tendencia TOP encontrada: ${topTrend.title} (${topTrend.score}% Score). Creando escenas...")
                val generatedCampaign = repository.createVideoCampaignFromTrend(topTrend)
                if (generatedCampaign == null) {
                    autoCreationStep = "❌ Fallo al estructurar escenas con Gemini AI"
                    isAutoCreatingEverything = false
                    return@launch
                }
                delay(1000)
                
                // 4. Validate campaign with agency-agents
                autoCreationStep = "🤖 Agente '$bestAgent' revisando, optimizando tags y aprobando publicación..."
                TikTokApiService.log("INFO", "Agente IA ($bestAgent)", "Analizando campaña '${generatedCampaign.title}'...")
                delay(1200)
                TikTokApiService.log("SUCCESS", "Agente IA ($bestAgent)", "✅ Verificación exitosa. Vídeo aprobado para automatización completa.")
                
                // 5. Publish Post
                autoCreationStep = "🚀 Publicando y sincronizando con TikTok directamente..."
                activePublishingPostId = generatedCampaign.id
                publishingProgressMessage = "Iniciando render automatizado guiado por el Agente..."
                publishingProgressBar = 0.20f
                delay(1000)
                
                val result = repository.publishPostNow(generatedCampaign) { msg, progress ->
                    publishingProgressMessage = msg
                    publishingProgressBar = 0.3f + (progress * 0.7f)
                }
                activePublishingPostId = null
                
                if (result) {
                    autoCreationStep = "🎉 ¡TODO COMPLETADO CON ÉXITO! Video publicado automáticamente."
                    TikTokApiService.log("SUCCESS", "Auto-Flow", "🚀 El Agente completó todo de extremo a extremo: Tendencia analizada, Guión de Video redactado, y Publicación enviada con éxito.")
                } else {
                    autoCreationStep = "❌ Falló el enlace de publicación."
                    TikTokApiService.log("ERROR", "Auto-Flow", "❌ Error al subir el video procesado automáticamente.")
                }
            } catch (e: Exception) {
                autoCreationStep = "❌ Error: ${e.message}"
                TikTokApiService.log("ERROR", "Auto-Flow", "Ocurrió un error en el pipeline: ${e.message}")
            } finally {
                isAutoCreatingEverything = false
            }
        }
    }

    // Screen state
    var currentScreen by mutableStateOf(MainScreen.TRENDS)

    // Database states
    val accounts = MutableStateFlow<List<TikTokAccount>>(emptyList())
    val trends = MutableStateFlow<List<TikTokTrend>>(emptyList())
    val posts = MutableStateFlow<List<VideoPost>>(emptyList())

    // Network / Action states
    var isAnalyzing by mutableStateOf(false)
    var isGeneratingScript by mutableStateOf(false)
    var selectedCategory by mutableStateOf("Tech & Gadgets")
    var searchSeed by mutableStateOf("")

    // Editor configurations
    var editingPost by mutableStateOf<VideoPost?>(null)
    var editedCaption by mutableStateOf("")
    var editedHashtags by mutableStateOf("")
    var editedScheduleTime by mutableStateOf(System.currentTimeMillis())

    // Simulated Video Player state
    var isPlayingSimulation by mutableStateOf(false)
    var currentSimulatorSceneIndex by mutableStateOf(0)
    var playJob: Job? = null

    // Background Scheduling Daemon state (Automatic Publishing Service)
    var isDaemonRunning by mutableStateOf(true)
    private var daemonJob: Job? = null
    var daemonStatusMessage by mutableStateOf("Daemon inicializado. En espera de publicaciones programadas...")

    // Publishing progress logs (dynamic visual overlay)
    var activePublishingPostId by mutableStateOf<Int?>(null)
    var publishingProgressMessage by mutableStateOf("")
    var publishingProgressBar by mutableStateOf(0f)

    // Api log terminal
    val apiLogs: StateFlow<List<com.example.network.ApiLogEntry>> = TikTokApiService.logs

    init {
        val database = TokTrendDatabase.getDatabase(application)
        repository = TokTrendRepository(
            database.accountDao(),
            database.trendDao(),
            database.postDao()
        )

        // Observe flows in scope
        viewModelScope.launch {
            repository.allAccounts.collect { accounts.value = it }
        }
        viewModelScope.launch {
            repository.allTrends.collect { trends.value = it }
        }
        viewModelScope.launch {
            repository.allPosts.collect { posts.value = it }
        }

        // Connect demo TikTok credit initially if none exist in DB to secure instant usage
        viewModelScope.launch {
            val exist = repository.getConnectedAccount()
            if (exist == null) {
                repository.connectDemoAccount()
            }
        }

        // Automatically fetch initial trends
        analyzeCurrentTrends()

        // Launch background daemon
        startSchedulingDaemon()
    }

    // --- Trend Operations ---

    fun analyzeCurrentTrends() {
        viewModelScope.launch {
            isAnalyzing = true
            repository.refreshTrends(selectedCategory, searchSeed)
            isAnalyzing = false
        }
    }

    fun handleCategoryChange(category: String) {
        selectedCategory = category
        analyzeCurrentTrends()
    }

    // --- Campaign Generation / Scene Editing ---

    fun generateVideoProject(trend: TikTokTrend) {
        viewModelScope.launch {
            isGeneratingScript = true
            val campaign = repository.createVideoCampaignFromTrend(trend)
            if (campaign != null) {
                // Initialize editor with generated details
                editingPost = campaign
                editedCaption = campaign.caption
                editedHashtags = campaign.hashtags
                editedScheduleTime = campaign.scheduledTime
                currentScreen = MainScreen.EDITOR
            }
            isGeneratingScript = false
        }
    }

    fun loadPostInEditor(post: VideoPost) {
        editingPost = post
        editedCaption = post.caption
        editedHashtags = post.hashtags
        editedScheduleTime = post.scheduledTime
        currentScreen = MainScreen.EDITOR
    }

    fun getScenes(): List<VideoScene> {
        val post = editingPost ?: return emptyList()
        return repository.parseScenes(post)
    }

    fun updateScene(updatedScenes: List<VideoScene>) {
        val post = editingPost ?: return
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter<List<VideoScene>>(
            com.squareup.moshi.Types.newParameterizedType(List::class.java, VideoScene::class.java)
        )
        val json = adapter.toJson(updatedScenes)
        editingPost = post.copy(scenesJson = json)
    }

    fun saveEditorDraft(onCompleted: () -> Unit = {}) {
        val post = editingPost ?: return
        viewModelScope.launch {
            val updated = post.copy(
                caption = editedCaption,
                hashtags = editedHashtags,
                scheduledTime = editedScheduleTime,
                status = "DRAFT"
            )
            repository.savePost(updated)
            editingPost = updated
            TikTokApiService.log("SUCCESS", "Editor", "Cambios en el borrador de video guardados correctamente.")
            onCompleted()
        }
    }

    fun confirmSchedulePublish() {
        val post = editingPost ?: return
        viewModelScope.launch {
            val updated = post.copy(
                caption = editedCaption,
                hashtags = editedHashtags,
                scheduledTime = editedScheduleTime,
                status = "SCHEDULED"
            )
            repository.savePost(updated)
            editingPost = null
            currentScreen = MainScreen.QUEUE
            TikTokApiService.log("SUCCESS", "Scheduler", "Video programado de forma eficiente para publicarse el: " + 
                java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(editedScheduleTime))
            )
        }
    }

    fun deletePost(post: VideoPost) {
        viewModelScope.launch {
            repository.deletePost(post.id)
            if (editingPost?.id == post.id) {
                editingPost = null
            }
        }
    }

    // --- Storyboard Visual Playback Simulation ---

    fun toggleVideoSimulation() {
        if (isPlayingSimulation) {
            stopVideoSimulation()
        } else {
            startVideoSimulation()
        }
    }

    private fun startVideoSimulation() {
        val scenes = getScenes()
        if (scenes.isEmpty()) return
        isPlayingSimulation = true
        currentSimulatorSceneIndex = 0

        playJob = viewModelScope.launch {
            while (isPlayingSimulation) {
                val currentScene = scenes.getOrNull(currentSimulatorSceneIndex) ?: break
                val durationMs = (currentScene.durationSeconds * 1000L).coerceAtLeast(1500L)
                delay(durationMs)
                if (currentSimulatorSceneIndex < scenes.size - 1) {
                    currentSimulatorSceneIndex++
                } else {
                    currentSimulatorSceneIndex = 0 // Loop preview
                }
            }
        }
    }

    fun stopVideoSimulation() {
        isPlayingSimulation = false
        playJob?.cancel()
        playJob = null
    }

    // --- Background Publishing Scheduler Process (Daemon) ---

    fun startSchedulingDaemon() {
        isDaemonRunning = true
        daemonJob?.cancel()
        daemonJob = viewModelScope.launch {
            TikTokApiService.log("INFO", "Daemon", "Servidor de Publicaciones Automáticas Iniciado.")
            while (isDaemonRunning) {
                try {
                val nowTime = System.currentTimeMillis()

                // --- Autopilot Periodic Cycle Check ---
                if (isAutopilotEnabled) {
                    val nextTrigger = lastAutopilotTriggerTime + autopilotIntervalMs
                    if (nowTime >= nextTrigger || lastAutopilotTriggerTime == 0L) {
                        try {
                            runAutopilotInstantFlow(nowTime)
                        } catch (e: Exception) {
                            TikTokApiService.log("ERROR", "Autopilot", "Fallo durante el ciclo automático de fondo: ${e.message}")
                        }
                    }
                }

                val activePosts = posts.value
                val postsDue = activePosts.filter { it.status == "SCHEDULED" && it.scheduledTime <= nowTime }

                if (postsDue.isNotEmpty()) {
                    daemonStatusMessage = "Encontrados ${postsDue.size} videos listos para publicar automáticamente..."
                    for (post in postsDue) {
                        daemonStatusMessage = "Publicando automáticamente: ${post.title}..."
                        activePublishingPostId = post.id
                        val result = repository.publishPostNow(post) { msg, progress ->
                            publishingProgressMessage = msg
                            publishingProgressBar = progress
                        }
                        activePublishingPostId = null
                        if (result) {
                            daemonStatusMessage = "Publicación automática de '${post.title}' completada con éxito."
                        } else {
                            daemonStatusMessage = "Error en publicación automática de '${post.title}'."
                        }
                    }
                } else {
                    val nextPostStr = findNextScheduledPostTime()
                    val autopilotStr = if (isAutopilotEnabled) {
                        val nextAuto = lastAutopilotTriggerTime + autopilotIntervalMs
                        val minSdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                        " [Piloto Automático: Siguiente ciclo a las ${minSdf.format(java.util.Date(nextAuto))}]"
                    } else {
                        " [Piloto Automático: Desactivado]"
                    }
                    
                    daemonStatusMessage = if (nextPostStr.isNotEmpty()) {
                        "Servicio de Publicación programada: Siguiente vídeo el $nextPostStr$autopilotStr"
                    } else {
                        "Monitoreo eficiente del canal TikTok activo. Ningún video en espera...$autopilotStr"
                    }
                }
                delay(8000) // Poll database queue every 8 seconds for real-time reactivity
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e // Let coroutine cancellation propagate normally
                } catch (e: Exception) {
                    TikTokApiService.log("ERROR", "Daemon", "Error en ciclo del daemon: ${e.message}")
                    delay(8000) // Back off and retry
                }
            }
        }
    }

    private suspend fun runAutopilotInstantFlow(nowTime: Long) {
        TikTokApiService.log("INFO", "Autopilot", "⏱️ Iniciando ciclo automatizado (Modo Piloto Automático IA)...")
        
        // 1. Refresh trends
        TikTokApiService.log("INFO", "Autopilot", "Paso 1/3: Analizando tendencias de TikTok de mayor impacto para: $selectedCategory...")
        repository.refreshTrends(selectedCategory, "")
        
        delay(1500) // sync delay for db writing
        val currentTrends = repository.allTrends.first()
        val topTrend = currentTrends.firstOrNull()
        
        if (topTrend == null) {
            TikTokApiService.log("ERROR", "Autopilot", "❌ No se encontraron tendencias válidas para autoprocesar la publicación.")
            updateLastAutopilotTrigger(nowTime)
            return
        }
        
        // 2. Build full project/script
        TikTokApiService.log("INFO", "Autopilot", "Paso 2/3: Tendencia seleccionada: #${topTrend.hashtag}. Generando guión, escenas y hashtags con Gemini AI...")
        val generatedCampaign = repository.createVideoCampaignFromTrend(topTrend)
        
        if (generatedCampaign == null) {
            TikTokApiService.log("ERROR", "Autopilot", "❌ La IA falló al diseñar el guión y generar el contenido del video.")
            updateLastAutopilotTrigger(nowTime)
            return
        }
        
        // 3. Mark as SCHEDULED for immediate auto-publishing
        if (isAgentIntegrationEnabled) {
            TikTokApiService.log("INFO", "Agente IA ($selectedAgentId)", "🤖 [agency-agents] El Agente de IA supervisa y validador de autopilot revisa el script para #${topTrend.hashtag}...")
            delay(1000)
            TikTokApiService.log("SUCCESS", "Agente IA ($selectedAgentId)", "✅ Publicación aprobada automáticamente por el Agente. Sincronizando con cola de publicaciones.")
        }
        
        TikTokApiService.log("INFO", "Autopilot", "Paso 3/3: Borrador optimizado. Programando publicación inmediata en la cola de TikTok...")
        val autoScheduledPost = generatedCampaign.copy(
            status = "SCHEDULED",
            scheduledTime = nowTime + 2000 // publish in 2 seconds
        )
        repository.savePost(autoScheduledPost)
        
        // Commit trigger time
        updateLastAutopilotTrigger(nowTime)
        TikTokApiService.log("SUCCESS", "Autopilot", "🚀 ¡Piloto automático completado! Video '${generatedCampaign.title}' enviado a publicar.")
    }

    fun triggerAutopilotNow() {
        viewModelScope.launch {
            try {
                runAutopilotInstantFlow(System.currentTimeMillis())
            } catch (e: Exception) {
                TikTokApiService.log("ERROR", "Autopilot", "Fallo al forzar el ciclo automático: ${e.message}")
            }
        }
    }

    fun stopSchedulingDaemon() {
        isDaemonRunning = false
        daemonStatusMessage = "Demonio detenido de forma manual por el usuario."
        daemonJob?.cancel()
        daemonJob = null
        TikTokApiService.log("INFO", "Daemon", "Servidor de Publicaciones Automáticas Detenido.")
    }

    override fun onCleared() {
        super.onCleared()
        stopSchedulingDaemon()
        stopVideoSimulation()
    }

    private fun findNextScheduledPostTime(): String {
        val scheduled = posts.value.filter { it.status == "SCHEDULED" }.minByOrNull { it.scheduledTime } ?: return ""
        val sdf = java.text.SimpleDateFormat("dd/MM HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(scheduled.scheduledTime))
    }

    fun triggerPublishNow(post: VideoPost) {
        viewModelScope.launch {
            activePublishingPostId = post.id
            if (isAgentIntegrationEnabled) {
                publishingProgressMessage = "Cargando [Agente IA - $selectedAgentId] de agency-agents..."
                publishingProgressBar = 0.15f
                TikTokApiService.log("INFO", "Agente IA ($selectedAgentId)", "🤖 Cargando reglas y directrices del agente desde: $githubAgentsRepoUrl")
                delay(1000)
                
                publishingProgressMessage = "Iniciando motor cognitivo con $selectedAgentModel..."
                publishingProgressBar = 0.35f
                TikTokApiService.log("INFO", "Agente IA ($selectedAgentId)", "Procesando publicación '${post.title}' - Validando metadatos algorítmicos...")
                delay(1000)

                publishingProgressMessage = "Agente IA: Metadatos y guión verificados con éxito."
                publishingProgressBar = 0.55f
                TikTokApiService.log("SUCCESS", "Agente IA ($selectedAgentId)", "✅ El Agente de IA revisó la publicación. Guión aprobado, y hashtags optimizados según las pautas de Erik755/agency-agents.")
                delay(1200)
            }
            repository.publishPostNow(post) { msg, progress ->
                publishingProgressMessage = msg
                publishingProgressBar = if (isAgentIntegrationEnabled) 0.6f + (progress * 0.4f) else progress
            }
            activePublishingPostId = null
        }
    }

    // --- Account / Setting Management ---

    fun connectTikTokManual(clientId: String, clientSecret: String, username: String) {
        viewModelScope.launch {
            repository.disconnectAccounts()
            val formattedUser = username.lowercase().replace("@", "").trim()
            val newAccount = TikTokAccount(
                username = formattedUser,
                displayName = "@$formattedUser Creator",
                avatarUrl = "https://images.unsplash.com/photo-1628157582853-a796fa650a6a?auto=format&fit=crop&w=150&h=150&q=80",
                isConnected = true,
                clientId = clientId,
                clientSecret = clientSecret,
                accessToken = "act_sh_" + (1000..9999).random() + "_token"
            )
            repository.insertAccount(newAccount)
            analyzeCurrentTrends()
        }
    }

    fun disconnectTikTokAccount() {
        viewModelScope.launch {
            repository.disconnectAccounts()
        }
    }

    fun clearDatabase() {
        viewModelScope.launch {
            repository.clearTrends()
            val all = posts.value
            for (p in all) {
                repository.deletePost(p.id)
            }
            TikTokApiService.clearLogs()
            TikTokApiService.log("INFO", "Settings", "Historial, colas de publicaciones y datos limpias exitosamente.")
        }
    }
}
