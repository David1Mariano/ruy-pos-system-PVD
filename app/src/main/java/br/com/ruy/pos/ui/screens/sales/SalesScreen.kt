package br.com.ruy.pos.ui.screens.sales

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.data.local.ProductLocalEntity
import br.com.ruy.pos.ui.components.RuyCard
import br.com.ruy.pos.ui.components.RuyHeader
import br.com.ruy.pos.ui.components.RuyPrimaryButton
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
    var registering by remember { mutableStateOf(false) }

    fun reais(c: Int): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(c / 100.0)
    var total by remember { mutableStateOf(0) }
    fun recalc() { total = cart.entries.sumOf { (name, qty) -> (unitPrice[name] ?: 0) * qty } }

    LaunchedEffect(Unit) {
        products = repo.productLocalAll().filter { it.active }
        products.forEach { unitPrice[it.name] = it.priceCents }
        recalc()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        RuyHeader(title = "Vendas")
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Produtos", fontWeight = FontWeight.SemiBold)
            LazyColumn(modifier = Modifier.heightIn(max = 220.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(products) { p ->
                    RuyCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(text = p.name, fontWeight = FontWeight.Medium)
                                Text(text = reais(p.priceCents))
                                val bmp = remember(p.imagePath) {
                                    val path = p.imagePath
                                    if (path != null) try { BitmapFactory.decodeFile(path)?.asImageBitmap() } catch (e: Exception) { null } else null
                                }
                                if (bmp != null) {
                                    Image(bitmap = bmp, contentDescription = null, modifier = Modifier.width(80.dp).height(60.dp), contentScale = ContentScale.Crop)
                                }
                            }
                            TextButton(onClick = {
                                cart[p.name] = (cart[p.name] ?: 0) + 1
                                recalc()
                            }) { Text(text = "+ Adicionar") }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Carrinho", fontWeight = FontWeight.SemiBold)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(cart.toList()) { (name, qty) ->
                    val up = unitPrice[name] ?: 0
                    RuyCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(text = "$name x $qty", fontWeight = FontWeight.Medium)
                                Text(text = reais(up * qty))
                            }
                            Row {
                                TextButton(onClick = {
                                    val n = (cart[name] ?: 0) - 1
                                    if (n <= 0) cart.remove(name) else cart[name] = n
                                    recalc()
                                }) { Text(text = "-") }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "TOTAL: " + reais(total), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            if (cart.isNotEmpty()) {
                RuyPrimaryButton(text = "FINALIZAR VENDA", onClick = {
                    val items = cart.map { it.key to it.value }
                    onCheckout(total, items, unitPrice)
                })
                RuyPrimaryButton(text = "Registrar pedido (balcão)", onClick = {
                    registering = true
                    scope.launch {
                        try {
                            val items = cart.map { it.key to it.value }
                            val id = repo.saveOrder(total, items, unitPrice, null, null)
                            cart.clear()
                            recalc()
                            Toast.makeText(context, "Pedido $id registrado", Toast.LENGTH_LONG).show()
                            onOrderRegistered(id)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro ao registrar: " + e.message, Toast.LENGTH_LONG).show()
                        } finally {
                            registering = false
                        }
                    }
                })
            }
            if (registering) {
                RuyPrimaryButton(text = "Registrando...", onClick = {})
            }
            RuyPrimaryButton(text = "Voltar", onClick = onBack)
        }
    }
}

