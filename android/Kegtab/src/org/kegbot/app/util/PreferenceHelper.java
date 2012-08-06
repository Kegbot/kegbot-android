/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
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

import java.io.IOException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceHelper {

  public static final String KEY_SETUP_VERSION = "setup_version";

  public static final String KEY_KEGBOT_URL = "kegbot_url";
  public static final String KEY_API_KEY = "api_key";
  public static final String KEY_USERNAME = "username";
  public static final String KEY_PIN = "pin";
  public static final String KEY_DEVICE_ID = "kbid";
  public static final String KEY_IS_REGISTERED = "is_registered";

  public static final String KEY_ALLOW_REGISTRAION = "allow_registration";
  public static final String KEY_ALLOW_MANUAL_LOGIN = "allow_manual_login";
  public static final String KEY_CACHE_CREDENTIALS = "cache_credentials";

  public static final String KEY_RUN_CORE = "run_core";

  private static final String KEY_GCM_REGISTRATION_ID = "gcm_reg_id";

  private static final String KEY_LAST_CHECKIN_ATTEMPT = "last_checkin_attempt";
  private static final String KEY_LAST_CHECKIN_SUCCESS = "last_checkin_success";
  private static final String KEY_LAST_CHECKIN_RESPONSE = "last_checkin_response";
  private static final String KEY_LAST_CHECKIN_STATUS = "last_checkin_status";

  private final SharedPreferences mSharedPreferences;

  public PreferenceHelper(final Context context) {
    mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
  }

  public String getKegbotUrl() {
    String uriString = mSharedPreferences.getString("kegbot_url", "");
    uriString = uriString.replaceAll("/+$", "");
    return uriString;
  }

  public String getApiUrl() {
    return getKegbotUrl() + "/api";
  }

  public void setKegbotUrl(String url) {
    url = url.replaceAll("/+$", "");
    mSharedPreferences.edit().putString("kegbot_url", url).apply();
  }

  public void setApiKey(String key) {
    mSharedPreferences.edit().putString(KEY_API_KEY, key).apply();
  }

  public String getApiKey() {
    return mSharedPreferences.getString(KEY_API_KEY, "");
  }

  public String getPin() {
    return mSharedPreferences.getString(KEY_PIN, "");
  }

  public void setPin(String pin) {
    mSharedPreferences.edit().putString(KEY_PIN, pin).apply();
  }

  public String getUsername() {
    return mSharedPreferences.getString(KEY_USERNAME, "");
  }

  public void setUsername(String username) {
    mSharedPreferences.edit().putString(KEY_USERNAME, username).apply();
  }

  public long getMinimumVolumeMl() {
    return Long.valueOf(mSharedPreferences.getString("minimum_volume_ml", "10")).longValue();
  }

  public void setRunCore(boolean value) {
    mSharedPreferences.edit().putBoolean(KEY_RUN_CORE, value).apply();
  }

  public boolean getRunCore() {
    return mSharedPreferences.getBoolean(KEY_RUN_CORE, true);
  }

  public long getIdleTimeoutMs() {
    return Long.valueOf(mSharedPreferences.getString("idle_timeout_seconds", "90")).longValue() * 1000;
  }

  public long getIdleWarningMs() {
    return Long.valueOf(mSharedPreferences.getString("idle_warning_seconds", "60")).longValue() * 1000;
  }

  public int getSetupVersion() {
    return mSharedPreferences.getInt(KEY_SETUP_VERSION, 0);
  }

  public void setSetupVersion(int value) {
    mSharedPreferences.edit().putInt(KEY_SETUP_VERSION, value).apply();
  }

  public String getDeviceId() {
    return mSharedPreferences.getString(KEY_DEVICE_ID, "");
  }

  public void setDeviceId(String value) {
    mSharedPreferences.edit().putString(KEY_DEVICE_ID, value).apply();
  }

  public boolean getIsRegistered() {
    return mSharedPreferences.getBoolean(KEY_IS_REGISTERED, false);
  }

  public void setIsRegistered(boolean value) {
    mSharedPreferences.edit().putBoolean(KEY_IS_REGISTERED, value).apply();
  }

  public boolean getAllowRegistration() {
    return mSharedPreferences.getBoolean(KEY_ALLOW_REGISTRAION, true);
  }

  public boolean getAllowManualLogin() {
    return mSharedPreferences.getBoolean(KEY_ALLOW_MANUAL_LOGIN, true);
  }

  public boolean getCacheCredentials() {
    return mSharedPreferences.getBoolean(KEY_CACHE_CREDENTIALS, true);
  }

  public String getGcmRegistrationId() {
    return mSharedPreferences.getString(KEY_GCM_REGISTRATION_ID, "");
  }

  public void setGcmRegistrationId(String regId) {
    mSharedPreferences.edit().putString(KEY_GCM_REGISTRATION_ID, regId).apply();
  }

  public long getLastCheckinAttempt() {
    return mSharedPreferences.getLong(KEY_LAST_CHECKIN_ATTEMPT, Long.MIN_VALUE);
  }

  public void setLastCheckinAttempt(long currentTimeMillis) {
    mSharedPreferences.edit().putLong(KEY_LAST_CHECKIN_ATTEMPT, currentTimeMillis).apply();
  }

  public long getLastCheckinSuccess() {
    return mSharedPreferences.getLong(KEY_LAST_CHECKIN_SUCCESS, Long.MIN_VALUE);
  }

  public void setLastCheckinSuccess(long currentTimeMillis) {
    mSharedPreferences.edit().putLong(KEY_LAST_CHECKIN_SUCCESS, currentTimeMillis).apply();
  }

  public JsonNode getLastCheckinResponse() {
    final String raw = mSharedPreferences.getString(KEY_LAST_CHECKIN_RESPONSE, "{}");
    final ObjectMapper mapper = new ObjectMapper();
    JsonNode rootNode;
    try {
      rootNode = mapper.readValue(raw, JsonNode.class);
    } catch (IOException e) {
      rootNode = null;
    }
    return rootNode;
  }

  public void setLastCheckinResponse(JsonNode response) {
    final String raw = response.toString();
    mSharedPreferences.edit().putString(KEY_LAST_CHECKIN_RESPONSE, raw).apply();
  }

  public String getLastCheckinStatus() {
    return mSharedPreferences.getString(KEY_LAST_CHECKIN_STATUS, "unknown");
  }

  public void setLastCheckinStatus(String status) {
    mSharedPreferences.edit().putString(KEY_LAST_CHECKIN_STATUS, status).apply();
  }

}
