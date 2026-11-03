package br.com.ruy.pos.data.local;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0010 \n\u0000\n\u0002\u0010\b\n\u0002\b\u0004\bg\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u001e\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\u00032\u0006\u0010\n\u001a\u00020\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u000bJ\u001c\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u00050\r2\u0006\u0010\u000e\u001a\u00020\u000fH\u00a7@\u00a2\u0006\u0002\u0010\u0010J\u0016\u0010\u0011\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0012\u00a8\u0006\u0013"}, d2 = {"Lbr/com/ruy/pos/data/local/SyncQueueDao;", "", "enqueue", "", "item", "Lbr/com/ruy/pos/data/local/SyncQueueEntity;", "(Lbr/com/ruy/pos/data/local/SyncQueueEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "incAttempt", "", "rowId", "now", "(JJLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "nextBatch", "", "limit", "", "(ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "remove", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
@androidx.room.Dao()
public abstract interface SyncQueueDao {
    
    @androidx.room.Insert()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object enqueue(@org.jetbrains.annotations.NotNull()
    br.com.ruy.pos.data.local.SyncQueueEntity item, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Long> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM sync_queue ORDER BY createdAt ASC LIMIT :limit")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object nextBatch(int limit, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<br.com.ruy.pos.data.local.SyncQueueEntity>> $completion);
    
    @androidx.room.Query(value = "DELETE FROM sync_queue WHERE rowId = :rowId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object remove(long rowId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "UPDATE sync_queue SET attempts = attempts + 1, lastAttemptAt = :now WHERE rowId = :rowId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object incAttempt(long rowId, long now, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}