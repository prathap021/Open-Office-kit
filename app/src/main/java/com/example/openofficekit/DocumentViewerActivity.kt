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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.webkit.WebView
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
            var webViewRef by remember { mutableStateOf<WebView?>(null) }

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
                            initialDocName.endsWith(".docx", true) || initialDocName.endsWith(".doc", true) -> {
                                Log.i(TAG, "Parsing as DOCX/DOC")
                                sdk.parseDocx(uri, onProgress).onSuccess { 
                                    docxPages = it; isLoading = false 
                                    Log.i(TAG, "DOCX/DOC parsed successfully: ${it.size} pages")
                                }
                                    .onFailure { 
                                        errorMessage = it.message; isLoading = false 
                                        Log.e(TAG, "Failed to parse DOCX/DOC", it)
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
                                errorMessage = "Unsupported file type: Please provide a valid DOCX, DOC, XLSX, XLS, PPTX, or PPT file."
                                isLoading = false
                            }
                        }
                    }
                }

                LaunchedEffect(uri) {
                    loadDocument()
                }

                val extension = initialDocName.substringAfterLast(".", "").lowercase()
                val (toolbarBg, toolbarText) = when {
                    extension in listOf("doc", "docx") -> Pair(
                        androidx.compose.ui.res.colorResource(R.color.word_toolbar_bg),
                        androidx.compose.ui.res.colorResource(R.color.word_toolbar_icon)
                    )
                    extension in listOf("xls", "xlsx") -> Pair(
                        androidx.compose.ui.res.colorResource(R.color.excel_toolbar_bg),
                        androidx.compose.ui.res.colorResource(R.color.excel_toolbar_icon)
                    )
                    extension in listOf("ppt", "pptx") -> Pair(
                        androidx.compose.ui.res.colorResource(R.color.ppt_toolbar_bg),
                        androidx.compose.ui.res.colorResource(R.color.ppt_toolbar_icon)
                    )
                    else -> Pair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                }

                Scaffold(
                    topBar = {
                        @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = toolbarBg,
                                titleContentColor = toolbarText,
                                actionIconContentColor = toolbarText
                            ),
                            title = {
                                if (showSearchBar) {
                                    TextField(
                                        value = searchInput,
                                        onValueChange = { 
                                            searchInput = it
                                            searchQuery = it
                                            webViewRef?.findAllAsync(it)
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                                        placeholder = { Text("Find in document...") },
                                        singleLine = true,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                            focusedTextColor = toolbarText,
                                            unfocusedTextColor = toolbarText,
                                            cursorColor = toolbarText,
                                            focusedIndicatorColor = toolbarText,
                                            unfocusedIndicatorColor = toolbarText.copy(alpha = 0.5f)
                                        )
                                    )
                                } else {
                                    Text(initialDocName, fontSize = 16.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                                }
                            },
                            actions = {
                                if (showSearchBar) {
                                    IconButton(onClick = { webViewRef?.findNext(false) }) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous Match")
                                    }
                                    IconButton(onClick = { webViewRef?.findNext(true) }) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next Match")
                                    }
                                    IconButton(onClick = { 
                                        showSearchBar = false
                                        searchQuery = ""
                                        searchInput = ""
                                        webViewRef?.clearMatches()
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close")
                                    }
                                } else {
                                    IconButton(onClick = { showSearchBar = true }) {
                                        Icon(Icons.Default.Search, contentDescription = "Search")
                                    }
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {

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
                            // Document Viewport (WebView based zoom)
                            docxPages?.let { 
                                com.poirender.sdk.renderer.DocxWebView(
                                    docxPages = it,
                                    isDarkMode = isDarkMode,
                                    textScale = textScale,
                                    onWebViewCreated = { webViewRef = it }
                                ) 
                            }
                            excelWorkbook?.let { 
                                com.poirender.sdk.renderer.ExcelWebView(
                                    excelWorkbook = it,
                                    isDarkMode = isDarkMode,
                                    textScale = textScale,
                                    onWebViewCreated = { webViewRef = it }
                                ) 
                            }
                            pptxSlides?.let { 
                                com.poirender.sdk.renderer.PptxWebView(
                                    pptxSlides = it,
                                    isDarkMode = isDarkMode,
                                    textScale = textScale,
                                    onWebViewCreated = { webViewRef = it }
                                ) 
                            }
                        }
                    }
                }
            }
        }
    }
}
