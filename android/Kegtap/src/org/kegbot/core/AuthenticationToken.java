/**
 * 
 */
package org.kegbot.core;

/**
 * Represents a hardware authentication token.
 */
public class AuthenticationToken {

  private final String mAuthDevice;

  private final String mTokenValue;

  public AuthenticationToken(String authDevice, String tokenValue) {
    mAuthDevice = authDevice;
    mTokenValue = tokenValue;
  }

  public String getAuthDevice() {
    return mAuthDevice;
  }

  public String getTokenValue() {
    return mTokenValue;
  }

}
