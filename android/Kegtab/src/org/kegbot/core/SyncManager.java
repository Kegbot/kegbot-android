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
package org.kegbot.core;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiNotFoundError;
import org.kegbot.app.event.CurrentSessionChangedEvent;
import org.kegbot.app.event.DrinkPostedEvent;
import org.kegbot.app.event.SystemEventListUpdateEvent;
import org.kegbot.app.event.TapListUpdateEvent;
import org.kegbot.app.storage.LocalDbHelper;
import org.kegbot.proto.Api.RecordDrinkRequest;
import org.kegbot.proto.Api.RecordTemperatureRequest;
import org.kegbot.proto.Internal.PendingPour;
import org.kegbot.proto.Models.Drink;
import org.kegbot.proto.Models.KegTap;
import org.kegbot.proto.Models.Session;
import org.kegbot.proto.Models.Stats;
import org.kegbot.proto.Models.SystemEvent;
import org.kegbot.proto.Models.ThermoLog;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.SystemClock;
import android.util.Log;

import com.google.common.collect.Lists;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;

/**
 * This service manages a connection to a Kegbot backend, using the Kegbot API.
 * It implements the {@link KegbotApi} interface, potentially employing caching.
 */
public class SyncManager extends BackgroundManager {

  private static String TAG = SyncManager.class.getSimpleName();

  /**
   * All pending asynchronous requests. They will be serviced in order in the
   * background thread.
   *
   * TODO(mikey): fix capacity appropriately.
   */
  private final BlockingQueue<AbstractMessage> mPendingRequests =
    new LinkedBlockingQueue<AbstractMessage>(10);

  private final KegbotApi mApi;
  private final Context mContext;

  private List<KegTap> mLastKegTapList = Lists.newArrayList();
  private List<SystemEvent> mLastSystemEventList = Lists.newArrayList();
  @Nullable private Session mLastSession = null;
  @Nullable private Stats mLastSessionStats = null;

  private SQLiteOpenHelper mLocalDbHelper;

  private boolean mRunning = true;
  private boolean mSyncImmediate = true;

  private long mNextSyncTime = Long.MIN_VALUE;

  private static final long SYNC_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(1);
  private static final long SYNC_INTERVAL_AGGRESSIVE_MILLIS = TimeUnit.SECONDS.toMillis(10);

  public static Comparator<SystemEvent> EVENTS_DESCENDING = new Comparator<SystemEvent>() {
    @Override
    public int compare(SystemEvent object1, SystemEvent object2) {
      try {
        final long time1 = org.kegbot.app.util.DateUtils.dateFromIso8601String(object1.getTime());
        final long time2 = org.kegbot.app.util.DateUtils.dateFromIso8601String(object2.getTime());
        return Long.valueOf(time2).compareTo(Long.valueOf(time1));
      } catch (IllegalArgumentException e) {
        Log.wtf(TAG, "Error parsing times", e);
        return 0;
      }
    }
  };


  public SyncManager(Bus bus, Context context, KegbotApi api) {
    super(bus);
    mApi = api;
    mContext = context;
  }

  @Override
  public synchronized void start() {
    Log.d(TAG, "Opening local database");
    mRunning = true;
    mSyncImmediate = true;
    mNextSyncTime = Long.MIN_VALUE;
    mLocalDbHelper = new LocalDbHelper(mContext);
    mRunning = true;
    getBus().register(this);
    super.start();
  }

  @Override
  public synchronized void stop() {
    mRunning = false;
    getBus().unregister(this);
    mLastKegTapList.clear();
    super.stop();
  }

  /**
   * Schedules a drink to be recorded asynchronously.
   * @param flow
   */
  public void recordDrinkAsync(final Flow flow) {
    final RecordDrinkRequest request = getRequestForFlow(flow);
    final PendingPour pour = PendingPour.newBuilder()
        .setDrinkRequest(request)
        .addAllImages(flow.getImages())
        .build();

    Log.d(TAG, ">>> Enqueuing pour: " + pour);
    if (mPendingRequests.remainingCapacity() == 0) {
      // Drop head when full.
      mPendingRequests.poll();
    }
    mPendingRequests.add(pour);
    Log.d(TAG, "<<< Pour enqueued.");
  }

  /**
   * Schedules a temperature reading to be recorded asynchronously.
   *
   * @param request
   */
  public void recordTemperatureAsync(final RecordTemperatureRequest request) {
    Log.d(TAG, "Recording temperature: " + request);
    if (mPendingRequests.remainingCapacity() == 0) {
      // Drop head when full.
      mPendingRequests.poll();
    }
    mPendingRequests.add(request);
  }

  @Override
  protected void runInBackground() {
    Log.i(TAG, "Running in background.");

    try {
      while (true) {
        synchronized (this) {
          if (!mRunning) {
            Log.d(TAG, "No longer running, exiting.");
            break;
          }
        }

        writeNewRequestsToDb();
        postPendingRequestsToServer();

        long now = SystemClock.elapsedRealtime();
        if (mSyncImmediate == true || now > mNextSyncTime) {
          Log.d(TAG, "Syncing: syncImmediate=" + mSyncImmediate + " mNextSyncTime=" + mNextSyncTime);
          mSyncImmediate = false;

          boolean syncError = true;
          try {
            syncError = syncNow();
          } finally {
            mNextSyncTime = SystemClock.elapsedRealtime() +
                (syncError ? SYNC_INTERVAL_AGGRESSIVE_MILLIS : SYNC_INTERVAL_MILLIS);
          }

        }
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          Log.d(TAG, "Interrupted.");
          Thread.currentThread().interrupt();
          break;
        }
      }
    } catch (Throwable e) {
      Log.wtf(TAG, "Uncaught exception in background.", e);
    }
  }

  private void postPendingRequestsToServer() {
    final int numPending = numPendingEntries();
    if (numPending > 0) {
      Log.d(TAG, "Posting pending requests: " + numPending);
      processRequestFromDb();
    }
  }

  private void processRequestFromDb() {
    final SQLiteDatabase db = mLocalDbHelper.getWritableDatabase();

    // Fetch most recent entry.
    final Cursor cursor =
      db.query(LocalDbHelper.TABLE_NAME,
          null, null, null, null, null, LocalDbHelper.COLUMN_NAME_ADDED_DATE + " ASC", "1");
    try {
      if (cursor.getCount() == 0) {
        //Log.i(TAG, "processRequestFromDb: empty result set, exiting");
        return;
      }
      cursor.moveToFirst();

      boolean processed = false;
      try {
        final AbstractMessage record = LocalDbHelper.getCurrentRow(db, cursor);
        if (record instanceof PendingPour) {
          final PendingPour pour = (PendingPour) record;
          final RecordDrinkRequest request = pour.getDrinkRequest();

          Log.d(TAG, "Posting pour");
          final Drink drink = mApi.recordDrink(request);
          Log.d(TAG, "Drink posted: " + drink);
          postOnMainThread(new DrinkPostedEvent(drink));

          if (pour.getImagesCount() > 0) {
            Log.d(TAG, "Drink had images, trying to post them..");
            for (final String imagePath : pour.getImagesList()) {
              try {
                if (drink != null) {
                  Log.d(TAG, "Uploading image: " + imagePath);
                  mApi.uploadDrinkImage(drink.getId(), imagePath);
                }
              } finally {
                new File(imagePath).delete();
                Log.d(TAG, "Deleted " + imagePath);
              }
            }
          }
          processed = true;

          // Sync taps, etc, on new drink.
          mSyncImmediate = true;
        } else if (record instanceof RecordTemperatureRequest) {
          processed = true; // XXX drop even if fail
          Log.d(TAG, "Posting thermo");
          final ThermoLog log = mApi.recordTemperature((RecordTemperatureRequest) record);
          Log.d(TAG, "ThermoLog posted: " + log);
        } else {
          Log.w(TAG, "Unknown row type.");
        }

      } catch (InvalidProtocolBufferException e) {
        Log.w(TAG, "Error processing column: " + e);
        processed = true;
      } catch (KegbotApiNotFoundError e) {
        Log.w(TAG, "Tap not found, dropping record");
        processed = true;
      } catch (KegbotApiException e) {
        Log.w(TAG, "Error processing column: " + e);
        processed = true;
      }

      if (processed) {
        final int deleteResult = LocalDbHelper.deleteCurrentRow(db, cursor);
        Log.d(TAG, "Deleted row, result = " + deleteResult);
      }
    } finally {
      cursor.close();
      db.close();
    }
  }

  private int writeNewRequestsToDb() {
    AbstractMessage message;
    int result = 0;
    while ((message = mPendingRequests.poll()) != null) {
      if (addSingleRequestToDb(message)) {
        result++;
      }
    }
    return result;
  }

  private boolean addSingleRequestToDb(AbstractMessage message) {
    Log.d(TAG, "Adding request to db!");
    final String type;
    if (message instanceof PendingPour) {
      type = "pour";
    } else if (message instanceof RecordTemperatureRequest) {
      type = "thermo";
    } else {
      Log.w(TAG, "Unknown record type; dropping.");
      return false;
    }
    Log.d(TAG, "Request is a " + type);

    final ContentValues values = new ContentValues();
    values.put(LocalDbHelper.COLUMN_NAME_TYPE, type);
    values.put(LocalDbHelper.COLUMN_NAME_RECORD, message.toByteArray());

    boolean inserted = false;
    final SQLiteDatabase db = mLocalDbHelper.getWritableDatabase();
    try {
      db.insert(LocalDbHelper.TABLE_NAME, null, values);
      inserted = true;
    } finally {
      db.close();
    }
    return inserted;
  }

  private boolean syncNow() {
    boolean error = false;
    // Taps.
    try {
      List<KegTap> newTaps = mApi.getAllTaps();
      if (!newTaps.equals(mLastKegTapList)) {
        mLastKegTapList = newTaps;
        postOnMainThread(new TapListUpdateEvent(newTaps));
      }
    } catch (KegbotApiException e) {
      Log.w(TAG, "Error syncing taps: " + e);
      error = true;
    }

    // System events.
    SystemEvent lastEvent = null;
    if (!mLastSystemEventList.isEmpty()) {
      lastEvent = mLastSystemEventList.get(0);
    }

    try {
      List<SystemEvent> newEvents;
      if (lastEvent != null) {
        newEvents = mApi.getRecentEvents(lastEvent.getId());
      } else {
        newEvents = mApi.getRecentEvents();
      }
      Collections.sort(newEvents, EVENTS_DESCENDING);

      if (!newEvents.isEmpty()) {
        mLastSystemEventList.clear();
        mLastSystemEventList.addAll(newEvents);
        postOnMainThread(new SystemEventListUpdateEvent(mLastSystemEventList));
      }
    } catch (KegbotApiException e) {
      Log.w(TAG, "Error syncing events: " + e);
      error = true;
    }

    // Current session
    try {
      Session currentSession = mApi.getCurrentSession();
      if ((currentSession == null && mLastSession != null) ||
          (mLastSession == null && currentSession != null) ||
          (currentSession != null && !currentSession.equals(mLastSession))) {
        Stats stats = null;
        if (currentSession != null) {
          stats = mApi.getSessionStats(currentSession.getId());
        }
        mLastSession = currentSession;
        mLastSessionStats = stats;
        postOnMainThread(new CurrentSessionChangedEvent(currentSession, stats));
      }
    } catch (KegbotApiException e) {
      Log.w(TAG, "Error syncing current session: " + e);
      error = true;
    }

    return error;
  }

  @Produce
  public TapListUpdateEvent produceTapList() {
    return new TapListUpdateEvent(Lists.newArrayList(mLastKegTapList));
  }

  @Produce
  public SystemEventListUpdateEvent produceSystemEvents() {
    return new SystemEventListUpdateEvent(Lists.newArrayList(mLastSystemEventList));
  }

  @Produce
  public CurrentSessionChangedEvent produceCurrentSession() {
    return new CurrentSessionChangedEvent(mLastSession, mLastSessionStats);
  }

  private int numPendingEntries() {
    final String[] columns = {LocalDbHelper.COLUMN_NAME_ID};
    final SQLiteDatabase db = mLocalDbHelper.getReadableDatabase();
    final Cursor cursor =
      db.query(LocalDbHelper.TABLE_NAME, columns, null, null, null, null, null);
    try {
      return cursor.getCount();
    } finally {
      cursor.close();
      db.close();
    }
  }

  public void requestSync() {
    Log.d(TAG, "Immediate sync requested.");
    mSyncImmediate = true;
  }

  private static RecordDrinkRequest getRequestForFlow(final Flow ended) {
    return RecordDrinkRequest.newBuilder()
        .setTapName(ended.getTap().getMeterName())
        .setTicks(ended.getTicks())
        .setVolumeMl((float) ended.getVolumeMl())
        .setUsername(ended.getUsername())
        .setSecondsAgo(0)
        .setDurationSeconds((int) (ended.getDurationMs() / 1000.0))
        .setSpilled(false)
        .setShout(ended.getShout())
        .setTickTimeSeries(ended.getTickTimeSeries().asString())
        .buildPartial();
  }

}
