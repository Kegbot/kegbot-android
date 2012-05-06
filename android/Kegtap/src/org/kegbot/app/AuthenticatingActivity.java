/**
 *
 */
package org.kegbot.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class AuthenticatingActivity extends Activity {

  private static final long AUTO_FINISH_DELAY_MILLIS = 5000;
  private TextView mBeginTitle;
  private TextView mFailTitle;
  private TextView mMessage;
  private ProgressBar mProgressBar;

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
    if (KegtapBroadcast.ACTION_AUTH_FAIL.equals(action)) {
      setFail();
    } else if (KegtapBroadcast.ACTION_AUTH_BEGIN.equals(action)) {
      setAuthenticating();
    } else if (KegtapBroadcast.ACTION_USER_AUTHED.equals(action)) {
      finish();
    }
  }

  private void setAuthenticating() {
    mBeginTitle.setVisibility(View.VISIBLE);
    mFailTitle.setVisibility(View.GONE);
    mMessage.setVisibility(View.GONE);
    mProgressBar.setVisibility(View.VISIBLE);

    mHandler.removeCallbacks(mFinishRunnable);
  }

  private void setFail() {
    mBeginTitle.setVisibility(View.GONE);
    mFailTitle.setVisibility(View.VISIBLE);
    mProgressBar.setVisibility(View.GONE);

    final Intent intent = getIntent();
    if (intent.hasExtra(KegtapBroadcast.AUTH_FAIL_EXTRA_MESSAGE)) {
      mMessage.setText(intent.getStringExtra(KegtapBroadcast.AUTH_FAIL_EXTRA_MESSAGE));
      Log.d("AUTH", mMessage.getText().toString());
      mMessage.setVisibility(View.VISIBLE);
    } else {
      mMessage.setVisibility(View.GONE);
    }
    mHandler.postDelayed(mFinishRunnable, AUTO_FINISH_DELAY_MILLIS);
  }

}
