package org.kegbot.app.setup;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.app.KegbotApplication;
import org.kegbot.app.R;
import org.kegbot.app.config.AppConfiguration;

import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.ButterKnife;

public class SetupPairStep extends SetupStep {

  private final String TAG = SetupPairStep.class.getSimpleName();

  private final Fragment mContentFragment = new Fragment() {

    private final AtomicBoolean mQuit = new AtomicBoolean();
    private TextView mPairingPrompt;
    private TextView mPairingUrl;
    private TextView mPairingPromptContinued;
    private TextView mPairingCode;
    private TextView mPairingCompleteText;

    private AsyncTask<Void, Void, String> mPollTask;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View view = inflater.inflate(R.layout.setup_pair_fragment, null);
      return view;
    }

    @Override
    public void onStart() {
      super.onStart();

      final View view = getView();

      mPairingPrompt = ButterKnife.findById(view, R.id.pairingPleaseVisit);
      mPairingUrl = ButterKnife.findById(view, R.id.pairingUrl);
      mPairingPromptContinued = ButterKnife.findById(view, R.id.pairingAndEnter);
      mPairingCode = ButterKnife.findById(view, R.id.pairingCode);
      mPairingCompleteText = ButterKnife.findById(view, R.id.pairingComplete);

      onPairingStarted();
      startPolling();

      final AppConfiguration config = KegbotApplication.get(getActivity()).getConfig();

      final String baseUrl = KegbotApplication.get(getActivity()).getConfig().getKegbotUrl();
      final String addUrl = baseUrl + "/link";

      mPairingUrl.setText(addUrl);
      Linkify.addLinks(mPairingUrl, Linkify.ALL);
    }

    @Override
    public void onStop() {
      super.onStop();
      stopPolling();
    }

    private void startPolling() {
      Preconditions.checkState(mPollTask == null, "Already running");

      final AppConfiguration config = KegbotApplication.get(getActivity()).getConfig();
      final KegbotApiImpl api = new KegbotApiImpl(config);

      mQuit.set(false);
      mPollTask = new AsyncTask<Void, Void, String>() {
        private Exception mError;
        @Override
        protected String doInBackground(Void... voids) {
          final String pairingCode;
          try {
            pairingCode = api.startDeviceLink(Build.MODEL);
            mHandler.post(new Runnable() {
              @Override
              public void run() {
                mPairingCode.setText(pairingCode);
              }
            });
          } catch (KegbotApiException e) {
            mError = e;
            return "";
          }

          while (!mQuit.get()) {
            try {
              Log.d(TAG, "Checking pairing status ...");
              final String apiKey = api.pollDeviceLink(pairingCode);
              if (!Strings.isNullOrEmpty(apiKey)) {
                return apiKey;
              }
            } catch (KegbotApiException e) {
              Log.w(TAG, "Error checking pairing code: " + e, e);
              mError = e;
              return "";
            }
            SystemClock.sleep(1000);
          }
          return null;
        }

        @Override
        protected void onPostExecute(String s) {
          if (mError != null) {
            onPairingFailed(mError);
            return;
          }
          config.setApiKey(s);
          onPairingComplete();
        }

      };

      mPollTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void stopPolling() {
      Preconditions.checkState(mPollTask != null, "Not running.");
      mQuit.set(true);
      mPollTask = null;
    }

    private void onPairingStarted() {
      mState.setNextButtonEnabled(false);

      mPairingPrompt.setVisibility(View.VISIBLE);
      mPairingUrl.setVisibility(View.VISIBLE);
      mPairingPromptContinued.setVisibility(View.VISIBLE);
      mPairingCode.setVisibility(View.VISIBLE);

      mPairingCompleteText.setVisibility(View.GONE);
    }

    private void onPairingComplete() {
      mState.setNextButtonEnabled(true);

      mPairingPrompt.setVisibility(View.GONE);
      mPairingUrl.setVisibility(View.GONE);
      mPairingPromptContinued.setVisibility(View.GONE);
      mPairingCode.setVisibility(View.GONE);

      mPairingCompleteText.setVisibility(View.VISIBLE);
    }

    private void onPairingFailed(Exception error) {
      if (error != null) {
        Log.e(TAG, "Failed to pair: " + error, error);
      } else {
        Log.e(TAG, "Pairing failed, error unknown");
      }
    }

  };

  public SetupPairStep(SetupActivity.SetupState state) {
    super(state);
  }

  @Override
  public Fragment getContentFragment() {
    return mContentFragment;
  }

  @Override
  public Fragment getControlsFragment() {
    return null;
  }

  @Override
  public SetupStep advance() throws SetupValidationException {
    final AppConfiguration config = KegbotApplication.get(mContentFragment.getActivity()).getConfig();
    return new SetupManagerPinStep(mState);
  }
}
