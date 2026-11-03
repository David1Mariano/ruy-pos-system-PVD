package br.com.ruy.pos.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.ui.screens.cash.CashRegisterScreen
import br.com.ruy.pos.ui.screens.categories.CategoriesScreen
import br.com.ruy.pos.ui.screens.checkout.CheckoutScreen
import br.com.ruy.pos.ui.screens.orders.OrdersScreen
import br.com.ruy.pos.ui.screens.products.ProductsScreen
import br.com.ruy.pos.ui.screens.sales.SalesScreen
import br.com.ruy.pos.ui.screens.tables.TablesScreen
import br.com.ruy.pos.ui.theme.RuyRed
import br.com.ruy.pos.ui.theme.RuyTextSecondary

@Composable
fun RuyNavHost(repo: Repository, start: String = "sales", onExit: () -> Unit) {
    var route by remember { mutableStateOf(start) }
    var checkoutData by remember { mutableStateOf<CheckoutData?>(null) }
    val showNav = route == "sales" || route == "tables" || route == "orders" || route == "products" || route == "categories" || route == "cash"
    Scaffold(
        bottomBar = {
            if (showNav) {
                NavigationBar {
                    NavigationBarItem(selected = route == "sales", onClick = { route = "sales" }, icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) }, label = { Text("Vendas") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = RuyRed, selectedTextColor = RuyRed, unselectedIconColor = RuyTextSecondary, unselectedTextColor = RuyTextSecondary))
                    NavigationBarItem(selected = route == "tables", onClick = { route = "tables" }, icon = { Icon(Icons.Default.TableRestaurant, contentDescription = null) }, label = { Text("Mesas") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = RuyRed, selectedTextColor = RuyRed, unselectedIconColor = RuyTextSecondary, unselectedTextColor = RuyTextSecondary))
                    NavigationBarItem(selected = route == "orders", onClick = { route = "orders" }, icon = { Icon(Icons.Default.History, contentDescription = null) }, label = { Text("Pedidos") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = RuyRed, selectedTextColor = RuyRed, unselectedIconColor = RuyTextSecondary, unselectedTextColor = RuyTextSecondary))
                    NavigationBarItem(selected = route == "cash", onClick = { route = "cash" }, icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) }, label = { Text("Caixa") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = RuyRed, selectedTextColor = RuyRed, unselectedIconColor = RuyTextSecondary, unselectedTextColor = RuyTextSecondary))
                    NavigationBarItem(selected = route == "categories", onClick = { route = "categories" }, icon = { Icon(Icons.Default.Home, contentDescription = null) }, label = { Text("Mais") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = RuyRed, selectedTextColor = RuyRed, unselectedIconColor = RuyTextSecondary, unselectedTextColor = RuyTextSecondary))
                }
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding)) {
            when (route) {
                "sales" -> SalesScreen(repo = repo, onBack = onExit, onOrderRegistered = { route = "orders" }, onCheckout = { total, items, up ->
                    checkoutData = CheckoutData(total, items, up, null, null)
                    route = "checkout"
                })
                "tables" -> TablesScreen(repo = repo, onBack = { route = "sales" }, onCheckoutTable = { total, items, up, comandaId, tableId ->
                    checkoutData = CheckoutData(total, items, up, comandaId, tableId)
                    route = "checkout"
                })
                "checkout" -> {
                    val data = checkoutData
                    if (data != null) {
                        CheckoutScreen(repo = repo, totalCents = data.totalCents, items = data.items, unitPrice = data.unitPrice, comandaId = data.comandaId, tableId = data.tableId, onBack = { route = if (data.comandaId != null) "tables" else "sales" }, onPaidAndClosed = { route = "tables" })
                    } else {
                        route = "sales"
                    }
                }
                "products" -> ProductsScreen(repo = repo, onBack = { route = "sales" })
                "categories" -> CategoriesScreen(repo = repo, onBack = { route = "sales" })
                "orders" -> OrdersScreen(repo = repo, onBack = { route = "sales" })
                "cash" -> CashRegisterScreen(repo = repo, onBack = { route = "sales" })
                else -> SalesScreen(repo = repo, onBack = onExit, onOrderRegistered = { route = "orders" }, onCheckout = { total, items, up ->
                    checkoutData = CheckoutData(total, items, up, null, null)
                    route = "checkout"
                })
            }
        }
    }
}

data class CheckoutData(
    val totalCents: Int,
    val items: List<Pair<String, Int>>,
    val unitPrice: Map<String, Int>,
    val comandaId: String?,
    val tableId: String?
)

