package br.com.ruy.pos.ui.screens.products

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.data.local.CategoryEntity
import br.com.ruy.pos.data.local.ProductLocalEntity
import br.com.ruy.pos.ui.components.RuyCard
import br.com.ruy.pos.ui.components.RuyHeader
import br.com.ruy.pos.ui.components.RuyPrimaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val path = try {
                    val input = context.contentResolver.openInputStream(uri) ?: return@launch
                    val dir = File(context.filesDir, "product_images")
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, "${UUID.randomUUID()}.jpg")
                    val out = FileOutputStream(file)
                    input.copyTo(out)
                    input.close()
                    out.close()
                    withContext(Dispatchers.Main) { imagePath = file.absolutePath }
                    file.absolutePath
                } catch (e: Exception) {
                    null
                }
            }
        }
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
                    TextButton(onClick = { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Adicionar foto") }
                    if (imagePath != null) {
                        TextButton(onClick = { imagePath = null }) { Text("Remover foto") }
                    }
                }
                if (imagePath != null) {
                    val bmp = remember(imagePath) {
                        try { BitmapFactory.decodeFile(imagePath)?.asImageBitmap() } catch (e: Exception) { null }
                    }
                    if (bmp != null) {
                        Image(bitmap = bmp, contentDescription = null, modifier = Modifier.fillMaxWidth().height(120.dp), contentScale = ContentScale.Crop)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RuyPrimaryButton(text = if (editing == null) "Salvar" else "Atualizar", onClick = {
                        scope.launch {
                            val id = editing?.id ?: UUID.randomUUID().toString()
                            val pc = price.toIntOrNull() ?: 0
                            repo.saveProductLocal(id, categoryId, name, pc, true, imagePath)
                            name = ""
                            price = ""
                            categoryId = null
                            editing = null
                            imagePath = null
                            items = repo.productLocalAll()
                        }
                    }, modifier = Modifier.weight(1f))
                    if (editing != null) {
                        TextButton(onClick = { editing = null; name = ""; price = ""; categoryId = null; imagePath = null }) { Text("Cancelar") }
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
                                TextButton(onClick = { editing = p; name = p.name; price = p.priceCents.toString(); categoryId = p.categoryId; imagePath = p.imagePath }) { Text("Editar") }
                            }
                            Text(text = "R$ " + (p.priceCents / 100.0), style = MaterialTheme.typography.bodyMedium)
                            val bmp = remember(p.imagePath) {
                                val path = p.imagePath
                                if (path != null) try { BitmapFactory.decodeFile(path)?.asImageBitmap() } catch (e: Exception) { null } else null
                            }
                            if (bmp != null) {
                                Image(bitmap = bmp, contentDescription = null, modifier = Modifier.fillMaxWidth().height(80.dp), contentScale = ContentScale.Crop)
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
