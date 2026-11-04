package br.com.ruy.pos.ui.screens.tables

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import br.com.ruy.pos.data.local.TableEntity
import br.com.ruy.pos.data.local.TableOrderItemEntity
import br.com.ruy.pos.ui.components.RuyCard
import br.com.ruy.pos.ui.components.RuyHeader
import br.com.ruy.pos.ui.components.RuyPriceText
import br.com.ruy.pos.ui.components.RuyPrimaryButton
import br.com.ruy.pos.ui.theme.RuyBackground
import br.com.ruy.pos.ui.theme.RuyBorder
import br.com.ruy.pos.ui.theme.RuyRed
import br.com.ruy.pos.ui.theme.RuySuccess
import br.com.ruy.pos.ui.theme.RuyTextPrimary
import br.com.ruy.pos.ui.theme.RuyTextSecondary
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

    LaunchedEffect(Unit) {
        repo.ensureTables(10)
        products = repo.productLocalAll().filter { it.active }
        products.forEach { unitPrice[it.name] = it.priceCents }
    }
    LaunchedEffect(Unit) { repo.observeTables().collect { tables = it } }

    if (selectedTable == null) {
        Column(modifier = Modifier.fillMaxSize().background(RuyBackground)) {
            RuyHeader(title = "Mesas", subtitle = "Gestão de comandas")
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                RuyCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = count, onValueChange = { count = it }, label = { Text("Quantidade de mesas") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                        Button(onClick = {
                            scope.launch {
                                val c = count.toIntOrNull() ?: 10
                                repo.ensureTables(c)
                                tables = repo.getTables()
                                Toast.makeText(context, "Mesas configuradas", Toast.LENGTH_SHORT).show()
                            }
                        }, shape = RoundedCornerShape(12.dp)) { Text("Aplicar") }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Salão", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 110.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(tables) { t ->
                        val livre = t.status == "LIVRE"
                        Card(
                            onClick = {
                                scope.launch {
                                    val cid = repo.openTable(t.tableId)
                                    comandaId = cid
                                    selectedTable = t
                                    items = repo.getComandaItems(cid)
                                    total = repo.getComandaTotal(cid)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (livre) RuyBorder else RuyRed.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Mesa %02d".format(t.number), style = MaterialTheme.typography.titleMedium, color = RuyTextPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(if (livre) "Livre" else "Ocupada", style = MaterialTheme.typography.bodySmall, color = if (livre) RuyTextSecondary else RuyRed)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                RuyPrimaryButton(text = "Voltar", onClick = onBack)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().background(RuyBackground)) {
            RuyHeader(title = "Mesa %02d".format(selectedTable!!.number), subtitle = if (selectedTable!!.status == "LIVRE") "Livre" else "Ocupada")
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Produtos", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 150.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(products) { p ->
                        RuyCard {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(p.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(reais(p.priceCents), style = MaterialTheme.typography.bodySmall, color = RuyTextSecondary)
                                }
                                FilledTonalButton(onClick = {
                                    scope.launch {
                                        comandaId?.let { cid ->
                                            try { repo.addItemToComanda(cid, p.name, p.priceCents, 1, p.id) }
                                            catch (e: IllegalStateException) {
                                                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                                                return@launch
                                            }
                                            items = repo.getComandaItems(cid)
                                            total = repo.getComandaTotal(cid)
                                            Toast.makeText(context, "Adicionado", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }, shape = RoundedCornerShape(10.dp)) { Text("Adicionar") }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Comanda", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items) { it ->
                        RuyCard {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("${it.productName}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Qtd: ${it.qty}", style = MaterialTheme.typography.bodySmall, color = RuyTextSecondary)
                                }
                                RuyPriceText(reais(it.unitCents * it.qty))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                RuyCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", style = MaterialTheme.typography.titleSmall)
                        Text(reais(total), style = MaterialTheme.typography.titleMedium, color = RuyRed, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RuyPrimaryButton(text = "Fechar conta", onClick = {
                        val list = items.map { it.productName to it.qty }
                        onCheckoutTable(total, list, unitPrice, comandaId ?: "", selectedTable!!.tableId)
                    }, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        scope.launch {
                            tables = repo.getTables()
                            selectedTable = null
                            comandaId = null
                            items = emptyList()
                            total = 0
                        }
                    }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Voltar") }
                }
            }
        }
    }
}

