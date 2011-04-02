package org.kegbot.kegtap.util;

import android.content.SharedPreferences;

public class PreferenceUtils {

  public static final String SELECTED_KEGBOT_KEY = "selected_kegbot";
  public static final String SELECTED_KEGBOT_NAME_KEY = "selected_kegbot_name";
  
  /**
   * Non instantiable.
   */
  private PreferenceUtils() {}

  public static String getKeybotUrl(SharedPreferences preferences) {
    return preferences.getString(SELECTED_KEGBOT_KEY, null);
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
