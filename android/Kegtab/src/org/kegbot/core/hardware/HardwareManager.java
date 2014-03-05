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

package org.kegbot.core.hardware;

import android.content.Context;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.squareup.otto.Bus;

import org.kegbot.app.alert.AlertCore;
import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.event.Event;
import org.kegbot.app.util.IndentingPrintWriter;
import org.kegbot.backend.Backend;
import org.kegbot.core.Manager;
import org.kegbot.proto.Models.KegTap;

import java.util.Set;

/**
 * The HardwareManager provides implementation-independent access to sensors to
 * the rest of the Kegbot core. It is also responsible for admitting and
 * removing controllers based on internal policy.
 */
public class HardwareManager extends Manager {

  private static String TAG = HardwareManager.class.getSimpleName();

  private final Set<Controller> mControllers = Sets.newLinkedHashSet();

  private final KegboardManager mKegboardManager;

  private final Backend mBackend;

  /**
   * Callback interface implemented by hardware-specific controllers, such as
   * {@link KegboardManager}.
   */
  interface Listener {
    /** Issued when a new controller has been attached. */
    public void onControllerAttached(Controller controller);

    /** Issued when a controller has an event. */
    public void onControllerEvent(Controller controller, Event event);

    /** Issued when a controller has been removed. */
    public void onControllerRemoved(Controller controller);
  }

  public HardwareManager(Bus bus, Context context, AppConfiguration config, Backend backend) {
    super(bus);
    mBackend = backend;

    mKegboardManager = new KegboardManager(getBus(), context, new Listener() {
      @Override
      public void onControllerAttached(Controller controller) {
        HardwareManager.this.onControllerAttached(controller);
      }

      @Override
      public void onControllerEvent(Controller controller, Event event) {
        HardwareManager.this.onControllerEvent(controller, event);
      }

      @Override
      public void onControllerRemoved(Controller controller) {
        HardwareManager.this.onControllerRemoved(controller);
      }
    });
  }

  @Override
  public void start() {
    mKegboardManager.start();
    getBus().register(this);
  }

  public void refreshSoon() {
    mKegboardManager.refreshSoon();
  }

  private synchronized void onControllerAttached(Controller controller) {
    Log.d(TAG, "Controller attached: " + controller);
    if (mControllers.contains(controller)) {
      Log.w(TAG, "Controller already attached!");
      return;
    }
    mControllers.add(controller);
    postOnMainThread(new ControllerAttachedEvent(controller));

    postAlert(AlertCore.newBuilder("Controller Attached")
        .setId(controller.getName())
        .build());
  }

  private synchronized void onControllerEvent(Controller controller, Event event) {
    if (!mControllers.contains(controller)) {
      Log.w(TAG, "Received event from unknown controller: " + controller);
      return;
    }
    postOnMainThread(event);
  }

  private synchronized void onControllerRemoved(Controller controller) {
    if (!mControllers.contains(controller)) {
      Log.w(TAG, "Unknown controller was detached: " + controller);
      return;
    }
    mControllers.remove(controller);
    postOnMainThread(new ControllerDetachedEvent(controller));

    postAlert(AlertCore.newBuilder("Controller Removed")
        .setId(controller.getName())
        .build());
  }

  @Override
  public void stop() {
    mKegboardManager.stop();
    getBus().unregister(this);
  }

  public void toggleOutput(final KegTap tap, final boolean enable) {
    Preconditions.checkNotNull(tap);
    Log.d(TAG, "setTapRelayEnabled tap=" + tap.getMeterName() + " enabled=" + enable);

    final String outputName = tap.getRelayName();
    if (Strings.isNullOrEmpty(outputName)) {
      Log.d(TAG, "No output configured for tap.");
      return;
    }
    mKegboardManager.toggleOutput(outputName, enable);
  }

  @Override
  protected void dump(IndentingPrintWriter writer) {
    writer.print("Dump of KegboardManager:");
    writer.println();
    writer.increaseIndent();
    try {
      mKegboardManager.dump(writer);
    } finally {
      writer.decreaseIndent();
    }
  }

}
