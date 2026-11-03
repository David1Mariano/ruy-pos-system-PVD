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
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18,16,18,20); setBackgroundColor(white) }
        scroll.addView(root); setContentView(scroll)
        val header = TextView(this).apply { text="RUY POS SYSTEM"; textSize=26f; setTextColor(red); gravity=Gravity.CENTER; setPadding(0,6,0,4) }
        root.addView(header)
        val sub = TextView(this).apply { text="Ruy Caldo de Cana"; textSize=13f; setTextColor(black); gravity=Gravity.CENTER; setPadding(0,0,0,10) }
        root.addView(sub)
        val bar = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(4,4,4,8) }
        val token = prefs.getString("device_token","") ?: ""
        val st = TextView(this).apply { text="Status: "; textSize=12f; setTextColor(black) }
        bar.addView(st, LinearLayout.LayoutParams(0,-2,1f))
        root.addView(bar)
        label(title, 18f)
    }
    
    private fun label(v: String, s: Float=16f) { root.addView(TextView(this).apply { text=v; textSize=s; setTextColor(black); setPadding(4,6,4,6) }) }
    private fun button(v: String, action: ()->Unit) {
        val b = Button(this).apply { text=v; isAllCaps=false; setBackgroundColor(red); setTextColor(white); setPadding(10,18,10,18); setOnClickListener { action() } }
        root.addView(b, LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=8 })
    }
    
    private fun home() {
        base("PDV - In?cio")
        button("NOVA VENDA") { sales() }
        button("SINCRONIZAR CAT?LOGO") { syncCatalog() }
        button("HIST?RICO") { history() }
        button("SINCRONIZAR AGORA") { syncNow() }
        button("DIAGN?STICO") { diag() }
        button("CONFIGURA??ES") { config() }
        label("Cat?logo:  itens", 12f)
    }
    
    private fun diag() {
        base("Diagn?stico")
        val baseUrl = prefs.getString("sync_url","") ?: ""
        val cat = if(baseUrl.contains("/api/pos/orders")) baseUrl.replace("/api/pos/orders","/api/pos/catalog") else if(baseUrl.isNotEmpty()) baseUrl else ""
        label("URL: ", 11f)
        label("Cat?logo URL: ", 11f)
        label("Token: ", 11f)
        button("TESTAR CONEX?O") { testConnection(baseUrl) }
        button("VOLTAR") { home() }
    }
    
    private fun config() {
        base("Configura??es")
        label("URL HTTPS homologa??o")
        val url = EditText(this).apply { setText(prefs.getString("sync_url","https://api.ruycaldodecana.com.br/api/pos/orders")); hint="https://host/api/pos/orders"; setPadding(12,12,12,12) }
        root.addView(url)
        label("Token do terminal (Bearer)")
        val tok = EditText(this).apply { setText(prefs.getString("device_token","")); hint="token hex"; setPadding(12,12,12,12) }
        root.addView(tok)
        button("SALVAR") {
            val u = url.text.toString().trim()
            val t = tok.text.toString().trim()
            if(!u.startsWith("https://")) { Toast.makeText(this,"Exigir HTTPS",Toast.LENGTH_LONG).show(); return@button }
            prefs.edit().putString("sync_url",u).putString("device_token",t).apply()
            Toast.makeText(this,"Salvo",Toast.LENGTH_SHORT).show()
            home()
        }
        button("VOLTAR") { home() }
    }
    
    private fun testConnection(u: String) {
        val catalogUrl = if(u.contains("/api/pos/orders")) u.replace("/api/pos/orders","/api/pos/catalog") else u
        thread {
            try {
                val conn = (URL(catalogUrl).openConnection() as HttpURLConnection)
                conn.requestMethod="GET"; conn.connectTimeout=5000; conn.readTimeout=5000
                val code = conn.responseCode
                val txt = if(code in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else ""
                runOnUiThread { Toast.makeText(this,"HTTP  items:",Toast.LENGTH_LONG).show() }
                conn.disconnect()
            } catch(e: Exception) {
                runOnUiThread { Toast.makeText(this,"Erro: ",Toast.LENGTH_LONG).show() }
            }
        }
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
        val catalogUrl = if(baseUrl.contains("/api/pos/orders")) baseUrl.replace("/api/pos/orders","/api/pos/catalog") else if(baseUrl.isNotEmpty()) baseUrl else ""
        if(!catalogUrl.startsWith("https://")) { Toast.makeText(this,"Exigir HTTPS",Toast.LENGTH_SHORT).show(); config(); return }
        Toast.makeText(this,"Sincronizando...",Toast.LENGTH_SHORT).show()
        thread {
            try {
                val conn = (URL(catalogUrl).openConnection() as HttpURLConnection)
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
                        Toast.makeText(this,"Cat?logo: ",Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread { Toast.makeText(this,"HTTP ",Toast.LENGTH_SHORT).show() }
                }
                conn.disconnect()
            } catch(e: Exception) {
                runOnUiThread { Toast.makeText(this,"Falha",Toast.LENGTH_SHORT).show() }
            }
        }
        home()
    }
    
    private fun sales() {
        base("VENDA")
        if(products.isEmpty()) label("Sem produtos. Sincronize cat?logo.")
        products.forEach { p -> button("+  | ") { cart[p.name] = (cart[p.name]?:0)+1; sales() } }
        label("CARRINHO", 18f)
        var total = 0
        cart.forEach { (name,qty) ->
            val unit = unitPrice[name] ?: 0
            total += unit*qty
            button(" x  =   [-]") {
                val n=(cart[name]?:0)-1; if(n<=0) cart.remove(name) else cart[name]=n; sales()
            }
        }
        label("TOTAL: ", 22f)
        if(cart.isNotEmpty()) button("REGISTRAR PEDIDO (LOCAL)") {
            thread {
                try {
                    val items = cart.map { it.key to it.value }
                    val id = kotlinx.coroutines.runBlocking { repo.saveOrder(total, items, unitPrice, null, null) }
                    runOnUiThread { Toast.makeText(this,"Pedido  salvo (pendente)",Toast.LENGTH_SHORT).show(); cart.clear(); home() }
                } catch(e: Exception) {
                    runOnUiThread { Toast.makeText(this,"Erro",Toast.LENGTH_SHORT).show() }
                }
            }
        }
        button("VOLTAR") { home() }
    }
    
    private fun history() { base("HIST?RICO"); label("(em constru??o)"); button("VOLTAR"){home()} }
    
    private fun syncNow() {
        val url = prefs.getString("sync_url","") ?: ""
        val token = prefs.getString("device_token","") ?: ""
        if(!url.startsWith("https://")) { Toast.makeText(this,"Exigir HTTPS",Toast.LENGTH_SHORT).show(); config(); return }
        if(token.isBlank()) { Toast.makeText(this,"Token ausente",Toast.LENGTH_SHORT).show(); config(); return }
        Toast.makeText(this,"Sincronizando agora...",Toast.LENGTH_SHORT).show()
        val work = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(this).enqueue(work)
        Handler(Looper.getMainLooper()).postDelayed({ Toast.makeText(this,"Sincroniza??o agendada",Toast.LENGTH_SHORT).show() }, 800)
    }
}
