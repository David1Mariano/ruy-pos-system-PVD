package br.com.ruy.pos.report

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import br.com.ruy.pos.data.local.CashClosingEntity
import br.com.ruy.pos.data.local.CashMovementEntity
import br.com.ruy.pos.data.local.PaymentEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReport {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val LINE_HEIGHT = 15f

    fun generateCashClosing(context: Context, closing: CashClosingEntity, movements: List<CashMovementEntity>,
                            payments: List<PaymentEntity>, operator: String): File {
        val doc = PdfDocument()
        val title = android.graphics.Paint().apply { isAntiAlias = true; textSize = 16f; setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)) }
        val body = android.graphics.Paint().apply { isAntiAlias = true; textSize = 10f }
        val small = android.graphics.Paint().apply { isAntiAlias = true; textSize = 8f }
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
        val reais: (Long) -> String = { NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(it / 100.0) }

        val page = doc.startPage(PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val canvas = page.canvas
        var y = MARGIN
        fun line(text: String, paint: android.graphics.Paint) { canvas.drawText(text, MARGIN, y, paint); y += LINE_HEIGHT }

        line("Relatório de Fechamento de Caixa", title); y += 6f
        line("Caixa #${closing.closingId.take(8)} • ${sdf.format(Date(closing.closedAt))}", body); y += 4f
        line("Operador: $operator", body); y += 8f
        line("Dinheiro contado: ${reais(closing.countedCashCents.toLong())}", body)
        line("Saldo esperado: ${reais(closing.expectedCashCents.toLong())}", body)
        line("Diferença: ${reais(closing.differenceCents.toLong())}", body)
        y += 6f
        line("Total vendas: ${reais(closing.totalSalesCents.toLong())}", body)
        line("Dinheiro: ${reais(closing.totalCashCents.toLong())} • Pix: ${reais(closing.totalPixCents.toLong())} • Débito: ${reais(closing.totalDebitCents.toLong())} • Crédito: ${reais(closing.totalCreditCents.toLong())}", body)
        line("Sangrias: ${reais(closing.sangriasCents.toLong())} • Suprimentos: ${reais(closing.suppliesCents.toLong())}", body)
        y += 6f
        line("Movimentações:", title)
        movements.forEach { line("  ${it.type}: ${reais(it.amountCents.toLong())} • ${it.reason ?: ""}", body) }
        y += 6f
        line("Pagamentos:", title)
        payments.forEach { line("  ${it.method} ${reais(it.amountCents.toLong())} • ${it.status}", body) }
        y += 6f
        line("Gerado pelo Ruy POS System • ${sdf.format(Date())}", small)
        doc.finishPage(page)
        val out = File(context.cacheDir, "caixa_${closing.closingId.take(8)}.pdf")
        ByteArrayOutputStream().use { baos ->
            doc.writeTo(baos)
            out.writeBytes(baos.toByteArray())
        }
        doc.close()
        return out
    }
}