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

package org.kegbot.app.util;

import com.google.common.collect.ImmutableMap;

import java.util.Collection;
import java.util.Map;

/**
 * Constants for well-known keg sizes. These are shared with the kegbot server.
 */
public class KegSizes {

  public static final String CORNY = "corny";
  public static final String SIXTH_BARREL = "sixth";
  public static final String EURO_HALF_BARREL = "euro-half";
  public static final String QUARTER_BARREL = "quarter";
  public static final String EURO = "euro";
  public static final String HALF_BARREL = "half-barrel";
  public static final String OTHER = "other";

  public static final Map<String, String> DESCRIPTIONS = ImmutableMap.<String, String>builder()
      .put(CORNY, "Corny Keg (5 gal)")
      .put(SIXTH_BARREL, "Sixth Barrel (5.17 gal)")
      .put(QUARTER_BARREL, "Quarter Barrel (7.75)")
      .put(EURO_HALF_BARREL, "European Half Barrel (50 L)")
      .put(HALF_BARREL, "Half Barrel (15.5 gal)")
      .put(EURO, "European Full Barrel (100 L)")
      .put(OTHER, "Other")
      .build();

  // Keep ordered by volume ascending.
  private static final Map<String, Double> VOLUMES_ML = ImmutableMap.<String, Double>builder()
      .put(CORNY, Double.valueOf(18927.1))
      .put(SIXTH_BARREL, Double.valueOf(19570.6))
      .put(QUARTER_BARREL, Double.valueOf(29336.9))
      .put(EURO_HALF_BARREL, Double.valueOf(50000.0))
      .put(HALF_BARREL, Double.valueOf(58673.9))
      .put(EURO, Double.valueOf(100000.0))
      .build();

  public static final String getDescription(String typeName) {
    return DESCRIPTIONS.get(typeName);
  }

  public static final double getVolumeMl(String typeName) {
    final Double value = VOLUMES_ML.get(typeName);
    if (value != null) {
      return value.doubleValue();
    }
    return 0.0;
  }

  public static final String getLabelForVolume(double volumeMl) {
    for (final Map.Entry<String, Double> entry : VOLUMES_ML.entrySet()) {
      final double currentVolume = entry.getValue().doubleValue();
      if (currentVolume == volumeMl) {
        return entry.getKey();
      }
    }
    return OTHER;
  }

  public static Collection<String> allLabelsAscendingVolume() {
    return VOLUMES_ML.keySet();
  }
}
