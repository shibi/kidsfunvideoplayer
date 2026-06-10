package com.example.ui

import android.annotation.SuppressLint
import android.text.format.DateUtils
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.AnalyticsEvent
import com.example.data.Video
import com.example.data.WatchStatus

// Immersive UI premium color scheme
val KidsRed = Color(0xFFBA1A1A)       // Brand Red / Play Icon backdrop
val KidsBlue = Color(0xFF21005D)      // Deep slate violet / dark accent
val KidsYellow = Color(0xFFFFD8E4)    // Accent light pink label background
val KidsGreen = Color(0xFF2E7D32)     // Forest green secondary success/confirm
val KidsCreamBackground = Color(0xFFFEF7FF) // Immersive background violet-white glow
val KidsCardBackground = Color(0xFFEADDFF)  // Soft purplish lavender card

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentVideo by viewModel.currentPlayingVideo.collectAsStateWithLifecycle()
    val isParentMode = viewModel.isParentModeActive

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (currentVideo != null) Color.Black else KidsCreamBackground)
    ) {
        if (currentVideo != null) {
            // Play fullscreen
            CustomFullScreenPlayer(
                video = currentVideo!!,
                onClose = { viewModel.stopVideo() },
                onProgress = { current, duration -> viewModel.updateProgress(current, duration) }
            )
        } else if (isParentMode) {
            // Dashboard views
            ParentDashboardView(viewModel = viewModel)
        } else {
            // Standard feed view
            KidsHomeView(viewModel = viewModel)
        }

        // Adults verification gate
        if (viewModel.isParentGateShowing) {
            ParentGateDialog(viewModel = viewModel)
        }
    }
}

@Composable
fun KidsHomeView(viewModel: MainViewModel) {
    val videos by viewModel.filteredVideos.collectAsStateWithLifecycle()
    val watchStatuses by viewModel.watchStatuses.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    val categories = listOf("All" to "🌎", "Cartoons" to "🐼", "Music" to "🎵", "Learning" to "💡")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // App Launcher Title Header bar (Inspired by Immersive UI header)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(KidsRed, RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color.White, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Kids PlayTube Icon",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "Kids PlayTube",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1D1B20), // Immersive deep text color
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-0.5).sp
                )
            }

            // Lock Settings Button (bg-[#eaddff] rounded-full active:scale-95)
            IconButton(
                onClick = { viewModel.showParentGate() },
                modifier = Modifier
                    .background(Color(0xFFEADDFF), CircleShape)
                    .size(44.dp)
                    .testTag("parent_gate_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings Control",
                    tint = Color(0xFF21005D),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Visual Category Filters with custom emojis (Immersive UI themed)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { (cat, emoji) ->
                val isSelected = selectedCategory == cat
                val buttonColor = if (isSelected) Color(0xFFFFD8E4) else Color.White
                val textColor = if (isSelected) Color(0xFF31111D) else Color(0xFF49454F)
                val borderColor = if (isSelected) Color.Transparent else Color(0xFFCAC4D0)

                Row(
                    modifier = Modifier
                        .background(buttonColor, RoundedCornerShape(18.dp))
                        .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
                        .clickable { viewModel.selectCategory(cat) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("category_button_$cat"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = emoji, fontSize = 16.sp)
                    Text(
                        text = cat,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Favorites / Safe Playlist statistics line (Inspired by Immersive UI category count)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "YOUR FAVORITES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF49454F),
                letterSpacing = 1.sp
            )
            Text(
                text = "${videos.size} Videos Loaded",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6750A4)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grid contents
        if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "No Content Indicator",
                        tint = KidsBlue,
                        modifier = Modifier.size(60.dp)
                    )
                    Text(
                        text = "Your custom playlist is empty!\nAsk a parent to add video links.",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 280.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("videos_grid"),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(videos, key = { it.id }) { video ->
                    val watchStatus = watchStatuses.find { it.videoId == video.id }
                    KidsVideoCard(
                        video = video,
                        watchStatus = watchStatus,
                        onClick = { viewModel.playVideo(video) }
                    )
                }
            }
        }
    }
}

@Composable
fun KidsVideoCard(
    video: Video,
    watchStatus: WatchStatus?,
    onClick: () -> Unit
) {
    val thumbnailUrl = "https://img.youtube.com/vi/${video.id}/hqdefault.jpg"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(2.dp, Color(0xFFEADDFF), RoundedCornerShape(24.dp)) // Immersive purple outline #eaddff
            .clickable { onClick() }
            .testTag("video_card_${video.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = video.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 0.dp, bottomEnd = 0.dp)),
                    contentScale = ContentScale.Crop
                )

                // Category overlay tag (Themed as live category tag #ffd8e4 + #31111d)
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .background(KidsYellow, RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = video.category,
                        color = Color(0xFF31111D), // Deep pink text
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }

                // Super cute play arrow button (Immersive glassmorphic design)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.25f), CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play Action",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Ongoing Watch Progress Bar indicator
                watchStatus?.let {
                    if (it.durationSeconds > 0) {
                        val progressRatio = it.progressSeconds / it.durationSeconds
                        LinearProgressIndicator(
                            progress = { progressRatio.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .align(Alignment.BottomCenter),
                            color = KidsGreen,
                            trackColor = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            // Description info section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = video.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val label = when (video.category) {
                        "Cartoons" -> "🐼 Cartoons"
                        "Music" -> "🎵 Sing-Along"
                        "Learning" -> "💡 Smart Fun"
                        else -> "⭐ Safe Video"
                    }
                    Text(
                        text = label,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Display watch completed tick badge
                    if (watchStatus?.completed == true) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Completed play badge",
                                tint = KidsGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Watched!",
                                color = KidsGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CustomFullScreenPlayer(
    video: Video,
    onClose: () -> Unit,
    onProgress: (currentTime: Float, duration: Float) -> Unit
) {
    val context = LocalContext.current
    var isReady by remember { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.domStorageEnabled = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
        }
    }

    DisposableEffect(webView) {
        val jsInterface = object {
            @JavascriptInterface
            fun onPlayerReady() {
                isReady = true
            }

            @JavascriptInterface
            fun onPlayerStateChange(state: Int) {
                if (state == 0) {
                    onClose() // Go back automatically when ended
                }
            }

            @JavascriptInterface
            fun reportProgress(currentTime: Float, duration: Float) {
                onProgress(currentTime, duration)
            }
        }
        webView.addJavascriptInterface(jsInterface, "AndroidInterface")

        onDispose {
            webView.removeJavascriptInterface("AndroidInterface")
            webView.stopLoading()
            webView.destroy()
        }
    }

    LaunchedEffect(video.id) {
        val htmlEmbed = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    body, html { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #000000; overflow: hidden; }
                    #player { width: 100%; height: 100%; }
                </style>
            </head>
            <body>
                <div id="player"></div>
                <script>
                    var tag = document.createElement('script');
                    tag.src = "https://www.youtube.com/iframe_api";
                    var firstScriptTag = document.getElementsByTagName('script')[0];
                    firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                    var player;
                    function onYouTubeIframeAPIReady() {
                        player = new YT.Player('player', {
                            height: '100%',
                            width: '100%',
                            videoId: '${video.id}',
                            playerVars: {
                                'playsinline': 1,
                                'autoplay': 1,
                                'controls': 1,
                                'rel': 0,
                                'modestbranding': 1,
                                'fs': 0,
                                'origin': 'https://www.youtube.com'
                            },
                            events: {
                                'onReady': onPlayerReady,
                                'onStateChange': onPlayerStateChange
                            }
                        });
                    }

                    function onPlayerReady(event) {
                        AndroidInterface.onPlayerReady();
                    }

                    function onPlayerStateChange(event) {
                        AndroidInterface.onPlayerStateChange(event.data);
                    }

                    setInterval(function() {
                        if (player && player.getCurrentTime && player.getDuration) {
                            AndroidInterface.reportProgress(player.getCurrentTime(), player.getDuration());
                        }
                    }, 1000);
                </script>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL("https://www.youtube.com", htmlEmbed, "text/html", "UTF-8", null)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("fullscreen_player")
    ) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )

        // Back cloud bar button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.Start
        ) {
            Button(
                onClick = { onClose() },
                colors = ButtonDefaults.buttonColors(containerColor = KidsRed),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Color.White),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("close_player_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close Player",
                        tint = Color.White
                    )
                    Text(
                        text = "BACK",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Loader animation in loading iframe
        AnimatedVisibility(
            visible = !isReady,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = KidsRed)
                Text(
                    text = "Loading Kid Video...",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun ParentGateDialog(viewModel: MainViewModel) {
    val challenge = viewModel.parentChallenge
    var inputVal by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { viewModel.closeParentGate() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🔐 Parents Section Gate",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.DarkGray
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Please solve this quick challenge to verify you are a parent and enter video settings.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                if (challenge != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KidsCardBackground, RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = challenge.question,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = KidsRed
                        )
                    }
                }

                OutlinedTextField(
                    value = inputVal,
                    onValueChange = { inputVal = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("Enter answer") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("parent_gate_input"),
                    singleLine = true,
                    isError = viewModel.parentGateError
                )

                if (viewModel.parentGateError) {
                    Text(
                        text = "Incorrect answer. Gate remains locked!",
                        color = KidsRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.submitParentGateAnswer(inputVal) },
                colors = ButtonDefaults.buttonColors(containerColor = KidsGreen),
                modifier = Modifier.testTag("submit_parent_gate_btn")
            ) {
                Text("Confirm Parent", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.closeParentGate() }) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color.White
    )
}

@Composable
fun ParentDashboardView(viewModel: MainViewModel) {
    var activeTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Curate Links" to Icons.Filled.AddCircle, 
        "Play Metrics" to Icons.Filled.Info, 
        "Firebase Sync" to Icons.Filled.Share
    )

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🛠️ Supervisor Controls",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.DarkGray
                    )

                    Button(
                        onClick = { viewModel.exitParentMode() },
                        colors = ButtonDefaults.buttonColors(containerColor = KidsRed),
                        modifier = Modifier.testTag("exit_parent_dashboard")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Lock icon", tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Lock System", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.White,
                    contentColor = KidsBlue
                ) {
                    tabs.forEachIndexed { idx, (title, icon) ->
                        Tab(
                            selected = activeTab == idx,
                            onClick = { activeTab = idx },
                            text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            icon = { Icon(imageVector = icon, contentDescription = title) }
                        )
                    }
                }
            }
        },
        containerColor = KidsCreamBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                0 -> CurateLinksManagement(viewModel = viewModel)
                1 -> AnalyticsDashboard(viewModel = viewModel)
                2 -> FirebaseConfigTab(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun CurateLinksManagement(viewModel: MainViewModel) {
    val videos by viewModel.videos.collectAsStateWithLifecycle()

    var inputTitle by remember { mutableStateOf("") }
    var inputUrl by remember { mutableStateOf("") }
    var inputCategory by remember { mutableStateOf("Cartoons") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Cartoons", "Music", "Learning")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Add Custom YouTube Video Link",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.DarkGray
                    )

                    OutlinedTextField(
                        value = inputTitle,
                        onValueChange = { inputTitle = it },
                        label = { Text("Video Action Title (e.g., Peppa Fun)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_video_title_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        label = { Text("YouTube URL (or short link)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_video_url_input"),
                        singleLine = true
                    )

                    Column {
                        Text(
                            text = "Filter Category Tag",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { c ->
                                val active = inputCategory == c
                                FilterChip(
                                    selected = active,
                                    onClick = { inputCategory = c },
                                    label = { Text(c) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = KidsBlue,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    errorMessage?.let {
                        Text(
                            text = "⚠️ $it",
                            color = KidsRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            errorMessage = null
                            if (inputUrl.isEmpty()) {
                                errorMessage = "Destination video YouTube URL cannot be blank."
                                return@Button
                            }
                            val error = viewModel.addNewVideo(inputTitle, inputUrl, inputCategory)
                            if (error != null) {
                                errorMessage = error
                            } else {
                                inputTitle = ""
                                inputUrl = ""
                                inputCategory = "Cartoons"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KidsGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_video_submit_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Video to Playlist", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "Currently Curation-Allowed Videos (${videos.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(videos, key = { it.id }) { video ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val thumb = "https://img.youtube.com/vi/${video.id}/hqdefault.jpg"
                    AsyncImage(
                        model = thumb,
                        contentDescription = "Curated thumbnail",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = video.url,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Category: ${video.category}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = KidsBlue
                        )
                    }

                    IconButton(
                        onClick = { viewModel.removeVideo(video.id) },
                        modifier = Modifier.testTag("delete_curation_btn_${video.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete curation link",
                            tint = KidsRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsDashboard(viewModel: MainViewModel) {
    val events by viewModel.analyticsEvents.collectAsStateWithLifecycle()
    val watchStatuses by viewModel.watchStatuses.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()

    val totalPlays = watchStatuses.sumOf { it.playCount }
    val completedCount = watchStatuses.count { it.completed }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(title = "Videos Curated", count = videos.size.toString(), color = KidsBlue, modifier = Modifier.weight(1f))
                StatCard(title = "Total Plays", count = totalPlays.toString(), color = KidsYellow, modifier = Modifier.weight(1f))
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(title = "Completed Plays", count = completedCount.toString(), color = KidsGreen, modifier = Modifier.weight(1f))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .weight(1f)
                        .clickable { viewModel.clearLocalStats() },
                    colors = CardDefaults.cardColors(containerColor = KidsRed.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, KidsRed)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear logs icon", tint = KidsRed)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Reset Logs", fontWeight = FontWeight.Bold, color = KidsRed, fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            Text(
                text = "Live Playback Log Feed",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.DarkGray
            )
        }

        if (events.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No play records yet. Let your kid start watching!", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(events) { event ->
                AnalyticsEventItem(event)
            }
        }
    }
}

@Composable
fun StatCard(title: String, count: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(84.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.dp, color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(count, color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun AnalyticsEventItem(event: AnalyticsEvent) {
    val dateString = DateUtils.getRelativeTimeSpanString(event.timestamp)
    val indicatorColor = when (event.eventType) {
        "PLAY" -> KidsBlue
        "PAUSE" -> KidsYellow
        "COMPLETE" -> KidsGreen
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(indicatorColor, CircleShape)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${event.eventType}: ${event.videoTitle}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.Black
                )
                Text(
                    text = "ID: ${event.videoId} • $dateString",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun FirebaseConfigTab(viewModel: MainViewModel) {
    val config by viewModel.firebaseConfigState.collectAsStateWithLifecycle()

    var dbUrl by remember(config) { mutableStateOf(config["url"] ?: "") }
    var apiKey by remember(config) { mutableStateOf(config["apiKey"] ?: "") }
    var projId by remember(config) { mutableStateOf(config["projectId"] ?: "") }
    var appId by remember(config) { mutableStateOf(config["appId"] ?: "") }
    var syncEnabled by remember(config) { mutableStateOf(config["enabled"] == "true") }

    var saveStatusMsg by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cloud Firebase Remote Sync Settings",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color.DarkGray
                    )

                    Text(
                        text = "Input your Firebase Realtime Database credentials below. When enabled, your local playlist will synchronize with Firebase Realtime Database path '/videos' in real-time. Kid watch statuses and play log analytics will automatically write to '/watch_status' and '/analytics'.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Enable Firebase Remote Sync",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.DarkGray
                        )
                        Switch(
                            checked = syncEnabled,
                            onCheckedChange = { syncEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = KidsGreen)
                        )
                    }

                    OutlinedTextField(
                        value = dbUrl,
                        onValueChange = { dbUrl = it },
                        label = { Text("Database URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("https://xxx-rtdb.firebaseio.com") }
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Web API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = projId,
                            onValueChange = { projId = it },
                            label = { Text("Project ID") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = appId,
                            onValueChange = { appId = it },
                            label = { Text("Application ID") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    saveStatusMsg?.let {
                        Text(
                            text = "✅ $it",
                            color = KidsGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.saveSettings(
                                url = dbUrl,
                                apiKey = apiKey,
                                projectId = projId,
                                appId = appId,
                                enabled = syncEnabled
                            )
                            saveStatusMsg = "Configuration saved!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KidsBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save & Apply Config", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
