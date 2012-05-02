/**
 *
 */
package org.kegbot.app.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import android.content.Context;
import android.provider.Settings.Secure;

import com.google.common.base.Strings;
import com.hoho.android.usbserial.util.HexDump;

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
    final PreferenceHelper helper = new PreferenceHelper(context);
    final String savedId = helper.getDeviceId();

    if (!Strings.isNullOrEmpty(savedId)) {
      return savedId;
    }

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

    final String id = HexDump.toHexString(shortDigest);
    helper.setDeviceId(id);
    return id;
  }

}
