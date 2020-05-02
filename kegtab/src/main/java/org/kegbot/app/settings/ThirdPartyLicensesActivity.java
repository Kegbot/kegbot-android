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
package org.kegbot.app.settings;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import org.kegbot.app.R;

/**
 * Shows the third party open source licenses in a {@link WebView}.
 */
public class ThirdPartyLicensesActivity extends Activity {

  private static final String TAG = ThirdPartyLicensesActivity.class.getSimpleName();

  private WebView mWebView;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate");
    setContentView(R.layout.third_party_licenses_fragment_layout);
    mWebView = (WebView) findViewById(R.id.thirdPartyLicensesWebView);
    mWebView.loadUrl("file:///android_asset/html/third_party_licenses.html");
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
      mWebView.onResume();
    }
  }

}
