package br.com.ruy.pos.ui.screens.reports

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.data.local.CashClosingEntity
import br.com.ruy.pos.data.local.CashMovementEntity
import br.com.ruy.pos.data.local.OrderEntity
import br.com.ruy.pos.data.local.PaymentEntity
import br.com.ruy.pos.report.PdfReport
import br.com.ruy.pos.report.ReportCalculator
import br.com.ruy.pos.ui.components.RuyCard
import br.com.ruy.pos.ui.components.RuyHeader
import br.com.ruy.pos.ui.components.RuyPrimaryButton
import br.com.ruy.pos.ui.theme.RuyBackground
import br.com.ruy.pos.ui.theme.RuyRed
import br.com.ruy.pos.ui.theme.RuyTextSecondary
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun ReportsScreen(repo: Repository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var orders by remember { mutableStateOf<List<OrderEntity>>(emptyList()) }
    var payments by remember { mutableStateOf<List<PaymentEntity>>(emptyList()) }
    var closings by remember { mutableStateOf<List<CashClosingEntity>>(emptyList()) }
    var movements by remember { mutableStateOf<List<CashMovementEntity>>(emptyList()) }
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    fun reais(c: Int): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(c / 100.0)
    fun reaisL(c: Long): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(c / 100.0)

    LaunchedEffect(Unit) {
        orders = repo.allOrders()
        payments = repo.getAllPayments()
        closings = repo.getCashClosings()
        repo.getOpenCashSession()?.let { movements = repo.getCashMovements(it.sessionId) }
    }

    val report = remember(orders, payments) { ReportCalculator.sales(orders, payments) }

    Column(modifier = Modifier.fillMaxSize().background(RuyBackground)) {
        RuyHeader(title = "Relatórios", subtitle = "Vendas e fechamentos")
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            RuyCard {
                Text("Resumo do dia", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total vendas", style = MaterialTheme.typography.bodyMedium); Text(reaisL(report.totalSalesCents), style = MaterialTheme.typography.bodyMedium, color = RuyRed, fontWeight = FontWeight.SemiBold) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Pedidos", style = MaterialTheme.typography.bodyMedium); Text(report.totalOrders.toString(), style = MaterialTheme.typography.bodyMedium) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Ticket médio", style = MaterialTheme.typography.bodyMedium); Text(reaisL(report.ticketMedioCents), style = MaterialTheme.typography.bodyMedium) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Pendentes", style = MaterialTheme.typography.bodyMedium); Text(report.pending.toString(), style = MaterialTheme.typography.bodyMedium) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Cancelados", style = MaterialTheme.typography.bodyMedium); Text(report.cancelled.toString(), style = MaterialTheme.typography.bodyMedium) }
            }
            Spacer(modifier = Modifier.height(10.dp))
            RuyCard {
                Text("Por forma de pagamento", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                report.byMethod.forEach { (method, total) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(method, style = MaterialTheme.typography.bodyMedium); Text(reaisL(total), style = MaterialTheme.typography.bodyMedium) } }
            }
            Spacer(modifier = Modifier.height(10.dp))
            RuyCard {
                Text("Fechamentos", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                if (closings.isEmpty()) Text("Nenhum fechamento", style = MaterialTheme.typography.bodySmall, color = RuyTextSecondary)
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(closings) { c ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${sdf.format(Date(c.closedAt))} • ${c.operator}", style = MaterialTheme.typography.bodySmall)
                            Text("Dif: ${reais(c.differenceCents)}", style = MaterialTheme.typography.bodySmall, color = if (c.differenceCents == 0) RuyTextSecondary else RuyRed)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            RuyPrimaryButton(text = "Exportar relatório PDF", onClick = {
                scope.launch {
                    try {
                        val session = repo.getOpenCashSession()
                        val mv = session?.let { repo.getCashMovements(it.sessionId) } ?: movements
                        val pay = payments
                        val c = closings.firstOrNull() ?: return@launch
                        val f = PdfReport.generateCashClosing(context, c, mv, pay, c.operator)
                        val share = br.com.ruy.pos.report.ShareFiles.intent(context, f, "application/pdf")
                        context.startActivity(Intent.createChooser(share, "Compartilhar relatório"))
                        Toast.makeText(context, "Relatório gerado", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) { Toast.makeText(context, e.message ?: "Falha no PDF", Toast.LENGTH_LONG).show() }
                }
            })
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) { Text("Voltar") }
        }
    }
}