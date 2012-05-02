/**
 *
 */
package org.kegbot.app.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.util.Log;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class Downloader {

  private static final String LOG_TAG = Downloader.class.getSimpleName();

  private static Downloader sInstance;

  // private final ExecutorService mExecutor = Executors.newFixedThreadPool(5);

  public Bitmap downloadBitmap(String url) {
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

  public static void downloadRaw(final String url, final File output) throws IOException {
    final URL destUrl = new URL(url);
    final HttpURLConnection connection = (HttpURLConnection) destUrl.openConnection();

    final FileOutputStream out = new FileOutputStream(output);
    final InputStream input = connection.getInputStream();

    try {
      byte buffer[] = new byte[4096];
      int len;
      while ((len = input.read(buffer)) >= 0) {
        out.write(buffer, 0, len);
      }
    } finally {
      out.close();
      input.close();
    }

  }

  public static synchronized Downloader getSingletonInstance() {
    if (sInstance == null) {
      sInstance = new Downloader();
    }
    return sInstance;
  }

}
