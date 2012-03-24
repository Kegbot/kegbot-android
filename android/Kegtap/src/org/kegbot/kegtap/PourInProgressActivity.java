package org.kegbot.kegtap;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

import org.kegbot.core.Flow;
import org.kegbot.core.FlowManager;
import org.kegbot.kegtap.camera.CameraFragment;
import org.kegbot.kegtap.util.PreferenceHelper;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class PourInProgressActivity extends CoreActivity {

  private static final String TAG = PourInProgressActivity.class.getSimpleName();

  private static final long FLOW_UPDATE_MILLIS = 500;

  private static final long FLOW_FINISH_DELAY_MILLIS = 5000;

  private static final int DIALOG_IDLE_WARNING = 1;

  private final FlowManager mFlowManager = FlowManager.getSingletonInstance();

  private int mPictureSeconds = 0;

  private CameraFragment mCameraFragment;
  private PourStatusFragment mPourStatus;

  private Button mPictureButton;

  private Button mEndPourButton;

  private AlertDialog mIdleDetectedDialog;

  private final Handler mHandler = new Handler();

  private PreferenceHelper mPrefs;

  private static final boolean DEBUG = false;

  private static final IntentFilter POUR_INTENT_FILTER = new IntentFilter(
      KegtapBroadcast.ACTION_POUR_START);
  static {
    POUR_INTENT_FILTER.addAction(KegtapBroadcast.ACTION_POUR_UPDATE);
    POUR_INTENT_FILTER.setPriority(100);
  }

  private final BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      if (KegtapBroadcast.ACTION_POUR_UPDATE.equals(action)
          || KegtapBroadcast.ACTION_POUR_START.equals(action)) {
        handleIntent(intent);
        abortBroadcast();
      }
    }
  };

  private final Runnable FLOW_UPDATE_RUNNABLE = new Runnable() {
    @Override
    public void run() {
      refreshFlows();
    }
  };

  private final Runnable FINISH_ACTIVITY_RUNNABLE = new Runnable() {
    @Override
    public void run() {
      cancelIdleWarning();
      finish();
    }
  };

  private final Runnable PICTURE_COUNTDOWN_RUNNABLE = new Runnable() {
    @Override
    public void run() {
      if (mPictureSeconds > 0) {
        mPictureButton.setClickable(false);
        mPictureButton.setText(mPictureSeconds + " ...");
        mPictureSeconds -= 1;
        mHandler.postDelayed(PICTURE_COUNTDOWN_RUNNABLE, 1000);
      } else {
        takePicture();
        mPictureButton.setClickable(true);
        mPictureButton.setText("Take Picture");
      }
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final ActionBar actionBar = getActionBar();
    if (actionBar != null) {
      actionBar.hide();
    }
    setContentView(R.layout.pour_in_progress_activity);
    bindToCoreService();

    findViewById(R.id.pourInProgressRightCol).setBackgroundDrawable(
        getResources().getDrawable(R.drawable.shape_rounded_rect));

    mPourStatus = (PourStatusFragment) getSupportFragmentManager()
        .findFragmentById(R.id.tap_status);
    mCameraFragment = (CameraFragment) getSupportFragmentManager().findFragmentById(R.id.camera);

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage("Hey, are you still pouring?").setCancelable(false).setPositiveButton(
        "Continue Pouring", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            for (final Flow flow : mFlowManager.getAllActiveFlows()) {
              flow.pokeActivity();
            }
            dialog.cancel();
          }
        }).setNegativeButton("Done Pouring", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        for (final Flow flow : mFlowManager.getAllActiveFlows()) {
          mFlowManager.endFlow(flow);
        }
        dialog.cancel();
      }
    });

    mIdleDetectedDialog = builder.create();

    // Attach camera click button.
    mPictureButton = ((Button) findViewById(R.id.takePictureButton));
    mPictureButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        schedulePicture();
      }
    });

    mPrefs = new PreferenceHelper(PreferenceManager.getDefaultSharedPreferences(this));
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    if (id == DIALOG_IDLE_WARNING) {
      return mIdleDetectedDialog;
    }
    return super.onCreateDialog(id);
  }

  @Override
  public void onStart() {
    super.onStart();
  }

  @Override
  protected void onResume() {
    super.onResume();
    registerReceiver(mUpdateReceiver, POUR_INTENT_FILTER);
    handleIntent(getIntent());
    schedulePicture();
  }

  @Override
  protected void onPause() {
    mHandler.removeCallbacks(FLOW_UPDATE_RUNNABLE);
    unregisterReceiver(mUpdateReceiver);
    super.onPause();
  }

  @Override
  public void onStop() {
    super.onStop();
  }

  @Override
  public void onBackPressed() {
    if (!mFlowManager.getAllActiveFlows().isEmpty()) {
      endAllFlows();
    } else {
      super.onBackPressed();
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    handleIntent(intent);
  }

  private void handleIntent(final Intent intent) {
    final String action = intent.getAction();
    Log.d(TAG, "Handling intent: " + intent);

    if (KegtapBroadcast.ACTION_POUR_UPDATE.equals(action)
        || KegtapBroadcast.ACTION_POUR_START.equals(action)) {
      refreshFlows();
    }

    if (KegtapBroadcast.ACTION_POUR_START.equals(action)) {
      // TODO(mikey): Do this safely.
      // schedulePicture();
    }
  }

  private void schedulePicture() {
    mPictureSeconds = 3;
    mHandler.removeCallbacks(PICTURE_COUNTDOWN_RUNNABLE);
    mHandler.post(PICTURE_COUNTDOWN_RUNNABLE);
  }

  private void refreshFlows() {
    mHandler.removeCallbacks(FLOW_UPDATE_RUNNABLE);

    final Collection<Flow> allFlows = mFlowManager.getAllActiveFlows();

    // Fetch all flows.
    long largestIdleTime = Long.MIN_VALUE;

    for (final Flow flow : allFlows) {
      if (DEBUG) Log.d(TAG, "Refreshing with flow: " + flow);
      if (flow.getState() == Flow.State.ACTIVE) {
        // Consider idle time (for warning) only for non-zero flows.
        if (flow.getTicks() > 0) {
          final long idleTimeMs = flow.getIdleTimeMs();
          if (idleTimeMs > largestIdleTime) {
            largestIdleTime = idleTimeMs;
          }
        }
      }
    }

    mPourStatus.updateWithFlows(allFlows);

    if (largestIdleTime >= mPrefs.getIdleWarningMs()) {
      sendIdleWarning();
    } else {
      cancelIdleWarning();
    }

    if (!allFlows.isEmpty()) {
      mHandler.removeCallbacks(FINISH_ACTIVITY_RUNNABLE);
      mHandler.postDelayed(FLOW_UPDATE_RUNNABLE, FLOW_UPDATE_MILLIS);
    } else {
      cancelIdleWarning();
      mHandler.postDelayed(FINISH_ACTIVITY_RUNNABLE, FLOW_FINISH_DELAY_MILLIS);
    }
  }

  private void endAllFlows() {
    for (final Flow flow : mFlowManager.getAllActiveFlows()) {
      mFlowManager.endFlow(flow);
    }
  }

  private void sendIdleWarning() {
    if (mIdleDetectedDialog.isShowing()) {
      return;
    }
    showDialog(DIALOG_IDLE_WARNING);
  }

  private void cancelIdleWarning() {
    if (!mIdleDetectedDialog.isShowing()) {
      return;
    }
    dismissDialog(DIALOG_IDLE_WARNING);
  }

  private void takePicture() {
    final ShutterCallback shutter = new ShutterCallback() {
      @Override
      public void onShutter() {
        Log.d(TAG, "camera: shutter");
      }
    };

    final PictureCallback raw = new PictureCallback() {
      @Override
      public void onPictureTaken(byte[] data, Camera camera) {
        Log.d(TAG, "camera: raw");
      }
    };

    final PictureCallback jpeg = new PictureCallback() {
      @Override
      public void onPictureTaken(byte[] data, Camera camera) {
        camera.startPreview();
        Log.d(TAG, "camera: jpeg");
        doSaveJpeg(data);
      }
    };

    mCameraFragment.takePicture(shutter, raw, jpeg);
  }

  private void doSaveJpeg(final byte[] data) {
    new ImageSaveTask().execute(data);
  }

  class ImageSaveTask extends AsyncTask<byte[], Void, Void> {

    @Override
    protected Void doInBackground(byte[]... params) {
      for (final Flow flow : mFlowManager.getAllActiveFlows()) {
        if (flow == null) {
          Log.w(TAG, "ImageSaveTask for empty flow.");
          return null;
        }
        final byte[] rawJpegData = params[0];
        final File imageDir = getDir("pour-images", MODE_WORLD_READABLE);
        final String baseName = "pour-" + flow.getFlowId();

        File imageFile = new File(imageDir, baseName + ".jpg");
        imageFile.setReadable(true, false);
        int ext = 2;
        while (imageFile.exists()) {
          imageFile = new File(imageDir, baseName + "-" + (ext++) + ".jpg");
          imageFile.setReadable(true, false);
        }

        // Bitmap imageBitmap = BitmapFactory.decodeByteArray(rawJpegData, 0,
        // rawJpegData.length);

        try {
          FileOutputStream fos = new FileOutputStream(imageFile);
          BufferedOutputStream bos = new BufferedOutputStream(fos);
          bos.write(rawJpegData);
          bos.flush();
          bos.close();
        } catch (IOException e) {
          Log.e(TAG, "Could not save image.", e);
          return null;
        }

        final String savedImage = imageFile.getAbsolutePath();
        Log.i(TAG, "Saved pour image: " + savedImage);
        flow.addImage(savedImage);
      }
      return null;
    }

  }

  public static Intent getStartIntent(Context context, final String tapName) {
    final Intent intent = new Intent(context, PourInProgressActivity.class);
    intent.setAction(KegtapBroadcast.ACTION_POUR_START);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    intent.putExtra(KegtapBroadcast.POUR_UPDATE_EXTRA_TAP_NAME, tapName);
    return intent;
  }

}
