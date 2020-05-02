/*
 * Copyright 2003-2020 The Kegbot Project contributors <info@kegbot.org>
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

import android.app.ActionBar;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Strings;

import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.util.ImageDownloader;
import org.kegbot.app.util.Utils;
import org.kegbot.core.KegbotCore;
import org.kegbot.proto.Models.User;

import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;

/**
 * Shows a list of available drinkers, returning the select username (using {@link #setResult(int,
 * Intent)}) when one is selected.
 */
public class DrinkerSelectActivity extends CoreActivity implements LoaderCallbacks<List<User>> {

  private static final String TAG = DrinkerSelectActivity.class.getSimpleName();

  /** Activity will auto-finish after this timeout. */
  private static final long TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(2);

  private static final int REQUEST_CREATE_DRINKER = 1001;

  private GridView mGridView;
  private ArrayAdapter<User> mAdapter;
  private Button mNewAccountButton;
  private Button mPourAsGuestButton;

  private final Handler mHandler = new Handler(Looper.getMainLooper());
  private final Runnable mTimeoutRunnable = new Runnable() {
    @Override
    public void run() {
      Log.d(TAG, "Timeout, finishing.");
      finish();
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.v(TAG, "onCreate");
    setContentView(R.layout.drinker_select_activity_layout);

    final AppConfiguration config = KegbotApplication.get(this).getConfig();

    final ActionBar actionBar = getActionBar();
    if (actionBar != null) {
      actionBar.hide();
    }

    mNewAccountButton = ButterKnife.findById(this, R.id.newAccountButton);
    mNewAccountButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        final Intent intent = KegtabCommon.getCreateDrinkerActivityIntent(
            DrinkerSelectActivity.this);
        startActivityForResult(intent, REQUEST_CREATE_DRINKER);
      }
    });
    if (!config.getAllowRegistration()) {
      findViewById(R.id.newAccountGroup).setVisibility(View.GONE);
    }

    mPourAsGuestButton = ButterKnife.findById(this, R.id.pourAnonymouslyButton);
    mPourAsGuestButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        finishAndReturnUsername("");
      }
    });

    mGridView = (GridView) findViewById(R.id.drinkerGridView);

    mAdapter = new ArrayAdapter<User>(this, R.layout.selectable_drinker,
        R.id.drinkerName) {

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        final User userDetail = getItem(position);
        final View view = super.getView(position, convertView, parent);

        try {
          applyUser(userDetail, view);
        } catch (Throwable e) {
          Log.wtf(TAG, "UNCAUGHT EXCEPTION", e);
        }
        return view;
      }

      private void applyUser(User userDetail, View view) {
        final ImageView icon = (ImageView) view.findViewById(R.id.drinkerIcon);
        icon.setImageBitmap(null);
        Utils.setBackground(icon, null);

        final String imageUrl;
        if (userDetail.hasImage()) {
          imageUrl = userDetail.getImage().getThumbnailUrl();
        } else {
          imageUrl = "";
        }

        final ImageDownloader downloader =
            KegbotCore.getInstance(DrinkerSelectActivity.this).getImageDownloader();
        if (!Strings.isNullOrEmpty(imageUrl)) {
          downloader.download(imageUrl, icon);
        } else {
          downloader.cancelDownloadForView(icon);
          icon.setBackgroundResource(R.drawable.unknown_drinker);
          icon.setAlpha(1.0f);
        }

        final TextView usernameText = (TextView) view.findViewById(R.id.drinkerName);

        final String displayName = userDetail.getDisplayName();
        final String username = userDetail.getUsername();

        final String name = Strings.isNullOrEmpty(displayName) ? username : displayName;
        usernameText.setText(name);
      }

    };

    AnimationSet set = new AnimationSet(true);

    Animation animation = new AlphaAnimation(0.0f, 1.0f);
    animation.setDuration(100);
    set.addAnimation(animation);
    animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.0f,
        Animation.RELATIVE_TO_SELF, 0.0f);
    animation.setDuration(300);
    set.addAnimation(animation);

    LayoutAnimationController controller = new LayoutAnimationController(set, 0.1f);
    mGridView.setAdapter(mAdapter);
    mGridView.setLayoutAnimation(controller);

    mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        final User user = (User) mGridView.getItemAtPosition(position);
        if (user == null) {
          Log.wtf(TAG, "Null user selected.");
          return;
        }
        Log.d(TAG, "Clicked on user: " + user);
        finishAndReturnUsername(user.getUsername());
      }
    });

    getLoaderManager().initLoader(0, null, this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    mHandler.postDelayed(mTimeoutRunnable, TIMEOUT_MILLIS);
  }

  @Override
  protected void onPause() {
    super.onPause();
    mHandler.removeCallbacks(mTimeoutRunnable);
  }

  private void finishAndReturnUsername(String username) {
    Intent resultData = new Intent();
    resultData.putExtras(getIntent());
    resultData.putExtra(KegtabCommon.ACTIVITY_AUTH_DRINKER_RESULT_EXTRA_USERNAME, username);
    setResult(RESULT_OK, resultData);
    finish();
  }

  @Override
  public Loader<List<User>> onCreateLoader(int id, Bundle args) {
    return new UserDetailListLoader(this);
  }

  @Override
  public void onLoadFinished(Loader<List<User>> loader, List<User> userList) {
    for (final User user : userList) {
      mAdapter.add(user);
    }
  }

  @Override
  public void onLoaderReset(Loader<List<User>> loader) {
    mAdapter.clear();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.d(TAG, "onActivityResult: " + requestCode + " : " + resultCode);
    switch (requestCode) {
      case REQUEST_CREATE_DRINKER:
        Log.d(TAG, "Got registration result.");
        if (resultCode == Activity.RESULT_OK && data != null) {
          final String username =
              data.getStringExtra(KegtabCommon.ACTIVITY_CREATE_DRINKER_RESULT_EXTRA_USERNAME);
          if (!Strings.isNullOrEmpty(username)) {
            Log.d(TAG, "Authenticating newly-created user.");
            AuthenticatingActivity.startAndAuthenticate(this, username);
          }
        }
        break;
      default:
        break;
    }
    finish();
  }
}
