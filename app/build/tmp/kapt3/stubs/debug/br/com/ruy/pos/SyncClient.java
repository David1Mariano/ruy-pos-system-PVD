package br.com.ruy.pos;

/**
 * Envia pedidos ao backend com idempotencia controlada via header Idempotency-Key.
 *
 * Comportamento conforme contrato:
 * - Mesmo UUID + mesmo corpo  -> retorna acele anterior, sem duplicata (200/201).
 * - Mesmo UUID + corpo diferente -> rejeita com 409 Conflict.
 * - Requisicao em processamento pode receber 409 transitorio, que deve ser tratado como temporario.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J1\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u00042\u0006\u0010\u0006\u001a\u00020\u00052\u0006\u0010\u0007\u001a\u00020\u00052\u0006\u0010\b\u001a\u00020\t\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\n\u0010\u000b\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006\f"}, d2 = {"Lbr/com/ruy/pos/SyncClient;", "", "()V", "send", "Lkotlin/Result;", "", "endpoint", "bearer", "order", "Lorg/json/JSONObject;", "send-0E7RQCE", "(Ljava/lang/String;Ljava/lang/String;Lorg/json/JSONObject;)Ljava/lang/Object;", "app_debug"})
public final class SyncClient {
    @org.jetbrains.annotations.NotNull()
    public static final br.com.ruy.pos.SyncClient INSTANCE = null;
    
    private SyncClient() {
        super();
    }
}