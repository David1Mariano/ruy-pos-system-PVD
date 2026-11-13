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
