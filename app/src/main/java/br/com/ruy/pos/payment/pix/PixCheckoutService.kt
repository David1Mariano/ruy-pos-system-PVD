package br.com.ruy.pos.payment.pix

import android.content.Context
import android.content.SharedPreferences
import br.com.ruy.pos.payment.PaymentCoordinator
import br.com.ruy.pos.security.SecurePrefs
import org.json.JSONObject
import java.util.UUID

class PixInvoiceStore internal constructor(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(SecurePrefs.get(context))
    fun save(id: String, q: PixQuote) {
        val o = JSONObject().put("payload", q.payload).put("txid", q.txid).put("amountCents", q.amountCents).put("receiver", q.receiver).put("accountFingerprint", q.accountFingerprint)
        check(prefs.edit().putString("pix_invoice:$id", o.toString()).commit())
    }
    fun load(id: String): PixQuote? = prefs.getString("pix_invoice:$id", null)?.let { encoded ->
        val o = JSONObject(encoded)
        val q = PixQuote(o.getString("payload"), o.getString("txid"), o.getInt("amountCents"), o.getString("receiver"), o.getString("accountFingerprint"))
        val parsed = PixBrCode.validate(q.payload)
        require(parsed.txid == q.txid && parsed.amountCents == q.amountCents && parsed.receiver == q.receiver)
        q
    }
    fun remove(id: String) { check(prefs.edit().remove("pix_invoice:$id").commit()) }
}

class PixCheckoutService(private val payments: PaymentCoordinator, private val settings: PixSettingsStore,
    private val invoices: PixInvoiceStore, private val generator: PixCodeGenerator = StaticPixGenerator()) {
    suspend fun prepare(orderId: String, amountCents: Int): br.com.ruy.pos.data.local.StoneAttemptEntity {
        val account = requireNotNull(settings.load()) { "Configure a conta de recebimento Pix" }
        val id = UUID.randomUUID().toString()
        val quote = generator.generate(PixCodeRequest(account, amountCents, UUID.randomUUID().toString().replace("-", "").take(25)))
        invoices.save(id, quote) // Orphan snapshots are harmless; no pending payment is created without a durable quote.
        return try { payments.beginPix(orderId, quote, id) }
        catch (e: Exception) { invoices.remove(id); throw e }
    }
}
