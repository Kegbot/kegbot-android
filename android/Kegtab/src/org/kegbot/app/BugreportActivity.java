/**
 *
 */
package org.kegbot.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.kegbot.core.KegbotCore;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.base.Strings;

/**
 * Bug report activity.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class BugreportActivity extends Activity {

  private static final String TAG = BugreportActivity.class.getSimpleName();

  private static final int INPUT_BUFFER_SIZE = 1024 * 64;

  private KegbotCore mCore;

  private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
  private ProgressBar mProgressBar;
  private TextView mMessageText;
  private TextView mDetailText;
  private String mBugreportFilename = "";
  private String mBugDate;

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

    if (!Strings.isNullOrEmpty(mBugreportFilename)) {
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
        new BugreportAsyncTask().executeOnExecutor(mExecutor, (Void) null);
        showRunning();
      }
    });

    mMessageText.setText(R.string.bugreport_message_idle);
    mDetailText.setVisibility(View.GONE);
    mProgressBar.setVisibility(View.INVISIBLE);
  }

  private void showRunning() {
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
        launchMailer();
      }
    });
    mMessageText.setText(R.string.bugreport_message_ready);

    mProgressBar.setVisibility(View.INVISIBLE);
    mDetailText.setText("");
    mDetailText.setVisibility(View.VISIBLE);
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

  private void launchMailer() {
    PackageManager pm = getPackageManager();
    PackageInfo packageInfo;
    try {
      packageInfo = pm.getPackageInfo(getPackageName(), 0);
    } catch (NameNotFoundException e) {
      showError("Fatal error getting package info: " + e);
      return;
    }

    String messageText = mDetailText.getText().toString();
    if (Strings.isNullOrEmpty(messageText)) {
      messageText = "[No detail was entered.]";
    }
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.setType("text/plain");
    intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"bugreport@kegbot.org"});
    intent.putExtra(Intent.EXTRA_SUBJECT, String.format("Kegtab %s bug report (%s)",
        packageInfo.versionName, mBugDate));
    intent.putExtra(Intent.EXTRA_TEXT, messageText);
    intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + mBugreportFilename));

    try {
      BugreportActivity.this.startActivity(intent);
      finish();
    } catch (ActivityNotFoundException e) {
      showError("Could not start your mail program.  Bugreport path: " + mBugreportFilename);
    }
    mBugreportFilename = "";
  }

  private String takeBugreport() {
    File bugreportDir = getExternalFilesDir(null);
    mBugDate = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());

    File bugreportFile = new File(bugreportDir, "kegbot-bugreport-" + mBugDate + ".txt");

    bugreportDir.mkdirs();

    FileOutputStream out;
    try {
      out = new FileOutputStream(bugreportFile);
    } catch (FileNotFoundException e) {
      showError("Could not create file " + bugreportFile.getAbsolutePath() + ", error: " + e);
      return "";
    }

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

    final String path = bugreportFile.getAbsolutePath();
    Log.i(TAG, "Created bug report: " + path);
    return path;
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

  private class BugreportAsyncTask extends AsyncTask<Void, Void, String> {
    @Override
    protected String doInBackground(Void... params) {
      return takeBugreport();
    }

    @Override
    protected void onCancelled() {
      super.onCancelled();
    }

    @Override
    protected void onPostExecute(String filename) {
      mProgressBar.setIndeterminate(false);
      mProgressBar.setProgress(100);

      mBugreportFilename = filename;

      if (Strings.isNullOrEmpty(mBugreportFilename)) {
        // Presume we called showError() somewhere.
        return;
      }

      showBugreportReady();
    }

  }

  public static void startBugreportActivity(Context context) {
    Intent intent = new Intent(context, BugreportActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK);
    PinActivity.startThroughPinActivity(context, intent);
  }

}
