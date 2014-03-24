package org.kegbot.app.util;

import junit.framework.TestCase;

public class KegSizesTest extends TestCase {

  public void testSizes() {
    for (final String size : KegSizes.allLabelsAscendingVolume()) {
      final double volume = KegSizes.getVolumeMl(size);
      assertEquals(size, KegSizes.getLabelForVolume(volume));
    }
  }

}
