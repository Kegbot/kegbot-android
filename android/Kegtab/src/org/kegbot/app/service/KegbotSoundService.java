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
package org.kegbot.app.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.app.KegtabBroadcast;
import org.kegbot.app.util.Downloader;
import org.kegbot.app.util.Units;
import org.kegbot.core.Flow;
import org.kegbot.core.Flow.State;
import org.kegbot.core.KegbotCore;
import org.kegbot.proto.Models.SoundEvent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class KegbotSoundService extends BackgroundService {

  private static final String TAG = KegbotSoundService.class.getSimpleName();

  private KegbotCore mCore;
  private KegbotApi mApi;

  private final LinkedBlockingQueue<Intent> mCommandQueue = Queues.newLinkedBlockingQueue();

  private final Set<SoundEvent> mSoundEvents = Sets.newLinkedHashSet();

  private final WeakHashMap<Flow, List<Double>> mThresholds = new WeakHashMap<Flow, List<Double>>();

  private final Random mRandom = new Random();

  private final Map<String, File> mFiles = Maps.newLinkedHashMap();

  private final ExecutorService mExecutor = Executors.newFixedThreadPool(3);

  private MediaPlayer mMediaPlayer;

  private boolean mQuit;

  private enum SoundEventHandler {
    POUR_START {
      @Override
      boolean match(SoundEvent event, String username, Flow flow) {
        if (!"pour_start".equals(event.getEventName())) {
          return false;
        }
        if (!Strings.isNullOrEmpty(username)) {
          if (event.hasUser() && !event.getUser().equals(username)) {
            return false;
          }
        }
        return true;
      }
    },
    POUR_UPDATE {
      @Override
      boolean match(SoundEvent event, String username, Flow flow) {
        if (!"flow.threshold.ounces".equals(event.getEventName())) {
          return false;
        }
        if (!event.hasEventPredicate()) {
          return false;
        }
        int ounces;
        try {
          ounces = Integer.parseInt(event.getEventPredicate());
        } catch (NumberFormatException e) {
          return false;
        }
        if (flow.getVolumeMl() >= Units.volumeOuncesToMl(ounces)) {
          return true;
        }
        return false;
      }
    },
    USER_AUTHED {
      @Override
      boolean match(SoundEvent event, String username, Flow flow) {
        if (!"user_authed".equals(event.getEventName())) {
          return false;
        }

        if (!event.hasUser()) {
          return true;
        }
        return event.getUser().equals(username);
      }
    };

    abstract boolean match(final SoundEvent event, final String username, final Flow flow);
  }

  private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      mCommandQueue.add(intent);
    }
  };

  private static final IntentFilter INTENT_FILTER = new IntentFilter();
  static {
    INTENT_FILTER.addAction(KegtabBroadcast.ACTION_USER_AUTHED);
    INTENT_FILTER.addAction(KegtabBroadcast.ACTION_POUR_START);
    INTENT_FILTER.addAction(KegtabBroadcast.ACTION_POUR_UPDATE);
    INTENT_FILTER.setPriority(1000);
  }

  public class LocalBinder extends Binder {
    KegbotSoundService getService() {
      return KegbotSoundService.this;
    }
  }

  private final IBinder mBinder = new LocalBinder();

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  @Override
  public void onCreate() {
    registerReceiver(mBroadcastReceiver, INTENT_FILTER);
    mCore = KegbotCore.getInstance(this);
    mApi = mCore.getApi();
    mMediaPlayer = new MediaPlayer();
    mQuit = false;
    super.onCreate();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    unregisterReceiver(mBroadcastReceiver);
    mQuit = true;
    mCommandQueue.clear();
    mMediaPlayer.release();
  }

  @Override
  protected void runInBackground() {
    Log.d(TAG, "Running in background.");
    try {
      final List<SoundEvent> allEvents = mApi.getAllSoundEvents();
      Log.d(TAG, "Sound events: " + allEvents);
      mSoundEvents.clear();
      mSoundEvents.addAll(allEvents);
      downloadAll();
    } catch (KegbotApiException e) {
      // Pass.
    }
    while (true) {
      synchronized (this) {
        if (mQuit) {
          break;
        }
      }
      Intent command;
      try {
        command = mCommandQueue.poll(200, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        continue;
      }
      if (command == null) {
        continue;
      }

      handleCommand(command);
    }
  }

  private void downloadAll() {
    for (SoundEvent e : mSoundEvents) {
      final String url = e.getSoundUrl();

      if (mFiles.containsKey(url)) {
        final File output = mFiles.get(url);
        if (output.exists()) {
          continue;
        } else {
          mFiles.remove(url);
        }
      }
      String[] parts = url.split("/");
      final String filename = parts[parts.length - 1];
      final File output = new File(getCacheDir(), filename);

      if (output.exists()) {
        Log.d(TAG, "File exists: " + url + " file=" + output) ;
        mFiles.put(url, output);
        continue;
      }

      final Runnable job = new Runnable() {
        @Override
        public void run() {
          Log.d(TAG, "Downloading: " + url);
          try {
            Downloader.downloadRaw(url, output);
          } catch (IOException exc) {
            Log.w(TAG, "Download failed: " + exc.toString(), exc);
            return;
          }
          Log.d(TAG, "Sucesss: " + url + " file=" + output);
          mFiles.put(url, output);
        }
      };
      mExecutor.submit(job);
    }
  }

  private void handleCommand(final Intent command) {
    final SoundEventHandler handler = getHandlerForEvent(command);
    Log.d(TAG, "Handling command: " + command + " mSoundEvents.size=" + mSoundEvents.size());

    if (handler == null) {
      Log.d(TAG, "No handler.");
      return;
    }

    final Flow flow = getFlowForPourIntent(command);
    String username = "";
    if (command.hasExtra(KegtabBroadcast.USER_AUTHED_EXTRA_USERNAME)) {
      username = command.getStringExtra(KegtabBroadcast.USER_AUTHED_EXTRA_USERNAME);
    } else if (flow != null && flow.isAuthenticated()) {
      username = flow.getUsername();
    }

    final List<SoundEvent> userEvents = Lists.newArrayList();
    final List<SoundEvent> allEvents = Lists.newArrayList();
    for (final SoundEvent e : mSoundEvents) {
      if (handler.match(e, username, flow)) {
        if (e.hasUser()) {
          userEvents.add(e);
        } else {
          allEvents.add(e);
        }
      }
    }

    Log.d(TAG, "Matched userEvents=" + userEvents.size());
    Log.d(TAG, "Matched allEvents=" + allEvents.size());

    if (!userEvents.isEmpty()) {
      playRandomSound(userEvents);
    } else if (!allEvents.isEmpty()) {
      playRandomSound(allEvents);
    } else {
      Log.d(TAG, "Nothing to do.");
    }

  }

  /**
   * @param allEvents
   */
  private void playRandomSound(List<SoundEvent> events) {
    if (events.isEmpty()) {
      Log.w(TAG, "Empty list.");
      return;
    }
    if (events.size() == 1) {
      playSound(events.get(0));
    } else {
      final SoundEvent event = events.get(mRandom.nextInt(events.size()));
      playSound(event);
    }
  }

  private void playSound(SoundEvent event) {
    final String soundUrl = event.getSoundUrl();
    Log.d(TAG, "Playing sound: " + soundUrl);

    final File soundFile = mFiles.get(soundUrl);
    mMediaPlayer.reset();
    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);

    FileInputStream fis;
    try {
      fis = new FileInputStream(soundFile);
    } catch (FileNotFoundException e) {
      Log.w(TAG, "Error loading file: " + e.toString(), e);
      return;
    }
    try {
      mMediaPlayer.setDataSource(fis.getFD());
      mMediaPlayer.prepare();
      mMediaPlayer.start();
    } catch (IllegalArgumentException e) {
      Log.w(TAG, "Error", e);
    } catch (IllegalStateException e) {
      Log.w(TAG, "Error", e);

    } catch (IOException e) {
      Log.w(TAG, "Error", e);

    } finally {
    }
  }

  private SoundEventHandler getHandlerForEvent(final Intent intent) {
    final String action = intent.getAction();
    final Flow flow = getFlowForPourIntent(intent);

    if (!mThresholds.containsKey(flow)) {
      mThresholds.put(flow, getThresholds());
    }

    if (KegtabBroadcast.ACTION_POUR_START.equals(action)) {
      return SoundEventHandler.POUR_START;
    } else if (KegtabBroadcast.ACTION_POUR_UPDATE.equals(action)) {
      if (flow == null) {
        return null;
      }
      if (flow.getState() == State.COMPLETED) {
        mThresholds.remove(flow);
        return null;
      }

      final List<Double> thresholds = mThresholds.get(flow);
      if (thresholds == null) {
        return null;
      }
      boolean match = false;
      while (!thresholds.isEmpty()) {
        if (thresholds.get(0).doubleValue() <= flow.getVolumeMl()) {
          match = true;
          thresholds.remove(0);
        } else {
          break;
        }
      }
      if (match) {
        return SoundEventHandler.POUR_UPDATE;
      }
    } else if (KegtabBroadcast.ACTION_USER_AUTHED.equals(action)) {
      return SoundEventHandler.USER_AUTHED;
    }
    return null;
  }

  private final List<Double> getThresholds() {
    final List<Double> result = Lists.newArrayList(
        Double.valueOf(Units.volumeOuncesToMl(6.0)),
        Double.valueOf(Units.volumeOuncesToMl(12.0)),
        Double.valueOf(Units.volumeOuncesToMl(16.0)),
        Double.valueOf(Units.volumeOuncesToMl(24.0)),
        Double.valueOf(Units.volumeOuncesToMl(32.0)),
        Double.valueOf(Units.volumeOuncesToMl(64.0))
        );
    return result;
  }

  private Flow getFlowForPourIntent(final Intent intent) {
    final String tapName = intent.getStringExtra(KegtabBroadcast.POUR_UPDATE_EXTRA_TAP_NAME);
    if (Strings.isNullOrEmpty(tapName)) {
      return null;
    }
    final Flow flow = mCore.getFlowManager().getFlowForMeterName(tapName);
    if (flow == null) {
      return null;
    }
    return flow;
  }

}
