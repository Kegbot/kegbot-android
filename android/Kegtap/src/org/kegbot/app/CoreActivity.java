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
import android.view.MenuItem;

import com.google.android.apps.analytics.easytracking.EasyTracker;

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
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EasyTracker.getTracker().setContext(this);
    bindToCoreService();
  }

  @Override
  protected void onStart() {
    super.onStart();
    EasyTracker.getTracker().trackActivityStart(this);
  }

  @Override
  protected void onStop() {
    super.onStop();
    EasyTracker.getTracker().trackActivityStop(this);
  }

  @Override
  protected void onResume() {
    setupActionBar();
    super.onResume();
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
    final ActionBar actionBar = getActionBar();
    if (actionBar != null) {
      actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_USE_LOGO);
      actionBar.setTitle("");
    }
  }

  protected KegbotCoreServiceInterface getCoreService() {
    return mCoreService;
  }

  protected void onCoreServiceBound() {

  }

  protected void onCoreServiceUnbound() {

  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        Intent intent = new Intent(this, KegtapActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

}
