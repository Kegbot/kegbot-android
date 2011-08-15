package org.kegbot.kegtap;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.kegbot.core.Flow;
import org.kegbot.core.Flow.State;
import org.kegbot.core.FlowManager;
import org.kegbot.kegtap.camera.CameraFragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class PourInProgressActivity extends CoreActivity {

  public final String LOG_TAG = PourInProgressActivity.class.getSimpleName();

  private static final int MESSAGE_FLOW_UPDATE = 1;

  private static final int MESSAGE_FLOW_FINISH = 2;

  private static final int MESSAGE_PICTURE_COUNTDOWN = 3;

  private static final int MESSAGE_TAKE_PICTURE = 4;

  private static final long FLOW_UPDATE_MILLIS = 500;

  private static final long FLOW_FINISH_DELAY_MILLIS = 5000;

  private Flow mCurrentFlow = null;

  private CameraFragment mCameraFragment;
  private PourStatusFragment mPourStatus;

  private Button mPictureButton;

  private Button mLogInButton;
  private Button mEndPourButton;

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

  private final Handler mHandler = new Handler() {

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MESSAGE_FLOW_UPDATE:
          if (mCurrentFlow == null) {
            return;
          }
          Log.d(LOG_TAG, "!!!!!!! Flow update!" + mCurrentFlow);
          doFlowUpdate();

          if (mCurrentFlow.getState() != Flow.State.COMPLETED) {
            final Message message = mHandler.obtainMessage(MESSAGE_FLOW_UPDATE);
            mHandler.sendMessageDelayed(message, FLOW_UPDATE_MILLIS);
          } else {
            mHandler.sendEmptyMessageDelayed(MESSAGE_FLOW_FINISH, FLOW_FINISH_DELAY_MILLIS);
          }
          return;
        case MESSAGE_FLOW_FINISH:
          Log.d(LOG_TAG, "!!!!!!!!!!! Flow finished!");
          doFlowUpdate();
          finish();
          return;
        case MESSAGE_PICTURE_COUNTDOWN:
          mPictureButton.setClickable(false);
          mPictureButton.setEnabled(false);
          final Integer time = (Integer) msg.obj;
          mPictureButton.setText(time.toString() + " ...");
          return;
        case MESSAGE_TAKE_PICTURE:
          mPictureButton.setClickable(true);
          mPictureButton.setEnabled(true);
          mPictureButton.setText("Take Picture");
          takePicture();
          return;
      }
      super.handleMessage(msg);
    }

  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getActionBar().hide();
    setContentView(R.layout.pour_in_progress_activity);
    bindToCoreService();

    findViewById(R.id.pourInProgressRightCol).setBackgroundDrawable(
        getResources().getDrawable(R.drawable.shape_rounded_rect));

    mPourStatus = (PourStatusFragment) getFragmentManager().findFragmentById(R.id.tap_status);
    mCameraFragment = (CameraFragment) getFragmentManager().findFragmentById(R.id.camera);

    mPictureButton = (Button) findViewById(R.id.takePictureButton);
    mPictureButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        onTakePictureButton();
      }
    });

    mLogInButton = (Button) findViewById(R.id.authenticateButton);
    mLogInButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        onLogInButton();
      }
    });

    mEndPourButton = (Button) findViewById(R.id.endPourButton);
    mEndPourButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        onEndPourButton();
      }
    });

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
  }

  @Override
  protected void onPause() {
    mHandler.removeMessages(MESSAGE_FLOW_UPDATE);
    unregisterReceiver(mUpdateReceiver);
    super.onPause();
  }

  @Override
  public void onStop() {
    super.onStop();
  }

  @Override
  public void onBackPressed() {
    if (mCurrentFlow != null && mCurrentFlow.getState() != State.COMPLETED) {
      onEndPourButton();
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
    Log.d(LOG_TAG, "Handling intent: " + intent);

    if (KegtapBroadcast.ACTION_POUR_UPDATE.equals(action)
        || KegtapBroadcast.ACTION_POUR_START.equals(action)) {
      final long flowId = intent.getLongExtra(KegtapBroadcast.POUR_UPDATE_EXTRA_FLOW_ID, -1);
      if (flowId > 0) {
        Log.d(LOG_TAG, "Flow id: " + flowId);
        updateForFlow(flowId);
      }
    }

    if (KegtapBroadcast.ACTION_POUR_START.equals(action)) {
      // TODO(mikey): Do this safely.
      //schedulePicture();
    }
  }

  private void updateForFlow(long flowId) {
    final FlowManager flowManager = FlowManager.getSingletonInstance();
    final Flow flow = flowManager.getFlowForFlowId(flowId);
    mCurrentFlow = flow;
    doFlowUpdate();
    final Message updateMessage = mHandler.obtainMessage(MESSAGE_FLOW_UPDATE, flow);
    mHandler.removeMessages(MESSAGE_FLOW_UPDATE);
    mHandler.sendMessageDelayed(updateMessage, FLOW_UPDATE_MILLIS);
  }

  private void doFlowUpdate() {
    // Log.d(LOG_TAG, "Updating from flow: " + flow);
    if (mCurrentFlow != null) {
      mPourStatus.updateForFlow(mCurrentFlow);

      if (mCurrentFlow.isAnonymous()) {
        findViewById(R.id.authenticateButton).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.pourDrinkerName)).setText("Anonymous Drinker");
      } else {
        findViewById(R.id.authenticateButton).setVisibility(View.INVISIBLE);
        ((TextView) findViewById(R.id.pourDrinkerName)).setText(mCurrentFlow.getUsername());
      }
    }
  }

  private void schedulePicture() {
    mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_PICTURE_COUNTDOWN, Integer.valueOf(3)));
    mHandler.sendMessageDelayed(mHandler.obtainMessage(MESSAGE_PICTURE_COUNTDOWN, Integer
        .valueOf(2)), 1000);
    mHandler.sendMessageDelayed(mHandler.obtainMessage(MESSAGE_PICTURE_COUNTDOWN, Integer
        .valueOf(1)), 2000);
    mHandler.sendEmptyMessageDelayed(MESSAGE_TAKE_PICTURE, 3000);
  }

  private void onTakePictureButton() {
    schedulePicture();
  }

  private void onLogInButton() {
    final Intent intent = new Intent(this, DrinkerSelectActivity.class);
    startActivity(intent);
  }

  private void onEndPourButton() {
    mEndPourButton.setClickable(false);
    mEndPourButton.setEnabled(false);
    final FlowManager flowManager = FlowManager.getSingletonInstance();
    flowManager.endFlow(mCurrentFlow);
  }

  private void takePicture() {
    final ShutterCallback shutter = new ShutterCallback() {
      @Override
      public void onShutter() {
        Log.d(LOG_TAG, "camera: shutter");
      }
    };

    final PictureCallback raw = new PictureCallback() {
      @Override
      public void onPictureTaken(byte[] data, Camera camera) {
        Log.d(LOG_TAG, "camera: raw");
      }
    };

    final PictureCallback jpeg = new PictureCallback() {
      @Override
      public void onPictureTaken(byte[] data, Camera camera) {
        Log.d(LOG_TAG, "camera: jpeg");
        doSaveJpeg(data);
      }
    };

    mCameraFragment.takePicture(shutter, raw, jpeg);
  }

  private void doSaveJpeg(final byte[] data) {
    new ImageSaveTask().execute(data);

  }

  class ImageSaveTask extends AsyncTask<byte[], Void, String> {

    @Override
    protected String doInBackground(byte[]... params) {
      final Flow flow = mCurrentFlow;
      if (flow == null) {
        Log.w(LOG_TAG, "ImageSaveTask for empty flow.");
        return null;
      }
      final byte[] rawJpegData = params[0];
      final File imageDir = getDir("pour-images", MODE_PRIVATE);
      final String baseName = "pour-" + flow.getFlowId();

      File imageFile = new File(imageDir, baseName + ".jpg");
      int ext = 2;
      while (imageFile.exists()) {
        imageFile = new File(imageDir, baseName + "-" + (ext++) + ".jpg");
      }

      //Bitmap imageBitmap = BitmapFactory.decodeByteArray(rawJpegData, 0, rawJpegData.length);

      try {
        FileOutputStream fos = new FileOutputStream(imageFile);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        bos.write(rawJpegData);
        bos.flush();
        bos.close();
      } catch (IOException e) {
        Log.e(LOG_TAG, "Could not save image.", e);
        return null;
      }

      final String savedImage = imageFile.getAbsolutePath();
      Log.i(LOG_TAG, "Saved pour image: " + savedImage);
      flow.addImage(savedImage);
      return savedImage;
    }

  }

  public static Intent getStartIntent(Context context, long flowId) {
    final Intent intent = new Intent(context, PourInProgressActivity.class);
    intent.setAction(KegtapBroadcast.ACTION_POUR_START);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    intent.putExtra(KegtapBroadcast.POUR_UPDATE_EXTRA_FLOW_ID, flowId);
    return intent;
  }

}
