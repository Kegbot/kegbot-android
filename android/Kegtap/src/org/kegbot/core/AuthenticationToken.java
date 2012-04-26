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

  @Override
  public String toString() {
    return "[AuthenticationToken authDevice=" + mAuthDevice + " tokenValue=" + mTokenValue + "]";
  }

  public String getAuthDevice() {
    return mAuthDevice;
  }

  public String getTokenValue() {
    return mTokenValue;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((mAuthDevice == null) ? 0 : mAuthDevice.hashCode());
    result = prime * result + ((mTokenValue == null) ? 0 : mTokenValue.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    AuthenticationToken other = (AuthenticationToken) obj;
    if (mAuthDevice == null) {
      if (other.mAuthDevice != null)
        return false;
    } else if (!mAuthDevice.equals(other.mAuthDevice))
      return false;
    if (mTokenValue == null) {
      if (other.mTokenValue != null)
        return false;
    } else if (!mTokenValue.equals(other.mTokenValue))
      return false;
    return true;
  }

}
