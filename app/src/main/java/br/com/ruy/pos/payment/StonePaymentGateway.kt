package br.com.ruy.pos.payment

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

data class StoneConfiguration(
    val terminalVerified: Boolean = false,
    val orderIdEnabled: Boolean = false,
    val pixSupported: Boolean = false,
    val merchantSupported: Boolean = false,
    val issuerSupported: Boolean = false
) {
    companion object {
        fun read(context: Context): StoneConfiguration {
            val prefs = context.getSharedPreferences("stone_terminal", Context.MODE_PRIVATE)
            if (prefs.getString("terminal", null) != "${Build.MANUFACTURER}/${Build.MODEL}/${Build.FINGERPRINT}") return StoneConfiguration()
            return StoneConfiguration(prefs.getBoolean("terminalVerified", false), prefs.getBoolean("orderIdEnabled", false),
                prefs.getBoolean("pixSupported", false), prefs.getBoolean("merchantSupported", false), prefs.getBoolean("issuerSupported", false))
        }
    }
    fun save(context: Context) {
        context.getSharedPreferences("stone_terminal", Context.MODE_PRIVATE).edit()
            .putString("terminal", "${Build.MANUFACTURER}/${Build.MODEL}/${Build.FINGERPRINT}")
            .putBoolean("terminalVerified", terminalVerified).putBoolean("orderIdEnabled", orderIdEnabled)
            .putBoolean("pixSupported", pixSupported).putBoolean("merchantSupported", merchantSupported)
            .putBoolean("issuerSupported", issuerSupported).apply()
    }
}

class StonePaymentGateway internal constructor(private val context: Context, private val configuration: StoneConfiguration = StoneConfiguration.read(context),
    private val paymentEnabled: Boolean = br.com.ruy.pos.BuildConfig.SMART_POS_REAL_ENABLED) : PaymentGateway {
    fun unavailableReason(): String? {
        if (!paymentEnabled) return "Stone Smart POS desabilitado até homologação autorizada; não é TapStone"
        if (!configuration.terminalVerified) return "Confirme um POS Android fornecido pela Stone e o aplicativo Pagamento oficial"
        if (!configuration.orderIdEnabled) return "Habilite ID do pedido no aplicativo Ajustes Stone e confirme a configuração"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("payment-app://pay"))
        val handlers = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return if (handlers.size != 1 || intent.resolveActivity(context.packageManager) == null)
            "Não foi possível resolver um único aplicativo Pagamento; verifique instalação, visibilidade e compatibilidade do POS" else null
    }
    override fun isAvailable() = unavailableReason() == null

    fun buildIntent(request: PaymentRequest): Intent {
        StoneProtocol.validate(request)
        require(configuration.terminalVerified && configuration.orderIdEnabled) { "Terminal/ID do pedido não verificados nos Ajustes Stone" }
        val type = StoneProtocol.method(request.method)!!
        require(type !in setOf("PIX", "INSTANT_PAYMENT") || configuration.pixSupported) { "Confirme suporte Pix/QR no equipamento" }
        require(request.installmentType != "MERCHANT" || configuration.merchantSupported) { "Parcelamento MERCHANT não confirmado" }
        require(request.installmentType != "ISSUER" || configuration.issuerSupported) { "Parcelamento ISSUER não confirmado" }
        val builder = Uri.Builder().scheme("payment-app").authority("pay")
            .appendQueryParameter("return_scheme", StoneProtocol.RETURN_SCHEME)
            .appendQueryParameter("amount", request.amountCents.toString())
            .appendQueryParameter("editable_amount", "0")
            .appendQueryParameter("transaction_type", type)
            .appendQueryParameter("order_id", request.reference)
        if (type == "CREDIT") {
            builder.appendQueryParameter("installment_type", request.installmentType)
            if (request.installments > 1) builder.appendQueryParameter("installment_count", request.installments.toString())
        }
        return Intent(Intent.ACTION_VIEW, builder.build()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Caller must persist and atomically claim the attempt BEFORE invoking this method. */
    override fun process(request: PaymentRequest): PaymentResult {
        unavailableReason()?.let { return PaymentResult(false, "NAO_INICIADO", message = it) }
        val intent = try {
            val prepared = buildIntent(request)
            unavailableReason()?.let { return PaymentResult(false, "NAO_INICIADO", message = it) }
            prepared.component = prepared.resolveActivity(context.packageManager)
                ?: return PaymentResult(false, "NAO_INICIADO", message = "Aplicativo não resolvido")
            prepared
        } catch (e: Exception) {
            // Validation/resolution happened before startActivity was invoked.
            return PaymentResult(false, "NAO_INICIADO", message = "Não foi possível preparar a abertura: ${e.message.orEmpty()}")
        }
        return try {
            context.startActivity(intent)
            PaymentResult(false, "AGUARDANDO", message = "Aguardando retorno e conciliação Stone")
        } catch (e: ActivityNotFoundException) {
            PaymentResult(false, "NAO_INICIADO", message = "Não foi possível abrir o aplicativo Pagamento")
        } catch (e: SecurityException) {
            PaymentResult(false, "NAO_INICIADO", message = "Abertura do aplicativo Pagamento bloqueada pelo terminal")
        } catch (e: Exception) {
            // A non-specific failure may occur after the receiving activity has been started.
            PaymentResult(false, "INDETERMINADO", message = "Falha de abertura não conclusiva; concilie antes de repetir")
        }
    }
}
