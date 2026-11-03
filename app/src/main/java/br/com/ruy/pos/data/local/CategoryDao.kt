package br.com.ruy.pos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE active = 1 ORDER BY sortOrder ASC")
    suspend fun getActive(): List<CategoryEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: String)
}
