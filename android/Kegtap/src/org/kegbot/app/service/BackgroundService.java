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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 *
 * @author mike
 */
public abstract class BackgroundService extends Service {

  protected final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

  protected final Runnable mBackgroundRunnable = new Runnable() {
    @Override
    public void run() {
      try {
        runInBackground();
      } catch (Throwable e) {
        Log.wtf("BackgroundService", "UNCAUGHT EXCEPTION", e);
      }
    }
  };

  @Override
  public void onCreate() {
    super.onCreate();
    mExecutorService.submit(mBackgroundRunnable);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onDestroy() {
    mExecutorService.shutdown();
    super.onDestroy();
  }

  protected abstract void runInBackground();

}
