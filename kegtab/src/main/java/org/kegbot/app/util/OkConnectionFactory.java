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

import com.github.kevinsawicki.http.HttpRequest;
import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

/**
 * A {@link com.github.kevinsawicki.http.HttpRequest.ConnectionFactory connection factory} which
 * uses OkHttp.
 * <p/>
 * Call {@link HttpRequest#setConnectionFactory(HttpRequest.ConnectionFactory)} with an instance of
 * this class to enable.
 * <p/>
 * Source: https://gist.github.com/JakeWharton/5797571
 */
public class OkConnectionFactory implements HttpRequest.ConnectionFactory {
  private final OkHttpClient client;

  public OkConnectionFactory() {
    this(new OkHttpClient());
  }

  public OkConnectionFactory(OkHttpClient client) {
    if (client == null) {
      throw new NullPointerException("Client must not be null.");
    }
    this.client = client;
  }

  @Override
  public HttpURLConnection create(URL url) throws IOException {
    return client.open(url);
  }

  @Override
  public HttpURLConnection create(URL url, Proxy proxy) throws IOException {
    throw new UnsupportedOperationException(
        "Per-connection proxy is not supported. Use OkHttpClient's setProxy instead.");
  }
}
