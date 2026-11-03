package br.com.ruy.pos.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile ProductDao _productDao;

  private volatile OrderDao _orderDao;

  private volatile CategoryDao _categoryDao;

  private volatile ProductLocalDao _productLocalDao;

  private volatile TableDao _tableDao;

  private volatile TableOrderDao _tableOrderDao;

  private volatile TableOrderItemDao _tableOrderItemDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(4) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `products` (`id` TEXT NOT NULL, `sku` TEXT, `name` TEXT NOT NULL, `category` TEXT, `priceCents` INTEGER NOT NULL, `active` INTEGER NOT NULL, `imageUrl` TEXT, `version` INTEGER NOT NULL, `updatedAt` TEXT, `unit` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `orders` (`id` TEXT NOT NULL, `totalCents` INTEGER NOT NULL, `paymentStatus` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, `itemsJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `operatorId` TEXT, `deviceId` TEXT, `stoneTransactionId` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `active` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `products_local` (`id` TEXT NOT NULL, `categoryId` TEXT, `name` TEXT NOT NULL, `priceCents` INTEGER NOT NULL, `active` INTEGER NOT NULL, `imageUrl` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `tables` (`tableId` TEXT NOT NULL, `number` INTEGER NOT NULL, `status` TEXT NOT NULL, `openedAt` INTEGER, `closedAt` INTEGER, PRIMARY KEY(`tableId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `table_orders` (`comandaId` TEXT NOT NULL, `tableId` TEXT NOT NULL, `status` TEXT NOT NULL, `totalCents` INTEGER NOT NULL, `openedAt` INTEGER NOT NULL, `closedAt` INTEGER, PRIMARY KEY(`comandaId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `table_order_items` (`itemId` TEXT NOT NULL, `comandaId` TEXT NOT NULL, `productId` TEXT, `productName` TEXT NOT NULL, `unitCents` INTEGER NOT NULL, `qty` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`itemId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '703b3e4b71bf331e240fbeef11542f5a')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `products`");
        db.execSQL("DROP TABLE IF EXISTS `orders`");
        db.execSQL("DROP TABLE IF EXISTS `categories`");
        db.execSQL("DROP TABLE IF EXISTS `products_local`");
        db.execSQL("DROP TABLE IF EXISTS `tables`");
        db.execSQL("DROP TABLE IF EXISTS `table_orders`");
        db.execSQL("DROP TABLE IF EXISTS `table_order_items`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsProducts = new HashMap<String, TableInfo.Column>(10);
        _columnsProducts.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("sku", new TableInfo.Column("sku", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("category", new TableInfo.Column("category", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("priceCents", new TableInfo.Column("priceCents", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("active", new TableInfo.Column("active", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("imageUrl", new TableInfo.Column("imageUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("version", new TableInfo.Column("version", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("updatedAt", new TableInfo.Column("updatedAt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("unit", new TableInfo.Column("unit", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysProducts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesProducts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoProducts = new TableInfo("products", _columnsProducts, _foreignKeysProducts, _indicesProducts);
        final TableInfo _existingProducts = TableInfo.read(db, "products");
        if (!_infoProducts.equals(_existingProducts)) {
          return new RoomOpenHelper.ValidationResult(false, "products(br.com.ruy.pos.data.local.ProductEntity).\n"
                  + " Expected:\n" + _infoProducts + "\n"
                  + " Found:\n" + _existingProducts);
        }
        final HashMap<String, TableInfo.Column> _columnsOrders = new HashMap<String, TableInfo.Column>(9);
        _columnsOrders.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOrders.put("totalCents", new TableInfo.Column("totalCents", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOrders.put("paymentStatus", new TableInfo.Column("paymentStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOrders.put("syncStatus", new TableInfo.Column("syncStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOrders.put("itemsJson", new TableInfo.Column("itemsJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOrders.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOrders.put("operatorId", new TableInfo.Column("operatorId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOrders.put("deviceId", new TableInfo.Column("deviceId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOrders.put("stoneTransactionId", new TableInfo.Column("stoneTransactionId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysOrders = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesOrders = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoOrders = new TableInfo("orders", _columnsOrders, _foreignKeysOrders, _indicesOrders);
        final TableInfo _existingOrders = TableInfo.read(db, "orders");
        if (!_infoOrders.equals(_existingOrders)) {
          return new RoomOpenHelper.ValidationResult(false, "orders(br.com.ruy.pos.data.local.OrderEntity).\n"
                  + " Expected:\n" + _infoOrders + "\n"
                  + " Found:\n" + _existingOrders);
        }
        final HashMap<String, TableInfo.Column> _columnsCategories = new HashMap<String, TableInfo.Column>(4);
        _columnsCategories.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("active", new TableInfo.Column("active", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCategories = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCategories = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCategories = new TableInfo("categories", _columnsCategories, _foreignKeysCategories, _indicesCategories);
        final TableInfo _existingCategories = TableInfo.read(db, "categories");
        if (!_infoCategories.equals(_existingCategories)) {
          return new RoomOpenHelper.ValidationResult(false, "categories(br.com.ruy.pos.data.local.CategoryEntity).\n"
                  + " Expected:\n" + _infoCategories + "\n"
                  + " Found:\n" + _existingCategories);
        }
        final HashMap<String, TableInfo.Column> _columnsProductsLocal = new HashMap<String, TableInfo.Column>(6);
        _columnsProductsLocal.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProductsLocal.put("categoryId", new TableInfo.Column("categoryId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProductsLocal.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProductsLocal.put("priceCents", new TableInfo.Column("priceCents", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProductsLocal.put("active", new TableInfo.Column("active", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProductsLocal.put("imageUrl", new TableInfo.Column("imageUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysProductsLocal = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesProductsLocal = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoProductsLocal = new TableInfo("products_local", _columnsProductsLocal, _foreignKeysProductsLocal, _indicesProductsLocal);
        final TableInfo _existingProductsLocal = TableInfo.read(db, "products_local");
        if (!_infoProductsLocal.equals(_existingProductsLocal)) {
          return new RoomOpenHelper.ValidationResult(false, "products_local(br.com.ruy.pos.data.local.ProductLocalEntity).\n"
                  + " Expected:\n" + _infoProductsLocal + "\n"
                  + " Found:\n" + _existingProductsLocal);
        }
        final HashMap<String, TableInfo.Column> _columnsTables = new HashMap<String, TableInfo.Column>(5);
        _columnsTables.put("tableId", new TableInfo.Column("tableId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTables.put("number", new TableInfo.Column("number", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTables.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTables.put("openedAt", new TableInfo.Column("openedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTables.put("closedAt", new TableInfo.Column("closedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTables = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTables = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTables = new TableInfo("tables", _columnsTables, _foreignKeysTables, _indicesTables);
        final TableInfo _existingTables = TableInfo.read(db, "tables");
        if (!_infoTables.equals(_existingTables)) {
          return new RoomOpenHelper.ValidationResult(false, "tables(br.com.ruy.pos.data.local.TableEntity).\n"
                  + " Expected:\n" + _infoTables + "\n"
                  + " Found:\n" + _existingTables);
        }
        final HashMap<String, TableInfo.Column> _columnsTableOrders = new HashMap<String, TableInfo.Column>(6);
        _columnsTableOrders.put("comandaId", new TableInfo.Column("comandaId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTableOrders.put("tableId", new TableInfo.Column("tableId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTableOrders.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTableOrders.put("totalCents", new TableInfo.Column("totalCents", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTableOrders.put("openedAt", new TableInfo.Column("openedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTableOrders.put("closedAt", new TableInfo.Column("closedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTableOrders = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTableOrders = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTableOrders = new TableInfo("table_orders", _columnsTableOrders, _foreignKeysTableOrders, _indicesTableOrders);
        final TableInfo _existingTableOrders = TableInfo.read(db, "table_orders");
        if (!_infoTableOrders.equals(_existingTableOrders)) {
          return new RoomOpenHelper.ValidationResult(false, "table_orders(br.com.ruy.pos.data.local.TableOrderEntity).\n"
                  + " Expected:\n" + _infoTableOrders + "\n"
                  + " Found:\n" + _existingTableOrders);
        }
        final HashMap<String, TableInfo.Column> _columnsTableOrderItems = new HashMap<String, TableInfo.Column>(7);
        _columnsTableOrderItems.put("itemId", new TableInfo.Column("itemId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTableOrderItems.put("comandaId", new TableInfo.Column("comandaId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTableOrderItems.put("productId", new TableInfo.Column("productId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTableOrderItems.put("productName", new TableInfo.Column("productName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTableOrderItems.put("unitCents", new TableInfo.Column("unitCents", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTableOrderItems.put("qty", new TableInfo.Column("qty", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTableOrderItems.put("addedAt", new TableInfo.Column("addedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTableOrderItems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTableOrderItems = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTableOrderItems = new TableInfo("table_order_items", _columnsTableOrderItems, _foreignKeysTableOrderItems, _indicesTableOrderItems);
        final TableInfo _existingTableOrderItems = TableInfo.read(db, "table_order_items");
        if (!_infoTableOrderItems.equals(_existingTableOrderItems)) {
          return new RoomOpenHelper.ValidationResult(false, "table_order_items(br.com.ruy.pos.data.local.TableOrderItemEntity).\n"
                  + " Expected:\n" + _infoTableOrderItems + "\n"
                  + " Found:\n" + _existingTableOrderItems);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "703b3e4b71bf331e240fbeef11542f5a", "1c6fc8a00940cb9ab00ed1b8ad01afab");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "products","orders","categories","products_local","tables","table_orders","table_order_items");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `products`");
      _db.execSQL("DELETE FROM `orders`");
      _db.execSQL("DELETE FROM `categories`");
      _db.execSQL("DELETE FROM `products_local`");
      _db.execSQL("DELETE FROM `tables`");
      _db.execSQL("DELETE FROM `table_orders`");
      _db.execSQL("DELETE FROM `table_order_items`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(ProductDao.class, ProductDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(OrderDao.class, OrderDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CategoryDao.class, CategoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ProductLocalDao.class, ProductLocalDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TableDao.class, TableDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TableOrderDao.class, TableOrderDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TableOrderItemDao.class, TableOrderItemDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public ProductDao productDao() {
    if (_productDao != null) {
      return _productDao;
    } else {
      synchronized(this) {
        if(_productDao == null) {
          _productDao = new ProductDao_Impl(this);
        }
        return _productDao;
      }
    }
  }

  @Override
  public OrderDao orderDao() {
    if (_orderDao != null) {
      return _orderDao;
    } else {
      synchronized(this) {
        if(_orderDao == null) {
          _orderDao = new OrderDao_Impl(this);
        }
        return _orderDao;
      }
    }
  }

  @Override
  public CategoryDao categoryDao() {
    if (_categoryDao != null) {
      return _categoryDao;
    } else {
      synchronized(this) {
        if(_categoryDao == null) {
          _categoryDao = new CategoryDao_Impl(this);
        }
        return _categoryDao;
      }
    }
  }

  @Override
  public ProductLocalDao productLocalDao() {
    if (_productLocalDao != null) {
      return _productLocalDao;
    } else {
      synchronized(this) {
        if(_productLocalDao == null) {
          _productLocalDao = new ProductLocalDao_Impl(this);
        }
        return _productLocalDao;
      }
    }
  }

  @Override
  public TableDao tableDao() {
    if (_tableDao != null) {
      return _tableDao;
    } else {
      synchronized(this) {
        if(_tableDao == null) {
          _tableDao = new TableDao_Impl(this);
        }
        return _tableDao;
      }
    }
  }

  @Override
  public TableOrderDao tableOrderDao() {
    if (_tableOrderDao != null) {
      return _tableOrderDao;
    } else {
      synchronized(this) {
        if(_tableOrderDao == null) {
          _tableOrderDao = new TableOrderDao_Impl(this);
        }
        return _tableOrderDao;
      }
    }
  }

  @Override
  public TableOrderItemDao tableOrderItemDao() {
    if (_tableOrderItemDao != null) {
      return _tableOrderItemDao;
    } else {
      synchronized(this) {
        if(_tableOrderItemDao == null) {
          _tableOrderItemDao = new TableOrderItemDao_Impl(this);
        }
        return _tableOrderItemDao;
      }
    }
  }
}
