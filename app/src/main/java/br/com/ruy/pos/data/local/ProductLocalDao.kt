package br.com.ruy.pos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProductLocalDao {
    @Query("SELECT * FROM products_local WHERE active = 1 ORDER BY name ASC")
    suspend fun getActive(): List<ProductLocalEntity>
    @Query("SELECT * FROM products_local ORDER BY name ASC")
    suspend fun getAll(): List<ProductLocalEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductLocalEntity)
    @Query("DELETE FROM products_local WHERE id = :id")
    suspend fun delete(id: String)
}
