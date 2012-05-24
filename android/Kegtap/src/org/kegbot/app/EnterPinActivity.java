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

import org.kegbot.app.util.PreferenceHelper;

import android.app.ActionBar;
import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class EnterPinActivity extends Activity {

  private static final int MAX_FAILURES = 3;

  private PreferenceHelper mPrefs;
  private EditText mPinText;
  private Button mPinButton;

  private int mFailCount = 0;

  @Override
  protected void onStart() {
    super.onStart();
    setContentView(R.layout.enter_pin_activity);

    mPrefs = new PreferenceHelper(getApplicationContext());
    mPinText = (EditText) findViewById(R.id.managerPin);

    mPinText.setOnEditorActionListener(new OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
          verifyPin();
          return true;
        }
        return false;
      }
    });

    mPinButton = (Button) findViewById(R.id.submitButton);
    mPinButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        verifyPin();
      }
    });

    final ActionBar bar = getActionBar();
    if (bar != null) {
      bar.setTitle("");
    }

  }

  private void verifyPin() {
    final String pinText = mPinText.getText().toString();
    if (mPrefs.getPin().equalsIgnoreCase(pinText)) {
      setResult(RESULT_OK);
      finish();
    } else {
      mPinText.setText("");
      mFailCount++;
      if (mFailCount >= MAX_FAILURES) {
        setResult(RESULT_CANCELED);
        finish();
      }
    }
  }

}
