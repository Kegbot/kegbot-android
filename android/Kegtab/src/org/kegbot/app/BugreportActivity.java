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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.base.Strings;

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

/**
 * Bug report activity.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class BugreportActivity extends Activity {

  private static final String TAG = BugreportActivity.class.getSimpleName();

  private static final int INPUT_BUFFER_SIZE = 1024 * 64;

  private static final String BUGREPORT_FILENAME = "bugreport.txt";

  private KegbotCore mCore;

  private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
  private ProgressBar mProgressBar;
  private TextView mMessageText;
  private TextView mDetailText;
  private File mBugreportFile;

  private Button mButton;

  private final Handler mHandler = new Handler();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.bugreport_activity);

    mButton = (Button) findViewById(R.id.bugreportButton);
    mProgressBar = (ProgressBar) findViewById(R.id.bugreportProgress);
    mProgressBar.setIndeterminate(true);
    mMessageText = (TextView) findViewById(R.id.bugreportMessage);
    mDetailText = (TextView) findViewById(R.id.bugreportDetail);

    mCore = KegbotCore.getInstance(this);
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
  }

  private void showCollectingBugreport() {
    mButton.setEnabled(false);
    mMessageText.setText(R.string.bugreport_message_running);
    mDetailText.setVisibility(View.GONE);
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
  }

  private void showSubmittingBugreport() {
    mButton.setEnabled(false);
    mMessageText.setText(R.string.bugreport_message_running);
    mDetailText.setEnabled(false);
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

        mMessageText.setText("Error: " + errorMessage);
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

        mCore.dump(writer);

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

  private class SubmitBugreportAsyncTask extends AsyncTask<File, Void, IOException> {
    @Override
    protected IOException doInBackground(File... params) {
      final String messageText = Strings.nullToEmpty(mDetailText.getText().toString());
      final CheckinClient client = CheckinClient.fromContext(getApplicationContext());
      try {
        client.submitBugreport(messageText, mBugreportFile);
      } catch (IOException e) {
        return e;
      }
      return null;
    }

    @Override
    protected void onPostExecute(IOException result) {
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
