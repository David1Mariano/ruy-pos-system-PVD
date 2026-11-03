package br.com.ruy.pos.payment;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H\u0016J\u0010\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0016\u00a8\u0006\t"}, d2 = {"Lbr/com/ruy/pos/payment/ManualPaymentGateway;", "Lbr/com/ruy/pos/payment/PaymentGateway;", "()V", "isAvailable", "", "process", "Lbr/com/ruy/pos/payment/PaymentResult;", "request", "Lbr/com/ruy/pos/payment/PaymentRequest;", "app_debug"})
public final class ManualPaymentGateway implements br.com.ruy.pos.payment.PaymentGateway {
    
    public ManualPaymentGateway() {
        super();
    }
    
    @java.lang.Override()
    public boolean isAvailable() {
        return false;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public br.com.ruy.pos.payment.PaymentResult process(@org.jetbrains.annotations.NotNull()
    br.com.ruy.pos.payment.PaymentRequest request) {
        return null;
    }
}