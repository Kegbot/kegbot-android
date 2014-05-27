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

package org.kegbot.app.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.google.common.collect.Sets;
import com.hoho.android.usbserial.util.HexDump;

import org.kegbot.app.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
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

/**
 * This helper class download images from the Internet and binds those with the provided ImageView.
 * <p/>
 * <p/>
 * A local cache of downloaded images is maintained internally to improve performance.
 */
public class ImageDownloader {
  private static final String TAG = ImageDownloader.class.getSimpleName();

  private static final int HANDLER_KEY_DOWNLOAD_COMPLETE = 1;

  private static final boolean DEBUG = false;

  /**
   * All ImageViews and the URL they have requested.
   */
  private final WeakHashMap<ImageView, String> mDownloadRequests =
      new WeakHashMap<ImageView, String>();

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

  private URL mBaseUrl;

  private File mCacheDir;

  @SuppressLint("HandlerLeak")
  private final Handler mHandler = new Handler(Looper.getMainLooper()) {

    @Override
    public void handleMessage(Message msg) {
      if (msg.what == HANDLER_KEY_DOWNLOAD_COMPLETE) {
        final DownloadResult result = (DownloadResult) msg.obj;
        handleDownloadComplete(result);
      }
      super.handleMessage(msg);
    }
  };

  public ImageDownloader(final Context context, final String baseUrl) {
    mContext = context;
    mCacheDir = context.getCacheDir();
    setBaseUrl(baseUrl);
  }

  public void setBaseUrl(String url) {
    if (url != null) {
      try {
        mBaseUrl = new URL(url);
      } catch (MalformedURLException e) {
        Log.w(TAG, "Bad base url: " + url);
        mBaseUrl = null;
      }
    }
  }

  /**
   * Download the specified image from the Internet and binds it to the provided ImageView. The
   * binding is immediate if the image is found in the cache and will be done asynchronously
   * otherwise. A null bitmap will be associated to the ImageView if an error occurs.
   *
   * @param url       The URL of the image to download.
   * @param imageView The ImageView to bind the downloaded image to.
   */
  public void download(String url, final ImageView imageView) {
    if (mBaseUrl != null) {
      URL fullUrl;
      try {
        fullUrl = new URL(mBaseUrl, url);
      } catch (MalformedURLException e) {
        Log.w(TAG, "Malformed URL: " + url);
        return;
      }
      if (DEBUG) Log.d(TAG, "original url=" + url);
      url = fullUrl.toString();
    }

    if (DEBUG) Log.d(TAG, "download url=" + url + " imageView=" + imageView);
    imageView.setTag(url);
    // resetPurgeTimer();
    final Bitmap bitmap = getBitmapFromCache(url.toString());

    if (bitmap != null) {
      if (DEBUG) Log.d(TAG, "download: cache hit");
      // Bitmap in cache: no download necessary.
      applyBitmapToImageView(bitmap, imageView);
    } else {
      if (DEBUG) Log.d(TAG, "download: cache miss");

      // TODO(mikey): this should be done only in an adapter when convertView !=
      // null
      // imageView.setBackgroundDrawable(null);
      // imageView.setImageBitmap(null);

      // No bitmap in cache: enqueue the download.
      synchronized (mDownloadRequests) {
        if (DEBUG) Log.d(TAG, "download: adding to request queue");
        if (!mDownloadRequests.containsValue(url)) {
          enqueueDownload(url);
        }
        mDownloadRequests.put(imageView, url);
      }
    }
  }

  private void enqueueDownload(final String url) {
    if (DEBUG) Log.d(TAG, "Enqueuing download: url=" + url);
    mExecutor.submit(new Runnable() {
      @Override
      public void run() {
        if (DEBUG) Log.d(TAG, "Download running for url=" + url);

        Bitmap bitmap = getBitmapFromFileCache(url);
        if (bitmap != null) {
          if (DEBUG) Log.d(TAG, "Found bitmap in file cache.");
        } else {
          if (DEBUG) Log.d(TAG, "Download running for url=" + url);
          bitmap = Downloader.downloadBitmap(url);
          Log.d(TAG, "Downloaded: " + url);
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
    if (DEBUG) Log.d(TAG, "Assigning bitmap=" + bitmap + " imageView=" + imageView);
    Utils.setBackground(imageView, null);
    imageView.setImageBitmap(bitmap);
    imageView.setAlpha(1.0f);
  }

  private void handleDownloadComplete(DownloadResult downloadResult) {
    final String url = downloadResult.url;
    final Bitmap bitmap = downloadResult.bitmap;
    if (DEBUG) Log.d(TAG, "handleDownloadComplete: url=" + url + " bitmap=" + bitmap);

    final Set<ImageView> toRemove = Sets.newLinkedHashSet();
    synchronized (mDownloadRequests) {
      for (final Map.Entry<ImageView, String> entry : mDownloadRequests.entrySet()) {
        if (url.equals(entry.getValue())) {
          final ImageView imageView = entry.getKey();
          toRemove.add(imageView);

          final String imageViewTag = (String) imageView.getTag();
          if (url.equals(imageViewTag)) {
            applyBitmapToImageView(bitmap, imageView);
            Animation myFadeInAnimation = AnimationUtils.loadAnimation(mContext,
                R.anim.image_fade_in);
            imageView.startAnimation(myFadeInAnimation);
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
  private final static ConcurrentHashMap<String, SoftReference<Bitmap>> sSoftBitmapCache =
      new ConcurrentHashMap<String, SoftReference<Bitmap>>(HARD_CACHE_CAPACITY / 2);

  /**
   * Adds this bitmap to the cache.
   *
   * @param bitmap The newly downloaded bitmap.
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
    if (bitmap == null) {
      return;
    }
    final File cacheFile = getCacheFilename(url);
    if (cacheFile.exists()) {
      Log.d(TAG, "Updating cached file url=" + url + " filename=" + cacheFile);
    } else {
      Log.d(TAG, "Creating cached file url=" + url + " filename=" + cacheFile);
    }

    try {
      cacheFile.createNewFile();
      FileOutputStream fos = new FileOutputStream(cacheFile);
      bitmap.compress(Bitmap.CompressFormat.PNG, 85, fos);
      fos.flush();
      fos.close();
    } catch (IOException e) {
      Log.w(TAG, "Error adding cache file.", e);
      cacheFile.delete();
    }

  }

  private Bitmap getBitmapFromFileCache(String url) {
    final File cacheFile = getCacheFilename(url);
    if (cacheFile.exists()) {
      if (DEBUG) Log.d(TAG, "getFromFileCache hit: url=" + url + " filename=" + cacheFile);
    } else {
      if (DEBUG) Log.d(TAG, "getFromFileCache MISS: url=" + url + " filename=" + cacheFile);
      return null;
    }
    return BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
  }

  /**
   * @param url The URL of the image that will be retrieved from the cache.
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
   * Clears the image cache used internally to improve performance. Note that for memory efficiency
   * reasons, the cache will automatically be cleared after a certain inactivity delay.
   */
  public void clearCache() {
    sHardBitmapCache.clear();
    sSoftBitmapCache.clear();
  }

}
