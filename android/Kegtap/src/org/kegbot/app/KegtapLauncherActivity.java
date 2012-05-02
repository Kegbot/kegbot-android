/**
 *
 */
package org.kegbot.app;

import org.kegbot.app.setup.SetupActivity;
import org.kegbot.app.setup.SetupTask;
import org.kegbot.app.util.PreferenceHelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Main launcher activity.
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class KegtapLauncherActivity extends Activity {

  private static final String TAG = KegtapLauncherActivity.class.getSimpleName();

  private static final int REQUEST_START_SETUP = 100;
  private static final int REQUEST_START_MAIN = 101;

  PreferenceHelper mPrefs;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mPrefs = new PreferenceHelper(getApplicationContext());
  }

  @Override
  protected void onStart() {
    super.onStart();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (isFinishing()) {
      return;
    }

    final int setupVersion = mPrefs.getSetupVersion();
    if (mPrefs.getSetupVersion() < SetupTask.SETUP_VERSION) {
      Log.d(TAG, "Setup is not complete, version=" + setupVersion + "current="
          + SetupTask.SETUP_VERSION);
      final Intent setupIntent = new Intent(this, SetupActivity.class);
      if (setupVersion > 0) {
        setupIntent.putExtra(SetupActivity.EXTRA_REASON, SetupActivity.EXTRA_REASON_UPGRADE);
      }
      startActivityForResult(setupIntent, REQUEST_START_SETUP);
    } else {
      Log.d(TAG, "Starting main activity");
      final Intent mainIntent = new Intent(this, KegtapActivity.class);
      startActivityForResult(mainIntent, REQUEST_START_MAIN);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_START_SETUP:
        if (resultCode == RESULT_OK) {
          Log.i(TAG, "Setup completed successfully, returning to main.");
          return;
        } else {
          Log.i(TAG, "Setup aborted, finishing.");
          finish();
          return;
        }
      case REQUEST_START_MAIN:
        // Finished main activity.
        finish();
        return;
      default:
        Log.w(TAG, "Unknown requestCode: " + requestCode);
        finish();
        return;
    }
  }

}
