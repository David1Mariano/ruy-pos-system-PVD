package br.com.ruy.pos.ui.screens.sales

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.data.local.ProductLocalEntity
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
fun SalesScreen(repo: Repository, onBack: () -> Unit, onOrderRegistered: (String) -> Unit = {}, onCheckout: (Int, List<Pair<String, Int>>, Map<String, Int>) -> Unit = { _, _, _ -> }) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var products by remember { mutableStateOf<List<ProductLocalEntity>>(emptyList()) }
    val cart = remember { mutableStateMapOf<String, Int>() }
    val unitPrice = remember { mutableStateMapOf<String, Int>() }
    var search by remember { mutableStateOf("") }
    var registering by remember { mutableStateOf(false) }

    fun reais(c: Int): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(c / 100.0)
    var total by remember { mutableStateOf(0) }
    fun recalc() { total = cart.entries.sumOf { (name, qty) -> (unitPrice[name] ?: 0) * qty } }

    LaunchedEffect(Unit) {
        products = repo.productLocalAll().filter { it.active }
        products.forEach { unitPrice[it.name] = it.priceCents }
        recalc()
    }

    val filtered = products.filter { it.name.contains(search, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().background(RuyBackground)) {
        RuyHeader(title = "Vendas", subtitle = "Balcão / PDV")
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar produto...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text("Produtos", style = MaterialTheme.typography.titleSmall, color = RuyTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 140.dp), modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered) { p ->
                    Card(
                        onClick = {
                            cart[p.name] = (cart[p.name] ?: 0) + 1
                            recalc()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RuyBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            val bmp = remember(p.imagePath) {
                                val path = p.imagePath
                                if (path != null) try { BitmapFactory.decodeFile(path)?.asImageBitmap() } catch (e: Exception) { null } else null
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(10.dp)).background(RuyBackground)) {
                                if (bmp != null) {
                                    Image(bitmap = bmp, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(p.name, style = MaterialTheme.typography.bodyMedium, color = RuyTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(4.dp))
                            RuyPriceText(reais(p.priceCents))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            RuyCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Carrinho", style = MaterialTheme.typography.titleSmall)
                        Text("${cart.values.sum()} itens", style = MaterialTheme.typography.bodySmall, color = RuyTextSecondary)
                    }
                    Text(reais(total), style = MaterialTheme.typography.titleMedium, color = RuyRed, fontWeight = FontWeight.SemiBold)
                }
                if (cart.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(cart.toList()) { (name, qty) ->
                            AssistChip(onClick = {}, label = { Text("$name x$qty") })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (cart.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RuyPrimaryButton(text = "Finalizar venda", onClick = {
                            val items = cart.map { it.key to it.value }
                            onCheckout(total, items, unitPrice)
                        }, modifier = Modifier.weight(1f))
                        FilledTonalButton(enabled = !registering, onClick = {
                            if (registering) return@FilledTonalButton
                            registering = true
                            scope.launch {
                                try {
                                    val items = cart.map { it.key to it.value }
                                    val id = repo.saveOrder(total, items, unitPrice, null, null)
                                    cart.clear()
                                    recalc()
                                    Toast.makeText(context, "Pedido ${id.take(8).uppercase()} registrado", Toast.LENGTH_SHORT).show()
                                    onOrderRegistered(id)
                                } catch (e: Exception) {
                                    Toast.makeText(context, br.com.ruy.pos.ui.components.OperationalText.error(e), Toast.LENGTH_SHORT).show()
                                } finally {
                                    registering = false
                                }
                            }
                        }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Text(if (registering) "Registrando..." else "Registrar pedido")
                        }
                    }
                }
            }
        }
    }
}

