package br.com.ruy.pos.payment.pix

import java.text.Normalizer
import java.util.Locale

enum class PixKeyType(val label: String) { CPF("CPF"), CNPJ("CNPJ"), EMAIL("E-mail"), PHONE("Celular"), RANDOM("Aleatória") }

data class PixAccount(val institution: String, val receiver: String, val taxId: String, val key: String,
    val keyType: PixKeyType, val label: String, val city: String, val registeredConfirmed: Boolean) {
    fun validated(): PixAccount {
        require(institution.trim().length in 2..80 && label.trim().length in 2..80) { "Informe a instituição e o nome da conta" }
        val name = PixValidation.emvText(receiver, 25)
        val town = PixValidation.emvText(city, 15)
        val document = taxId.trim().replace(Regex("[./ -]"), "").uppercase(Locale.ROOT)
        require(document.isEmpty() || PixValidation.cpf(document) || PixValidation.cnpj(document)) { "CPF/CNPJ inválido" }
        require(registeredConfirmed) { "Confirme que a chave está cadastrada no banco em nome do recebedor" }
        return copy(institution = institution.trim(), receiver = name, taxId = document, key = PixValidation.key(keyType, key), label = label.trim(), city = town)
    }
}

object PixValidation {
    fun emvText(value: String, max: Int): String {
        val text = Normalizer.normalize(value.trim(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").uppercase(Locale.ROOT)
        require(text.length in 1..max && text.all { it.code in 32..126 } && text.any { it.isLetterOrDigit() }) {
            "Nome/cidade deve ter de 1 a $max caracteres, sem símbolos não suportados"
        }
        return text
    }
    fun cpf(s: String): Boolean {
        if (!s.matches(Regex("[0-9]{11}")) || s.toSet().size == 1) return false
        fun digit(n: Int): Int { val r = (0 until n).sumOf { s[it].digitToInt() * (n + 1 - it) } % 11; return if (r < 2) 0 else 11 - r }
        return digit(9) == s[9].digitToInt() && digit(10) == s[10].digitToInt()
    }
    fun cnpj(s: String): Boolean {
        if (!s.matches(Regex("[0-9A-Z]{12}[0-9]{2}")) || s.toSet().size == 1) return false
        fun digit(n: Int): Int {
            val weights = if (n == 12) intArrayOf(5,4,3,2,9,8,7,6,5,4,3,2) else intArrayOf(6,5,4,3,2,9,8,7,6,5,4,3,2)
            val r = (0 until n).sumOf { (s[it].code - 48) * weights[it] } % 11
            return if (r < 2) 0 else 11 - r
        }
        return digit(12) == s[12].digitToInt() && digit(13) == s[13].digitToInt()
    }
    fun key(type: PixKeyType, input: String): String {
        val value = when (type) {
            PixKeyType.CPF, PixKeyType.CNPJ -> input.trim().replace(Regex("[./ -]"), "").uppercase(Locale.ROOT)
            PixKeyType.PHONE -> input.trim().replace(Regex("[() -]"), "")
            else -> input.trim().lowercase(Locale.ROOT)
        }
        val valid = when (type) {
            PixKeyType.CPF -> cpf(value)
            PixKeyType.CNPJ -> cnpj(value)
            PixKeyType.PHONE -> value.matches(Regex("\\+55[1-9][0-9]9[0-9]{8}"))
            PixKeyType.RANDOM -> value.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
            PixKeyType.EMAIL -> value.length <= 77 && value.substringBefore('@').length <= 64 && value.matches(Regex("[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+")) && !value.startsWith('.') && !value.substringBefore('@').endsWith('.') && !value.contains("..")
        }
        require(valid && value.length in 1..77 && value.all { it.code in 33..126 }) { "Chave Pix inválida para o tipo selecionado" }
        return value
    }
}
