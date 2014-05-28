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
    private TextView mPairingCodeText;
    private TextView mPairingCompleteText;

    private AsyncTask<Void, Void, String> mPollTask;
    private String mPairingCode;

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
      mPairingCodeText = ButterKnife.findById(view, R.id.pairingCode);
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
      final KegbotApiImpl api = KegbotApiImpl.fromContext(getActivity());

      mQuit.set(false);
      mPollTask = new AsyncTask<Void, Void, String>() {
        private Exception mError;

        @Override
        protected String doInBackground(Void... voids) {
          while (!mQuit.get()) {
            if (Strings.isNullOrEmpty(mPairingCode)) {
              try {
                mPairingCode = api.startDeviceLink(Build.MODEL);
                mHandler.post(new Runnable() {
                  @Override
                  public void run() {
                    mPairingCodeText.setText(mPairingCode);
                  }
                });
              } catch (KegbotApiException e) {
                mError = e;
                return "";
              }
            }

            while (!mQuit.get()) {
              try {
                Log.d(TAG, "Checking pairing status ...");
                final String apiKey = api.pollDeviceLink(mPairingCode);
                if (!Strings.isNullOrEmpty(apiKey)) {
                  return apiKey;
                }
              } catch (KegbotApiException e) {
                Log.w(TAG, "Error checking pairing code: " + e, e);
                mError = e;
                mPairingCode = "";  // fetch a new code next time
                break;
              }
              SystemClock.sleep(1000);
            }

            Log.w(TAG, "Pairing aborted unexpectedly, retrying in 1s.");
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
          if (Strings.isNullOrEmpty(s)) {
            // We quit.
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
      mPairingCodeText.setVisibility(View.VISIBLE);

      mPairingCompleteText.setVisibility(View.GONE);
    }

    private void onPairingComplete() {
      mState.setNextButtonEnabled(true);

      mPairingPrompt.setVisibility(View.GONE);
      mPairingUrl.setVisibility(View.GONE);
      mPairingPromptContinued.setVisibility(View.GONE);
      mPairingCodeText.setVisibility(View.GONE);

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
