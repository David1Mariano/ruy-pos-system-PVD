package br.com.ruy.pos.report

import br.com.ruy.pos.ui.components.Money
import org.junit.Assert.*
import org.junit.Test

class MoneyTest {
    @Test fun decimalEntryUsesExactIntegerCents() {
        assertEquals(2300, Money.cents("23,00")); assertEquals(8, Money.cents("0,08"))
        assertEquals(10000, Money.cents("100")); assertEquals(123456, Money.cents("1.234,56"))
        assertEquals(2300, Money.cents("23.00")); assertEquals("23,00", Money.input(2300))
        for (bad in listOf("-1", "1,001", "NaN", "1e9", "1.2.3", "21474836,48", "", "1,2,3")) assertNull(Money.cents(bad))
    }
}
