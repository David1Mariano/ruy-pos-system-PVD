package br.com.ruy.pos.ui.screens.checkout

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.BuildConfig
import br.com.ruy.pos.data.local.*
import br.com.ruy.pos.payment.PaymentCoordinator
import br.com.ruy.pos.payment.pix.*
import br.com.ruy.pos.ui.components.Money
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun PixPaymentDialog(attempt: StoneAttemptEntity, payment: PaymentEntity, quote: PixQuote?, busy: Boolean,
    error: String?, onDismiss: () -> Unit, onConfirm: (PaymentCoordinator.ManualReceiptProof) -> Unit) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var review by remember { mutableStateOf(false) }
    var received by remember { mutableStateOf(true) }
    var amount by remember { mutableStateOf("") }
    var operator by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var evidence by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf(false) }
    LaunchedEffect(quote?.payload) {
        if (quote != null && !BuildConfig.HOMOLOGATION) bitmap = withContext(Dispatchers.Default) {
            val matrix = QRCodeWriter().encode(quote.payload, BarcodeFormat.QR_CODE, 480, 480, mapOf(EncodeHintType.MARGIN to 4, EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M))
            val pixels = IntArray(matrix.width * matrix.height) { index -> if (matrix[index % matrix.width, index / matrix.width]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }
            Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
        }
    }
    DisposableEffect(bitmap) { val image = bitmap; onDispose { image?.recycle() } }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Pix • aguardando pagamento") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Pedido ${attempt.orderId.take(8).uppercase()} • ${Money.format(payment.amountCents.toLong())}")
            quote?.let { Text("Recebedor: ${it.receiver}") }
            if (BuildConfig.HOMOLOGATION) Text("HOMOLOGAÇÃO • dados fictícios. Exibição, cópia e compartilhamento do QR bancário estão bloqueados.")
            else if (quote == null) Text("O código anterior não está disponível neste aparelho. Confira o recebimento no banco antes de encerrar o registro. Não gere outro código enquanto houver dúvida.")
            else if (!review) {
                bitmap?.let { Image(it.asImageBitmap(), contentDescription = "QR Code Pix da venda", modifier = Modifier.fillMaxWidth().aspectRatio(1f)) }
                Text("Confira o titular e o valor no aplicativo bancário antes de pagar.")
                Text(quote.payload, style = MaterialTheme.typography.bodySmall)
                Row {
                    TextButton(onClick = {
                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Pix Copia e Cola", quote.payload))
                    }) { Text("Copiar Pix") }
                    TextButton(onClick = { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, quote.payload) }, "Compartilhar Pix Copia e Cola")) }) { Text("Compartilhar") }
                }
            }
            Text("Gerar ou mostrar este QR não confirma recebimento. Confira o crédito no extrato do banco.")
            if (review) {
                Row {
                    FilterChip(received, { received = true; checked = false }, label = { Text("Recebido") })
                    FilterChip(!received, { received = false; checked = false }, label = { Text("Não recebido") })
                }
                if (!received) Text("O QR estático não expira e não pode ser cancelado no banco por este aplicativo. Se ele foi compartilhado, continue conferindo possíveis créditos posteriores.")
                OutlinedTextField(amount, { amount = it }, label = { Text("Valor conferido (R$)") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                if (received) OutlinedTextField(reference, { reference = it }, label = { Text("Referência do comprovante bancário") })
                OutlinedTextField(operator, { operator = it }, label = { Text("Operador responsável") })
                OutlinedTextField(evidence, { evidence = it }, label = { Text("Extrato consultado • data e hora") })
                Row { Checkbox(checked, { checked = it }); Text(if (received) "Conferi o crédito deste pedido no banco" else "Conferi o extrato: sem crédito e sem pagamento em andamento") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = {
        TextButton(enabled = !busy && (!review || checked), onClick = {
            if (!review) review = true else onConfirm(PaymentCoordinator.ManualReceiptProof(attempt.stoneOrderId, attempt.orderId,
                Money.cents(amount) ?: -1, received, operator, evidence, reference, checked))
        }) { Text(if (review) "Registrar conferência manual" else "Conferir recebimento") }
    }, dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Voltar mantendo pendente") } })
}
