package br.com.ruy.pos.payment

import br.com.ruy.pos.payment.pix.*
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.*
import org.junit.Test

class PixBrCodeTest {
    private val a = PixAccount("Banco fictício", "Loja Fictícia", "", "demo@example.invalid", PixKeyType.EMAIL, "Teste", "São Paulo", true)
    @Test fun officialBcbStaticExampleAndIndependentCrcVectorAreAccepted() {
        val official = "00020126580014br.gov.bcb.pix0136123e4567-e12b-12d1-a456-4266554400005204000053039865802BR5913Fulano de Tal6008BRASILIA62070503***63041D3D"
        val p = PixBrCode.validate(official)
        assertEquals("***", p.txid); assertNull(p.amountCents)
        assertEquals("1D3D", PixBrCode.crc16(official.dropLast(4)))
        assertEquals("29B1", PixBrCode.crc16("123456789"))
    }
    @Test fun exactAmountAndAsciiNormalizationRoundTripThroughQrEncoderAndDecoder() {
        val quote = StaticPixGenerator().generate(PixCodeRequest(a, 2300, "FICTICIO123"))
        val parsed = PixBrCode.validate(quote.payload)
        assertEquals(2300, parsed.amountCents); assertEquals(a.key, parsed.key)
        assertEquals("LOJA FICTICIA", parsed.receiver); assertEquals("SAO PAULO", parsed.city)
        assertTrue(quote.payload.contains("540523.00"))
        val matrix = QRCodeWriter().encode(quote.payload, BarcodeFormat.QR_CODE, 480, 480)
        val pixels = IntArray(matrix.width * matrix.height) { i -> if (matrix[i % matrix.width, i / matrix.width]) -0x1000000 else -1 }
        val decoded = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(matrix.width, matrix.height, pixels))))
        assertEquals(quote.payload, decoded.text)
        val fixtureDir = System.getenv("RUY_PIX_FIXTURE_DIR")
        if (!fixtureDir.isNullOrBlank() && br.com.ruy.pos.BuildConfig.DEBUG && !br.com.ruy.pos.BuildConfig.HOMOLOGATION) {
            val dir = java.io.File(fixtureDir).apply { mkdirs() }
            java.io.File(dir, "pix-ficticio-copia-cola.txt").writeText(quote.payload)
            val modules = QRCodeWriter().encode(quote.payload, BarcodeFormat.QR_CODE, 1, 1, mapOf(EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 4))
            val svg = buildString {
                append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"480\" height=\"480\" viewBox=\"0 0 ${modules.width} ${modules.height}\" shape-rendering=\"crispEdges\"><title>HOMOLOGACAO - CHAVE FICTICIA - SEM VALOR FINANCEIRO</title><rect width=\"100%\" height=\"100%\" fill=\"white\"/>")
                for (y in 0 until modules.height) for (x in 0 until modules.width) if (modules[x, y]) append("<rect x=\"$x\" y=\"$y\" width=\"1\" height=\"1\" fill=\"black\"/>")
                append("</svg>")
            }
            java.io.File(dir, "pix-ficticio-validado.svg").writeText(svg)
            java.io.File(dir, "LEIA-ANTES-DO-QR.txt").writeText("HOMOLOGAÇÃO: chave fictícia demo@example.invalid. Sem valor financeiro. Não é comprovante nem teste bancário. O payload e a imagem foram validados por CRC, parser BR Code e decodificação ZXing, sem comunicação com bancos.")
        }
    }
    @Test fun corruptionUnsupportedReferencesAndNonPositiveAmountsAreRejected() {
        val payload = PixBrCode.encode(a, 1, "FICTICIO1")
        assertEquals(1, PixBrCode.validate(payload).amountCents)
        assertThrows(IllegalArgumentException::class.java) { PixBrCode.validate(payload.dropLast(1) + if(payload.last() == '0') "1" else "0") }
        for (amount in listOf(0, -1)) assertThrows(IllegalArgumentException::class.java) { PixBrCode.encode(a, amount, "TESTE") }
        for (reference in listOf("X".repeat(26), "with-dash", "", "com espaço")) assertThrows(IllegalArgumentException::class.java) { PixBrCode.encode(a, 100, reference) }
        assertThrows(IllegalArgumentException::class.java) { PixBrCode.validate(payload + payload) }
    }
}
