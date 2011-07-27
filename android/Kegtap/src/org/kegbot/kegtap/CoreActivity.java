/**
 *
 */
package org.kegbot.kegtap;

import org.kegbot.kegtap.service.KegbotCoreService;
import org.kegbot.kegtap.service.KegbotCoreServiceInterface;

import android.app.ActionBar;
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
      onCoreServiceBound();
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
      mCoreService = null;
      onCoreServiceUnbound();
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

  protected void setupActionBar() {
    ActionBar actionBar = getActionBar();
    //actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.header_bg_square));
    //actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_USE_LOGO);
    actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME);
    actionBar.setTitle("");
  }

  protected KegbotCoreServiceInterface getCoreService() {
    return mCoreService;
  }

  protected void onCoreServiceBound() {

  }

  protected void onCoreServiceUnbound() {

  }

}
