package br.com.ruy.pos.ui.screens.tables

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.data.local.TableEntity
import br.com.ruy.pos.data.local.TableOrderItemEntity
import br.com.ruy.pos.ui.components.RuyCard
import br.com.ruy.pos.ui.components.RuyHeader
import br.com.ruy.pos.ui.components.RuyPrimaryButton
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TablesScreen(repo: Repository, onBack: () -> Unit, onCheckoutTable: (Int, List<Pair<String, Int>>, Map<String, Int>, String, String) -> Unit = { _, _, _, _, _ -> }) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var tables by remember { mutableStateOf<List<TableEntity>>(emptyList()) }
    var count by remember { mutableStateOf("10") }
    var selectedTable by remember { mutableStateOf<TableEntity?>(null) }
    var comandaId by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<TableOrderItemEntity>>(emptyList()) }
    var products by remember { mutableStateOf<List<br.com.ruy.pos.data.local.ProductLocalEntity>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    val unitPrice = remember { mutableStateMapOf<String, Int>() }
    fun reais(c: Int): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(c / 100.0)

    suspend fun refresh() {
        tables = repo.getTables()
        comandaId?.let { cid ->
            items = repo.getComandaItems(cid)
            total = repo.getComandaTotal(cid)
        }
    }

    LaunchedEffect(Unit) {
        repo.ensureTables(10)
        tables = repo.getTables()
        products = repo.productLocalAll().filter { it.active }
        products.forEach { unitPrice[it.name] = it.priceCents }
    }

    if (selectedTable == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            RuyHeader(title = "Mesas")
            Column(modifier = Modifier.padding(12.dp)) {
                RuyCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = count, onValueChange = { count = it }, label = { Text("Qtd mesas") }, modifier = Modifier.weight(1f))
                        Button(onClick = {
                            scope.launch {
                                val c = count.toIntOrNull() ?: 10
                                repo.ensureTables(c)
                                tables = repo.getTables()
                                Toast.makeText(context, "Mesas configuradas", Toast.LENGTH_SHORT).show()
                            }
                        }) { Text("Configurar") }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tables) { t ->
                        Card(onClick = {
                            scope.launch {
                                val cid = repo.openTable(t.tableId)
                                comandaId = cid
                                selectedTable = t
                                items = repo.getComandaItems(cid)
                                total = repo.getComandaTotal(cid)
                            }
                        }) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Mesa %02d".format(t.number), fontWeight = FontWeight.Bold)
                                Text(text = t.status)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                RuyPrimaryButton(text = "Voltar", onClick = onBack)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            RuyHeader(title = "Mesa %02d".format(selectedTable!!.number))
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "Produtos", fontWeight = FontWeight.SemiBold)
                LazyColumn(modifier = Modifier.heightIn(max = 160.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(products) { p ->
                        RuyCard {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(p.name)
                                    Text(reais(p.priceCents))
                                }
                                TextButton(onClick = {
                                    scope.launch {
                                        comandaId?.let { cid ->
                                            repo.addItemToComanda(cid, p.name, p.priceCents, 1, p.id)
                                            items = repo.getComandaItems(cid)
                                            total = repo.getComandaTotal(cid)
                                            Toast.makeText(context, "Adicionado", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }) { Text("+") }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Consumo", fontWeight = FontWeight.SemiBold)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(items) { it ->
                        RuyCard {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "${it.productName} x ${it.qty}")
                                Text(text = reais(it.unitCents * it.qty))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "TOTAL: ${reais(total)}", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(6.dp))
                RuyPrimaryButton(text = "Fechar conta / Finalizar", onClick = {
                    val list = items.map { it.productName to it.qty }
                    onCheckoutTable(total, list, unitPrice, comandaId ?: "", selectedTable!!.tableId)
                })
                RuyPrimaryButton(text = "Voltar às mesas", onClick = {
                    scope.launch {
                        tables = repo.getTables()
                        selectedTable = null
                        comandaId = null
                        items = emptyList()
                        total = 0
                    }
                })
            }
        }
    }
}

