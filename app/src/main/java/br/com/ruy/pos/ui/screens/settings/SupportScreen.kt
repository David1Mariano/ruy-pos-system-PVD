package br.com.ruy.pos.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.BuildConfig
import br.com.ruy.pos.ui.components.*

@Composable fun SupportScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RuyHeader(title = "Suporte", subtitle = "Diagnóstico autorizado no aparelho")
        Text("Versão ${BuildConfig.VERSION_NAME} • ${BuildConfig.VERSION_CODE}")
        Text("Android ${android.os.Build.VERSION.RELEASE} • ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        Text("${BuildConfig.APPLICATION_ID} • ${if (BuildConfig.HOMOLOGATION) "Homologação" else "Comercial"}")
        Text("Registros de diagnóstico desta execução")
        SupportDiagnostics.recent().forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        Text("O diagnóstico não contém dados bancários nem detalhes de cartão.")
        OutlinedButton(onClick = onBack) { Text("Voltar") }
    }
}
