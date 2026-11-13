package br.com.ruy.pos.ui.screens.checkout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.data.local.*
import br.com.ruy.pos.payment.PaymentCoordinator
import br.com.ruy.pos.BuildConfig

@Composable
fun TapStoneDialog(a: StoneAttemptEntity, payment: PaymentEntity, busy: Boolean, error: String?,
    onDismiss: () -> Unit, onConfirm: (PaymentCoordinator.TapStoneProof) -> Unit) {
    var order by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("") }
    var operator by remember { mutableStateOf("") }
    var evidence by remember { mutableStateOf("") }
    var receipt by remember { mutableStateOf("") }
    var received by remember { mutableStateOf(true) }
    var checked by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("PAGAMENTO POR APROXIMAÇÃO — STONE EXTERNO") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (BuildConfig.HOMOLOGATION) Text("HOMOLOGAÇÃO: somente evidências fictícias. Não efetue cobrança.")
            Text("Pedido: ${a.orderId}")
            Text("Valor: ${payment.amountCents} centavos • ${payment.method} • à vista")
            Text("Status: ${a.state}. Referência local: ${a.stoneOrderId}.")
            Text("Acesse manualmente o aplicativo oficial Stone: Vender > TapStone. Informe exatamente o valor acima e a modalidade. Confira a aprovação e o comprovante antes de voltar. O Ruy POS não inicia nem verifica a cobrança.")
            Text("Se houver recusa, abandono, falta de internet ou resultado indeterminado, mantenha pendente até conferir o extrato. Voltar nunca registra recebimento.")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(received, { received = true; checked = false }, label = { Text("Recebido") }, enabled = !busy)
                FilterChip(!received, { received = false; checked = false }, label = { Text("Não recebido") }, enabled = !busy)
            }
            OutlinedTextField(order, { order = it }, label = { Text("Pedido local conferido (UUID completo)") })
            OutlinedTextField(amount, { amount = it }, label = { Text("Valor conferido (centavos)") })
            OutlinedTextField(method, { method = it }, label = { Text("Modalidade conferida (TAPSTONE_DEBIT/CREDIT)") })
            if (received) OutlinedTextField(receipt, { receipt = it }, label = { Text("Referência única do comprovante aprovado") })
            OutlinedTextField(operator, { operator = it }, label = { Text("Operador responsável") })
            OutlinedTextField(evidence, { evidence = it }, label = { Text("Extrato consultado: data/hora e referência") })
            Row {
                Checkbox(checked, { checked = it }, enabled = !busy)
                Text(if (received) "Conferi a aprovação, valor, modalidade e comprovante deste pedido"
                     else "Conferi no extrato: sem recebimento e sem transação em andamento")
            }
            Text("Confirmação manual auditada. Não informe número, nome, validade, senha ou código de segurança do cartão.")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(enabled = !busy && checked, onClick = {
        onConfirm(PaymentCoordinator.TapStoneProof(a.stoneOrderId, order.trim(), amount.toIntOrNull() ?: -1,
            method.trim(), received, operator, evidence, receipt, checked))
    }) { Text("Registrar conferência manual") } }, dismissButton = {
        TextButton(enabled = !busy, onClick = onDismiss) { Text("Voltar mantendo pendente") }
    })
}
