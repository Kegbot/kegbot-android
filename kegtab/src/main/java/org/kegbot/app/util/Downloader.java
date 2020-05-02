/*
 * Copyright 2003-2020 The Kegbot Project contributors <info@kegbot.org>
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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility methods for HTTP downloads.
 */
public class Downloader {

  private static final String LOG_TAG = Downloader.class.getSimpleName();

  /**
   * Downloads and returns a URL as a {@link Bitmap}.
   *
   * @param url the image to download
   * @return a new {@link Bitmap}, or {@code null} if any error occurred
   */
  public static Bitmap downloadBitmap(String url) {
    final OkHttpClient client = new OkHttpClient();
    final Request request = new Request.Builder()
            .url(url)
            .build();

    Response response = null;
    Bitmap bitmap = null;
    try {
      response = client.newCall(request).execute();
    } catch (IOException e) {
      return null;
    }

    if (response.isSuccessful()) {
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inSampleSize = 2;
        try {
            bitmap = BitmapFactory.decodeStream(response.body().byteStream(), null, options);
        } catch (Exception e) {
            return null;
        }
    }

    return bitmap;
  }

  /**
   * Downloads an HTTP resource to an output file.
   *
   * @param url    the resource to download
   * @param output the output file
   * @throws IOException upon any error
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
