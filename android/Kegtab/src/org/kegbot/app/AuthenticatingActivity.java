/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
 *
 * This file is part of the Kegtab package from the Kegbot project. For more
 * information on Kegtab or Kegbot, see <http://kegbot.org/>.
 *
 * Kegtab is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2.
 *
 * Kegtab is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Kegtab. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kegbot.app;

import org.kegbot.api.KegbotApiException;
import org.kegbot.core.KegbotCore;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.base.Strings;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class AuthenticatingActivity extends Activity {

  private static final String TAG = AuthenticatingActivity.class.getSimpleName();

  private static final long AUTO_FINISH_DELAY_MILLIS = 10000;
  private static final int REQUEST_SELECT_USER_TO_BIND = 100;

  private KegbotCore mCore;

  private ViewGroup mButtonGroup;
  private TextView mBeginTitle;
  private TextView mFailTitle;
  private TextView mMessage;
  private ProgressBar mProgressBar;

  private Button mCancelButton;
  private Button mAssignButton;

  private final Handler mHandler = new Handler();

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
    setContentView(R.layout.authenticating_activity);

    mCore = KegbotCore.getInstance(this);

    mBeginTitle = (TextView) findViewById(R.id.authenticatingBeginTitle);
    mFailTitle = (TextView) findViewById(R.id.authenticatingFailTitle);
    mMessage = (TextView) findViewById(R.id.authenticatingMessage);
    mProgressBar = (ProgressBar) findViewById(R.id.authenticatingProgress);
    mButtonGroup = (ViewGroup) findViewById(R.id.buttonGroup);

    mCancelButton = (Button) findViewById(R.id.cancelButton);
    mCancelButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
      }
    });

    mAssignButton = (Button) findViewById(R.id.assignButton);
    mAssignButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        final Intent intent = KegtabCommon
            .getAuthDrinkerActivityIntent(AuthenticatingActivity.this);
        Log.d(TAG, "Starting auth drinker activity");
        startActivityForResult(intent, REQUEST_SELECT_USER_TO_BIND);
      }
    });

    // getWindow().setBackgroundDrawable(new ColorDrawable(0));

    handleIntent();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    handleIntent();
  }

  private void handleIntent() {
    final Intent intent = getIntent();
    final String action = intent.getAction();
    if (KegtabBroadcast.ACTION_AUTH_FAIL.equals(action)) {
      setFail();
    } else if (KegtabBroadcast.ACTION_AUTH_BEGIN.equals(action)) {
      setAuthenticating();
    } else if (KegtabBroadcast.ACTION_USER_AUTHED.equals(action)) {
      finish();
    }
  }

  private void setAuthenticating() {
    mBeginTitle.setVisibility(View.VISIBLE);
    mFailTitle.setVisibility(View.GONE);
    mButtonGroup.setVisibility(View.GONE);
    mMessage.setVisibility(View.GONE);
    mProgressBar.setVisibility(View.VISIBLE);

    mHandler.removeCallbacks(mFinishRunnable);
  }

  private void setFail() {
    mBeginTitle.setVisibility(View.GONE);
    mFailTitle.setVisibility(View.VISIBLE);
    mProgressBar.setVisibility(View.GONE);
    mButtonGroup.setVisibility(View.VISIBLE);

    final Intent intent = getIntent();
    if (intent.hasExtra(KegtabBroadcast.AUTH_FAIL_EXTRA_MESSAGE)) {
      mMessage.setText(intent.getStringExtra(KegtabBroadcast.AUTH_FAIL_EXTRA_MESSAGE));
      Log.d("AUTH", mMessage.getText().toString());
      mMessage.setVisibility(View.VISIBLE);
    } else {
      mMessage.setVisibility(View.GONE);
    }
    mHandler.postDelayed(mFinishRunnable, AUTO_FINISH_DELAY_MILLIS);
  }

  @Override
  protected void onPause() {
    super.onPause();
    mHandler.removeCallbacks(mFinishRunnable);
  }

  private void bindUserAsync(final String username) {
    final String authDevice = getIntent().getStringExtra(
        KegtabBroadcast.AUTH_FAIL_EXTRA_AUTH_DEVICE);
    final String tokenValue = getIntent().getStringExtra(
        KegtabBroadcast.AUTH_FAIL_EXTRA_TOKEN_VALUE);

    setAuthenticating();

    new AsyncTask<Void, Void, Boolean>() {
      @Override
      protected Boolean doInBackground(Void... params) {
        Log.d(TAG, "Assigning authDevice=" + authDevice + " tokenValue=" + tokenValue
            + "to username=" + username);
        try {
          mCore.getApi().assignToken(authDevice, tokenValue, username);
        } catch (KegbotApiException e) {
          Log.w(TAG, "Assignment failed!", e);
          return Boolean.FALSE;
        }
        mCore.getAuthenticationManager().authenticateUsernameAsync(username);
        finish();
        return Boolean.TRUE;
      }

      @Override
      protected void onPostExecute(Boolean result) {
        if (result == Boolean.TRUE) {
          setAuthenticating();
        } else {
          setFail();
        }
      }


    }.execute();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.d(TAG, "onActivityResult");
    switch (requestCode) {
      case REQUEST_SELECT_USER_TO_BIND:
        if (resultCode == RESULT_OK && data != null) {
          final String username = data
              .getStringExtra(KegtabCommon.ACTIVITY_AUTH_DRINKER_RESULT_EXTRA_USERNAME);
          if (!Strings.isNullOrEmpty(username)) {
            bindUserAsync(username);
          }
        }
        break;
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

}
