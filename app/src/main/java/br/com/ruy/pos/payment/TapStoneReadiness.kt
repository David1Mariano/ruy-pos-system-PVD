package br.com.ruy.pos.payment

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.nfc.NfcManager
import android.os.Build

data class TapStoneReadiness(val device: String, val androidSupported: Boolean, val nfcPresent: Boolean,
                             val nfcEnabled: Boolean, val internetValidated: Boolean) {
    val locallyReady get() = androidSupported && nfcPresent && nfcEnabled && internetValidated
    val description get() = "$device • Android 10+: $androidSupported • NFC: $nfcPresent • NFC ativo: $nfcEnabled • Internet validada: $internetValidated"
    val operatorDescription get() = when {
        !androidSupported -> "A aproximação pela Stone requer Android 10 ou superior."
        !nfcPresent -> "Este aparelho não possui NFC. Use outra forma de pagamento."
        !nfcEnabled -> "Ative o NFC nas configurações do Android para receber por aproximação."
        !internetValidated -> "Conecte o aparelho à internet para usar o TapStone."
        else -> "NFC ativo e conexão disponível. Confira o acesso ao TapStone no aplicativo oficial Stone."
    }
    companion object {
        fun read(context: Context): TapStoneReadiness {
            val adapter = (context.getSystemService(Context.NFC_SERVICE) as? NfcManager)?.defaultAdapter
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            return TapStoneReadiness("${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})",
                Build.VERSION.SDK_INT >= 29, adapter != null, adapter?.isEnabled == true,
                network?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true)
        }
    }
}
