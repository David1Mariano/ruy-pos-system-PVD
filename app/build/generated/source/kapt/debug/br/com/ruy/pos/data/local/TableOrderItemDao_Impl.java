package br.com.ruy.pos.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TableOrderItemDao_Impl implements TableOrderItemDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TableOrderItemEntity> __insertionAdapterOfTableOrderItemEntity;

  public TableOrderItemDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTableOrderItemEntity = new EntityInsertionAdapter<TableOrderItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `table_order_items` (`itemId`,`comandaId`,`productId`,`productName`,`unitCents`,`qty`,`addedAt`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TableOrderItemEntity entity) {
        if (entity.getItemId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getItemId());
        }
        if (entity.getComandaId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getComandaId());
        }
        if (entity.getProductId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getProductId());
        }
        if (entity.getProductName() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getProductName());
        }
        statement.bindLong(5, entity.getUnitCents());
        statement.bindLong(6, entity.getQty());
        statement.bindLong(7, entity.getAddedAt());
      }
    };
  }

  @Override
  public Object insert(final TableOrderItemEntity item,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTableOrderItemEntity.insert(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getItems(final String comandaId,
      final Continuation<? super List<TableOrderItemEntity>> $completion) {
    final String _sql = "SELECT * FROM table_order_items WHERE comandaId = ? ORDER BY addedAt";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (comandaId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, comandaId);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TableOrderItemEntity>>() {
      @Override
      @NonNull
      public List<TableOrderItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfItemId = CursorUtil.getColumnIndexOrThrow(_cursor, "itemId");
          final int _cursorIndexOfComandaId = CursorUtil.getColumnIndexOrThrow(_cursor, "comandaId");
          final int _cursorIndexOfProductId = CursorUtil.getColumnIndexOrThrow(_cursor, "productId");
          final int _cursorIndexOfProductName = CursorUtil.getColumnIndexOrThrow(_cursor, "productName");
          final int _cursorIndexOfUnitCents = CursorUtil.getColumnIndexOrThrow(_cursor, "unitCents");
          final int _cursorIndexOfQty = CursorUtil.getColumnIndexOrThrow(_cursor, "qty");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final List<TableOrderItemEntity> _result = new ArrayList<TableOrderItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TableOrderItemEntity _item;
            final String _tmpItemId;
            if (_cursor.isNull(_cursorIndexOfItemId)) {
              _tmpItemId = null;
            } else {
              _tmpItemId = _cursor.getString(_cursorIndexOfItemId);
            }
            final String _tmpComandaId;
            if (_cursor.isNull(_cursorIndexOfComandaId)) {
              _tmpComandaId = null;
            } else {
              _tmpComandaId = _cursor.getString(_cursorIndexOfComandaId);
            }
            final String _tmpProductId;
            if (_cursor.isNull(_cursorIndexOfProductId)) {
              _tmpProductId = null;
            } else {
              _tmpProductId = _cursor.getString(_cursorIndexOfProductId);
            }
            final String _tmpProductName;
            if (_cursor.isNull(_cursorIndexOfProductName)) {
              _tmpProductName = null;
            } else {
              _tmpProductName = _cursor.getString(_cursorIndexOfProductName);
            }
            final int _tmpUnitCents;
            _tmpUnitCents = _cursor.getInt(_cursorIndexOfUnitCents);
            final int _tmpQty;
            _tmpQty = _cursor.getInt(_cursorIndexOfQty);
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            _item = new TableOrderItemEntity(_tmpItemId,_tmpComandaId,_tmpProductId,_tmpProductName,_tmpUnitCents,_tmpQty,_tmpAddedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
