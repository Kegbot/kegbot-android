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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.kegbot.app.util.Units;
import org.kegbot.app.view.BadgeView;
import org.kegbot.backend.Backend;
import org.kegbot.backend.BackendException;
import org.kegbot.core.FlowMeter;
import org.kegbot.core.KegbotCore;
import org.kegbot.core.hardware.HardwareManager;
import org.kegbot.core.hardware.MeterUpdateEvent;
import org.kegbot.proto.Models.KegTap;

public class CalibrationActivity extends CoreActivity {

  private static final String TAG = CalibrationActivity.class.getSimpleName();
  private static final boolean DEBUG = false;

  private static final String EXTRA_METER_NAME = "meter_name";
  private static final String EXTRA_ML_PER_TICK = "ml_per_tick";

  // 50% - 150%
  // (0, 0.5)
  // (100, 1.5)
  private static final double SCALE_M = (1.5 - 0.5) / 100;
  // y = Mx + b
  // b = y - Mx
  private static final double SCALE_B = 1.5 - (SCALE_M * 100);

  private HardwareManager mHardwareManager;

  private KegTap mTap;
  private String mMeterName;

  private double mTicksPerMl;
  private double mExistingTicksPerMl;

  private BadgeView mTicksBadge;
  private SeekBar mSeekBar;
  private BadgeView mVolumeBadge;
  private TextView mPercentText;
  private TextView mCalibratedMlTickText;
  private TextView mOriginalMlTickText;

  private Button mDoneButton;
  private Button mResetButton;

  private long mLastReading = Long.MIN_VALUE;
  private long mTicks = 0;

  @Subscribe
  public void onMeterUpdate(MeterUpdateEvent event) {
    final FlowMeter meter = event.getMeter();
    final String name = meter.getMeterName();
    Log.d(TAG, "Meter update: " + name);
    if (name.equals(mMeterName)) {
      handleMeterUpdate(meter.getTicks());
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.calibration_activity);
  }

  @Override
  protected void onStart() {
    super.onStart();
    mMeterName = getIntent().getStringExtra(EXTRA_METER_NAME);
    mTap = KegbotCore.getInstance(this).getTapManager().getTapForMeterName(mMeterName);
    if (mTap == null || mTap.getMeter() == null) {
      Log.e(TAG, "No tap for meter name: " + mMeterName);
      finish();
      return;
    }

    mExistingTicksPerMl = mTap.getMeter().getTicksPerMl();
    mTicksPerMl = mExistingTicksPerMl;

    mTicksBadge = (BadgeView) findViewById(R.id.ticksBadge);
    mTicksBadge.setBadgeCaption("Ticks");
    mTicksBadge.setBadgeValue("0");

    mVolumeBadge = (BadgeView) findViewById(R.id.volumeBadge);
    mVolumeBadge.setBadgeCaption("Actual Ounces "); // TODO(mikey): units
    mVolumeBadge.setBadgeValue("0.0");

    mVolumeBadge.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onVolumeBadgeClicked();
      }
    });

    mPercentText = (TextView) findViewById(R.id.calibratePercent);
    mCalibratedMlTickText = (TextView) findViewById(R.id.calibratedTicksPerMl);
    mOriginalMlTickText = (TextView) findViewById(R.id.originalTicksPerMl);
    mOriginalMlTickText.setText(Double.valueOf(mExistingTicksPerMl).toString());
    mOriginalMlTickText.setText(String.format("%.2f", Double.valueOf(mExistingTicksPerMl)));

    mSeekBar = (SeekBar) findViewById(R.id.calibrateSeekBar);
    mSeekBar.setMax(99);
    mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        double mult = getMultiplier();
        mTicksPerMl = mExistingTicksPerMl * mult;
        updateMetrics();
        if (DEBUG) Log.d(TAG, "onProgressCchanged: progress=" + progress + " mult=" + mult);
      }
    });

    mResetButton = (Button) findViewById(R.id.calibrateReset);
    mResetButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        resetCalibration();
      }
    });

    mDoneButton = (Button) findViewById(R.id.calibrateDone);
    mDoneButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        finishCalibration();
      }
    });

    Log.d(TAG, "Started: meterName=" + mMeterName);
    resetCalibration();
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.d(TAG, "onResume");
    mHardwareManager = KegbotCore.getInstance(getApplicationContext()).getHardwareManager();
    mHardwareManager.toggleOutput(mTap, true);

    final KegbotCore core = KegbotCore.getInstance(this);
    core.getBus().register(this);
    core.getFlowManager().setPaused(true);

    mTap = KegbotCore.getInstance(getApplicationContext()).getTapManager().getTapForMeterName(
        mMeterName);
    mHardwareManager.toggleOutput(mTap, true);
  }

  @Override
  protected void onPause() {
    mHardwareManager.toggleOutput(mTap, false);
    final KegbotCore core = KegbotCore.getInstance(this);
    core.getFlowManager().setPaused(false);
    core.getBus().unregister(this);
    super.onPause();
  }

  private void handleMeterUpdate(long meterReading) {
    if (mLastReading < 0) {
      mLastReading = meterReading;
      return;
    }
    final long delta = meterReading - mLastReading;
    mTicks += delta;
    Log.d(TAG, "Update: delta=" + delta + " ticks=" + mTicks);
    mLastReading = meterReading;

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        updateMetrics();
      }
    });
  }

  private double getMultiplier() {
    double result = SCALE_M * mSeekBar.getProgress() + SCALE_B;
    if (DEBUG) {
      Log.d(TAG, "getMultipler: " + result + " = " + SCALE_M + "*" + mSeekBar.getProgress() + " + "
          + SCALE_B);
    }
    return result;
  }

  private void updateMetrics() {
    mTicksBadge.setBadgeValue(String.valueOf(mTicks));
    mVolumeBadge.setBadgeValue(getDisplayVolume());
    mCalibratedMlTickText.setText(String.format("%.2f", Double.valueOf(mTicksPerMl)));

    Integer percent = Integer.valueOf((int) (getMultiplier() * 100));
    mPercentText.setText(String.format("%s%%", percent));

    if (mTicksPerMl != mExistingTicksPerMl) {
      mResetButton.setEnabled(true);
      mDoneButton.setEnabled(true);
    } else {
      mResetButton.setEnabled(false);
      mDoneButton.setEnabled(false);
    }
  }

  private void resetCalibration() {
    mTicksPerMl = mExistingTicksPerMl;
    mSeekBar.setEnabled(true);
    mSeekBar.setProgress(50);
    mResetButton.setEnabled(false);
    updateMetrics();
  }

  private void finishCalibration() {
    if (mExistingTicksPerMl == mTicksPerMl) {
      Log.d(TAG, "No change.");
      finish();
      return;
    }

    final ProgressDialog dialog = new ProgressDialog(this);
    dialog.setIndeterminate(true);
    dialog.setCancelable(false);
    dialog.setTitle("Calibrating Tap");
    dialog.setMessage("Please wait ...");
    dialog.show();

    new AsyncTask<Void, Void, String>() {
      @Override
      protected String doInBackground(Void... params) {
        final KegbotCore core = KegbotCore.getInstance(CalibrationActivity.this);
        final Backend backend = core.getBackend();

        try {
          backend.calibrateMeter(mTap.getMeter(), mTicksPerMl);
          core.getSyncManager().requestSync();
          return "";
        } catch (BackendException e) {
          Log.w(TAG, "Error calibrating: " + e, e);
          return e.toString();
        }
      }

      @Override
      protected void onPostExecute(String result) {
        dialog.dismiss();
        if (result.isEmpty()) {
          Log.d(TAG, "Calibrated successfully!");
          finish();
          return;
        }
        new AlertDialog.Builder(CalibrationActivity.this)
            .setCancelable(true)
            .setNegativeButton("Ok", null)
            .setTitle("Calibration failed")
            .setMessage("Calibration failed: " + result)
            .show();
      }
    }.execute();

  }

  private void onVolumeBadgeClicked() {
    if (mTicks <= 0) {
      showNeedTicksDialog();
    } else {
      showCustomVolumeDialog();
    }
  }

  /** Admonishes the user to pour something before calibrating. */
  private void showNeedTicksDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setCancelable(true)
        .setTitle(R.string.calibrate_dialog_no_ticks_title)
        .setMessage(R.string.calibrate_dialog_no_ticks_message)
        .setPositiveButton("Ok", null)
        .show();
  }

  /** Prompts the user to enter a custom volume for current ticks. */
  private void showCustomVolumeDialog() {
    final View dialogView =
        getLayoutInflater().inflate(R.layout.calibrate_custom_volume_dialog, null);
    final EditText dialogVolume = (EditText) dialogView.findViewById(R.id.calibrateCustomVolume);
    dialogVolume.append(getDisplayVolume());  // advances cursor to end, too

    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setCancelable(true)
        .setTitle(R.string.calibrate_dialog_custom_title)
        .setMessage(R.string.calibrate_dialog_custom_message)
        .setPositiveButton("Use This Volume", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            Log.d(TAG, "Custom volume: " + dialogVolume.getText());
            onCustomVolumeEntered(dialogVolume.getText().toString());
          }
        })
        .setView(dialogView)
        .setNegativeButton("Cancel", null)
        .show();
  }

  private void onCustomVolumeEntered(String stringValue) {
    final Double value;
    try {
      value = Double.valueOf(stringValue);
    } catch (NumberFormatException e) {
      Log.w(TAG, "Dropping bogus value: " + e);
      return;
    }

    if (value.doubleValue() <= 0) {
      Log.w(TAG, "Value out of range: " + value);
      return;
    }

    mTicksPerMl = mTicks / Units.volumeOuncesToMl(value.doubleValue());

    mVolumeBadge.setBadgeValue(String.format("%.2f", value));
    mSeekBar.setEnabled(false);
    mResetButton.setEnabled(true);
    updateMetrics();
  }

  private double getTicksPerMl() {
    return getMultiplier() * mTicksPerMl;
  }

  private double getVolumeMl() {
    return mTicks / getTicksPerMl();
  }

  private String getDisplayVolume() {
    return String.format("%.2f", Double.valueOf(Units.volumeMlToOunces(getVolumeMl())));
  }

  static Intent getStartIntent(final Context context, final KegTap tap) {
    final Intent intent = new Intent(context, CalibrationActivity.class);
    intent.putExtra(EXTRA_METER_NAME, tap.getMeter().getName());
    intent.putExtra(EXTRA_ML_PER_TICK, 1 / tap.getMeter().getTicksPerMl());
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    return intent;
  }

}
