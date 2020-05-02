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

import junit.framework.TestCase;

public class VersionTest extends TestCase {

  public void testVersions() {
    assertEquals(Version.UNKNOWN, Version.fromString("a.b.c"));
    assertEquals(Version.UNKNOWN, Version.fromString("1.2.x"));
    assertEquals(Version.UNKNOWN, Version.fromString("1.x"));
    assertEquals(Version.UNKNOWN, Version.fromString("1"));

    assertEquals(new Version(1, 2, 3, ""), Version.fromString("1.2.3"));
    assertEquals(new Version(1, 2, 3, "a1"), Version.fromString("1.2.3a1"));
    assertEquals(new Version(99, 88, 7, ""), Version.fromString("99.88.7"));

    assertEquals(-1, Version.fromString("1.2.3").compareTo(Version.fromString("1.2.4")));
  }

}
