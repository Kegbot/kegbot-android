package org.kegbot.kegtap.util;

import android.content.SharedPreferences;
import android.net.Uri;

public class PreferenceHelper {

  public static final String KEY_KEGBOT_URL = "kegbot_url";
  public static final String KEY_USERNAME = "username";
  public static final String KEY_PASSWORD = "password";

  public static final String KEY_SELECTED_KEGBOT = "selected_kegbot";
  public static final String KEY_SELECTED_KEGBOT_NAME = "selected_kegbot_name";

  private final SharedPreferences mSharedPreferences;

  public PreferenceHelper(final SharedPreferences sharedPreferences) {
    mSharedPreferences = sharedPreferences;
  }

  public Uri getKegbotUrl() {
    String uriString = mSharedPreferences.getString("kegbot_url", "");
    uriString = uriString.replaceAll("/+$", "");
    return Uri.parse(uriString);
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

  public boolean getRunCore() {
    return mSharedPreferences.getBoolean("run_core", false);
  }

  public long getIdleTimeoutMs() {
    return Long.valueOf(mSharedPreferences.getString("idle_timeout_seconds", "20")).longValue() * 1000;
  }

  public long getIdleWarningMs() {
    return Long.valueOf(mSharedPreferences.getString("idle_warning_seconds", "20")).longValue() * 1000;
  }

}
