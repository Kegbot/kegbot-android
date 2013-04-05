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
package org.kegbot.api;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.hoho.android.usbserial.util.HexDump;
import com.squareup.okhttp.OkHttpClient;

/**
 * Convenience class for making {@link Request}s against an {@link OkHttpClient}.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
class Http {

  private static final String TAG = Http.class.getSimpleName();

  public static final String GET = "GET";
  public static final String POST = "POST";
  public static final String PATCH = "PATCH";
  public static final String PUT = "PUT";
  public static final String HEAD = "HEAD";

  private static final byte[] HYPHENS = {'-', '-'};
  private static final byte[] CRLF = {'\r', '\n'};

  private final OkHttpClient mClient;

  public Http(OkHttpClient client) {
    mClient = client;
  }

  public InputStream request(Request request)
      throws IOException {

    // Prepare parameters and body.
    Log.d(TAG, String.format("--> %s %s", request.getMethod(), request.getUrl()));

    String url = request.getUrl();
    final String method = request.getMethod();
    final List<Pair<String, String>> params = request.getParameters();

    if (GET.equals(method) && !params.isEmpty()) {
      url = String.format("%s?%s", url, getUrlParamsString(request));
    }

    final HttpURLConnection connection = mClient.open(new URL(url));
    for (final Map.Entry<String, String> e : request.getHeaders().entrySet()) {
      connection.setRequestProperty(e.getKey(), e.getValue());
    }
    connection.setRequestMethod(request.getMethod());

    // Execute request.
    buildBody(request, connection);
    return connection.getInputStream();
  }

  public JsonNode requestJson(Request request)
      throws IOException {
    final InputStream input = request(request);
    try {
      final ObjectMapper mapper = new ObjectMapper();
      final JsonNode rootNode = mapper.readValue(input, JsonNode.class);
      return rootNode;
    } finally {
      input.close();
    }
  }

  private static String getUrlParamsString(Request request) {
    final List<String> parts = Lists.newArrayList();
    for (Pair<String, String> part : request.getParameters()) {
      parts.add(String.format("%s=%s", URLEncoder.encode(part.first), URLEncoder.encode(part.second)));
    }
    return Joiner.on('&').join(parts);
  }

  private static void buildBody(Request request, HttpURLConnection connection) throws IOException {
    if ((request.getMethod().equals(GET) || request.getParameters().isEmpty())
        && request.getFiles().isEmpty()) {
      return;
    }

    connection.setDoOutput(true);
    connection.setDoInput(true);

    final String boundary = getBoundary();
    final byte[] boundaryBytes = boundary.getBytes();
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();

    final boolean multiPart = !request.getFiles().isEmpty();

    if (!multiPart) {
      connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      bos.write(getUrlParamsString(request).getBytes());
    } else {
      connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

      // Parameters.
      for (final Pair<String, String> entry : request.getParameters()) {
        bos.write(HYPHENS);
        bos.write(boundaryBytes);
        bos.write(CRLF);
        bos.write(
            String.format("Content-Disposition: form-data; name=\"%s\"", entry.first).getBytes());
        bos.write(CRLF);
        bos.write(CRLF);
        bos.write(entry.second.getBytes());
        bos.write(CRLF);
      }

      // Files.
      for (final Map.Entry<String, File> entry : request.getFiles().entrySet()) {
        final String entityName = entry.getKey();
        final File file = entry.getValue();

        bos.write(HYPHENS);
        bos.write(boundaryBytes);
        bos.write(CRLF);
        bos.write(
            String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"",
                entityName, file.getName()).getBytes());
        bos.write(CRLF);
        bos.write(CRLF);

        final FileInputStream fis = new FileInputStream(file);
        try {
          ByteStreams.copy(fis, bos);
        } finally {
          fis.close();
        }
        bos.write(CRLF);
      }
      bos.write(HYPHENS);
      bos.write(boundaryBytes);
      bos.write(HYPHENS);
      bos.write(CRLF);
    }
    bos.flush();

    final byte[] outputBytes = bos.toByteArray();
    Log.d(TAG, HexDump.dumpHexString(outputBytes, 0, Math.min(1024, outputBytes.length)));
    connection.addRequestProperty("Content-Length", Integer.valueOf(outputBytes.length).toString());

    final OutputStream s = connection.getOutputStream();
    try {
      s.write(outputBytes);
    } finally {
      s.close();
    }

  }

  /** Generates a pseudo-random boundary string. */
  private static String getBoundary() {
    return "-------KegbotMultipart" + SystemClock.elapsedRealtime();
  }

}
