package br.com.ruy.pos.payment

import org.junit.Assert.*
import org.junit.Test

/** SIMULATED protocol data. Never connects to a payment terminal/acquirer. */
class StoneProtocolSimulatedTest {
    private val request = PaymentRequest("internal-uuid", 1234, "DEBITO", reference = "42")
    private val approved = "ruypos-stone://pay-response?code=0&order_id=42&amount=1234&type=D%C3%A9bito&installment_count=0&atk=SIMULATED123&itk=SIMULATED-ITK&authorization_code=123456&authorization_date_time=04%2F11%2F2026%2012%3A00%3A00"
    private fun state(uri: String, expected: PaymentRequest = request) = StoneProtocol.assess(StoneProtocol.parse(uri), expected).state
    private fun invalid(block: () -> Unit) { assertThrows(IllegalArgumentException::class.java, block) }

    @Test fun positiveLongReferencesIncludingMaximum() {
        assertEquals(1L, StoneProtocol.reference("1"))
        assertEquals(Long.MAX_VALUE, StoneProtocol.reference(Long.MAX_VALUE.toString()))
        listOf("0", "-1", "+1", "01", "1.0", "9223372036854775808", "43dfeaaa-0000-0000-0000-000000000000", " 42").forEach { assertNull(StoneProtocol.reference(it)) }
    }
    @Test fun centsAreIntegerAndBounded() {
        StoneProtocol.validate(request.copy(amountCents = 1))
        StoneProtocol.validate(request.copy(amountCents = 999999999))
        listOf(0, -1, 1000000000, Int.MAX_VALUE).forEach { invalid { StoneProtocol.validate(request.copy(amountCents = it)) } }
    }
    @Test fun allOfficialModalitiesAndLocalizedReturnTypes() {
        listOf("CREDIT", "DEBIT", "VOUCHER", "INSTANT_PAYMENT", "PIX").forEach { StoneProtocol.validate(request.copy(method = it)) }
        assertEquals("DEBIT", StoneProtocol.method("Débito"))
        assertEquals("CREDIT", StoneProtocol.method("Crédito"))
        assertNull(StoneProtocol.method("CRYPTO"))
        invalid { StoneProtocol.validate(request.copy(method = "CASH")) }
    }
    @Test fun creditInstallmentRulesWithoutInventedRates() {
        StoneProtocol.validate(request.copy(method = "CREDIT"))
        for (type in listOf("MERCHANT", "ISSUER")) for (count in listOf(2, 99))
            StoneProtocol.validate(request.copy(method = "CREDIT", installments = count, installmentType = type))
        for (bad in listOf(request.copy(installments = 2, installmentType = "MERCHANT"),
            request.copy(method = "CREDIT", installments = 2), request.copy(method = "CREDIT", installmentType = "ISSUER"),
            request.copy(method = "CREDIT", installments = 100, installmentType = "ISSUER"))) invalid { StoneProtocol.validate(bad) }
    }
    @Test fun approvedReturnIsEvidenceNotFinancialConfirmation() { assertEquals("EM_CONCILIACAO", state(approved)) }
    @Test fun zeroAloneNeverConfirms() { assertEquals("INDETERMINADO", state("ruypos-stone://pay-response?code=0&order_id=42")) }
    @Test fun officialDecline1016() { assertEquals("RECUSADO", state("ruypos-stone://pay-response?order_id=42&success=false&reason=1016&response_code=-6&code=-6")) }
    @Test fun officialApplicationExitIsNotProofOfNoCharge() { assertEquals("CANCELADO", state("ruypos-stone://pay-response?code=2&order_id=42")) }
    @Test fun unknownErrorsRequireReconciliation() { assertEquals("INDETERMINADO", state("ruypos-stone://pay-response?code=-999&order_id=42")) }
    @Test fun mismatchedAmount() { assertEquals("INDETERMINADO", state(approved.replace("amount=1234", "amount=9999"))) }
    @Test fun mismatchedReference() { assertEquals("INDETERMINADO", state(approved.replace("order_id=42", "order_id=43"))) }
    @Test fun mismatchedModality() { assertEquals("INDETERMINADO", state(approved.replace("type=D%C3%A9bito", "type=CREDIT"))) }
    @Test fun mismatchedInstallments() { assertEquals("INDETERMINADO", state(approved.replace("installment_count=0", "installment_count=2"))) }
    @Test fun conflictingSuccessFlags() { assertEquals("INDETERMINADO", state("$approved&success=false")) }
    @Test fun missingAuthorizationEvidence() { assertEquals("INDETERMINADO", state(approved.replace("&authorization_code=123456", ""))) }
    @Test fun pixEvidenceRequiresReconciliationToo() {
        val pix = approved.replace("type=D%C3%A9bito", "type=PIX").replace("&authorization_code=123456", "")
        assertEquals("EM_CONCILIACAO", state(pix, request.copy(method = "PIX")))
        assertEquals("INDETERMINADO", state(pix, request.copy(method = "INSTANT_PAYMENT")))
    }
    @Test fun invalidEndpointsAndDuplicateFieldsRejected() {
        listOf(approved.replace("ruypos-stone:", "evil:"), approved.replace("pay-response", "cancel"),
            "$approved&code=0", "$approved&%63ode=2", "$approved#fragment", approved.replace("pay-response", "user@pay-response"),
            approved.replace("pay-response", "pay-response:80"), approved.replace("pay-response?", "pay-response/other?"),
            approved.replace("code=0", "code=NaN")).forEach { invalid { StoneProtocol.parse(it) } }
    }
    @Test fun sensitiveFieldsDiscardedAndEquivalentDuplicatesCanonicalized() {
        val first = StoneProtocol.parse("$approved&pan=DO_NOT_PERSIST&cardholder_name=DO_NOT_PERSIST&message=DO_NOT_PERSIST")
        assertEquals(StoneProtocol.parse(approved).fingerprint, first.fingerprint)
        assertFalse(first.fields.toString().contains("DO_NOT_PERSIST"))
        val reordered = "ruypos-stone://pay-response?" + approved.substringAfter('?').split('&').reversed().joinToString("&")
        assertEquals(first.fingerprint, StoneProtocol.parse(reordered).fingerprint)
    }
}
