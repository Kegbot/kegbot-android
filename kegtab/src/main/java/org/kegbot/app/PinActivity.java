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
package org.kegbot.app;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.google.common.base.Strings;

import org.kegbot.app.config.AppConfiguration;
import org.kegbot.core.KegbotCore;

/**
 * Pass-through activity which verifies the manager pin. PIN-verified activities need no special
 * awareness of the PIN. Use {@link #startThroughPinActivity(Context, Intent)} to launch
 * PIN-verified activities.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class PinActivity extends Activity {

  private static final String TAG = PinActivity.class.getSimpleName();
  private static final String EXTRA_KEY_START_INTENT = "start_intent";

  private AppConfiguration mConfig;
  private EditText mPinText;
  private TextView mErrorText;
  private Button mPinButton;

  @Override
  protected void onStart() {
    super.onStart();

    mConfig = KegbotCore.getInstance(this).getConfiguration();
    if (Strings.isNullOrEmpty(mConfig.getPin())) {
      onPinSuccess();
      return;
    }

    setContentView(R.layout.enter_pin_activity);

    mPinText = (EditText) findViewById(R.id.managerPin);

    mPinText.addTextChangedListener(new TextWatcher() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Fade out error text after pin is re-typed.
        if (mErrorText.getVisibility() == View.VISIBLE) {
          final Animation out = new AlphaAnimation(1.0f, 0.0f);
          out.setDuration(1000);
          mErrorText.setAnimation(out);
          mErrorText.setVisibility(View.INVISIBLE);
        }
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void afterTextChanged(Editable s) {
      }
    });

    mPinText.setOnEditorActionListener(new OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (isNexusDone(actionId)
                || (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN)) {
          verifyPin();
          return true;
        }
        return false;
      }
    });

    mErrorText = (TextView) findViewById(R.id.managerPinError);

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

  //Events may vary by keyboard; This one works on N7
  private static boolean isNexusDone(int actionId){
    return actionId == EditorInfo.IME_ACTION_DONE;
  }

  private void onPinSuccess() {
    setResult(RESULT_OK);
    final Intent startIntent = getIntent().getParcelableExtra(EXTRA_KEY_START_INTENT);
    Log.i(TAG, "Pin validated, starting activity.");
    if (startIntent == null) {
      Log.wtf(TAG, "Start intent was null");
      return;
    }
    startActivity(startIntent);
    finish();
  }

  private void onPinFailure() {
    mPinText.setText("");
    mErrorText.setVisibility(View.VISIBLE);
  }

  private void verifyPin() {
    final String pinText = mPinText.getText().toString();
    if (mConfig.getPin().equalsIgnoreCase(pinText)) {
      onPinSuccess();
    } else {
      onPinFailure();
    }
  }

  /**
   * Start a new activity with pin verification.
   *
   * @param context     the context to be used for starting.
   * @param startIntent the intent which shall be used to start the activity upon success.
   */
  public static void startThroughPinActivity(Context context, Intent startIntent) {
    if (Strings.isNullOrEmpty(KegbotCore.getInstance(context).getConfiguration().getPin())) {
      // Short circuit: no manager pin.
      context.startActivity(startIntent);
      return;
    }
    final Intent intent = new Intent(context, PinActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    if ((startIntent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    intent.putExtra(EXTRA_KEY_START_INTENT, startIntent);
    context.startActivity(intent);
  }

}
