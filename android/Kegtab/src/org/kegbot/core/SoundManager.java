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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.kegbot.app.event.Event;
import org.kegbot.app.event.FlowUpdateEvent;
import org.kegbot.app.event.SoundEventListUpdateEvent;
import org.kegbot.app.util.Downloader;
import org.kegbot.app.util.IndentingPrintWriter;
import org.kegbot.app.util.Units;
import org.kegbot.proto.Models.SoundEvent;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

/**
 * Plays sounds at specific events.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class SoundManager extends BackgroundManager {

  private static final String TAG = SoundManager.class.getSimpleName();
  private static final boolean DEBUG = true;

  private static final String EVENT_FLOW_THRESHOLD_OUNCES = "flow.threshold.ounces";

  /** Queue of Events, shipped from main thread to our background thread. */
  private final LinkedBlockingQueue<Event> mCommandQueue = Queues.newLinkedBlockingQueue();

  /** All sound events. */
  private final Set<SoundEvent> mSoundEvents = Sets.newLinkedHashSet();

  /** Map of flows to last observed volume. */
  private final Map<Flow, Double> mFlowsByLastVolume = Maps.newLinkedHashMap();

  /** Map of URLs to locally-cached files. */
  private final Map<String, File> mFiles = Maps.newLinkedHashMap();

  /** Executor for playing sounds. */
  private final ExecutorService mExecutor = Executors.newFixedThreadPool(3);

  /** Map of volume (mL) to sound event. */
  private final Map<Double, SoundEvent> mVolumeThresholds = Maps.newLinkedHashMap();

  private Context mContext;
  private MediaPlayer mMediaPlayer;

  private boolean mQuit;

  public SoundManager(Bus bus, Context context) {
    super(bus);
    mContext = context;
  }

  @Override
  public synchronized void start() {
    mMediaPlayer = new MediaPlayer();
    mQuit = false;
    getBus().register(this);
    super.start();
  }

  @Override
  public synchronized void stop() {
    mQuit = true;
    mCommandQueue.clear();
    mMediaPlayer.release();
    getBus().unregister(this);
    super.stop();
  }

  @Override
  protected void dump(IndentingPrintWriter writer) {
    writer.printPair("numFlows", Integer.valueOf(mFlowsByLastVolume.size()).toString()).println();

    if (!mFlowsByLastVolume.isEmpty()) {
      writer.println("Flows:");
      writer.increaseIndent();
      for (Map.Entry<Flow, Double> entry : mFlowsByLastVolume.entrySet()) {
        writer.printPair("flowId", Integer.valueOf(entry.getKey().getFlowId()).toString())
          .println();
        writer.printPair("lastVolumeMl", entry.getValue().toString())
          .println();
        writer.println();
      }
    }
  }

  @Override
  protected void runInBackground() {
    while (true) {
      synchronized (this) {
        if (mQuit) {
          break;
        }
      }

      Event event;
      try {
        event = mCommandQueue.poll(200, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        continue;
      }
      if (event != null) {
        if (DEBUG) {
          Log.d(TAG, "Handling event: " + event + " mSoundEvents.size=" + mSoundEvents.size());
        }
        if (event instanceof FlowUpdateEvent) {
          processFlowUpdate(((FlowUpdateEvent) event).getFlow());
        } else if (event instanceof SoundEventListUpdateEvent) {
          processSoundEventListUpdateEvent((SoundEventListUpdateEvent) event);
        } else {
          if (DEBUG) Log.w(TAG, "Unrecognized event type.");
        }
      }
    }
  }

  /** Background callback, handles a flow update. */
  private void processFlowUpdate(Flow flow) {
    if (flow.isFinished()) {
      mFlowsByLastVolume.remove(flow);
      return;
    }

    final Double currentVolume = Double.valueOf(flow.getVolumeMl());
    Double lastVolume = mFlowsByLastVolume.get(flow);
    if (lastVolume == null) {
      lastVolume = Double.valueOf(0);
    }
    mFlowsByLastVolume.put(flow, currentVolume);

    if (currentVolume == lastVolume) {
      return;
    }

    // Find the next volume threshold, based on lastVolume.
    List<Double> thresholds = Lists.newArrayList(mVolumeThresholds.keySet());
    Collections.sort(thresholds);
    Double threshold = null;

    for (Double v : thresholds) {
      if (v.doubleValue() > lastVolume.doubleValue()) {
        threshold = v;
        break;
      }
    }

    if (threshold == null) {
      if (DEBUG) Log.d(TAG, "No threshold.");
      return;
    }

    if (currentVolume.doubleValue() >= threshold.doubleValue()) {
      Log.d(TAG, "Tripped threshold: " + threshold);
      SoundEvent e = mVolumeThresholds.get(threshold);
      if (e == null) {
        Log.e(TAG, "No event.");
        return;
      }
      playSound(e);
    }
  }

  /** Background callback, handles an updated sound event list. */
  private void processSoundEventListUpdateEvent(SoundEventListUpdateEvent event) {
    final Set<SoundEvent> newEvents = Sets.newHashSet(event.getEvents());
    Log.d(TAG, "Updated sound events: " + Joiner.on(", ").join(newEvents));
    if (!newEvents.equals(mSoundEvents)) {
      mSoundEvents.clear();
      Log.d(TAG, "New/updated sound events: ");
      for (SoundEvent e : newEvents) {
        Log.d(TAG, "Event: " + e);
        downloadEvent(e);
        mSoundEvents.add(e);
      }
    }
    recomputeVolumeThresholds();
  }

  /** Downloads a single file. */
  private void downloadEvent(SoundEvent e) {
    final String url = e.getSoundUrl();

    if (mFiles.containsKey(url)) {
      final File output = mFiles.get(url);
      if (output.exists()) {
        return;
      } else {
        mFiles.remove(url);
      }
    }
    String[] parts = url.split("/");
    final String filename = parts[parts.length - 1];
    final File output = new File(mContext.getCacheDir(), filename);

    if (output.exists()) {
      Log.d(TAG, "File exists: " + url + " file=" + output) ;
      mFiles.put(url, output);
      return;
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

  /** Plays a single, previously-downloaded sound. */
  private void playSound(SoundEvent event) {
    final String soundUrl = event.getSoundUrl();
    Log.d(TAG, "Playing sound: " + soundUrl);

    final File soundFile = mFiles.get(soundUrl);
    mMediaPlayer.reset();
    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);

    FileInputStream fis;
    try {
      fis = new FileInputStream(soundFile);
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
        try {
          fis.close();
        } catch (IOException e) {
          // Close quietly.
        }
      }
    } catch (FileNotFoundException e) {
      Log.w(TAG, "Error loading file: " + e.toString(), e);
      return;
    }

  }

  @Subscribe
  public void onFlowUpdateEvent(FlowUpdateEvent event) {
    mCommandQueue.add(event);
  }

  @Subscribe
  public void onSoundEventListUpdateEvent(SoundEventListUpdateEvent event) {
    mCommandQueue.add(event);
  }

  /** Recomputes {@link #mVolumeThresholds} based on {@link #mSoundEvents}. */
  private void recomputeVolumeThresholds() {
    final Map<Double, SoundEvent> result = Maps.newLinkedHashMap();

    for (final SoundEvent event : mSoundEvents) {
      if (!EVENT_FLOW_THRESHOLD_OUNCES.equals(event.getEventName())) {
        continue;
      }
      try {
        final Double ounces = Double.valueOf(event.getEventPredicate());
        final Double ml = Double.valueOf(Units.volumeOuncesToMl(ounces.doubleValue()));
        result.put(ml, event);
      } catch (NumberFormatException e) {
        // Ignore
      }
    }

    if (!result.equals(mVolumeThresholds)) {
      mVolumeThresholds.clear();
      mVolumeThresholds.putAll(result);
      Log.d(TAG, "Updated volume thresholds: " + Joiner.on(", ").join(result.keySet()));
    }
  }

}
