package br.com.ruy.pos.report

import br.com.ruy.pos.data.local.PaymentEntity
import org.junit.Assert.*
import org.junit.Test

class PaymentBreakdownTest {
    private fun p(id: String, method: String, value: Int, status: String = "PAGO") = PaymentEntity(id, "ORDER-FICTICIO", value, method, status)
    @Test fun manualApiTapExternalVoucherPendingAndCancelledStaySeparate() {
        val cash = p("cash", "DINHEIRO", 2300)
        val r = PaymentBreakdowns.calculate(listOf(cash, cash, p("pix", "PIX_STATIC", 1000), p("api", "PIX_API", 700),
            p("d", "DEBIT", 300), p("c", "CREDIT", 400), p("tap", "TAPSTONE_CREDIT", 500), p("v", "VOUCHER_MEAL", 600),
            p("wait", "PIX_STATIC", 900, "AGUARDANDO"), p("cancel", "DEBIT", 800, "SEM_RECEBIMENTO")))
        assertEquals(5800L, r.receivedCents); assertEquals(2300L, r.received["CASH"])
        assertEquals(1000L, r.received["PIX_MANUAL"]); assertEquals(700L, r.received["PIX_API"])
        assertEquals(300L, r.received["DEBIT_EXTERNAL"]); assertEquals(400L, r.received["CREDIT_EXTERNAL"])
        assertEquals(500L, r.received["TAPSTONE"]); assertEquals(600L, r.received["VOUCHER_EXTERNAL"])
        assertEquals(900L, r.pendingCents); assertEquals(800L, r.cancelledCents)
    }
    @Test fun splitPaymentsDoNotArtificiallyReduceAverageTicket() {
        val order = br.com.ruy.pos.data.local.OrderEntity("ORDER-FICTICIO", 2300, "PAGO", "PENDING", "[]", 1, null, null, null)
        val payments = listOf(p("cash", "DINHEIRO", 1600).copy(orderId = order.id), p("pix", "PIX_STATIC", 700).copy(orderId = order.id))
        assertEquals(2300L, ReportCalculator.sales(listOf(order), payments).ticketMedioCents)
    }
}
