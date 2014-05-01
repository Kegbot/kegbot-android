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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import org.codehaus.jackson.JsonNode;
import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.app.BuildConfig;
import org.kegbot.app.event.ConnectivityChangedEvent;
import org.kegbot.app.event.ControllerListUpdateEvent;
import org.kegbot.app.event.CurrentSessionChangedEvent;
import org.kegbot.app.event.DrinkPostedEvent;
import org.kegbot.app.event.FlowMeterListUpdateEvent;
import org.kegbot.app.event.FlowToggleListUpdateEvent;
import org.kegbot.app.event.SoundEventListUpdateEvent;
import org.kegbot.app.event.SystemEventListUpdateEvent;
import org.kegbot.app.event.TapListUpdateEvent;
import org.kegbot.app.storage.LocalDbHelper;
import org.kegbot.app.util.TimeSeries;
import org.kegbot.backend.Backend;
import org.kegbot.backend.BackendException;
import org.kegbot.backend.NotFoundException;
import org.kegbot.proto.Api.RecordDrinkRequest;
import org.kegbot.proto.Api.RecordTemperatureRequest;
import org.kegbot.proto.Api.SyncResponse;
import org.kegbot.proto.Internal.PendingPour;
import org.kegbot.proto.Models;
import org.kegbot.proto.Models.Controller;
import org.kegbot.proto.Models.Drink;
import org.kegbot.proto.Models.FlowMeter;
import org.kegbot.proto.Models.KegTap;
import org.kegbot.proto.Models.Session;
import org.kegbot.proto.Models.SoundEvent;
import org.kegbot.proto.Models.SystemEvent;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

/**
 * Performs asynchronous work against a {@link Backend} and provides
 * non-blocking cached data from it.
 */
public class SyncManager extends BackgroundManager {

  private static String TAG = SyncManager.class.getSimpleName();
  private static final boolean DEBUG = BuildConfig.DEBUG;

  private final Backend mBackend;
  private final Context mContext;

  @Nullable private SyncResponse mLastSync;
  private List<KegTap> mLastKegTapList = Lists.newArrayList();
  private List<SystemEvent> mLastSystemEventList = Lists.newArrayList();
  private List<SoundEvent> mLastSoundEventList = Lists.newArrayList();
  private List<Controller> mLastControllers = Lists.newArrayList();
  private List<FlowMeter> mLastFlowMeters = Lists.newArrayList();
  private List<Models.FlowToggle> mLastFlowToggles = Lists.newArrayList();
  @Nullable private Session mLastSession = null;
  @Nullable private JsonNode mLastSessionStats = null;

  private SQLiteOpenHelper mLocalDbHelper;

  private boolean mRunning = true;
  private boolean mSyncImmediate = true;

  private long mNextSyncTime = Long.MIN_VALUE;

  private static final long SYNC_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(1);
  private static final long SYNC_INTERVAL_AGGRESSIVE_MILLIS = TimeUnit.SECONDS.toMillis(10);

  private ExecutorService mBackendExecutorService;

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

  public static Comparator<SoundEvent> SOUND_EVENT_COMPARATOR = new Comparator<SoundEvent>() {
    @Override
    public int compare(SoundEvent object1, SoundEvent object2) {
      int cmp = object1.getEventName().compareTo(object2.getEventName());
      if (cmp == 0) {
        cmp = object1.getEventPredicate().compareTo(object2.getEventPredicate());
      }
      return cmp;
    }
  };

  public SyncManager(Bus bus, Context context, Backend api) {
    super(bus);
    mBackend = api;
    mContext = context;
  }

  @Override
  public synchronized void start() {
    Log.d(TAG, "Opening local database");
    mRunning = true;
    mSyncImmediate = true;
    mNextSyncTime = Long.MIN_VALUE;
    mBackendExecutorService = Executors.newSingleThreadExecutor();
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
    mBackendExecutorService.shutdown();
    super.stop();
  }

  /** Schedules a drink to be recorded asynchronously. */
  public synchronized void recordDrinkAsync(final Flow flow) {
    if (!mRunning) {
      Log.e(TAG, "Record drink request while not running.");
      return;
    }
    final RecordDrinkRequest request = getRequestForFlow(flow);
    final PendingPour pour = PendingPour.newBuilder()
        .setDrinkRequest(request)
        .addAllImages(flow.getImages())
        .build();

    postDeferredPoursAsync();
    mBackendExecutorService.submit(new Runnable() {
      @Override
      public void run() {
        try {
          postPour(pour);
        } catch (KegbotApiException e) {
          Log.d(TAG, "Caught exception posting pour: " + e);
          deferPostPour(pour);
        } catch (Exception e) {
          Log.w(TAG, "Error posting pour: " + e, e);
        }
      }
    });
  }

  private void postDeferredPoursAsync() {
    mBackendExecutorService.submit(new Runnable() {
      @Override
      public void run() {
        postDeferredPours();
      }
    });
  }

  /**
   * Schedules a temperature reading to be recorded asynchronously.
   *
   * @param request
   */
  public synchronized void recordTemperatureAsync(final RecordTemperatureRequest request) {
    if (!mRunning) {
      Log.e(TAG, "Record thermo request while not running.");
      return;
    }
    mBackendExecutorService.submit(new Runnable() {
      @Override
      public void run() {
        try {
          postThermoLog(request);
        } catch (BackendException e) {
          // Don't both retrying.
          Log.w(TAG, String.format("Error posting thermo, dropping: %s", e));
        }
      }
    });
  }

  public synchronized void requestSync() {
    Log.d(TAG, "Immediate sync requested.");
    mSyncImmediate = true;
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

  @Produce
  public SoundEventListUpdateEvent produceSoundEvents() {
    return new SoundEventListUpdateEvent(mLastSoundEventList);
  }

  public List<Controller> getCurrentControllers() {
    return ImmutableList.copyOf(mLastControllers);
  }

  public List<FlowMeter> getCurrentFlowMeters() {
    return ImmutableList.copyOf(mLastFlowMeters);
  }

  public List<Models.FlowToggle> getCurrentFlowToggles() {
    return ImmutableList.copyOf(mLastFlowToggles);
  }

  @Subscribe
  public void handleConnectivityChangedEvent(ConnectivityChangedEvent event) {
    if (event.isConnected()) {
      Log.d(TAG, "Connection is up, requesting sync.");
      requestSync();
    } else {
      Log.d(TAG, "Connection is down.");
    }
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

  private void deferPostPour(PendingPour pour) {
    Log.d(TAG, "Deferring pour: " + pour);
    addSingleRequestToDb(pour);
  }

  /**
   * Synchronously posts a single pour to the remote backend. This method is
   * guaranteed to have succeeded on non-exceptional return.
   */
  private void postPour(final PendingPour pour) throws KegbotApiException {
    final RecordDrinkRequest request = pour.getDrinkRequest();
    Log.d(TAG, ">>> Posting pour: tap=" + request.getTapName() + " ticks=" + request.getTicks());

    if (!isConnected()) {
      throw new KegbotApiException("Not connected.");
    }

    final Drink drink;

    File picture = null;
    if (pour.getImagesCount() > 0) {
      // TODO(mikey): Single image everywhere.
      picture = new File(pour.getImagesList().get(0));
      if (!picture.exists()) {
        picture = null;
      }
    }

    try {
      TimeSeries ts = null;
      if (request.hasTickTimeSeries()) {
        ts = TimeSeries.fromString(request.getTickTimeSeries());
      }
      drink = mBackend.recordDrink(request.getTapName(), (long) request.getVolumeMl(),
          request.getTicks(), request.getShout(), request.getUsername(), request.getRecordDate(),
          request.getDurationSeconds() * 1000L, ts, picture);
    } catch (NotFoundException e) {
      Log.w(TAG, "Tap does not exist, dropping pour.");
      return;
    } catch (BackendException e) {
      // TODO: Handle error.
      Log.w(TAG, "Other error.");
      return;
    } finally {
      for (final String image : pour.getImagesList()) {
        if (new File(image).delete()) {
          Log.d(TAG, "Deleted " + image);
        }
      }
    }

    Log.d(TAG, "<<< Success, drink posted: " + drink);
    postOnMainThread(new DrinkPostedEvent(drink));
    requestSync();
  }

  /**
   * Synchronously posts a single thermo log to the remote backend. This method
   * is guaranteed to have succeeded on non-exceptional return.
   */
  private void postThermoLog(final RecordTemperatureRequest request) throws BackendException {
    Log.d(TAG, ">>> Posting thermo log: tap=" + request.getSensorName() + " value=" + request.getTempC());
    if (!isConnected()) {
      throw new KegbotApiException("Not connected.");
    }
    mBackend.recordTemperature(request);
    Log.d(TAG, "<<< Success.");
  }

  /** Posts any queued requests to the api service. */
  private void postDeferredPours() {
    final SQLiteDatabase db = mLocalDbHelper.getWritableDatabase();

    // Fetch most recent entry.
    final Cursor cursor =
      db.query(LocalDbHelper.TABLE_NAME,
          null, null, null, null, null, LocalDbHelper.COLUMN_NAME_ADDED_DATE + " ASC", "1");
    try {
      final int numPending = cursor.getCount();
      if (numPending == 0) {
        return;
      }

      Log.d(TAG, String.format("Processing %s deferred pour%s.",
          Integer.valueOf(numPending), numPending == 1 ? "" : "s"));
      cursor.moveToFirst();

      boolean deleteRow = true;
      try {
        final AbstractMessage record = LocalDbHelper.getCurrentRow(db, cursor);
        if (record instanceof PendingPour) {
          try {
            postPour((PendingPour) record);
          } catch (KegbotApiException e) {
            // Try later.
            deleteRow = false;
          }
          // Sync taps, etc, on new drink.
          mSyncImmediate = true;
        }
      } catch (InvalidProtocolBufferException e) {
        Log.w(TAG, "Error processing column: " + e);
      }

      if (deleteRow) {
        final int deleteResult = LocalDbHelper.deleteCurrentRow(db, cursor);
        Log.d(TAG, "Deleted row, result = " + deleteResult);
      }
    } finally {
      cursor.close();
      db.close();
    }
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

  private boolean isConnected() {
    final ConnectivityManager cm =
        (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    final NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    if (activeNetwork == null || !activeNetwork.isConnected()) {
      return false;
    }
    return true;
  }

  private boolean syncNow() {
    boolean error = false;

    if (mBackend instanceof KegbotApiImpl) {
      if (!isConnected()) {
        error = true;
        Log.d(TAG, "Network not connected.");
        return error;
      }
    }

    postDeferredPoursAsync();

    // Taps.
    try {
      List<KegTap> newTaps = mBackend.getTaps();
      if (!newTaps.equals(mLastKegTapList)) {
        if (DEBUG) {
          Log.d(TAG, "Updated taps:");
          for (final KegTap tap : newTaps) {
            Log.d(TAG, "TAP: " + tap);
            Log.d(TAG, "################");
          }
        }
        mLastKegTapList = newTaps;
        postOnMainThread(new TapListUpdateEvent(newTaps));
      }
    } catch (BackendException e) {
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
        newEvents = mBackend.getEventsSince(lastEvent.getId());
      } else {
        newEvents = mBackend.getEvents();
      }
      Collections.sort(newEvents, EVENTS_DESCENDING);

      if (!newEvents.isEmpty()) {
        mLastSystemEventList.clear();
        mLastSystemEventList.addAll(newEvents);
        postOnMainThread(new SystemEventListUpdateEvent(mLastSystemEventList));
      }
    } catch (BackendException e) {
      Log.w(TAG, "Error syncing events: " + e);
      error = true;
    }

    // Current session
    try {
      Session currentSession = mBackend.getCurrentSession();
      if ((currentSession == null && mLastSession != null) ||
          (mLastSession == null && currentSession != null) ||
          (currentSession != null && !currentSession.equals(mLastSession))) {
        JsonNode stats = null;
        if (currentSession != null) {
          stats = mBackend.getSessionStats(currentSession.getId());
        }
        mLastSession = currentSession;
        mLastSessionStats = stats;
        postOnMainThread(new CurrentSessionChangedEvent(currentSession, stats));
      }
    } catch (BackendException e) {
      Log.w(TAG, "Error syncing current session: " + e);
      error = true;
    }

    // Sound events
    try {
      List<SoundEvent> events = mBackend.getSoundEvents();
      Collections.sort(events, SOUND_EVENT_COMPARATOR);
      if (!events.equals(mLastSoundEventList)) {
        mLastSoundEventList.clear();
        mLastSoundEventList.addAll(events);
        postOnMainThread(new SoundEventListUpdateEvent(mLastSoundEventList));
      }
    } catch (BackendException e) {
      Log.w(TAG, "Error syncing sound events: " + e);
      error = true;
    }

    // Controllers
    try {
      List<Controller> controllers = mBackend.getControllers();
      if (!controllers.equals(mLastControllers)) {
        mLastControllers.clear();
        mLastControllers.addAll(controllers);
        postOnMainThread(new ControllerListUpdateEvent(mLastControllers));
      }
    } catch (BackendException e) {
      Log.w(TAG, "Error syncing controllers: " + e);
      error = true;
    }

    // Flow Meters
    try {
      List<FlowMeter> meters = mBackend.getFlowMeters();
      if (!meters.equals(mLastFlowMeters)) {
        mLastFlowMeters.clear();
        mLastFlowMeters.addAll(meters);
        postOnMainThread(new FlowMeterListUpdateEvent(mLastFlowMeters));
      }
    } catch (BackendException e) {
      Log.w(TAG, "Error syncing flow meters: " + e);
      error = true;
    }

    // Flow Toggles
    try {
      List<Models.FlowToggle> toggles = mBackend.getFlowToggles();
      if (!toggles.equals(mLastFlowToggles)) {
        mLastFlowToggles.clear();
        mLastFlowToggles.addAll(toggles);
        postOnMainThread(new FlowToggleListUpdateEvent(mLastFlowToggles));
      }
    } catch (BackendException e) {
      Log.w(TAG, "Error syncing flow toggles: " + e);
      error = true;
    }

    return error;
  }

  private static RecordDrinkRequest getRequestForFlow(final Flow ended) {
    return RecordDrinkRequest.newBuilder()
        .setTapName(ended.getTap().getMeter().getName())
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
