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

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import android.util.Pair;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * A container and builder for generic HTTP requests. Bring your own transport.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class Request {

  public static final String GET = "GET";
  public static final String POST = "POST";
  public static final String PATCH = "PATCH";
  public static final String PUT = "PUT";
  public static final String HEAD = "HEAD";

  private final String mUrl;
  private final String mMethod;;
  private final List<Pair<String, String>> mParameters;
  private final Map<String, File> mFiles;
  private final Map<String, String> mHeaders;

  private Request(String url, String method, List<Pair<String, String>> parameters,
      Map<String, File> files, Map<String, String> headers) {
    mUrl = Preconditions.checkNotNull(url);
    mMethod = Preconditions.checkNotNull(method);
    mParameters = Preconditions.checkNotNull(parameters);
    mFiles = Preconditions.checkNotNull(files);
    mHeaders = Preconditions.checkNotNull(headers);
  }

  public String getUrl() {
    return mUrl;
  }

  public String getMethod() {
    return mMethod;
  }

  public List<Pair<String, String>> getParameters() {
    return mParameters;
  }

  public Map<String, File> getFiles() {
    return mFiles;
  }

  public Map<String, String> getHeaders() {
    return mHeaders;
  }

  public static Builder newBuilder(String url) {
    return new Request.Builder(url);
  }

  public static class Builder {

    private final String mUrl;
    private String mMethod = Http.GET;
    @Nullable
    private String mBody = null;

    private final List<Pair<String, String>> mParameters = Lists.newArrayList();
    private final Map<String, File> mFiles = Maps.newLinkedHashMap();
    private final Map<String, String> mHeaders = Maps.newLinkedHashMap();

    public Builder(String url) {
      mUrl = url;
    }

    public Builder setMethod(String method) {
      mMethod = method;
      return this;
    }

    public Builder addParameter(String key, String value) {
      mParameters.add(Pair.create(key, value));
      return this;
    }

    public Builder addFile(String entityName, File file) {
      mFiles.put(entityName, file);
      return this;
    }

    public Builder addHeader(String headerName, String headerValue) {
      mHeaders.put(headerName, headerValue);
      return this;
    }

    public Request build() {
      return new Request(mUrl, mMethod, mParameters, mFiles, mHeaders);
    }

  }

}
