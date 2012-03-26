package org.kegbot.kegtap.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

public class PreferenceHelper {

  public static final String KEY_SETUP_VERSION = "setup_version";

  public static final String KEY_KEGBOT_URL = "kegbot_url";
  public static final String KEY_API_KEY = "api_key";
  public static final String KEY_USERNAME = "username";
  public static final String KEY_PASSWORD = "password";

  public static final String KEY_RUN_CORE = "run_core";

  public static final String KEY_SELECTED_KEGBOT = "selected_kegbot";
  public static final String KEY_SELECTED_KEGBOT_NAME = "selected_kegbot_name";

  private final SharedPreferences mSharedPreferences;

  public PreferenceHelper(final Context context) {
    mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
  }

  public Uri getKegbotUrl() {
    String uriString = mSharedPreferences.getString("kegbot_url", "");
    uriString = uriString.replaceAll("/+$", "");
    return Uri.parse(uriString);
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

  public String getUsername() {
    return mSharedPreferences.getString("username", "");
  }

  public String getPassword() {
    return mSharedPreferences.getString("password", "");
  }

  public void setKegbotName(final String name) {
    mSharedPreferences.edit().putString(KEY_SELECTED_KEGBOT_NAME, name).apply();
  }

  public CharSequence getKegbotName() {
    return mSharedPreferences.getString(KEY_SELECTED_KEGBOT_NAME, null);
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
    return Long.valueOf(mSharedPreferences.getString("idle_timeout_seconds", "20")).longValue() * 1000;
  }

  public long getIdleWarningMs() {
    return Long.valueOf(mSharedPreferences.getString("idle_warning_seconds", "20")).longValue() * 1000;
  }

  public int getSetupVersion() {
    return mSharedPreferences.getInt(KEY_SETUP_VERSION, 0);
  }

  public void setSetupVersion(int value) {
    mSharedPreferences.edit().putInt(KEY_SETUP_VERSION, value).apply();
  }

}
