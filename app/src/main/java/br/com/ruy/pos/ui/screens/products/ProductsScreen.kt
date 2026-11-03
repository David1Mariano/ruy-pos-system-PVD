package br.com.ruy.pos.ui.screens.products

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
import br.com.ruy.pos.data.local.ProductLocalEntity
import br.com.ruy.pos.ui.components.RuyCard
import br.com.ruy.pos.ui.components.RuyHeader
import br.com.ruy.pos.ui.components.RuyPrimaryButton
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(repo: Repository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<ProductLocalEntity>>(emptyList()) }
    var editing by remember { mutableStateOf<ProductLocalEntity?>(null) }
    var categories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        items = repo.productLocalAll()
        categories = repo.categories()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        RuyHeader(title = "Produtos")
        Column(modifier = Modifier.padding(12.dp)) {
            RuyCard {
                Text(text = "Novo/Editar produto", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Preço (centavos)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = categories.firstOrNull { it.id == categoryId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { c ->
                            DropdownMenuItem(text = { Text(c.name) }, onClick = { categoryId = c.id; expanded = false })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RuyPrimaryButton(text = if (editing == null) "Salvar" else "Atualizar", onClick = {
                        scope.launch {
                            val id = editing?.id ?: UUID.randomUUID().toString()
                            val pc = price.toIntOrNull() ?: 0
                            repo.saveProductLocal(id, categoryId, name, pc, true)
                            name = ""
                            price = ""
                            categoryId = null
                            editing = null
                            items = repo.productLocalAll()
                        }
                    }, modifier = Modifier.weight(1f))
                    if (editing != null) {
                        TextButton(onClick = { editing = null; name = ""; price = ""; categoryId = null }) { Text("Cancelar") }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { p ->
                    RuyCard {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = p.name, fontWeight = FontWeight.Medium)
                                TextButton(onClick = { editing = p; name = p.name; price = p.priceCents.toString(); categoryId = p.categoryId }) { Text("Editar") }
                            }
                            Text(text = "R$ " + (p.priceCents / 100.0), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            RuyPrimaryButton(text = "Voltar", onClick = onBack)
        }
    }
}
