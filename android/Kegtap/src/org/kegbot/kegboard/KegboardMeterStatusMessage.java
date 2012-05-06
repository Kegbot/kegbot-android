/**
 *
 */
package org.kegbot.kegboard;


/**
 *
 * @author mike
 */
public class KegboardMeterStatusMessage extends KegboardMessage {

  public static final int MESSAGE_TYPE = 0x10;

  public static final int TAG_METER_NAME = 0x01;
  public static final int TAG_METER_READING = 0x02;

  public KegboardMeterStatusMessage(byte[] wholeMessage) throws KegboardMessageException {
    super(wholeMessage);
  }

  @Override
  public String getStringExtra() {
    return "meter=" + getMeterName() + " ticks=" + getMeterReading();
  }

  public String getMeterName() {
    return readTagAsString(TAG_METER_NAME);
  }

  public long getMeterReading() {
    Long result = readTagAsLong(TAG_METER_READING);
    if (result != null) {
      return result.longValue();
    }
    return 0;
  }

  @Override
  public short getMessageType() {
    return MESSAGE_TYPE;
  }

}
