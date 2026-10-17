package br.com.tecoff.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.tecoff.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetadataChips(
    banca: String?,
    orgao: String?,
    cargo: String?,
    ano: Int?,
    dificuldade: String?,
    materiaNome: String?,
    assuntoNome: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Matéria e Assunto Header
        if (!materiaNome.isNullOrBlank() || !assuntoNome.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                if (!materiaNome.isNullOrBlank()) {
                    Text(
                        text = materiaNome,
                        color = TecAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                if (!materiaNome.isNullOrBlank() && !assuntoNome.isNullOrBlank()) {
                    Text(
                        text = " › ",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                if (!assuntoNome.isNullOrBlank()) {
                    Text(
                        text = assuntoNome,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }

        // Tags FlowRow
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            banca?.let { TagChip(text = it, containerColor = TecPrimary.copy(alpha = 0.1f), textColor = TecPrimary) }
            orgao?.let { TagChip(text = it, containerColor = MaterialTheme.colorScheme.surfaceVariant, textColor = MaterialTheme.colorScheme.onSurfaceVariant) }
            ano?.let { TagChip(text = it.toString(), containerColor = MaterialTheme.colorScheme.surfaceVariant, textColor = MaterialTheme.colorScheme.onSurfaceVariant) }
            cargo?.let { TagChip(text = it, containerColor = MaterialTheme.colorScheme.surfaceVariant, textColor = MaterialTheme.colorScheme.onSurfaceVariant) }
            dificuldade?.let {
                val (color, label) = parseDificuldade(it)
                TagChip(text = label, containerColor = color.copy(alpha = 0.12f), textColor = color)
            }
        }
    }
}

@Composable
fun TagChip(
    text: String,
    containerColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun parseDificuldade(dif: String): Pair<Color, String> {
    return when {
        dif.contains("MUITO_FACIL", ignoreCase = true) || dif.contains("muito fácil", ignoreCase = true) ->
            Pair(Color(0xFF059669), "Muito Fácil")
        dif.contains("FACIL", ignoreCase = true) || dif.contains("fácil", ignoreCase = true) ->
            Pair(TecSuccess, "Fácil")
        dif.contains("MEDIA", ignoreCase = true) || dif.contains("média", ignoreCase = true) ->
            Pair(TecWarning, "Média")
        dif.contains("MUITO_DIFICIL", ignoreCase = true) || dif.contains("muito difícil", ignoreCase = true) ->
            Pair(Color(0xFF991B1B), "Muito Difícil")
        dif.contains("DIFICIL", ignoreCase = true) || dif.contains("difícil", ignoreCase = true) ->
            Pair(TecError, "Difícil")
        else -> Pair(TecTextSecondaryLight, dif)
    }
}
