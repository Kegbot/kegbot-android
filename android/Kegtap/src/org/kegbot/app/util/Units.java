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

import javax.measure.quantities.Volume;
import javax.measure.units.NonSI;
import javax.measure.units.SI;

import org.jscience.physics.measures.Measure;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class Units {

  private Units() {
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

}
