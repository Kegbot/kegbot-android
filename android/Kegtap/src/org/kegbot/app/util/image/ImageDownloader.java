/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.kegbot.app.util.image;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.kegbot.app.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.google.common.collect.Sets;
import com.hoho.android.usbserial.util.HexDump;

/**
 * This helper class download images from the Internet and binds those with the
 * provided ImageView.
 *
 * <p>
 * It requires the INTERNET permission, which should be added to your
 * application's manifest file.
 * </p>
 *
 * A local cache of downloaded images is maintained internally to improve
 * performance.
 */
public class ImageDownloader {
  private static final String LOG_TAG = "ImageDownloader";

  private static final int HANDLER_KEY_DOWNLOAD_COMPLETE = 1;

  private static ImageDownloader sSingleton = null;

  private static final boolean DEBUG = false;

  /**
   * All ImageViews and the URL they have requested.
   */
  private final WeakHashMap<ImageView, String> mDownloadRequests = new WeakHashMap<ImageView, String>();

  private static class DownloadResult {
    String url;
    Bitmap bitmap;

    DownloadResult(String url, Bitmap bitmap) {
      this.url = url;
      this.bitmap = bitmap;
    }
  }

  private final ExecutorService mExecutor = Executors.newFixedThreadPool(5);

  private final Context mContext;

  private File mCacheDir;

  private final Handler mHandler = new Handler() {

    @Override
    public void handleMessage(Message msg) {
      if (msg.what == HANDLER_KEY_DOWNLOAD_COMPLETE) {
        final DownloadResult result = (DownloadResult) msg.obj;
        handleDownloadComplete(result);
      }
      super.handleMessage(msg);
    }
  };

  private ImageDownloader(final Context context) {
    mContext = context;
    mCacheDir = context.getCacheDir();
  }

  /**
   * Download the specified image from the Internet and binds it to the provided
   * ImageView. The binding is immediate if the image is found in the cache and
   * will be done asynchronously otherwise. A null bitmap will be associated to
   * the ImageView if an error occurs.
   *
   * @param url
   *          The URL of the image to download.
   * @param imageView
   *          The ImageView to bind the downloaded image to.
   */
  public void download(final String url, final ImageView imageView) {
    if (DEBUG) Log.d(LOG_TAG, "download url=" + url + " imageView=" + imageView);
    imageView.setTag(url);
    // resetPurgeTimer();
    final Bitmap bitmap = getBitmapFromCache(url);

    if (bitmap != null) {
      if (DEBUG) Log.d(LOG_TAG, "download: cache hit");
      // Bitmap in cache: no download necessary.
      applyBitmapToImageView(bitmap, imageView);
    } else {
      if (DEBUG) Log.d(LOG_TAG, "download: cache miss");

      // TODO(mikey): this should be done only in an adapter when convertView !=
      // null
      // imageView.setBackgroundDrawable(null);
      // imageView.setImageBitmap(null);

      // No bitmap in cache: enqueue the download.
      synchronized (mDownloadRequests) {
        if (DEBUG) Log.d(LOG_TAG, "download: adding to request queue");
        if (!mDownloadRequests.containsValue(url)) {
          enqueueDownload(url);
        }
        mDownloadRequests.put(imageView, url);
      }
    }
  }

  private void enqueueDownload(final String url) {
    if (DEBUG) Log.d(LOG_TAG, "Enqueuing download: url=" + url);
    mExecutor.submit(new Runnable() {
      @Override
      public void run() {
        if (DEBUG) Log.d(LOG_TAG, "Download running for url=" + url);

        Bitmap bitmap = getBitmapFromFileCache(url);
        if (bitmap != null) {
          if (DEBUG) Log.d(LOG_TAG, "Found bitmap in file cache.");
        } else {
          if (DEBUG) Log.d(LOG_TAG, "Download running for url=" + url);
          bitmap = downloadBitmap(url);
          Log.d(LOG_TAG, "Downloaded: " + url);
          addBitmapToFileCache(url, bitmap);
        }

        addBitmapToCache(url, bitmap);
        postDownloadCompletedToHandler(url, bitmap);
      }
    });
  }

  private void postDownloadCompletedToHandler(String url, Bitmap bitmap) {
    final DownloadResult result = new DownloadResult(url, bitmap);
    final Message msg = mHandler.obtainMessage(HANDLER_KEY_DOWNLOAD_COMPLETE, result);
    mHandler.sendMessage(msg);
  }

  private void applyBitmapToImageView(Bitmap bitmap, ImageView imageView) {
    if (DEBUG) Log.d(LOG_TAG, "Assigning bitmap=" + bitmap + " imageView=" + imageView);
    imageView.setBackgroundDrawable(null);
    imageView.setImageBitmap(bitmap);
    imageView.setAlpha(1.0f);
  }

  private void handleDownloadComplete(DownloadResult downloadResult) {
    final String url = downloadResult.url;
    final Bitmap bitmap = downloadResult.bitmap;
    if (DEBUG) Log.d(LOG_TAG, "handleDownloadComplete: url=" + url + " bitmap=" + bitmap);

    final Set<ImageView> toRemove = Sets.newLinkedHashSet();
    synchronized (mDownloadRequests) {
      for (final Map.Entry<ImageView, String> entry : mDownloadRequests.entrySet()) {
        if (url.equals(entry.getValue())) {
          final ImageView imageView = entry.getKey();
          toRemove.add(imageView);

          final String imageViewTag = (String) imageView.getTag();
          if (url.equals(imageViewTag)) {
            applyBitmapToImageView(bitmap, imageView);
            Animation myFadeInAnimation = AnimationUtils.loadAnimation(mContext, R.anim.image_fade_in);
            imageView.startAnimation(myFadeInAnimation); //Set animation to your ImageView
          }
        }
      }

      for (final ImageView view : toRemove) {
        mDownloadRequests.remove(view);
      }
    }
  }

  public void cancelDownloadForView(ImageView view) {
    view.setTag(null);
    synchronized (mDownloadRequests) {
      mDownloadRequests.remove(view);
    }
  }

  private Bitmap downloadBitmap(String url) {
    // AndroidHttpClient is not allowed to be used from the main thread
    final HttpClient client = new DefaultHttpClient();
    final HttpGet getRequest = new HttpGet(url);

    try {
      HttpResponse response = client.execute(getRequest);
      final int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        Log.w("ImageDownloader", "Error " + statusCode + " while retrieving bitmap from " + url);
        return null;
      }

      final HttpEntity entity = response.getEntity();
      if (entity != null) {
        InputStream inputStream = null;
        try {
          BitmapFactory.Options options = new BitmapFactory.Options();
          options.inSampleSize = 2;

          inputStream = entity.getContent();
          return BitmapFactory.decodeStream(inputStream, null, options);
          // Bug on slow connections, fixed in future release.
          // return BitmapFactory.decodeStream(new
          // FlushedInputStream(inputStream));
        } finally {
          if (inputStream != null) {
            inputStream.close();
          }
          entity.consumeContent();
        }
      }
    } catch (IOException e) {
      getRequest.abort();
      Log.w(LOG_TAG, "I/O error while retrieving bitmap from " + url, e);
    } catch (IllegalStateException e) {
      getRequest.abort();
      Log.w(LOG_TAG, "Incorrect URL: " + url);
    } catch (Exception e) {
      getRequest.abort();
      Log.w(LOG_TAG, "Error while retrieving bitmap from " + url, e);
    } finally {
      if ((client instanceof AndroidHttpClient)) {
        ((AndroidHttpClient) client).close();
      }
    }
    return null;
  }

  /*
   * An InputStream that skips the exact number of bytes provided, unless it
   * reaches EOF.
   */
  static class FlushedInputStream extends FilterInputStream {
    public FlushedInputStream(InputStream inputStream) {
      super(inputStream);
    }

    @Override
    public long skip(long n) throws IOException {
      long totalBytesSkipped = 0L;
      while (totalBytesSkipped < n) {
        long bytesSkipped = in.skip(n - totalBytesSkipped);
        if (bytesSkipped == 0L) {
          int b = read();
          if (b < 0) {
            break; // we reached EOF
          } else {
            bytesSkipped = 1; // we read one byte
          }
        }
        totalBytesSkipped += bytesSkipped;
      }
      return totalBytesSkipped;
    }
  }

  /*
   * Cache-related fields and methods.
   *
   * We use a hard and a soft cache. A soft reference cache is too aggressively
   * cleared by the Garbage Collector.
   */

  private static final int HARD_CACHE_CAPACITY = 10;
  private static final int DELAY_BEFORE_PURGE = 10 * 1000; // in milliseconds

  // Hard cache, with a fixed maximum capacity and a life duration
  private final HashMap<String, Bitmap> sHardBitmapCache = new LinkedHashMap<String, Bitmap>(
      HARD_CACHE_CAPACITY / 2, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(LinkedHashMap.Entry<String, Bitmap> eldest) {
      if (size() > HARD_CACHE_CAPACITY) {
        // Entries push-out of hard reference cache are transferred to soft
        // reference cache
        sSoftBitmapCache.put(eldest.getKey(), new SoftReference<Bitmap>(eldest.getValue()));
        return true;
      } else {
        return false;
      }
    }
  };

  // Soft cache for bitmaps kicked out of hard cache
  private final static ConcurrentHashMap<String, SoftReference<Bitmap>> sSoftBitmapCache = new ConcurrentHashMap<String, SoftReference<Bitmap>>(
      HARD_CACHE_CAPACITY / 2);

  private final Handler purgeHandler = new Handler();

  private final Runnable purger = new Runnable() {
    @Override
    public void run() {
      clearCache();
    }
  };

  /**
   * Adds this bitmap to the cache.
   *
   * @param bitmap
   *          The newly downloaded bitmap.
   */
  private void addBitmapToCache(String url, Bitmap bitmap) {
    if (bitmap != null) {
      synchronized (sHardBitmapCache) {
        sHardBitmapCache.put(url, bitmap);
      }
    }
  }

  private static String getFingerprint(String uri) {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
    md.update(uri.getBytes());
    byte[] digest = md.digest();
    return HexDump.toHexString(digest);
  }

  private File getCacheFilename(String url) {
    return new File(mCacheDir, "ImageDownloader-" + getFingerprint(url));
  }

  private void addBitmapToFileCache(String url, Bitmap bitmap) {
    final File cacheFile = getCacheFilename(url);
    if (cacheFile.exists()) {
      Log.d(LOG_TAG, "Updating cached file url=" + url + " filename=" + cacheFile);
    } else {
      Log.d(LOG_TAG, "Creating cached file url=" + url + " filename=" + cacheFile);
    }

    try {
      cacheFile.createNewFile();
      FileOutputStream fos = new FileOutputStream(cacheFile);
      bitmap.compress(Bitmap.CompressFormat.PNG, 85, fos);
      fos.flush();
      fos.close();
    } catch (IOException e) {
      Log.w(LOG_TAG, "Error adding cache file.", e);
      cacheFile.delete();
    }

  }

  private Bitmap getBitmapFromFileCache(String url) {
    final File cacheFile = getCacheFilename(url);
    if (cacheFile.exists()) {
      if (DEBUG) Log.d(LOG_TAG, "getFromFileCache hit: url=" + url + " filename=" + cacheFile);
    } else {
      if (DEBUG) Log.d(LOG_TAG, "getFromFileCache MISS: url=" + url + " filename=" + cacheFile);
      return null;
    }
    return BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
  }

  /**
   * @param url
   *          The URL of the image that will be retrieved from the cache.
   * @return The cached bitmap or null if it was not found.
   */
  private Bitmap getBitmapFromCache(String url) {
    // First try the hard reference cache
    synchronized (sHardBitmapCache) {
      final Bitmap bitmap = sHardBitmapCache.get(url);
      if (bitmap != null) {
        // Bitmap found in hard cache
        // Move element to first position, so that it is removed last
        sHardBitmapCache.remove(url);
        sHardBitmapCache.put(url, bitmap);
        return bitmap;
      }
    }

    // Then try the soft reference cache
    SoftReference<Bitmap> bitmapReference = sSoftBitmapCache.get(url);
    if (bitmapReference != null) {
      final Bitmap bitmap = bitmapReference.get();
      if (bitmap != null) {
        // Bitmap found in soft cache
        return bitmap;
      }
      // Soft reference has been Garbage Collected
      sSoftBitmapCache.remove(url);
    }

    return null;
  }

  /**
   * Clears the image cache used internally to improve performance. Note that
   * for memory efficiency reasons, the cache will automatically be cleared
   * after a certain inactivity delay.
   */
  public void clearCache() {
    sHardBitmapCache.clear();
    sSoftBitmapCache.clear();
  }

  /**
   * Allow a new delay before the automatic cache clear is done.
   */
  private void resetPurgeTimer() {
    purgeHandler.removeCallbacks(purger);
    purgeHandler.postDelayed(purger, DELAY_BEFORE_PURGE);
  }

  public synchronized static ImageDownloader getSingletonInstance(final Context context) {
    if (sSingleton == null) {
      sSingleton = new ImageDownloader(context.getApplicationContext());
    }
    return sSingleton;
  }
}
