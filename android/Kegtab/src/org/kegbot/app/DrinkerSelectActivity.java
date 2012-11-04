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
import org.kegbot.core.KegbotCore;
import org.kegbot.proto.Models.User;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class DrinkerSelectActivity extends CoreActivity {

  private static final String TAG = DrinkerSelectActivity.class.getSimpleName();

  private static final String EXTRA_USERNAME = "username";

  private KegbotCore mCore;

  private String mSelectedUsername = "";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.v(TAG, "onCreate");

    mCore = KegbotCore.getInstance(this);

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
  }

  @Override
  protected void onResume() {
    super.onResume();
    final AuthenticationManager am = mCore.getAuthenticationManager();

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
    data.putExtra(KegtabCommon.ACTIVITY_AUTH_DRINKER_RESULT_EXTRA_USERNAME, mSelectedUsername);
    setResult(RESULT_OK, data);
    super.onPause();
  }

  public void handlerUserSelected(User user) {
    Intent resultData = new Intent();
    resultData.putExtra(EXTRA_USERNAME, user.getUsername());
    setResult(RESULT_OK, resultData);
    finish();
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
    }
  }

}
