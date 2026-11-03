package br.com.ruy.pos.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.ui.components.RuyHeader
import br.com.ruy.pos.ui.components.RuyPrimaryButton
import br.com.ruy.pos.ui.screens.categories.CategoriesScreen
import br.com.ruy.pos.ui.screens.checkout.CheckoutScreen
import br.com.ruy.pos.ui.screens.orders.OrdersScreen
import br.com.ruy.pos.ui.screens.products.ProductsScreen
import br.com.ruy.pos.ui.screens.sales.SalesScreen
import br.com.ruy.pos.ui.screens.tables.TablesScreen

@Composable
fun RuyNavHost(repo: Repository, start: String = "home", onExit: () -> Unit) {
    var route by remember { mutableStateOf(start) }
    var checkoutData by remember { mutableStateOf<CheckoutData?>(null) }
    when (route) {
        "home" -> HomeScreen(
            onSales = { route = "sales" },
            onTables = { route = "tables" },
            onProducts = { route = "products" },
            onCategories = { route = "categories" },
            onOrders = { route = "orders" },
            onBack = onExit
        )
        "sales" -> SalesScreen(repo = repo, onBack = { route = "home" }, onOrderRegistered = { route = "orders" }, onCheckout = { total, items, up ->
            checkoutData = CheckoutData(total, items, up, null, null)
            route = "checkout"
        })
        "tables" -> TablesScreen(repo = repo, onBack = { route = "home" }, onCheckoutTable = { total, items, up, comandaId, tableId ->
            checkoutData = CheckoutData(total, items, up, comandaId, tableId)
            route = "checkout"
        })
        "checkout" -> {
            val data = checkoutData
            if (data != null) {
                CheckoutScreen(repo = repo, totalCents = data.totalCents, items = data.items, unitPrice = data.unitPrice, comandaId = data.comandaId, tableId = data.tableId, onBack = { route = if (data.comandaId != null) "tables" else "sales" }, onPaidAndClosed = { route = "tables" })
            } else {
                SalesScreen(repo = repo, onBack = { route = "home" }, onOrderRegistered = { route = "orders" }, onCheckout = { total, items, up ->
                    checkoutData = CheckoutData(total, items, up, null, null)
                    route = "checkout"
                })
            }
        }
        "products" -> ProductsScreen(repo = repo, onBack = { route = "home" })
        "categories" -> CategoriesScreen(repo = repo, onBack = { route = "home" })
        "orders" -> OrdersScreen(repo = repo, onBack = { route = "home" })
        else -> HomeScreen(
            onSales = { route = "sales" },
            onTables = { route = "tables" },
            onProducts = { route = "products" },
            onCategories = { route = "categories" },
            onOrders = { route = "orders" },
            onBack = onExit
        )
    }
}

data class CheckoutData(
    val totalCents: Int,
    val items: List<Pair<String, Int>>,
    val unitPrice: Map<String, Int>,
    val comandaId: String?,
    val tableId: String?
)

@Composable
private fun HomeScreen(
    onSales: () -> Unit,
    onTables: () -> Unit,
    onProducts: () -> Unit,
    onCategories: () -> Unit,
    onOrders: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        RuyHeader(title = "Ruy POS System")
        Column(modifier = Modifier.padding(12.dp)) {
            RuyPrimaryButton(text = "VENDAS", onClick = onSales)
            RuyPrimaryButton(text = "MESAS", onClick = onTables)
            RuyPrimaryButton(text = "HISTÓRICO DE PEDIDOS", onClick = onOrders)
            RuyPrimaryButton(text = "PRODUTOS", onClick = onProducts)
            RuyPrimaryButton(text = "CATEGORIAS", onClick = onCategories)
            RuyPrimaryButton(text = "VOLTAR AO LEGAADO", onClick = onBack)
        }
    }
}

