/*
 * Copyright 2003-2020 The Kegbot Project contributors <info@kegbot.org>
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

/**
 *
 */
package org.kegbot.app.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Set;

/**
 * A {@link ConfigurationStore} backed by an Android {@link SharedPreferences} instance.
 */
public class SharedPreferencesConfigurationStore implements ConfigurationStore {

  private final SharedPreferences mSharedPreferences;

  public SharedPreferencesConfigurationStore(SharedPreferences prefs) {
    mSharedPreferences = prefs;
  }

  public static SharedPreferencesConfigurationStore getDefaultSharedPreferncesConfigurationStore(Context context){
    return new SharedPreferencesConfigurationStore(PreferenceManager.getDefaultSharedPreferences(context));
  }

  public static SharedPreferencesConfigurationStore fromName(Context context, String sharedPrefsName) {
    return new SharedPreferencesConfigurationStore(
        context.getSharedPreferences(sharedPrefsName, 0));
  }

  @Override
  public void putString(String key, String value) {
    mSharedPreferences.edit().putString(getKey(key), value).apply();
  }

  @Override
  public void putStringSet(String key, Set<String> values) {
    mSharedPreferences.edit().putStringSet(key, values).apply();
  }

  @Override
  public void putInteger(String key, int value) {
    mSharedPreferences.edit().putInt(getKey(key), value).apply();
  }

  @Override
  public void putLong(String key, long value) {
    mSharedPreferences.edit().putLong(getKey(key), value).apply();
  }

  @Override
  public void putBoolean(String key, boolean value) {
    mSharedPreferences.edit().putBoolean(getKey(key), value).apply();
  }

  @Override
  public String getString(String key, String defaultValue) {
    return mSharedPreferences.getString(getKey(key), defaultValue);
  }

  @Override
  public Set<String> getStringSet(String key, Set<String> defaultValues) {
    return mSharedPreferences.getStringSet(key, defaultValues);
  }

  @Override
  public int getInteger(String key, int defaultValue) {
    return mSharedPreferences.getInt(getKey(key), defaultValue);
  }

  @Override
  public long getLong(String key, long defaultValue) {
    return mSharedPreferences.getLong(getKey(key), defaultValue);
  }

  @Override
  public boolean getBoolean(String key, boolean defaultValue) {
    return mSharedPreferences.getBoolean(getKey(key), defaultValue);
  }

  /**
   * Returns the key used within shared preferences.
   *
   * @param basename base key name used in {@link #getString(String, String)} and {@link
   *                 #putString(String, String)}.
   * @return the actual key used in shared preferences
   */
  static String getKey(String basename) {
    return String.format("config:%s", basename);
  }

}
