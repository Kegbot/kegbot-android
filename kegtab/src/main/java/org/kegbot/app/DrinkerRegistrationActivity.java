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
package org.kegbot.app;

import android.app.ActionBar;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.google.common.base.Strings;

import org.kegbot.app.camera.CameraFragment;
import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.setup.SetupProgressDialogFragment;
import org.kegbot.backend.Backend;
import org.kegbot.backend.BackendException;
import org.kegbot.core.KegbotCore;
import org.kegbot.proto.Models.User;

import java.util.regex.Pattern;

public class DrinkerRegistrationActivity extends CoreActivity {

  private static final String TAG = DrinkerRegistrationActivity.class.getSimpleName();

  private static final Pattern USERNAME_PATTERN = Pattern.compile("^[\\w-]+$");

  private KegbotCore mCore;

  private AppConfiguration mConfig;
  private ViewFlipper mFlipper;
  private Button mSubmitButton;
  private TextView mSubtitle;
  private EditText mUsername;
  private EditText mEmail;
  private CheckBox mConsentCheckbox;
  private CameraFragment mCameraFragment;

  private DialogFragment mDialog;

  private class CheckUsernameTask extends AsyncTask<String, Void, Boolean> {

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      showProgressDialog();
    }

    @Override
    protected Boolean doInBackground(String... params) {
      Backend api = mCore.getBackend();
      try {
        api.getUser(params[0]);
        return Boolean.TRUE;
      } catch (BackendException e) {
      }
      return Boolean.FALSE;
    }

    @Override
    protected void onPostExecute(Boolean result) {
      super.onPostExecute(result);
      hideDialog();
      if (result.booleanValue()) {
        // User already exists.
        mUsername.setError("This username is not available.");
      } else {
        if (mConfig.getUseCamera() && mConfig.getTakePhotosDuringRegistration()) {
          showGetPhoto();
        } else {
          doRegister();
        }
      }
    }

  }

  private class RegistrationTask extends AsyncTask<Void, Void, User> {
    @Override
    protected User doInBackground(Void... params) {
      Backend api = mCore.getBackend();
      Log.d(TAG, "Registering...");
      final String imagePath = mCameraFragment.getLastFilename();
      try {
        final long startTime = SystemClock.elapsedRealtime();
        final User user = api.createUser(mUsername.getText().toString(),
            mEmail.getText().toString(),
            null, imagePath);

        // Make sure we're showing the progress bar for at least
        // 2s; the flashing is jarring / hard to read otherwise.
        final long duration = SystemClock.elapsedRealtime() - startTime;
        if (duration < 2000) {
          SystemClock.sleep(2000 - duration);
        }

        return user;
      } catch (BackendException e) {
        // TODO: Highlight field errors.
        Log.w(TAG, "Registration failed: " + e.toString());
      }
      return null;
    }

    @Override
    protected void onPostExecute(User result) {
      hideDialog();
      if (result != null) {
        Log.d(TAG, "Registration succeeded!");
        final Intent data = new Intent();
        data.putExtra(KegtabCommon.ACTIVITY_CREATE_DRINKER_RESULT_EXTRA_USERNAME,
            result.getUsername());
        setResult(RESULT_OK, data);

        finish();
      }
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ActionBar actionBar = getActionBar();
    if (actionBar != null) {
      actionBar.hide();
    }

    setContentView(R.layout.create_drinker_activity);

    mConfig = KegbotApplication.get(this).getConfig();

    mSubtitle = (TextView) findViewById(R.id.new_drinker_subtitle);
    mFlipper = (ViewFlipper) findViewById(R.id.new_drinker_flipper);
    mFlipper.setInAnimation(this, android.R.anim.fade_in);
    mFlipper.setOutAnimation(this, android.R.anim.fade_out);
    mEmail = (EditText) findViewById(R.id.email);
    mUsername = (EditText) findViewById(R.id.username);
    mSubmitButton = (Button) findViewById(R.id.submitButton);
    mConsentCheckbox = (CheckBox) findViewById(R.id.keghubAgreeTerms);

    final String terms = getString(R.string.register_agree_terms_text);
    mConsentCheckbox.setText(Html.fromHtml(terms));
    mConsentCheckbox.setMovementMethod(LinkMovementMethod.getInstance());
    mConsentCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (b) {
          compoundButton.setError(null);
        }
      }
    });

    if (!mConfig.isKeghub()) {
      mConsentCheckbox.setVisibility(View.GONE);
    }

    mEmail.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_NEXT &&
            (event == null || event.getAction() == KeyEvent.ACTION_DOWN)) {
          onEmailAddressEntered();
        }
        return true;
      }
    });

    mUsername.addTextChangedListener(new TextWatcher() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void afterTextChanged(Editable s) {
        String text = s.toString();
        int length = text.length();

        if (!USERNAME_PATTERN.matcher(text).matches()) {
          if (length > 0) {
            s.delete(length - 1, length);
          }
        }
      }
    });

    mUsername.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_NEXT &&
            (event == null || event.getAction() == KeyEvent.ACTION_DOWN)) {
          onUsernameEntered();
        }
        return true;
      }
    });

    mCameraFragment = (CameraFragment) getFragmentManager().findFragmentById(R.id.camera);
    mCameraFragment.getView().setVisibility(View.GONE);

    mSubmitButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        if (mSubmitButton.isEnabled()) {
          mSubmitButton.setEnabled(false);
          doRegister();
        }
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    mCore = KegbotCore.getInstance(this);
  }

  @Override
  public void onBackPressed() {
    final int child = mFlipper.getDisplayedChild();
    if (child > 0) {
      mFlipper.setDisplayedChild(child - 1);
    } else {
      super.onBackPressed();
    }
  }

  private void onEmailAddressEntered() {
    final String emailAddress = mEmail.getText().toString();
    if (!isValidEmail(emailAddress)) {
      Log.d(TAG, "Invalid email address: " + emailAddress);
      mEmail.setError(getString(R.string.register_error_email_invalid));
      return;
    }

    showGetUsername();
  }

  private void onUsernameEntered() {
    final String username = mUsername.getText().toString();
    if (!USERNAME_PATTERN.matcher(username).matches()) {
      mUsername.setError("Please enter a valid username.");
      showSoftKeyboard(true);
      return;
    }

    if (mConsentCheckbox.getVisibility() == View.VISIBLE) {
      if (!mConsentCheckbox.isChecked()) {
        mConsentCheckbox.setError(getString(R.string.register_error_must_agree));
        return;
      }
    }

    new CheckUsernameTask().execute(username);
  }

  private void showGetUsername() {
    mFlipper.setDisplayedChild(1);
    mSubtitle.setText(R.string.register_username_description);
    showSoftKeyboard(true);
  }

  private void showGetPhoto() {
    mCameraFragment.getView().setVisibility(View.VISIBLE);

    mFlipper.setDisplayedChild(2);
    mSubtitle.setText(R.string.register_photo_description);
    showSoftKeyboard(false);
  }

  private void showSoftKeyboard(boolean enable) {
    InputMethodManager imm = (InputMethodManager) getSystemService(
        Context.INPUT_METHOD_SERVICE);
    if (enable) {
      imm.showSoftInput(mFlipper, 0);
    } else {
      imm.hideSoftInputFromWindow(mFlipper.getWindowToken(), 0);
    }
  }

  private void doRegister() {
    showProgressDialog();
    new RegistrationTask().execute();
  }

  private void showProgressDialog() {
    hideDialog();
    mDialog = new SetupProgressDialogFragment(new SetupProgressDialogFragment.Listener() {
      @Override
      public void onCancel(DialogInterface dialog) {
        //mHandler.sendEmptyMessage(MESSAGE_VALIDATION_ABORTED);
      }
    });
    //mDialog.getDialog().setTitle("Registering");
    mDialog.show(getFragmentManager(), "dialog");
  }

  private void hideDialog() {
    if (mDialog != null) {
      mDialog.dismiss();
      mDialog = null;
    }
    mSubmitButton.setEnabled(true);
  }

  private static boolean isValidEmail(String target) {
    if (Strings.isNullOrEmpty(target)) {
      return false;
    }
    return Patterns.EMAIL_ADDRESS.matcher(target).matches();
  }

}
