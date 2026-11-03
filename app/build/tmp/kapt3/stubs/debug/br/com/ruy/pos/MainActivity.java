package br.com.ruy.pos;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000d\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010%\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u0007\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\t\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u001a\u001a\u00020\u001b2\u0006\u0010\u001c\u001a\u00020\u0007H\u0002J\u001e\u0010\u001d\u001a\u00020\u001b2\u0006\u0010\u001e\u001a\u00020\u00072\f\u0010\u001f\u001a\b\u0012\u0004\u0012\u00020\u001b0 H\u0002J\b\u0010!\u001a\u00020\u001bH\u0002J\b\u0010\"\u001a\u00020\u001bH\u0002J\b\u0010#\u001a\u00020\u001bH\u0002J\b\u0010$\u001a\u00020\u001bH\u0002J\u001a\u0010%\u001a\u00020\u001b2\u0006\u0010\u001e\u001a\u00020\u00072\b\b\u0002\u0010&\u001a\u00020\'H\u0002J\b\u0010(\u001a\u00020\u001bH\u0002J\u0012\u0010)\u001a\u00020\u001b2\b\u0010*\u001a\u0004\u0018\u00010+H\u0014J\u0010\u0010,\u001a\u00020\u00072\u0006\u0010-\u001a\u00020\u0004H\u0002J\b\u0010.\u001a\u00020\u001bH\u0002J\b\u0010/\u001a\u00020\u001bH\u0002J\b\u00100\u001a\u00020\u001bH\u0002J\b\u00101\u001a\u00020\u001bH\u0002J\u0010\u00102\u001a\u00020\u001b2\u0006\u00103\u001a\u00020\u0007H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082D\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0005\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00040\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R#\u0010\b\u001a\n \n*\u0004\u0018\u00010\t0\t8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\r\u0010\u000e\u001a\u0004\b\u000b\u0010\fR\u0014\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00110\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\u0016X\u0082.\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0017\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00040\u0018X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0019\u001a\u00020\u0004X\u0082D\u00a2\u0006\u0002\n\u0000\u00a8\u00064"}, d2 = {"Lbr/com/ruy/pos/MainActivity;", "Landroid/app/Activity;", "()V", "black", "", "cart", "Ljava/util/LinkedHashMap;", "", "prefs", "Landroid/content/SharedPreferences;", "kotlin.jvm.PlatformType", "getPrefs", "()Landroid/content/SharedPreferences;", "prefs$delegate", "Lkotlin/Lazy;", "products", "", "Lbr/com/ruy/pos/data/local/ProductEntity;", "red", "repo", "Lbr/com/ruy/pos/data/Repository;", "root", "Landroid/widget/LinearLayout;", "unitPrice", "", "white", "base", "", "title", "button", "v", "action", "Lkotlin/Function0;", "config", "diag", "history", "home", "label", "s", "", "loadCatalog", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "reais", "c", "sales", "scheduleSync", "syncCatalog", "syncNow", "testConnection", "u", "app_debug"})
public final class MainActivity extends android.app.Activity {
    private final int red = 0;
    private final int black = android.graphics.Color.BLACK;
    private final int white = android.graphics.Color.WHITE;
    private android.widget.LinearLayout root;
    private br.com.ruy.pos.data.Repository repo;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy prefs$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.LinkedHashMap<java.lang.String, java.lang.Integer> cart = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<br.com.ruy.pos.data.local.ProductEntity> products = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> unitPrice = null;
    
    public MainActivity() {
        super();
    }
    
    private final android.content.SharedPreferences getPrefs() {
        return null;
    }
    
    private final java.lang.String reais(int c) {
        return null;
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void scheduleSync() {
    }
    
    private final void base(java.lang.String title) {
    }
    
    private final void label(java.lang.String v, float s) {
    }
    
    private final void button(java.lang.String v, kotlin.jvm.functions.Function0<kotlin.Unit> action) {
    }
    
    private final void home() {
    }
    
    private final void diag() {
    }
    
    private final void config() {
    }
    
    private final void testConnection(java.lang.String u) {
    }
    
    private final void loadCatalog() {
    }
    
    private final void syncCatalog() {
    }
    
    private final void sales() {
    }
    
    private final void history() {
    }
    
    private final void syncNow() {
    }
}