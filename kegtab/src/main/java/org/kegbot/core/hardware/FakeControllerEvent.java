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

import org.kegbot.app.DebugBroadcastReceiver;
import org.kegbot.app.event.Event;

/**
 * Private event used by {@link DebugBroadcastReceiver} to notify {@link FakeControllerManager}.
 */
public class FakeControllerEvent implements Event {

  final Controller mController;
  final boolean mAdded;

  public FakeControllerEvent(final Controller controller, final boolean added) {
    mController = controller;
    mAdded = added;
  }

  public Controller getController() {
    return mController;
  }

  public boolean isAdded() {
    return mAdded;
  }

}
