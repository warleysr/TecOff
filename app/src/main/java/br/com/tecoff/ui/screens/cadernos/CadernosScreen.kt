package br.com.tecoff.ui.screens.cadernos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.tecoff.data.local.entity.PastaEntity
import br.com.tecoff.data.local.entity.SubpastaEntity
import br.com.tecoff.data.model.Caderno
import br.com.tecoff.ui.components.SessionExpiredDialog
import br.com.tecoff.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadernosScreen(
    viewModel: CadernosViewModel,
    onOpenCaderno: (cadernoId: Long) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExpiredDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSessionExpired) {
        if (uiState.isSessionExpired) {
            showExpiredDialog = true
        }
    }

    if (showExpiredDialog) {
        SessionExpiredDialog(
            onLoginClick = {
                showExpiredDialog = false
                onNavigateToLogin()
            },
            onDismiss = { showExpiredDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "TecOff",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (uiState.isLogged) TecSuccess else TecWarning)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (uiState.isLogged) "ONLINE" else "OFFLINE",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    // Sync Pending Resolutions Button
                    if (uiState.totalPendingSync > 0) {
                        IconButton(onClick = { viewModel.syncOfflineResolutions() }) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = TecWarning) {
                                        Text("${uiState.totalPendingSync}")
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.CloudUpload,
                                    contentDescription = "Sincronizar resoluções",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    // Refresh / Sync Folders Button
                    IconButton(onClick = { viewModel.syncAllPastasAndCadernos() }) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Atualizar cadernos do TEC",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // Login / Account Button
                    IconButton(onClick = onNavigateToLogin) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Conta / Login",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TecPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Sync Progress Banner
            AnimatedVisibility(visible = uiState.syncProgress.isSyncing) {
                Surface(
                    color = TecPrimaryDark,
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.syncProgress.statusText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            if (uiState.syncProgress.total > 0) {
                                Text(
                                    text = "${uiState.syncProgress.current}/${uiState.syncProgress.total}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (uiState.syncProgress.total > 0) {
                            LinearProgressIndicator(
                                progress = { uiState.syncProgress.current.toFloat() / uiState.syncProgress.total },
                                modifier = Modifier.fillMaxWidth(),
                                color = TecAccentLight,
                                trackColor = TecPrimaryLight
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = TecAccentLight,
                                trackColor = TecPrimaryLight
                            )
                        }
                    }
                }
            }

            // Error Message Banner
            uiState.errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = TecErrorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = TecError)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = TecErrorText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.dismissError() }) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = TecError)
                        }
                    }
                }
            }

            // Empty state when no folders exist yet
            if (uiState.pastas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = TecPrimary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Nenhum caderno sincronizado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Faça login com sua conta do TEC Concursos para sincronizar suas pastas e cadernos de estudo para resolução offline.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = onNavigateToLogin,
                            colors = ButtonDefaults.buttonColors(containerColor = TecPrimary)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Fazer Login no TEC")
                        }
                    }
                }
            } else {
                // Hierarchical Folders / Subfolders / Cadernos List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.pastas, key = { it.id }) { pasta ->
                        PastaCard(
                            pasta = pasta,
                            subpastas = uiState.subpastasByPasta[pasta.id] ?: emptyList(),
                            cadernosByFolder = uiState.cadernosByFolder,
                            onOpenCaderno = onOpenCaderno,
                            onDownloadCaderno = { viewModel.downloadCaderno(it) },
                            onSyncCaderno = { viewModel.syncOfflineResolutions(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PastaCard(
    pasta: PastaEntity,
    subpastas: List<SubpastaEntity>,
    cadernosByFolder: Map<String, List<Caderno>>,
    onOpenCaderno: (Long) -> Unit,
    onDownloadCaderno: (Long) -> Unit,
    onSyncCaderno: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(pasta.ativa || subpastas.isNotEmpty()) }
    val directCadernos = cadernosByFolder["pasta_${pasta.id}"] ?: emptyList()
    val totalCadernos = directCadernos.size + subpastas.sumOf { cadernosByFolder["subpasta_${it.id}"]?.size ?: 0 }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Folder Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.FolderOpen else Icons.Filled.Folder,
                    contentDescription = null,
                    tint = TecPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pasta.nome,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$totalCadernos cadernos",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Direct cadernos in this folder
                    directCadernos.forEach { caderno ->
                        CadernoItem(
                            caderno = caderno,
                            onOpen = { onOpenCaderno(caderno.id) },
                            onDownload = { onDownloadCaderno(caderno.id) },
                            onSync = { onSyncCaderno(caderno.id) }
                        )
                    }

                    // Subfolders
                    subpastas.forEach { subpasta ->
                        SubpastaCard(
                            subpasta = subpasta,
                            cadernos = cadernosByFolder["subpasta_${subpasta.id}"] ?: emptyList(),
                            onOpenCaderno = onOpenCaderno,
                            onDownloadCaderno = onDownloadCaderno,
                            onSyncCaderno = onSyncCaderno
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubpastaCard(
    subpasta: SubpastaEntity,
    cadernos: List<Caderno>,
    onOpenCaderno: (Long) -> Unit,
    onDownloadCaderno: (Long) -> Unit,
    onSyncCaderno: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FolderSpecial,
                    contentDescription = null,
                    tint = TecAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = subpasta.nome,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${cadernos.size}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    cadernos.forEach { caderno ->
                        CadernoItem(
                            caderno = caderno,
                            onOpen = { onOpenCaderno(caderno.id) },
                            onDownload = { onDownloadCaderno(caderno.id) },
                            onSync = { onSyncCaderno(caderno.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CadernoItem(
    caderno: Caderno,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    onSync: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = caderno.nome,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${caderno.quantidadeItens} questões • ${caderno.data ?: ""}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Offline status badge
                if (caderno.downloadedOffline) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TecSuccessContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "OFFLINE OK",
                            color = TecSuccessText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Progress Bar (if downloaded and resolved)
            if (caderno.downloadedOffline) {
                Spacer(modifier = Modifier.height(8.dp))
                val progress = if (caderno.quantidadeItens > 0) {
                    caderno.totalResolucoes.toFloat() / caderno.quantidadeItens
                } else 0f

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = TecAccent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Resolvidas: ${caderno.totalResolucoes}/${caderno.quantidadeItens}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (caderno.totalResolucoes > 0) {
                        Text(
                            text = "Acertos: %.1f%% (%d/%d)".format(caderno.taxaAcerto, caderno.totalAcertos, caderno.totalResolucoes),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (caderno.taxaAcerto >= 70f) TecSuccess else TecError
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (caderno.pendenciasSync > 0) {
                    OutlinedButton(
                        onClick = onSync,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = TecWarning)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sincronizar (${caderno.pendenciasSync})", fontSize = 12.sp, color = TecWarning)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (!caderno.downloadedOffline) {
                    Button(
                        onClick = onDownload,
                        colors = ButtonDefaults.buttonColors(containerColor = TecAccent),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Baixar Offline", fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = onOpen,
                        colors = ButtonDefaults.buttonColors(containerColor = TecPrimary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resolver", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
