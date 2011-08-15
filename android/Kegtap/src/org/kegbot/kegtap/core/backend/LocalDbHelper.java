/**
 *
 */
package org.kegbot.kegtap.core.backend;

import org.kegbot.proto.Api.RecordTemperatureRequest;
import org.kegbot.proto.Internal.PendingPour;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class LocalDbHelper extends SQLiteOpenHelper {

  private static final String TAG = LocalDbHelper.class.getSimpleName();

  private static final int DATABASE_VERSION = 1;
  private static final String DATABASE_NAME = "kegbot_localdb.db";

  public static final String TABLE_NAME = "pending_items";

  public static final String COLUMN_NAME_ID = BaseColumns._ID;
  public static final String COLUMN_NAME_ADDED_DATE = "added";
  public static final String COLUMN_NAME_TYPE = "type";
  public static final String COLUMN_NAME_RECORD = "record";

  /**
   * @param context
   */
  public LocalDbHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
        + COLUMN_NAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + COLUMN_NAME_ADDED_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
        + COLUMN_NAME_TYPE + " TEXT NOT NULL, "
        + COLUMN_NAME_RECORD + " BLOB)");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
        + ", destroying all old data");
    db.execSQL("DROP TABLE IF EXISTS "  + TABLE_NAME);
    onCreate(db);
  }

  /**
   * Deletes a single row from the database.
   *
   * @param db
   * @param rowId
   * @return
   */
  public static int deleteRow(final SQLiteDatabase db, int rowId) {
    final String where = COLUMN_NAME_ID + " = ?";
    final String[] whereArgs = {
        String.valueOf(rowId)
    };
    return db.delete(TABLE_NAME, where, whereArgs);
  }

  public static boolean insertRecord(final SQLiteDatabase db, final AbstractMessage record) {
    final String type;
    if (record instanceof PendingPour) {
      type = "pour";
    } else if (record instanceof RecordTemperatureRequest) {
      type = "thermo";
    } else {
      Log.w(TAG, "Unknown record type; dropping.");
      return false;
    }
    Log.d(TAG, "Request is a " + type);

    final ContentValues values = new ContentValues();

    values.put(LocalDbHelper.COLUMN_NAME_TYPE, type);
    values.put(LocalDbHelper.COLUMN_NAME_RECORD, record.toByteArray());
    db.insert(TABLE_NAME, null, values);
    return true;
  }

  public static AbstractMessage getCurrentRow(final SQLiteDatabase db, final Cursor cursor) throws InvalidProtocolBufferException {
    final String type = cursor.getString(cursor.getColumnIndex(LocalDbHelper.COLUMN_NAME_TYPE));
    final byte[] data = cursor.getBlob(cursor.getColumnIndex(LocalDbHelper.COLUMN_NAME_RECORD));
    Log.w(TAG, "getCurrentRow: " + type);
    if ("pour".equals(type)) {
      return PendingPour.parseFrom(data);
    } else if ("thermo".equals(type)) {
      return RecordTemperatureRequest.parseFrom(data);
    } else {
      return null;
    }
  }

  public static int deleteCurrentRow(final SQLiteDatabase db, final Cursor cursor) {
    final int rowId = cursor.getInt(cursor.getColumnIndex(LocalDbHelper.COLUMN_NAME_ID));
    return deleteRow(db, rowId);
  }

}
