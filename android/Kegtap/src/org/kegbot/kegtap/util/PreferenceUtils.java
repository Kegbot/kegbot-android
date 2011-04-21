package org.kegbot.kegtap.util;

import android.content.SharedPreferences;

public class PreferenceUtils {

  public static final String SELECTED_KEGBOT_KEY = "selected_kegbot";
  public static final String SELECTED_KEGBOT_NAME_KEY = "selected_kegbot_name";

  /**
   * Non instantiable.
   */
  private PreferenceUtils() {}

  public static String getKegbotUrl(SharedPreferences preferences) {
    return preferences.getString("kegbot_url", "");
  }

  public static String getUsername(SharedPreferences preferences) {
    return preferences.getString("username", "");
  }

  public static String getPassword(SharedPreferences preferences) {
    return preferences.getString("password", "");
  }

  public static void setKegbotName(SharedPreferences preferences, String name) {
    preferences.edit()
    .putString(SELECTED_KEGBOT_NAME_KEY, name)
    .apply();
  }

  public static CharSequence getKegbotName(SharedPreferences preferences) {
    return preferences.getString(SELECTED_KEGBOT_NAME_KEY, null);
  }
}
