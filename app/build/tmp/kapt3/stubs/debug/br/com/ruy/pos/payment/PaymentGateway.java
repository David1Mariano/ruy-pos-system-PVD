package br.com.ruy.pos.payment;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\bf\u0018\u00002\u00020\u0001J\b\u0010\u0002\u001a\u00020\u0003H&J\u0010\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u0007H&\u00a8\u0006\b"}, d2 = {"Lbr/com/ruy/pos/payment/PaymentGateway;", "", "isAvailable", "", "process", "Lbr/com/ruy/pos/payment/PaymentResult;", "request", "Lbr/com/ruy/pos/payment/PaymentRequest;", "app_debug"})
public abstract interface PaymentGateway {
    
    public abstract boolean isAvailable();
    
    @org.jetbrains.annotations.NotNull()
    public abstract br.com.ruy.pos.payment.PaymentResult process(@org.jetbrains.annotations.NotNull()
    br.com.ruy.pos.payment.PaymentRequest request);
}