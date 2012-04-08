/*
 * Based on CameraFragment example from Android:
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.kegbot.kegtap.camera;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.kegbot.kegtap.KegtapBroadcast;
import org.kegbot.kegtap.R;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class CameraFragment extends Fragment {

  private static final String TAG = CameraFragment.class.getSimpleName();

  private Preview mPreview;
  Camera mCamera;
  int mNumberOfCameras;
  int mRotation = 0;

  private Button mPictureButton;
  private int mPictureSeconds = 0;
  private final Handler mHandler = new Handler();

  // The first rear facing camera
  int mDefaultCameraId;

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
    mNumberOfCameras = Camera.getNumberOfCameras();

    // Find the ID of the default camera
    CameraInfo cameraInfo = new CameraInfo();
    for (int i = 0; i < mNumberOfCameras; i++) {
      Camera.getCameraInfo(i, cameraInfo);
      if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
        mDefaultCameraId = i;
      }
    }
  }

  public void takePicture() {
    final ShutterCallback shutterCallback = new ShutterCallback() {
      @Override
      public void onShutter() {
        Log.d(TAG, "camera shutter");
      }
    };

    final PictureCallback jpegCallback = new PictureCallback() {
      @Override
      public void onPictureTaken(byte[] data, Camera camera) {
        camera.startPreview();
        Log.d(TAG, "camera jpeg: " + data);
        new ImageSaveTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, data);
      }
    };

    takePicture(shutterCallback, null, jpegCallback);
  }


  private class ImageSaveTask extends AsyncTask<byte[], Void, String> {
    @Override
    protected String doInBackground(byte[]... params) {
      byte[] data = params[0];
      final int rotation = getDisplayOrientation();
      final Bitmap bitmap = decodeAndRotateFromJpeg(data, rotation);

      final File imageDir = getActivity().getCacheDir();
      final Date pourDate = new Date(System.currentTimeMillis());
      final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");
      final String baseName = "pour-" + format.format(pourDate);

      File imageFile = new File(imageDir, baseName + ".jpg");
      imageFile.setReadable(true, false);
      int ext = 2;
      while (imageFile.exists()) {
        imageFile = new File(imageDir, baseName + "-" + (ext++) + ".jpg");
        imageFile.setReadable(true, false);
      }

      try {
        FileOutputStream fos = new FileOutputStream(imageFile);
        bitmap.compress(CompressFormat.JPEG, 100, fos);
        fos.close();
      } catch (IOException e) {
        Log.w(TAG, "Could not save image.", e);
        return null;
      }

      final String savedImage = imageFile.getAbsolutePath();
      Log.i(TAG, "Saved pour image: " + savedImage);
      return savedImage;
    }

    @Override
    protected void onPostExecute(String result) {
      super.onPostExecute(result);
      final Intent intent = KegtapBroadcast.getPictureTakenBroadcastIntent(result);
      getActivity().sendBroadcast(intent);
    }

    private Bitmap decodeAndRotateFromJpeg(byte[] data, int rotation) {
      Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
      if (rotation != 0) {
        Log.w(TAG, "ImageSaveTask: rotation=" + rotation);

        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix,
            true);
      }
      return bitmap;
    }
  }

  private void takePicture(final ShutterCallback shutter, final PictureCallback raw,
      final PictureCallback jpeg) {
    mCamera.cancelAutoFocus();
    mCamera.autoFocus(new Camera.AutoFocusCallback() {
      @Override
      public void onAutoFocus(boolean success, Camera camera) {
        mCamera.takePicture(shutter, raw, jpeg);
      }
    });
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.camera_fragment_layout, container, false);
    mPreview = (Preview) view.findViewById(R.id.cameraPreview);
    mPictureButton = ((Button) view.findViewById(R.id.cameraTakePictureButton));
    mPictureButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        schedulePicture();
      }
    });
    view.setBackgroundDrawable(
        getResources().getDrawable(R.drawable.shape_rounded_rect));
    return view;
  }

  public void schedulePicture() {
    mPictureSeconds = 3;
    mHandler.removeCallbacks(PICTURE_COUNTDOWN_RUNNABLE);
    mHandler.post(PICTURE_COUNTDOWN_RUNNABLE);
  }

  public void cancelPendingPicture() {
    mHandler.removeCallbacks(PICTURE_COUNTDOWN_RUNNABLE);
  }

  @Override
  public void onResume() {
    super.onResume();
    mCamera = Camera.open(mDefaultCameraId);
    setCameraDisplayOrientation(getActivity(), mDefaultCameraId, mCamera);
    mPreview.setCamera(mCamera);
  }

  @Override
  public void onPause() {
    super.onPause();
    if (mCamera != null) {
      mPreview.setCamera(null);
      mCamera.release();
      mCamera = null;
    }
  }

  public void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
    Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
    Camera.getCameraInfo(cameraId, info);
    int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
    int degrees = 0;
    switch (rotation) {
      case Surface.ROTATION_0:
        degrees = 0;
        break;
      case Surface.ROTATION_90:
        degrees = 90;
        break;
      case Surface.ROTATION_180:
        degrees = 180;
        break;
      case Surface.ROTATION_270:
        degrees = 270;
        break;
    }
    Log.d(TAG, "setCameraDisplayOrientation: degrees=" + degrees);

    int result;
    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
      result = (info.orientation + degrees) % 360;
      result = (360 - result) % 360; // compensate the mirror
    } else { // back-facing
      result = (info.orientation - degrees + 360) % 360;
    }
    mRotation = result;
    camera.setDisplayOrientation(result);
  }

  public int getDisplayOrientation() {
    return mRotation;
  }

}
