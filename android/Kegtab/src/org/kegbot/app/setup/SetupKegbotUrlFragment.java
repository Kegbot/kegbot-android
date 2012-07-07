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

import java.util.List;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.app.R;
import org.kegbot.app.service.CheckinService;
import org.kegbot.app.util.PreferenceHelper;
import org.kegbot.proto.Models.SystemEvent;

import android.net.Uri;
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
    Log.d(TAG, "Got base URL: " + baseUrl);

    final String apiUrl = baseUrl + "/api";

    final Uri uri = Uri.parse(apiUrl);
    final String scheme = uri.getScheme();

    if (!"http".equals(scheme) && !"https".equals(scheme)) {
      return "Please enter an HTTP or HTTPs URL.";
    }
    if (Strings.isNullOrEmpty(uri.getHost())) {
      return "Please provide a valid URL.";
    }

    PreferenceHelper prefs = new PreferenceHelper(getActivity());
    CheckinService.requestImmediateCheckin(getActivity());

    KegbotApi api = new KegbotApiImpl();
    api.setApiUrl(apiUrl);

    try {
      final List<SystemEvent> events = api.getRecentEvents();
      Log.d(TAG, "Success: " + events);
    } catch (KegbotApiException e) {
      Log.d(TAG, "Error: " + e.toString(), e);
      StringBuilder builder = new StringBuilder();
      builder.append("Error contacting the Kegbot site: ");
      builder.append(toHumanError(e));
      builder.append("\n\nPlease check the URL and try again.");
      return builder.toString();
    }

    prefs.setKegbotUrl(baseUrl);

    return "";
  }

}
