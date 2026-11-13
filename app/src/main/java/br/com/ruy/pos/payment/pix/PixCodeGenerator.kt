package br.com.ruy.pos.payment.pix

import java.math.BigDecimal
import java.util.Locale

data class PixCodeRequest(val account: PixAccount, val amountCents: Int, val txid: String)
data class PixQuote(val payload: String, val txid: String, val amountCents: Int, val receiver: String, val accountFingerprint: String)
interface PixCodeGenerator { fun generate(request: PixCodeRequest): PixQuote }
class StaticPixGenerator : PixCodeGenerator {
    override fun generate(request: PixCodeRequest): PixQuote {
        val a = request.account.validated()
        val payload = PixBrCode.encode(a, request.amountCents, request.txid)
        val canonical = listOf(a.institution, a.receiver, a.taxId, a.key, a.keyType.name, a.label, a.city).joinToString("") { "${it.length}:$it" }
        return PixQuote(payload, request.txid, request.amountCents, a.receiver, PixSettingsStore.fingerprint(canonical))
    }
}

/** Only the static BR Code subset. No network request, provider endpoint or automatic approval. */
object PixBrCode {
    data class Parsed(val key: String, val txid: String, val amountCents: Int?, val receiver: String, val city: String)
    private fun tlv(id: String, value: String): String {
        require(id.matches(Regex("[0-9]{2}")) && value.length in 1..99 && value.all { it.code in 32..126 })
        return id + "%02d".format(Locale.ROOT, value.length) + value
    }
    fun crc16(text: String): String {
        var crc = 0xffff
        for (byte in text.toByteArray(Charsets.UTF_8)) {
            crc = crc xor ((byte.toInt() and 255) shl 8)
            repeat(8) { crc = if ((crc and 0x8000) != 0) (crc shl 1) xor 0x1021 else crc shl 1; crc = crc and 0xffff }
        }
        return "%04X".format(Locale.ROOT, crc)
    }
    fun encode(account: PixAccount, amountCents: Int?, txid: String): String {
        val a = account.validated()
        require(txid == "***" || txid.matches(Regex("[A-Za-z0-9]{1,25}"))) { "Identificação Pix inválida" }
        require(amountCents == null || amountCents > 0) { "Valor Pix deve ser positivo" }
        val body = tlv("00", "01") + tlv("26", tlv("00", "br.gov.bcb.pix") + tlv("01", a.key)) + tlv("52", "0000") + tlv("53", "986") +
            (amountCents?.let { tlv("54", BigDecimal.valueOf(it.toLong(), 2).toPlainString()) } ?: "") + tlv("58", "BR") +
            tlv("59", a.receiver) + tlv("60", a.city) + tlv("62", tlv("05", txid)) + "6304"
        return (body + crc16(body)).also { validate(it) }
    }
    private fun parse(text: String): LinkedHashMap<String, String> {
        val fields = linkedMapOf<String, String>(); var offset = 0
        while (offset < text.length) {
            require(offset + 4 <= text.length)
            val id = text.substring(offset, offset + 2); val length = text.substring(offset + 2, offset + 4)
            require(id.matches(Regex("[0-9]{2}")) && length.matches(Regex("[0-9]{2}")))
            val end = offset + 4 + length.toInt(); require(end <= text.length && end > offset + 4)
            require(fields.put(id, text.substring(offset + 4, end)) == null)
            offset = end
        }
        return fields
    }
    fun validate(payload: String): Parsed {
        require(payload.length <= 512 && payload.all { it.code in 32..126 })
        val f = parse(payload)
        require(f.keys.last() == "63" && payload.takeLast(8).startsWith("6304") && f["63"] == crc16(payload.dropLast(4))) { "CRC Pix inválido" }
        require(f["00"] == "01" && f["52"] == "0000" && f["53"] == "986" && f["58"] == "BR")
        val account = parse(requireNotNull(f["26"]))
        require(account["00"].equals("br.gov.bcb.pix", true) && account.keys == setOf("00", "01"))
        val key = requireNotNull(account["01"]); require(key.length in 1..77)
        require(PixKeyType.values().any { type -> runCatching { PixValidation.key(type, key) }.isSuccess }) { "Formato de chave Pix inválido" }
        val txid = requireNotNull(parse(requireNotNull(f["62"]))["05"])
        require(txid == "***" || txid.matches(Regex("[A-Za-z0-9]{1,25}")))
        val name = requireNotNull(f["59"]); val city = requireNotNull(f["60"])
        require(name.length in 1..25 && city.length in 1..15)
        val amount = f["54"]?.let { require(it.matches(Regex("[0-9]+\\.[0-9]{2}"))); BigDecimal(it).movePointRight(2).intValueExact().also { c -> require(c > 0) } }
        return Parsed(key, txid, amount, name, city)
    }
}

/** Server-side provider boundary. Configure only after a bank contract and verified sandbox tests. */
interface DynamicPixProvider {
    val available: Boolean
    suspend fun create(orderId: String, amountCents: Int, idempotencyKey: String): PixQuote
    suspend fun query(txid: String): PixApiStatus
    suspend fun cancel(txid: String)
    suspend fun refund(endToEndId: String, amountCents: Int, idempotencyKey: String)
}
data class PixApiStatus(val txid: String, val amountCents: Int, val status: String, val endToEndId: String?)
object UnconfiguredDynamicPixProvider : DynamicPixProvider {
    override val available = false
    private fun unavailable(): Nothing = error("Pix por API indisponível: contrato bancário e backend seguro não configurados")
    override suspend fun create(orderId: String, amountCents: Int, idempotencyKey: String): PixQuote = unavailable()
    override suspend fun query(txid: String): PixApiStatus = unavailable()
    override suspend fun cancel(txid: String): Unit = unavailable()
    override suspend fun refund(endToEndId: String, amountCents: Int, idempotencyKey: String): Unit = unavailable()
}
