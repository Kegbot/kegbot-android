/*
 * Copyright 2003-2020 The Kegbot Project contributors <info@kegbot.org>
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
