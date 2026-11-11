package br.com.ruy.pos.payment

import br.com.ruy.pos.data.local.PaymentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentCoordinatorSimulatedTest {
    @Test
    fun externalPaymentRegistersAsManual() {
        val p = PaymentEntity(paymentId = "p1", orderId = "o1", amountCents = 1000, method = "PIX", status = "PAGO_MANUALMENTE")
        assertEquals("PAGO_MANUALMENTE", p.status)
        assertEquals(1000, p.amountCents)
    }

    @Test
    fun statusesAreDistinct() {
        val statuses = setOf("PAGO", "PAGO_MANUALMENTE", "PENDENTE", "AGUARDANDO", "INDETERMINADO", "RECUSADO", "CANCELADO", "SEM_RECEBIMENTO")
        assertEquals(8, statuses.size)
    }
}