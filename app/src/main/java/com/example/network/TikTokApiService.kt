package com.example.network

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ApiLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String,
    val type: String, // "INFO", "REQUEST", "RESPONSE", "ERROR", "SUCCESS"
    val service: String, // "TikTok API v2" or "Gemini API"
    val message: String,
    val detail: String? = null
)

object TikTokApiService {
    private const val TAG = "TikTokApiService"
    private val _logs = MutableStateFlow<List<ApiLogEntry>>(emptyList())
    val logs: StateFlow<List<ApiLogEntry>> = _logs.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun redactSensitiveInfo(text: String?): String? {
        if (text == null) return null
        return text
            .replace(Regex("\"access_token\"\\s*:\\s*\"[^\"]+\""), "\"access_token\":\"[REDACTED]\"")
            .replace(Regex("\"refresh_token\"\\s*:\\s*\"[^\"]+\""), "\"refresh_token\":\"[REDACTED]\"")
            .replace(Regex("Bearer\\s+[a-zA-Z0-9._-]+"), "Bearer [REDACTED]")
            .replace(Regex("client_secret=[a-zA-Z0-9._-]+"), "client_secret=[REDACTED]")
            .replace(Regex("code=[a-zA-Z0-9._-]+"), "code=[REDACTED]")
    }

    fun log(type: String, service: String, message: String, detail: String? = null) {
        val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val time = formatter.format(Date())
        val cleanMessage = redactSensitiveInfo(message) ?: ""
        val cleanDetail = redactSensitiveInfo(detail)
        val entry = ApiLogEntry(timestamp = time, type = type, service = service, message = cleanMessage, detail = cleanDetail)
        _logs.value = listOf(entry) + _logs.value.take(49) // Keep last 50
        Log.d(TAG, "[$service] [$type] $cleanMessage")
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    /**
     * Builds the authorization URL for TikTok Login OAuth 2.0.
     */
    fun buildAuthorizeUrl(clientId: String, redirectUri: String): String {
        val scope = "user.info.basic,video.publish,video.upload"
        val state = java.util.UUID.randomUUID().toString().take(8)
        val url = "https://www.tiktok.com/v2/auth/authorize/" +
                "?client_key=$clientId" +
                "&scope=$scope" +
                "&response_type=code" +
                "&redirect_uri=$redirectUri" +
                "&state=$state"
        log("REQUEST", "TikTok Auth", "Generando URL de autorización OAuth2", url)
        return url
    }

    /**
     * Exchanges auth code for an access token using real TikTok API format.
     */
    suspend fun exchangeCodeForToken(
        clientId: String,
        clientSecret: String,
        code: String,
        redirectUri: String
    ): TikTokTokenResponse? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val url = "https://open.tiktokapis.com/v2/oauth/token/"
        log("REQUEST", "TikTok API v2", "Intercambiando código de autorización por Token. URL: $url")

        val payload = "client_key=$clientId&client_secret=$clientSecret&code=$code&grant_type=authorization_code&redirect_uri=$redirectUri"
        val requestBody = payload.toRequestBody("application/x-www-form-urlencoded".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseStr = response.body?.string() ?: ""
            log("RESPONSE", "TikTok API v2", "Token exchange status: ${response.code}", responseStr)

            if (response.isSuccessful) {
                val json = JSONObject(responseStr)
                val accessToken = json.optString("access_token", "")
                val refreshToken = json.optString("refresh_token", "")
                val expiresIn = json.optLong("expires_in", 86400)
                val openId = json.optString("open_id", "")
                return@withContext TikTokTokenResponse(accessToken, refreshToken, expiresIn, openId)
            }
        } catch (e: Exception) {
            log("ERROR", "TikTok API v2", "Fallo al conectar con TikTok API: ${e.message}")
        }
        null
    }

    /**
     * Initiates a TikTok Direct Post for a Video campaign.
     * Endpoint: POST https://open.tiktokapis.com/v2/post/publish/video/init/
     */
    suspend fun publishVideoDirect(
        accessToken: String,
        title: String,
        caption: String,
        hashtags: String,
        scenesCount: Int
    ): TikTokPublishResponse = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val url = "https://open.tiktokapis.com/v2/post/publish/video/init/"
        log("REQUEST", "TikTok API v2", "Inicializando publicación Direct Post de video: $title")

        // Format post according to TikTok Direct Post v2 body specification
        val postInfo = JSONObject().apply {
            put("title", caption)
            put("privacy_level", "PUBLIC_TO_EVERYONE")
            put("allow_comment", true)
            put("allow_duet", true)
            put("allow_stitch", true)
            put("brand_content_tags", JSONArray()) // Default empty
            put("video_cover_timestamp_ms", 1000)
        }

        val requestBodyJson = JSONObject().apply {
            put("post_info", postInfo)
            put("source_info", JSONObject().apply {
                put("source", "FILE_UPLOAD")
                put("video_size", 1048576 * scenesCount) // Simulated estimated bytes (1MB per scene)
            })
        }

        val maskedToken = if (accessToken.length > 8) "${accessToken.take(4)}…${accessToken.takeLast(4)}" else "***"
        log("INFO", "TikTok API v2", "Enviando Meta-data de video con token Bearer $maskedToken", requestBodyJson.toString(2))

        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseStr = response.body?.string() ?: ""
            log("RESPONSE", "TikTok API v2", "Video Init Status: ${response.code}", responseStr)

            if (response.isSuccessful) {
                val json = JSONObject(responseStr)
                val dataObj = json.optJSONObject("data")
                if (dataObj != null) {
                    val publishId = dataObj.optString("publish_id", "")
                    val uploadUrl = dataObj.optString("upload_url", "")
                    log("SUCCESS", "TikTok API v2", "Sesión de carga inicializada. ID de publicación: $publishId")
                    return@withContext TikTokPublishResponse(true, publishId, uploadUrl, "Pronto para subir video binario a TikTok.")
                }
            }
            TikTokPublishResponse(false, "", "", "TikTok API Error: Código ${response.code}")
        } catch (e: Exception) {
            val errMsg = "Error de red al publicar a TikTok: ${e.message}"
            log("ERROR", "TikTok API v2", errMsg)
            TikTokPublishResponse(false, "", "", errMsg)
        }
    }

    /**
     * Simulates complete video upload and publishing flow for visual display and scheduling.
     */
    suspend fun simulatePublishingFlow(
        username: String,
        title: String,
        caption: String,
        hashtags: String,
        scenesCount: Int,
        onProgress: (String, Float) -> Unit
    ): TikTokPublishResponse {
        log("INFO", "Publisher", "Iniciando simulador de publicación para cuenta: @$username")
        onProgress("Codificando escenas del video con codecs optimizados para móvil...", 0.15f)
        kotlinx.coroutines.delay(1200)

        onProgress("Consolidando audio, efectos de voz TTS y subtítulos...", 0.35f)
        kotlinx.coroutines.delay(1200)

        onProgress("Iniciando conexión con los servidores de TikTok...", 0.55f)
        log("REQUEST", "TikTok POST API", "Iniciando subida segmentada de video de 5.4MB.")
        kotlinx.coroutines.delay(1000)

        onProgress("Iniciando carga de bloque de vídeo (Direct Post API)...", 0.75f)
        log("SUCCESS", "TikTok S3 Upload", "Fragmento cargado correctamente. Hash MD5 verificado.")
        kotlinx.coroutines.delay(1200)

        onProgress("Verificando copyright de pista musical y hashtags de tendencia...", 0.90f)
        kotlinx.coroutines.delay(800)

        onProgress("Procesado finalizado! Publicación programada.", 1.0f)
        val publishId = "v_pub_sim_" + (100000..999999).random()
        log("SUCCESS", "TikTok API v2", "Publicación realizada con éxito. ID de TikTok: $publishId")

        return TikTokPublishResponse(
            success = true,
            publishId = publishId,
            uploadUrl = "https://upload.tiktokapis.com/v2/direct/$publishId",
            message = "Video publicado con éxito en la cuenta @$username!"
        )
    }
}

data class TikTokTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val openId: String
)

data class TikTokPublishResponse(
    val success: Boolean,
    val publishId: String,
    val uploadUrl: String,
    val message: String
)
