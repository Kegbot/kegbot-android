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
package org.kegbot.app.config;

import java.util.Set;

/**
 * A generic key-value interface for storing configuration data.
 */
public interface ConfigurationStore {

  public void putString(String key, String value);

  public void putStringSet(String key, Set<String> values);

  public void putInteger(String key, int value);

  public void putLong(String key, long value);

  public void putBoolean(String key, boolean value);

  public String getString(String key, String defaultValue);

  public Set<String> getStringSet(String key, Set<String> defaultValues);

  public int getInteger(String key, int defaultValue);

  public long getLong(String key, long defaultValue);

  public boolean getBoolean(String key, boolean defaultValue);

}
