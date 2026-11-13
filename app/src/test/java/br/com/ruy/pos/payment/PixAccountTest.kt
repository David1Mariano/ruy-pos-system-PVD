package br.com.ruy.pos.payment

import br.com.ruy.pos.payment.pix.*
import org.junit.Assert.*
import org.junit.Test

class PixAccountTest {
    @Test fun typesDocumentsAndNormalizationFollowApplicableFormats() {
        assertTrue(PixValidation.cpf("12345678909")); assertFalse(PixValidation.cpf("11111111111"))
        assertTrue(PixValidation.cnpj("12ABC34501DE35")); assertFalse(PixValidation.cnpj("12ABC34501DE36"))
        assertEquals("+5561912345678", PixValidation.key(PixKeyType.PHONE, "+55 (61) 91234-5678"))
        assertEquals("demo@example.invalid", PixValidation.key(PixKeyType.EMAIL, "Demo@Example.invalid"))
        assertEquals("SAO PAULO", PixValidation.emvText("São Paulo", 15))
        for ((type, key) in listOf(PixKeyType.EMAIL to "a@", PixKeyType.EMAIL to "a..b@example.invalid", PixKeyType.CPF to "123", PixKeyType.RANDOM to "1234", PixKeyType.PHONE to "61912345678"))
            assertThrows(IllegalArgumentException::class.java) { PixValidation.key(type, key) }
    }
    @Test fun accountRequiresRegistrationAttestationAndSafeFieldLengths() {
        val a = PixAccount("Banco fictício", "Loja Fictícia", "", "demo@example.invalid", PixKeyType.EMAIL, "Conta de teste", "São Paulo", true)
        assertEquals("LOJA FICTICIA", a.validated().receiver)
        assertThrows(IllegalArgumentException::class.java) { a.copy(registeredConfirmed = false).validated() }
        assertThrows(IllegalArgumentException::class.java) { a.copy(receiver = "X".repeat(26)).validated() }
        assertThrows(IllegalArgumentException::class.java) { a.copy(taxId = "123").validated() }
    }
}
