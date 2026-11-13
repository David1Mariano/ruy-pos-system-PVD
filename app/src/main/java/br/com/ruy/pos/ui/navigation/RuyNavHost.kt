package br.com.ruy.pos.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Backup
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.TextButton
import kotlinx.coroutines.launch
import org.json.JSONArray
import br.com.ruy.pos.data.local.OrderEntity
import androidx.compose.ui.Modifier
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.ui.screens.cash.CashRegisterScreen
import br.com.ruy.pos.ui.screens.backup.BackupScreen
import br.com.ruy.pos.ui.screens.reports.ReportsScreen
import br.com.ruy.pos.ui.screens.categories.CategoriesScreen
import br.com.ruy.pos.ui.screens.checkout.CheckoutScreen
import br.com.ruy.pos.ui.screens.orders.OrdersScreen
import br.com.ruy.pos.ui.screens.products.ProductsScreen
import br.com.ruy.pos.ui.screens.sales.SalesScreen
import br.com.ruy.pos.ui.screens.tables.TablesScreen
import br.com.ruy.pos.ui.theme.RuyRed
import br.com.ruy.pos.ui.theme.RuyTextSecondary

@Composable
fun RuyNavHost(repo: Repository, start: String = "sales", onExit: () -> Unit, onRestoreClick: () -> Unit = {}) {
    var route by remember { mutableStateOf(start) }
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    androidx.compose.runtime.DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP && route in setOf("support", "pix-account")) route = "settings"
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    var checkoutData by remember { mutableStateOf<CheckoutData?>(null) }
    val scope = rememberCoroutineScope()
    val active by remember { repo.payments.observeActive() }.collectAsState(initial = null)
    fun resumeOrder(order: OrderEntity) {
        scope.launch {
            val arr = JSONArray(order.itemsJson)
            val lines = (0 until arr.length()).map { arr.getJSONObject(it) }
            val table = order.comandaId?.let { repo.getComanda(it)?.tableId }
            checkoutData = CheckoutData(order.totalCents, lines.map { it.getString("name") to it.getInt("qty") },
                lines.associate { it.getString("name") to it.getInt("unitCents") }, order.comandaId, table, order.id)
            route = "checkout"
        }
    }
    LaunchedEffect(active?.orderId) {
        if (checkoutData == null) active?.let { attempt -> repo.getOrder(attempt.orderId)?.let { resumeOrder(it) } }
    }
    val showNav = route == "sales" || route == "tables" || route == "orders" || route == "products" || route == "categories" || route == "cash" || route == "backup" || route == "reports"
    Scaffold(
        topBar = {
            if (br.com.ruy.pos.BuildConfig.HOMOLOGATION) androidx.compose.material3.Surface(color = RuyRed) {
                Text(br.com.ruy.pos.Homologation.LABEL + " • DADOS FICTÍCIOS", color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.padding(androidx.compose.ui.unit.Dp(8f)))
            }
        },
        bottomBar = {
            if (showNav) {
                NavigationBar {
                    NavigationBarItem(selected = route == "sales", onClick = { route = "sales" }, icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) }, label = { Text("Vendas") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = RuyRed, selectedTextColor = RuyRed, unselectedIconColor = RuyTextSecondary, unselectedTextColor = RuyTextSecondary))
                    NavigationBarItem(selected = route == "tables", onClick = { route = "tables" }, icon = { Icon(Icons.Default.TableRestaurant, contentDescription = null) }, label = { Text("Mesas") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = RuyRed, selectedTextColor = RuyRed, unselectedIconColor = RuyTextSecondary, unselectedTextColor = RuyTextSecondary))
                    NavigationBarItem(selected = route == "orders", onClick = { route = "orders" }, icon = { Icon(Icons.Default.History, contentDescription = null) }, label = { Text("Pedidos") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = RuyRed, selectedTextColor = RuyRed, unselectedIconColor = RuyTextSecondary, unselectedTextColor = RuyTextSecondary))
                    NavigationBarItem(selected = route == "cash", onClick = { route = "cash" }, icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) }, label = { Text("Caixa") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = RuyRed, selectedTextColor = RuyRed, unselectedIconColor = RuyTextSecondary, unselectedTextColor = RuyTextSecondary))
                    NavigationBarItem(selected = route == "categories", onClick = { route = "categories" }, icon = { Icon(Icons.Default.Home, contentDescription = null) }, label = { Text("Mais") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = RuyRed, selectedTextColor = RuyRed, unselectedIconColor = RuyTextSecondary, unselectedTextColor = RuyTextSecondary))
                    NavigationBarItem(selected = route == "reports", onClick = { route = "reports" }, icon = { Icon(Icons.Default.Description, contentDescription = null) }, label = { Text("Relatórios") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = RuyRed, selectedTextColor = RuyRed, unselectedIconColor = RuyTextSecondary, unselectedTextColor = RuyTextSecondary))
                    NavigationBarItem(selected = route == "backup", onClick = { route = "backup" }, icon = { Icon(Icons.Default.Backup, contentDescription = null) }, label = { Text("Backup") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = RuyRed, selectedTextColor = RuyRed, unselectedIconColor = RuyTextSecondary, unselectedTextColor = RuyTextSecondary))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (active != null && route != "checkout") TextButton(onClick = {
                scope.launch { active?.let { repo.getOrder(it.orderId)?.let { order -> resumeOrder(order) } } }
            }) { Text("Pagamento pendente • conferir recebimento") }
            Box(Modifier.weight(1f)) {
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
                        androidx.compose.runtime.key(data.orderId, data.comandaId, data.items) {
                            CheckoutScreen(repo = repo, totalCents = data.totalCents, items = data.items, unitPrice = data.unitPrice, comandaId = data.comandaId, tableId = data.tableId, existingOrderId = data.orderId, onBack = { route = if (data.comandaId != null) "tables" else "orders" }, onPaidAndClosed = { checkoutData = null; route = if (data.comandaId != null) "tables" else "orders" })
                        }
                    } else {
                        route = "sales"
                    }
                }
                "products" -> ProductsScreen(repo = repo, onBack = { route = "sales" })
                "categories" -> CategoriesScreen(repo = repo, onBack = { route = "sales" }, onBackup = { route = "backup" }, onReports = { route = "reports" }, onSettings = { route = "settings" })
                "settings" -> br.com.ruy.pos.ui.screens.settings.SettingsScreen(onBack = { route = "categories" }, onSupport = { route = "support" }, onPix = { route = "pix-account" })
                "pix-account" -> br.com.ruy.pos.ui.screens.settings.PixAccountScreen(onBack = { route = "settings" })
                "support" -> br.com.ruy.pos.ui.screens.settings.SupportScreen(onBack = { route = "settings" })
                "orders" -> OrdersScreen(repo = repo, onBack = { route = "sales" }, onResumeOrder = { resumeOrder(it) })
                "cash" -> CashRegisterScreen(repo = repo, onBack = { route = "sales" })
                "backup" -> BackupScreen(repo = repo, onBack = { route = "categories" }, onRestoreClick = onRestoreClick)
                "reports" -> ReportsScreen(repo = repo, onBack = { route = "sales" })
                else -> SalesScreen(repo = repo, onBack = onExit, onOrderRegistered = { route = "orders" }, onCheckout = { total, items, up ->
                    checkoutData = CheckoutData(total, items, up, null, null)
                    route = "checkout"
                })
            }
            }
        }
    }
}

data class CheckoutData(
    val totalCents: Int,
    val items: List<Pair<String, Int>>,
    val unitPrice: Map<String, Int>,
    val comandaId: String?,
    val tableId: String?,
    val orderId: String? = null
)

