/*
 * Copyright 2014 Mike Wakerly <opensource@hoho.com>.
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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Log;

import com.github.kevinsawicki.http.HttpRequest;
import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;
import com.google.common.collect.ImmutableMap;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.kegbot.app.KegbotApplication;
import org.kegbot.app.config.AppConfiguration;

import java.io.File;
import java.io.IOException;

public class CheckinClient {

  private static final String TAG = CheckinClient.class.getSimpleName();
  private static final String CHECKIN_URL = "https://kegbotcheckin.appspot.com/checkin";
  private static final String BUGREPORT_URL = "https://kegbotcheckin.appspot.com/bugreport";

  private final AppConfiguration mConfig;
  private final PackageInfo mPackageInfo;
  private final String mUserAgent;

  public CheckinClient(final AppConfiguration config, final PackageInfo packageInfo,
      final String userAgent) {
    mConfig = config;
    mPackageInfo = packageInfo;
    mUserAgent = userAgent;

    //HttpRequest.setConnectionFactory(new OkConnectionFactory());
  }

  public static CheckinClient fromContext(final Context context) {
    final AppConfiguration config =
        ((KegbotApplication) context.getApplicationContext()).getConfig();
    final PackageInfo pinfo = Utils.getOwnPackageInfo(context);
    final String userAgent = Utils.getUserAgent(context);

    return new CheckinClient(config, pinfo, userAgent);
  }

  public JsonNode checkin() throws IOException {
    Log.d(TAG, "Performing checkin: " + CHECKIN_URL);

    final long now = System.currentTimeMillis();
    mConfig.setLastCheckinAttempt(now);
    try {
      final HttpRequest request = new HttpRequest(CHECKIN_URL, "POST");
      request.header("User-Agent", mUserAgent);

      final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
      builder.put("product", "kegtab-android");
      builder.put("reg_id", mConfig.getRegistrationId());
      builder.put("android_version", String.valueOf(Build.VERSION.SDK_INT));
      builder.put("android_device", Build.DEVICE);
      builder.put("gcm_reg_id", mConfig.getGcmRegistrationId());

      if (mPackageInfo != null) {
        if (mPackageInfo.signatures != null && mPackageInfo.signatures.length > 0) {
          builder.put("android_build_fingerprint",
              Utils.getFingerprintForSignature(mPackageInfo.signatures[0]));
        }
        builder.put("version", String.valueOf(mPackageInfo.versionCode));
      }

      request.form(builder.build());
      final int statusCode = request.code();
      if (statusCode != 200) {
        Log.w(TAG, "Checkin failed, code: " + statusCode);
        throw new IOException("Remote server returned HTTP error " + statusCode);
      }

      final String responseBody = request.body();
      final ObjectMapper mapper = new ObjectMapper();
      final JsonNode rootNode = mapper.readValue(responseBody, JsonNode.class);
      processLastCheckinResponse(rootNode);
      return rootNode;
    } catch (HttpRequestException e) {
      throw e.getCause();
    }
  }

  public void submitBugreport(String message, File reportData) throws IOException {
    final HttpRequest request = new HttpRequest(BUGREPORT_URL, "POST");
    request.header("User-Agent", mUserAgent);
    request.part("product", "kegtab-android");
    request.part("reg_id", mConfig.getRegistrationId());
    request.part("message", message);
    request.part("data", reportData);

    if (mPackageInfo != null) {
      if (mPackageInfo.signatures != null && mPackageInfo.signatures.length > 0) {
        request.part("android_build_fingerprint",
            Utils.getFingerprintForSignature(mPackageInfo.signatures[0]));
      }
      request.part("version", String.valueOf(mPackageInfo.versionCode));
    }

    final int code;
    try {
      code = request.code();
    } catch (HttpRequestException e) {
      throw e.getCause();
    }
    if (code != 200) {
      throw new IOException("Response code: " + code);
    }
  }

  private void processLastCheckinResponse(JsonNode response) {
    Log.d(TAG, "Checkin response: " + response);

    // Sanity check: "status" must be "ok".
    final JsonNode statusNode = response.get("status");
    if (statusNode == null || !statusNode.isTextual()) {
      Log.d(TAG, "Invalid checkin response: no status.");
      return;
    }

    final String status = statusNode.getTextValue();
    if ("ok".equals(status)) {
      Log.d(TAG, "Checkin status: " + status);
    } else {
      Log.d(TAG, "Invalid checkin response: unknown status: " + status);
      return;
    }

    // Ensure "reg_id" field exists.
    final JsonNode regIdNode = response.get("reg_id");
    if (regIdNode == null || !regIdNode.isTextual()) {
      Log.d(TAG, "Invalid checkin response: no status.");
      return;
    }

    // Update saved registration ID if necessary.
    final String existingRegistrationId = mConfig.getRegistrationId();
    final String regId = regIdNode.getTextValue();
    if (existingRegistrationId == null || !existingRegistrationId.equals(regId)) {
      Log.d(TAG, "Updating registration id: " + regId);
      mConfig.setRegistrationId(regId);
    }

    boolean updateAvailable = false;
    final JsonNode updateNeededNode = response.get("update_available");
    if (updateNeededNode != null && updateNeededNode.isBoolean()
        && updateNeededNode.getBooleanValue()) {
      updateAvailable = true;
    }

    boolean updateRequired = false;
    final JsonNode updateRequiredNode = response.get("update_required");
    if (updateRequiredNode != null && updateRequiredNode.isBoolean()
        && updateRequiredNode.getBooleanValue()) {
      updateRequired = true;
    }

    mConfig.setLastCheckinStatus(status);
    mConfig.setUpdateAvailable(updateAvailable);
    mConfig.setUpdateRequired(updateRequired);
  }

}
