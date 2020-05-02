/*
 * Copyright 2003-2020 The Kegbot Project contributors <info@kegbot.org>
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

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.squareup.otto.Bus;

import org.kegbot.app.alert.AlertCore.Alert;
import org.kegbot.app.event.AlertEvent;
import org.kegbot.app.util.IndentingPrintWriter;

/**
 * Base class for Kegbot core components.
 */
public abstract class Manager {

  private final String mName;
  private final Bus mBus;
  private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

  public Manager(Bus bus) {
    mBus = bus;
    mName = getClass().getSimpleName();
    Log.d(mName, "Manager started: " + mName);
  }

  public String getName() {
    return mName;
  }

  /**
   * Called when the Kegbot Core is starting. The manager should initialize any resources it needs
   * during normal operation. The default implementation is a no-op.
   */
  protected void start() {

  }

  /**
   * Called when the Kegbot Core is stopping. The manager should release any resources it is
   * holding. The default implementation is a no-op.
   */
  protected void stop() {

  }

  /**
   * Called when the Kegbot Core requests the manager to reload any state. This should be logically
   * equivalent to <code>stop(); start();</code> (which is the default implementation).
   */
  protected void reload() {
    Log.d(mName, "Reloading (default implementation).");
    stop();
    start();
  }

  protected Bus getBus() {
    return mBus;
  }

  protected void postAlert(Alert alert) {
    postOnMainThread(new AlertEvent(alert));
  }

  protected void postOnMainThread(final Object event) {
    if (Looper.getMainLooper() == Looper.myLooper()) {
      // Called from the main thread.
      getBus().post(event);
    } else {
      // Called from some other thread, enqueue on Handler.
      mMainThreadHandler.post(new Runnable() {
        @Override
        public void run() {
          getBus().post(event);
        }
      });
    }
  }

  /**
   * Writes manager-specific debug information.
   */
  protected void dump(IndentingPrintWriter writer) {

  }

}
