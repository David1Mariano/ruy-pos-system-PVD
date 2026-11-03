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
import java.lang.Integer;
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
public final class PaymentDao_Impl implements PaymentDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PaymentEntity> __insertionAdapterOfPaymentEntity;

  public PaymentDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPaymentEntity = new EntityInsertionAdapter<PaymentEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `payments` (`paymentId`,`orderId`,`amountCents`,`method`,`status`,`receivedCents`,`changeCents`,`installments`,`transactionRef`,`createdAt`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PaymentEntity entity) {
        if (entity.getPaymentId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getPaymentId());
        }
        if (entity.getOrderId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getOrderId());
        }
        statement.bindLong(3, entity.getAmountCents());
        if (entity.getMethod() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getMethod());
        }
        if (entity.getStatus() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getStatus());
        }
        if (entity.getReceivedCents() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getReceivedCents());
        }
        if (entity.getChangeCents() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getChangeCents());
        }
        statement.bindLong(8, entity.getInstallments());
        if (entity.getTransactionRef() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getTransactionRef());
        }
        statement.bindLong(10, entity.getCreatedAt());
      }
    };
  }

  @Override
  public Object insert(final PaymentEntity payment, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPaymentEntity.insert(payment);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getByOrder(final String orderId,
      final Continuation<? super List<PaymentEntity>> $completion) {
    final String _sql = "SELECT * FROM payments WHERE orderId = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (orderId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, orderId);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PaymentEntity>>() {
      @Override
      @NonNull
      public List<PaymentEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPaymentId = CursorUtil.getColumnIndexOrThrow(_cursor, "paymentId");
          final int _cursorIndexOfOrderId = CursorUtil.getColumnIndexOrThrow(_cursor, "orderId");
          final int _cursorIndexOfAmountCents = CursorUtil.getColumnIndexOrThrow(_cursor, "amountCents");
          final int _cursorIndexOfMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "method");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfReceivedCents = CursorUtil.getColumnIndexOrThrow(_cursor, "receivedCents");
          final int _cursorIndexOfChangeCents = CursorUtil.getColumnIndexOrThrow(_cursor, "changeCents");
          final int _cursorIndexOfInstallments = CursorUtil.getColumnIndexOrThrow(_cursor, "installments");
          final int _cursorIndexOfTransactionRef = CursorUtil.getColumnIndexOrThrow(_cursor, "transactionRef");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<PaymentEntity> _result = new ArrayList<PaymentEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PaymentEntity _item;
            final String _tmpPaymentId;
            if (_cursor.isNull(_cursorIndexOfPaymentId)) {
              _tmpPaymentId = null;
            } else {
              _tmpPaymentId = _cursor.getString(_cursorIndexOfPaymentId);
            }
            final String _tmpOrderId;
            if (_cursor.isNull(_cursorIndexOfOrderId)) {
              _tmpOrderId = null;
            } else {
              _tmpOrderId = _cursor.getString(_cursorIndexOfOrderId);
            }
            final int _tmpAmountCents;
            _tmpAmountCents = _cursor.getInt(_cursorIndexOfAmountCents);
            final String _tmpMethod;
            if (_cursor.isNull(_cursorIndexOfMethod)) {
              _tmpMethod = null;
            } else {
              _tmpMethod = _cursor.getString(_cursorIndexOfMethod);
            }
            final String _tmpStatus;
            if (_cursor.isNull(_cursorIndexOfStatus)) {
              _tmpStatus = null;
            } else {
              _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            }
            final Integer _tmpReceivedCents;
            if (_cursor.isNull(_cursorIndexOfReceivedCents)) {
              _tmpReceivedCents = null;
            } else {
              _tmpReceivedCents = _cursor.getInt(_cursorIndexOfReceivedCents);
            }
            final Integer _tmpChangeCents;
            if (_cursor.isNull(_cursorIndexOfChangeCents)) {
              _tmpChangeCents = null;
            } else {
              _tmpChangeCents = _cursor.getInt(_cursorIndexOfChangeCents);
            }
            final int _tmpInstallments;
            _tmpInstallments = _cursor.getInt(_cursorIndexOfInstallments);
            final String _tmpTransactionRef;
            if (_cursor.isNull(_cursorIndexOfTransactionRef)) {
              _tmpTransactionRef = null;
            } else {
              _tmpTransactionRef = _cursor.getString(_cursorIndexOfTransactionRef);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new PaymentEntity(_tmpPaymentId,_tmpOrderId,_tmpAmountCents,_tmpMethod,_tmpStatus,_tmpReceivedCents,_tmpChangeCents,_tmpInstallments,_tmpTransactionRef,_tmpCreatedAt);
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
