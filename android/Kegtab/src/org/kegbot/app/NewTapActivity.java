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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;

import org.kegbot.backend.Backend;
import org.kegbot.backend.BackendException;
import org.kegbot.core.KegbotCore;

/**
 * @author mike wakerly (opensource@hoho.com)
 */
public class NewTapActivity extends Activity {
  private static final String TAG = NewTapActivity.class.getSimpleName();

  @InjectView(R.id.newTapName)
  TextView mName;

  @InjectView(R.id.newTapButton)
  Button mActivateButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.new_tap_activity);
    ButterKnife.inject(this);

    mActivateButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        handleDoneButton();
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  private void handleDoneButton() {
    final ProgressDialog dialog = new ProgressDialog(this);
    dialog.setIndeterminate(true);
    dialog.setCancelable(false);
    dialog.setTitle("Activating Tap");
    dialog.setMessage("Please wait ...");
    dialog.show();

    new AsyncTask<Void, Void, String>() {
      @Override
      protected String doInBackground(Void... params) {
        try {
          final Backend backend = KegbotCore.getInstance(NewTapActivity.this).getBackend();
          backend.createTap(mName.getText().toString());
          return "";
        } catch (BackendException e) {
          Log.w(TAG, "Activation failed.", e);
          return e.toString();
        }
      }

      @Override
      protected void onPostExecute(String result) {
        dialog.dismiss();
        if (result.isEmpty()) {
          Log.d(TAG, "Activated successfully!");
          KegbotCore.getInstance(NewTapActivity.this).getSyncManager().requestSync();
          finish();
          return;
        }
        new AlertDialog.Builder(NewTapActivity.this)
            .setCancelable(true)
            .setNegativeButton("Ok", null)
            .setTitle("Activation failed")
            .setMessage("Activation failed: " + result)
            .show();
      }

    }.execute();
  }

  static Intent getStartIntent(Context context) {
    final Intent intent = new Intent(context, NewTapActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    return intent;
  }

}
