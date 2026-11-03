package br.com.ruy.pos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products_local")
data class ProductLocalEntity(
    @PrimaryKey val id: String,
    val categoryId: String? = null,
    val name: String,
    val priceCents: Int,
    val active: Boolean = true,
    val imageUrl: String? = null
)
