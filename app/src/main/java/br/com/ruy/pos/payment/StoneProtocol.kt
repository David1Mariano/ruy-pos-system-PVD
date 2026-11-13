package br.com.ruy.pos.payment

import java.net.URI
import java.net.URLDecoder
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

object StoneProtocol {
    const val RETURN_SCHEME = "ruypos-stone"
    const val RETURN_HOST = "pay-response"
    const val MAX_AMOUNT = 999999999
    const val RETURN_TIMEOUT_MS = 120000L

    fun reference(value: String): Long? = value.takeIf { it.matches(Regex("[1-9][0-9]{0,18}")) }
        ?.toLongOrNull()?.takeIf { it > 0 }

    fun method(value: String?): String? {
        val normalized = Normalizer.normalize(value.orEmpty(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "").uppercase(Locale.ROOT)
        return when (normalized) {
            "CREDIT", "CREDITO" -> "CREDIT"
            "DEBIT", "DEBITO" -> "DEBIT"
            "PIX" -> "PIX"
            "INSTANT_PAYMENT" -> "INSTANT_PAYMENT"
            "VOUCHER" -> "VOUCHER"
            else -> null
        }
    }

    fun validate(request: PaymentRequest) {
        require(request.amountCents in 1..MAX_AMOUNT) { "Valor Stone inválido (centavos)" }
        require(reference(request.reference) != null) { "Referência Stone deve ser um Long positivo" }
        val type = requireNotNull(method(request.method)) { "Modalidade Stone inválida" }
        if (type == "CREDIT") {
            require((request.installments == 1 && request.installmentType == "NONE") ||
                (request.installments in 2..99 && request.installmentType in setOf("MERCHANT", "ISSUER"))) {
                "Selecione parcelamento suportado pelo estabelecimento e pelo terminal"
            }
        } else {
            require(request.installments == 1 && request.installmentType == "NONE") { "Parcelamento somente no crédito" }
        }
    }

    data class Callback(val fields: Map<String, String>, val fingerprint: String) {
        val reference get() = fields["order_id"]?.let(StoneProtocol::reference)
        val code get() = fields["code"]?.toIntOrNull()
    }
    // PAN, cardholder_name, brand, free text message, and unknown fields are discarded.
    private val persistedFields = setOf("code", "success", "reason", "response_code", "amount", "type",
        "order_id", "atk", "itk", "authorization_code", "authorization_date_time", "installment_count")

    fun fingerprint(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    fun parse(raw: String): Callback {
        require(raw.length <= 8192) { "Retorno excede limite" }
        val uri = URI(raw)
        require(uri.scheme == RETURN_SCHEME && uri.host == RETURN_HOST && uri.port == -1 &&
            uri.userInfo == null && uri.fragment == null && uri.path.orEmpty() in setOf("", "/")) { "Origem de retorno inválida" }
        val all = linkedMapOf<String, String>()
        for (part in requireNotNull(uri.rawQuery).split('&')) {
            val pair = part.split('=', limit = 2)
            require(pair.size == 2) { "Parâmetro inválido" }
            val key = URLDecoder.decode(pair[0], "UTF-8")
            val value = URLDecoder.decode(pair[1], "UTF-8")
            require(!all.containsKey(key) && key.length <= 64 && value.length <= 512) { "Parâmetro duplicado ou excessivo" }
            all[key] = value
        }
        require(all["code"]?.matches(Regex("-?[0-9]{1,6}")) == true) { "Código ausente/inválido" }
        val safe = all.filterKeys { it in persistedFields }.toSortedMap()
        br.com.ruy.pos.security.PaymentAudit.validate(*safe.values.toTypedArray())
        val canonical = safe.entries.joinToString("") { "${it.key.length}:${it.key}${it.value.length}:${it.value}" }
        return Callback(safe, fingerprint(canonical))
    }

    data class Assessment(val state: String, val detail: String)
    fun assess(callback: Callback, request: PaymentRequest): Assessment {
        val f = callback.fields
        fun unknown(detail: String) = Assessment("INDETERMINADO", detail)
        if (callback.reference != reference(request.reference)) return unknown("Referência ausente ou divergente; conferir extrato")
        if (f["amount"] != null && f["amount"]?.toIntOrNull() != request.amountCents) return unknown("Valor divergente")
        if (f["type"] != null && method(f["type"]) != method(request.method)) return unknown("Modalidade divergente")
        if (callback.code == 2) return Assessment("CANCELADO", "Aplicativo encerrado; confirmar ausência de recebimento no extrato")
        if (callback.code == -6 && f["reason"] == "1016" && f["success"] == "false")
            return Assessment("RECUSADO", "Não autorizada (1016); conferir extrato antes de tentar novamente")
        if (callback.code != 0) return unknown("Erro Stone não conclusivo; conferir extrato")
        if (f["success"] == "false" || (f["response_code"] != null && f["response_code"]?.toIntOrNull() != 0))
            return unknown("Indicadores de resultado conflitantes")
        if (f["amount"]?.toIntOrNull() != request.amountCents || method(f["type"]) != method(request.method))
            return unknown("Aprovação sem valor/modalidade verificáveis")
        val count = f["installment_count"]?.toIntOrNull()
        if ((request.installments == 1 && count !in setOf(0, 1)) || (request.installments > 1 && count != request.installments))
            return unknown("Parcelas ausentes ou divergentes")
        if (listOf("atk", "itk", "authorization_date_time").any { f[it].isNullOrBlank() } ||
            (method(request.method) !in setOf("PIX", "INSTANT_PAYMENT") && f["authorization_code"].isNullOrBlank()))
            return unknown("Aprovação sem identificadores/autorização suficientes")
        return Assessment("EM_CONCILIACAO", "Aprovação informada; conferir comprovante/extrato Stone para registrar recebimento")
    }
}
