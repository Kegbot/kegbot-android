/**
 * 
 */
package org.kegbot.core.net;

import org.codehaus.jackson.JsonNode;

/**
 * 
 * @author mike
 */
public class KegnetMessage {

  private final String mEventName;
  private final JsonNode mBody;

  KegnetMessage(String eventName, JsonNode body) {
    mEventName = eventName;
    mBody = body;
  }

  @Override
  public String toString() {
    return "KegnetMessage: event=" + mEventName + " body=" + mBody;
  }

  public String getEventName() {
    return mEventName;
  }

  public JsonNode getBody() {
    return mBody;
  }

}
