package br.com.ruy.pos.ui.screens.checkout

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.payment.*
import br.com.ruy.pos.ui.components.*
import br.com.ruy.pos.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import org.json.JSONArray

@Composable
fun CheckoutScreen(
    repo: Repository,
    totalCents: Int,
    items: List<Pair<String, Int>>,
    unitPrice: Map<String, Int>,
    comandaId: String? = null,
    tableId: String? = null,
    existingOrderId: String? = null,
    onBack: () -> Unit,
    onPaidAndClosed: () -> Unit = onBack
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    fun reais(c: Int) = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(c / 100.0)
    fun notify(message: String) { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
    var orderId by rememberSaveable { mutableStateOf(existingOrderId) }
    var method by remember { mutableStateOf("DINHEIRO") }
    var received by remember { mutableStateOf("") }
    var installments by remember { mutableStateOf("1") }
    var installmentType by remember { mutableStateOf("NONE") }
    var amount by remember { mutableStateOf("") }
    var processing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var setup by remember { mutableStateOf(false) }
    var reconciliation by remember { mutableStateOf(false) }
    var configuration by remember { mutableStateOf(StoneConfiguration.read(context)) }
    val stone = remember(configuration) { StonePaymentGateway(context, configuration) }
    val active by remember { repo.payments.observeActive() }.collectAsState(initial = null)

    LaunchedEffect(Unit) {
        try { if (orderId == null) orderId = repo.prepareCheckout(totalCents, items, unitPrice, comandaId) }
        catch (e: Exception) { error = e.message ?: "Não foi possível salvar o pedido" }
    }
    val order by remember(orderId) { repo.payments.observeOrder(orderId.orEmpty()) }.collectAsState(initial = null)
    val payments by remember(orderId) { repo.payments.observePayments(orderId.orEmpty()) }.collectAsState(initial = emptyList())
    val attempts by remember(orderId) { repo.payments.observeAttempts(orderId.orEmpty()) }.collectAsState(initial = emptyList())
    val paid = payments.filter { it.status == "PAGO" }.sumOf { it.amountCents }
    val remaining = ((order?.totalCents ?: totalCents) - paid).coerceAtLeast(0)
    val amountCents = amount.toIntOrNull() ?: 0
    val receivedCents = received.toIntOrNull() ?: 0
    val change = if (method == "DINHEIRO" && receivedCents >= amountCents) receivedCents - amountCents else 0
    val attempt = attempts.firstOrNull { it.activeSlot == 1 }
    val payment = payments.firstOrNull { it.paymentId == attempt?.paymentId }
    val legacy = payments.firstOrNull { p -> p.status in setOf("AGUARDANDO", "INDETERMINADO", "EM_CONCILIACAO", "CRIADA") && attempts.none { it.paymentId == p.paymentId } }
    val summary = remember(order?.itemsJson, items, unitPrice) {
        order?.let { saved ->
            val arr = JSONArray(saved.itemsJson)
            (0 until arr.length()).map { arr.getJSONObject(it) }.map { Triple(it.getString("name"), it.getInt("qty"), it.getInt("unitCents")) }
        } ?: items.map { (name, qty) -> Triple(name, qty, unitPrice[name] ?: 0) }
    }
    LaunchedEffect(remaining) { amount = remaining.toString() }
    LaunchedEffect(order?.paymentStatus, active) {
        if (order?.paymentStatus == "PAGO" && active == null) onPaidAndClosed()
    }

    if (setup) StoneSetupDialog(configuration, onDismiss = { setup = false }, onSaved = { configuration = it; setup = false })
    if (reconciliation && attempt != null && payment != null) StoneReconciliationDialog(attempt, payment, processing,
        error = error, onDismiss = { reconciliation = false; error = null }, onConfirm = { input ->
            if (!processing) {
                processing = true
                scope.launch {
                    try {
                        repo.payments.reconcile(input)
                        reconciliation = false
                        error = null
                        notify(if (input.received) "Recebimento conciliado e registrado no Caixa" else "Ausência de recebimento conferida; tentativa encerrada")
                    } catch (e: Exception) { error = e.message ?: "Falha na conciliação" }
                    finally { processing = false }
                }
            }
        })

    Column(Modifier.fillMaxSize().background(RuyBackground)) {
        RuyHeader(title = "Finalizar venda", subtitle = if (tableId != null) "Mesa ${tableId.takeLast(2)}" else "Balcão")
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RuyCard {
                Text("Resumo do pedido", style = MaterialTheme.typography.titleSmall)
                summary.forEach { (name, qty, price) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$name x$qty", style = MaterialTheme.typography.bodyMedium)
                        Text(reais(price * qty), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = RuyBorder)
                Text("Total: ${reais(order?.totalCents ?: totalCents)} • Recebido: ${reais(paid)}")
                Text("Saldo a pagar: ${reais(remaining)}", color = RuyRed, fontWeight = FontWeight.SemiBold)
                orderId?.let { Text("Pedido ${it.take(8)}", style = MaterialTheme.typography.bodySmall) }
            }
            RuyCard {
                Text("Forma de pagamento", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("DINHEIRO" to "Dinheiro", "PIX" to "Pix", "DEBITO" to "Débito", "CREDITO" to "Crédito").forEach { (value, label) ->
                        FilterChip(selected = method == value, enabled = !processing && active == null,
                            onClick = { method = value }, label = { Text(label) })
                    }
                }
                OutlinedTextField(amount, { amount = it }, enabled = !processing && active == null,
                    label = { Text("Valor desta cobrança (centavos)") }, modifier = Modifier.fillMaxWidth())
                if (method == "DINHEIRO") {
                    OutlinedTextField(received, { received = it }, enabled = !processing && active == null,
                        label = { Text("Valor recebido (centavos)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Text("Troco: ${reais(change)}")
                }
                if (method == "CREDITO") {
                    OutlinedTextField(installments, { installments = it }, enabled = !processing && active == null,
                        label = { Text("Parcelas (1 a 99)") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("NONE", "MERCHANT", "ISSUER").forEach { value ->
                            FilterChip(selected = installmentType == value,
                                enabled = !processing && active == null && (value == "NONE" ||
                                    value == "MERCHANT" && configuration.merchantSupported || value == "ISSUER" && configuration.issuerSupported),
                                onClick = { installmentType = value }, label = { Text(value) })
                        }
                    }
                    Text("NONE: à vista. Parcelamento conforme contrato e opções do terminal; sem cálculo de taxas.", style = MaterialTheme.typography.bodySmall)
                }
                if (method != "DINHEIRO") {
                    Text(stone.unavailableReason() ?: "Aplicativo resolvido; pagamento sujeito ao suporte do terminal", style = MaterialTheme.typography.bodySmall)
                    TextButton(enabled = !processing && active == null, onClick = { setup = true }) { Text("Conferir Ajustes Stone") }
                    if (method == "PIX") Text("Pix depende do equipamento. QR exibido não confirma recebimento.", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (attempt != null) RuyCard {
                Text("Stone ${attempt.state} • referência ${attempt.stoneOrderId}", style = MaterialTheme.typography.titleSmall)
                Text(attempt.detail ?: "Aguardando retorno. Não inicie outra cobrança.")
                Text("ATK informado: ${attempt.atk ?: "não informado"}", style = MaterialTheme.typography.bodySmall)
                TextButton(enabled = !processing, onClick = { error = null; reconciliation = true }) { Text("Conferir comprovante/extrato e conciliar") }
            }
            if (active != null && attempt == null) Text("Existe pagamento pendente em outro pedido. Retome-o antes de cobrar novamente.")
            if (legacy != null && active == null) TextButton(enabled = !processing, onClick = {
                processing = true
                scope.launch {
                    try { repo.payments.recoverLegacyPayment(legacy.paymentId) }
                    catch (e: Exception) { error = e.message }
                    finally { processing = false }
                }
            }) { Text("Recuperar pagamento anterior para conciliação") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            RuyPrimaryButton(text = if (processing) "Processando…" else "Confirmar pagamento",
                enabled = !processing && order != null && active == null && remaining > 0,
                onClick = {
                    if (processing) return@RuyPrimaryButton
                    processing = true
                    error = null
                    scope.launch {
                        try {
                            val id = requireNotNull(orderId) { "Pedido ainda não salvo" }
                            require(amountCents in 1..remaining) { "Valor deve ser positivo e não exceder o saldo" }
                            if (method == "DINHEIRO") {
                                repo.payments.confirmCash(id, amountCents, receivedCents)
                                notify("Dinheiro registrado. Troco: ${reais(change)}")
                            } else {
                                check(stone.isAvailable()) { stone.unavailableReason().orEmpty() }
                                val count = if (method == "CREDITO") requireNotNull(installments.toIntOrNull()) { "Parcelas inválidas" } else 1
                                val type = if (method == "CREDITO") installmentType else "NONE"
                                stone.buildIntent(PaymentRequest(id, amountCents, method, count, "1", type))
                                val created = repo.payments.createAttempt(id, amountCents, method, count, type)
                                val request = repo.payments.request(created)
                                if (repo.payments.claimLaunch(created.stoneOrderId)) {
                                    val result = stone.process(request)
                                    if (result.status != "AGUARDANDO") repo.payments.launchFailed(created.stoneOrderId, result.status == "NAO_INICIADO")
                                    notify(result.message ?: "Retome o pagamento para conciliar")
                                }
                            }
                        } catch (e: Exception) { error = e.message ?: "Falha no pagamento; confira as tentativas persistidas" }
                        finally { processing = false }
                    }
                })
            OutlinedButton(onClick = onBack, enabled = !processing, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) { Text("Voltar") }
        }
    }
}
