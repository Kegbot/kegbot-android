package org.kegbot.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

/**
 * An activity representing a list of Taps. This activity has different presentations for handset
 * and tablet-size devices. On handsets, the activity presents a list of items, which when touched,
 * lead to a {@link TapDetailActivity} representing item details. On tablets, the activity presents
 * the list of items and item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a {@link TapListFragment} and the
 * item details (if present) is a {@link TapDetailFragment}.
 * <p/>
 * This activity also implements the required {@link TapListFragment.Callbacks} interface to listen
 * for item selections.
 */
public class TapListActivity extends CoreActivity implements TapListFragment.Callbacks {

  /**
   * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
   */
  private boolean mTwoPane;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_tap_list);
    // Show the Up button in the action bar.
    getActionBar().setDisplayHomeAsUpEnabled(true);

    if (findViewById(R.id.tap_detail_container) != null) {
      // The detail container view will be present only in the
      // large-screen layouts (res/values-large and
      // res/values-sw600dp). If this view is present, then the
      // activity should be in two-pane mode.
      mTwoPane = true;

      // In two-pane mode, list items should be given the
      // 'activated' state when touched.
      ((TapListFragment) getFragmentManager()
          .findFragmentById(R.id.tap_list))
          .setActivateOnItemClick(true);
    }

    // TODO: If exposing deep links into your app, handle intents here.
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        // This ID represents the Home or Up button. In the case of this
        // activity, the Up button is shown. Use NavUtils to allow users
        // to navigate up one level in the application structure. For
        // more details, see the Navigation pattern on Android Design:
        //
        // http://developer.android.com/design/patterns/navigation.html#up-vs-back
        //
        NavUtils.navigateUpFromSameTask(this);
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  /**
   * Callback method from {@link TapListFragment.Callbacks} indicating that the item with the given
   * ID was selected.
   */
  @Override
  public void onItemSelected(int tapId) {
    if (mTwoPane) {
      // In two-pane mode, show the detail view in this activity by
      // adding or replacing the detail fragment using a
      // fragment transaction.
      Bundle arguments = new Bundle();
      arguments.putInt(TapDetailFragment.ARG_ITEM_ID, tapId);
      TapDetailFragment fragment = new TapDetailFragment();
      fragment.setArguments(arguments);
      getFragmentManager().beginTransaction()
          .replace(R.id.tap_detail_container, fragment)
          .commit();

    } else {
      // In single-pane mode, simply start the detail activity
      // for the selected item ID.
      Intent detailIntent = new Intent(this, TapDetailActivity.class);
      detailIntent.putExtra(TapDetailFragment.ARG_ITEM_ID, tapId);
      startActivity(detailIntent);
    }
  }

  public static void startActivity(Context context) {
    final Intent intent = new Intent(context, TapListActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    PinActivity.startThroughPinActivity(context, intent);
  }
}
