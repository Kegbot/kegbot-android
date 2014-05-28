/*
 * Copyright 2014 Bevbot LLC <info@bevbot.com>
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

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.common.base.Strings;

import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.app.KegbotApplication;
import org.kegbot.app.R;
import org.kegbot.app.config.AppConfiguration;

public class SetupLogInFragment extends SetupFragment {

  private final String TAG = SetupLogInFragment.class.getSimpleName();

  private View mView;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.setup_log_in_fragment, null);
    final AppConfiguration prefs = ((KegbotApplication) getActivity().getApplication()).getConfig();

    EditText text = (EditText) mView.findViewById(R.id.apiUsername);
    text.setText(prefs.getUsername());
    return mView;
  }

  private String getUsername() {
    EditText text = (EditText) mView.findViewById(R.id.apiUsername);
    return text.getText().toString();
  }

  private String getPassword() {
    EditText text = (EditText) mView.findViewById(R.id.apiPassword);
    return text.getText().toString();
  }

  @Override
  public String validate() {
    final AppConfiguration prefs = ((KegbotApplication) getActivity().getApplication()).getConfig();

    final String username = getUsername();
    final String password = getPassword();

    if (Strings.isNullOrEmpty(username)) {
      return "Please enter a username.";
    } else if (Strings.isNullOrEmpty(password)) {
      return "Please enter a password";
    }

    final KegbotApiImpl api = KegbotApiImpl.fromContext(getActivity());

    Log.d(TAG, "Logging in ...");

    try {
      api.login(username, password);
    } catch (KegbotApiException e) {
      return "Error logging in: " + toHumanError(e);
    }

    final String apiKey;
    try {
      apiKey = api.getApiKey();
    } catch (KegbotApiException e) {
      return "Error fetching API key: " + toHumanError(e);
    }

    prefs.setUsername(username);
    prefs.setApiKey(apiKey);
    return "";
  }

}
