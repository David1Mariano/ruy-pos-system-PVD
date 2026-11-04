package br.com.ruy.pos.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE orders ADD COLUMN comandaId TEXT")
            db.execSQL("CREATE UNIQUE INDEX index_orders_comandaId ON orders(comandaId)")
            db.execSQL("ALTER TABLE payments ADD COLUMN cashSessionId TEXT")
            db.execSQL("CREATE TABLE stone_attempts (stoneOrderId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, attemptId TEXT NOT NULL, paymentId TEXT NOT NULL, orderId TEXT NOT NULL, installmentType TEXT NOT NULL, origin TEXT NOT NULL, state TEXT NOT NULL, activeSlot INTEGER, atk TEXT, itk TEXT, authorizationCode TEXT, authorizationDateTime TEXT, returnCode TEXT, detail TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, reconciledBy TEXT, reconciliationEvidence TEXT)")
            for (field in listOf("attemptId", "paymentId", "activeSlot", "atk")) {
                db.execSQL("CREATE UNIQUE INDEX index_stone_attempts_$field ON stone_attempts($field)")
            }
            db.execSQL("CREATE TABLE stone_callback_events (fingerprint TEXT PRIMARY KEY NOT NULL, attemptId TEXT, result TEXT NOT NULL, safeFields TEXT, createdAt INTEGER NOT NULL)")
        }
    }
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
        }
    }
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS categories (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, active INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS products_local (id TEXT PRIMARY KEY NOT NULL, categoryId TEXT, name TEXT NOT NULL, priceCents INTEGER NOT NULL, active INTEGER NOT NULL DEFAULT 1)")
        }
    }
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS tables (tableId TEXT PRIMARY KEY NOT NULL, number INTEGER NOT NULL, status TEXT NOT NULL, openedAt INTEGER, closedAt INTEGER)")
            db.execSQL("CREATE TABLE IF NOT EXISTS table_orders (comandaId TEXT PRIMARY KEY NOT NULL, tableId TEXT NOT NULL, status TEXT NOT NULL, totalCents INTEGER NOT NULL DEFAULT 0, openedAt INTEGER NOT NULL, closedAt INTEGER)")
            db.execSQL("CREATE TABLE IF NOT EXISTS table_order_items (itemId TEXT PRIMARY KEY NOT NULL, comandaId TEXT NOT NULL, productId TEXT, productName TEXT NOT NULL, unitCents INTEGER NOT NULL, qty INTEGER NOT NULL, addedAt INTEGER NOT NULL)")
        }
    }
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try { db.execSQL("ALTER TABLE products_local ADD COLUMN imagePath TEXT") } catch (e: Exception) {}
        }
    }
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS payments (paymentId TEXT PRIMARY KEY NOT NULL, orderId TEXT NOT NULL, amountCents INTEGER NOT NULL, method TEXT NOT NULL, status TEXT NOT NULL, receivedCents INTEGER, changeCents INTEGER, installments INTEGER NOT NULL DEFAULT 1, transactionRef TEXT, createdAt INTEGER NOT NULL)")
        }
    }
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS cash_sessions (sessionId TEXT PRIMARY KEY NOT NULL, operator TEXT NOT NULL, openedAt INTEGER NOT NULL, closedAt INTEGER, openingBalanceCents INTEGER NOT NULL, status TEXT NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS cash_movements (movementId TEXT PRIMARY KEY NOT NULL, sessionId TEXT NOT NULL, type TEXT NOT NULL, amountCents INTEGER NOT NULL, reason TEXT, orderId TEXT, createdAt INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS cash_closings (closingId TEXT PRIMARY KEY NOT NULL, sessionId TEXT NOT NULL, closedAt INTEGER NOT NULL, expectedCashCents INTEGER NOT NULL, countedCashCents INTEGER NOT NULL, differenceCents INTEGER NOT NULL, totalSalesCents INTEGER NOT NULL, totalCashCents INTEGER NOT NULL, totalPixCents INTEGER NOT NULL, totalDebitCents INTEGER NOT NULL, totalCreditCents INTEGER NOT NULL, sangriasCents INTEGER NOT NULL, suppliesCents INTEGER NOT NULL, operator TEXT NOT NULL, note TEXT)")
        }
    }
}

