package br.com.tecoff.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.tecoff.ui.theme.*

@Composable
fun AlternativeItem(
    letter: String,
    htmlContent: String,
    isSelected: Boolean,
    isCorrect: Boolean?,
    isCorrectAlternative: Boolean,
    isAnswered: Boolean,
    isStrikethrough: Boolean,
    onSelect: () -> Unit,
    onToggleStrikethrough: () -> Unit,
    onImageClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Determine colors based on state
    val borderColor: Color
    val backgroundColor: Color
    val badgeColor: Color
    val badgeTextColor: Color

    when {
        isAnswered -> {
            when {
                isCorrectAlternative -> {
                    borderColor = TecSuccess
                    backgroundColor = TecSuccessContainer.copy(alpha = 0.5f)
                    badgeColor = TecSuccess
                    badgeTextColor = Color.White
                }
                isSelected && isCorrect == false -> {
                    borderColor = TecError
                    backgroundColor = TecErrorContainer.copy(alpha = 0.5f)
                    badgeColor = TecError
                    badgeTextColor = Color.White
                }
                else -> {
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    backgroundColor = MaterialTheme.colorScheme.surface
                    badgeColor = MaterialTheme.colorScheme.surfaceVariant
                    badgeTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                }
            }
        }
        isSelected -> {
            borderColor = TecAccent
            backgroundColor = TecAccent.copy(alpha = 0.08f)
            badgeColor = TecAccent
            badgeTextColor = Color.White
        }
        else -> {
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            backgroundColor = MaterialTheme.colorScheme.surface
            badgeColor = MaterialTheme.colorScheme.surfaceVariant
            badgeTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    val itemAlpha = if (isStrikethrough && !isAnswered) 0.35f else 1f

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(if (isSelected || (isAnswered && isCorrectAlternative)) 2.dp else 1.dp, borderColor),
        onClick = { if (!isAnswered) onSelect() },
        enabled = !isAnswered,
        modifier = modifier
            .fillMaxWidth()
            .alpha(itemAlpha)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Letter Circle Badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                if (isAnswered && isCorrectAlternative) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Correta",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (isAnswered && isSelected && isCorrect == false) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Incorreta",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = letter,
                        color = badgeTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Alternative Content with Full-Area Clickability
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp)
            ) {
                HtmlText(
                    html = htmlContent,
                    onImageClick = onImageClick
                )

                // Transparent overlay on top of WebView to guarantee click interception across the entire text area
                if (!isAnswered) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onSelect()
                            }
                    )
                }
            }

            // Strikethrough / Discard Button (only if not answered)
            if (!isAnswered) {
                IconButton(
                    onClick = onToggleStrikethrough,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FormatStrikethrough,
                        contentDescription = "Riscar alternativa",
                        tint = if (isStrikethrough) TecError else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
