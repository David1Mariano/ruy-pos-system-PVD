package br.com.ruy.pos.ui.screens.checkout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.data.local.PaymentEntity
import br.com.ruy.pos.data.local.StoneAttemptEntity
import br.com.ruy.pos.payment.PaymentCoordinator
import br.com.ruy.pos.payment.StoneConfiguration

@Composable
private fun CheckLine(text: String, checked: Boolean, onCheck: (Boolean) -> Unit) {
    Row { Checkbox(checked = checked, onCheckedChange = onCheck); Text(text, modifier = Modifier.padding(top = 12.dp)) }
}

@Composable
fun StoneSetupDialog(initial: StoneConfiguration, onDismiss: () -> Unit, onSaved: (StoneConfiguration) -> Unit) {
    val context = LocalContext.current
    var configuration by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Configuração do terminal Stone") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Confira estas opções no equipamento. Esta confirmação do operador não representa homologação Stone.")
            CheckLine("Estou em um POS Android fornecido pela Stone, com o aplicativo Pagamento oficial", configuration.terminalVerified) { configuration = configuration.copy(terminalVerified = it) }
            CheckLine("Habilitei ID do pedido no aplicativo Ajustes Stone", configuration.orderIdEnabled) { configuration = configuration.copy(orderIdEnabled = it) }
            CheckLine("Pix está habilitado e seu fluxo/QR foi verificado neste terminal", configuration.pixSupported) { configuration = configuration.copy(pixSupported = it) }
            CheckLine("MERCHANT (sem juros) está suportado e acordado", configuration.merchantSupported) { configuration = configuration.copy(merchantSupported = it) }
            CheckLine("ISSUER (com juros) está suportado e acordado", configuration.issuerSupported) { configuration = configuration.copy(issuerSupported = it) }
            Text("Nenhuma taxa é calculada pelo Ruy POS. A configuração é vinculada a este modelo/firmware; confira novamente após alterações nos Ajustes.")
        }
    }, confirmButton = { TextButton(onClick = { configuration.save(context); onSaved(configuration) }) { Text("Salvar conferência") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Voltar") } })
}

@Composable
fun StoneReconciliationDialog(attempt: StoneAttemptEntity, payment: PaymentEntity, busy: Boolean,
                              error: String?, onDismiss: () -> Unit, onConfirm: (PaymentCoordinator.Reconciliation) -> Unit) {
    var operator by remember { mutableStateOf("") }
    var evidence by remember { mutableStateOf("") }
    // Deliberately blank: re-enter independent receipt data, do not merely echo the deeplink.
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("") }
    var installments by remember { mutableStateOf("") }
    var atk by remember { mutableStateOf("") }
    var authorization by remember { mutableStateOf("") }
    var received by remember { mutableStateOf(true) }
    var checked by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Conciliar pagamento Stone") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Pedido ${attempt.orderId.take(8).uppercase()}")
            if (attempt.origin == "LEGACY_MANUAL") Text("Registro de versão anterior. Se confirmado, será regularizado no caixa atualmente aberto. Confira o pedido pelo comprovante; vínculos antigos de mesa não serão presumidos.")
            Text("${br.com.ruy.pos.ui.components.Money.format(payment.amountCents.toLong())} • ${br.com.ruy.pos.ui.components.OperationalText.method(payment.method)} • ${payment.installments} parcela(s)")
            Text(br.com.ruy.pos.ui.components.OperationalText.status(attempt.state))
            Text("Abra o histórico/extrato na Stone e confira pedido, valor, modalidade, parcelas e referência do comprovante. QR exibido ou aplicativo encerrado não comprovam recebimento.")
            Row {
                FilterChip(selected = received, enabled = !busy, onClick = { received = true; checked = false }, label = { Text("Recebido") })
                Spacer(Modifier.width(4.dp))
                FilterChip(selected = !received, enabled = !busy, onClick = { received = false; checked = false }, label = { Text("Não recebido") })
            }
            OutlinedTextField(amount, { amount = it }, label = { Text("Valor conferido (R$)") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("PIX" to "Pix", "DEBIT" to "Débito", "CREDIT" to "Crédito").forEach { (value, label) ->
                    FilterChip(method == value, { method = value }, label = { Text(label) }, enabled = !busy)
                }
            }
            OutlinedTextField(installments, { installments = it }, label = { Text("Parcelas conferidas (1 à vista)") })
            if (received) {
                OutlinedTextField(atk, { atk = it }, label = { Text("Referência do comprovante Stone") })
                OutlinedTextField(authorization, { authorization = it }, label = { Text("Autorização (cartão)") })
            }
            OutlinedTextField(operator, { operator = it }, label = { Text("Operador responsável") })
            OutlinedTextField(evidence, { evidence = it }, label = { Text("Comprovante/extrato consultado (data/hora e identificador)") })
            CheckLine(if (received) "Conferi no comprovante/extrato Stone que este valor foi recebido para este pedido"
                else "Conferi o extrato Stone: sem recebimento e sem transação em andamento; é seguro tentar novamente", checked) { checked = it }
            Text("Conciliação manual registrada com o operador. Não é consulta automática à Stone.")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(enabled = !busy && checked,
        onClick = { onConfirm(PaymentCoordinator.Reconciliation(attempt.stoneOrderId, br.com.ruy.pos.ui.components.Money.cents(amount) ?: -1,
            method, installments.toIntOrNull() ?: -1, received, operator, evidence, checked, atk, authorization)) }) {
        Text(if (busy) "Salvando…" else "Registrar conciliação")
    } }, dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Voltar") } })
}
