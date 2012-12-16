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
package org.kegbot.app.setup;

import org.kegbot.app.R;
import org.kegbot.app.service.CheckinService;
import org.kegbot.app.util.PreferenceHelper;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.common.base.Strings;

public class SetupKegbotUrlFragment extends SetupFragment {

  private static final String TAG = SetupKegbotUrlFragment.class.getSimpleName();

  private View mView;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.setup_kegbot_url_fragment, null);
    PreferenceHelper prefs = new PreferenceHelper(getActivity());
    EditText text = (EditText) mView.findViewById(R.id.kegbotUrl);
    final String existingUrl = prefs.getKegbotUrl();
    if (!Strings.isNullOrEmpty(existingUrl)) {
      // Don't clobber the hint if empty.
      text.setText(existingUrl);
    }
    mView.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_rounded_rect));
    return mView;
  }

  public String getUrl() {
    EditText text = (EditText) mView.findViewById(R.id.kegbotUrl);
    return text.getText().toString();
  }

  @Override
  public String validate() {
    String baseUrl = getUrl();
    baseUrl = baseUrl.replaceAll("/$", "");
    if (Strings.isNullOrEmpty(baseUrl)) {
      return "Please provide a valid URL.";
    }

    if (!baseUrl.startsWith("http")) {
      baseUrl = "http://" + baseUrl;
    }

    Log.d(TAG, "Got base URL: " + baseUrl);

    PreferenceHelper prefs = new PreferenceHelper(getActivity());
    CheckinService.requestImmediateCheckin(getActivity());
    prefs.setKegbotUrl(baseUrl);
    return "";
  }

}
