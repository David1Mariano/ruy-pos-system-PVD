package br.com.ruy.pos.data.local;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\'\u0018\u0000 \u00112\u00020\u0001:\u0001\u0011B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H&J\b\u0010\u0005\u001a\u00020\u0006H&J\b\u0010\u0007\u001a\u00020\bH&J\b\u0010\t\u001a\u00020\nH&J\b\u0010\u000b\u001a\u00020\fH&J\b\u0010\r\u001a\u00020\u000eH&J\b\u0010\u000f\u001a\u00020\u0010H&\u00a8\u0006\u0012"}, d2 = {"Lbr/com/ruy/pos/data/local/AppDatabase;", "Landroidx/room/RoomDatabase;", "()V", "categoryDao", "Lbr/com/ruy/pos/data/local/CategoryDao;", "orderDao", "Lbr/com/ruy/pos/data/local/OrderDao;", "productDao", "Lbr/com/ruy/pos/data/local/ProductDao;", "productLocalDao", "Lbr/com/ruy/pos/data/local/ProductLocalDao;", "tableDao", "Lbr/com/ruy/pos/data/local/TableDao;", "tableOrderDao", "Lbr/com/ruy/pos/data/local/TableOrderDao;", "tableOrderItemDao", "Lbr/com/ruy/pos/data/local/TableOrderItemDao;", "Companion", "app_debug"})
@androidx.room.Database(entities = {br.com.ruy.pos.data.local.ProductEntity.class, br.com.ruy.pos.data.local.OrderEntity.class, br.com.ruy.pos.data.local.CategoryEntity.class, br.com.ruy.pos.data.local.ProductLocalEntity.class, br.com.ruy.pos.data.local.TableEntity.class, br.com.ruy.pos.data.local.TableOrderEntity.class, br.com.ruy.pos.data.local.TableOrderItemEntity.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends androidx.room.RoomDatabase {
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private static volatile br.com.ruy.pos.data.local.AppDatabase INSTANCE;
    @org.jetbrains.annotations.NotNull()
    public static final br.com.ruy.pos.data.local.AppDatabase.Companion Companion = null;
    
    public AppDatabase() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract br.com.ruy.pos.data.local.ProductDao productDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract br.com.ruy.pos.data.local.OrderDao orderDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract br.com.ruy.pos.data.local.CategoryDao categoryDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract br.com.ruy.pos.data.local.ProductLocalDao productLocalDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract br.com.ruy.pos.data.local.TableDao tableDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract br.com.ruy.pos.data.local.TableOrderDao tableOrderDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract br.com.ruy.pos.data.local.TableOrderItemDao tableOrderItemDao();
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0006\u001a\u00020\u0007R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\b"}, d2 = {"Lbr/com/ruy/pos/data/local/AppDatabase$Companion;", "", "()V", "INSTANCE", "Lbr/com/ruy/pos/data/local/AppDatabase;", "get", "context", "Landroid/content/Context;", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final br.com.ruy.pos.data.local.AppDatabase get(@org.jetbrains.annotations.NotNull()
        android.content.Context context) {
            return null;
        }
    }
}