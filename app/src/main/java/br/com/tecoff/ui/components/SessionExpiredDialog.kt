package br.com.tecoff.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import br.com.tecoff.ui.theme.TecPrimary
import br.com.tecoff.ui.theme.TecWarning

@Composable
fun SessionExpiredDialog(
    onLoginClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Aviso de sessão",
                tint = TecWarning
            )
        },
        title = {
            Text(text = "Sessão Expirada")
        },
        text = {
            Text(
                text = "Sua sessão no TEC Concursos expirou ou foi invalidada. " +
                        "Para sincronizar resoluções ou baixar novos cadernos, faça login novamente. " +
                        "Você pode continuar resolvendo os cadernos já baixados offline normalmente."
            )
        },
        confirmButton = {
            Button(
                onClick = onLoginClick,
                colors = ButtonDefaults.buttonColors(containerColor = TecPrimary)
            ) {
                Text("Fazer Login")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Continuar Offline")
            }
        }
    )
}
