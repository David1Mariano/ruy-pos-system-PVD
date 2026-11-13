package br.com.ruy.pos.payment

import android.content.*
import android.content.pm.*
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import javax.xml.parsers.DocumentBuilderFactory
import java.io.File

/** SIMULATED Android 11+ package manager/launch failures. Never opens a real payment app. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StoneGatewaySimulatedTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val configuration = StoneConfiguration(true, true, true, true, true)
    private val request = PaymentRequest("internal-uuid-never-sent", 1234, "DEBITO", reference = "42")

    private fun registerHandler() {
        val info = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "br.com.stone.simulated"
                name = "br.com.stone.simulated.PaymentActivity"
                exported = true
                applicationInfo = ApplicationInfo().apply { packageName = "br.com.stone.simulated" }
            }
        }
        shadowOf(context.packageManager).addResolveInfoForIntent(Intent(Intent.ACTION_VIEW, Uri.parse("payment-app://pay")), info)
        shadowOf(context.packageManager).addResolveInfoForIntent(StonePaymentGateway(context, configuration, paymentEnabled = true).buildIntent(request), info)
    }
    @Test fun buildsPaymentIntentWithIntegerCentsNumericReferenceAndFixedAmount() {
        val intent = StonePaymentGateway(context, configuration, paymentEnabled = true).buildIntent(request)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("payment-app", intent.data!!.scheme)
        assertEquals("pay", intent.data!!.host)
        assertEquals("1234", intent.data!!.getQueryParameter("amount"))
        assertEquals("42", intent.data!!.getQueryParameter("order_id"))
        assertEquals("DEBIT", intent.data!!.getQueryParameter("transaction_type"))
        assertEquals("0", intent.data!!.getQueryParameter("editable_amount"))
        assertEquals(StoneProtocol.RETURN_SCHEME, intent.data!!.getQueryParameter("return_scheme"))
        assertFalse(intent.dataString!!.contains(request.orderId))
        assertNull(intent.data!!.getQueryParameter("installment_count"))
    }
    @Test fun creditAtSightUsesNoneAndOmitsCount() {
        val data = StonePaymentGateway(context, configuration, paymentEnabled = true).buildIntent(request.copy(method = "CREDIT")).data!!
        assertEquals("NONE", data.getQueryParameter("installment_type"))
        assertNull(data.getQueryParameter("installment_count"))
    }
    @Test fun merchantAndIssuerSendCountOnlyWhenApplicable() {
        for (type in listOf("MERCHANT", "ISSUER")) {
            val data = StonePaymentGateway(context, configuration, paymentEnabled = true).buildIntent(request.copy(method = "CREDIT", installments = 3, installmentType = type)).data!!
            assertEquals(type, data.getQueryParameter("installment_type"))
            assertEquals("3", data.getQueryParameter("installment_count"))
        }
    }
    @Test fun pixInstantAndVoucherDoNotInventCreditParameters() {
        for (method in listOf("PIX", "INSTANT_PAYMENT", "VOUCHER")) {
            val data = StonePaymentGateway(context, configuration, paymentEnabled = true).buildIntent(request.copy(method = method)).data!!
            assertEquals(method, data.getQueryParameter("transaction_type"))
            assertNull(data.getQueryParameter("installment_type"))
            assertNull(data.getQueryParameter("installment_count"))
        }
    }
    @Test fun unverifiedTerminalOrDisabledOrderIdBlocksIntent() {
        for (config in listOf(StoneConfiguration(), configuration.copy(orderIdEnabled = false), configuration.copy(terminalVerified = false))) {
            val gateway = StonePaymentGateway(context, config)
            assertFalse(gateway.isAvailable())
            assertThrows(IllegalArgumentException::class.java) { gateway.buildIntent(request) }
        }
    }
    @Test fun unsupportedPixOrInstallmentsAreBlocked() {
        assertThrows(IllegalArgumentException::class.java) { StonePaymentGateway(context, configuration.copy(pixSupported = false)).buildIntent(request.copy(method = "PIX")) }
        assertThrows(IllegalArgumentException::class.java) { StonePaymentGateway(context, configuration.copy(issuerSupported = false))
            .buildIntent(request.copy(method = "CREDIT", installments = 2, installmentType = "ISSUER")) }
    }
    @Test fun unresolvedActivityIsReportedAsUnresolvedNotAppAbsent() {
        val gateway = StonePaymentGateway(context, configuration, paymentEnabled = true)
        assertFalse(gateway.isAvailable())
        assertTrue(gateway.unavailableReason()!!.contains("visibilidade"))
        assertEquals("NAO_INICIADO", gateway.process(request).status)
    }
    @Test fun resolveActivityAndActivityNotFoundRaceIsCaptured() {
        registerHandler()
        val fake = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) { throw ActivityNotFoundException("SIMULATED") }
        }
        val gateway = StonePaymentGateway(fake, configuration, paymentEnabled = true)
        assertTrue(gateway.isAvailable())
        assertEquals("NAO_INICIADO", gateway.process(request).status)
    }
    @Test fun securityFailureAndAmbiguousLaunchFailureDiffer() {
        registerHandler()
        val blocked = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) { throw SecurityException("SIMULATED") }
        }
        assertEquals("NAO_INICIADO", StonePaymentGateway(blocked, configuration, paymentEnabled = true).process(request).status)
        val ambiguous = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) { throw IllegalStateException("SIMULATED") }
        }
        assertEquals("INDETERMINADO", StonePaymentGateway(ambiguous, configuration, paymentEnabled = true).process(request).status)
        val ambiguousArgument = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) { throw IllegalArgumentException("SIMULATED after launch invoked") }
        }
        assertEquals("INDETERMINADO", StonePaymentGateway(ambiguousArgument, configuration, paymentEnabled = true).process(request).status)
    }
    @Test fun successfulLaunchIsExplicitAndRemainsAwaitingReturn() {
        registerHandler()
        var launched: Intent? = null
        val fake = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) { launched = intent }
        }
        val result = StonePaymentGateway(fake, configuration, paymentEnabled = true).process(request)
        assertFalse(result.success)
        assertEquals("AGUARDANDO", result.status)
        assertEquals("br.com.stone.simulated", launched!!.component!!.packageName)
    }
    @Test fun manifestDeclaresPaymentVisibilityAndReturnEndpoint() {
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))
        val queries = doc.getElementsByTagName("queries").item(0) as org.w3c.dom.Element
        val data = queries.getElementsByTagName("data").item(0) as org.w3c.dom.Element
        val ns = "http://schemas.android.com/apk/res/android"
        assertEquals("payment-app", data.getAttributeNS(ns, "scheme"))
        assertEquals("pay", data.getAttributeNS(ns, "host"))
        val scheme = if (br.com.ruy.pos.BuildConfig.HOMOLOGATION) "ruypos-stone-homologation" else "ruypos-stone"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$scheme://pay-response?code=0"))
        val activity = intent.resolveActivity(context.packageManager)
        assertEquals("br.com.ruy.pos.ui.activity.PosActivity", activity!!.className)
        assertEquals(ActivityInfo.LAUNCH_SINGLE_TASK, context.packageManager.getActivityInfo(activity, 0).launchMode)
    }
}
