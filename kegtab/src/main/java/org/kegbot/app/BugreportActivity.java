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

/**
 *
 */
package org.kegbot.app;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.base.Strings;

import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.util.CheckinClient;
import org.kegbot.core.KegbotCore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.ButterKnife;

/**
 * Bug report activity.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class BugreportActivity extends Activity {

  private static final String TAG = BugreportActivity.class.getSimpleName();

  private static final int INPUT_BUFFER_SIZE = 1024 * 64;

  private static final String BUGREPORT_FILENAME = "bugreport.txt";

  private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
  private AppConfiguration mConfig;

  private ProgressBar mProgressBar;
  private TextView mMessageText;
  private TextView mDetailText;
  private EditText mEmailText;
  private File mBugreportFile;

  private Button mButton;

  private final Handler mHandler = new Handler(Looper.getMainLooper());

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.bugreport_activity);

    mConfig = ((KegbotApplication) getApplicationContext()).getConfig();

    mButton = ButterKnife.findById(this, R.id.bugreportButton);
    mProgressBar = ButterKnife.findById(this, R.id.bugreportProgress);
    mProgressBar.setIndeterminate(true);
    mMessageText = ButterKnife.findById(this, R.id.bugreportMessage);
    mDetailText = ButterKnife.findById(this, R.id.bugreportDetail);
    mEmailText = ButterKnife.findById(this, R.id.bugreportEmail);
  }

  @Override
  protected void onStart() {
    super.onStart();

    if (mBugreportFile != null) {
      showBugreportReady();
    } else {
      showIdle();
    }

    final ActionBar bar = getActionBar();
    if (bar != null) {
      bar.setTitle("");
    }

    final String email = mConfig.getEmailAddress();
    if (!Strings.isNullOrEmpty(email)) {
      mEmailText.setText(email);
    }
  }

  private void showIdle() {
    mButton.setEnabled(true);
    mButton.setText(R.string.bugreport_button_idle);

    mButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        collectBugreport();
      }
    });

    mMessageText.setText(R.string.bugreport_message_idle);
    mDetailText.setVisibility(View.GONE);
    mProgressBar.setVisibility(View.INVISIBLE);
    mEmailText.setVisibility(View.GONE);
  }

  private void showCollectingBugreport() {
    mButton.setEnabled(false);
    mMessageText.setText(R.string.bugreport_message_running);
    mDetailText.setVisibility(View.GONE);
    mEmailText.setVisibility(View.GONE);
    mProgressBar.setVisibility(View.VISIBLE);
  }

  private void showBugreportReady() {
    mButton.setEnabled(true);
    mButton.setText(R.string.bugreport_button_ready);
    mButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        submitBugreport();
      }
    });
    mMessageText.setEnabled(true);
    mMessageText.setText(R.string.bugreport_message_ready);
    mButton.setEnabled(true);

    mProgressBar.setVisibility(View.INVISIBLE);
    mDetailText.setText("");
    mDetailText.setVisibility(View.VISIBLE);
    mDetailText.setEnabled(true);
    mEmailText.setVisibility(View.VISIBLE);
    mEmailText.setEnabled(true);
  }

  private void showSubmittingBugreport() {
    mButton.setEnabled(false);
    mMessageText.setText(R.string.bugreport_message_running);
    mDetailText.setEnabled(false);
    mEmailText.setEnabled(false);
    mProgressBar.setVisibility(View.VISIBLE);
    mProgressBar.setIndeterminate(true);
  }

  private void showFinished() {
    mButton.setEnabled(false);
    mMessageText.setText(R.string.bugreport_message_submitted);
    mDetailText.setEnabled(false);
    mProgressBar.setVisibility(View.VISIBLE);
    mProgressBar.setProgress(100);
    mProgressBar.setIndeterminate(false);

    mHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        finish();
      }
    }, 3000);
  }

  private void showError(final String errorMessage) {
    mHandler.post(new Runnable() {
      @Override
      public void run() {
        Log.e(TAG, errorMessage);
        showIdle();

        mMessageText.setText(errorMessage);
      }
    });
  }

  private void collectBugreport() {
    showCollectingBugreport();
    new CollectBugreportAsyncTask().executeOnExecutor(mExecutor, (Void) null);
  }

  private void submitBugreport() {
    showSubmittingBugreport();
    new SubmitBugreportAsyncTask().execute(mBugreportFile);
  }

  private class CollectBugreportAsyncTask extends AsyncTask<Void, Void, File> {
    @Override
    protected File doInBackground(Void... params) {
      final FileOutputStream out;
      try {
        out = openFileOutput(BUGREPORT_FILENAME, MODE_PRIVATE);
      } catch (FileNotFoundException e) {
        showError("Could not create bugreport file, error: " + e);
        return null;
      }

      try {
        PrintWriter writer = new PrintWriter(out);
        writer.println("Kegbot bugreport.");
        writer.println();

        writer.println("--- Kegbot core ---");
        writer.println();
        writer.flush();

        final KegbotCore core = KegbotCore.getRunningInstance(BugreportActivity.this);
        if (core == null) {
          writer.println("Not running.");
          writer.println();
        } else {
          core.dump(writer);
        }

        writer.println("--- System logs ---");
        writer.println();
        writer.flush();

        try {
          getDeviceLogs(out);
          writer.flush();
        } catch (IOException e) {
          showError("Error grabbing bugreport: " + e);
        }

        writer.println("--- (end of bugreport) ---");
        writer.flush();
        writer.close();
      } finally {
        try {
          out.close();
        } catch (IOException e) {
          // Ignore.
        }
      }
      return getFileStreamPath(BUGREPORT_FILENAME);
    }

    @Override
    protected void onCancelled() {
      super.onCancelled();
    }

    @Override
    protected void onPostExecute(File file) {
      mBugreportFile = file;
      if (mBugreportFile == null) {
        // Presume we called showError() somewhere.
        return;
      }
      showBugreportReady();
    }

  }

  private class SubmitBugreportAsyncTask extends AsyncTask<File, Void, Exception> {
    @Override
    protected Exception doInBackground(File... params) {
      final String messageText = Strings.nullToEmpty(mDetailText.getText().toString());
      final CheckinClient client = CheckinClient.fromContext(getApplicationContext());
      final String email = mEmailText.getText().toString();
      if (!Strings.isNullOrEmpty(email)) {
        mConfig.setEmailAddress(email);
      }
      try {
        client.submitBugreport(messageText, mBugreportFile, email);
      } catch (Exception e) {
        return e;
      }
      return null;
    }

    @Override
    protected void onPostExecute(Exception result) {
      if (result == null) {
        showFinished();
      } else {
        showError("Error submitting bugreport: " + result.getMessage());
      }
    }
  }

  private static void getDeviceLogs(OutputStream outputStream) throws IOException {
    final String command = "logcat -v time -d";
    Log.d(TAG, "Running command: " + command);
    final Process process = Runtime.getRuntime().exec(command);
    final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process
        .getInputStream()), INPUT_BUFFER_SIZE);

    while (true) {
      final String line = bufferedReader.readLine();
      if (line == null) {
        break;
      }
      outputStream.write(line.getBytes());
      outputStream.write('\n');
    }
  }

  public static void startBugreportActivity(Context context) {
    Intent intent = new Intent(context, BugreportActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK);
    PinActivity.startThroughPinActivity(context, intent);
  }

}
