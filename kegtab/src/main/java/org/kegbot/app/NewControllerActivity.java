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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.base.Strings;

import org.kegbot.backend.Backend;
import org.kegbot.backend.BackendException;
import org.kegbot.core.KegbotCore;
import org.kegbot.core.SyncManager;
import org.kegbot.proto.Models.Controller;

import butterknife.ButterKnife;

/**
 * Activity shown when a new controller is connected.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class NewControllerActivity extends Activity {

  private static final String TAG = NewControllerActivity.class.getSimpleName();

  private static final String EXTRA_CONTROLLER_NAME = "name";
  private static final String EXTRA_SERIAL_NUMBER = "serial";
  private static final String EXTRA_DEVICE_TYPE = "type";

  private String mControllerName;
  private String mSerialNumber;
  private String mDeviceType;

  TextView mTitle;
  TextView mSubtitle;
  ProgressBar mProgressBar;
  Button mCancelButton;
  Button mAssignButton;
  ViewGroup mNumMetersGroup;
  NumberPicker mNumMeters;

  private final Handler mHandler = new Handler(Looper.getMainLooper());

  private final Runnable mFinishRunnable = new Runnable() {
    @Override
    public void run() {
      finish();
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.new_controller_activity);

    mTitle = ButterKnife.findById(this, R.id.new_controller_title);
    mSubtitle = ButterKnife.findById(this, R.id.new_controller_subtitle);
    mProgressBar = ButterKnife.findById(this, R.id.new_controller_progbar);
    mCancelButton = ButterKnife.findById(this, R.id.new_controller_cancel_button);
    mAssignButton = ButterKnife.findById(this, R.id.new_controller_add_button);
    mNumMetersGroup = ButterKnife.findById(this, R.id.new_controller_num_meters_group);
    mNumMeters = ButterKnife.findById(this, R.id.new_controller_num_meters);

    mProgressBar.setVisibility(View.INVISIBLE);
    mProgressBar.setIndeterminate(true);
    mNumMeters.setMinValue(1);
    mNumMeters.setMaxValue(16);
    mNumMeters.setValue(2);

    // Prevents soft keyboard from appearing.
    mNumMeters.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

    mAssignButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        createController();
      }
    });

    mCancelButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
      }
    });

    handleIntent();
  }

  private void createController() {
    showAdding();

    new AsyncTask<Void, Void, String>() {
      @Override
      protected String doInBackground(Void... params) {
        Log.d(TAG, "Creating controller...");

        final Backend backend = KegbotCore.getInstance(NewControllerActivity.this).getBackend();
        try {
          final Controller controller =
              backend.createController(mControllerName, mSerialNumber, mDeviceType);

          final int numMeters = mNumMeters.getValue();
          for (int portNum = 0; portNum < numMeters; portNum++) {
            backend.createFlowMeter(controller,
                String.format("flow%d", Integer.valueOf(portNum)), 5.4d);
          }

          final SyncManager syncManager =
              KegbotCore.getInstance(NewControllerActivity.this).getSyncManager();
          syncManager.requestSync();
          Log.d(TAG, "Done!");
          return null;
        } catch (BackendException e) {
          Log.e(TAG, "Error creating controller: " + e, e);
          return String.format("Error creating controller (%s).", e.getMessage());
        }
      }

      @Override
      protected void onPostExecute(String result) {
        if (result == null) {
          showSuccess();
        } else {
          showError(result);
        }
      }

    }.execute();
  }

  private void showInitial() {
    mSubtitle.setText(R.string.new_controller_title);
    mSubtitle.setText(String.format("%s %s", mSubtitle.getText(), mControllerName));

    mProgressBar.setVisibility(View.INVISIBLE);
    mAssignButton.setVisibility(View.VISIBLE);
    mCancelButton.setVisibility(View.VISIBLE);
    mNumMetersGroup.setVisibility(View.VISIBLE);

  }

  private void showAdding() {
    mSubtitle.setText(R.string.new_controller_status_adding);

    mProgressBar.setVisibility(View.VISIBLE);
    mProgressBar.setIndeterminate(true);
    mAssignButton.setVisibility(View.GONE);
    mCancelButton.setVisibility(View.GONE);
    mNumMetersGroup.setVisibility(View.GONE);
  }

  private void showSuccess() {
    mSubtitle.setText(R.string.new_controller_status_added);
    mProgressBar.setIndeterminate(false);
    mProgressBar.setProgress(100);

    mHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        finish();
      }
    }, 3000);
  }

  private void showError(String error) {
    mSubtitle.setText(error);

    mProgressBar.setProgress(100);
    mProgressBar.setIndeterminate(false);
    mCancelButton.setVisibility(View.VISIBLE);

    mHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        showInitial();
      }
    }, 5000);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    Log.d(TAG, "onNewIntent: " + intent);
    setIntent(intent);
    handleIntent();
  }

  private void handleIntent() {
    final Intent intent = getIntent();
    Log.d(TAG, "Handling intent: " + intent);

    mControllerName = intent.getStringExtra(EXTRA_CONTROLLER_NAME);
    mSerialNumber = Strings.nullToEmpty(intent.getStringExtra(EXTRA_SERIAL_NUMBER));
    mDeviceType = Strings.nullToEmpty(intent.getStringExtra(EXTRA_DEVICE_TYPE));
    mSubtitle.setText(String.format("%s %s", mSubtitle.getText(), mControllerName));
  }


  @Override
  protected void onPause() {
    super.onPause();
    mHandler.removeCallbacks(mFinishRunnable);
  }

  public static void startForNewController(Context context, String controllerName,
      String serialNumber, String deviceType) {
    final Intent intent = new Intent(context, NewControllerActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.putExtra(EXTRA_CONTROLLER_NAME, Strings.nullToEmpty(controllerName));
    intent.putExtra(EXTRA_SERIAL_NUMBER, Strings.nullToEmpty(serialNumber));
    intent.putExtra(EXTRA_DEVICE_TYPE, Strings.nullToEmpty(deviceType));
    context.startActivity(intent);
  }

}
