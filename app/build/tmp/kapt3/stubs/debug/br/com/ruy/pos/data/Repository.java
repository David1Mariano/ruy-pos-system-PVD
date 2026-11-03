package br.com.ruy.pos.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u00a8\u0001\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\u0010$\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J:\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u00182\u0006\u0010\u001a\u001a\u00020\u001b2\u0006\u0010\u001c\u001a\u00020\u001b2\n\b\u0002\u0010\u001d\u001a\u0004\u0018\u00010\u0018H\u0086@\u00a2\u0006\u0002\u0010\u001eJ\u0014\u0010\u001f\u001a\b\u0012\u0004\u0012\u00020!0 H\u0086@\u00a2\u0006\u0002\u0010\"J\u0014\u0010#\u001a\b\u0012\u0004\u0012\u00020$0 H\u0086@\u00a2\u0006\u0002\u0010\"J\u0016\u0010%\u001a\u00020\u00162\u0006\u0010&\u001a\u00020\u001bH\u0086@\u00a2\u0006\u0002\u0010\'J\u001c\u0010(\u001a\b\u0012\u0004\u0012\u00020)0 2\u0006\u0010\u0017\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010*J\u0016\u0010+\u001a\u00020\u001b2\u0006\u0010\u0017\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010*J\u0018\u0010,\u001a\u0004\u0018\u00010-2\u0006\u0010.\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010*J\u0014\u0010/\u001a\b\u0012\u0004\u0012\u0002000 H\u0086@\u00a2\u0006\u0002\u0010\"J\u0016\u00101\u001a\u00020\u00162\u0006\u00102\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010*J\u0016\u00103\u001a\u00020\u00182\u0006\u0010.\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010*J\u0014\u00104\u001a\b\u0012\u0004\u0012\u00020!0 H\u0086@\u00a2\u0006\u0002\u0010\"J\u0014\u00105\u001a\b\u0012\u0004\u0012\u0002060 H\u0086@\u00a2\u0006\u0002\u0010\"J\u0014\u00107\u001a\b\u0012\u0004\u0012\u0002060 H\u0086@\u00a2\u0006\u0002\u0010\"J\u0014\u00108\u001a\b\u0012\u0004\u0012\u0002090 H\u0086@\u00a2\u0006\u0002\u0010\"J\u001e\u0010:\u001a\u00020\u00162\u0006\u00102\u001a\u00020\u00182\u0006\u0010;\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010<JX\u0010=\u001a\u00020\u00182\u0006\u0010>\u001a\u00020\u001b2\u0018\u0010?\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0018\u0012\u0004\u0012\u00020\u001b0@0 2\u0012\u0010\u001a\u001a\u000e\u0012\u0004\u0012\u00020\u0018\u0012\u0004\u0012\u00020\u001b0A2\b\u0010B\u001a\u0004\u0018\u00010\u00182\b\u0010C\u001a\u0004\u0018\u00010\u0018H\u0086@\u00a2\u0006\u0002\u0010DJ:\u0010E\u001a\u00020\u00162\u0006\u00102\u001a\u00020\u00182\b\u0010F\u001a\u0004\u0018\u00010\u00182\u0006\u0010;\u001a\u00020\u00182\u0006\u0010G\u001a\u00020\u001b2\b\b\u0002\u0010H\u001a\u00020IH\u0086@\u00a2\u0006\u0002\u0010JR\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006K"}, d2 = {"Lbr/com/ruy/pos/data/Repository;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "categoryDao", "Lbr/com/ruy/pos/data/local/CategoryDao;", "db", "Lbr/com/ruy/pos/data/local/AppDatabase;", "orderDao", "Lbr/com/ruy/pos/data/local/OrderDao;", "productDao", "Lbr/com/ruy/pos/data/local/ProductDao;", "productLocalDao", "Lbr/com/ruy/pos/data/local/ProductLocalDao;", "tableDao", "Lbr/com/ruy/pos/data/local/TableDao;", "tableOrderDao", "Lbr/com/ruy/pos/data/local/TableOrderDao;", "tableOrderItemDao", "Lbr/com/ruy/pos/data/local/TableOrderItemDao;", "addItemToComanda", "", "comandaId", "", "productName", "unitCents", "", "qty", "productId", "(Ljava/lang/String;Ljava/lang/String;IILjava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "allOrders", "", "Lbr/com/ruy/pos/data/local/OrderEntity;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "categories", "Lbr/com/ruy/pos/data/local/CategoryEntity;", "ensureTables", "count", "(ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getComandaItems", "Lbr/com/ruy/pos/data/local/TableOrderItemEntity;", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getComandaTotal", "getOpenComanda", "Lbr/com/ruy/pos/data/local/TableOrderEntity;", "tableId", "getTables", "Lbr/com/ruy/pos/data/local/TableEntity;", "markSynced", "id", "openTable", "pendingOrders", "productLocal", "Lbr/com/ruy/pos/data/local/ProductLocalEntity;", "productLocalAll", "products", "Lbr/com/ruy/pos/data/local/ProductEntity;", "saveCategory", "name", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "saveOrder", "totalCents", "items", "Lkotlin/Pair;", "", "deviceId", "operatorId", "(ILjava/util/List;Ljava/util/Map;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "saveProductLocal", "categoryId", "priceCents", "active", "", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class Repository {
    @org.jetbrains.annotations.NotNull()
    private final br.com.ruy.pos.data.local.AppDatabase db = null;
    @org.jetbrains.annotations.NotNull()
    private final br.com.ruy.pos.data.local.ProductDao productDao = null;
    @org.jetbrains.annotations.NotNull()
    private final br.com.ruy.pos.data.local.OrderDao orderDao = null;
    @org.jetbrains.annotations.NotNull()
    private final br.com.ruy.pos.data.local.CategoryDao categoryDao = null;
    @org.jetbrains.annotations.NotNull()
    private final br.com.ruy.pos.data.local.ProductLocalDao productLocalDao = null;
    @org.jetbrains.annotations.NotNull()
    private final br.com.ruy.pos.data.local.TableDao tableDao = null;
    @org.jetbrains.annotations.NotNull()
    private final br.com.ruy.pos.data.local.TableOrderDao tableOrderDao = null;
    @org.jetbrains.annotations.NotNull()
    private final br.com.ruy.pos.data.local.TableOrderItemDao tableOrderItemDao = null;
    
    public Repository(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object products(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<br.com.ruy.pos.data.local.ProductEntity>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object categories(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<br.com.ruy.pos.data.local.CategoryEntity>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object productLocal(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<br.com.ruy.pos.data.local.ProductLocalEntity>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object productLocalAll(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<br.com.ruy.pos.data.local.ProductLocalEntity>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object saveCategory(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object saveProductLocal(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.Nullable()
    java.lang.String categoryId, @org.jetbrains.annotations.NotNull()
    java.lang.String name, int priceCents, boolean active, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object saveOrder(int totalCents, @org.jetbrains.annotations.NotNull()
    java.util.List<kotlin.Pair<java.lang.String, java.lang.Integer>> items, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.Integer> unitCents, @org.jetbrains.annotations.Nullable()
    java.lang.String deviceId, @org.jetbrains.annotations.Nullable()
    java.lang.String operatorId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object pendingOrders(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<br.com.ruy.pos.data.local.OrderEntity>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object markSynced(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object allOrders(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<br.com.ruy.pos.data.local.OrderEntity>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object ensureTables(int count, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getTables(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<br.com.ruy.pos.data.local.TableEntity>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object openTable(@org.jetbrains.annotations.NotNull()
    java.lang.String tableId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getOpenComanda(@org.jetbrains.annotations.NotNull()
    java.lang.String tableId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super br.com.ruy.pos.data.local.TableOrderEntity> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object addItemToComanda(@org.jetbrains.annotations.NotNull()
    java.lang.String comandaId, @org.jetbrains.annotations.NotNull()
    java.lang.String productName, int unitCents, int qty, @org.jetbrains.annotations.Nullable()
    java.lang.String productId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getComandaItems(@org.jetbrains.annotations.NotNull()
    java.lang.String comandaId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<br.com.ruy.pos.data.local.TableOrderItemEntity>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getComandaTotal(@org.jetbrains.annotations.NotNull()
    java.lang.String comandaId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Integer> $completion) {
        return null;
    }
}