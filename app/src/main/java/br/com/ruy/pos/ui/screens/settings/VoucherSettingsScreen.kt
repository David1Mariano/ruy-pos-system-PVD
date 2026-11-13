package br.com.ruy.pos.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.payment.*
import br.com.ruy.pos.ui.components.*
import kotlinx.coroutines.*

@Composable fun VoucherSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf<PaymentCapabilitiesStore?>(null) }
    var provider by remember { mutableStateOf("") }; var brand by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var operator by remember { mutableStateOf("") }; var checked by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }; var busy by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("Voucher desabilitado até conferência do credenciamento") }
    LaunchedEffect(Unit) {
        try {
            val result = withContext(Dispatchers.IO) { val s = PaymentCapabilitiesStore(context); s to s.load() }
            settings = result.first; selected = result.second.vouchers.map { it.method }.toSet()
            result.second.vouchers.firstOrNull()?.let { provider = it.provider; brand = it.brand }
        } catch (e: Exception) { message = "Não foi possível abrir as configurações protegidas."; SupportDiagnostics.record(e) }
        finally { busy = false }
    }
    if (confirm) AlertDialog(onDismissRequest = { if (!busy) confirm = false }, title = { Text("Confirmar modalidades externas") }, text = {
        Text(if (selected.isEmpty()) "Desabilitar todos os vouchers para novas vendas?" else "Habilitar ${selected.joinToString { OperationalText.method(it) }} em $provider • $brand? A conferência de credenciamento é de responsabilidade do estabelecimento.")
    }, confirmButton = { TextButton(enabled = !busy, onClick = {
        busy = true; scope.launch {
            try {
                withContext(Dispatchers.IO) { requireNotNull(settings).save(ExternalPaymentOptions(selected.map { VoucherRule(it, provider, brand) }), operator, checked) }
                message = "Configuração salva. Só registre após aprovação no provedor autorizado."; checked = false; confirm = false
            } catch (e: Exception) { message = "Confira os dados e a autorização das modalidades selecionadas."; SupportDiagnostics.record(e); confirm = false }
            finally { busy = false }
        }
    }) { Text("Confirmar alteração") } }, dismissButton = { TextButton(enabled = !busy, onClick = { confirm = false }) { Text("Revisar") } })
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RuyHeader(title = "Voucher e benefícios", subtitle = "Pagamentos externos")
        Text(message)
        Text("O suporte depende do credenciamento da empresa, da bandeira e do aplicativo ou equipamento utilizado. Não há aprovação automática no Ruy POS.")
        OutlinedTextField(provider, { provider = it; checked = false }, label = { Text("Provedor/equipamento autorizado") }, enabled = !busy)
        OutlinedTextField(brand, { brand = it; checked = false }, label = { Text("Bandeira efetivamente habilitada") }, enabled = !busy)
        ExternalPaymentOptions.VOUCHER_METHODS.forEach { method ->
            Row { Checkbox(method in selected, { selected = if (it) selected + method else selected - method; checked = false }, enabled = !busy); Text(OperationalText.method(method)) }
        }
        Text("Uma configuração se refere ao provedor e à bandeira informados. Não habilite uma modalidade por analogia com outra. Ticket no TapStone também depende de cadastro específico e compatibilidade.")
        OutlinedTextField(operator, { operator = it }, label = { Text("Responsável pela conferência") }, enabled = !busy)
        Row { Checkbox(checked, { checked = it }, enabled = !busy); Text("Conferi o credenciamento e a disponibilidade destas modalidades ou desejo desabilitá-las") }
        RuyPrimaryButton(text = "Salvar modalidades", enabled = !busy && settings != null && checked && operator.trim().length >= 3, onClick = { confirm = true })
        OutlinedButton(onClick = onBack, enabled = !busy) { Text("Voltar") }
    }
}
