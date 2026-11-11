package br.com.ruy.pos.report

import br.com.ruy.pos.data.local.PaymentEntity
import br.com.ruy.pos.data.local.OrderEntity

data class ReportRow(
    val label: String,
    val qty: Int = 0,
    val amountCents: Long = 0
)

data class SalesReport(
    val generatedAt: Long,
    val totalSalesCents: Long,
    val totalOrders: Int,
    val ticketMedioCents: Long,
    val byMethod: Map<String, Long>,
    val topProducts: List<ReportRow>,
    val cancelled: Int,
    val pending: Int
)

object ReportCalculator {
    fun sales(orders: List<OrderEntity>, payments: List<PaymentEntity>): SalesReport {
        val paid = payments.filter { it.status == "PAGO" || it.status == "PAGO_MANUALMENTE" }
        val totalSales = paid.sumOf { it.amountCents.toLong() }
        val totalOrders = orders.size
        val ticketMedio = if (paid.isNotEmpty()) totalSales / paid.size else 0L
        val byMethod = paid.groupBy { it.method }.mapValues { it.value.sumOf { p -> p.amountCents.toLong() } }
        val pending = orders.count { it.paymentStatus == "PENDENTE" || it.paymentStatus == "NAO_PAGO" }
        val cancelled = orders.count { it.paymentStatus == "CANCELADO" }
        val top = paid.groupBy { it.method }.entries.sortedByDescending { it.value.sumOf { p -> p.amountCents.toLong() } }
            .map { ReportRow(it.key, it.value.size, it.value.sumOf { p -> p.amountCents.toLong() }) }
        return SalesReport(System.currentTimeMillis(), totalSales, totalOrders, ticketMedio, byMethod, top, cancelled, pending)
    }
}