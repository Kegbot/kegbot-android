/**
 *
 */
package org.kegbot.kegtap;

import android.os.Bundle;
import android.util.Log;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class DrinkerSelectActivity extends CoreActivity {

  private static final String TAG = DrinkerSelectActivity.class.getSimpleName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.drinker_select_activity);
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.d(TAG, "onResume()");
    ((DrinkerSelectFragment) getFragmentManager().findFragmentById(
        R.id.drinkers)).loadEvents();
    getActionBar().hide();
  }

}
