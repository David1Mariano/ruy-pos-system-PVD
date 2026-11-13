package br.com.ruy.pos.ui.screens.settings

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.ui.components.RuyHeader

@Composable fun SettingsScreen(onBack: () -> Unit, onSupport: () -> Unit) {
    val context = LocalContext.current
    val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    val unlock = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) onSupport()
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RuyHeader(title = "Configurações", subtitle = "Preferências do estabelecimento")
        OutlinedButton(enabled = keyguard.isDeviceSecure, onClick = {
            keyguard.createConfirmDeviceCredentialIntent("Suporte Ruy POS", "Confirme o bloqueio do aparelho para acessar o diagnóstico")?.let(unlock::launch)
        }) { Text("Suporte • acesso protegido") }
        if (!keyguard.isDeviceSecure) Text("Configure o bloqueio de tela do Android para acessar o suporte.")
        OutlinedButton(onClick = onBack) { Text("Voltar") }
    }
}
