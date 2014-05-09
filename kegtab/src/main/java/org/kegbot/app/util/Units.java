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
package org.kegbot.app.util;

import android.util.Pair;

import org.jscience.physics.measures.Measure;
import org.kegbot.app.config.AppConfiguration;

import javax.measure.quantities.Volume;
import javax.measure.units.NonSI;
import javax.measure.units.SI;

/**
 * Various unit conversion helpers.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class Units {

  private Units() {
    throw new IllegalStateException("Non-instantiable class.");
  }

  public static double volumeMlToOunces(double volumeMl) {
    Measure<Volume> vol = Measure.valueOf(volumeMl, SI.MILLI(NonSI.LITER));
    double ounces = vol.doubleValue(NonSI.OUNCE_LIQUID_US);
    return ounces;
  }

  public static double volumeOuncesToMl(double volumeOunces) {
    Measure<Volume> vol = Measure.valueOf(volumeOunces, NonSI.OUNCE_LIQUID_US);
    double ml = vol.doubleValue(SI.MILLI(NonSI.LITER));
    return ml;
  }

  public static double volumeMlToPints(double volumeMl) {
    return volumeMlToOunces(volumeMl) / 16.0;
  }

  public static double temperatureCToF(double tempC) {
    return (9.0 / 5.0) * tempC + 32;
  }

  /**
   * Returns a humanized value for the given units, according to local preferences. Examples: 3.2
   * oz, 1.5 L, 33mL, 4.5 pint.
   *
   * @param config
   * @param volumeMl
   * @return pair of (amount, label)
   */
  public static Pair<String, String> localize(AppConfiguration config, double volumeMl) {
    return localize(config, volumeMl, true);
  }

  /**
   * Like {@link #localize(AppConfiguration, double)}, but leaves units in terms of their smallest
   * measure (mL instead of L, oz instead of Pints).
   *
   * @param config
   * @param volumeMl
   * @return pair of (amount, label)
   */
  public static Pair<String, String> localizeWithoutScaling(AppConfiguration config,
      double volumeMl) {
    return localize(config, volumeMl, false);
  }

  /**
   * Returns a humanized value for the given units, according to local preferences. Examples: 3.2
   * oz, 1.5 L, 33mL, 4.5 pint.
   *
   * @param config
   * @param volumeMl
   * @param scaleUp  whether to scale units up (mL to L, oz to pint) from base unit.
   * @return
   */
  private static Pair<String, String> localize(AppConfiguration config, double volumeMl,
      boolean scaleUp) {
    final String amount;
    final String label;

    if (config.getUseMetric()) {
      if (Math.abs(volumeMl) < 1000 || !scaleUp) {
        amount = String.format("%d", Integer.valueOf((int) volumeMl));
        label = "mL";
      } else {
        amount = String.format("%.1f", Double.valueOf(volumeMl / 1000.0));
        label = amount == "1.0" ? "liter" : "liters";
      }
    } else {
      double ounces = volumeMlToOunces(volumeMl);
      if (Math.abs(ounces) < 16.0 || !scaleUp) {
        amount = String.format("%.1f", Double.valueOf(ounces));
        label = "oz";
      } else {
        amount = String.format("%.1f", Double.valueOf(ounces / 16.0));
        label = amount == "1.0" ? "pint" : "pints";
      }
    }

    return Pair.create(amount, label);
  }

  /**
   * Returns a capitalized version of a units string, suitable for use in a header.
   *
   * @param units
   * @return
   */
  public static String capitalizeUnits(String units) {
    if ("pints".equals(units)) {
      return "Pints";
    } else if ("pint".equals(units)) {
      return "Pint";
    } else if ("liter".equals(units)) {
      return "Liter";
    } else if ("liters".equals(units)) {
      return "Liters";
    }
    return units;
  }


}
