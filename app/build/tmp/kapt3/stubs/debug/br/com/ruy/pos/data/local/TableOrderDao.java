package br.com.ruy.pos.data.local;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0010\b\n\u0002\b\u0002\bg\u0018\u00002\u00020\u0001J\u0014\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0005J\u0018\u0010\u0006\u001a\u0004\u0018\u00010\u00042\u0006\u0010\u0007\u001a\u00020\bH\u00a7@\u00a2\u0006\u0002\u0010\tJ\u0016\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u0004H\u00a7@\u00a2\u0006\u0002\u0010\rJ\u001e\u0010\u000e\u001a\u00020\u000b2\u0006\u0010\u000f\u001a\u00020\b2\u0006\u0010\u0010\u001a\u00020\u0011H\u00a7@\u00a2\u0006\u0002\u0010\u0012\u00a8\u0006\u0013"}, d2 = {"Lbr/com/ruy/pos/data/local/TableOrderDao;", "", "getOpen", "", "Lbr/com/ruy/pos/data/local/TableOrderEntity;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getOpenByTable", "tableId", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "insert", "", "comanda", "(Lbr/com/ruy/pos/data/local/TableOrderEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateTotal", "comandaId", "totalCents", "", "(Ljava/lang/String;ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
@androidx.room.Dao()
public abstract interface TableOrderDao {
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insert(@org.jetbrains.annotations.NotNull()
    br.com.ruy.pos.data.local.TableOrderEntity comanda, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM table_orders WHERE tableId = :tableId AND status = \'ABERTA\' LIMIT 1")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getOpenByTable(@org.jetbrains.annotations.NotNull()
    java.lang.String tableId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super br.com.ruy.pos.data.local.TableOrderEntity> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM table_orders WHERE status = \'ABERTA\' ORDER BY openedAt DESC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getOpen(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<br.com.ruy.pos.data.local.TableOrderEntity>> $completion);
    
    @androidx.room.Query(value = "UPDATE table_orders SET totalCents = :totalCents WHERE comandaId = :comandaId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateTotal(@org.jetbrains.annotations.NotNull()
    java.lang.String comandaId, int totalCents, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}