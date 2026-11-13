package br.com.ruy.pos.report

import br.com.ruy.pos.data.local.PaymentEntity

data class PaymentBreakdown(val received: Map<String, Long>, val pendingCents: Long, val cancelledCents: Long, val refundedCents: Long) {
    val receivedCents get() = received.values.sum()
}

object PaymentBreakdowns {
    val labels = linkedMapOf("CASH" to "Dinheiro", "PIX_MANUAL" to "Pix confirmado manualmente", "PIX_API" to "Pix confirmado por API",
        "DEBIT_EXTERNAL" to "Débito externo", "CREDIT_EXTERNAL" to "Crédito externo", "TAPSTONE" to "Aproximação TapStone",
        "VOUCHER_EXTERNAL" to "Voucher externo", "SIMULATED" to "Simulado • teste", "OTHER" to "Outros recebimentos")
    private fun group(method: String) = when (method) {
        "DINHEIRO" -> "CASH"
        "PIX", "PIX_STATIC", "INSTANT_PAYMENT" -> "PIX_MANUAL"
        "PIX_API" -> "PIX_API"
        "DEBIT", "DEBITO" -> "DEBIT_EXTERNAL"
        "CREDIT", "CREDITO" -> "CREDIT_EXTERNAL"
        "TAPSTONE_DEBIT", "TAPSTONE_CREDIT" -> "TAPSTONE"
        "VOUCHER", "VOUCHER_FOOD", "VOUCHER_MEAL" -> "VOUCHER_EXTERNAL"
        "SIMULADO" -> "SIMULATED"
        else -> "OTHER"
    }
    fun calculate(payments: List<PaymentEntity>): PaymentBreakdown {
        require(payments.all { it.amountCents >= 0 })
        val unique = payments.distinctBy { it.paymentId }
        val received = unique.filter { it.status in setOf("PAGO", "PAGO_MANUALMENTE") }.groupBy { group(it.method) }.mapValues { (_, values) -> values.sumOf { it.amountCents.toLong() } }
        val pending = unique.filter { it.status in setOf("CRIADA", "AGUARDANDO", "INDETERMINADO", "EM_CONCILIACAO", "RECUSADO") }.sumOf { it.amountCents.toLong() }
        val cancelled = unique.filter { it.status in setOf("CANCELADO", "SEM_RECEBIMENTO", "NAO_INICIADO") }.sumOf { it.amountCents.toLong() }
        val refunded = unique.filter { it.status in setOf("ESTORNADO", "DEVOLVIDO") }.sumOf { it.amountCents.toLong() }
        return PaymentBreakdown(received, pending, cancelled, refunded)
    }
}
