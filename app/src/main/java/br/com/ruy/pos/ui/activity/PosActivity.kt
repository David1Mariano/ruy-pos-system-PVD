package br.com.ruy.pos.ui.activity

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.ui.navigation.RuyNavHost
import br.com.ruy.pos.ui.theme.RuyPosTheme
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PosActivity : ComponentActivity() {
    private lateinit var repo: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = Repository(this)
        handlePaymentReturn(intent)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    repo.payments.expireMissingReturn()
                    delay(30000)
                }
            }
        }
        setContent {
            RuyPosTheme {
                RuyNavHost(repo = repo, onExit = { finish() })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePaymentReturn(intent)
    }

    private fun handlePaymentReturn(incoming: Intent?) {
        if (incoming?.action != Intent.ACTION_VIEW) return
        val raw = incoming.dataString ?: return
        lifecycleScope.launch {
            try {
                val result = repo.payments.receive(raw)
                Toast.makeText(this@PosActivity, result, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                // Keep the persisted flight locked if storage/processing fails. No URI/card data in logs.
                Toast.makeText(this@PosActivity, "Retorno não persistido; retome a conciliação em Pedidos", Toast.LENGTH_LONG).show()
            }
        }
    }
}
