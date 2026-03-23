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

package org.kegbot.core.hardware;

import android.content.Context;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.squareup.otto.Bus;

import org.kegbot.app.alert.AlertCore;
import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.event.Event;
import org.kegbot.app.util.IndentingPrintWriter;
import org.kegbot.backend.Backend;
import org.kegbot.core.Manager;
import org.kegbot.proto.Models.FlowToggle;
import org.kegbot.proto.Models.KegTap;

import java.util.Map;
import java.util.Set;

/**
 * The HardwareManager provides implementation-independent access to sensors to the rest of the
 * Kegbot core. It is also responsible for admitting and removing controllers based on internal
 * policy. <h2>Life of a Controller</h2> <p> Discovery, connection, and management of controllers is
 * not done directly by HardwareManager. Instead, subordinate manager classes handle this; at time
 * of writing, {@link KegboardManager} is the sole implementation, althought others are possible.
 * </p> <h3>Notification to HardwareManager</h3> <p> When a controller is attached, the subordinate
 * manager will deliver a callback to {@link HardwareManager} via {@link
 * #onControllerAttached(Controller)}. This callback will be issued regardless of the controller's
 * operational status ( {@link Controller#getStatus()}), in other words, the controller may be
 * attached in a non-functional state. </p> <h3>Notification to upper layers</h3> <p> After
 * receiving notification from the subordinate manager, the HardwareManager will issue a {@link
 * ControllerAttachedEvent} on the core bus, and the controller will be considered <em>operationally
 * disabled</em> until {@link #enableController(Controller)} is called. This allows a higher-level
 * component (such as the user interface) to gate operational status. In the typical case, a
 * controller will be immediately enabled unless it is unrecognized ({@link Controller#getName()}
 * does not match any known to the backend). </p>
 */
public class HardwareManager extends Manager {

  private static String TAG = HardwareManager.class.getSimpleName();

  /** All controllers, by operational status. */
  private final Map<Controller, Boolean> mControllers = Maps.newLinkedHashMap();
  
  /** Track which controller names have been notified to prevent duplicate events. */
  private final Set<String> mNotifiedControllerNames = Sets.newHashSet();

  private final Set<ControllerManager> mManagers = Sets.newLinkedHashSet();
  private KegboardManager mKegboardManager;

  private final ControllerManager.Listener mListener = new ControllerManager.Listener() {
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
  };

  public HardwareManager(Bus bus, Context context, AppConfiguration config) {
    super(bus);

    mKegboardManager = new KegboardManager(getBus(), context, mListener);
    mManagers.add(mKegboardManager);
    mManagers.add(new FakeControllerManager(getBus(), mListener));
    mManagers.add(new NetworkControllerManager(getBus(), mListener, config));
    mManagers.add(new MQTTControllerManager(getBus(), mListener, config));
  }

  @Override
  public void start() {
    for (final ControllerManager subordinate : mManagers) {
      subordinate.start();
    }
    getBus().register(this);
  }

  public void refreshSoon() {
    for (final ControllerManager subordinate : mManagers) {
      subordinate.refreshSoon();
    }
  }

  private synchronized void onControllerAttached(Controller controller) {
    Log.d(TAG, "Controller attached: " + controller + " with name: " + controller.getName());
    
    // Check if a controller with the same name already exists
    Controller existingController = null;
    for (Controller c : mControllers.keySet()) {
      if (c.getName().equals(controller.getName())) {
        existingController = c;
        break;
      }
    }
    
    if (existingController != null) {
      // Check if it's actually the same controller instance
      if (existingController == controller) {
        Log.w(TAG, "Same controller instance '" + controller.getName() + "' attached again! Ignoring duplicate.");
        return;
      }
      
      Log.w(TAG, "Controller with name '" + controller.getName() + "' already attached! This appears to be a reconnection, updating controller instance.");
      // Remove the old controller instance and replace with new one (for reconnection scenarios)
      Boolean wasEnabled = mControllers.get(existingController);
      mControllers.remove(existingController);
      mControllers.put(controller, wasEnabled); // Preserve the enabled state
      // Don't post ControllerAttachedEvent for reconnections to avoid duplicate notifications
      return;
    }
    
    // Check if we've already notified about this controller name
    if (mNotifiedControllerNames.contains(controller.getName())) {
      Log.w(TAG, "Controller with name '" + controller.getName() + "' has already been notified, skipping duplicate notification.");
      mControllers.put(controller, Boolean.FALSE);
      return;
    }
    
    // This is a truly new controller
    Log.d(TAG, "Adding new controller: " + controller.getName());
    mControllers.put(controller, Boolean.FALSE);
    mNotifiedControllerNames.add(controller.getName());
    postOnMainThread(new ControllerAttachedEvent(controller));
  }

  private synchronized void onControllerEvent(Controller controller, Event event) {
    // Check if a controller with the same name exists
    Controller existingController = null;
    for (Controller c : mControllers.keySet()) {
      if (c.getName().equals(controller.getName())) {
        existingController = c;
        break;
      }
    }
    
    if (existingController == null) {
      Log.w(TAG, "Received event from unknown controller: " + controller);
      return;
    }
    if (controller.getStatus().equals(Controller.STATUS_OK)) {
      postOnMainThread(event);
    } else {
      Log.d(TAG, String.format("Dropping event from offline controller %s: %s",
          controller, event));
    }
  }

  private synchronized void onControllerRemoved(Controller controller) {
    // Find and remove controller with the same name
    Controller controllerToRemove = null;
    for (Controller c : mControllers.keySet()) {
      if (c.getName().equals(controller.getName())) {
        controllerToRemove = c;
        break;
      }
    }
    
    if (controllerToRemove == null) {
      Log.w(TAG, "Unknown controller was detached: " + controller);
      return;
    }
    mControllers.remove(controllerToRemove);
    mNotifiedControllerNames.remove(controller.getName()); // Clear notification tracking
    postOnMainThread(new ControllerDetachedEvent(controller));

    postAlert(AlertCore.newBuilder("Controller Removed")
        .setId(controller.getName())
        .build());
  }

  @Override
  public void stop() {
    for (final ControllerManager subordinate : mManagers) {
      subordinate.stop();
    }
    getBus().unregister(this);
  }

  public void enableController(Controller controller) {

  }

  public void toggleOutput(final KegTap tap, final boolean enable) {
    Preconditions.checkNotNull(tap);
    Log.d(TAG, "toggleOutput tap=" + tap.getId() + " enabled=" + enable);

    final FlowToggle toggle = tap.getToggle();
    if (toggle == null) {
      Log.d(TAG, "No toggle bound to tap.");
      return;
    }

    mKegboardManager.toggleOutput(toggle, enable);
  }

  @Override
  protected void dump(IndentingPrintWriter writer) {
    writer.print("Controllers:");
    writer.println();
    writer.increaseIndent();
    for (final Controller controller : mControllers.keySet()) {
      writer.printPair(controller.toString(), mControllers.get(controller)).println();
    }
    writer.decreaseIndent();
    writer.println();

    for (final ControllerManager subordinate : mManagers) {
      writer.print("Dump of " + subordinate + ":\n");
      writer.increaseIndent();
      try {
        subordinate.dump(writer);
      } finally {
        writer.decreaseIndent();
        writer.println();
      }
    }
  }

}
