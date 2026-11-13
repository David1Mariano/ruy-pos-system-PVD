package br.com.ruy.pos.ui.screens.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.backup.BackupManager
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.ui.components.RuyCard
import br.com.ruy.pos.ui.components.RuyHeader
import br.com.ruy.pos.ui.components.RuyPrimaryButton
import br.com.ruy.pos.ui.theme.RuyBackground
import br.com.ruy.pos.ui.theme.RuyTextSecondary
import kotlinx.coroutines.launch

const val REQUEST_BACKUP_RESTORE = 1002

@Composable
fun BackupScreen(repo: Repository, onBack: () -> Unit, onRestoreClick: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showConfirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val backupFile = remember { BackupManager.backupFile(context) }

    LaunchedEffect(backupFile.exists()) { message = if (backupFile.exists()) "Backup disponível em cache" else "Sem backup em cache" }

    if (showConfirm) {
        AlertDialog(onDismissRequest = { if (!busy) showConfirm = false }, title = { Text("Confirmar restauração") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("A restauração substitui o banco de dados e as fotos atuais. Os dados atuais serão perdidos.")
                Text("Recomendação: exporte um backup atual antes de restaurar.", style = MaterialTheme.typography.bodySmall, color = RuyTextSecondary)
                Text("Se o ZIP estiver corrompido, os dados atuais serão mantidos.", style = MaterialTheme.typography.bodySmall, color = RuyTextSecondary)
            }
        }, confirmButton = { TextButton(enabled = !busy, onClick = { onRestoreClick(); showConfirm = false }) { Text("Restaurar") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { showConfirm = false }) { Text("Cancelar") } })
    }

    Column(modifier = Modifier.fillMaxSize().background(RuyBackground)) {
        RuyHeader(title = "Backup", subtitle = "Exportar e restaurar dados locais")
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            RuyCard {
                Text("Exportar", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Salva o banco Room (com WAL), as fotos dos produtos e os cadastros em um arquivo ZIP.", style = MaterialTheme.typography.bodySmall, color = RuyTextSecondary)
                Spacer(modifier = Modifier.height(10.dp))
                RuyPrimaryButton(text = if (busy) "Exportando…" else "Exportar backup", onClick = {
                    busy = true
                    scope.launch {
                        try {
                            val target = BackupManager.backupFile(context)
                            val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { BackupManager.export(context, Uri.fromFile(target)) }
                            message = if (ok) "Backup exportado com sucesso" else "Falha na exportação"
                        } catch (e: Exception) { message = br.com.ruy.pos.ui.components.OperationalText.error(e) }
                        finally { busy = false }
                    }
                })
            }
            Spacer(modifier = Modifier.height(12.dp))
            RuyCard {
                Text("Restaurar", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Substitui o banco e as fotos atuais. Esta ação não pode ser desfeita.", style = MaterialTheme.typography.bodySmall, color = RuyTextSecondary)
                Spacer(modifier = Modifier.height(10.dp))
                RuyPrimaryButton(text = "Restaurar backup", enabled = !busy, onClick = { showConfirm = true })
                if (backupFile.exists()) TextButton(onClick = {
                    context.startActivity(Intent.createChooser(br.com.ruy.pos.report.ShareFiles.intent(context, backupFile, "application/zip"), "Compartilhar backup"))
                }) { Text("Compartilhar backup") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) { Text("Voltar") }
        }
    }
}

fun handleBackupResult(context: Context, resultCode: Int, data: Intent?): Boolean {
    if (resultCode != android.app.Activity.RESULT_OK || data?.data == null) return false
    return BackupManager.restore(context, data.data!!)
}
