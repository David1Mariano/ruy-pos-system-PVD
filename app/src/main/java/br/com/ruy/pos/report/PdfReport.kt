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

        var pageNumber = 1
        var page = doc.startPage(PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN
        fun watermark() {
            if (br.com.ruy.pos.BuildConfig.HOMOLOGATION) {
                canvas.drawText("HOMOLOGAÇÃO — DADOS FICTÍCIOS — SEM VALOR FINANCEIRO", MARGIN, y, small)
                y += LINE_HEIGHT
            }
        }
        watermark()
        fun line(text: String, paint: android.graphics.Paint) {
            var rest = text.ifEmpty { " " }
            while (rest.isNotEmpty()) {
                if (y > PAGE_HEIGHT - MARGIN) {
                    doc.finishPage(page)
                    page = doc.startPage(PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, ++pageNumber).create())
                    canvas = page.canvas; y = MARGIN; watermark()
                }
                val count = paint.breakText(rest, true, PAGE_WIDTH - 2 * MARGIN, null).coerceAtLeast(1)
                canvas.drawText(rest.take(count), MARGIN, y, paint)
                y += LINE_HEIGHT; rest = rest.drop(count)
            }
        }

        line("Relatório de Fechamento de Caixa", title); y += 6f
        line("Caixa #${closing.closingId.take(8)} • ${sdf.format(Date(closing.closedAt))}", body); y += 4f
        line("Operador: $operator", body); y += 8f
        line("Dinheiro contado: ${reais(closing.countedCashCents.toLong())}", body)
        line("Saldo esperado: ${reais(closing.expectedCashCents.toLong())}", body)
        line("Diferença: ${reais(closing.differenceCents.toLong())}", body)
        y += 6f
        line("Total vendas: ${reais(closing.totalSalesCents.toLong())}", body)
        val breakdown = PaymentBreakdowns.calculate(payments.filter { it.cashSessionId == closing.sessionId })
        if (breakdown.receivedCents == closing.totalSalesCents.toLong()) PaymentBreakdowns.labels.forEach { (mode, label) ->
            if (mode != "SIMULATED" || br.com.ruy.pos.BuildConfig.HOMOLOGATION) line("$label: ${reais(breakdown.received[mode] ?: 0L)}", body)
        } else {
            line("Detalhamento por origem indisponível neste fechamento histórico.", body)
            line("Dinheiro: ${reais(closing.totalCashCents.toLong())} • Pix: ${reais(closing.totalPixCents.toLong())}", body)
            line("Débito: ${reais(closing.totalDebitCents.toLong())} • Crédito: ${reais(closing.totalCreditCents.toLong())}", body)
        }
        line("Pendentes: ${reais(breakdown.pendingCents)} • Cancelados/sem recebimento: ${reais(breakdown.cancelledCents)}", body)
        line("Estornos/devoluções registrados: ${reais(breakdown.refundedCents)}", body)
        line("Valores brutos. Taxas e estornos são tratados no provedor; não calculados ou executados neste relatório.", small)
        line("Sangrias: ${reais(closing.sangriasCents.toLong())} • Suprimentos: ${reais(closing.suppliesCents.toLong())}", body)
        y += 6f
        line("Movimentações:", title)
        movements.filter { it.sessionId == closing.sessionId }.forEach { line("  ${br.com.ruy.pos.ui.components.OperationalText.movement(it.type)}: ${reais(it.amountCents.toLong())} • ${it.reason ?: ""}", body) }
        y += 6f
        line("Pagamentos:", title)
        payments.filter { it.cashSessionId == closing.sessionId }.forEach { line("  ${br.com.ruy.pos.ui.components.OperationalText.method(it.method)} ${reais(it.amountCents.toLong())} • ${br.com.ruy.pos.ui.components.OperationalText.status(it.status)} • ${it.installments} parcela(s)", body) }
        y += 6f
        line("Gerado pelo Ruy POS System • ${sdf.format(Date())}", small)
        doc.finishPage(page)
        val out = ShareFiles.exportFile(context, "caixa_${closing.closingId.take(8)}.pdf")
        ByteArrayOutputStream().use { baos ->
            doc.writeTo(baos)
            out.writeBytes(baos.toByteArray())
        }
        doc.close()
        return out
    }
}
