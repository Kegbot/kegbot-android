
package org.kegbot.core.hardware;

import org.kegbot.app.event.Event;
import org.kegbot.core.AuthenticationToken;

public class TokenDetachedEvent implements Event {

  private final AuthenticationToken mToken;

  public TokenDetachedEvent(AuthenticationToken token) {
    mToken = token;
  }

  public AuthenticationToken getToken() {
    return mToken;
  }

}
