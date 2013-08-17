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
package org.kegbot.app.config;

/**
 * Helper methods for getting and setting preferences from a
 * {@link ConfigurationStore}. This is the primary interface for app-local
 * configuration settings.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class AppConfiguration {

  public static final String TRUE = Boolean.TRUE.toString();
  public static final String FALSE = Boolean.FALSE.toString();

  private final ConfigurationStore mConfig;

  public AppConfiguration(final ConfigurationStore config) {
    mConfig = config;
  }

  private String get(ConfigKey key) {
    return mConfig.getString(key.getName(), key.getDefault());
  }

  private void set(ConfigKey key, String value) {
    mConfig.putString(key.getName(), value);
  }

  private boolean getBoolean(ConfigKey key) {
    return mConfig.getBoolean(key.getName(), Boolean.valueOf(key.getDefault()).booleanValue());
  }

  private void setBoolean(ConfigKey key, boolean value) {
    mConfig.putBoolean(key.getName(), value);
  }

  private int getInteger(ConfigKey key) {
    return mConfig.getInteger(key.getName(), Integer.valueOf(key.getDefault()).intValue());
  }

  private void setInteger(ConfigKey key, int value) {
    mConfig.putInteger(key.getName(), value);
  }

  private long getLong(ConfigKey key) {
    return mConfig.getLong(key.getName(), Long.valueOf(key.getDefault()).longValue());
  }

  private void setLong(ConfigKey key, long value) {
    mConfig.putLong(key.getName(), value);
  }

  public String getKegbotUrl() {
    String uriString = get(ConfigKey.KEGBOT_URL);
    uriString = uriString.replaceAll("/+$", "");
    return uriString;
  }

  public String getApiUrl() {
    return getKegbotUrl() + "/api";
  }

  public void setKegbotUrl(String url) {
    url = url.replaceAll("/+$", "");
    set(ConfigKey.KEGBOT_URL, url);
  }

  public void setApiKey(String key) {
    set(ConfigKey.API_KEY, key);
  }

  public String getApiKey() {
    return get(ConfigKey.API_KEY);
  }

  public String getPin() {
    return get(ConfigKey.PIN);
  }

  public void setPin(String pin) {
    set(ConfigKey.PIN, pin);
  }

  public String getUsername() {
    return get(ConfigKey.USERNAME);
  }

  public void setUsername(String username) {
    set(ConfigKey.USERNAME, username);
  }

  public long getMinimumVolumeMl() {
    // TODO(mikey): Stored as a string due to EditTextPreference stupidity. Fix.
    return Long.valueOf(get(ConfigKey.FLOW_MINIMUM_VOLUME_ML)).longValue();
  }

  public void setRunCore(boolean value) {
    setBoolean(ConfigKey.RUN_CORE, value);
  }

  public boolean getRunCore() {
    return getBoolean(ConfigKey.RUN_CORE);
  }

  public long getIdleTimeoutMs() {
    // TODO(mikey): Stored as a string due to EditTextPreference stupidity. Fix.
    return Long.valueOf(get(ConfigKey.FLOW_IDLE_TIMEOUT_SECONDS)).longValue() * 1000;
  }

  public long getIdleWarningMs() {
    // TODO(mikey): Stored as a string due to EditTextPreference stupidity. Fix.
    return Long.valueOf(get(ConfigKey.FLOW_IDLE_WARNING_SECONDS)).longValue() * 1000;
  }

  public int getSetupVersion() {
    return getInteger(ConfigKey.SETUP_VERSION);
  }

  public void setSetupVersion(int value) {
    setInteger(ConfigKey.SETUP_VERSION, value);
  }

  public boolean getIsRegistered() {
    return getBoolean(ConfigKey.IS_REGISTERED);
  }

  public void setIsRegistered(boolean value) {
    setBoolean(ConfigKey.IS_REGISTERED, value);
  }

  public boolean getAllowRegistration() {
    return getBoolean(ConfigKey.ALLOW_REGISTRATION);
  }

  public boolean getAllowManualLogin() {
    return getBoolean(ConfigKey.ALLOW_MANUAL_LOGIN);
  }

  public boolean getCacheCredentials() {
    return getBoolean(ConfigKey.CACHE_CREDENTIALS);
  }

  public String getGcmRegistrationId() {
    return get(ConfigKey.GCM_REGISTRATION_ID);
  }

  public void setGcmRegistrationId(String regId) {
    set(ConfigKey.GCM_REGISTRATION_ID, regId);
  }

  public boolean getEnableFlowAutoStart() {
    return getBoolean(ConfigKey.ENABLE_AUTOMATIC_FLOW_START);
  }

  public void setEnableFlowAutoStart(boolean value) {
    setBoolean(ConfigKey.ENABLE_AUTOMATIC_FLOW_START, value);
  }

  public boolean getEnableAutoTakePhoto() {
    return getBoolean(ConfigKey.AUTO_TAKE_PHOTOS);
  }

  public void setEnableAutoTakePhoto(boolean value) {
    setBoolean(ConfigKey.AUTO_TAKE_PHOTOS, value);
  }

  public boolean getEnableCameraSounds() {
      return getBoolean(ConfigKey.ENABLE_CAMERA_SOUNDS);
  }

  public void setEnableCameraSounds(boolean value) {
      setBoolean(ConfigKey.ENABLE_CAMERA_SOUNDS, value);
  }

  public long getLastCheckinAttempt() {
    return getLong(ConfigKey.LAST_CHECKIN_ATTEMPT_MILLIS);
  }

  public void setLastCheckinAttempt(long value) {
    setLong(ConfigKey.LAST_CHECKIN_ATTEMPT_MILLIS, value);
  }

  public long getLastCheckinSuccess() {
    return getLong(ConfigKey.LAST_CHECKIN_SUCCESS_MILLIS);
  }

  public void setLastCheckinSuccess(long value) {
    setLong(ConfigKey.LAST_CHECKIN_SUCCESS_MILLIS, value);
  }

  public String getLastCheckinStatus() {
    return get(ConfigKey.LAST_CHECKIN_STATUS);
  }

  public void setLastCheckinStatus(String value) {
    set(ConfigKey.LAST_CHECKIN_STATUS, value);
  }

  public void setUpdateNeeded(boolean value) {
    setBoolean(ConfigKey.UPDATE_NEEDED, value);
  }

  public boolean getUpdateNeeded() {
    return getBoolean(ConfigKey.UPDATE_NEEDED);
  }

  public void setUpdateRequired(boolean value) {
    setBoolean(ConfigKey.UPDATE_REQUIRED, value);
  }

  public boolean getUpdateRequired() {
    return getBoolean(ConfigKey.UPDATE_REQUIRED);
  }

  public String getKegboardName() {
    return get(ConfigKey.KEGBOARD_NAME);
  }

  public void setKegboardName(String value) {
    set(ConfigKey.KEGBOARD_NAME, value);
  }

  public void setUseMetric(boolean value) {
    setBoolean(ConfigKey.VOLUME_UNITS_METRIC, value);
  }

  public boolean getUseMetric() {
    return getBoolean(ConfigKey.VOLUME_UNITS_METRIC);
  }

  public void setTemperaturesCelsius(boolean value) {
    setBoolean(ConfigKey.TEMPERATURE_UNITS_CELSIUS, value);
  }

  public boolean getTemperaturesCelsius() {
    return getBoolean(ConfigKey.TEMPERATURE_UNITS_CELSIUS);
  }

  public boolean stayAwake() {
    return getBoolean(ConfigKey.STAY_AWAKE);
  }

  public boolean keepScreenOn() {
    return getBoolean(ConfigKey.KEEP_SCREEN_ON);
  }

}
