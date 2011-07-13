/**
 *
 */
package org.kegbot.kegtap;

import org.kegbot.kegtap.service.KegbotCoreService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

/**
 * An activity which is bound to the core service.
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class CoreActivity extends Activity {

  protected KegbotCoreService mCoreService;
  protected boolean mCoreServiceBound;

  /**
   * Connection to the Core service.
   */
  protected ServiceConnection mCoreServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      mCoreService = ((KegbotCoreService.LocalBinder) service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
      mCoreService = null;
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    bindToCoreService();
  }

  @Override
  protected void onDestroy() {
    try {
      unbindFromCoreService();
    } finally {
      super.onDestroy();
    }
  }

  protected void bindToCoreService() {
    final Intent serviceIntent = new Intent(this, KegbotCoreService.class);
    bindService(serviceIntent, mCoreServiceConnection, BIND_AUTO_CREATE);
    mCoreServiceBound = true;
  }

  protected void unbindFromCoreService() {
    if (mCoreServiceBound) {
      unbindService(mCoreServiceConnection);
      mCoreServiceBound = false;
    }
  }

}
