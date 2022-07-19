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
package org.kegbot.app.setup;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.google.common.base.Strings;

import org.kegbot.app.KegbotApplication;
import org.kegbot.app.R;
import org.kegbot.app.config.AppConfiguration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SetupKegbotUrlFragment extends SetupFragment {

  private static final String TAG = SetupKegbotUrlFragment.class.getSimpleName();

  private static final Pattern URL_PATTERN = Pattern.compile("^(https?://)?(.*)");

  private View mView;
  private TextView mText;
  private TextView mSchemeText;
  private CheckBox mUseSsl;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final AppConfiguration prefs = ((KegbotApplication) getActivity().getApplication()).getConfig();

    mView = inflater.inflate(R.layout.setup_kegbot_url_fragment, null);
    mText = (EditText) mView.findViewById(R.id.kegbotUrl);
    mSchemeText = (TextView) mView.findViewById(R.id.kegbotUrlScheme);
    mUseSsl = (CheckBox) mView.findViewById(R.id.useSsl);

    mUseSsl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (b) {
          mSchemeText.setText(R.string.kegbot_url_scheme_https);
        } else {
          mSchemeText.setText(R.string.kegbot_url_scheme_http);
        }
      }
    });
    mUseSsl.setChecked(mText.getText().toString().startsWith("https"));

    final String existingUrl = prefs.getKegbotUrl();
    if (!Strings.isNullOrEmpty(existingUrl)) {
      // Don't clobber the hint if empty.
      final Matcher matcher = URL_PATTERN.matcher(existingUrl);
      if (matcher.matches()) {
        mText.setText(Strings.nullToEmpty(matcher.group(2)));
        mUseSsl.setChecked(Strings.nullToEmpty(matcher.group(1)).startsWith("https"));
      }
    }

    mText.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
      }

      @Override
      public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        final String textStr = mText.getText().toString();

        if (textStr.endsWith(".keghub.com") && !mUseSsl.isChecked()) {
          mUseSsl.setChecked(true);
        }
      }

      @Override
      public void afterTextChanged(Editable editable) {
      }
    });

    return mView;
  }

  public String getUrl() {
    return String.format("%s%s",
        mUseSsl.isChecked() ? "https://" : "http://",
        mText.getText());
  }

  @Override
  public String validate() {
    String baseUrl = getUrl();
    baseUrl = baseUrl.replaceAll("/$", "");
    if (Strings.isNullOrEmpty(baseUrl)) {
      return "Please provide a valid URL.";
    }

    Log.d(TAG, "Got base URL: " + baseUrl);

    final AppConfiguration prefs = ((KegbotApplication) getActivity().getApplication()).getConfig();
    prefs.setKegbotUrl(baseUrl);
    return "";
  }

}
