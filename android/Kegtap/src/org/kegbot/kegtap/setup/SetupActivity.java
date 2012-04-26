/**
 *
 */
package org.kegbot.kegtap.setup;

import java.util.List;

import org.kegbot.kegtap.R;

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

import com.google.android.apps.analytics.easytracking.EasyTracker;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

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
          if (mValidatorTask != null) {
            mValidatorTask.cancel(true);
          }
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
    EasyTracker.getTracker().setContext(this);
    setContentView(R.layout.setup_activity);
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
      setTask(SetupTask.WELCOME);
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
            EasyTracker.getTracker().trackEvent("SetupTask", mCurrentTask.toString(), "", 1);
          }
        }
        return result;
      }

      @Override
      protected void onCancelled() {
        hideDialog();
        onValidationResult("Validation cancelled, please try again.");
      }

      @Override
      protected void onPostExecute(String result) {
        hideDialog();
        onValidationResult(result);
      }

    };
    mValidatorTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
  }

  private void onValidationResult(final String result) {
    if (Strings.isNullOrEmpty(result)) {
      Log.d(TAG, "Validation for " + mCurrentTask + " successful!");
      mCurrentTask.onExitSuccess(this);
      setTask(mCurrentTask.next());
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
    EasyTracker.getTracker().trackActivityStart(this);
  }

  @Override
  protected void onStop() {
    super.onStop();
    EasyTracker.getTracker().trackActivityStop(this);
  }

}
