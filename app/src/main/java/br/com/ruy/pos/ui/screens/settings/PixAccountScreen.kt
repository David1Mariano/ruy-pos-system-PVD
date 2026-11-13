package br.com.ruy.pos.ui.screens.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.BuildConfig
import br.com.ruy.pos.payment.pix.*
import br.com.ruy.pos.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun PixAccountScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var store by remember { mutableStateOf<PixSettingsStore?>(null) }
    var institution by remember { mutableStateOf("") }
    var receiver by remember { mutableStateOf("") }
    var taxId by remember { mutableStateOf("") }
    var pixKey by remember { mutableStateOf("") }
    var keyType by remember { mutableStateOf(PixKeyType.EMAIL) }
    var label by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var registered by remember { mutableStateOf(false) }
    var operator by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(true) }
    var confirm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Conta não configurada") }
    DisposableEffect(context) {
        val window = (context as? android.app.Activity)?.window
        val wasSecure = window?.attributes?.flags?.and(android.view.WindowManager.LayoutParams.FLAG_SECURE) != 0
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (!wasSecure) window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
    }
    LaunchedEffect(Unit) {
        try {
            val result = withContext(Dispatchers.IO) { val settings = PixSettingsStore(context); settings to settings.load() }
            store = result.first
            result.second?.let { a -> institution = a.institution; receiver = a.receiver; taxId = a.taxId; pixKey = a.key; keyType = a.keyType; label = a.label; city = a.city; message = "Configurada • recebimentos conferidos manualmente" }
        } catch (e: Exception) { message = "Não foi possível abrir as configurações protegidas. Procure o suporte."; SupportDiagnostics.record(e) }
        finally { busy = false }
    }
    if (confirm) AlertDialog(onDismissRequest = { if (!busy) confirm = false }, title = { Text("Confirmar conta de recebimento") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$institution • $receiver • $city")
            Text("${keyType.label}: $pixKey")
            Text("As próximas vendas Pix usarão esta chave. Confira os dados diretamente no banco. Esta alteração será registrada com o responsável.")
        }
    }, confirmButton = { TextButton(enabled = !busy, onClick = {
        busy = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) { requireNotNull(store).save(PixAccount(institution, receiver, taxId, pixKey, keyType, label, city, registered), operator, true) }
                message = "Conta salva • confirmação de Pix continua manual"; confirm = false; registered = false
            } catch (e: IllegalArgumentException) { message = e.message ?: "Confira os dados da conta"; confirm = false }
            catch (e: Exception) { message = "Não foi possível salvar a conta. Confira as configurações."; SupportDiagnostics.record(e); confirm = false }
            finally { busy = false }
        }
    }) { Text("Confirmar e salvar") } }, dismissButton = { TextButton(enabled = !busy, onClick = { confirm = false }) { Text("Revisar") } })
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RuyHeader(title = "Conta de recebimento Pix", subtitle = "Pagamentos")
        if (BuildConfig.HOMOLOGATION) Text("HOMOLOGAÇÃO: use somente e-mail fictício com final .invalid. O QR bancário fica bloqueado.")
        Text(message)
        OutlinedTextField(institution, { institution = it }, label = { Text("Instituição financeira") }, enabled = !busy)
        OutlinedTextField(label, { label = it }, label = { Text("Nome da conta") }, enabled = !busy)
        OutlinedTextField(receiver, { receiver = it }, label = { Text("Nome do recebedor • até 25 caracteres") }, enabled = !busy)
        OutlinedTextField(taxId, { taxId = it }, label = { Text("CPF/CNPJ do recebedor • opcional") }, enabled = !busy)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PixKeyType.values().forEach { type -> FilterChip(keyType == type, { keyType = type; registered = false }, label = { Text(type.label) }, enabled = !busy) }
        }
        OutlinedTextField(pixKey, { pixKey = it; registered = false }, label = { Text(if (keyType == PixKeyType.PHONE) "Celular com +55 e DDD" else "Chave Pix") }, enabled = !busy)
        OutlinedTextField(city, { city = it }, label = { Text("Cidade do recebedor • até 15 caracteres") }, enabled = !busy)
        Text("A chave deve estar cadastrada e vinculada à conta do recebedor no banco. O aplicativo valida o formato, sem consultar titularidade ou saldo.")
        Row { Checkbox(registered, { registered = it }, enabled = !busy); Text("Conferi a chave e o titular diretamente no banco") }
        OutlinedTextField(operator, { operator = it }, label = { Text("Responsável pela configuração") }, enabled = !busy)
        Text("Não informe senha bancária, token, cartão ou PIN. Pix por API está indisponível até contratação e homologação do banco.")
        RuyPrimaryButton(text = "Salvar conta", enabled = !busy && store != null && registered && operator.trim().length >= 3, onClick = { confirm = true })
        OutlinedButton(onClick = onBack, enabled = !busy) { Text("Voltar") }
    }
}
