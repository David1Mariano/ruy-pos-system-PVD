package br.com.ruy.pos.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
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
public final class OrderDao_Impl implements OrderDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<OrderEntity> __insertionAdapterOfOrderEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateSyncStatus;

  public OrderDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfOrderEntity = new EntityInsertionAdapter<OrderEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `orders` (`id`,`totalCents`,`paymentStatus`,`syncStatus`,`itemsJson`,`createdAt`,`operatorId`,`deviceId`,`stoneTransactionId`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final OrderEntity entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        statement.bindLong(2, entity.getTotalCents());
        if (entity.getPaymentStatus() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getPaymentStatus());
        }
        if (entity.getSyncStatus() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getSyncStatus());
        }
        if (entity.getItemsJson() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getItemsJson());
        }
        statement.bindLong(6, entity.getCreatedAt());
        if (entity.getOperatorId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getOperatorId());
        }
        if (entity.getDeviceId() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getDeviceId());
        }
        if (entity.getStoneTransactionId() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getStoneTransactionId());
        }
      }
    };
    this.__preparedStmtOfUpdateSyncStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE orders SET syncStatus = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final OrderEntity order, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfOrderEntity.insert(order);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSyncStatus(final String id, final String status,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateSyncStatus.acquire();
        int _argIndex = 1;
        if (status == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, status);
        }
        _argIndex = 2;
        if (id == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, id);
        }
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateSyncStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object pending(final Continuation<? super List<OrderEntity>> $completion) {
    final String _sql = "SELECT * FROM orders WHERE syncStatus = 'PENDING' ORDER BY createdAt";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<OrderEntity>>() {
      @Override
      @NonNull
      public List<OrderEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTotalCents = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCents");
          final int _cursorIndexOfPaymentStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "paymentStatus");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfItemsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "itemsJson");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfOperatorId = CursorUtil.getColumnIndexOrThrow(_cursor, "operatorId");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfStoneTransactionId = CursorUtil.getColumnIndexOrThrow(_cursor, "stoneTransactionId");
          final List<OrderEntity> _result = new ArrayList<OrderEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final OrderEntity _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final int _tmpTotalCents;
            _tmpTotalCents = _cursor.getInt(_cursorIndexOfTotalCents);
            final String _tmpPaymentStatus;
            if (_cursor.isNull(_cursorIndexOfPaymentStatus)) {
              _tmpPaymentStatus = null;
            } else {
              _tmpPaymentStatus = _cursor.getString(_cursorIndexOfPaymentStatus);
            }
            final String _tmpSyncStatus;
            if (_cursor.isNull(_cursorIndexOfSyncStatus)) {
              _tmpSyncStatus = null;
            } else {
              _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            }
            final String _tmpItemsJson;
            if (_cursor.isNull(_cursorIndexOfItemsJson)) {
              _tmpItemsJson = null;
            } else {
              _tmpItemsJson = _cursor.getString(_cursorIndexOfItemsJson);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpOperatorId;
            if (_cursor.isNull(_cursorIndexOfOperatorId)) {
              _tmpOperatorId = null;
            } else {
              _tmpOperatorId = _cursor.getString(_cursorIndexOfOperatorId);
            }
            final String _tmpDeviceId;
            if (_cursor.isNull(_cursorIndexOfDeviceId)) {
              _tmpDeviceId = null;
            } else {
              _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            }
            final String _tmpStoneTransactionId;
            if (_cursor.isNull(_cursorIndexOfStoneTransactionId)) {
              _tmpStoneTransactionId = null;
            } else {
              _tmpStoneTransactionId = _cursor.getString(_cursorIndexOfStoneTransactionId);
            }
            _item = new OrderEntity(_tmpId,_tmpTotalCents,_tmpPaymentStatus,_tmpSyncStatus,_tmpItemsJson,_tmpCreatedAt,_tmpOperatorId,_tmpDeviceId,_tmpStoneTransactionId);
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

  @Override
  public Object all(final Continuation<? super List<OrderEntity>> $completion) {
    final String _sql = "SELECT * FROM orders ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<OrderEntity>>() {
      @Override
      @NonNull
      public List<OrderEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTotalCents = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCents");
          final int _cursorIndexOfPaymentStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "paymentStatus");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfItemsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "itemsJson");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfOperatorId = CursorUtil.getColumnIndexOrThrow(_cursor, "operatorId");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfStoneTransactionId = CursorUtil.getColumnIndexOrThrow(_cursor, "stoneTransactionId");
          final List<OrderEntity> _result = new ArrayList<OrderEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final OrderEntity _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final int _tmpTotalCents;
            _tmpTotalCents = _cursor.getInt(_cursorIndexOfTotalCents);
            final String _tmpPaymentStatus;
            if (_cursor.isNull(_cursorIndexOfPaymentStatus)) {
              _tmpPaymentStatus = null;
            } else {
              _tmpPaymentStatus = _cursor.getString(_cursorIndexOfPaymentStatus);
            }
            final String _tmpSyncStatus;
            if (_cursor.isNull(_cursorIndexOfSyncStatus)) {
              _tmpSyncStatus = null;
            } else {
              _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            }
            final String _tmpItemsJson;
            if (_cursor.isNull(_cursorIndexOfItemsJson)) {
              _tmpItemsJson = null;
            } else {
              _tmpItemsJson = _cursor.getString(_cursorIndexOfItemsJson);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpOperatorId;
            if (_cursor.isNull(_cursorIndexOfOperatorId)) {
              _tmpOperatorId = null;
            } else {
              _tmpOperatorId = _cursor.getString(_cursorIndexOfOperatorId);
            }
            final String _tmpDeviceId;
            if (_cursor.isNull(_cursorIndexOfDeviceId)) {
              _tmpDeviceId = null;
            } else {
              _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            }
            final String _tmpStoneTransactionId;
            if (_cursor.isNull(_cursorIndexOfStoneTransactionId)) {
              _tmpStoneTransactionId = null;
            } else {
              _tmpStoneTransactionId = _cursor.getString(_cursorIndexOfStoneTransactionId);
            }
            _item = new OrderEntity(_tmpId,_tmpTotalCents,_tmpPaymentStatus,_tmpSyncStatus,_tmpItemsJson,_tmpCreatedAt,_tmpOperatorId,_tmpDeviceId,_tmpStoneTransactionId);
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
