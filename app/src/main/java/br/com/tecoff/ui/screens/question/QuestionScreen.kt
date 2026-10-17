package br.com.tecoff.ui.screens.question

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.tecoff.ui.components.AlternativeItem
import br.com.tecoff.ui.components.HtmlText
import br.com.tecoff.ui.components.ImageViewerDialog
import br.com.tecoff.ui.components.MetadataChips
import br.com.tecoff.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionScreen(
    viewModel: QuestionViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var activeZoomImageUrl by remember { mutableStateOf<String?>(null) }

    // Reset scroll when question index changes
    LaunchedEffect(uiState.currentIndex) {
        scrollState.scrollTo(0)
    }

    // Full-screen Zoomable Image Viewer Dialog
    activeZoomImageUrl?.let { url ->
        ImageViewerDialog(
            imageUrl = url,
            onDismiss = { activeZoomImageUrl = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.cadernoNome,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Questão ${uiState.currentIndex + 1} de ${uiState.totalQuestions}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                            uiState.currentQuestion?.let { q ->
                                Text(
                                    text = " • #${q.id}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    // Pending sync badge
                    if (uiState.currentQuestion?.pendingSync == true) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(TecWarning)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "OFFLINE",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TecPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            // Individual Cards with Icons & Long-press Tooltips (no cut-off text labels)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Anterior
                    NavButtonCard(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        tooltipText = "Questão Anterior",
                        enabled = uiState.canGoBackward,
                        onClick = { viewModel.goBackward() },
                        tint = MaterialTheme.colorScheme.onSurface
                    )

                    // 2. Próxima não resolvida
                    NavButtonCard(
                        icon = Icons.Outlined.SkipNext,
                        tooltipText = "Próxima Não Resolvida",
                        enabled = uiState.unsolvedCount > 0,
                        onClick = { viewModel.goNextUnsolved() },
                        tint = TecAccent
                    )

                    // 3. Aleatória não resolvida
                    NavButtonCard(
                        icon = Icons.Outlined.Shuffle,
                        tooltipText = "Questão Aleatória (Não Resolvida)",
                        enabled = uiState.unsolvedCount > 0,
                        onClick = { viewModel.goRandomUnsolved() },
                        tint = TecPrimary
                    )

                    // 4. Desfazer navegação (Undo)
                    NavButtonCard(
                        icon = Icons.AutoMirrored.Filled.Undo,
                        tooltipText = "Desfazer Última Navegação",
                        enabled = uiState.canUndo,
                        onClick = { viewModel.undoNavigation() },
                        tint = TecWarning
                    )

                    // 5. Próxima
                    NavButtonCard(
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        tooltipText = "Próxima Questão",
                        enabled = uiState.canGoForward,
                        onClick = { viewModel.goForward() },
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TecPrimary)
            }
        } else if (uiState.currentQuestion == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhuma questão disponível neste caderno.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val q = uiState.currentQuestion!!

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Metadata Tags
                MetadataChips(
                    banca = q.banca,
                    orgao = q.orgao,
                    cargo = q.cargo,
                    ano = q.ano,
                    dificuldade = q.dificuldade,
                    materiaNome = q.materiaNome,
                    assuntoNome = q.assuntoNome
                )

                // Enunciado (Question Statement with offline HTML images and tap to zoom)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Enunciado",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        HtmlText(
                            html = q.enunciadoHtml,
                            onImageClick = { activeZoomImageUrl = it }
                        )
                    }
                }

                // Alternatives Header
                Text(
                    text = "Alternativas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Alternatives List
                val letters = if (q.isCertoErrado) listOf("C", "E") else listOf("A", "B", "C", "D", "E")
                q.alternativasHtml.forEachIndexed { idx, altHtml ->
                    val altIndex = idx + 1 // 1-based index
                    val letter = letters.getOrElse(idx) { ('A'.code + idx).toChar().toString() }

                    val isCorrectAlternative = when {
                        q.numeroAlternativaCorreta != null -> q.numeroAlternativaCorreta == altIndex
                        q.isCertoErrado -> {
                            val gab = q.gabarito?.trim()?.uppercase() ?: ""
                            if (gab.startsWith("C") || gab == "1") altIndex == 1
                            else if (gab.startsWith("E") || gab == "2") altIndex == 2
                            else false
                        }
                        !q.gabarito.isNullOrBlank() -> {
                            val gab = q.gabarito.trim().uppercase()
                            letters.indexOf(gab) + 1 == altIndex
                        }
                        else -> false
                    }

                    val isSelected = uiState.selectedAlternative == altIndex
                    val isStrikethrough = uiState.strikethroughList.contains(altIndex)

                    AlternativeItem(
                        letter = letter,
                        htmlContent = altHtml,
                        isSelected = isSelected,
                        isCorrect = uiState.isCorrect,
                        isCorrectAlternative = isCorrectAlternative,
                        isAnswered = uiState.isAnswered,
                        isStrikethrough = isStrikethrough,
                        onSelect = { viewModel.selectAlternative(altIndex) },
                        onToggleStrikethrough = { viewModel.toggleStrikethrough(altIndex) },
                        onImageClick = { activeZoomImageUrl = it }
                    )
                }

                // Submit Button (only shown before answering)
                if (!uiState.isAnswered) {
                    Button(
                        onClick = { viewModel.submitAnswer() },
                        enabled = uiState.selectedAlternative != null,
                        colors = ButtonDefaults.buttonColors(containerColor = TecPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Responder",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Result Feedback Banner
                    val correctLabel = when {
                        q.isCertoErrado -> {
                            if (q.numeroAlternativaCorreta == 1 || q.gabarito?.contains("Certo", true) == true) "Certo" else "Errado"
                        }
                        q.numeroAlternativaCorreta != null -> {
                            "Letra " + letters.getOrElse(q.numeroAlternativaCorreta - 1) { q.numeroAlternativaCorreta.toString() }
                        }
                        !q.gabarito.isNullOrBlank() -> "Letra " + q.gabarito
                        else -> "Ver comentário"
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.isCorrect == true) TecSuccessContainer else TecErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (uiState.isCorrect == true) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (uiState.isCorrect == true) TecSuccess else TecError,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (uiState.isCorrect == true) "Você acertou! 🎉" else "Resposta incorreta.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (uiState.isCorrect == true) TecSuccessText else TecErrorText
                                )
                                Text(
                                    text = "Gabarito oficial: $correctLabel",
                                    fontSize = 13.sp,
                                    color = if (uiState.isCorrect == true) TecSuccessText else TecErrorText
                                )
                            }
                        }
                    }
                }

                // Professor Commentary Section
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TecAccent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleProfessorComment() }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                tint = TecAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Comentário do Professor",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TecAccent,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (uiState.showProfessorComment) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TecAccent
                            )
                        }

                        AnimatedVisibility(visible = uiState.showProfessorComment) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(10.dp))
                                val comText = if (!q.comentarioHtml.isNullOrBlank()) {
                                    q.comentarioHtml
                                } else {
                                    "<p style=\"color: #64748B; font-style: italic;\">Esta questão ainda não possui comentário do professor cadastrado no TEC Concursos.</p>"
                                }
                                HtmlText(
                                    html = comText,
                                    onImageClick = { activeZoomImageUrl = it }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavButtonCard(
    icon: ImageVector,
    tooltipText: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tint: Color,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(tooltipText)
            }
        },
        state = rememberTooltipState()
    ) {
        ElevatedCard(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = containerColor
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = if (enabled) 3.dp else 0.dp
            ),
            modifier = Modifier.size(54.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = tooltipText,
                    tint = if (enabled) tint else tint.copy(alpha = 0.25f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
