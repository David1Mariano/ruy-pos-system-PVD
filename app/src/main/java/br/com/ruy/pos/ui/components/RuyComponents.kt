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
import br.com.ruy.pos.ui.theme.RuyRed
import br.com.ruy.pos.ui.theme.RuyWhite

@Composable
fun RuyHeader(title: String, subtitle: String = "Ruy Caldo de Cana") {
    Column(
        modifier = Modifier.fillMaxWidth().background(RuyWhite).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = RuyRed)
        Text(text = subtitle, fontSize = 13.sp, color = RuyBlack)
        Spacer(modifier = Modifier.height(4.dp))
        Divider(color = RuyRed.copy(alpha = 0.2f))
    }
}

@Composable
fun RuyPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = RuyRed, contentColor = RuyWhite)
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun RuyCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp), content = content)
    }
}
