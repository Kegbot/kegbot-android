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
package org.kegbot.app.util;

import android.content.Context;
import android.provider.Settings.Secure;

import com.google.common.base.Strings;

import org.apache.commons.codec.binary.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

/**
 * Utility methods for getting device-unique information.
 */
public class DeviceId {

  /**
   * Generates and returns a unique device id.
   *
   * @param context
   * @return
   */
  public static String getDeviceId(Context context) {
    final MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException();
    }

    final String androidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);

    if (!Strings.isNullOrEmpty(androidId)) {
      md.update(androidId.getBytes());
    } else {
      final Random random = new Random();
      byte randBytes[] = new byte[16];
      random.nextBytes(randBytes);
      md.update(randBytes);
    }

    final byte[] digest = md.digest();
    final byte[] shortDigest = Arrays.copyOfRange(digest, 0, 8);

    final String id = Hex.encodeHexString(shortDigest, true);
    return id;
  }

}
