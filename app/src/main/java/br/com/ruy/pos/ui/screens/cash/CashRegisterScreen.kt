package br.com.ruy.pos.ui.screens.cash

import android.content.Intent
import android.widget.Toast
import br.com.ruy.pos.report.PdfReport
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.data.local.CashClosingEntity
import br.com.ruy.pos.data.local.CashMovementEntity
import br.com.ruy.pos.ui.components.RuyCard
import br.com.ruy.pos.ui.components.RuyHeader
import br.com.ruy.pos.ui.components.RuyPrimaryButton
import br.com.ruy.pos.ui.theme.RuyBackground
import br.com.ruy.pos.ui.theme.RuyTextSecondary
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CashRegisterScreen(repo: Repository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    fun reais(c: Int): String = NumberFormat.getCurrencyInstance(Locale("pt","BR")).format(c/100.0)
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt","BR"))
    var openSession by remember { mutableStateOf<br.com.ruy.pos.data.local.CashSessionEntity?>(null) }
    var movements by remember { mutableStateOf<List<CashMovementEntity>>(emptyList()) }
    var closings by remember { mutableStateOf<List<CashClosingEntity>>(emptyList()) }
    var operator by remember { mutableStateOf("Operador") }
    var opening by remember { mutableStateOf("100,00") }
    var sangria by remember { mutableStateOf("") }
    var supply by remember { mutableStateOf("") }
    var counted by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val payments by remember { repo.observePayments() }.collectAsState(initial = emptyList())
    val breakdown = br.com.ruy.pos.report.PaymentBreakdowns.calculate(payments.filter { it.cashSessionId == openSession?.sessionId && openSession != null })
    fun registerMovement(type: String, text: String, onSaved: () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                repo.addCashMovement(requireNotNull(openSession).sessionId, type, requireNotNull(br.com.ruy.pos.ui.components.Money.cents(text)) { "Valor inválido" }, if (type == "SANGRIA") "Sangria" else "Suprimento")
                onSaved()
            } catch (e: Exception) { Toast.makeText(context, br.com.ruy.pos.ui.components.OperationalText.error(e), Toast.LENGTH_LONG).show() }
            finally { busy = false }
        }
    }

    LaunchedEffect(Unit) {
        openSession = repo.getOpenCashSession()
        openSession?.let { movements = repo.getCashMovements(it.sessionId) }
        closings = repo.getCashClosings()
    }
    LaunchedEffect(openSession?.sessionId) {
        openSession?.let { session -> repo.observeCashMovements(session.sessionId).collect { movements = it } }
    }

    Column(modifier = Modifier.fillMaxSize().background(RuyBackground)) {
        RuyHeader(title = "Caixa", subtitle = "Abertura, movimentações e fechamento")
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (openSession == null) {
                RuyCard {
                    Text("Abrir caixa", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = operator, onValueChange = { operator = it }, label = { Text("Operador") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = opening, onValueChange = { opening = it }, label = { Text("Saldo inicial (R$)") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    RuyPrimaryButton(text = "Abrir sessão", enabled = !busy, onClick = {
                        if (busy) return@RuyPrimaryButton
                        busy = true
                        scope.launch {
                            try {
                                repo.openCashSession(operator, requireNotNull(br.com.ruy.pos.ui.components.Money.cents(opening)) { "Saldo inicial inválido" })
                                openSession = repo.getOpenCashSession()
                                openSession?.let { movements = repo.getCashMovements(it.sessionId) }
                                Toast.makeText(context, "Caixa aberto", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) { Toast.makeText(context, br.com.ruy.pos.ui.components.OperationalText.error(e), Toast.LENGTH_LONG).show() }
                            finally { busy = false }
                        }
                    })
                }
            } else {
                RuyCard {
                    Text("Sessão aberta", style = MaterialTheme.typography.titleSmall)
                    Text("Operador: ${openSession!!.operator}", style = MaterialTheme.typography.bodySmall, color = RuyTextSecondary)
                    Text("Abertura: ${sdf.format(Date(openSession!!.openedAt))}", style = MaterialTheme.typography.bodySmall, color = RuyTextSecondary)
                    Text("Saldo inicial: ${reais(openSession!!.openingBalanceCents)}", style = MaterialTheme.typography.bodySmall)
                    br.com.ruy.pos.report.PaymentBreakdowns.labels.forEach { (key, label) ->
                        if (key != "SIMULATED" || br.com.ruy.pos.BuildConfig.HOMOLOGATION) Text("$label: ${br.com.ruy.pos.ui.components.Money.format(breakdown.received[key] ?: 0L)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Aguardando conferência: ${br.com.ruy.pos.ui.components.Money.format(breakdown.pendingCents)}", style = MaterialTheme.typography.bodySmall)
                    Text("Cancelados/sem recebimento: ${br.com.ruy.pos.ui.components.Money.format(breakdown.cancelledCents)}", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(10.dp))
                RuyCard {
                    Text("Movimentações", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = sangria, onValueChange = { sangria = it }, label = { Text("Sangria (R$)") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                        Button(enabled = !busy, onClick = {
                            registerMovement("SANGRIA", sangria) { sangria = "" }
                        }, shape = RoundedCornerShape(10.dp)) { Text("Registrar") }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = supply, onValueChange = { supply = it }, label = { Text("Suprimento (R$)") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                        Button(enabled = !busy, onClick = {
                            registerMovement("SUPRIMENTO", supply) { supply = "" }
                        }, shape = RoundedCornerShape(10.dp)) { Text("Registrar") }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 100.dp)) {
                        items(movements) { m -> Text("${br.com.ruy.pos.ui.components.OperationalText.movement(m.type)}: ${reais(m.amountCents)} - ${m.reason ?: ""}", style = MaterialTheme.typography.bodySmall) }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                RuyCard {
                    Text("Fechamento", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(value = counted, onValueChange = { counted = it }, label = { Text("Dinheiro contado (R$)") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    RuyPrimaryButton(text = "Fechar caixa", enabled = !busy, onClick = {
                        if (busy) return@RuyPrimaryButton
                        busy = true
                        scope.launch {
                            try {
                                val closing = repo.payments.closeCash(openSession!!.sessionId, requireNotNull(br.com.ruy.pos.ui.components.Money.cents(counted)) { "Informe o dinheiro contado" })
                                openSession = null
                                movements = emptyList()
                                closings = repo.getCashClosings()
                                Toast.makeText(context, "Caixa fechado. Diferença: ${reais(closing.differenceCents)}", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, br.com.ruy.pos.ui.components.OperationalText.error(e), Toast.LENGTH_LONG).show()
                            } finally { busy = false }
                        }
                    })
                }
            }
Spacer(modifier = Modifier.height(10.dp))
                Text("Histórico de fechamentos", style = MaterialTheme.typography.titleSmall)
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(closings) { c ->
                        RuyCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("Fechamento ${c.closingId.take(6)} - Diferença: ${reais(c.differenceCents)}", style = MaterialTheme.typography.bodySmall)
                                Text("Total vendas: ${reais(c.totalSalesCents)} • Dinheiro: ${reais(c.totalCashCents)}", style = MaterialTheme.typography.bodySmall, color = RuyTextSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(onClick = {
                                        scope.launch {
                                            try {
                                                val movements = repo.getCashMovements(c.sessionId)
                                                val payments = repo.getAllPayments().filter { it.cashSessionId == c.sessionId }
                                                val f: File = PdfReport.generateCashClosing(context, c, movements, payments, c.operator)
                                                val share = br.com.ruy.pos.report.ShareFiles.intent(context, f, "application/pdf")
                                                context.startActivity(Intent.createChooser(share, "Compartilhar relatório"))
                                                Toast.makeText(context, "Relatório gerado", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) { Toast.makeText(context, br.com.ruy.pos.ui.components.OperationalText.error(e), Toast.LENGTH_LONG).show() }
                                        }
                                    }, shape = RoundedCornerShape(10.dp)) { Text("Exportar PDF") }
                                }
                            }
                        }
                    }
                }
            Spacer(modifier = Modifier.height(8.dp))
            RuyPrimaryButton(text = "Voltar", onClick = onBack)
        }
    }
}
