package br.com.ruy.pos.payment

data class PaymentRequest(
    val orderId: String,
    val amountCents: Int,
    val method: String,
    val installments: Int = 1,
    val reference: String
)

data class PaymentResult(
    val success: Boolean,
    val status: String = if (success) "PAGO" else "INDETERMINADO",
    val transactionId: String? = null,
    val message: String? = null,
    val raw: String? = null
)

interface PaymentGateway {
    fun isAvailable(): Boolean
    fun process(request: PaymentRequest): PaymentResult
}

class ManualPaymentGateway : PaymentGateway {
    override fun isAvailable(): Boolean = true
    override fun process(request: PaymentRequest): PaymentResult {
        return PaymentResult(success = false, status = "NAO_INICIADO", message = "Pagamento manual - aguardando confirmação do operador")
    }
}

class StonePaymentGateway : PaymentGateway {
    override fun isAvailable(): Boolean = false
    override fun process(request: PaymentRequest): PaymentResult {
        return PaymentResult(success = false, status = "NAO_INICIADO", message = "Stone indisponível: hardware/credenciais/homologação não configurados nesta sessão")
    }
}

