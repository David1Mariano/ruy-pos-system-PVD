package br.com.ruy.pos.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.ui.navigation.RuyNavHost
import br.com.ruy.pos.ui.screens.backup.handleBackupResult
import br.com.ruy.pos.ui.theme.RuyPosTheme
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PosActivity : ComponentActivity() {
    private lateinit var repo: Repository
    private val restoring = androidx.compose.runtime.mutableStateOf(false)
    private val loadedRepo = androidx.compose.runtime.mutableStateOf<Repository?>(null)
    private val startupMessage = androidx.compose.runtime.mutableStateOf("Preparando dados locais…")

    private val restoreLauncher = registerForActivityResult(RESULT_CONTRACT) { result ->
        restoring.value = true
        lifecycleScope.launch {
        val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { handleBackupResult(this@PosActivity, result.resultCode, result.data) }
        if (ok) android.app.AlertDialog.Builder(this@PosActivity)
            .setTitle("Backup validado e preparado")
            .setMessage("A restauração será aplicada ao reabrir o aplicativo. Feche aqui e abra novamente pelo ícone. Não registre novas operações antes disso.")
            .setCancelable(false)
            .setPositiveButton("Fechar para aplicar") { _, _ -> finishAffinity(); android.os.Process.killProcess(android.os.Process.myPid()) }
            .show()
        else { restoring.value = false; Toast.makeText(this@PosActivity, "Restauração cancelada ou arquivo inválido; dados mantidos", Toast.LENGTH_LONG).show() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            try {
                repo = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    Repository(this@PosActivity).also { br.com.ruy.pos.Homologation.seed(this@PosActivity) }
                }
                loadedRepo.value = repo
            } catch (e: Exception) {
                startupMessage.value = "Não foi possível abrir os dados locais. Preserve os arquivos e procure suporte antes de tentar restaurar novamente."
                android.util.Log.e("RuyStartup", "Dados locais indisponíveis", e)
                return@launch
            }
            getSharedPreferences("restore_state", MODE_PRIVATE).getString("notice", null)?.let {
                Toast.makeText(this@PosActivity, it, Toast.LENGTH_LONG).show()
                getSharedPreferences("restore_state", MODE_PRIVATE).edit().remove("notice").apply()
            }
            handlePaymentReturn(intent)
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    repo.payments.expireMissingReturn()
                    delay(30000)
                }
            }
        }
        setContent {
            RuyPosTheme {
                if (restoring.value) androidx.compose.ui.window.Dialog(onDismissRequest = {},
                    properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
                    androidx.compose.material3.Surface {
                        androidx.compose.material3.Text(if (br.com.ruy.pos.BuildConfig.HOMOLOGATION) "RUY POS — HOMOLOGAÇÃO • Validando restauração…" else "Validando restauração…")
                    }
                }
                loadedRepo.value?.let { readyRepo ->
                    RuyNavHost(repo = readyRepo, onExit = { finish() }, onRestoreClick = { restoreLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply { type = "application/zip" }) })
                } ?: androidx.compose.material3.Text((if (br.com.ruy.pos.BuildConfig.HOMOLOGATION) "RUY POS — HOMOLOGAÇÃO\n" else "") + startupMessage.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePaymentReturn(intent)
    }

    private fun handlePaymentReturn(incoming: Intent?) {
        if (!::repo.isInitialized) return // Cold startup consumes the latest intent after opening Room.
        if (incoming?.action != Intent.ACTION_VIEW) return
        val raw = incoming.dataString ?: return
        lifecycleScope.launch {
            try {
                val result = repo.payments.receive(raw)
                Toast.makeText(this@PosActivity, result, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@PosActivity, "Retorno não persistido; retome a conciliação em Pedidos", Toast.LENGTH_LONG).show()
            }
        }
    }

    private companion object {
        val RESULT_CONTRACT = ActivityResultContracts.StartActivityForResult()
    }
}
