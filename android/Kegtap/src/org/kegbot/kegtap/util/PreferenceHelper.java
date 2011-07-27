package org.kegbot.kegtap.util;

import android.content.SharedPreferences;

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

  public String getKegbotUrl() {
    return mSharedPreferences.getString("kegbot_url", "");
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
}
