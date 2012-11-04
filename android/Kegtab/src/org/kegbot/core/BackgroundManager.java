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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A {@link Manager} which automatically executes {@link #runInBackground()}.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public abstract class BackgroundManager extends Manager {

  private ExecutorService mExecutorService;

  private final Runnable mRunnable = new Runnable() {
    @Override
    public void run() {
      runInBackground();
    }
  };

  @Override
  protected synchronized void start() {
    mExecutorService = Executors.newSingleThreadExecutor();
    mExecutorService.execute(mRunnable);
  }

  @Override
  protected synchronized void stop() {
    mExecutorService.shutdown();
    mExecutorService = null;
  }

  /**
   * Runs in the background after calling {@link #start()}. Implementations
   * performing long-lived background work should respond to {@link #stop()} and
   * abort the work.
   */
  protected abstract void runInBackground();

}
