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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.hoho.android.usbserial.util.HexDump;

import org.kegbot.app.config.AppConfiguration;
import org.kegbot.backend.BackendException;
import org.kegbot.core.AuthenticationManager;
import org.kegbot.core.AuthenticationToken;
import org.kegbot.core.FlowManager;
import org.kegbot.core.KegbotCore;
import org.kegbot.core.TapManager;
import org.kegbot.proto.Models.KegTap;
import org.kegbot.proto.Models.User;

/**
 * Activity shown while authenticating a user.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class AuthenticatingActivity extends Activity {

  private static final String TAG = AuthenticatingActivity.class.getSimpleName();

  private static final long AUTO_FINISH_DELAY_MILLIS = 10000;
  private static final int REQUEST_SELECT_USER_TO_BIND = 100;

  private static final String EXTRA_USERNAME = "username";
  private static final String EXTRA_AUTH_DEVICE = "auth_device";
  private static final String EXTRA_TOKEN_VALUE = "token";
  private static final String EXTRA_TAP_ID = "tap";

  private AppConfiguration mConfig;

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

    // getWindow().setBackgroundDrawable(new ColorDrawable(0));

    handleIntent();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mConfig = ((KegbotApplication) getApplicationContext()).getConfig();
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

    setAuthenticating();
    if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {
      Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
      byte[] id = tag.getId();
      if (id != null && id.length > 0) {
        String tagId = HexDump.toHexString(id).toLowerCase();
        Log.d(TAG, "Read NFC tag with id: " + tagId);
        // TODO: use tag technology as part of id?
        intent.putExtra(EXTRA_TOKEN_VALUE, tagId);  // needed by onActivityResult
        authenticateTokenAsync("nfc", tagId);
      } else {
        setFail("Unknown NFC tag.");
      }
    } else if (intent.hasExtra(EXTRA_USERNAME)) {
      authenticateUsernameAsync(intent.getStringExtra(EXTRA_USERNAME));
    } else if (intent.hasExtra(EXTRA_AUTH_DEVICE)) {
      authenticateTokenAsync(intent.getStringExtra(EXTRA_AUTH_DEVICE),
          intent.getStringExtra(EXTRA_TOKEN_VALUE));
    } else {
      Log.e(TAG, "Unknown start intent. Aborting.");
      finish();
    }
  }

  /**
   * Processes a launch request from {@link #startAndAuthenticate(android.content.Context, String, String)}.
   *
   * @param username
   */
  private void authenticateUsernameAsync(final String username) {
    new AsyncTask<Void, Void, User>() {
      @Override
      protected User doInBackground(Void... params) {
        final KegbotCore core = KegbotCore.getInstance(AuthenticatingActivity.this);
        return core.getAuthenticationManager().authenticateUsername(username);
      }

      @Override
      protected void onPostExecute(User user) {
        if (user == null) {
          Log.d(TAG, "Null result from auth manager.");
          setFail("User not found.");
        } else {
          Log.d(TAG, "Auth manager returned " + user.getUsername());
          activateUser(user.getUsername());
          finish();
        }
      }

    }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
  }

  private void authenticateTokenAsync(final String authDevice, final String tokenValue) {
    final AuthenticationToken authToken = new AuthenticationToken(authDevice, tokenValue);
    new AsyncTask<Void, Void, User>() {
      @Override
      protected User doInBackground(Void... params) {
        final KegbotCore core = KegbotCore.getInstance(AuthenticatingActivity.this);
        return core.getAuthenticationManager().authenticateToken(authToken);
      }

      @Override
      protected void onPostExecute(User user) {
        if (user == null) {
          Log.d(TAG, "Null result from auth manager.");
          setFail("User not found.");
        } else {
          Log.d(TAG, "Auth manager returned " + user.getUsername());
          activateUser(user.getUsername());
          finish();
        }
      }

    }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
  }

  private void activateUser(String username) {
    final int tapId = getIntent().getIntExtra(EXTRA_TAP_ID, 0);
    final KegbotCore core = KegbotCore.getInstance(this);

    if (tapId > 0) {
      final KegTap tap = core.getTapManager().getTap(tapId);
      core.getFlowManager().activateUserAtTap(tap, username);
    } else {
      core.getFlowManager().activateUserAmbiguousTap(username);
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

  private void setFail(String message) {
    mBeginTitle.setVisibility(View.GONE);
    mFailTitle.setVisibility(View.VISIBLE);
    mProgressBar.setVisibility(View.GONE);
    mButtonGroup.setVisibility(View.VISIBLE);

    if (!Strings.isNullOrEmpty(message)) {
      mMessage.setText(message);
      Log.d(TAG, mMessage.getText().toString());
      mMessage.setVisibility(View.VISIBLE);
    } else {
      mMessage.setVisibility(View.GONE);
    }
    mHandler.postDelayed(mFinishRunnable, AUTO_FINISH_DELAY_MILLIS);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (mConfig.getAllowRegistration()) {
      mAssignButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          final Intent intent = KegtabCommon
              .getAuthDrinkerActivityIntent(AuthenticatingActivity.this);
          Log.d(TAG, "Starting auth drinker activity");
          startActivityForResult(intent, REQUEST_SELECT_USER_TO_BIND);
        }
      });
    } else {
      mAssignButton.setVisibility(View.GONE);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    mHandler.removeCallbacks(mFinishRunnable);
  }

  private void assignUserToTokenAsync(final String username) {
    final String authDevice = getIntent().getStringExtra(EXTRA_AUTH_DEVICE);
    final String tokenValue = getIntent().getStringExtra(EXTRA_TOKEN_VALUE);
    Preconditions.checkNotNull(authDevice);
    Preconditions.checkNotNull(tokenValue);

    setAuthenticating();

    new AsyncTask<Void, Void, Boolean>() {
      @Override
      protected Boolean doInBackground(Void... params) {
        Log.d(TAG, "Assigning authDevice=" + authDevice + " tokenValue=" + tokenValue
            + "to username=" + username);
        final KegbotCore core = KegbotCore.getInstance(AuthenticatingActivity.this);
        try {
          core.getBackend().assignToken(authDevice, tokenValue, username);
        } catch (BackendException e) {
          Log.w(TAG, "Assignment failed!", e);
          return Boolean.FALSE;
        }
        Log.w(TAG, "Assignment succeeded, authenticating.");
        startAndAuthenticate(AuthenticatingActivity.this, username, (KegTap) null);
        return Boolean.TRUE;
      }

      @Override
      protected void onPostExecute(Boolean result) {
        if (result == Boolean.TRUE) {
          setAuthenticating();
        } else {
          setFail("Token assignment failed.");
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
            assignUserToTokenAsync(username);
          }
        }
        break;
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  /**
   * Starts the authentication activity for the supplied username. Upon
   * successful authentication, a new flow will be started.
   *
   * @param context
   * @param username
   *
   */
  public static void startAndAuthenticate(Context context, String username) {
    startAndAuthenticate(context, username, (KegTap) null);
  }

  /**
   * Starts the authentication activity for the supplied username. Upon
   * successful authentication, a new flow will be started.
   *
   * @param context
   * @param username
   * @param tap tap to authenticate against, or {@code null} if ambiguous
   *
   */
  public static void startAndAuthenticate(Context context, String username, KegTap tap) {
    final Intent intent = new Intent(context, AuthenticatingActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.putExtra(EXTRA_USERNAME, username);
    if (tap != null) {
      intent.putExtra(EXTRA_TAP_ID, Integer.valueOf(tap.getId()));
    }
    context.startActivity(intent);
  }

  /**
   * Starts the authentication activity for the supplied token. Upon successful
   * authentication, a new flow will be started.
   *
   * @param context
   * @param authDevice
   * @param tokenValue
   */
  public static void startAndAuthenticate(Context context, String authDevice, String tokenValue) {
    final Intent intent = new Intent(context, AuthenticatingActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.putExtra(EXTRA_AUTH_DEVICE, authDevice);
    intent.putExtra(EXTRA_TOKEN_VALUE, tokenValue);
    context.startActivity(intent);
  }

  public static Intent getStartForNfcIntent(Context context) {
    final Intent intent = new Intent(context, AuthenticatingActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.putExtra(EXTRA_AUTH_DEVICE, "nfc");
    return intent;
  }

}
