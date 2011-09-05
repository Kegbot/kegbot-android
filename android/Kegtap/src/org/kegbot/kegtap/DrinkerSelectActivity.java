/**
 *
 */
package org.kegbot.kegtap;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class DrinkerSelectActivity extends CoreActivity {

  private static final String TAG = DrinkerSelectActivity.class.getSimpleName();

  static final String EXTRA_TAP_NAME = "tap";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.drinker_select_activity);
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.d(TAG, "onResume()");
    ((DrinkerSelectFragment) getSupportFragmentManager().findFragmentById(
        R.id.drinkers)).loadEvents();
    getActionBar().hide();
  }

  public static Intent getStartIntentForTap(final Context context, final String tapName) {
    final Intent intent = new Intent(context, DrinkerSelectActivity.class);
    intent.putExtra(EXTRA_TAP_NAME, tapName);
    return intent;
  }

}
