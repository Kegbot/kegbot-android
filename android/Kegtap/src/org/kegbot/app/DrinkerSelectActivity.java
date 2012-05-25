/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
 *
 * This file is part of the Kegtab package from the Kegbot project. For
 * more information on Kegtab or Kegbot, see <http://kegbot.org/>.
 *
 * Kegtab is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 2.
 *
 * Kegtab is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with Kegtab. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kegbot.app;

import org.kegbot.core.AuthenticationManager;
import org.kegbot.proto.Models.User;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.common.base.Strings;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class DrinkerSelectActivity extends CoreActivity {

  private static final String TAG = DrinkerSelectActivity.class.getSimpleName();

  private static final String EXTRA_USERNAME = "username";

  private String mSelectedUsername = "";

  private AuthenticationManager mAuthManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.v(TAG, "onCreate");

    final ActionBar bar = getActionBar();
    bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

    bar.addTab(bar.newTab()
        .setText("All Drinkers")
        .setTabListener(new TabListener<DrinkerSelectFragment>(
                this, "simple", DrinkerSelectFragment.class)));

    final Bundle args = new Bundle();
    args.putString(DrinkerSelectFragment.LOAD_SOURCE, DrinkerSelectFragment.LOAD_SOURCE_RECENT);
    bar.addTab(bar.newTab()
        .setText("Recent Drinkers")
        .setTabListener(new TabListener<DrinkerSelectFragment>(
                this, "recent", DrinkerSelectFragment.class, args)));

    bar.setTitle("Select Drinker");

    if (savedInstanceState != null) {
        bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
    }

    mAuthManager = AuthenticationManager.getSingletonInstance(this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    final AuthenticationManager am = AuthenticationManager.getSingletonInstance(this);

    if (!am.getAllRecent().isEmpty()) {
      final ActionBar bar = getActionBar();
      bar.setSelectedNavigationItem(1);
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

  public void handlerUserSelected(User user) {
    final String tapName = getIntent().getStringExtra(KegtapBroadcast.DRINKER_SELECT_EXTRA_TAP_NAME);
    if (!Strings.isNullOrEmpty(tapName)) {
      mAuthManager.noteUserAuthenticated(user, tapName);
    } else {
      mAuthManager.noteUserAuthenticated(user);
    }
    finish();
  }

  public static Intent getStartIntentForTap(final Context context, final String tapName) {
    final Intent intent = new Intent(context, DrinkerSelectActivity.class);
    intent.putExtra(KegtapBroadcast.DRINKER_SELECT_EXTRA_TAP_NAME, tapName);
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
