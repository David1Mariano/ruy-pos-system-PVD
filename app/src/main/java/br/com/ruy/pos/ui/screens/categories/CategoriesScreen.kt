package br.com.ruy.pos.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.data.local.CategoryEntity
import br.com.ruy.pos.ui.components.RuyCard
import br.com.ruy.pos.ui.components.RuyHeader
import br.com.ruy.pos.ui.components.RuyPrimaryButton
import br.com.ruy.pos.ui.theme.RuyBackground
import br.com.ruy.pos.ui.theme.RuyTextPrimary
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun CategoriesScreen(repo: Repository, onBack: () -> Unit, onBackup: () -> Unit = {}, onReports: () -> Unit = {}, onSettings: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }

    LaunchedEffect(Unit) {
        items = repo.categories()
    }

    Column(modifier = Modifier.fillMaxSize().background(RuyBackground)) {
        RuyHeader(title = "Categorias", subtitle = "Organizar cardápio")
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            RuyCard {
                Text(if (editing == null) "Nova categoria" else "Editar categoria", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RuyPrimaryButton(text = if (editing == null) "Salvar" else "Atualizar", onClick = {
                        scope.launch {
                            val id = editing?.id ?: UUID.randomUUID().toString()
                            repo.saveCategory(id, name)
                            name = ""
                            editing = null
                            items = repo.categories()
                        }
                    }, modifier = Modifier.weight(1f))
                    if (editing != null) {
                        OutlinedButton(onClick = { editing = null; name = "" }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Cancelar") }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Categorias", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(items) { c ->
                    RuyCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(c.name, style = MaterialTheme.typography.bodyMedium, color = RuyTextPrimary)
                            TextButton(onClick = { editing = c; name = c.name }) { Text("Editar") }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            RuyPrimaryButton(text = "Configurações", onClick = onSettings)
            Spacer(modifier = Modifier.height(8.dp))
            RuyPrimaryButton(text = "Backup / Restaurar", onClick = onBackup)
            Spacer(modifier = Modifier.height(8.dp))
            RuyPrimaryButton(text = "Relatórios", onClick = onReports)
            Spacer(modifier = Modifier.height(8.dp))
            RuyPrimaryButton(text = "Voltar", onClick = onBack)
        }
    }
}

