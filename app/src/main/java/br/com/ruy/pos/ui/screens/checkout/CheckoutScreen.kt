package br.com.ruy.pos.ui.screens.checkout

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.payment.StonePaymentGateway
import br.com.ruy.pos.ui.components.RuyCard
import br.com.ruy.pos.ui.components.RuyHeader
import br.com.ruy.pos.ui.components.RuyPriceText
import br.com.ruy.pos.ui.components.RuyPrimaryButton
import br.com.ruy.pos.ui.theme.RuyBackground
import br.com.ruy.pos.ui.theme.RuyBorder
import br.com.ruy.pos.ui.theme.RuyRed
import br.com.ruy.pos.ui.theme.RuyTextPrimary
import br.com.ruy.pos.ui.theme.RuyTextSecondary
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

    Column(modifier = Modifier.fillMaxSize().background(RuyBackground)) {
        RuyHeader(title = "Finalizar venda", subtitle = if (tableId != null) "Mesa ${tableId.takeLast(2)}" else "Balcão")
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            RuyCard {
                Text("Resumo do pedido", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                items.forEach { (name, qty) ->
                    val up = unitPrice[name] ?: 0
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$name x$qty", style = MaterialTheme.typography.bodyMedium)
                        Text(reais(up * qty), style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = RuyBorder)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total a pagar", style = MaterialTheme.typography.titleSmall)
                    Text(reais(totalCents), style = MaterialTheme.typography.titleMedium, color = RuyRed, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            RuyCard {
                Text("Forma de pagamento", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = method == "DINHEIRO", onClick = { method = "DINHEIRO" }, label = { Text("Dinheiro") })
                    FilterChip(selected = method == "PIX", onClick = { method = "PIX" }, label = { Text("Pix") })
                    FilterChip(selected = method == "DEBITO", onClick = { method = "DEBITO" }, label = { Text("Débito") })
                    FilterChip(selected = method == "CREDITO", onClick = { method = "CREDITO" }, label = { Text("Crédito") })
                }
                if (method == "DINHEIRO") {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(value = received, onValueChange = { received = it }, label = { Text("Valor recebido (centavos)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Troco", style = MaterialTheme.typography.bodyMedium)
                        Text(reais(change), style = MaterialTheme.typography.titleSmall, color = RuyRed)
                    }
                }
                if (method == "CREDITO") {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(value = installments, onValueChange = { installments = it }, label = { Text("Parcelas") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
                if (method == "DEBITO" || method == "CREDITO") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Stone: ${if (stone.isAvailable()) "disponível" else "indisponível nesta sessão"}", style = MaterialTheme.typography.bodySmall, color = RuyTextSecondary)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
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
                                Toast.makeText(context, "Pagamento em dinheiro confirmado. Troco: ${reais(change)}", Toast.LENGTH_LONG).show()
                                if (comandaId != null && tableId != null) {
                                    repo.markTableFree(tableId)
                                    onPaidAndClosed()
                                } else {
                                    onBack()
                                }
                            } else if (method == "PIX") {
                                repo.savePayment(orderId, totalCents, method, "AGUARDANDO", null, null, 1, null)
                                Toast.makeText(context, "Pagamento Pix registrado como aguardando confirmação.", Toast.LENGTH_LONG).show()
                                onBack()
                            } else {
                                repo.savePayment(orderId, totalCents, method, "NAO_INICIADO", null, null, installments.toIntOrNull() ?: 1, null)
                                Toast.makeText(context, "Pagamento eletrônico não iniciado. Mesa não liberada.", Toast.LENGTH_LONG).show()
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
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) { Text("Voltar") }
        }
    }
}

