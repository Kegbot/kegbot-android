/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
 *
 * This file is part of the Kegtab package from the Kegbot project. For more
 * information on Kegtab or Kegbot, see <http://kegbot.org/>.
 *
 * Kegtab is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2.
 *
 * Kegtab is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Kegtab. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kegbot.core;

import org.kegbot.app.util.IndentingPrintWriter;

import android.util.Log;

import com.squareup.otto.Bus;

/**
 * Base class for Kegbot core components.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public abstract class Manager {

  private final String mName;
  private final Bus mBus;

  public Manager(Bus bus) {
    mBus = bus;
    mName = getClass().getSimpleName();
    Log.d(mName, "Manager started: " + mName);
  }

  public String getName() {
    return mName;
  }

  /**
   * Called when the Kegbot Core is starting. The manager should initialize any
   * resources it needs during normal operation. The default implementation is a
   * no-op.
   */
  protected void start() {
  }

  /**
   * Called when the Kegbot Core is stopping. The manager should release any
   * resources it is holding. The default implementation is a no-op.
   */
  protected void stop() {

  }

  /**
   * Called when the Kegbot Core requests the manager to reload any state. This
   * should be logically equivalent to <code>stop(); start();</code> (which is
   * the default implementation).
   */
  protected void reload() {
    Log.d(mName, "Reloading (default implementation).");
    stop();
    start();
  }

  /**
   * Writes manager-specific debug information.
   */
  protected void dump(IndentingPrintWriter writer) {

  }

}
