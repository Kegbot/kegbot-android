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

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.app.camera.CameraFragment;
import org.kegbot.app.setup.SetupAlertDialogFragment;
import org.kegbot.app.setup.SetupProgressDialogFragment;
import org.kegbot.core.AuthenticationManager;
import org.kegbot.proto.Models.User;

import android.app.ActionBar;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class DrinkerRegistrationActivity extends CoreActivity {

  private static final String TAG = DrinkerRegistrationActivity.class.getSimpleName();

  private Button mSubmitButton;
  private EditText mUsername;
  private EditText mEmail;
  private EditText mPassword;
  private CameraFragment mCameraFragment;
  private AuthenticationManager mAuthManager;

  private DialogFragment mDialog;

  private class RegistrationTask extends AsyncTask<Void, Void, User> {

    @Override
    protected User doInBackground(Void... params) {
      KegbotApi api = KegbotApiImpl.getSingletonInstance();
      Log.d(TAG, "Registering...");
      final String imagePath = mCameraFragment.getLastFilename();
      try {
        return api.register(mUsername.getText().toString(), mEmail.getText().toString(),
            mPassword.getText().toString(), imagePath);
      } catch (KegbotApiException e) {
        Log.w(TAG, "Registration failed: " + e.toString() + " errors=" + e.getErrors());
      }
      return null;
    }

    @Override
    protected void onPostExecute(User result) {
      hideDialog();
      if (result != null) {
        Log.d(TAG, "Registration succeeded! Posting authentication.");
        mAuthManager.noteUserAuthenticated(result);
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
    mUsername = (EditText) findViewById(R.id.username);
    mEmail = (EditText) findViewById(R.id.email);
    mPassword = (EditText) findViewById(R.id.password);
    mSubmitButton = (Button) findViewById(R.id.submitButton);

    mCameraFragment = (CameraFragment) getFragmentManager().findFragmentById(R.id.camera);

    mAuthManager = AuthenticationManager.getSingletonInstance(this);

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

  private void doRegister() {
    showProgressDialog();
    new RegistrationTask().execute();
  }

  private void showAlertDialog(String message) {
    hideDialog();
    final DialogFragment dialog = SetupAlertDialogFragment.newInstance(message);
    dialog.show(getFragmentManager(), "dialog");
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

}
