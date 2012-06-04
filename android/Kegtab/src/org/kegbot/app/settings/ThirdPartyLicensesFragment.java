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
package org.kegbot.app.settings;

import org.kegbot.app.R;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

/**
 * Shows the third party open source licenses in a {@link WebView}.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class ThirdPartyLicensesFragment extends Fragment {

  private static final String TAG = ThirdPartyLicensesFragment.class.getSimpleName();

  private ViewGroup mView;
  private WebView mWebView;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView");
    mView = (ViewGroup) inflater.inflate(R.layout.third_party_licenses_fragment_layout, container, false);
    mWebView = (WebView) mView.findViewById(R.id.thirdPartyLicensesWebView);
    mWebView.loadUrl("file:///android_asset/html/third_party_licenses.html");

    return mView;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "onDestroy");
    if (mWebView != null) {
      mWebView.destroy();
      mWebView = null;
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    Log.d(TAG, "onDestroyView");

    if (mWebView != null) {
      mView.removeView(mWebView);
      mWebView.destroy();
      mWebView = null;
    }
    mView = null;
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.d(TAG, "onPause");
    if (mWebView != null) {
      mWebView.onPause();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume");
    if (mWebView != null) {
      //mWebView.onResume();
    }
  }

}
