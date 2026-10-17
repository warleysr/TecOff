package br.com.tecoff.ui.screens.login

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import br.com.tecoff.data.remote.SessionManager
import br.com.tecoff.data.remote.TecApiService
import br.com.tecoff.data.remote.TecHttpClient
import br.com.tecoff.ui.theme.TecPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(
    sessionManager: SessionManager,
    apiService: TecApiService,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: WebView, 1: Import Cookies
    var cookieText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Login no TEC Concursos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TecPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Login com Navegador") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Importar Cookies") }
                )
            }

            if (selectedTab == 0) {
                // WebView login mode
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    userAgentString = TecHttpClient.USER_AGENT
                                }
                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        checkCookies()
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        checkCookies()
                                    }

                                    private fun checkCookies() {
                                        val cookiesString = cookieManager.getCookie("https://www.tecconcursos.com.br")
                                        if (!cookiesString.isNullOrBlank()) {
                                            if (cookiesString.contains("TecPermanecerLogado") || cookiesString.contains("JSESSIONID")) {
                                                sessionManager.importCookiesFromText(cookiesString)
                                                scope.launch {
                                                    val logged = apiService.whoami()
                                                    if (logged) {
                                                        Toast.makeText(context, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                                                        onLoginSuccess()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                loadUrl("${TecHttpClient.BASE_URL}/login")
                            }
                        }
                    )
                }
            } else {
                // Manual Cookie import mode (like `tec login --cookies`)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Importar Cookies de Sessão",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cole aqui os cookies do TEC Concursos (no formato 'nome=valor; nome2=valor2' ou JSON). " +
                                "O cookie 'TecPermanecerLogado' permite manter o login ativo por até 14 dias.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = cookieText,
                        onValueChange = { cookieText = it },
                        label = { Text("Cookies (TecPermanecerLogado, JSESSIONID...)") },
                        placeholder = { Text("TecPermanecerLogado=...; JSESSIONID=...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    statusMessage?.let {
                        Text(
                            text = it,
                            color = if (it.startsWith("Erro")) MaterialTheme.colorScheme.error else TecPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Button(
                        onClick = {
                            if (cookieText.isNotBlank()) {
                                isLoading = true
                                statusMessage = "Validando cookies com o TEC..."
                                sessionManager.importCookiesFromText(cookieText)
                                scope.launch {
                                    val logged = apiService.whoami()
                                    isLoading = false
                                    if (logged) {
                                        Toast.makeText(context, "Sessão validada com sucesso!", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    } else {
                                        statusMessage = "Erro: Cookies inválidos ou expirados pelo TEC Concursos."
                                    }
                                }
                            }
                        },
                        enabled = !isLoading && cookieText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Validando...")
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Salvar e Validar Cookies")
                        }
                    }
                }
            }
        }
    }
}
