package br.com.ruy.pos

import android.content.Context
import androidx.room.withTransaction
import br.com.ruy.pos.data.local.*

object Homologation {
    const val LABEL = "RUY POS — HOMOLOGAÇÃO"
    suspend fun seed(context: Context) {
        if (!BuildConfig.HOMOLOGATION) return
        val db = AppDatabase.get(context)
        db.withTransaction {
            // A database sentinel, never a preference, prevents reseeding after restore/restart.
            if (db.categoryDao().getAll().any { it.id == "HML-SALGADOS" }) return@withTransaction
            db.categoryDao().insert(CategoryEntity("HML-SALGADOS", "Salgados"))
            db.categoryDao().insert(CategoryEntity("HML-BEBIDAS", "Bebidas"))
            db.productLocalDao().insert(ProductLocalEntity("HML-COXINHA", "HML-SALGADOS", "Coxinha", 800, true, null))
            db.productLocalDao().insert(ProductLocalEntity("HML-CALDO", "HML-BEBIDAS", "Caldo de Cana", 700, true, null))
            db.tableDao().upsertTable(TableEntity("T05", 5, "LIVRE"))
            db.cashSessionDao().insert(CashSessionEntity("HML-CAIXA", "Operador fictício", System.currentTimeMillis(),
                openingBalanceCents = 10000, status = "ABERTO"))
        }
    }
}
