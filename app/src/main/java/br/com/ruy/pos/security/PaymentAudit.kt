package br.com.ruy.pos.security

/** Reject likely PAN input in audit fields. These fields are for merchant references, never card data. */
object PaymentAudit {
    private fun luhn(number: String): Boolean {
        var sum = 0
        number.reversed().forEachIndexed { index, c -> var digit = c.digitToInt(); if (index % 2 == 1) { digit *= 2; if (digit > 9) digit -= 9 }; sum += digit }
        return sum % 10 == 0
    }
    fun validate(vararg values: String) {
        val pattern = Regex("(?<![A-Za-z0-9])(?:[0-9][ -]?){13,19}(?![A-Za-z0-9])")
        values.forEach { value ->
            require(value.none { it.isISOControl() }) { "Informe somente a referência e a conferência do comprovante" }
            pattern.findAll(value).forEach { match -> require(!luhn(match.value.filter { it.isDigit() })) { "Não informe número de cartão nos campos de comprovante ou auditoria" } }
        }
    }
}
