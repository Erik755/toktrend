package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.TikTokAccount
import com.example.data.TikTokTrend
import com.example.data.VideoPost
import com.example.data.VideoScene
import com.example.network.TikTokApiService
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.text.TextStyle

@Composable
fun ImmersiveBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TikTokDarkBg)
            .drawBehind {
                val designRadius = size.width * 0.75f
                if (designRadius > 0.1f) {
                    // Top-right soft cyan glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(TikTokCyan.copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(size.width * 0.95f, size.height * 0.05f),
                            radius = designRadius
                        ),
                        radius = designRadius,
                        center = Offset(size.width * 0.95f, size.height * 0.05f)
                    )
                    // Bottom-left soft pink/rose glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(TikTokRed.copy(alpha = 0.10f), Color.Transparent),
                            center = Offset(size.width * 0.05f, size.height * 0.85f),
                            radius = designRadius
                        ),
                        radius = designRadius,
                        center = Offset(size.width * 0.05f, size.height * 0.85f)
                    )
                }
            }
    ) {
        content()
    }
}

@Composable
fun TokTrendApp(viewModel: TokTrendViewModel) {
    val context = LocalContext.current
    val accountsState by viewModel.accounts.collectAsState()
    val connectedAccount = accountsState.find { it.isConnected }
    
    ImmersiveBackground {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            bottomBar = {
                TokTrendBottomBar(
                    currentScreen = viewModel.currentScreen,
                    onScreenSelected = { viewModel.currentScreen = it }
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (viewModel.currentScreen) {
                    MainScreen.TRENDS -> TrendsScreen(viewModel, connectedAccount)
                    MainScreen.EDITOR -> EditorScreen(viewModel)
                    MainScreen.QUEUE -> QueueScreen(viewModel)
                    MainScreen.LOGGER -> LoggerScreen(viewModel)
                    MainScreen.SETTINGS -> SettingsScreen(viewModel, connectedAccount)
                }

                // Quick Floating Progress Overlay when a video is publishing
                viewModel.activePublishingPostId?.let { postId ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f))
                            .clickable(enabled = false) {},
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .padding(16.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp)),
                            colors = CardDefaults.cardColors(containerColor = TikTokSurface.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = TikTokRed,
                                    modifier = Modifier.size(64.dp),
                                    strokeWidth = 5.dp
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "PUBLICANDO EN TIKTOK",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TikTokRed,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = viewModel.publishingProgressMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                LinearProgressIndicator(
                                    progress = { viewModel.publishingProgressBar },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = TikTokCyan,
                                    trackColor = Color.White.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TokTrendBottomBar(
    currentScreen: MainScreen,
    onScreenSelected: (MainScreen) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF0F1014),
        contentColor = Color.White,
        windowInsets = WindowInsets.navigationBars,
        modifier = Modifier.drawBehind {
            // High fidelity thin top border
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 1.dp.toPx()
            )
        }
    ) {
        val items = listOf(
            Triple(MainScreen.TRENDS, "Tendencias", Icons.Default.Refresh),
            Triple(MainScreen.QUEUE, "Cola Post", Icons.Default.Add),
            Triple(MainScreen.LOGGER, "Terminal API", Icons.Default.Info),
            Triple(MainScreen.SETTINGS, "Ajustes", Icons.Default.Settings)
        )

        items.forEach { (screen, label, icon) ->
            val isSelected = currentScreen == screen || (screen == MainScreen.QUEUE && currentScreen == MainScreen.EDITOR)
            NavigationBarItem(
                selected = isSelected,
                onClick = { onScreenSelected(screen) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { 
                    Text(
                        text = label, 
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 10.sp
                    ) 
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TikTokCyan,
                    selectedTextColor = TikTokCyan,
                    unselectedIconColor = TikTokMuted,
                    unselectedTextColor = TikTokMuted,
                    indicatorColor = TikTokCyan.copy(alpha = 0.12f)
                ),
                modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
            )
        }
    }
}

// --- SCREEN 1: TRENDS & ANALYZER ---
@Composable
fun TrendsScreen(viewModel: TokTrendViewModel, connectedAccount: TikTokAccount?) {
    val trendsList by viewModel.trends.collectAsState()
    val categories = listOf("Tech & Gadgets", "Lifestyle & Hacks", "Music", "Comedy", "Business", "Gaming", "General")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // App Header Brand
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "TRENDFLOW AI",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TikTokMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "TokTrend IA",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = (-0.5).sp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(TikTokCyan, TikTokRed)
                        )
                    )
                )
            }

            // Small profile badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(TikTokSurface.copy(alpha = 0.5f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .clickable { viewModel.currentScreen = MainScreen.SETTINGS }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (connectedAccount != null) {
                    AsyncImage(
                        model = connectedAccount.avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "@${connectedAccount.username}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 100.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.Green)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "No Conectado",
                        tint = TikTokRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Conectar",
                        style = MaterialTheme.typography.bodySmall,
                        color = TikTokRed
                    )
                }
            }
        }

        // --- 🤖 ONE-TOUCH AUTOMATION HERO BANNER ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(
                    width = 1.5.dp,
                    color = if (viewModel.isAutoCreatingEverything) TikTokCyan else TikTokRed,
                    shape = RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = TikTokSurface.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TikTokRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Auto",
                                tint = TikTokRed,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Autoproducción Directa IA",
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "El Agente IA se encarga de todo con un toque",
                                color = TikTokMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (viewModel.isAutoCreatingEverything) {
                    // Running animation state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    color = TikTokCyan,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = viewModel.autoCreationStep,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { viewModel.runFullAiAutomationPipeline() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("one_touch_autopilot_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = TikTokRed),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AUTOPILOTO: CREAR Y PUBLICAR TODO YA",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🤖 Autoselección de agente, análisis, elaboración de guiones con Gemini y subida directa.",
                        color = TikTokMuted,
                        fontSize = 10.sp,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Category Hub (Pills)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val isSelected = viewModel.selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (isSelected) {
                                Brush.horizontalGradient(listOf(TikTokCyan, TikTokRed))
                            } else {
                                Brush.linearGradient(listOf(TikTokSurface.copy(alpha = 0.40f), TikTokSurface.copy(alpha = 0.15f)))
                            }
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.06f),
                            RoundedCornerShape(24.dp)
                        )
                        .clickable { viewModel.handleCategoryChange(category) }
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = category,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // Seed Search Bar
        val keyboardController = LocalSoftwareKeyboardController.current
        OutlinedTextField(
            value = viewModel.searchSeed,
            onValueChange = { viewModel.searchSeed = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            placeholder = { Text("Filtrar o forzar nicho (ej. 'pantallas', 'organizar')", color = TikTokMuted) },
            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = "Filtro", tint = TikTokMuted) },
            trailingIcon = {
                if (viewModel.searchSeed.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchSeed = ""; viewModel.analyzeCurrentTrends() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.White)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                viewModel.analyzeCurrentTrends()
                keyboardController?.hide()
            }),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TikTokCyan,
                unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                focusedContainerColor = TikTokSurface.copy(alpha = 0.5f),
                unfocusedContainerColor = TikTokSurface.copy(alpha = 0.25f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (viewModel.isAnalyzing) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = TikTokRed)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Analizando corrientes algorítmicas de TikTok...",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Utilizando Gemini 1.5 Flash en tiempo real",
                    style = MaterialTheme.typography.bodySmall,
                    color = TikTokMuted
                )
            }
        } else {
            if (trendsList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Vacío",
                        tint = TikTokMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sin tendencias generadas",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.analyzeCurrentTrends() },
                        colors = ButtonDefaults.buttonColors(containerColor = TikTokRed)
                    ) {
                        Text("Escanear Tendencias Ahora")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(trendsList) { trend ->
                        TrendCard(trend, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun TrendCard(trend: TikTokTrend, viewModel: TokTrendViewModel) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (expanded) TikTokCyan.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = TikTokSurface.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(TikTokRed.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = trend.hashtag,
                                color = TikTokRed,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Score Viral: ${trend.score}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = TikTokCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = trend.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = if (expanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Ver más",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Score Visual Progress Bar with modern translucent slate track
            LinearProgressIndicator(
                progress = { trend.score / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (trend.score > 90) TikTokRed else TikTokCyan,
                trackColor = Color.White.copy(alpha = 0.08f)
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = trend.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Metadata details
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "RITMO RECOMENDADO", style = MaterialTheme.typography.labelSmall, color = TikTokMuted, fontWeight = FontWeight.Bold)
                        Text(text = trend.musicRecommendation, style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "DIRECCIÓN DE ARTE", style = MaterialTheme.typography.labelSmall, color = TikTokMuted, fontWeight = FontWeight.Bold)
                        Text(text = trend.visualStyle, style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.generateVideoProject(trend) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("gen_video_btn_${trend.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = TikTokRed),
                    shape = RoundedCornerShape(24.dp),
                    enabled = !viewModel.isGeneratingScript
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (viewModel.isGeneratingScript) "PLANIFICANDO SCRIPT DE VIDEO CON IA..." else "CREAR VIDEO CON IA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}


// --- SCREEN 2: BOLD VIDEO CREATOR & STORYBOARD EDITOR ---
@Composable
fun EditorScreen(viewModel: TokTrendViewModel) {
    val post = viewModel.editingPost ?: return
    val scenes = viewModel.getScenes()
    var selectedSceneIndex by remember { mutableStateOf(0) }
    
    // Dynamic brush for video mockup screen
    val brushGradients = listOf(
        listOf(Color(0xFFE94057), Color(0xFF8A2387)),
        listOf(Color(0xFF2C3E50), Color(0xFFFD746C)),
        listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)),
        listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)),
        listOf(Color(0xFF11998e), Color(0xFF38ef7d))
    )
    val colorIndex = (post.id + (selectedSceneIndex)) % brushGradients.size
    val currentBrush = Brush.linearGradient(brushGradients[colorIndex])

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.currentScreen = MainScreen.QUEUE }) {
                Icon(Icons.Default.Clear, contentDescription = "Atrás", tint = Color.White)
            }
            Text(
                text = "Storyboard de Video IA",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            IconButton(onClick = { viewModel.saveEditorDraft() }) {
                Icon(Icons.Default.Check, contentDescription = "Guardar", tint = TikTokCyan)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Left Side Equivalent: Cinematic Interactive Video Player mockup
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(32.dp))
                .background(currentBrush)
                .border(2.dp, if (post.id % 2 == 0) TikTokCyan else TikTokRed, RoundedCornerShape(32.dp))
                .testTag("video_mockup_player")
        ) {
            val renderScene = scenes.getOrNull(if (viewModel.isPlayingSimulation) viewModel.currentSimulatorSceneIndex else selectedSceneIndex)
            
            if (renderScene != null) {
                // Background visual synthesis (simulating moving lines or waves)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pathWidth = size.width
                    val pathHeight = size.height
                    
                    // Ripple waves
                    val phase = if (viewModel.isPlayingSimulation) (System.currentTimeMillis() % 1000) / 1000f else 0.5f
                    for (i in 0..3) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.05f),
                            radius = (pathWidth * 0.4f) + (i * 30.dp.toPx() * phase),
                            center = Offset(pathWidth / 2, pathHeight / 2),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // TikTok layout overlays
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top stats details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (viewModel.isPlayingSimulation) Color.Red else Color.LightGray)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (viewModel.isPlayingSimulation) "LIVE PLAYBACK" else "ESCENA ${renderScene.sceneNumber}/${scenes.size}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.drawBehind {
                                    // Subtle light overlay
                                }
                            )
                        }
                        Text(
                            text = "${renderScene.durationSeconds}s",
                            color = TikTokCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    // Centered description storyboard visual cue
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "VISUAL PROMPT (IA):",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TikTokCyan,
                                fontSize = 8.sp
                            )
                            Text(
                                text = renderScene.visualPrompt,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Bottom caption & tags overlays
                    Column {
                        // High-contrast video subtitles matching real TikTok subtitles
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = renderScene.overlayText.uppercase(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black,
                                        offset = Offset(2f, 2f),
                                        blurRadius = 4f
                                    )
                                ),
                                color = Color.Yellow,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Voiceover TTS box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TikTokRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "NARRADOR / VOZ EN IA:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TikTokMuted,
                                        fontSize = 8.sp
                                    )
                                    Text(
                                        text = "\"${renderScene.voiceoverText}\"",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Playback and controls
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { viewModel.toggleVideoSimulation() },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (viewModel.isPlayingSimulation) Color.DarkGray else TikTokRed
            ),
            modifier = Modifier.fillMaxWidth(0.5f).height(44.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (viewModel.isPlayingSimulation) Icons.Default.Clear else Icons.Default.PlayArrow,
                    contentDescription = "Simulación",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (viewModel.isPlayingSimulation) "PAUSAR PREPLAY" else "COMPONER VIDEO",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Horizontal Scene Slider Selector (Manual preview selection)
        Text(
            text = "Seleccionar Escena para editar:",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            scenes.forEachIndexed { idx, s ->
                val isSelected = selectedSceneIndex == idx && !viewModel.isPlayingSimulation
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) TikTokCyan.copy(alpha = 0.2f) else TikTokSurface)
                        .border(
                            2.dp,
                            if (isSelected) TikTokCyan else Color.DarkGray,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            viewModel.stopVideoSimulation()
                            selectedSceneIndex = idx
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("S-${s.sceneNumber}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("${s.durationSeconds}s", color = TikTokMuted, fontSize = 10.sp)
                    }
                }
            }
        }

        // Active scene edits
        scenes.getOrNull(selectedSceneIndex)?.let { activeScene ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = TikTokSurface.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ajustes de Escena ${activeScene.sceneNumber}",
                        fontWeight = FontWeight.Bold,
                        color = TikTokCyan,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Texto en Pantalla (TikTok Overlay)", style = MaterialTheme.typography.labelSmall, color = TikTokMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = activeScene.overlayText,
                        onValueChange = { newVal ->
                            val list = scenes.toMutableList()
                            list[selectedSceneIndex] = activeScene.copy(overlayText = newVal)
                            viewModel.updateScene(list)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TikTokCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                            focusedContainerColor = TikTokSurface.copy(alpha = 0.5f),
                            unfocusedContainerColor = TikTokSurface.copy(alpha = 0.25f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "Narrador Hablado (TTS)", style = MaterialTheme.typography.labelSmall, color = TikTokMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = activeScene.voiceoverText,
                        onValueChange = { newVal ->
                            val list = scenes.toMutableList()
                            list[selectedSceneIndex] = activeScene.copy(voiceoverText = newVal)
                            viewModel.updateScene(list)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TikTokCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                            focusedContainerColor = TikTokSurface.copy(alpha = 0.5f),
                            unfocusedContainerColor = TikTokSurface.copy(alpha = 0.25f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "Prompt Visual (Para renderizar/planificar)", style = MaterialTheme.typography.labelSmall, color = TikTokMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = activeScene.visualPrompt,
                        onValueChange = { newVal ->
                            val list = scenes.toMutableList()
                            list[selectedSceneIndex] = activeScene.copy(visualPrompt = newVal)
                            viewModel.updateScene(list)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TikTokCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                            focusedContainerColor = TikTokSurface.copy(alpha = 0.5f),
                            unfocusedContainerColor = TikTokSurface.copy(alpha = 0.25f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Campaign caption and details configuration
        Text(
            text = "Copy & Configuración del Post:",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.editedCaption,
            onValueChange = { viewModel.editedCaption = it },
            label = { Text("Descripción de TikTok", color = Color.LightGray) },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TikTokCyan,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.editedHashtags,
            onValueChange = { viewModel.editedHashtags = it },
            label = { Text("Hashtags", color = Color.LightGray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TikTokCyan,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Visual Date & Programmed Scheduling Pickers
        Text(
            text = "Programación de la Publicación:",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val systemOffsets = listOf(
                Pair("Inmediato", 0L),
                Pair("+15 min", 15 * 60 * 1000L),
                Pair("+1 hora", 60 * 60 * 1000L),
                Pair("+12 horas", 12 * 60 * 60 * 1000L),
                Pair("+1 día", 24 * 60 * 60 * 1000L)
            )

            systemOffsets.forEach { (label, offset) ->
                val selected = if (offset == 0L) {
                    viewModel.editedScheduleTime <= System.currentTimeMillis() + 60000L
                } else {
                    val approxTarget = System.currentTimeMillis() + offset
                    Math.abs(viewModel.editedScheduleTime - approxTarget) < 2 * 60000L
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) TikTokCyan else TikTokSurface)
                        .border(1.dp, if (selected) TikTokCyan else Color.DarkGray, RoundedCornerShape(8.dp))
                        .clickable { viewModel.editedScheduleTime = System.currentTimeMillis() + offset }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Display converted date string
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TikTokSurface)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = "Fecha", tint = TikTokCyan)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "HORA ESTABLECIDA DE LANZAMIENTO:", style = MaterialTheme.typography.labelSmall, color = TikTokMuted)
                    val sdf = SimpleDateFormat("EEEE d 'de' MMMM, HH:mm", Locale("es", "ES"))
                    Text(
                        text = sdf.format(Date(viewModel.editedScheduleTime)),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Button(
            onClick = { viewModel.confirmSchedulePublish() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("publish_schedule_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = TikTokRed),
            shape = RoundedCornerShape(26.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("CONFIRMAR Y PROGRAMAR", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { viewModel.saveEditorDraft { viewModel.currentScreen = MainScreen.QUEUE } },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("GUARDAR BORRADOR OFFLINE", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}


// --- SCREEN 3: SCHEDULE QUEUE & DEAMON CONTROLLERS ---
@Composable
fun QueueScreen(viewModel: TokTrendViewModel) {
    val postsList by viewModel.posts.collectAsState()
    
    val scheduled = postsList.filter { it.status == "SCHEDULED" }
    val drafts = postsList.filter { it.status == "DRAFT" }
    val published = postsList.filter { it.status == "PUBLISHED" || it.status == "FAILED" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Queue Header Title with beautiful tracking and gradient display
        Text(
            text = "AUTO-POST SYSTEM",
            style = MaterialTheme.typography.labelMedium.copy(
                color = TikTokMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Planificación Automática",
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineSmall.copy(
                brush = Brush.horizontalGradient(colors = listOf(TikTokCyan, TikTokRed))
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Background Daemon Monitoring Dashboard Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .border(
                    1.dp,
                    if (viewModel.isDaemonRunning) TikTokCyan.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.06f),
                    RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = TikTokSurface.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pulsing online green dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (viewModel.isDaemonRunning) Color.Green else Color.Red)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SERVICIO AUTO-POST (LOGS)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    // On/Off Switch Button for Background worker
                    TextButton(
                        onClick = {
                            if (viewModel.isDaemonRunning) {
                                viewModel.stopSchedulingDaemon()
                            } else {
                                viewModel.startSchedulingDaemon()
                            }
                        }
                    ) {
                        Text(
                            text = if (viewModel.isDaemonRunning) "DETENER" else "INICIAR",
                            color = if (viewModel.isDaemonRunning) Color.Red else TikTokCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                // Display Current Active Daemon polling logs
                Text(
                    text = viewModel.daemonStatusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Green,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scrollable Columns of Queues
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Queue SECTION 1: Scheduled Posts
            if (scheduled.isNotEmpty()) {
                item {
                    Text(
                        text = "PROGRAMADOS EN COLA (${scheduled.size})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = TikTokCyan
                    )
                }
                items(scheduled) { post ->
                    QueuePostCard(post, viewModel)
                }
            }

            // Queue SECTION 2: Drafts
            if (drafts.isNotEmpty()) {
                item {
                    Text(
                        text = "BORRADORES DE VIDEO DE IA (${drafts.size})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.Yellow
                    )
                }
                items(drafts) { post ->
                    QueuePostCard(post, viewModel)
                }
            }

            // Queue SECTION 3: Published Log Histroy
            if (published.isNotEmpty()) {
                item {
                    Text(
                        text = "HISTORIAL Y RESULTADOS (${published.size})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = TikTokMuted
                    )
                }
                items(published) { post ->
                    QueuePostCard(post, viewModel)
                }
            }

            // If empty
            if (postsList.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp), tint = TikTokMuted)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No tienes videos planificados.",
                            color = TikTokMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QueuePostCard(post: VideoPost, viewModel: TokTrendViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = TikTokSurface.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Post Title
                Text(
                    text = post.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                // Status Badge
                val (color, text) = when (post.status) {
                    "SCHEDULED" -> Pair(TikTokCyan, "PROGRAMADO")
                    "DRAFT" -> Pair(Color.Yellow, "BORRADOR")
                    "PUBLISHING" -> Pair(TikTokRed, "CARGANDO")
                    "PUBLISHED" -> Pair(Color.Green, "PUBLICADO")
                    "FAILED" -> Pair(Color.Red, "FALLIDO")
                    else -> Pair(Color.Gray, post.status)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = text,
                        color = color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${post.caption} ${post.hashtags}",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scheduled/Created date
                val format = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                val timeLabel = if (post.status == "SCHEDULED") "Publica: " + format.format(Date(post.scheduledTime)) 
                                else "Último: " + format.format(Date(post.createdAt))
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = TikTokMuted,
                    fontSize = 11.sp
                )

                // Campaign actions row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { viewModel.deletePost(post) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                    
                    if (post.status == "DRAFT" || post.status == "FAILED") {
                        IconButton(onClick = { viewModel.loadPostInEditor(post) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    if (post.status == "SCHEDULED" || post.status == "DRAFT" || post.status == "FAILED") {
                        Button(
                            onClick = { viewModel.triggerPublishNow(post) },
                            colors = ButtonDefaults.buttonColors(containerColor = TikTokRed),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp),
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            Text("PUBLICAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Error display
            post.errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Error: $err",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp
                )
            }
        }
    }
}


// --- SCREEN 4: DIRECT API TERMINAL LOGGER VIEW (TRANSPARENCY ON CALLS) ---
@Composable
fun LoggerScreen(viewModel: TokTrendViewModel) {
    val logs by viewModel.apiLogs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Title Logger Header with beautiful tracking and gradient display
        Text(
            text = "INSPECTOR DE LLAMADAS SYSTEM",
            style = MaterialTheme.typography.labelMedium.copy(
                color = TikTokMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Consola de Peticiones",
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineSmall.copy(
                    brush = Brush.horizontalGradient(colors = listOf(TikTokCyan, TikTokRed))
                )
            )
            IconButton(onClick = { TikTokApiService.clearLogs() }) {
                Icon(Icons.Default.Delete, contentDescription = "Limpiar Logs", tint = Color.Red.copy(alpha = 0.8f))
            }
        }

        if (logs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp), tint = TikTokMuted)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Ningún log guardado todavía.", color = TikTokMuted, textAlign = TextAlign.Center)
                Text(
                    text = "Genera scripts o publica videos para ver llamadas HTTP.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TikTokMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(TikTokSurface.copy(alpha = 0.40f), RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { logEntry ->
                    LogCard(logEntry)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun LogCard(logEntry: com.example.network.ApiLogEntry) {
    var expanded by remember { mutableStateOf(false) }

    val contentColor = when (logEntry.type) {
        "REQUEST" -> TikTokCyan
        "RESPONSE" -> Color.Green
        "SUCCESS" -> Color.Green
        "ERROR" -> Color.Red
        else -> Color.White
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 4.dp, horizontal = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "[${logEntry.timestamp}]",
                    color = TikTokMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = logEntry.service,
                    color = if (logEntry.service.contains("Gemini")) Color.Yellow else TikTokRed,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(${logEntry.type})",
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }

        Text(
            text = logEntry.message,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp)
        )

        if (expanded && logEntry.detail != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .background(Color.Black)
                    .border(0.5.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = logEntry.detail,
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        }
    }
    Divider(color = Color.DarkGray.copy(alpha = 0.3f), thickness = 0.5.dp)
}


// --- SCREEN 5: SETTINGS & ACCOUNT MANAGEMENT ---
@Composable
fun SettingsScreen(viewModel: TokTrendViewModel, connectedAccount: TikTokAccount?) {
    var customClientId by remember { mutableStateOf("") }
    var customClientSecret by remember { mutableStateOf("") }
    var customUsername by remember { mutableStateOf("@sanchezerik836") }
    
    val controller = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Title Header Settings with beautiful tracking and gradient display
        Text(
            text = "AUTO-POST CONFIG",
            style = MaterialTheme.typography.labelMedium.copy(
                color = TikTokMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Herramientas de Cuenta",
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineSmall.copy(
                brush = Brush.horizontalGradient(colors = listOf(TikTokCyan, TikTokRed))
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Show Current Active Status connected with premium 24.dp card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = TikTokSurface.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (connectedAccount != null) {
                    AsyncImage(
                        model = connectedAccount.avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = connectedAccount.displayName,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "@${connectedAccount.username} • Conectado",
                            color = Color.Green,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Button(
                        onClick = { viewModel.disconnectTikTokAccount() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("SALIR", fontSize = 10.sp, color = Color.White)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Mundo", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sin Cuenta Vinculada",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Conecta tu credencial para publicar",
                            color = TikTokMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION: AI AUTOPILOT MODE ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, if (viewModel.isAutopilotEnabled) TikTokCyan.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = TikTokSurface.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (viewModel.isAutopilotEnabled) TikTokCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Autopilot",
                                tint = if (viewModel.isAutopilotEnabled) TikTokCyan else Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Piloto Automático Inteligente",
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Proceso 100% IA (Análisis + Guión + Post)",
                                color = TikTokMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Switch(
                        checked = viewModel.isAutopilotEnabled,
                        onCheckedChange = { viewModel.updateAutopilotEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TikTokDarkBg,
                            checkedTrackColor = TikTokCyan,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("autopilot_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "PERIODICIDAD DE PUBLICACIÓN AUTOMÁTICA:",
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Selector grid of buttons (Horizontal Scrollable Row)
                val intervals = listOf(
                    Triple("15 min", 900000L, "Demos"),
                    Triple("1 hora", 3600000L, "Frecuente"),
                    Triple("4 horas", 4 * 3600000L, "Recomendado"),
                    Triple("12 horas", 12 * 3600000L, "Orgánico"),
                    Triple("24 horas", 24 * 3600000L, "Diario")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    intervals.forEach { (label, ms, type) ->
                        val isSelected = viewModel.autopilotIntervalMs == ms
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) TikTokCyan.copy(alpha = 0.15f)
                                    else Color.White.copy(alpha = 0.04f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) TikTokCyan else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.setAutopilotInterval(ms) }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = label,
                                    color = if (isSelected) TikTokCyan else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = type,
                                    color = if (isSelected) TikTokCyan.copy(alpha = 0.7f) else TikTokMuted,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val sdf = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Estado:", color = TikTokMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.4f))
                            Text(
                                text = if (viewModel.isAutopilotEnabled) "Activo 🟢" else "Inactivo 🔴",
                                color = if (viewModel.isAutopilotEnabled) TikTokCyan else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(0.6f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Último Ciclo:", color = TikTokMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.4f))
                            Text(
                                text = if (viewModel.lastAutopilotTriggerTime == 0L) "Nunca ejecutado" else sdf.format(Date(viewModel.lastAutopilotTriggerTime)),
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(0.6f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Siguiente Ciclo:", color = TikTokMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.4f))
                            val nextTime = if (viewModel.lastAutopilotTriggerTime == 0L) System.currentTimeMillis() else viewModel.lastAutopilotTriggerTime + viewModel.autopilotIntervalMs
                            Text(
                                text = if (viewModel.isAutopilotEnabled) sdf.format(Date(nextTime)) else "Esperando activación",
                                color = if (viewModel.isAutopilotEnabled) Color.Green else Color.Gray,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(0.6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action simulation button
                OutlinedButton(
                    onClick = { viewModel.triggerAutopilotNow() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TikTokCyan),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, TikTokCyan.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Ejecutar Autopilot", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PROBAR CICLO AUTOMÁTICO COMPLETO AHORA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION: AGENCY-AGENTS IA INTEGRATION ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, if (viewModel.isAgentIntegrationEnabled) TikTokRed.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = TikTokSurface.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (viewModel.isAgentIntegrationEnabled) TikTokRed.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Agentes de IA",
                                tint = if (viewModel.isAgentIntegrationEnabled) TikTokRed else Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Agentes de IA (agency-agents)",
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Integración de Agente desde Github",
                                color = TikTokMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Switch(
                        checked = viewModel.isAgentIntegrationEnabled,
                        onCheckedChange = { viewModel.updateAgentIntegrationEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TikTokDarkBg,
                            checkedTrackColor = TikTokRed,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }

                if (viewModel.isAgentIntegrationEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "REPOSITORIO DE AGENTES DE GITHUB (agency-agents):",
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    var tempRepoUrl by remember { mutableStateOf(viewModel.githubAgentsRepoUrl) }
                    OutlinedTextField(
                        value = tempRepoUrl,
                        onValueChange = { 
                            tempRepoUrl = it
                            viewModel.updateGithubAgentsRepoUrl(it)
                        },
                        placeholder = { Text("https://github.com/Erik755/agency-agents.git") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TikTokRed,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "PERFIL DE AGENTE ACTIVO (DE SU ROSTER):",
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val activeAgents = listOf(
                        Pair("tiktok_publisher_agent", "🎬 TikTok Publisher (Specialized)"),
                        Pair("social_media_ninja", "📢 Social Ninja (Marketing)"),
                        Pair("operations_manager", "💼 Operations Manager (Management)"),
                        Pair("frontend_wizard", "💻 Frontend Wizard (Engineering)")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeAgents.forEach { (id, label) ->
                            val isSelected = viewModel.selectedAgentId == id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) TikTokRed.copy(alpha = 0.15f)
                                        else Color.White.copy(alpha = 0.04f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) TikTokRed else Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.updateSelectedAgentId(id) }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) TikTokRed else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "MODELO DE LLM PARA EL AGENTE:",
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val models = listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        models.forEach { model ->
                            val isSelected = viewModel.selectedAgentModel == model
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) Color.White.copy(alpha = 0.12f)
                                        else Color.White.copy(alpha = 0.03f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.updateSelectedAgentModel(model) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = model,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "🎯 Función del Agente en TokTrend:",
                                fontWeight = FontWeight.Bold,
                                color = TikTokRed,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "El agente de Erik755/agency-agents ejecutará rutinas de validación algorítmica y optimización de descripciones en cada publicación manual o ciclo de piloto automático.",
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Direct Integration manual setup fields (Configurar Tiktok Creds)
        Text(
            text = "Configuración Developer de TikTok:",
            fontWeight = FontWeight.Bold,
            color = TikTokCyan,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Permite a TokTrend realizar solicitudes OAuth2 válidas contra el API de socios comerciales de TikTok.",
            color = TikTokMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = customUsername,
            onValueChange = { customUsername = it },
            label = { Text("Usuario personal de TikTok (ej. @mi_usuario)", color = Color.LightGray) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("@sanchezerik836", color = TikTokMuted) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TikTokCyan,
                unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                focusedContainerColor = TikTokSurface.copy(alpha = 0.5f),
                unfocusedContainerColor = TikTokSurface.copy(alpha = 0.25f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = customClientId,
            onValueChange = { customClientId = it },
            label = { Text("TikTok Client ID", color = Color.LightGray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TikTokCyan,
                unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                focusedContainerColor = TikTokSurface.copy(alpha = 0.5f),
                unfocusedContainerColor = TikTokSurface.copy(alpha = 0.25f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = customClientSecret,
            onValueChange = { customClientSecret = it },
            label = { Text("TikTok Client Secret", color = Color.LightGray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TikTokCyan,
                unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                focusedContainerColor = TikTokSurface.copy(alpha = 0.5f),
                unfocusedContainerColor = TikTokSurface.copy(alpha = 0.25f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val enteredUsername = customUsername.trim()
                if (enteredUsername.isNotEmpty()) {
                    val finalClientId = if (customClientId.trim().isEmpty()) "demo_client_key_12345" else customClientId.trim()
                    val finalClientSecret = if (customClientSecret.trim().isEmpty()) "demo_client_secret_abcde" else customClientSecret.trim()
                    viewModel.connectTikTokManual(finalClientId, finalClientSecret, enteredUsername)
                    controller?.hide()
                    // clear fields
                    customUsername = ""
                    customClientId = ""
                    customClientSecret = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TikTokRed),
            shape = RoundedCornerShape(24.dp),
            enabled = customUsername.trim().isNotEmpty()
        ) {
            Text("VINCULAR CUENTA CON CREDENCIALES", fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Demo login triggers
        Text(
            text = "O inicia sesión en modo de pruebas instantáneo:",
            color = TikTokMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        val buttonUsername = if (customUsername.isNotEmpty()) {
            customUsername.trim().lowercase().removePrefix("@")
        } else {
            "sanchezerik836"
        }

        OutlinedButton(
            onClick = {
                viewModel.connectTikTokManual("demo_client", "demo_secret", buttonUsername)
                controller?.hide()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TikTokCyan),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, TikTokCyan.copy(alpha = 0.6f))
        ) {
            Text("CONECTAR @$buttonUsername (MODO DEMO)", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // General Database settings
        Text(
            text = "Mantenimiento local de la App:",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.clearDatabase() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("BORRA HISTORIAL Y COLA LOCAL", fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // Security Caution Details (mandated in gemini-api guidelines Option B info disclosure)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.35f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB300).copy(alpha = 0.05f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "Seguridad", tint = Color(0xFFFFB300))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "AVISO DE SEGURIDAD (PROTOTIPO)", style = MaterialTheme.typography.titleSmall, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Este prototipo utiliza la clave de Gemini inyectada desde los secretos del usuario locales de AI Studio para agilizar las pruebas. Para implementaciones de producción en tiendas públicas, configure Firebase AI App Check para proteger la facturación de sus claves.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
