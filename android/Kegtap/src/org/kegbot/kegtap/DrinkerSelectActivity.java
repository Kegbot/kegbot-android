/**
 *
 */
package org.kegbot.kegtap;

import org.kegbot.kegtap.setup.SetupEmptyFragment;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class DrinkerSelectActivity extends CoreActivity {

  private static final String TAG = DrinkerSelectActivity.class.getSimpleName();

  private static final String EXTRA_USERNAME = "username";

  static final String EXTRA_TAP_NAME = "tap";

  private String mSelectedUsername = "";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ActionBar bar = getActionBar();
    bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

    bar.addTab(bar.newTab()
        .setText("All Drinkers")
        .setTabListener(new TabListener<DrinkerSelectFragment>(
                this, "simple", DrinkerSelectFragment.class)));

    bar.addTab(bar.newTab()
        .setText("Recent Drinkers")
        .setTabListener(new TabListener<SetupEmptyFragment>(
                this, "recent", SetupEmptyFragment.class)));

    bar.addTab(bar.newTab()
        .setText("New Drinker")
        .setTabListener(new TabListener<SetupEmptyFragment>(
                this, "new", SetupEmptyFragment.class)));

    bar.setTitle("Select Drinker");

    if (savedInstanceState != null) {
        bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
  }

  @Override
  protected void onPause() {
    final Intent data = new Intent();
    data.putExtra(EXTRA_USERNAME, mSelectedUsername);
    setResult(RESULT_OK, data);
    super.onPause();
  }

  public static Intent getStartIntentForTap(final Context context, final String tapName) {
    final Intent intent = new Intent(context, DrinkerSelectActivity.class);
    intent.putExtra(EXTRA_TAP_NAME, tapName);
    return intent;
  }

  public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
    private final Activity mActivity;
    private final String mTag;
    private final Class<T> mClass;
    private final Bundle mArgs;
    private Fragment mFragment;

    public TabListener(Activity activity, String tag, Class<T> clz) {
      this(activity, tag, clz, null);
    }

    public TabListener(Activity activity, String tag, Class<T> clz, Bundle args) {
      mActivity = activity;
      mTag = tag;
      mClass = clz;
      mArgs = args;

      // Check to see if we already have a fragment for this tab, probably
      // from a previously saved state. If so, deactivate it, because our
      // initial state is that a tab isn't shown.
      mFragment = mActivity.getFragmentManager().findFragmentByTag(mTag);
      if (mFragment != null && !mFragment.isDetached()) {
        FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
        ft.detach(mFragment);
        ft.commit();
      }
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
      if (mFragment == null) {
        mFragment = Fragment.instantiate(mActivity, mClass.getName(), mArgs);
        ft.add(android.R.id.content, mFragment, mTag);
      } else {
        ft.attach(mFragment);
      }
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
      if (mFragment != null) {
        ft.detach(mFragment);
      }
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
      Toast.makeText(mActivity, "Reselected!", Toast.LENGTH_SHORT).show();
    }
  }

}
