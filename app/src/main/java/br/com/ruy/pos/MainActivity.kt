package br.com.ruy.pos

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.*
import androidx.work.*
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.data.local.ProductEntity
import br.com.ruy.pos.sync.SyncWorker
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private val red = Color.rgb(185, 19, 31)
    private val black = Color.BLACK
    private val white = Color.WHITE
    private lateinit var root: LinearLayout
    private lateinit var repo: Repository
    private val prefs by lazy { getSharedPreferences("ruy_pos_v1", MODE_PRIVATE) }
    private val cart = linkedMapOf<String, Int>()
    private val products = mutableListOf<ProductEntity>()
    private val unitPrice = mutableMapOf<String, Int>()
    private fun reais(c: Int): String = NumberFormat.getCurrencyInstance(Locale("pt","BR")).format(c/100.0)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = Repository(this)
        scheduleSync()
        loadCatalog()
        home()
    }
    
    private fun scheduleSync() {
        val req = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("ruy_pos_sync", ExistingPeriodicWorkPolicy.KEEP, req)
    }
    
    private fun base(title: String) {
        val scroll = ScrollView(this)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18,20,18,24); setBackgroundColor(white) }
        scroll.addView(root); setContentView(scroll)
        val header = TextView(this).apply { text="RUY POS SYSTEM"; textSize=24f; setTextColor(red); gravity=Gravity.CENTER; setPadding(0,8,0,12) }
        root.addView(header)
        val sub = TextView(this).apply { text="Ruy Caldo de Cana"; textSize=14f; setTextColor(black); gravity=Gravity.CENTER; setPadding(0,0,0,12) }
        root.addView(sub)
        label(title, 19f)
    }
    
    private fun label(v: String, s: Float=16f) { root.addView(TextView(this).apply { text=v; textSize=s; setTextColor(black); setPadding(4,8,4,8) }) }
    private fun button(v: String, action: ()->Unit) {
        val b = Button(this).apply { text=v; isAllCaps=false; setBackgroundColor(red); setTextColor(white); setOnClickListener { action() } }
        root.addView(b, LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=6 })
    }
    
    private fun home() {
        base("Painel")
        button("Nova venda") { sales() }
        button("Catálogo (sincronizar)") { syncCatalog() }
        button("Pedidos locais") { history() }
        button("Sincronizar agora") { syncNow() }
        button("Configuração") { config() }
        val st = prefs.getString("sync_url","") ?: ""
        label(if(st.isEmpty()) "Status: Sem servidor configurado" else "Status: Servidor configurado", 14f)
    }
    
    private fun config() {
        base("Configuração HTTPS")
        val url = EditText(this).apply { setText(prefs.getString("sync_url","")); hint="https://.../api/pos/orders" }
        root.addView(url)
        button("Salvar") {
            val u = url.text.toString().trim()
            if(u.startsWith("https://")) { prefs.edit().putString("sync_url",u).apply(); Toast.makeText(this,"Salvo",Toast.LENGTH_SHORT).show() }
            else Toast.makeText(this,"Use HTTPS",Toast.LENGTH_LONG).show()
            home()
        }
        button("Voltar") { home() }
    }
    
    private fun loadCatalog() {
        thread {
            try {
                val list = kotlinx.coroutines.runBlocking { repo.products() }
                runOnUiThread {
                    products.clear(); unitPrice.clear()
                    list.forEach { p -> products.add(p); unitPrice[p.name] = p.priceCents }
                }
            } catch(e: Exception) {}
        }
    }
    
    private fun syncCatalog() {
        val baseUrl = prefs.getString("sync_url","") ?: ""
        val catalogUrl = if(baseUrl.endsWith("/api/pos/orders")) baseUrl.replace("/api/pos/orders","/api/pos/catalog") else baseUrl
        if(catalogUrl.isEmpty() || !catalogUrl.startsWith("https://")) { Toast.makeText(this,"Configure servidor HTTPS",Toast.LENGTH_SHORT).show(); config(); return }
        Toast.makeText(this,"Sincronizando catálogo...",Toast.LENGTH_SHORT).show()
        thread {
            try {
                val url = URL(catalogUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod="GET"; conn.connectTimeout=10000; conn.readTimeout=10000
                val code = conn.responseCode
                if(code in 200..299) {
                    val txt = conn.inputStream.bufferedReader().use { it.readText() }
                    val obj = JSONObject(txt)
                    val arr = obj.optJSONArray("items") ?: JSONArray()
                    val list = mutableListOf<ProductEntity>()
                    for(i in 0 until arr.length()) {
                        val it = arr.getJSONObject(i)
                        list.add(ProductEntity(
                            id = it.optString("id", UUID.randomUUID().toString()),
                            sku = it.optString("sku",null),
                            name = it.optString("name",""),
                            category = it.optString("category",null),
                            priceCents = it.optInt("price_cents",0),
                            active = it.optBoolean("active",true),
                            imageUrl = it.optString("image_url",null),
                            version = it.optLong("version",1),
                            updatedAt = it.optString("updated_at",null),
                            unit = it.optString("unit",null)
                        ))
                    }
                    runOnUiThread { 
                        products.clear(); unitPrice.clear()
                        list.filter { it.active }.forEach { p -> products.add(p); unitPrice[p.name]=p.priceCents }
                        Toast.makeText(this,"Catálogo atualizado: ${products.size} itens",Toast.LENGTH_SHORT).show()
                    }
                }
                conn.disconnect()
            } catch(e: Exception) {
                runOnUiThread { Toast.makeText(this,"Falha ao sincronizar catálogo",Toast.LENGTH_SHORT).show() }
            }
        }
        home()
    }
    
    private fun sales() {
        base("Nova venda")
        if(products.isEmpty()) label("Nenhum produto. Sincronize o catálogo.")
        products.forEach { p -> button("+ ${p.name} · ${reais(p.priceCents)}") { cart[p.name] = (cart[p.name]?:0)+1; sales() } }
        label("CARRINHO", 18f)
        var total = 0
        cart.forEach { (name,qty) ->
            val unit = unitPrice[name] ?: 0
            total += unit*qty
            button("$qty × $name — ${reais(unit*qty)} [remover]") {
                val n=(cart[name]?:0)-1; if(n<=0) cart.remove(name) else cart[name]=n; sales()
            }
        }
        label("Total: ${reais(total)}", 20f)
        if(cart.isNotEmpty()) button("Registrar pedido (sem cobrar)") {
            thread {
                try {
                    val items = cart.map { it.key to it.value }
                    val id = kotlinx.coroutines.runBlocking { repo.saveOrder(total, items, unitPrice) }
                    runOnUiThread {
                        cart.clear()
                        AlertDialog.Builder(this).setTitle("Pedido salvo").setMessage("Pedido registrado localmente.").setPositiveButton("OK"){_,_->home()}.show()
                    }
                } catch(e: Exception) {
                    runOnUiThread { Toast.makeText(this,"Erro ao salvar",Toast.LENGTH_SHORT).show() }
                }
            }
        }
        button("Voltar") { home() }
    }
    
    private fun history() {
        base("Pedidos locais")
        thread {
            try {
                val list = kotlinx.coroutines.runBlocking { repo.allOrders() }
                runOnUiThread {
                    if(list.isEmpty()) label("Nenhum pedido")
                    else list.take(30).forEach { o -> label("${o.id.take(8)} | ${reais(o.totalCents)} | ${o.syncStatus} | ${o.paymentStatus}", 11f) }
                    button("Voltar") { home() }
                }
            } catch(e: Exception) {
                runOnUiThread { label("Erro ao carregar"); button("Voltar"){home()} }
            }
        }
    }
    
    private fun syncNow() {
        Toast.makeText(this,"Sincronizando...",Toast.LENGTH_SHORT).show()
        WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
        Handler(Looper.getMainLooper()).postDelayed({ home() }, 2000)
    }
}
