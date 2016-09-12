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
package org.kegbot.app.camera;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.List;

/**
 * A simple wrapper around a Camera and a SurfaceView that renders a centered preview of the Camera
 * to the surface. We need to center the SurfaceView because not all devices have cameras that
 * support preview sizes at the same aspect ratio as the device's display.
 * <p/>
 * <p/>
 * Local modifications: when layout height is specified as 0, it is set to <code>layout_width /
 * {@link #ASPECT_RATIO}</code> in {@link #onMeasure(int, int)}.
 * <p/>
 * <p/>
 * Adapted from: http://developer.android.com/resources/samples/HoneycombGallery/src/com/example/android/hcgallery/CameraFragment.html
 */
public class Preview extends ViewGroup implements SurfaceHolder.Callback {
  private final String TAG = Preview.class.getSimpleName();

  private static final double ASPECT_RATIO = 4.0 / 3.0;

  SurfaceView mSurfaceView;
  SurfaceHolder mHolder;
  Size mPreviewSize;
  List<Size> mSupportedPreviewSizes;
  Camera mCamera;
  boolean mLoggedSize = false;

  public Preview(Context context) {
    super(context);
    init(context);
  }

  public Preview(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  public Preview(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public void setCamera(Camera camera) {
    mCamera = camera;
    if (mCamera != null) {
      mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
      if (mHolder != null) {
        try {
          mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
          Log.e(TAG, "Error in setCamera", e);
        }
      }
      requestLayout();
    }

  }

  private void init(Context context) {
    if (mSurfaceView == null) {
      mSurfaceView = new SurfaceView(context);
      addView(mSurfaceView);
      mHolder = mSurfaceView.getHolder();
      mHolder.addCallback(this);
    }
  }


  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // We purposely disregard child measurements because act as a
    // wrapper to a SurfaceView that centers the camera preview instead
    // of stretching it.
    final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
    int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

    // Special case: adjust height to match width, preserving aspect
    // ratio, when specified as zero.
    if (height == 0) {
      height = (int) (width / ASPECT_RATIO);
    }

    setMeasuredDimension(width, height);

    if (mSupportedPreviewSizes != null) {
      mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
    }
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    if (changed && getChildCount() > 0) {
      final View child = getChildAt(0);

      final int width = r - l;
      final int height = b - t;

      int previewWidth = width;
      int previewHeight = height;
      if (mPreviewSize != null) {
        previewWidth = mPreviewSize.width;
        previewHeight = mPreviewSize.height;
      }

      // Center the child SurfaceView within the parent.
      if (width * previewHeight > height * previewWidth) {
        final int scaledChildWidth = previewWidth * height / previewHeight;
        child.layout((width - scaledChildWidth) / 2, 0, (width + scaledChildWidth) / 2, height);
      } else {
        final int scaledChildHeight = previewHeight * width / previewWidth;
        child.layout(0, (height - scaledChildHeight) / 2, width, (height + scaledChildHeight) / 2);
      }
    }
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    Log.d(TAG, "surfaceCreated mCamera=" + mCamera);
    // The Surface has been created, acquire the camera and tell it where
    // to draw.
    try {
      if (mCamera != null) {
        mCamera.setPreviewDisplay(holder);
      }
    } catch (IOException exception) {
      Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
    }
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    Log.d(TAG, "surfaceDestroyed mCamera=" + mCamera);

    // Surface will be destroyed when we return, so stop the preview.
    if (mCamera != null) {
      mCamera.stopPreview();
    }
  }

  private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
    final double ASPECT_TOLERANCE = 0.1;
    double targetRatio = (double) w / h;
    if (sizes == null)
      return null;

    Size optimalSize = null;
    double minDiff = Double.MAX_VALUE;

    int targetHeight = h;

    // Try to find an size match aspect ratio and size
    for (Size size : sizes) {
      double ratio = (double) size.width / size.height;
      if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
        continue;
      if (Math.abs(size.height - targetHeight) < minDiff) {
        optimalSize = size;
        minDiff = Math.abs(size.height - targetHeight);
      }
    }

    // Cannot find the one match the aspect ratio, ignore the requirement
    if (optimalSize == null) {
      minDiff = Double.MAX_VALUE;
      for (Size size : sizes) {
        if (Math.abs(size.height - targetHeight) < minDiff) {
          optimalSize = size;
          minDiff = Math.abs(size.height - targetHeight);
        }
      }
    }
    if (!mLoggedSize) {
      if (optimalSize != null) {
        Log.d(TAG, "getOptimalPreviewSize: " + optimalSize.width + " x " + optimalSize.height);
      } else {
        Log.d(TAG, "getOptimalPreviewSize: null");
      }
      mLoggedSize = true;
    }
    return optimalSize;
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    // Now that the size is known, set up the camera parameters and begin
    // the preview.
    Log.d(TAG, "surfaceChanged mCamera=" + mCamera);

    if (mCamera == null) {
      Log.e(TAG, "Warning: surfaceChanged: null");
      return;
    }
    Camera.Parameters parameters = mCamera.getParameters();
    parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);

    List<String> flashModes = parameters.getSupportedFlashModes();
    if (flashModes != null && flashModes.contains(Parameters.FLASH_MODE_AUTO)) {
      parameters.setFlashMode(Parameters.FLASH_MODE_AUTO);
    }
    requestLayout();

    mCamera.stopPreview();
    mCamera.setParameters(parameters);
    mCamera.startPreview();
  }

}
