package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.example.data.TikTokTrend
import com.example.data.VideoScene
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Analyzes and generates current trending video concepts using Gemini.
         * Returns a list of TikTokTrend items.
     */
    suspend fun generateTrends(category: String, searchSeed: String = ""): List<TikTokTrend> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is missing or default placeholder.")
            return@withContext getLocalBackupTrends(category)
        }

        val prompt = """
            Generate 4 highly creative, current TikTok-style trending concept ideas about the category '$category'${if(searchSeed.isNotEmpty()) " filtered by '$searchSeed'" else ""}.
            Each concept must represent what would go viral right now.
            
            Return the output strictly as a JSON array where each object has these exact fields:
            - hashtag: String (including '#' e.g., '#cleantok')
            - title: String (catchy trend title)
            - description: String (quick description of why this trend is viral right now and what users are doing)
            - score: Int (trend score from 65 to 99)
            - category: String (the category name '$category')
            - visualStyle: String (recommended lighting, angles, visual filters, or video layout)
            - musicRecommendation: String (recommended energetic Audio sound, specific artist/sound type, or tempo)
            
            Provide ONLY raw JSON. No markdown tags. No ```json prefix.
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.85)
            })
        }

        try {
            val url = "$BASE_URL?key=$apiKey"
            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e(TAG, "Request failed: ${response.code} / $responseBody")
                return@withContext getLocalBackupTrends(category)
            }

            val responseJson = JSONObject(responseBody)
            val textToParse = responseJson
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val listType = Types.newParameterizedType(List::class.java, TikTokTrend::class.java)
            val adapter = moshi.adapter<List<TikTokTrend>>(listType)
            val trends = adapter.fromJson(textToParse) ?: emptyList()
            trends
        } catch (e: Exception) {
            Log.e(TAG, "Error generating trends: ${e.message}", e)
            getLocalBackupTrends(category)
        }
    }

    /**
     * Generates a fully produced, scene-by-scene storyboard (script) based on a trending topic.
     */
    suspend fun generateScriptForTrend(trend: TikTokTrend, tone: String = "Captivating & Energetic"): List<VideoScene> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is missing. Returning default scenes.")
            return@withContext getBackupScript(trend)
        }

        val prompt = """
            Create a professional 15-to-30 second scene-by-scene TikTok video script storyboard based on this trend:
            Hashtag: ${trend.hashtag}
            Title: ${trend.title}
            Description: ${trend.description}
            Category: ${trend.category}
            Music recommended: ${trend.musicRecommendation}
            Tone of Voice: $tone

            Divide this video into 3 to 5 key dynamic scenes.
            Return the output strictly as a JSON array where each object represents a scene with these exact fields:
            - sceneNumber: Int (starting at 1)
            - visualPrompt: String (extremely descriptive visual text prompt explaining what occurs on screen, optimal for AI image generators or framing)
            - overlayText: String (short, high-contrast text overlay shown on the screen, like TikTok captions. Under 5 words.)
            - durationSeconds: Int (between 2 and 5 seconds per scene)
            - voiceoverText: String (what is spoken by the narrator/creator-TTS in this scene. Under 15 words, extremely catchy, strong hooks!)

            Provide ONLY raw JSON. No markdown tags. No formatting.
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.75)
            })
        }

        try {
            val url = "$BASE_URL?key=$apiKey"
            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e(TAG, "Request failed: ${response.code} / $responseBody")
                return@withContext getBackupScript(trend)
            }

            val responseJson = JSONObject(responseBody)
            val textToParse = responseJson
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val listType = Types.newParameterizedType(List::class.java, VideoScene::class.java)
            val adapter = moshi.adapter<List<VideoScene>>(listType)
            val scenes = adapter.fromJson(textToParse) ?: emptyList()
            scenes
        } catch (e: Exception) {
            Log.e(TAG, "Error generating script: ${e.message}", e)
            getBackupScript(trend)
        }
    }

    /**
     * Fallback standard high-quality trends if key is missing or request fails.
     */
    private fun getLocalBackupTrends(category: String): List<TikTokTrend> {
        return when (category.lowercase()) {
            "tech & gadgets" -> listOf(
                TikTokTrend(
                    hashtag = "#DeskSetup",
                    title = "Transformación Extrema de Escritorio con IA",
                    description = "Creadores mostrando cómo usan luces LED inteligentes y fondos dinámicos generados por IA para revitalizar sus habitaciones de productividad en 10 segundos.",
                    score = 96,
                    category = "Tech & Gadgets",
                    visualStyle = "Transiciones rápidas con zoom, iluminación de fondo neón, tomas de ángulo cenital macro.",
                    musicRecommendation = "Synthwave instrumental veloz con bajos profundos."
                ),
                TikTokTrend(
                    hashtag = "#GadgetHacks",
                    title = "El Accesorio Secreto de tu Celular",
                    description = "Demostrar usos extremadamente oscuros del cargador inalámbrico magnético o cables de expansión de los que nadie habla pero salvan vidas.",
                    score = 88,
                    category = "Tech & Gadgets",
                    visualStyle = "Primer plano ultra-enfocado de conectores, tomas en perspectiva subjetiva (POV).",
                    musicRecommendation = "Tech house minimalista, ritmo limpio."
                ),
                TikTokTrend(
                    hashtag = "#AITools",
                    title = "Automatizando mi Vida Laboral",
                    description = "Vídeo rápido revelando una app que toma notas de reuniones y escribe correos al instante mientras simulas estar tomando un café.",
                    score = 94,
                    category = "Tech & Gadgets",
                    visualStyle = "Pantalla dividida con grabaciones de pantalla de alta definición y cara de sorpresa del creador.",
                    musicRecommendation = "Lofi Hip-Hop moderno y relajado."
                )
            )
            "lifestyle & hacks" -> listOf(
                TikTokTrend(
                    hashtag = "#SundayReset",
                    title = "Reinicio de Domingo Brutalista",
                    description = "Limpieza minimalista y organización semanal enfocada en la estética nórdica con un toque industrial y cero desorden.",
                    score = 95,
                    category = "Lifestyle & Hacks",
                    visualStyle = "Tomas simétricas lentas, colores beige y pizarra neutros, luz natural suave.",
                    musicRecommendation = "Melodía acústica relajante con sonido de lluvia ambiental."
                ),
                TikTokTrend(
                    hashtag = "#MealPrepEasy",
                    title = "Cocina para Perezosos Productivos",
                    description = "Preparar desayunos completos que duran toda la semana usando recipientes herméticos y una sola bandeja de horno.",
                    score = 91,
                    category = "Lifestyle & Hacks",
                    visualStyle = "Tomas macro picando ingredientes a alta velocidad con cortes perfectamente sincronizados.",
                    musicRecommendation = "Pop acústico alegre de ritmo medio."
                )
            )
            else -> listOf(
                TikTokTrend(
                    hashtag = "#ViralHack",
                    title = "El truco diario que cambia todo",
                    description = "Resolver un pequeño dilema diario, como guardar cables o empacar maletas, aplicando un método físico súper sencillo e hipnotizante.",
                    score = 92,
                    category = "General",
                    visualStyle = "Toma frontal fija bien iluminada mostrando el cambio del 'Antes vs Después'.",
                    musicRecommendation = "Sonido de sintetizador ascendente bailable."
                ),
                TikTokTrend(
                    hashtag = "#AIEverywhere",
                    title = "Cómo la Inteligencia Artificial diseña mi rutina",
                    description = "Dejar que un modelo de lenguaje organice tus comidas, outfits y entrenamientos del día y documentar el loco resultado.",
                    score = 97,
                    category = "General",
                    visualStyle = "Cámara en mano vlog, reacciones genuinas, ritmo de corte energético.",
                    musicRecommendation = "Ritmo electrónico futurista dinámico."
                )
            )
        }
    }

    private fun getBackupScript(trend: TikTokTrend): List<VideoScene> {
        return listOf(
            VideoScene(
                sceneNumber = 1,
                visualPrompt = "Dynamic overlay with red glow showing a messy corner transitioning quickly to an ultra-modern setups under colorful smart lighting.",
                overlayText = "¡ESTO CAMBIA TODO!",
                durationSeconds = 4,
                voiceoverText = "¿Sigues con tu Setup aburrido? Mira cómo la IA lo transforma por completo en segundos."
            ),
            VideoScene(
                sceneNumber = 2,
                visualPrompt = "Close-up of smart accent lighting glowing automatically in response to computer screen colors.",
                overlayText = "Estética Inteligente",
                durationSeconds = 3,
                voiceoverText = "Solo necesitas este pequeño gadget con sensores de color automáticos."
            ),
            VideoScene(
                sceneNumber = 3,
                visualPrompt = "Over-the-shoulder view of a clean minimal desk glowing with cosmic blue and purple backlighting.",
                overlayText = "El Resultado Final",
                durationSeconds = 4,
                voiceoverText = "Ideal para programar, jugar o crear contenido épico. Sígueme para más gadgets."
            )
        )
    }
}
