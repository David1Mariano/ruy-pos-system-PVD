package br.com.ruy.pos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.ruy.pos.ui.theme.RuyBlack
import br.com.ruy.pos.ui.theme.RuyBorder
import br.com.ruy.pos.ui.theme.RuyRed
import br.com.ruy.pos.ui.theme.RuyTextPrimary
import br.com.ruy.pos.ui.theme.RuyTextSecondary
import br.com.ruy.pos.ui.theme.RuyWhite

@Composable
fun RuyHeader(title: String, subtitle: String = "Ruy Caldo de Cana") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RuyWhite)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = RuyRed)
        Text(text = subtitle, fontSize = 12.sp, color = RuyTextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = RuyBorder)
    }
}

@Composable
fun RuyPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = RuyRed, contentColor = RuyWhite, disabledContainerColor = RuyBorder, disabledContentColor = RuyTextSecondary)
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun RuySecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(RuyBorder)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = RuyTextPrimary)
    ) {
        Text(text = text, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun RuyCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, RuyBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), content = content)
    }
}

@Composable
fun RuySectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, color = RuyTextPrimary)
}

@Composable
fun RuyPriceText(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, color = RuyRed, fontWeight = FontWeight.SemiBold)
}

