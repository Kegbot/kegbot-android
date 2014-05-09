/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
 *
 * This file is part of the Kegtab package from the Kegbot project. For more
 * information on Kegtab or Kegbot, see <http://kegbot.org/>.
 *
 * Kegtab is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2.
 *
 * Kegtab is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Kegtab. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * Based on Android sample code which has the following license:
 *
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

package org.kegbot.app.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility methods for HTTP downloads.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class Downloader {

  private static final String LOG_TAG = Downloader.class.getSimpleName();

  /**
   * Downloads and returns a URL as a {@link Bitmap}.
   *
   * @param url
   *          the image to download
   * @return a new {@link Bitmap}, or {@code null} if any error occurred
   */
  public static Bitmap downloadBitmap(String url) {
    final HttpClient client = new DefaultHttpClient();
    final HttpGet getRequest = new HttpGet(url);

    try {
      HttpResponse response = client.execute(getRequest);
      final int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        Log.w(LOG_TAG, "Error " + statusCode + " while retrieving bitmap from " + url);
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

  /**
   * Downloads an HTTP resource to an output file.
   *
   * @param url
   *          the resource to download
   * @param output
   *          the output file
   * @throws IOException
   *           upon any error
   */
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

}
