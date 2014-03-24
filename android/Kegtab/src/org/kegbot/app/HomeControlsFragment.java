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

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.common.base.Strings;

import org.kegbot.app.config.AppConfiguration;
import org.kegbot.core.KegbotCore;

/**
 * Fragment showing default controls for the home screen.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class HomeControlsFragment extends Fragment {

  private static final String TAG = HomeControlsFragment.class.getSimpleName();

  private KegbotCore mCore;
  private AppConfiguration mConfig;

  private View mView;
  private Button mBeerMeButton;
  private Button mNewDrinkerButton;

  private static final int REQUEST_AUTHENTICATE = 1000;
  private static final int REQUEST_CREATE_DRINKER = 1001;

  private final OnClickListener mOnBeerMeClickedListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      if (mConfig.useAccounts()) {
        final Intent intent = KegtabCommon.getAuthDrinkerActivityIntent(getActivity());
        startActivityForResult(intent, REQUEST_AUTHENTICATE);
      } else {
        mCore.getFlowManager().activateUserAmbiguousTap("");
      }
    }
  };

  private final OnClickListener mOnNewDrinkerClickedListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      final Intent intent = KegtabCommon.getCreateDrinkerActivityIntent(getActivity());
      startActivityForResult(intent, REQUEST_CREATE_DRINKER);
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mCore = KegbotCore.getInstance(getActivity());
    mConfig = mCore.getConfiguration();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.main_controls_fragment_layout, container, false);

    mBeerMeButton = (Button) mView.findViewById(R.id.beerMeButton);
    mBeerMeButton.setOnClickListener(mOnBeerMeClickedListener);

    mNewDrinkerButton = (Button) mView.findViewById(R.id.newDrinkerButton);
    mNewDrinkerButton.setOnClickListener(mOnNewDrinkerClickedListener);

    return mView;
  }

  @Override
  public void onResume() {
    super.onResume();

    boolean showControls = false;
    if (mConfig.getAllowManualLogin()) {
      mBeerMeButton.setVisibility(View.VISIBLE);
      showControls = true;
    } else {
      mBeerMeButton.setVisibility(View.GONE);
    }

    if (mConfig.getAllowRegistration() && mConfig.useAccounts()) {
      mNewDrinkerButton.setVisibility(View.VISIBLE);
      showControls = true;
    } else {
      mNewDrinkerButton.setVisibility(View.GONE);
    }

    if (showControls && mConfig.getRunCore()) {
      mView.setVisibility(View.VISIBLE);
    } else {
      mView.setVisibility(View.GONE);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_AUTHENTICATE:
        Log.d(TAG, "Got authentication result.");
        if (resultCode == Activity.RESULT_OK && data != null) {
          final String username =
              data.getStringExtra(KegtabCommon.ACTIVITY_AUTH_DRINKER_RESULT_EXTRA_USERNAME);
          if (!Strings.isNullOrEmpty(username)) {
            AuthenticatingActivity.startAndAuthenticate(getActivity(), username,
                mCore.getTapManager().getFocusedTap());
          }
        }
        break;
      case REQUEST_CREATE_DRINKER:
        Log.d(TAG, "Got registration result.");
        if (resultCode == Activity.RESULT_OK && data != null) {
          final String username =
              data.getStringExtra(KegtabCommon.ACTIVITY_CREATE_DRINKER_RESULT_EXTRA_USERNAME);
          if (!Strings.isNullOrEmpty(username)) {
            Log.d(TAG, "Authenticating newly-created user.");
            AuthenticatingActivity.startAndAuthenticate(getActivity(), username,
                mCore.getTapManager().getFocusedTap());
          }
        }
        break;
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

}
