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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.kegbot.app.KegbotApplication;
import org.kegbot.app.R;
import org.kegbot.app.config.AppConfiguration;

public class SetupManagerPinFragment extends SetupFragment {

  private View mView;
  private EditText mPinText;
  private EditText mPinConfirmText;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.setup_manager_pin_fragment, null);

    final AppConfiguration prefs = ((KegbotApplication) getActivity().getApplication()).getConfig();

    mPinText = (EditText) mView.findViewById(R.id.managerPin);
    mPinText.setText(prefs.getPin());
    mPinText.addTextChangedListener(new TextWatcher() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        mPinConfirmText.setText("");
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void afterTextChanged(Editable s) {
      }
    });
    mPinConfirmText = (EditText) mView.findViewById(R.id.managerPinConfirm);
    mPinConfirmText.setText(prefs.getPin());

    return mView;
  }

  public String getPin() {
    return mPinText.getText().toString();
  }

  public String getConfirmedPin() {
    return mPinConfirmText.getText().toString();
  }

  @Override
  public String validate() {
    if (!getPin().equals(getConfirmedPin())) {
      return "Pins do not match.";
    }
    final AppConfiguration prefs = ((KegbotApplication) getActivity().getApplication()).getConfig();
    prefs.setPin(getPin());
    return "";
  }

  @Override
  protected void onValidationFailed() {
    clearPins();
  }

  private void clearPins() {
    mPinText.setText("");
    mPinConfirmText.setText("");
    mPinText.requestFocus();
  }

}
