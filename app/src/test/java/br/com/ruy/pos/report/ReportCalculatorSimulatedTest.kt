package br.com.ruy.pos.report

import br.com.ruy.pos.data.local.OrderEntity
import br.com.ruy.pos.data.local.PaymentEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportCalculatorSimulatedTest {
    private fun order(id: String, total: Int, status: String) =
        OrderEntity(id = id, totalCents = total, paymentStatus = status, syncStatus = "SYNCED", itemsJson = "[]", createdAt = 0,
            operatorId = null, deviceId = null, stoneTransactionId = null)

    @Test
    fun totalsAndTicket() {
        val orders = listOf(order("1", 1000, "PAGO"), order("2", 2000, "PAGO"), order("3", 500, "PENDENTE"))
        val payments = listOf(
            PaymentEntity(paymentId = "p1", orderId = "1", amountCents = 1000, method = "DINHEIRO", status = "PAGO"),
            PaymentEntity(paymentId = "p2", orderId = "2", amountCents = 2000, method = "PIX", status = "PAGO_MANUALMENTE")
        )
        val r = ReportCalculator.sales(orders, payments)
        assertEquals(3000, r.totalSalesCents)
        assertEquals(3, r.totalOrders)
        assertEquals(1500, r.ticketMedioCents)
        assertEquals(1, r.pending)
        assertEquals(2000L, r.byMethod["PIX"])
    }
}