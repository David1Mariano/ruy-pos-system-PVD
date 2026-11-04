package br.com.ruy.pos.ui.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.data.local.OrderEntity
import br.com.ruy.pos.ui.components.RuyCard
import br.com.ruy.pos.ui.components.RuyHeader
import br.com.ruy.pos.ui.components.RuyPrimaryButton
import br.com.ruy.pos.ui.theme.RuyBackground
import br.com.ruy.pos.ui.theme.RuyBorder
import br.com.ruy.pos.ui.theme.RuyTextPrimary
import br.com.ruy.pos.ui.theme.RuyTextSecondary
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OrdersScreen(repo: Repository, onBack: () -> Unit, onResumeOrder: (OrderEntity) -> Unit = {}) {
    val orders by remember { repo.observeOrders() }.collectAsState(initial = emptyList())
    var selected by remember { mutableStateOf<OrderEntity?>(null) }
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    fun reais(c: Int): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(c / 100.0)


    Column(modifier = Modifier.fillMaxSize().background(RuyBackground)) {
        RuyHeader(title = "Pedidos", subtitle = "Histórico")
        if (selected == null) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (orders.isEmpty()) {
                    RuyCard {
                        Text("Nenhum pedido registrado", style = MaterialTheme.typography.bodyMedium, color = RuyTextSecondary)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        items(orders) { o ->
                            Card(onClick = { selected = o }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(1.dp, RuyBorder)) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Pedido ${o.id.take(8).uppercase()}", style = MaterialTheme.typography.titleSmall)
                                        Text(reais(o.totalCents), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(sdf.format(Date(o.createdAt)), style = MaterialTheme.typography.bodySmall, color = RuyTextSecondary)
                                    Text("Pagamento: ${o.paymentStatus} • Sync: ${o.syncStatus}", style = MaterialTheme.typography.bodySmall, color = RuyTextSecondary)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                RuyPrimaryButton(text = "Voltar", onClick = onBack)
            }
        } else {
            val o = orders.firstOrNull { it.id == selected!!.id } ?: selected!!
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                RuyCard {
                    Text("Pedido ${o.id.take(8).uppercase()}", style = MaterialTheme.typography.titleSmall)
                    Text(sdf.format(Date(o.createdAt)), style = MaterialTheme.typography.bodySmall, color = RuyTextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Total: ${reais(o.totalCents)}", style = MaterialTheme.typography.titleSmall)
                    Text("Pagamento: ${o.paymentStatus} • Sync: ${o.syncStatus}", style = MaterialTheme.typography.bodySmall, color = RuyTextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Itens", style = MaterialTheme.typography.titleSmall)
                    val itemsArr = try { JSONArray(o.itemsJson) } catch (e: Throwable) { null }
                    if (itemsArr != null) {
                        for (i in 0 until itemsArr.length()) {
                            val it = itemsArr.getJSONObject(i)
                            val name = it.optString("name")
                            val qty = it.optInt("qty")
                            val uc = it.optInt("unitCents")
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("$name x$qty", style = MaterialTheme.typography.bodyMedium)
                                Text(reais(uc * qty), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                        Text(o.itemsJson, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (o.paymentStatus != "PAGO") {
                    RuyPrimaryButton(text = "Retomar checkout / conciliação", onClick = { onResumeOrder(o) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
                RuyPrimaryButton(text = "Voltar à lista", onClick = { selected = null })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) { Text("Voltar ao menu") }
            }
        }
    }
}

