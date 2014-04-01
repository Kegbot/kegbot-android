
package org.kegbot.core.hardware;

import org.kegbot.app.event.Event;
import org.kegbot.core.AuthenticationToken;

public class TokenAttachedEvent implements Event {

  private final AuthenticationToken mToken;

  public TokenAttachedEvent(AuthenticationToken token) {
    mToken = token;
  }

  public AuthenticationToken getToken() {
    return mToken;
  }

}
