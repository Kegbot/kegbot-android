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

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class CameraFragment extends Fragment {

  private Preview mPreview;
  Camera mCamera;
  int mNumberOfCameras;
  int mCameraCurrentlyLocked;

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

    // Find the ID of the default camera
    CameraInfo cameraInfo = new CameraInfo();
    for (int i = 0; i < mNumberOfCameras; i++) {
      Camera.getCameraInfo(i, cameraInfo);
      if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
        mDefaultCameraId = i;
      }
    }
    //setHasOptionsMenu(mNumberOfCameras > 1);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    // Add an up arrow to the "home" button, indicating that the button will go
    // "up"
    // one activity in the app's Activity heirarchy.
    // Calls to getActionBar() aren't guaranteed to return the ActionBar when
    // called
    // from within the Fragment's onCreate method, because the Window's decor
    // hasn't been
    // initialized yet. Either call for the ActionBar reference in
    // Activity.onCreate()
    // (after the setContentView(...) call), or in the Fragment's
    // onActivityCreated method.
    Activity activity = this.getActivity();
    ActionBar actionBar = activity.getActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
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
    mCameraCurrentlyLocked = mDefaultCameraId;
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

  /*
  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    if (mNumberOfCameras > 1) {
      // Inflate our menu which can gather user input for switching camera
      inflater.inflate(R.menu.camera_menu, menu);
    } else {
      super.onCreateOptionsMenu(menu, inflater);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    switch (item.getItemId()) {
      case R.id.switch_cam:
        // Release this camera -> mCameraCurrentlyLocked
        if (mCamera != null) {
          mCamera.stopPreview();
          mPreview.setCamera(null);
          mCamera.release();
          mCamera = null;
        }

        // Acquire the next camera and request Preview to reconfigure
        // parameters.
        mCamera = Camera.open((mCameraCurrentlyLocked + 1) % mNumberOfCameras);
        mCameraCurrentlyLocked = (mCameraCurrentlyLocked + 1) % mNumberOfCameras;
        mPreview.switchCamera(mCamera);

        // Start the preview
        mCamera.startPreview();
        return true;
      case android.R.id.home:
        Intent intent = new Intent(this.getActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);

      default:
        return super.onOptionsItemSelected(item);
    }
  }
  */
}
