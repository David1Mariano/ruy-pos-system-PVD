package br.com.ruy.pos.ui.components

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object Money {
    fun cents(text: String): Int? = try {
        val clean = text.trim()
        val normalized = if (',' in clean) {
            require(clean.matches(Regex("(?:[0-9]+|[0-9]{1,3}(?:\\.[0-9]{3})+),[0-9]{1,2}")))
            clean.replace(".", "").replace(',', '.')
        } else {
            require(clean.matches(Regex("[0-9]+(?:\\.[0-9]{1,2})?")))
            clean
        }
        BigDecimal(normalized).movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).intValueExact().takeIf { it >= 0 }
    } catch (_: Exception) { null }
    fun input(cents: Int): String = BigDecimal.valueOf(cents.toLong(), 2).toPlainString().replace('.', ',')
    fun format(cents: Long): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(BigDecimal.valueOf(cents, 2))
}

object OperationalText {
    fun method(value: String): String = when (value) {
        "DINHEIRO" -> "Dinheiro"
        "PIX", "PIX_STATIC", "INSTANT_PAYMENT" -> "Pix confirmado manualmente"
        "PIX_API" -> "Pix confirmado pelo banco"
        "DEBIT", "DEBITO" -> "Débito externo"
        "CREDIT", "CREDITO" -> "Crédito externo"
        "TAPSTONE_DEBIT" -> "TapStone • débito"
        "TAPSTONE_CREDIT" -> "TapStone • crédito"
        "VOUCHER_FOOD" -> "Vale-alimentação externo"
        "VOUCHER_MEAL" -> "Vale-refeição externo"
        "VOUCHER" -> "Voucher externo"
        "SIMULADO" -> "Simulado • teste"
        else -> "Pagamento externo"
    }
    fun status(value: String): String = when (value) {
        "PAGO", "CONFIRMADO" -> "Pago"
        "PAGO_MANUALMENTE" -> "Confirmado manualmente"
        "PARCIAL" -> "Pago parcialmente"
        "CANCELADO" -> "Cancelado"
        "RECUSADO" -> "Recusado"
        "SEM_RECEBIMENTO", "NAO_INICIADO" -> "Sem recebimento"
        "INDETERMINADO" -> "Resultado não confirmado • conferir recebimento"
        else -> "Aguardando pagamento"
    }
    fun connection(sync: String): String = when (sync) {
        "PENDING" -> "Salvo neste aparelho • envio pendente"
        "SYNCED" -> "Enviado ao sistema"
        "HOMOLOGATION" -> "Dados de teste"
        else -> "Salvo neste aparelho"
    }
    fun movement(type: String): String = when (type) {
        "SANGRIA" -> "Sangria"
        "SUPRIMENTO" -> "Suprimento"
        else -> if (type.startsWith("VENDA_")) method(type.removePrefix("VENDA_")) else "Movimento de caixa"
    }
    fun error(error: Throwable): String {
        SupportDiagnostics.record(error)
        val message = error.message.orEmpty()
        return when {
            "Configure a conta" in message -> "Cadastre a conta Pix em Mais > Configurações > Pagamentos antes de gerar o código."
            "Caixa" in message || "caixa" in message -> "Confira se o caixa está aberto e se não há pagamentos pendentes."
            "saldo" in message || "Valor" in message || "valor" in message -> "Confira o valor informado e o saldo a pagar."
            "comprovante" in message || "Comprovante" in message || "Referência" in message -> "Confira o comprovante. Ele deve corresponder a este pagamento e não pode ser reutilizado."
            "cancelado" in message || "encerrada" in message -> "Este pedido ou comanda já foi encerrado. Consulte o histórico."
            "pendente" in message || "Concilie" in message -> "Há um pagamento em conferência. Verifique o recebimento antes de iniciar outro."
            else -> "Não foi possível concluir. Confira o pedido e os recebimentos antes de tentar novamente."
        }
    }
}

/** No exception messages, payloads, account details or stack traces are retained. */
object SupportDiagnostics {
    private val events = ArrayDeque<String>()
    @Synchronized fun record(error: Throwable) {
        if (events.size == 20) events.removeFirst()
        events.addLast("${System.currentTimeMillis()} • ${error.javaClass.simpleName}")
    }
    @Synchronized fun recent() = events.toList()
}
