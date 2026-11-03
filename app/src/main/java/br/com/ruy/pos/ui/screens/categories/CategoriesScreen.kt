package br.com.ruy.pos.ui.screens.categories

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.data.local.CategoryEntity
import br.com.ruy.pos.ui.components.RuyCard
import br.com.ruy.pos.ui.components.RuyHeader
import br.com.ruy.pos.ui.components.RuyPrimaryButton
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun CategoriesScreen(repo: Repository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }

    LaunchedEffect(Unit) {
        items = repo.categories()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        RuyHeader(title = "Categorias")
        Column(modifier = Modifier.padding(12.dp)) {
            RuyCard {
                Text("Nova/Editar categoria", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        TextButton(onClick = { editing = null; name = "" }) { Text("Cancelar") }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { c ->
                    RuyCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(c.name, fontWeight = FontWeight.Medium)
                            TextButton(onClick = { editing = c; name = c.name }) { Text("Editar") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            RuyPrimaryButton(text = "Voltar", onClick = onBack)
        }
    }
}
