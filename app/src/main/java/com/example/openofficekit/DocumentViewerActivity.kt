package com.example.openofficekit
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poirender.sdk.PoiRenderSDK
import com.poirender.sdk.model.*
import com.poirender.sdk.renderer.DocxRenderer
import com.poirender.sdk.renderer.ExcelRenderer
import com.poirender.sdk.renderer.PptxRenderer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.net.toUri

class DocumentViewerActivity : ComponentActivity() {

    companion object {
        private const val TAG = "DocumentViewer"
    }

    private lateinit var sdk: PoiRenderSDK

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sdk = PoiRenderSDK.init(this)

        val uriString = intent.getStringExtra("document_uri")
        if (uriString == null) {
            Toast.makeText(this, "No document provided.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val uri = uriString.toUri()
        Log.d(TAG, "Opening document: $uri")

        var initialDocName = "unknown"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                initialDocName = cursor.getString(nameIndex)
            }
        }
        if (initialDocName == "unknown") {
            initialDocName = uri.lastPathSegment ?: "unknown"
        }
        Log.d(TAG, "Resolved document name: $initialDocName")

        setContent {
            var isDarkMode by remember { mutableStateOf(false) }
            var isHighContrast by remember { mutableStateOf(false) }
            var textScale by remember { mutableFloatStateOf(1f) }

            MaterialTheme(
                colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()
            ) {
                var searchQuery by remember { mutableStateOf("") }
                var searchInput by remember { mutableStateOf("") }
                var showSearchBar by remember { mutableStateOf(false) }

                var docxPages by remember { mutableStateOf<List<DocumentPage>?>(null) }
                var excelWorkbook by remember { mutableStateOf<WorkbookData?>(null) }
                var pptxSlides by remember { mutableStateOf<List<SlideData>?>(null) }
                
                var errorMessage by remember { mutableStateOf<String?>(null) }
                var isLoading by remember { mutableStateOf(true) }
                var progressValue by remember { mutableFloatStateOf(0f) }
                
                var requiresPassword by remember { mutableStateOf(false) }

                val coroutineScope = rememberCoroutineScope()

                fun loadDocument() {
                    isLoading = true
                    errorMessage = null
                    requiresPassword = false
                    progressValue = 0f
                    
                    // Mocking a password prompt for demonstration if filename contains "secret"
                    if (initialDocName.contains("secret", ignoreCase = true) && !requiresPassword) {
                        requiresPassword = true
                        isLoading = false
                        return
                    }

                    coroutineScope.launch {
                        Log.d(TAG, "Starting to parse document: $initialDocName")
                        val onProgress: (Float) -> Unit = { 
                            progressValue = it 
                            Log.v(TAG, "Loading progress: ${(it * 100).toInt()}%")
                        }
                        when {
                            initialDocName.endsWith(".docx", true) -> {
                                Log.i(TAG, "Parsing as DOCX")
                                sdk.parseDocx(uri, onProgress).onSuccess { 
                                    docxPages = it; isLoading = false 
                                    Log.i(TAG, "DOCX parsed successfully: ${it.size} pages")
                                }
                                    .onFailure { 
                                        errorMessage = it.message; isLoading = false 
                                        Log.e(TAG, "Failed to parse DOCX", it)
                                    }
                            }
                            initialDocName.endsWith(".pptx", true) || initialDocName.endsWith(".ppt", true) -> {
                                Log.i(TAG, "Parsing as PPTX")
                                sdk.parsePptx(uri, onProgress).onSuccess { 
                                    pptxSlides = it; isLoading = false 
                                    Log.i(TAG, "PPTX parsed successfully: ${it.size} slides")
                                }
                                    .onFailure { 
                                        errorMessage = it.message; isLoading = false 
                                        Log.e(TAG, "Failed to parse PPTX", it)
                                    }
                            }
                            initialDocName.endsWith(".xlsx", true) || initialDocName.endsWith(".xls", true) -> {
                                Log.i(TAG, "Parsing as EXCEL")
                                sdk.parseExcel(uri, onProgress).onSuccess { 
                                    excelWorkbook = it; isLoading = false 
                                    Log.i(TAG, "Excel parsed successfully: ${it.sheets.size} sheets")
                                }
                                    .onFailure { 
                                        errorMessage = it.message; isLoading = false 
                                        Log.e(TAG, "Failed to parse Excel", it)
                                    }
                            }
                            else -> {
                                Log.w(TAG, "Unsupported file type: $initialDocName")
                                errorMessage = "Unsupported file type: Please provide a valid DOCX, XLSX, or PPTX file."
                                isLoading = false
                            }
                        }
                    }
                }

                LaunchedEffect(uri) {
                    loadDocument()
                }

                var scale by remember { mutableFloatStateOf(1f) }
                var offsetX by remember { mutableFloatStateOf(0f) }
                var offsetY by remember { mutableFloatStateOf(0f) }
                var showMenu by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(initialDocName, fontSize = 16.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                                    val badge = initialDocName.substringAfterLast(".", "unknown").uppercase()
                                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                                        Text(badge, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            },
                            actions = {
                                IconButton(onClick = { showSearchBar = !showSearchBar }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(text = { Text("Print") }, onClick = { showMenu = false; Toast.makeText(this@DocumentViewerActivity, "Print...", Toast.LENGTH_SHORT).show() })
                                    DropdownMenuItem(text = { Text("Share Link") }, onClick = { showMenu = false; Toast.makeText(this@DocumentViewerActivity, "Sharing...", Toast.LENGTH_SHORT).show() })
                                    DropdownMenuItem(text = { Text("Download File") }, onClick = { showMenu = false; Toast.makeText(this@DocumentViewerActivity, "Downloading...", Toast.LENGTH_SHORT).show() })
                                    HorizontalDivider(
                                        Modifier,
                                        DividerDefaults.Thickness,
                                        DividerDefaults.color
                                    )
                                    DropdownMenuItem(text = { Text(if (isDarkMode) "Light Mode" else "Dark Mode") }, onClick = { isDarkMode = !isDarkMode; showMenu = false })
                                    DropdownMenuItem(text = { Text(if (isHighContrast) "Normal Contrast" else "High Contrast") }, onClick = { isHighContrast = !isHighContrast; showMenu = false })
                                    DropdownMenuItem(text = { Text("Text Size Override (+20%)") }, onClick = { textScale += 0.2f; showMenu = false })
                                    HorizontalDivider(
                                        Modifier,
                                        DividerDefaults.Thickness,
                                        DividerDefaults.color
                                    )
                                    DropdownMenuItem(text = { Text("Zoom 50%") }, onClick = { scale = 0.5f; offsetX = 0f; offsetY = 0f; showMenu = false })
                                    DropdownMenuItem(text = { Text("Zoom 75%") }, onClick = { scale = 0.75f; offsetX = 0f; offsetY = 0f; showMenu = false })
                                    DropdownMenuItem(text = { Text("Zoom 100%") }, onClick = { scale = 1.0f; offsetX = 0f; offsetY = 0f; showMenu = false })
                                    DropdownMenuItem(text = { Text("Fit to Screen / Width") }, onClick = { scale = 1.0f; offsetX = 0f; offsetY = 0f; showMenu = false })
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                        if (showSearchBar) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = searchInput,
                                    onValueChange = { searchInput = it; searchQuery = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Find in document...") },
                                    singleLine = true
                                )
                                IconButton(onClick = { showSearchBar = false; searchQuery = ""; searchInput = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }
                        }

                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Document is loading...",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    // Loading skeleton
                                    Box(modifier = Modifier.fillMaxWidth(0.8f).height(20.dp).background(Color.LightGray.copy(alpha=0.3f)))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.fillMaxWidth(0.6f).height(20.dp).background(Color.LightGray.copy(alpha=0.3f)))
                                }

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${(progressValue * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { progressValue },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp),
                                        strokeCap = StrokeCap.Round
                                    )
                                }
                            }
                        }
else if (requiresPassword) {
                            Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.Lock, contentDescription = "Password", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Password Protected File", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(value = "", onValueChange = {}, label = { Text("Enter Password") })
                                Button(onClick = { requiresPassword = false; isLoading = true; coroutineScope.launch { delay(500); loadDocument() } }, modifier = Modifier.padding(top = 8.dp)) { Text("Unlock") }
                            }
                        } else if (errorMessage != null) {
                            Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(onClick = { loadDocument() }) {
                                    Text("Retry Loading")
                                }
                            }
                        } else {
                            // Document Viewport
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            scale = (scale * zoom).coerceIn(0.1f, 5f)
                                            offsetX += pan.x * scale
                                            offsetY += pan.y * scale
                                        }
                                    }
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    )
                            ) {
                                docxPages?.let { DocxRenderer(pages = it, searchQuery = searchQuery) }
                                excelWorkbook?.let { ExcelRenderer(workbook = it, searchQuery = searchQuery) }
                                pptxSlides?.let { PptxRenderer(slides = it, searchQuery = searchQuery) }
                            }
                        }
                    }
                }
            }
        }
    }
}
