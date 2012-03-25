/*
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

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

public class CameraFragment extends Fragment {

  private static final String TAG = CameraFragment.class.getSimpleName();

  private Preview mPreview;
  Camera mCamera;
  int mNumberOfCameras;
  int mRotation = 0;

  // The first rear facing camera
  int mDefaultCameraId;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Create a RelativeLayout container that will hold a SurfaceView,
    // and set it as the content of our activity.
    mPreview = new Preview(this.getActivity());

    // Find the total number of cameras available
    mNumberOfCameras = Camera.getNumberOfCameras();
    Log.d(TAG, "_______ mNumberOfCameras=" + mNumberOfCameras);

    // Find the ID of the default camera
    CameraInfo cameraInfo = new CameraInfo();
    for (int i = 0; i < mNumberOfCameras; i++) {
      Camera.getCameraInfo(i, cameraInfo);
      if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
        mDefaultCameraId = i;
        Log.d(TAG, "_______ mDefaultCameraId=" + mDefaultCameraId);

      }
    }
  }

  public void takePicture(final ShutterCallback shutter, final PictureCallback raw,
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
    return mPreview;
  }

  @Override
  public void onResume() {
    super.onResume();

    // Open the default i.e. the first rear facing camera.
    mCamera = Camera.open(mDefaultCameraId);
    setCameraDisplayOrientation(getActivity(), mDefaultCameraId, mCamera);
    mPreview.setCamera(mCamera);
  }

  @Override
  public void onPause() {
    super.onPause();

    // Because the Camera object is a shared resource, it's very
    // important to release it when the activity is paused.
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
