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
package org.kegbot.core;

import java.util.Map;

import org.kegbot.proto.Models.KegTap;

import com.google.common.collect.Maps;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class ConfigurationManager {

  private static ConfigurationManager sSingleton = null;

  /**
   * Maps tap names to TapDetail.
   */
  private final Map<String, KegTap> mTapConfig = Maps.newLinkedHashMap();

  public KegTap getTapDetail(final String tapName) {
    return mTapConfig.get(tapName);
  }

  public void setTapDetail(final String tapName, final KegTap tapDetail) {
    mTapConfig.put(tapName, tapDetail);
  }

  public static ConfigurationManager getSingletonInstance() {
    if (sSingleton == null) {
      sSingleton = new ConfigurationManager();
    }
    return sSingleton;
  }

}
