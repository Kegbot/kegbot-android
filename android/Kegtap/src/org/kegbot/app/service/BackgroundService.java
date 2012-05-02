/**
 *
 */
package org.kegbot.kegtap.service;

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
