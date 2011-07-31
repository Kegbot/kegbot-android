/**
 *
 */
package org.kegbot.kegtap;

import android.os.Bundle;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class DrinkerSelectActivity extends CoreActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.drinker_select_activity);
  }

  @Override
  protected void onResume() {
    super.onResume();

    ((DrinkerSelectFragment) getFragmentManager().findFragmentById(
        R.id.drinkers)).loadEvents();

    getActionBar().hide();
  }

}
