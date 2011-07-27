/**
 *
 */
package org.kegbot.kegtap.core;

import android.content.Intent;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class KegtapBroadcast {

  /**
   * Action set when a pour is being updated.
   */
  public static final String ACTION_POUR_UPDATE = "org.kegbot.action.POUR_UPDATE";

  /**
   * Extra to be sent with {@link #ACTION_POUR_UPDATE}, giving the flow id as a
   * long.
   */
  public static final String POUR_UPDATE_EXTRA_FLOW_ID = "flow";

  /**
   * Action set when a new temperature reading is recorded.
   */
  public static final String ACTION_THERMO_UPDATE = "org.kegbot.action.THERMO_UPDATE";

  public static final String THERMO_UPDATE_EXTRA_SENSOR_NAME = "sensor_name";
  public static final String THERMO_UPDATE_EXTRA_TEMP_C = "temp_c";

  /**
   * Non-instantiable.
   */
  private KegtapBroadcast() {
    assert (false);
  }

  public static Intent getPourUpdateBroadcastIntent(final long flowId) {
    final Intent intent = new Intent(ACTION_POUR_UPDATE);
    intent.putExtra(POUR_UPDATE_EXTRA_FLOW_ID, flowId);
    return intent;
  }

  public static Intent getThermoUpdateBroadcastIntent(final String sensorName, final double tempC) {
    final Intent intent = new Intent(ACTION_POUR_UPDATE);
    intent.putExtra(THERMO_UPDATE_EXTRA_SENSOR_NAME, sensorName);
    intent.putExtra(THERMO_UPDATE_EXTRA_TEMP_C, tempC);
    return intent;
  }

}
