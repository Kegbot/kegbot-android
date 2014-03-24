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
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.kegbot.app.R;
import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.service.KegbotCoreService;
import org.kegbot.core.KegbotCore;

import java.util.List;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class SetupActivity extends Activity {

  private static final String TAG = SetupActivity.class.getSimpleName();

  private SetupTask mCurrentTask = null;
  private final List<SetupTask> mTaskHistory = Lists.newArrayList();

  private Button mBackButton;
  private Button mNextButton;
  private DialogFragment mDialog;

  private AsyncTask<Void, Void, String> mValidatorTask;

  public static final String EXTRA_REASON = "reason";
  public static final String EXTRA_REASON_UPGRADE = "upgrade";
  public static final String EXTRA_REASON_USER = "user";

  private static final int MESSAGE_GO_BACK = 100;
  private static final int MESSAGE_START_VALIDATION = 101;
  private static final int MESSAGE_VALIDATION_ABORTED = 102;

  private final Handler mHandler = new Handler() {
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
   * Listener for the on-screen "back" button. Currently just simulates a "back"
   * press.
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

    if (mCurrentTask == null) {
      final String reason = getIntent().getStringExtra(EXTRA_REASON);
      final SetupTask initialTask;
      if (EXTRA_REASON_USER.equals(reason)) {
        initialTask = SetupTask.FIRST_SETUP_STEP;
      } else if (EXTRA_REASON_UPGRADE.equals(reason)) {
        initialTask = SetupTask.UPGRADE;
      } else {
        initialTask = SetupTask.WELCOME;
      }
      setTask(initialTask);
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
    Log.d(TAG, "Starting validation: " + mCurrentTask);
    mValidatorTask = new AsyncTask<Void, Void, String>() {
      @Override
      protected void onPreExecute() {
        showProgressDialog();
      }

      @Override
      protected String doInBackground(Void... params) {
        final Fragment fragment = mCurrentTask.getFragment();
        String result = "";
        if (fragment instanceof SetupFragment) {
          final SetupFragment setupFragment = (SetupFragment) fragment;
          result = setupFragment.validate();
          if (Strings.isNullOrEmpty(result)) {
            EasyTracker.getTracker().sendEvent("SetupTask", mCurrentTask.toString(), "",
                Long.valueOf(1));
          }
        }
        return result;
      }

      @Override
      protected void onPostExecute(String result) {
        if (!isCancelled()) {
          hideDialog();
          onValidationResult(result);
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

  private void onValidationResult(final String result) {
    final AppConfiguration config = KegbotCore.getInstance(this).getConfiguration();
    if (Strings.isNullOrEmpty(result)) {
      Log.d(TAG, "Validation for " + mCurrentTask + " successful!");
      mCurrentTask.onExitSuccess(config);
      setTask(mCurrentTask.next(config));
    } else {
      Log.d(TAG, "Validation for " + mCurrentTask + " unsuccessful: " + result);
      final Fragment fragment = mCurrentTask.getFragment();
      if (fragment instanceof SetupFragment) {
        final SetupFragment setupFragment = (SetupFragment) fragment;
        setupFragment.onValidationFailed();
      }
      showAlertDialog(result);
    }
  }

  private void setTask(SetupTask task) {
    if (task == null) {
      Log.d(TAG, "Null task, finishing.");
      setResult(RESULT_OK);
      mTaskHistory.clear();
      finish();
      return;
    }

    Log.d(TAG, "Loading SetupTask: " + task);
    mTaskHistory.add(task);

    Fragment bodyFragment = task.getFragment();
    if (bodyFragment == null) {
      bodyFragment = new SetupEmptyFragment();
    }

    final String title = getResources().getString(task.getTitle());
    final String description = getResources().getString(task.getDescription());
    final SetupTextFragment textFragment = new SetupTextFragment(title, description);

    mCurrentTask = task;

    FragmentManager fragmentManager = getFragmentManager();
    final FragmentTransaction transaction = fragmentManager.beginTransaction();
    transaction.replace(R.id.setupTextFragment, textFragment);
    transaction.replace(R.id.setupBodyFragment, bodyFragment);
    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
    transaction.addToBackStack(null);
    transaction.commit();
  }

  private void popTask() {
    mTaskHistory.remove(mTaskHistory.size() - 1);
    if (!mTaskHistory.isEmpty()) {
      mCurrentTask = mTaskHistory.get(mTaskHistory.size() - 1);
      Log.d(TAG, "Popped task, current=" + mCurrentTask);
    } else {
      Log.d(TAG, "Popped last task.");
    }
  }

  @Override
  public void onBackPressed() {
    popTask();
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
  }

  @Override
  protected void onStop() {
    super.onStop();
    final KegbotCore core = KegbotCore.getRunningInstance(this);
    if (core != null) {
      core.stop();
    }
    KegbotCoreService.startService(this);
    EasyTracker.getInstance().activityStop(this);
  }

}
