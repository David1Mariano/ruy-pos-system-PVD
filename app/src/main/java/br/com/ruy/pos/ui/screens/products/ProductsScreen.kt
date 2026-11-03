package br.com.ruy.pos.ui.screens.products

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.data.local.CategoryEntity
import br.com.ruy.pos.data.local.ProductLocalEntity
import br.com.ruy.pos.ui.components.RuyCard
import br.com.ruy.pos.ui.components.RuyHeader
import br.com.ruy.pos.ui.components.RuyPriceText
import br.com.ruy.pos.ui.components.RuyPrimaryButton
import br.com.ruy.pos.ui.theme.RuyBackground
import br.com.ruy.pos.ui.theme.RuyTextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(repo: Repository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<ProductLocalEntity>>(emptyList()) }
    var editing by remember { mutableStateOf<ProductLocalEntity?>(null) }
    var categories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var imagePath by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val input = context.contentResolver.openInputStream(uri) ?: return@launch
                    val dir = File(context.filesDir, "product_images")
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, "${UUID.randomUUID()}.jpg")
                    val out = FileOutputStream(file)
                    input.copyTo(out)
                    input.close()
                    out.close()
                    withContext(Dispatchers.Main) { imagePath = file.absolutePath }
                } catch (e: Exception) {}
            }
        }
    }

    fun reais(c: Int): String = NumberFormat.getCurrencyInstance(Locale("pt","BR")).format(c/100.0)

    LaunchedEffect(Unit) {
        items = repo.productLocalAll()
        categories = repo.categories()
    }

    val filtered = items.filter { it.name.contains(search, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().background(RuyBackground)) {
        RuyHeader(title = "Produtos", subtitle = "Cadastro e catálogo")
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Buscar produto...") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(10.dp))
            RuyCard {
                Text(if (editing == null) "Novo produto" else "Editar produto", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Preço (centavos)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(value = categories.firstOrNull { it.id == categoryId }?.name ?: "", onValueChange = {}, readOnly = true, label = { Text("Categoria") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { c -> DropdownMenuItem(text = { Text(c.name) }, onClick = { categoryId = c.id; expanded = false }) }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, shape = RoundedCornerShape(10.dp)) { Text("Selecionar foto") }
                    if (imagePath != null) { TextButton(onClick = { imagePath = null }) { Text("Remover") } }
                }
                val bmp = remember(imagePath) { if (imagePath != null) try { BitmapFactory.decodeFile(imagePath)?.asImageBitmap() } catch (e: Exception) { null } else null }
                if (bmp != null) {
                    Image(bitmap = bmp, contentDescription = null, modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RuyPrimaryButton(text = if (editing == null) "Salvar" else "Atualizar", onClick = {
                        scope.launch {
                            val id = editing?.id ?: UUID.randomUUID().toString()
                            val pc = price.toIntOrNull() ?: 0
                            repo.saveProductLocal(id, categoryId, name, pc, true, imagePath)
                            name = ""; price = ""; categoryId = null; editing = null; imagePath = null
                            items = repo.productLocalAll()
                        }
                    }, modifier = Modifier.weight(1f))
                    if (editing != null) { OutlinedButton(onClick = { editing = null; name = ""; price = ""; categoryId = null; imagePath = null }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Cancelar") } }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Lista de produtos", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(filtered) { p ->
                    RuyCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val pbmp = remember(p.imagePath) { if (p.imagePath != null) try { BitmapFactory.decodeFile(p.imagePath)?.asImageBitmap() } catch (e: Exception) { null } else null }
                                Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(RuyBackground)) {
                                    if (pbmp != null) { Image(bitmap = pbmp, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(p.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = RuyTextPrimary)
                                    RuyPriceText(reais(p.priceCents))
                                }
                            }
                            TextButton(onClick = { editing = p; name = p.name; price = p.priceCents.toString(); categoryId = p.categoryId; imagePath = p.imagePath }) { Text("Editar") }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            RuyPrimaryButton(text = "Voltar", onClick = onBack)
        }
    }
}
