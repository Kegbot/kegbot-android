/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
 *
 * This file is part of the Kegtab package from the Kegbot project. For
 * more information on Kegtab or Kegbot, see <http://kegbot.org/>.
 *
 * Kegtab is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 2.
 *
 * Kegtab is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with Kegtab. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kegbot.backend;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.kegbot.app.util.TimeSeries;
import org.kegbot.proto.Models.Beverage;
import org.kegbot.proto.Models.BeverageProducer;
import org.kegbot.proto.Models.Controller;
import org.kegbot.proto.Models.Drink;
import org.kegbot.proto.Models.FlowMeter;
import org.kegbot.proto.Models.Keg;
import org.kegbot.proto.Models.KegTap;

import java.util.List;

import javax.annotation.Nullable;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class LocalBackendDbHelper extends SQLiteOpenHelper {

  private static final String TAG = LocalBackendDbHelper.class.getSimpleName();

  private static final int DATABASE_VERSION = 2;

  @VisibleForTesting
  static final String DATABASE_NAME = "local_backend.db";

  private static final String COLUMN_ID = BaseColumns._ID;

  private static final String TABLE_KEGS = "kegs";
  private static final String COLUMN_KEG_FULL_VOLUME_ML = "full_volume_ml";
  private static final String COLUMN_KEG_SERVED_VOLUME_ML = "served_volume_ml";
  private static final String COLUMN_KEG_START_TIME = "start_time";
  private static final String COLUMN_KEG_END_TIME = "end_time";
  private static final String COLUMN_KEG_ONLINE = "online";
  private static final String COLUMN_KEG_KEG_TYPE = "keg_type";
  private static final String COLUMN_KEG_BEER_TYPE_NAME = "beer_name";
  private static final String COLUMN_KEG_BEER_BREWER_NAME = "brewer_name";
  private static final String COLUMN_KEG_BEER_STYLE_NAME = "style_name";

  private static final String TABLE_TAPS = "taps";
  private static final String COLUMN_TAP_TAP_NAME = "tap_name";
  private static final String COLUMN_TAP_CURRENT_KEG = "current_keg_id";
  private static final String COLUMN_TAP_SORT_ORDER = "sort_order";
  private static final String COLUMN_TAP_FLOW_METER_ID = "flow_meter";

  private static final String TABLE_DRINKS = "drinks";
  private static final String COLUMN_DRINK_KEG_ID = "keg_id";
  private static final String COLUMN_DRINK_TICKS = "ticks";
  private static final String COLUMN_DRINK_VOLUME_ML = "volume_ml";
  private static final String COLUMN_DRINK_TIME = "time";
  private static final String COLUMN_DRINK_USERNAME = "username";
  private static final String COLUMN_DRINK_PICTURE_URL = "picture_url";
  private static final String COLUMN_DRINK_SHOUT = "shout";

  private static final String TABLE_CONTROLLERS = "controllers";
  private static final String COLUMN_CONTROLLER_NAME = "name";
  private static final String COLUMN_CONTROLLER_MODEL_NAME = "model_name";
  private static final String COLUMN_CONTROLLER_SERIAL_NUMBER = "serial_number";

  private static final String TABLE_FLOW_METERS = "flow_meters";
  private static final String COLUMN_FLOW_METER_CONTROLLER_ID = "controller_id";
  private static final String COLUMN_FLOW_METER_PORT_NAME = "port_name";
  private static final String COLUMN_FLOW_METER_TICKS_PER_ML = "ticks_per_ml";

  /**
   * @param context
   */
  public LocalBackendDbHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    Log.d(TAG, "Creating table " + TABLE_KEGS);
    db.execSQL("CREATE TABLE " + TABLE_KEGS + " ("
        + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + COLUMN_KEG_START_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
        + COLUMN_KEG_END_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
        + COLUMN_KEG_BEER_TYPE_NAME + " TEXT NOT NULL, "
        + COLUMN_KEG_BEER_BREWER_NAME + " TEXT NOT NULL, "
        + COLUMN_KEG_BEER_STYLE_NAME + " TEXT NOT NULL, "
        + COLUMN_KEG_ONLINE + " INTEGER NOT NULL DEFAULT 1, "
        + COLUMN_KEG_KEG_TYPE + " TEXT NOT NULL, "
        + COLUMN_KEG_FULL_VOLUME_ML + " REAL NOT NULL, "
        + COLUMN_KEG_SERVED_VOLUME_ML + " REAL NOT NULL)");

    Log.d(TAG, "Creating table " + TABLE_TAPS);
    db.execSQL("CREATE TABLE " + TABLE_TAPS + " ("
        + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + COLUMN_TAP_TAP_NAME + " TEXT NOT NULL, "
        + COLUMN_TAP_SORT_ORDER + " INTEGER NOT NULL DEFAULT 0, "
        + COLUMN_TAP_FLOW_METER_ID + " INTEGER, "
        + COLUMN_TAP_CURRENT_KEG + " INTEGER)");

    Log.d(TAG, "Creating table " + TABLE_DRINKS);
    db.execSQL("CREATE TABLE " + TABLE_DRINKS + " ("
        + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + COLUMN_DRINK_KEG_ID + " INTEGER NOT NULL, "
        + COLUMN_DRINK_TICKS + " INTEGER NOT NULL, "
        + COLUMN_DRINK_VOLUME_ML + " INTEGER NOT NULL, "
        + COLUMN_DRINK_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
        + COLUMN_DRINK_USERNAME + " STRING, "
        + COLUMN_DRINK_PICTURE_URL + " STRING, "
        + COLUMN_DRINK_SHOUT + " TEXT)");

    Log.d(TAG, "Creating table " + TABLE_CONTROLLERS);
    db.execSQL("CREATE TABLE " + TABLE_CONTROLLERS + " ("
        + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + COLUMN_CONTROLLER_NAME + " STRING UNIQUE NOT NULL, "
        + COLUMN_CONTROLLER_MODEL_NAME + " STRING, "
        + COLUMN_CONTROLLER_SERIAL_NUMBER + " STRING)");

    Log.d(TAG, "Creating table " + TABLE_FLOW_METERS);
    db.execSQL("CREATE TABLE " + TABLE_FLOW_METERS + " ("
        + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + COLUMN_FLOW_METER_CONTROLLER_ID + " INTEGER NOT NULL, "
        + COLUMN_FLOW_METER_PORT_NAME + " STRING NOT NULL, "
        + COLUMN_FLOW_METER_TICKS_PER_ML + " REAL)");

    setDefaults(db);
  }

  private void setDefaults(SQLiteDatabase db) {
    Controller controller = Controller.newBuilder()
        .setId(1)
        .setName("kegboard")
        .build();
    controller = createOrUpdateController(controller, db);
    Log.d(TAG, "Created controller: " + controller);

    FlowMeter meter = FlowMeter.newBuilder()
        .setId(1)
        .setName("kegboard")
        .setPortName("flow0")
        .setTicksPerMl(2.2f)
        .setController(controller)
        .build();
    meter = createOrUpdateFlowMeter(meter, db);
    Log.d(TAG, "Created meter: " + meter);

    KegTap tap = KegTap.newBuilder()
        .setName("Main Tap")
        .setMeter(meter)
        .setId(1)
        .build();
    tap = createOrUpdateTap(tap, db);
    Log.d(TAG, "Created tap: " + tap);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
    if (oldVersion != newVersion) {
      db.execSQL("DROP TABLE IF EXISTS "  + TABLE_DRINKS);
      db.execSQL("DROP TABLE IF EXISTS "  + TABLE_TAPS);
      db.execSQL("DROP TABLE IF EXISTS "  + TABLE_KEGS);
      db.execSQL("DROP TABLE IF EXISTS "  + TABLE_CONTROLLERS);
      db.execSQL("DROP TABLE IF EXISTS "  + TABLE_FLOW_METERS);
      onCreate(db);
    }
  }

  List<Controller> getAllControllers() {
    final List<Controller> result = Lists.newArrayList();
    final SQLiteDatabase db = getReadableDatabase();
    try {
      final Cursor cursor =
          db.query(TABLE_CONTROLLERS,
              null, null, null, null, null, COLUMN_ID + " ASC");
      try {
        if (cursor.getCount() == 0) {
          return result;
        }

        Log.d(TAG, "getAllControllers: count=" + cursor.getCount());

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
          final Controller controller = controllerFromCursor(cursor);
          result.add(controller);
          cursor.moveToNext();
        }
      } finally {
        cursor.close();
      }
    } finally {
    }
    return result;
  }


  Controller getController(int controllerId) {
    final SQLiteDatabase db = getReadableDatabase();
    try {
      return getController(controllerId, db);
    } finally {
    }
  }

  List<FlowMeter> getAllFlowMeters() {
    final SQLiteDatabase db = getReadableDatabase();
    final List<FlowMeter> result = Lists.newArrayList();
    try {
      final Cursor cursor =
          db.query(TABLE_FLOW_METERS,
              null, null, null, null, null, COLUMN_ID + " ASC");
      try {
        if (cursor.getCount() == 0) {
          return result;
        }

        Log.d(TAG, "getAllFlowMeters: count=" + cursor.getCount());

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
          final FlowMeter tap = flowMeterFromCursor(cursor, db);
          result.add(tap);
          cursor.moveToNext();
        }
      } finally {
        cursor.close();
      }
    } finally {
    }
    return result;
  }

  FlowMeter getFlowMeter(int flowMeterId) {
    final SQLiteDatabase db = getReadableDatabase();
    return getFlowMeter(flowMeterId, db);
  }

  List<KegTap> getAllTaps() {
    final SQLiteDatabase db = getReadableDatabase();
    final List<KegTap> result = Lists.newArrayList();
    try {
      final Cursor cursor =
          db.query(TABLE_TAPS,
              null, null, null, null, null, COLUMN_TAP_SORT_ORDER + " ASC");
      try {
        if (cursor.getCount() == 0) {
          return result;
        }

        Log.d(TAG, "getTaps: count=" + cursor.getCount());

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
          final KegTap tap = kegTapFromCursor(cursor, db);
          result.add(tap);
          cursor.moveToNext();
        }
      } finally {
        cursor.close();
      }
    } finally {
    }
    return result;
  }

  KegTap getTap(int tapId) {
    final SQLiteDatabase db = getReadableDatabase();
    try {
      return getTap(tapId, db);
    } finally {
    }
  }

  KegTap getTap(String meterName) {
    for (final KegTap tap : getAllTaps()) {
      if (tap.getMeter().getName().equals(meterName)) {
        return tap;
      }
    }
    return null;
  }

  Keg getKeg(long kegId) {
    final SQLiteDatabase db = getReadableDatabase();
    try {
      final Cursor cursor = getRow(db, TABLE_KEGS, (int) kegId);
      try {
        return kegFromCursor(cursor);
      } finally {
        cursor.close();
      }
    } finally {
    }
  }

  Drink getDrink(int drinkId) {
    final SQLiteDatabase db = getReadableDatabase();
    try {
      final Cursor cursor = getRow(db, TABLE_DRINKS, drinkId);
      try {
        return drinkFromCursor(cursor);
      } finally {
        cursor.close();
      }
    } finally {
    }
  }

  Drink recordDrink(String tapName, long volumeMl, long ticks, @Nullable String shout,
      @Nullable String username, @Nullable String recordDate, long durationMillis,
      @Nullable TimeSeries timeSeries, @Nullable String pictureUrl) throws NotFoundException {
    if (!Strings.isNullOrEmpty(username)) {
      Log.w(TAG, "recordDrink: Ignoring username.");
    }

    final KegTap tap = getTap(tapName);
    if (tap == null) {
      throw new NotFoundException("Unknown tap: " + tapName);
    }

    final int kegId = tap.getCurrentKegId();
    if (kegId == 0) {
      throw new NotFoundException("Tap does not have an active keg.");
    }
    final Keg keg = getKeg(kegId);

    // Subtract volume from keg.
    final double servedMl = keg.getServedVolumeMl() + volumeMl;
    final double remainMl = keg.getRemainingVolumeMl() - volumeMl;
    final double percentFull = remainMl / keg.getFullVolumeMl() * 100.0;

    final Keg updatedKeg = Keg.newBuilder(keg)
        .setServedVolumeMl(servedMl)
        .setRemainingVolumeMl(remainMl)
        .setPercentFull(percentFull)
        .build();
    createOrUpdateKeg(updatedKeg);

    // Create the drink.
    final SQLiteDatabase db = getWritableDatabase();
    final ContentValues values = new ContentValues();
    values.put(COLUMN_DRINK_KEG_ID, Integer.valueOf(keg.getId()));
    values.put(COLUMN_DRINK_TICKS, Long.valueOf(ticks));
    values.put(COLUMN_DRINK_SHOUT, shout);
    values.put(COLUMN_DRINK_USERNAME, username); // TODO
    values.put(COLUMN_DRINK_VOLUME_ML, Long.valueOf(volumeMl));
    if (!Strings.isNullOrEmpty(pictureUrl)) {
      values.put(COLUMN_DRINK_PICTURE_URL, pictureUrl);
    }
    final long drinkId = db.insert(TABLE_DRINKS, null, values);
    if (drinkId < 0) {
      // TODO: handle differently?
      throw new SQLiteException("Error inserting drink");
    }

    return getDrink((int) drinkId);
  }

  Keg createOrUpdateKeg(final Keg keg) {
    final SQLiteDatabase db = getWritableDatabase();
    final long result = db.insertWithOnConflict(TABLE_KEGS, null, toContentValues(keg),
        SQLiteDatabase.CONFLICT_REPLACE);
    if (result < 0) {
      throw new SQLiteException("Error during insert: " + result);
    }
    return getKeg(result);
  }

  KegTap createOrUpdateTap(KegTap tap) {
    final SQLiteDatabase db = getWritableDatabase();
    return createOrUpdateTap(tap, db);
  }
  private KegTap createOrUpdateTap(KegTap tap, SQLiteDatabase db) {
    final long result = db.insertWithOnConflict(TABLE_TAPS, null, toContentValues(tap),
        SQLiteDatabase.CONFLICT_REPLACE);
    if (result < 0) {
      throw new SQLiteException("Error during insert: " + result);
    }
    Log.d(TAG, "Created/updated tap, id: " + result);
    return getTap((int) result, db);
  }

  Controller createOrUpdateController(Controller controller, SQLiteDatabase db) {
    final long result = db.insertWithOnConflict(TABLE_CONTROLLERS, null, toContentValues(controller),
        SQLiteDatabase.CONFLICT_REPLACE);
    if (result < 0) {
      throw new SQLiteException("Error during insert: " + result);
    }
    Log.d(TAG, "Created/updated controller, id: " + result);
    return getController((int) result, db);
  }

  Controller createOrUpdateController(Controller controller) {
    final SQLiteDatabase db = getWritableDatabase();
    return createOrUpdateController(controller, db);
  }

  FlowMeter createOrUpdateFlowMeter(FlowMeter meter, SQLiteDatabase db) {
    final long result = db.insertWithOnConflict(TABLE_FLOW_METERS, null, toContentValues(meter),
        SQLiteDatabase.CONFLICT_REPLACE);
    if (result < 0) {
      throw new SQLiteException("Error during insert: " + result);
    }
    Log.d(TAG, "Created/updated meter, id: " + result);
    return getFlowMeter((int) result, db);
  }

  FlowMeter createOrUpdateFlowMeter(FlowMeter meter) {
    final SQLiteDatabase db = getWritableDatabase();
    return createOrUpdateFlowMeter(meter, db);
  }

  KegTap connectTapToMeter(final KegTap tap, @Nullable final FlowMeter meter) {
    Log.d(TAG, "connectTapToMeter: " + tap.getName() + " meter: " + (meter != null ? meter.getId() : null));
    // Unlink a meter.
    if (meter == null) {
      return createOrUpdateTap(KegTap.newBuilder(tap)
          .clearMeter()
          .clearMeterName()
          .build());
    }

    Log.d(TAG, "Assigning tap " + tap.getName() + " to meter " + meter.getName());

    // Unlink meter on any other taps (should be just one).
    for (final KegTap otherTap : getAllTaps()) {
      if (!otherTap.hasMeter()) {
        continue;
      }
      if (otherTap.getMeter().getId() != meter.getId()) {
        continue;
      }
      Log.d(TAG, "-- unlinking tap " + otherTap.getName());
      connectTapToMeter(otherTap, null);
    }

    return createOrUpdateTap(KegTap.newBuilder(tap)
        .setMeter(meter)
        .setMeterName(meter.getName())
        .build());
  }

  boolean deleteTap(KegTap tap) {
    Preconditions.checkArgument(tap.getId() != 0);
    return deleteRow(TABLE_TAPS, tap.getId());
  }

  private boolean deleteRow(final String tableName, final int rowId) {
    final SQLiteDatabase db = getWritableDatabase();
    final int numRows = db.delete(tableName, COLUMN_ID + " = ?",
        new String[] {String.valueOf(rowId)});
    if (numRows > 1) {
      throw new IllegalStateException("Too many rows deleted!");
    }
    return numRows == 1;
  }

  private Cursor getRow(SQLiteDatabase db, String tableName, int rowId) {
    Log.d(TAG, "querying: " + tableName + ", row: " + rowId);
    final Cursor cursor =
        db.query(tableName, null, COLUMN_ID + " = ?",
            new String[] {String.valueOf(rowId)}, null, null, null);
    final int count = cursor.getCount();
    if (count == 0) {
      throw new SQLiteException("No matching keg found.");
    } else if (count > 1) {
      throw new SQLiteException("Multiple records found: " + count);
    }
    cursor.moveToFirst();
    return cursor;
  }

  private Controller getController(int controllerId, SQLiteDatabase db) {
    final Cursor cursor = getRow(db, TABLE_CONTROLLERS, controllerId);
    try {
      return controllerFromCursor(cursor);
    } finally {
      cursor.close();
    }
  }

  private FlowMeter getFlowMeter(int flowMeterId, SQLiteDatabase db) {
    final Cursor cursor = getRow(db, TABLE_FLOW_METERS, flowMeterId);
    try {
      return flowMeterFromCursor(cursor, db);
    } finally {
      cursor.close();
    }
  }

  private KegTap getTap(int tapId, SQLiteDatabase db) {
    final Cursor cursor = getRow(db, TABLE_TAPS, tapId);
    try {
      return kegTapFromCursor(cursor, db);
    } finally {
      cursor.close();
    }
  }

  private Controller controllerFromCursor(Cursor cursor) {
    final int id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
    if (id < 0) {
      throw new IllegalStateException("Bad column id: " + id);
    }
    final Controller controller = Controller.newBuilder()
        .setId(id)
        .setName(cursor.getString(cursor.getColumnIndex(COLUMN_CONTROLLER_NAME)))
        .setModelName(cursor.getString(cursor.getColumnIndex(COLUMN_CONTROLLER_MODEL_NAME)))
        .setSerialNumber(cursor.getString(cursor.getColumnIndex(COLUMN_CONTROLLER_SERIAL_NUMBER)))
        .build();
    return controller;
  }

  private FlowMeter flowMeterFromCursor(Cursor cursor, SQLiteDatabase db) {
    final int id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
    if (id < 0) {
      throw new IllegalStateException("Bad column id: " + id);
    }

    final int controllerId = cursor.getInt(cursor.getColumnIndex(COLUMN_FLOW_METER_CONTROLLER_ID));
    final String portName = cursor.getString(cursor.getColumnIndex(COLUMN_FLOW_METER_PORT_NAME));

    final Controller controller = getController(controllerId, db);
    final FlowMeter flowMeter = FlowMeter.newBuilder()
        .setId(id)
        .setPortName(portName)
        .setTicksPerMl(cursor.getFloat(cursor.getColumnIndex(COLUMN_FLOW_METER_TICKS_PER_ML)))
        .setController(controller)
        .setName(String.format("%s.%s", controller.getName(), portName))
        .build();
    return flowMeter;
  }

  private Keg kegFromCursor(final Cursor cursor) {
    final int kegId = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
    final double fullMl = cursor.getDouble(cursor.getColumnIndex(COLUMN_KEG_FULL_VOLUME_ML));
    final double servedMl = cursor.getDouble(cursor.getColumnIndex(COLUMN_KEG_SERVED_VOLUME_ML));
    final double remainMl = fullMl - servedMl;
    final double percentFull = remainMl / fullMl * 100.0f;

    final String beerName = cursor.getString(cursor.getColumnIndex(COLUMN_KEG_BEER_TYPE_NAME));
    final String brewerName = cursor.getString(cursor.getColumnIndex(COLUMN_KEG_BEER_BREWER_NAME));
    final String beerStyle = cursor.getString(cursor.getColumnIndex(COLUMN_KEG_BEER_STYLE_NAME));

    final Keg keg = Keg.newBuilder()
        .setId(kegId)
        .setStartTime(formatDatetime(cursor.getString(cursor.getColumnIndex(COLUMN_KEG_START_TIME))))
        .setEndTime(formatDatetime(cursor.getString(cursor.getColumnIndex(COLUMN_KEG_END_TIME))))
        .setPercentFull(percentFull)
        .setFullVolumeMl(fullMl)
        .setRemainingVolumeMl(remainMl)
        .setServedVolumeMl(servedMl)
        .setSpilledVolumeMl(0.0)
        .setKegType(cursor.getString(cursor.getColumnIndex(COLUMN_KEG_KEG_TYPE)))
        .setOnline(cursor.getInt(cursor.getColumnIndex(COLUMN_KEG_ONLINE)) != 0)
        .setBeverage(Beverage.newBuilder()
            .setId(0)
            .setBeverageType("beer")
            .setName(beerName)
            .setStyle(beerStyle)
            .setProducer(BeverageProducer.newBuilder()
                .setId(0)
                .setName(brewerName)
                .build())
            .build())
        .build();
    return keg;
  }

  private KegTap kegTapFromCursor(Cursor cursor, SQLiteDatabase db) {
    final int id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
    if (id < 0) {
      throw new IllegalStateException("Bad column id: " + id);
    }

    final int flowMeterId = cursor.getInt(cursor.getColumnIndex(COLUMN_TAP_FLOW_METER_ID));
    FlowMeter meter = null;
    if (flowMeterId > 0) {
      Log.d(TAG, "Getting flow meter, ID: " + flowMeterId);
      meter = getFlowMeter(flowMeterId, db);
      Log.d(TAG, "Got meter: " + meter);
    }

    final int currentKegId = cursor.getInt(cursor.getColumnIndex(COLUMN_TAP_CURRENT_KEG));
    Keg currentKeg = null;
    if (currentKegId > 0) {
      currentKeg = getKeg(currentKegId);
    }

    final KegTap.Builder builder = KegTap.newBuilder()
        .setId(id)
        .setName(cursor.getString(cursor.getColumnIndex(COLUMN_TAP_TAP_NAME)));
    if (currentKegId > 0) {
      builder.setCurrentKegId(currentKegId);
    }
    if (meter != null) {
      builder.setMeter(meter);
    }
    if (currentKeg != null) {
      builder.setCurrentKeg(currentKeg);
    }
    return builder.build();
  }

  private Drink drinkFromCursor(final Cursor cursor) {
    final int drinkId = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
    final int kegId = cursor.getInt(cursor.getColumnIndex(COLUMN_DRINK_KEG_ID));

    final Drink drink = Drink.newBuilder()
        .setId(drinkId)
        .setSessionId(0)
        .setTime("") // TODO
        .setVolumeMl(cursor.getFloat(cursor.getColumnIndex(COLUMN_DRINK_VOLUME_ML)))
        .setTicks((int) cursor.getLong(cursor.getColumnIndex(COLUMN_DRINK_TICKS)))
        .setShout(cursor.getString(cursor.getColumnIndex(COLUMN_DRINK_SHOUT)))
        .setUserId(cursor.getString(cursor.getColumnIndex(COLUMN_DRINK_USERNAME)))
        .setKegId(kegId)
        .build();
    return drink;
  }

  private static ContentValues toContentValues(final Keg keg) {
    final ContentValues values = new ContentValues();
    if (keg.getId() != 0) {
      values.put(COLUMN_ID, Integer.valueOf(keg.getId()));
    }
    values.put(COLUMN_KEG_ONLINE, Integer.valueOf(keg.getOnline() ? 1 : 0));
    values.put(COLUMN_KEG_KEG_TYPE, keg.getKegType());
    values.put(COLUMN_KEG_START_TIME, keg.getStartTime());
    values.put(COLUMN_KEG_END_TIME, keg.getStartTime());
    values.put(COLUMN_KEG_FULL_VOLUME_ML, Double.valueOf(keg.getFullVolumeMl()));
    values.put(COLUMN_KEG_SERVED_VOLUME_ML, Double.valueOf(keg.getServedVolumeMl()));
    values.put(COLUMN_KEG_BEER_TYPE_NAME, keg.getBeverage().getName());
    values.put(COLUMN_KEG_BEER_BREWER_NAME, keg.getBeverage().getProducer().getName());
    values.put(COLUMN_KEG_BEER_STYLE_NAME, keg.getBeverage().getStyle());
    return values;
  }

  private static ContentValues toContentValues(final Controller controller) {
    final ContentValues values = new ContentValues();
    if (controller.getId() != 0) {
      values.put(COLUMN_ID, Integer.valueOf(controller.getId()));
    }
    values.put(COLUMN_CONTROLLER_NAME, controller.getName());
    values.put(COLUMN_CONTROLLER_MODEL_NAME, controller.getModelName());
    values.put(COLUMN_CONTROLLER_SERIAL_NUMBER, controller.getSerialNumber());
    return values;
  }

  private static ContentValues toContentValues(final FlowMeter meter) {
    final ContentValues values = new ContentValues();
    if (meter.getId() != 0) {
      values.put(COLUMN_ID, Integer.valueOf(meter.getId()));
    }
    values.put(COLUMN_FLOW_METER_CONTROLLER_ID, Integer.valueOf(meter.getController().getId()));
    values.put(COLUMN_FLOW_METER_PORT_NAME, meter.getPortName());
    values.put(COLUMN_FLOW_METER_TICKS_PER_ML, Double.valueOf(meter.getTicksPerMl()));
    return values;
  }

  private static ContentValues toContentValues(final KegTap tap) {
    final ContentValues values = new ContentValues();
    if (tap.getId() != 0) {
      values.put(COLUMN_ID, Integer.valueOf(tap.getId()));
    }
    values.put(COLUMN_TAP_TAP_NAME, tap.getName());
    values.put(COLUMN_TAP_SORT_ORDER, Integer.valueOf(0));  // TODO
    values.put(COLUMN_TAP_CURRENT_KEG, Integer.valueOf(tap.getCurrentKegId()));
    if (tap.hasMeter() && tap.getMeter().getId() > 0) {
      values.put(COLUMN_TAP_FLOW_METER_ID, Integer.valueOf(tap.getMeter().getId()));
    } else {
      values.put(COLUMN_TAP_FLOW_METER_ID, Integer.valueOf(0));
    }
    return values;
  }

  private static String formatDatetime(String original) {
    return "";
  }

}
