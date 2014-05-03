package org.kegbot.app.util;

import junit.framework.TestCase;

public class VersionTest extends TestCase {

  public void testVersions() {
    assertEquals(Version.UNKNOWN, Version.fromString("a.b.c"));
    assertEquals(Version.UNKNOWN, Version.fromString("1.2.x"));
    assertEquals(Version.UNKNOWN, Version.fromString("1.x"));
    assertEquals(Version.UNKNOWN, Version.fromString("1"));

    assertEquals(new Version(1, 2, 3, ""), Version.fromString("1.2.3"));
    assertEquals(new Version(99, 88, 7, ""), Version.fromString("99.88.7"));

    assertEquals(1, Version.fromString("1.2.3").compareTo(Version.fromString("1.2.4")));
  }

}
