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
public final class TableDao_Impl implements TableDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TableEntity> __insertionAdapterOfTableEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateStatus;

  public TableDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTableEntity = new EntityInsertionAdapter<TableEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `tables` (`tableId`,`number`,`status`,`openedAt`,`closedAt`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TableEntity entity) {
        if (entity.getTableId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getTableId());
        }
        statement.bindLong(2, entity.getNumber());
        if (entity.getStatus() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getStatus());
        }
        if (entity.getOpenedAt() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getOpenedAt());
        }
        if (entity.getClosedAt() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getClosedAt());
        }
      }
    };
    this.__preparedStmtOfUpdateStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE tables SET status = ?, openedAt = ? WHERE tableId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsertTable(final TableEntity table, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTableEntity.insert(table);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateStatus(final String tableId, final String status, final Long openedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateStatus.acquire();
        int _argIndex = 1;
        if (status == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, status);
        }
        _argIndex = 2;
        if (openedAt == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindLong(_argIndex, openedAt);
        }
        _argIndex = 3;
        if (tableId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, tableId);
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
          __preparedStmtOfUpdateStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getAll(final Continuation<? super List<TableEntity>> $completion) {
    final String _sql = "SELECT * FROM tables ORDER BY number";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TableEntity>>() {
      @Override
      @NonNull
      public List<TableEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTableId = CursorUtil.getColumnIndexOrThrow(_cursor, "tableId");
          final int _cursorIndexOfNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "number");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfOpenedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "openedAt");
          final int _cursorIndexOfClosedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "closedAt");
          final List<TableEntity> _result = new ArrayList<TableEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TableEntity _item;
            final String _tmpTableId;
            if (_cursor.isNull(_cursorIndexOfTableId)) {
              _tmpTableId = null;
            } else {
              _tmpTableId = _cursor.getString(_cursorIndexOfTableId);
            }
            final int _tmpNumber;
            _tmpNumber = _cursor.getInt(_cursorIndexOfNumber);
            final String _tmpStatus;
            if (_cursor.isNull(_cursorIndexOfStatus)) {
              _tmpStatus = null;
            } else {
              _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            }
            final Long _tmpOpenedAt;
            if (_cursor.isNull(_cursorIndexOfOpenedAt)) {
              _tmpOpenedAt = null;
            } else {
              _tmpOpenedAt = _cursor.getLong(_cursorIndexOfOpenedAt);
            }
            final Long _tmpClosedAt;
            if (_cursor.isNull(_cursorIndexOfClosedAt)) {
              _tmpClosedAt = null;
            } else {
              _tmpClosedAt = _cursor.getLong(_cursorIndexOfClosedAt);
            }
            _item = new TableEntity(_tmpTableId,_tmpNumber,_tmpStatus,_tmpOpenedAt,_tmpClosedAt);
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
