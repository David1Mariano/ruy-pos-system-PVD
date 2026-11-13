package br.com.ruy.pos.payment

import br.com.ruy.pos.security.PaymentAudit
import org.junit.Assert.*
import org.junit.Test

class PaymentAuditTest {
    @Test fun rejectsSyntheticCardNumberWithoutRecordingIt() {
        val synthetic = "4" + "1".repeat(15)
        assertThrows(IllegalArgumentException::class.java) { PaymentAudit.validate(synthetic) }
        assertThrows(IllegalArgumentException::class.java) { PaymentAudit.validate(synthetic.chunked(4).joinToString(" ")) }
        assertThrows(IllegalArgumentException::class.java) { PaymentAudit.validate("texto\nprivado") }
        assertThrows(IllegalArgumentException::class.java) { StoneProtocol.parse("ruypos-stone://pay-response?code=0&atk=$synthetic") }
        PaymentAudit.validate("E0000000020261113FICTICIO123456789", "Extrato conferido 13/11/2026 10:00", "Operador fictício")
    }
}
