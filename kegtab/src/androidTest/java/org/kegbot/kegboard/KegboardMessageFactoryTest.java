/*
 * Copyright 2014 Bevbot LLC <info@bevbot.com>
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
package org.kegbot.kegboard;

import android.test.InstrumentationTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author mike
 */
public class KegboardMessageFactoryTest extends InstrumentationTestCase {

  private KegboardMessageFactory mFactory;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    mFactory = new KegboardMessageFactory();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testReader() throws IOException {
    List<KegboardMessage> messages = new ArrayList<KegboardMessage>();

    InputStream is =
        getInstrumentation().getTargetContext().getResources().getAssets().open("one_flow_active.bin");
    try {
      byte[] buf = new byte[76];
      while (true) {
        int amt = is.read(buf);
        if (amt < 0) {
          break;
        }

        mFactory.addBytes(buf, amt);
      }

      while (true) {
        final KegboardMessage message = mFactory.getMessage();
        if (message == null) {
          break;
        }
        messages.add(message);
      }

      assertEquals(23, messages.size());
      assertTrue(messages.get(0) instanceof KegboardHelloMessage);
      assertTrue(messages.get(2) instanceof KegboardMeterStatusMessage);

      KegboardMeterStatusMessage status = (KegboardMeterStatusMessage) messages.get(2);

      Map<Integer, byte[]> tags = status.mTags;
      dumpTags(tags);
      assertEquals(2, tags.size());

      for (KegboardMessage message : messages) {
        System.out.println(message);
      }
    } finally {
      is.close();
    }

  }

  private void dumpTags(Map<Integer, byte[]> tags) {
    for (Map.Entry<Integer, byte[]> entry : tags.entrySet()) {
      System.out.println(String.format("%04x", entry.getKey())
          + HexDump.dumpHexString(entry.getValue()));
    }
  }

}
