/**
 *
 */
package org.kegbot.kegboard;

/**
 *
 * @author mike
 */
public class KegboardTemperatureReadingMessage extends KegboardMessage {

  public static final int MESSAGE_TYPE = 0x11;

  public static final int TAG_SENSOR_NAME = 0x01;

  public static final int TAG_SENSOR_VALUE = 0x02;

  public KegboardTemperatureReadingMessage(byte[] wholeMessage) throws KegboardMessageException {
    super(wholeMessage);
  }

  public String getName() {
    return readTagAsString(TAG_SENSOR_NAME);
  }

  @Override
  protected String getStringExtra() {
    return String.format("name=%s value=%.2f", getName(), Double.valueOf(getValue()));
  }

  public double getValue() {
    final Long rawValue = readTagAsLong(TAG_SENSOR_VALUE);
    if (rawValue == null) {
      throw new IllegalStateException("Missing tag.");
    }
    return rawValue.doubleValue() / 1e6;
  }

  @Override
  public short getMessageType() {
    return MESSAGE_TYPE;
  }

}
