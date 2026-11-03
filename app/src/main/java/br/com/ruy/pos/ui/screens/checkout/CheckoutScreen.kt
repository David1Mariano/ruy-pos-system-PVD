package br.com.ruy.pos.ui.screens.checkout

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.payment.StonePaymentGateway
import br.com.ruy.pos.ui.components.RuyCard
import br.com.ruy.pos.ui.components.RuyHeader
import br.com.ruy.pos.ui.components.RuyPrimaryButton
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CheckoutScreen(
    repo: Repository,
    totalCents: Int,
    items: List<Pair<String, Int>>,
    unitPrice: Map<String, Int>,
    comandaId: String? = null,
    tableId: String? = null,
    onBack: () -> Unit,
    onPaidAndClosed: () -> Unit = onBack
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    fun reais(c: Int): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(c / 100.0)
    var method by remember { mutableStateOf("DINHEIRO") }
    var received by remember { mutableStateOf("") }
    var installments by remember { mutableStateOf("1") }
    var processing by remember { mutableStateOf(false) }
    val receivedCents = received.toIntOrNull() ?: 0
    val change = if (method == "DINHEIRO" && receivedCents >= totalCents) receivedCents - totalCents else 0
    val stone = remember { StonePaymentGateway() }

    Column(modifier = Modifier.fillMaxSize()) {
        RuyHeader(title = "Finalizar Venda")
        Column(modifier = Modifier.padding(12.dp)) {
            RuyCard {
                Text(text = "Itens", fontWeight = FontWeight.SemiBold)
                items.forEach { (name, qty) ->
                    val up = unitPrice[name] ?: 0
                    Text(text = "$name x $qty - ${reais(up * qty)}")
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Subtotal: ${reais(totalCents)}")
                Text(text = "Total: ${reais(totalCents)}", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            RuyCard {
                Text(text = "Forma de pagamento", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = method == "DINHEIRO", onClick = { method = "DINHEIRO" }, label = { Text("Dinheiro") })
                    FilterChip(selected = method == "PIX", onClick = { method = "PIX" }, label = { Text("Pix") })
                    FilterChip(selected = method == "DEBITO", onClick = { method = "DEBITO" }, label = { Text("Débito") })
                    FilterChip(selected = method == "CREDITO", onClick = { method = "CREDITO" }, label = { Text("Crédito") })
                }
                if (method == "DINHEIRO") {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(value = received, onValueChange = { received = it }, label = { Text("Valor recebido (centavos)") }, modifier = Modifier.fillMaxWidth())
                    Text(text = "Troco: ${reais(change)}")
                }
                if (method == "CREDITO") {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(value = installments, onValueChange = { installments = it }, label = { Text("Parcelas") }, modifier = Modifier.fillMaxWidth())
                }
                if (method == "DEBITO" || method == "CREDITO") {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Stone: ${if (stone.isAvailable()) "disponível" else "indisponível (hardware/credenciais/homologação não configurados nesta sessão)"}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (!processing) {
                RuyPrimaryButton(text = "Confirmar pagamento", onClick = {
                    if (method == "DINHEIRO" && receivedCents < totalCents) {
                        Toast.makeText(context, "Valor recebido insuficiente", Toast.LENGTH_LONG).show()
                        return@RuyPrimaryButton
                    }
                    if ((method == "DEBITO" || method == "CREDITO") && !stone.isAvailable()) {
                        scope.launch {
                            val orderId = repo.saveOrder(totalCents, items, unitPrice, null, null)
                            repo.savePayment(orderId, totalCents, method, "NAO_INICIADO", null, null, installments.toIntOrNull() ?: 1, null)
                            Toast.makeText(context, "Pagamento não iniciado - gateway indisponível. Mesa não será liberada.", Toast.LENGTH_LONG).show()
                        }
                        return@RuyPrimaryButton
                    }
                    processing = true
                    scope.launch {
                        try {
                            val orderId = repo.saveOrder(totalCents, items, unitPrice, null, null)
                            if (method == "DINHEIRO") {
                                repo.savePayment(orderId, totalCents, method, "PAGO", receivedCents, change, 1, null)
                                Toast.makeText(context, "Pagamento em dinheiro registrado. Troco: ${reais(change)}", Toast.LENGTH_LONG).show()
                                if (comandaId != null && tableId != null) {
                                    repo.markTableFree(tableId)
                                    onPaidAndClosed()
                                } else {
                                    onBack()
                                }
                            } else if (method == "PIX") {
                                repo.savePayment(orderId, totalCents, method, "AGUARDANDO", null, null, 1, null)
                                Toast.makeText(context, "Pagamento Pix registrado como aguardando confirmação. Mesa não liberada.", Toast.LENGTH_LONG).show()
                                onBack()
                            } else {
                                repo.savePayment(orderId, totalCents, method, "NAO_INICIADO", null, null, installments.toIntOrNull() ?: 1, null)
                                Toast.makeText(context, "Pagamento eletrônico não iniciado - Stone indisponível. Mesa não liberada.", Toast.LENGTH_LONG).show()
                                onBack()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            processing = false
                        }
                    }
                })
            } else {
                RuyPrimaryButton(text = "Processando...", onClick = {})
            }
            RuyPrimaryButton(text = "Voltar", onClick = onBack)
        }
    }
}
