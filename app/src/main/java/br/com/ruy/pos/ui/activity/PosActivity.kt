package br.com.ruy.pos.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.ui.navigation.RuyNavHost
import br.com.ruy.pos.ui.theme.RuyPosTheme

class PosActivity : ComponentActivity() {
    private lateinit var repo: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = Repository(this)
        setContent {
            RuyPosTheme {
                RuyNavHost(repo = repo, onExit = { finish() })
            }
        }
    }
}
