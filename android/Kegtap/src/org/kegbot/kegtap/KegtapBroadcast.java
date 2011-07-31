/**
 *
 */
package org.kegbot.kegtap;

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
   * Action set when a pour is being updated.
   */
  public static final String ACTION_POUR_START = "org.kegbot.action.POUR_START";

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

  public static final String ACTION_USER_AUTHED = "org.kegbot.action.USER_AUTHED";

  public static final String USER_AUTHED_EXTRA_USERNAME = "username";

  /**
   * Non-instantiable.
   */
  private KegtapBroadcast() {
    assert (false);
  }

  public static Intent getPourStartBroadcastIntent(final long flowId) {
    final Intent intent = new Intent(ACTION_POUR_START);
    intent.putExtra(POUR_UPDATE_EXTRA_FLOW_ID, flowId);
    return intent;
  }

  public static Intent getPourUpdateBroadcastIntent(final long flowId) {
    final Intent intent = new Intent(ACTION_POUR_UPDATE);
    intent.putExtra(POUR_UPDATE_EXTRA_FLOW_ID, flowId);
    return intent;
  }

  public static Intent getThermoUpdateBroadcastIntent(final String sensorName, final double tempC) {
    final Intent intent = new Intent(ACTION_THERMO_UPDATE);
    intent.putExtra(THERMO_UPDATE_EXTRA_SENSOR_NAME, sensorName);
    intent.putExtra(THERMO_UPDATE_EXTRA_TEMP_C, tempC);
    return intent;
  }

  public static Intent getUserAuthedBroadcastIntent(final String username) {
    final Intent intent = new Intent(ACTION_USER_AUTHED);
    intent.putExtra(USER_AUTHED_EXTRA_USERNAME, username);
    return intent;
  }

}
