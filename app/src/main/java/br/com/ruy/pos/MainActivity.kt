package br.com.ruy.pos

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

// MVP offline: registros de pedidos locais, SEM integração de cobrança real.
class MainActivity : Activity() {
    private data class Item(val name: String, val cents: Int)
    private val products = mutableListOf<Item>()
    private val cart = linkedMapOf<String, Int>()
    private lateinit var root: LinearLayout
    private val red = Color.rgb(185, 19, 31)
    private val prefs by lazy { getSharedPreferences("ruy_pos_v1", MODE_PRIVATE) }
    private fun reais(cents: Int): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(cents / 100.0)
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); loadProducts(); home() }
    private fun base(title: String) {
        val scroll = ScrollView(this)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18, 20, 18, 24); setBackgroundColor(Color.WHITE) }
        scroll.addView(root); setContentView(scroll)
        val header = TextView(this).apply { text = "RUY  |  POS SYSTEM"; textSize = 22f; setTextColor(red); gravity = Gravity.CENTER; setPadding(0, 8, 0, 12) }
        root.addView(header); label(title, 19f)
    }
    private fun label(value: String, size: Float = 16f) { root.addView(TextView(this).apply { text = value; textSize = size; setTextColor(Color.BLACK); setPadding(4, 8, 4, 8) }) }
    private fun button(value: String, action: () -> Unit) {
        val b = Button(this).apply { text = value; isAllCaps = false; setOnClickListener { action() } }
        root.addView(b, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 5 })
    }
    private fun input(hint: String, numeric: Boolean = false): EditText = EditText(this).apply {
        this.hint = hint
        if (numeric) inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    }
    private fun home() {
        base("Painel inicial")
        button("Nova venda") { sales() }
        button("Produtos e preços") { catalog() }
        button("Pedidos locais / pendências") { history() }
        button("Sincronizar pedidos pendentes") { syncOrders() }
        button("Configurar conexão HTTPS") { configureSync() }
        label("Protótipo: pedidos sem pagamento. Sincronização depende de uma API do Sistema Ruy e NÃO confirma recebimento nem baixa estoque.", 14f)
    }
    private fun sales() {
        base("Nova venda")
        products.forEach { p -> button("+ ${p.name} · ${reais(p.cents)}") { cart[p.name] = (cart[p.name] ?: 0) + 1; sales() } }
        label("CARRINHO", 18f)
        var total = 0
        cart.forEach { (name, qty) ->
            val unit = products.firstOrNull { it.name == name }?.cents ?: 0
            total += unit * qty
            button("$qty × $name — ${reais(unit * qty)}   [remover 1]") {
                val next = (cart[name] ?: 0) - 1; if (next <= 0) cart.remove(name) else cart[name] = next; sales()
            }
        }
        label("Total: ${reais(total)}", 20f)
        if (cart.isNotEmpty()) button("Registrar pedido (sem cobrar)") {
            val order = JSONObject().put("id", UUID.randomUUID().toString())
                .put("createdAt", System.currentTimeMillis())
                .put("totalCents", total).put("paymentStatus", "UNPAID")
                .put("syncStatus", "PENDING").put("status", "PEDIDO REGISTRADO - PAGAMENTO NÃO PROCESSADO")
            val lines = JSONArray(); cart.forEach { (name, qty) -> lines.put(JSONObject().put("name", name).put("qty", qty).put("unitCents", products.firstOrNull { it.name == name }?.cents ?: 0)) }; order.put("items", lines)
            val old = JSONArray(prefs.getString("orders", "[]")); old.put(order)
            prefs.edit().putString("orders", old.toString()).apply(); cart.clear()
            AlertDialog.Builder(this).setTitle("Pedido salvo").setMessage("Pedido registrado no aparelho. Nenhum pagamento foi realizado.").setPositiveButton("OK") { _, _ -> home() }.show()
        }
        button("Voltar ao início") { home() }
    }
    private fun catalog() {
        base("Catálogo (preços de exemplo, altere antes de usar)")
        products.forEachIndexed { idx, p -> button("${p.name} — ${reais(p.cents)} · editar") { editProduct(idx) } }
        button("Adicionar produto") { editProduct(null) }
        button("Voltar") { home() }
    }
    private fun editProduct(index: Int?) {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(25, 8, 25, 8) }
        val name = input("Nome do produto").apply { setText(index?.let { products[it].name } ?: "") }
        val price = input("Preço em reais (ex.: 8,50)", true).apply { setText(index?.let { "%.2f".format(Locale.US, products[it].cents / 100.0) } ?: "") }
        layout.addView(name); layout.addView(price)
        AlertDialog.Builder(this).setTitle(if (index == null) "Novo produto" else "Editar produto").setView(layout)
            .setPositiveButton("Salvar") { _, _ ->
                val n = name.text.toString().trim()
                val parsed = price.text.toString().replace(',', '.').toBigDecimalOrNull()
                val cents = parsed?.multiply(java.math.BigDecimal(100))?.setScale(0, java.math.RoundingMode.HALF_UP)?.toInt()
                if (n.isBlank() || cents == null || cents < 0 || (products.any { it.name == n } && (index == null || products[index].name != n))) {
                    Toast.makeText(this, "Nome ou preço inválido (ou nome duplicado)", Toast.LENGTH_LONG).show()
                } else { if (index == null) products.add(Item(n, cents)) else { val prior = products[index].name; products[index] = Item(n, cents); if (prior != n) cart.remove(prior) }; saveProducts() }
                catalog()
            }.setNegativeButton("Cancelar", null).show()
    }
    private fun history() {
        base("Pedidos neste aparelho")
        val arr = JSONArray(prefs.getString("orders", "[]")); var sum = 0
        for (i in arr.length() - 1 downTo 0) {
            val o = arr.getJSONObject(i); val amount = o.optInt("totalCents"); sum += amount
            label("Pedido #${i+1} · ${reais(amount)} · ${o.optString("syncStatus", "LEGACY")} · pagamento NÃO confirmado · ${java.text.SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")).format(java.util.Date(o.getLong("createdAt")))}")
        }
        label("Total dos pedidos registrados (não é faturamento): ${reais(sum)}")
        label("Atenção: este valor NÃO equivale ao dinheiro recebido. Não há confirmação de pagamento.", 14f)
        button("Voltar") { home() }
    }
    private fun configureSync() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(25, 8, 25, 8) }
        val url = input("URL HTTPS da API (ex.: https://servidor.exemplo/api/pos/orders)").apply {
            setSingleLine(true); setText(prefs.getString("sync_url", ""))
        }
        val token = input("Token de API (não use chave Supabase service_role)").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText("")
        }
        layout.addView(url); layout.addView(token)
        AlertDialog.Builder(this).setTitle("Conexão do Sistema Ruy").setView(layout)
            .setPositiveButton("Salvar") { _, _ ->
                val endpoint = url.text.toString().trim()
                if (!endpoint.startsWith("https://") || !endpoint.endsWith("/api/pos/orders")) {
                    Toast.makeText(this, "Use HTTPS com caminho /api/pos/orders", Toast.LENGTH_LONG).show()
                } else {
                    prefs.edit().putString("sync_url", endpoint).apply()
                    // Credencial somente em memória nesta versão; não persistimos segredo em SharedPreferences.
                    SessionToken.value = token.text.toString().trim()
                    Toast.makeText(this, "Configuração salva; token só dura até fechar o app", Toast.LENGTH_LONG).show()
                }
                home()
            }.setNegativeButton("Cancelar", null).show()
    }
    private fun syncOrders() {
        val url = prefs.getString("sync_url", "") ?: ""
        val token = SessionToken.value
        if (!url.startsWith("https://") || token.isBlank()) {
            AlertDialog.Builder(this).setMessage("Configure uma URL HTTPS e um token temporário. A API ainda precisa ser implantada no Sistema Ruy.")
                .setPositiveButton("Configurar") { _, _ -> configureSync() }.setNegativeButton("Cancelar", null).show()
            return
        }
        buttonSyncRun(url, token)
    }
    private fun buttonSyncRun(url: String, token: String) {
        Toast.makeText(this, "Sincronizando pedidos pendentes...", Toast.LENGTH_SHORT).show()
        Thread {
            var sent = 0; var failure = ""
            val orders = JSONArray(prefs.getString("orders", "[]"))
            for (i in 0 until orders.length()) {
                val order = orders.getJSONObject(i)
                if (order.optString("syncStatus") != "PENDING") continue
                try {
                    val receipt = SyncClient.send(url, token, order)
                    if (receipt) { order.put("syncStatus", "SYNCED"); sent++ }
                    else { failure = "Resposta inválida do servidor"; break }
                } catch (e: Exception) { failure = e.message ?: "Falha de rede"; break }
            }
            // All modifications happen on the main thread to avoid races with new orders.
            runOnUiThread {
                val live = JSONArray(prefs.getString("orders", "[]"))
                val ackIds = mutableSetOf<String>()
                for (i in 0 until orders.length()) if (orders.getJSONObject(i).optString("syncStatus") == "SYNCED") {
                    ackIds.add(orders.getJSONObject(i).optString("id"))
                }
                for (i in 0 until live.length()) {
                    val o = live.getJSONObject(i)
                    if (o.optString("id").isNotEmpty() && o.optString("id") in ackIds) o.put("syncStatus", "SYNCED")
                }
                prefs.edit().putString("orders", live.toString()).apply()
                AlertDialog.Builder(this).setTitle("Resultado da sincronização")
                    .setMessage("Pedidos confirmados pela API: $sent\n${if (failure.isNotBlank()) "Pendências preservadas: $failure" else "Sem erro de comunicação detectado."}\nIsso NÃO confirma pagamento.")
                    .setPositiveButton("OK") { _, _ -> home() }.show()
            }
        }.start()
    }
    private object SessionToken { var value = "" }
    private fun loadProducts() {
        val saved = prefs.getString("products", null)
        if (saved == null) {
            products.addAll(listOf(Item("Caldo de cana 300 ml", 700), Item("Coxinha", 800), Item("Joelho", 900), Item("Kibe", 800), Item("Pastelão", 1200), Item("Bolo fatia", 1000)))
            saveProducts()
        } else { val arr = JSONArray(saved); for (i in 0 until arr.length()) { val p = arr.getJSONObject(i); products.add(Item(p.getString("name"), p.getInt("cents"))) } }
    }
    private fun saveProducts() { val arr = JSONArray(); products.forEach { arr.put(JSONObject().put("name", it.name).put("cents", it.cents)) }; prefs.edit().putString("products", arr.toString()).apply() }
}
