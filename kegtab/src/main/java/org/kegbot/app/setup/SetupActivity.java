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
package org.kegbot.app.setup;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.common.collect.Lists;

import org.kegbot.app.KegbotApplication;
import org.kegbot.app.R;
import org.kegbot.app.service.KegbotCoreService;

import java.util.List;

/**
 * @author mike wakerly (mike@wakerly.com)
 */
public class SetupActivity extends Activity {

  private static final String TAG = SetupActivity.class.getSimpleName();

  public static final int SETUP_VERSION = 6;

  private SetupStep mCurrentStep = null;
  private final List<SetupStep> mTaskHistory = Lists.newArrayList();

  private Button mBackButton;
  private Button mNextButton;
  private DialogFragment mDialog;

  private AsyncTask<Void, Void, Void> mValidatorTask;

  public static final String EXTRA_REASON = "reason";
  public static final String EXTRA_REASON_UPGRADE = "upgrade";
  public static final String EXTRA_REASON_USER = "user";

  private static final int MESSAGE_GO_BACK = 100;
  private static final int MESSAGE_START_VALIDATION = 101;
  private static final int MESSAGE_VALIDATION_ABORTED = 102;

  public interface SetupState {
    public void setNextButtonEnabled(boolean enabled);

    public void setNextButtonText(int resource);
  }

  private final SetupState mSetupState = new SetupState() {
    @Override
    public void setNextButtonEnabled(boolean enabled) {
      mNextButton.setEnabled(enabled);
    }

    @Override
    public void setNextButtonText(int resource) {
      mNextButton.setText(resource);
    }
  };

  private final Handler mHandler = new Handler(Looper.getMainLooper()) {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MESSAGE_GO_BACK:
          dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
          dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
          break;
        case MESSAGE_START_VALIDATION:
          startValidation();
          break;
        case MESSAGE_VALIDATION_ABORTED:
          cancelValidation();
          showAlertDialog("Verification aborted; please try again.");
          break;
        default:
          super.handleMessage(msg);
      }
    }
  };

  /**
   * Listener for the on-screen "back" button. Currently just simulates a "back" press.
   */
  private final OnClickListener mBackListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      Log.d(TAG, "Back pressed.");
      mHandler.sendEmptyMessage(MESSAGE_GO_BACK);
    }
  };

  private final OnClickListener mNextListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      Log.d(TAG, "Next pressed.");
      mHandler.sendEmptyMessage(MESSAGE_START_VALIDATION);
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EasyTracker.getInstance().setContext(this);
    setContentView(R.layout.setup_activity);

    KegbotCoreService.stopService(this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    final ActionBar actionBar = getActionBar();
    if (actionBar != null) {
      actionBar.hide();
    }
    mBackButton = (Button) findViewById(R.id.setupBackButton);
    mBackButton.setOnClickListener(mBackListener);
    mNextButton = (Button) findViewById(R.id.setupNextButton);
    mNextButton.setOnClickListener(mNextListener);

    if (mCurrentStep == null) {
      final String reason = getIntent().getStringExtra(EXTRA_REASON);
      final SetupStep initialStep;
      if (EXTRA_REASON_USER.equals(reason)) {
        initialStep = new SetupSelectBackendStep(mSetupState);
      } else if (EXTRA_REASON_UPGRADE.equals(reason)) {
        initialStep = new SetupWelcomeStep(mSetupState);
      } else {
        initialStep = new SetupWelcomeStep(mSetupState);
      }
      setTask(initialStep);
    }
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
        mHandler.sendEmptyMessage(MESSAGE_VALIDATION_ABORTED);
      }
    });
    mDialog.show(getFragmentManager(), "dialog");
  }

  private void hideDialog() {
    if (mDialog != null) {
      mDialog.dismiss();
      mDialog = null;
    }
  }

  private void startValidation() {
    Log.d(TAG, "Starting validation: " + mCurrentStep);
    mValidatorTask = new AsyncTask<Void, Void, Void>() {

      private SetupValidationException mValidationError;
      private SetupStep mNextStep;

      @Override
      protected void onPreExecute() {
        showProgressDialog();
      }

      @Override
      protected Void doInBackground(Void... params) {
        mValidationError = null;
        mNextStep = null;

        try {
          mNextStep = mCurrentStep.advance();
        } catch (SetupValidationException e) {
          mValidationError = e;
        }

        return null;
      }

      @Override
      protected void onPostExecute(Void avoid) {
        if (!isCancelled()) {
          hideDialog();
          if (mValidationError == null) {
            onValidationSuccess(mNextStep);
          } else {
            onValidationFailure(mValidationError);
          }
        }
      }

    };
    mValidatorTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
  }

  private void cancelValidation() {
    if (mValidatorTask != null) {
      mValidatorTask.cancel(true);
    }
  }

  private void onValidationFailure(final SetupValidationException error) {
    Log.d(TAG, "Validation unsuccessful: " + error, error);
    showAlertDialog(error.getMessage());
  }

  private void onValidationSuccess(final SetupStep nextStep) {
    Log.d(TAG, "Validation successful, next step=" + nextStep);
    setTask(nextStep);
  }

  private void setTask(SetupStep step) {
    if (step == null) {
      Log.d(TAG, "Null task, finishing.");
      KegbotApplication.get(this).getConfig().setSetupVersion(SETUP_VERSION);
      setResult(RESULT_OK);
      mTaskHistory.clear();
      finish();
      return;
    }

    Log.d(TAG, "Loading SetupStep: " + step);
    step.onDisplay();
    mCurrentStep = step;

    mTaskHistory.add(step);
    Fragment contentFragment = step.getContentFragment();
    Fragment controlsFragment = step.getControlsFragment();
    if (controlsFragment == null) {
      controlsFragment = new SetupEmptyFragment();
    }

    FragmentManager fragmentManager = getFragmentManager();
    final FragmentTransaction transaction = fragmentManager.beginTransaction();
    transaction.replace(R.id.setupContentFragment, contentFragment);
    transaction.replace(R.id.setupControlsFragment, controlsFragment);
    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
    transaction.addToBackStack(null);
    transaction.commit();
  }

  private void popTask() {
    mTaskHistory.remove(mTaskHistory.size() - 1);
    if (!mTaskHistory.isEmpty()) {
      mCurrentStep = mTaskHistory.get(mTaskHistory.size() - 1);
    } else {
      Log.d(TAG, "Popped last step.");
    }
  }

  @Override
  public void onBackPressed() {
    popTask();
    mCurrentStep.onDisplay();
    if (mTaskHistory.isEmpty()) {
      setResult(RESULT_CANCELED);
      finish();
    } else {
      super.onBackPressed();
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    EasyTracker.getInstance().activityStart(this);
    KegbotCoreService.stopService(this);
  }

  @Override
  protected void onStop() {
    super.onStop();
    EasyTracker.getInstance().activityStop(this);
  }

}
