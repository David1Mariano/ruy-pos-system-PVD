package br.com.ruy.pos.ui.screens.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.data.local.OrderEntity
import br.com.ruy.pos.ui.components.RuyCard
import br.com.ruy.pos.ui.components.RuyHeader
import br.com.ruy.pos.ui.components.RuyPrimaryButton
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OrdersScreen(repo: Repository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var orders by remember { mutableStateOf<List<OrderEntity>>(emptyList()) }
    var selected by remember { mutableStateOf<OrderEntity?>(null) }
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    fun reais(c: Int): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(c / 100.0)

    LaunchedEffect(Unit) {
        orders = repo.allOrders()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        RuyHeader(title = "Histórico de Pedidos")
        if (selected == null) {
            Column(modifier = Modifier.padding(12.dp)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(orders) { o ->
                        RuyCard {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Pedido ${o.id.take(8)}", fontWeight = FontWeight.SemiBold)
                                    Text(text = reais(o.totalCents))
                                }
                                Text(text = sdf.format(Date(o.createdAt)), style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Pagamento: ${o.paymentStatus} | Sync: ${o.syncStatus}", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(onClick = { selected = o }) { Text(text = "Ver detalhes") }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                RuyPrimaryButton(text = "Voltar", onClick = onBack)
            }
        } else {
            val o = selected!!
            Column(modifier = Modifier.padding(12.dp)) {
                RuyCard {
                    Text(text = "Pedido ${o.id}", fontWeight = FontWeight.SemiBold)
                    Text(text = sdf.format(Date(o.createdAt)))
                    Text(text = "Total: ${reais(o.totalCents)}")
                    Text(text = "Pagamento: ${o.paymentStatus} | Sync: ${o.syncStatus}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Itens", fontWeight = FontWeight.SemiBold)
                    val itemsArr = try { JSONArray(o.itemsJson) } catch (e: Throwable) { null }
                    if (itemsArr != null) {
                        for (i in 0 until itemsArr.length()) {
                            val it = itemsArr.getJSONObject(i)
                            val name = it.optString("name")
                            val qty = it.optInt("qty")
                            val uc = it.optInt("unitCents")
                            Text(text = "- $name x $qty (${reais(uc * qty)})")
                        }
                    } else {
                        Text(text = o.itemsJson)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                RuyPrimaryButton(text = "Voltar à lista", onClick = { selected = null })
                RuyPrimaryButton(text = "Voltar ao menu", onClick = onBack)
            }
        }
    }
}
