package br.com.ruy.pos.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.lang.Long;
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
public final class TableOrderDao_Impl implements TableOrderDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TableOrderEntity> __insertionAdapterOfTableOrderEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateTotal;

  public TableOrderDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTableOrderEntity = new EntityInsertionAdapter<TableOrderEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `table_orders` (`comandaId`,`tableId`,`status`,`totalCents`,`openedAt`,`closedAt`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TableOrderEntity entity) {
        if (entity.getComandaId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getComandaId());
        }
        if (entity.getTableId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getTableId());
        }
        if (entity.getStatus() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getStatus());
        }
        statement.bindLong(4, entity.getTotalCents());
        statement.bindLong(5, entity.getOpenedAt());
        if (entity.getClosedAt() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getClosedAt());
        }
      }
    };
    this.__preparedStmtOfUpdateTotal = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE table_orders SET totalCents = ? WHERE comandaId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final TableOrderEntity comanda,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTableOrderEntity.insert(comanda);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTotal(final String comandaId, final int totalCents,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateTotal.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, totalCents);
        _argIndex = 2;
        if (comandaId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, comandaId);
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
          __preparedStmtOfUpdateTotal.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getOpenByTable(final String tableId,
      final Continuation<? super TableOrderEntity> $completion) {
    final String _sql = "SELECT * FROM table_orders WHERE tableId = ? AND status = 'ABERTA' LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (tableId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, tableId);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TableOrderEntity>() {
      @Override
      @Nullable
      public TableOrderEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfComandaId = CursorUtil.getColumnIndexOrThrow(_cursor, "comandaId");
          final int _cursorIndexOfTableId = CursorUtil.getColumnIndexOrThrow(_cursor, "tableId");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfTotalCents = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCents");
          final int _cursorIndexOfOpenedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "openedAt");
          final int _cursorIndexOfClosedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "closedAt");
          final TableOrderEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpComandaId;
            if (_cursor.isNull(_cursorIndexOfComandaId)) {
              _tmpComandaId = null;
            } else {
              _tmpComandaId = _cursor.getString(_cursorIndexOfComandaId);
            }
            final String _tmpTableId;
            if (_cursor.isNull(_cursorIndexOfTableId)) {
              _tmpTableId = null;
            } else {
              _tmpTableId = _cursor.getString(_cursorIndexOfTableId);
            }
            final String _tmpStatus;
            if (_cursor.isNull(_cursorIndexOfStatus)) {
              _tmpStatus = null;
            } else {
              _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            }
            final int _tmpTotalCents;
            _tmpTotalCents = _cursor.getInt(_cursorIndexOfTotalCents);
            final long _tmpOpenedAt;
            _tmpOpenedAt = _cursor.getLong(_cursorIndexOfOpenedAt);
            final Long _tmpClosedAt;
            if (_cursor.isNull(_cursorIndexOfClosedAt)) {
              _tmpClosedAt = null;
            } else {
              _tmpClosedAt = _cursor.getLong(_cursorIndexOfClosedAt);
            }
            _result = new TableOrderEntity(_tmpComandaId,_tmpTableId,_tmpStatus,_tmpTotalCents,_tmpOpenedAt,_tmpClosedAt);
          } else {
            _result = null;
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
  public Object getOpen(final Continuation<? super List<TableOrderEntity>> $completion) {
    final String _sql = "SELECT * FROM table_orders WHERE status = 'ABERTA' ORDER BY openedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TableOrderEntity>>() {
      @Override
      @NonNull
      public List<TableOrderEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfComandaId = CursorUtil.getColumnIndexOrThrow(_cursor, "comandaId");
          final int _cursorIndexOfTableId = CursorUtil.getColumnIndexOrThrow(_cursor, "tableId");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfTotalCents = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCents");
          final int _cursorIndexOfOpenedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "openedAt");
          final int _cursorIndexOfClosedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "closedAt");
          final List<TableOrderEntity> _result = new ArrayList<TableOrderEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TableOrderEntity _item;
            final String _tmpComandaId;
            if (_cursor.isNull(_cursorIndexOfComandaId)) {
              _tmpComandaId = null;
            } else {
              _tmpComandaId = _cursor.getString(_cursorIndexOfComandaId);
            }
            final String _tmpTableId;
            if (_cursor.isNull(_cursorIndexOfTableId)) {
              _tmpTableId = null;
            } else {
              _tmpTableId = _cursor.getString(_cursorIndexOfTableId);
            }
            final String _tmpStatus;
            if (_cursor.isNull(_cursorIndexOfStatus)) {
              _tmpStatus = null;
            } else {
              _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            }
            final int _tmpTotalCents;
            _tmpTotalCents = _cursor.getInt(_cursorIndexOfTotalCents);
            final long _tmpOpenedAt;
            _tmpOpenedAt = _cursor.getLong(_cursorIndexOfOpenedAt);
            final Long _tmpClosedAt;
            if (_cursor.isNull(_cursorIndexOfClosedAt)) {
              _tmpClosedAt = null;
            } else {
              _tmpClosedAt = _cursor.getLong(_cursorIndexOfClosedAt);
            }
            _item = new TableOrderEntity(_tmpComandaId,_tmpTableId,_tmpStatus,_tmpTotalCents,_tmpOpenedAt,_tmpClosedAt);
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
